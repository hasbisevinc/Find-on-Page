package com.hasbis.findonpage;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int CAMERA_REQUEST = 1888;
    private static final int CAMERA_PERMISSION_CODE = 100;

    FirebaseVisionText firebaseVisionText = null;

    private DrawableImageView imageView;
    private LinearLayout findBox;
    private Button photoButton;
    private Button findButton;
    private EditText textToFind;
    private String mCameraFileName = "";

    private int mlImageHeight = -1;
    private int mlImageWidth = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.imageView = this.findViewById(R.id.preview_imageview);
        this.findBox = this.findViewById(R.id.find_layout);
        this.photoButton = this.findViewById(R.id.camera_button);
        this.findButton = this.findViewById(R.id.find_button);
        this.textToFind = this.findViewById(R.id.word_edittext);

        photoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCameraButtonClicked();
            }
        });

        findButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markText(textToFind.getText().toString());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.new_image) {
            onCameraButtonClicked();
        }
        return super.onOptionsItemSelected(item);
    }

    private void onCameraButtonClicked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE},
                        CAMERA_PERMISSION_CODE);
            } else {
                getImageFromCamera();
            }
        } else {
            getImageFromCamera();
        }
    }

    private void getImageFromCamera() {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        Intent intent = new Intent();
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);

        Date date = new Date();
        DateFormat df = new SimpleDateFormat("-mm-ss");

        String newPicFile = df.format(date) + ".jpg";
        String outPath = "/sdcard/" + newPicFile;
        File outFile = new File(outPath);

        mCameraFileName = outFile.toString();
        Uri outuri = Uri.fromFile(outFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outuri);
        startActivityForResult(intent, CAMERA_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent cameraIntent = new
                        Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            } else {
                Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CAMERA_REQUEST) {
                findBox.setVisibility(View.VISIBLE);
                photoButton.setVisibility(View.GONE);
                Uri image = null;
                if (mCameraFileName != null) {
                    image = Uri.fromFile(new File(mCameraFileName));
                    imageView.setImageURI(image);
                    imageView.setVisibility(View.VISIBLE);
                    findText(image);
                }
                File file = new File(mCameraFileName);
                if (!file.exists()) {
                    file.mkdir();
                }
            }
        }
    }

    private void showResultCount(int count) {
        String str;
        if (count == 0) {
            str = getString(R.string.no_result_found);
        } else {
            str = getString(R.string.result_count, String.valueOf(count));
        }

        Toast.makeText(this, str, Toast.LENGTH_LONG)
                .show();
    }

    private void findText(Uri uri) {
        FirebaseVisionImage image;
        try {
            image = FirebaseVisionImage.fromFilePath(this, uri);
            mlImageWidth = image.getBitmapForDebugging().getWidth();
            mlImageHeight = image.getBitmapForDebugging().getHeight();
        } catch (IOException e) {
            Log.e(TAG, "findText: ", e);
            return;
        }

        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();

        Task<FirebaseVisionText> result =
                detector.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText local_firebaseVisionText) {
                        firebaseVisionText = local_firebaseVisionText;
                    }
                })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "onFailure: ", e);
                            }
                        });

    }

    private void markText(String text) {
        if (firebaseVisionText == null) {
            onCameraButtonClicked();
            Toast.makeText(this, getString(R.string.try_again), Toast.LENGTH_LONG)
                    .show();
            return;
        }

        Log.d(TAG, "markText: all text:"+firebaseVisionText.getText());
        ArrayList<Rect> rects = new ArrayList<>();
        for (FirebaseVisionText.TextBlock block: firebaseVisionText.getTextBlocks()) {
            for (FirebaseVisionText.Line line: block.getLines()) {
                for (FirebaseVisionText.Element element: line.getElements()) {
                    String elementText = element.getText();
                    Rect elementFrame = element.getBoundingBox();

                    if (elementFrame != null &&
                            elementText.toLowerCase().contains(text.toLowerCase())) {
                        elementFrame = convertRect(elementFrame);
                        rects.add(elementFrame);
                    }

                }
            }
        }
        showResultCount(rects.size());
        imageView.setRects(rects);
    }

    private Rect convertRect(Rect rect) {
        if (imageView.getWidth() == 0 || imageView.getHeight() == 0) {
            return new Rect(0, 0, 0, 0);
        }

        float scaleW = (float)mlImageWidth / (float)imageView.getWidth();
        float scaleH = (float)mlImageHeight / (float)imageView.getHeight();
        int left = scaleRect(rect.left, scaleW);
        int top = scaleRect(rect.top, scaleH);
        int right = scaleRect(rect.right, scaleW);
        int bottom = scaleRect(rect.bottom, scaleH);

        return new Rect(left, top, right, bottom);
    }

    private static int scaleRect(float px, float scale){
        return (int)(px/scale);
    }
}
