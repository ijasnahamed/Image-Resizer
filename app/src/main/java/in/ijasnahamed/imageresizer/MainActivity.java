package in.ijasnahamed.imageresizer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    private ImageHandler handler;

    private static ImageView image;
    private Button selectImageBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new ImageHandler(this);

        image = (ImageView) findViewById(R.id.imageView);
        selectImageBtn = (Button) findViewById(R.id.addImageBtn);

        selectImageBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        handler.showView();
                    }
                }
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        handler.onResult(requestCode, resultCode, data);
    }

    public static class PostCaptureHandler{
        public void setBitmap(Bitmap bm){
            image.setImageBitmap(bm);
        }
    }
}
