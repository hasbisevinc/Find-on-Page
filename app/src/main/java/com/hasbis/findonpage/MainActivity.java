package com.hasbis.findonpage;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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

import dmax.dialog.SpotsDialog;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int CAMERA_REQUEST = 1888;
    private static final int CAMERA_PERMISSION_CODE = 100;

    FirebaseVisionText firebaseVisionText = null;

    private DrawableImageView imageView;
    private LinearLayout findBox;
    private LinearLayout showFindBoxLayout;
    private Button photoButton;
    private Button findButton;
    private Button selectAllButton;
    private EditText textToFind;

    private String mCameraFileName = "";
    private int mlImageHeight = -1;
    private int mlImageWidth = -1;

    private AlertDialog spotsDialog = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.imageView = this.findViewById(R.id.preview_imageview);
        this.findBox = this.findViewById(R.id.find_layout);
        this.showFindBoxLayout = this.findViewById(R.id.show_find_box_layout);
        this.photoButton = this.findViewById(R.id.camera_button);
        this.findButton = this.findViewById(R.id.find_button);
        this.selectAllButton = this.findViewById(R.id.select_all_button);
        this.textToFind = this.findViewById(R.id.word_edittext);

        photoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCameraButtonClicked();
            }
        });

        selectAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (firebaseVisionText != null &&
                        !TextUtils.isEmpty(firebaseVisionText.getText())) {
                    showSelectAllDialog();
                }
            }
        });

        findButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (textToFind.getText().toString().length() < 1) {
                    return;
                }

                markText(textToFind.getText().toString());
                hideKeyboard();
                hideFindBox();
            }
        });

        showFindBoxLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFindBox();
            }
        });

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
                hideFindBox();
            }
        });
    }

    private void showSelectAllDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_all);

        final EditText input = new EditText(this);
        input.setText(firebaseVisionText.getText());
        builder.setView(input);

        builder.setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }

    private void hideFindBox() {
        findBox.animate().translationY(-1 * findBox.getHeight());
        showFindBoxLayout.setAlpha(0.7f);
    }

    private void showFindBox() {
        findBox.animate().translationY(0);
        showFindBoxLayout.setAlpha(0);
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
                onCameraButtonClicked();
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
                imageView.setRects(new ArrayList<Rect>());
                findBox.setVisibility(View.VISIBLE);
                photoButton.setVisibility(View.GONE);
                Uri image = null;
                if (mCameraFileName != null) {
                    image = Uri.fromFile(new File(mCameraFileName));
                    imageView.setImageURI(image);
                    imageView.setVisibility(View.VISIBLE);
                    findText(image);
                }
            }
        }
    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view == null) {
            view = new View(this);
        }
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
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

        showLoadingDialog();
        runOCR(image);
    }

    private void runOCR(final FirebaseVisionImage image) {
        runOCR(image,false, false);
    }

    private void runOCR(final FirebaseVisionImage image, final boolean rotatedLeft, final boolean rotatedRight) {
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();

        detector.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText local_firebaseVisionText) {
                        hideLoadingDialog();
                        firebaseVisionText = local_firebaseVisionText;
                        if (firebaseVisionText == null ||
                                TextUtils.isEmpty(firebaseVisionText.getText())) {
                            if (!rotatedLeft) {
                                // rotate left
                                showLoadingDialog();
                                runOCR(rotateImage(image, true),
                                        true, false);
                            } else if (rotatedLeft && !rotatedRight) {
                                // rotate right
                                showLoadingDialog();
                                runOCR(rotateImage(image, false),
                                        true, false);
                            } else {
                                //fail
                                onCameraButtonClicked();
                                showToast(getString(R.string.no_text_found_try_again));
                            }
                        } else {
                            showFindBox();
                        }
                    }
                })
                .addOnFailureListener(
                    new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            hideLoadingDialog();
                            Log.e(TAG, "onFailure: ", e);
                        }
                });
    }

    private void showLoadingDialog(){
        if (spotsDialog != null) {
            hideLoadingDialog();
        }

        spotsDialog = new SpotsDialog.Builder().setContext(this)
                .setMessage(getString(R.string.please_wait))
                .setCancelable(false)
                .build();
        spotsDialog.show();
    }

    private void hideLoadingDialog() {
        if (spotsDialog != null) {
            spotsDialog.dismiss();
        }

        spotsDialog = null;
    }

    private FirebaseVisionImage rotateImage(final FirebaseVisionImage image, final boolean rotateLeft) {
        float angle = 270;
        if (rotateLeft) {
            angle = 90;
        }

        Bitmap bitmap = rotateBitmap(image.getBitmapForDebugging(), angle);
        return FirebaseVisionImage.fromBitmap(bitmap);
    }

    public Bitmap rotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0,
                source.getWidth(), source.getHeight(), matrix, true);
    }

    private void showToast(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, str, Toast.LENGTH_LONG)
                        .show();
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
