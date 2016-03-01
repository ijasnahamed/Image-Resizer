package in.ijasnahamed.imageresizer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImageHandler {
    private Context context;
    private int IMAGE_PIC_CODE = 1000, CROP_CAMERA_REQUEST = 1001, CROP_GALLARY_REQUEST = 1002;
    private Intent imageCaptureintent, galleryIntent;
    private boolean isActivityAvailable;
    private List<ResolveInfo> cameraList;
    private List<Intent> cameraIntents;
    private Uri outputFileUri, selectedImageUri;
    private String cameraImageFilePath, absoluteCameraImagePath;
    private Bitmap bitmap;

    public ImageHandler(Context context) {
        this.context = context;

        setFileUriForCameraImage();
    }

    /* Set Path to save captured photo from camera */

    private void setFileUriForCameraImage() {
        File appDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Image-Resizer");

        if(!appDir.exists() && !appDir.isDirectory()){
            if (appDir.mkdirs()) {
                // app directory creatory
            } else {
                // Failed to create directory
            }
        } else {
            // App dir already exist
        }

        final String fname = "image_cropper_"+System.currentTimeMillis() + ".jpg";

        final File sdImageMainDirectory = new File(appDir, fname);
        absoluteCameraImagePath = sdImageMainDirectory.getAbsolutePath();
        outputFileUri = Uri.fromFile(sdImageMainDirectory);
    }

    public String getCameraImagePath() {
        return cameraImageFilePath;
    }

    /* Check if Camera is available in phone */

    private void getActivities() {

        imageCaptureintent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        PackageManager packageManager = ((Activity) context)
                .getPackageManager();
        this.cameraList = packageManager.queryIntentActivities(
                imageCaptureintent, 0);
        if (cameraList.size() > 0) {
            isActivityAvailable = true;
        } else {
            isActivityAvailable = false;
        }
    }

    /* Get All Apps that can capture image */

    private void fillCameraActivities() {
        getActivities();
        if (!isActivityAvailable) {
            return;
        }
        cameraIntents = new ArrayList<Intent>();
        for (ResolveInfo resolveInfo : cameraList) {
            Intent intent = new Intent(imageCaptureintent);
            intent.setPackage(resolveInfo.activityInfo.packageName);
            intent.setComponent(new ComponentName(
                    resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name));
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            cameraIntents.add(intent);
        }
    }

    /* Select all apps that show images */

    private void fillGallaryIntent() {
        galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_PICK);
    }

    /* Shows alert to user to select image either from camera or gallery */

    public void showView() {
        fillCameraActivities();
        fillGallaryIntent();

        // Chooser of gallery options.
        final Intent chooserIntent = Intent.createChooser(galleryIntent,
                "Select Source");

        // Add the camera options.
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                cameraIntents.toArray(new Parcelable[]{}));

        ((Activity) context).startActivityForResult(chooserIntent, IMAGE_PIC_CODE);
    }

    /* Convert saved image to bitmap format */

    private Bitmap getBitmapFromURL(String src) {

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(src, options);

        options.inSampleSize = calculateInSampleSize(options, 192, 256);

        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(src, options);

    }

    /* Calculate In Sample size */

    private int calculateInSampleSize(BitmapFactory.Options options,
                                      int reqWidth, int reqHeight) {

        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /* Get complete path of selected image from gallery */

    public String getRealPathFromURI(Context context, Uri contentUri) {

        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri, proj, null,
                    null, null);
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /* Status callback method after image select action */

    public void onResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == IMAGE_PIC_CODE) {
                boolean isCamera;
                if (data == null) {
                    isCamera = true;
                } else {
                    final String action = data.getAction();

                    if (action == null) {
                        isCamera = false;
                    } else {
                        isCamera = action
                                .equals(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH
                            && action != null) {
                        isCamera = true;
                    } else {

                    }
                }
                if (isCamera) {
                    selectedImageUri = outputFileUri;
                    onResultCameraOK();
                } else {
                    selectedImageUri = data == null ? null : data.getData();
                    onResultGalleryOK();
                }
            }
        }

        if (requestCode == CROP_CAMERA_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                resultOnCropOkOfCamera(data);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                resultOnCroppingCancel();
            }
        }

        if (requestCode == CROP_GALLARY_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                resultOnCropOkOfGallary(data);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                resultOnCroppingCancel();
            }
        }

    }

    /* Implements image cropping */

    private void doCropping(int code) {

        Intent cropIntent = new Intent("com.android.camera.action.CROP");
        cropIntent.setDataAndType(selectedImageUri, "image/*");
        cropIntent.putExtra("crop", "true");
        cropIntent.putExtra("aspectX", 1);
        cropIntent.putExtra("aspectY", 1);
        cropIntent.putExtra("outputX", 256);
        cropIntent.putExtra("outputY", 256);
        cropIntent.putExtra("return-data", true);
        try {
            ((Activity) context).startActivityForResult(cropIntent, code);
        } catch (Exception e) {

        }
    }

    /* Image captured from camera */

    private void onResultCameraOK() {

        this.cameraImageFilePath = absoluteCameraImagePath;
        this.bitmap = getBitmapFromURL(cameraImageFilePath);
        doCropping(CROP_CAMERA_REQUEST);
    }

    /* Image selected from gallery */

    private void onResultGalleryOK() {

        this.cameraImageFilePath = selectedImageUri.toString();
        this.bitmap = getBitmapFromURL(getRealPathFromURI(context,
                selectedImageUri));
        doCropping(CROP_GALLARY_REQUEST);
    }

    /* Post handler method after cropping captured image from camera */

    private void resultOnCropOkOfCamera(Intent data) {

        this.bitmap = data.getExtras().getParcelable("data");
        setImageProfile();
    }

    /* Post handler method after cropping selected image from gallery */

    private void resultOnCropOkOfGallary(Intent data) {

        Bundle extras2 = data.getExtras();
        this.bitmap = extras2.getParcelable("data");
        setImageProfile();
    }

    /* Cancelled cropping and shows slected/captured image without crop */

    private void resultOnCroppingCancel() {
        setImageProfile();
    }

    /* Sets Final image to image view */

    private void setImageProfile() {

        if(bitmap!=null){
            MainActivity.PostCaptureHandler handler = new MainActivity.PostCaptureHandler();
            handler.setBitmap(bitmap);
        } else {

        }
    }
}
