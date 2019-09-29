package com.hasbis.findonpage;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.hasbis.findonpage.features.ocr.ImageToText;
import com.hasbis.findonpage.features.ocr.OCRActions;
import com.hasbis.findonpage.features.views.DrawableImageView;
import com.hasbis.findonpage.utils.FirebaseAnalyticsUtils;
import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.ListHolder;
import com.orhanobut.dialogplus.OnItemClickListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import dmax.dialog.SpotsDialog;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int CAMERA_REQUEST = 1888;
    private static final int GALLERY_REQUEST = 2888;
    private static final int CAMERA_PERMISSION_CODE = 100;

    private DrawableImageView imageView;
    private LinearLayout findBox;
    private LinearLayout showFindBoxLayout;
    private Button photoButton;
    private Button galleryButton;
    private Button findButton;
    private Button selectAllButton;
    private EditText textToFind;

    private String mCameraFileName = "";
    String textResult = "";

    private AlertDialog spotsDialog = null;
    private FirebaseAnalyticsUtils analyticsUtils;
    private ImageToText ocr;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initAnalytic();
        initView();
        initButtons();
        initOCR();
        bindToOCR();
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
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_list_item_1, new String[]{getString(R.string.take_a_photo_from_camera),
                    getString(R.string.take_a_photo_from_gallery), getString(R.string.cancel)}
            );
            DialogPlus dialog = DialogPlus.newDialog(this)
                    .setAdapter(adapter)
                    .setOnItemClickListener(new OnItemClickListener() {
                        @Override
                        public void onItemClick(DialogPlus dialog, Object item, View view, int position) {
                        }
                    })
                    .setContentHolder(new ListHolder())
                    .setGravity(Gravity.CENTER)
                    .setExpanded(true)
                    .setHeader(R.layout.dialog_header)
                    .setOnItemClickListener(new OnItemClickListener() {
                        @Override
                        public void onItemClick(DialogPlus dialog, Object item, View view, int position) {
                            if (position == 0) {
                                analyticsUtils.onNewPhoto();
                                onCameraButtonClicked();
                            } else if (position == 1) {
                                analyticsUtils.onNewGalleryPhoto();
                                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                                photoPickerIntent.setType("image/*");
                                startActivityForResult(photoPickerIntent, GALLERY_REQUEST);
                            }
                            dialog.dismiss();
                        }
                    })
                    .create();
            dialog.show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onCameraButtonClicked();
            } else {
                showToast(getString(R.string.permission_denied));
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CAMERA_REQUEST) {
                updateUIForImageGained();
                if (mCameraFileName != null) {
                    Uri image = Uri.fromFile(new File(mCameraFileName));
                    imageView.setImageURI(image);
                    imageView.setVisibility(View.VISIBLE);
                    startOCR(image);
                }
            } else if (requestCode == GALLERY_REQUEST) {
                updateUIForImageGained();
                final Uri image = data.getData();
                imageView.setImageURI(image);
                imageView.setVisibility(View.VISIBLE);
                startOCR(image);
            }
        }
    }

    private void updateUIForImageGained() {
        imageView.setRects(new ArrayList<Rect>());
        findBox.setVisibility(View.VISIBLE);
        photoButton.setVisibility(View.GONE);
        galleryButton.setVisibility(View.GONE);
    }

    private void initAnalytic() {
        analyticsUtils = FirebaseAnalyticsUtils.getInstance();
        analyticsUtils.init(this);
    }

    private void initView() {
        this.imageView = this.findViewById(R.id.preview_imageview);
        this.findBox = this.findViewById(R.id.find_layout);
        this.showFindBoxLayout = this.findViewById(R.id.show_find_box_layout);
        this.photoButton = this.findViewById(R.id.camera_button);
        this.galleryButton = this.findViewById(R.id.gallery_button);
        this.findButton = this.findViewById(R.id.find_button);
        this.selectAllButton = this.findViewById(R.id.select_all_button);
        this.textToFind = this.findViewById(R.id.word_edittext);
    }

    private void initButtons() {
        photoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                analyticsUtils.onNewPhoto();
                onCameraButtonClicked();
            }
        });

        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                analyticsUtils.onNewGalleryPhoto();
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, GALLERY_REQUEST);
            }
        });

        selectAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(textResult)) {
                    analyticsUtils.onSelectAll();
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

                analyticsUtils.onNewSearch();
                markText(textToFind.getText().toString());
                hideKeyboard();
                hideFindBox();
            }
        });

        showFindBoxLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                analyticsUtils.onShowFindBoxClicked();
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

    private void initOCR() {
        ocr = new ImageToText(this);
    }

    private void bindToOCR() {
        ocr.getActionObserveable()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {
                    compositeDisposable.add(d);
                }

                @Override
                public void onNext(Integer action) {
                    switch (action) {
                        case OCRActions.PROCESS_STARTED:
                            showLoadingDialog();
                            hideFindBox();
                            break;
                        case OCRActions.PROCESS_FINISHED:
                            textResult = ocr.getAllText();
                            hideLoadingDialog();
                            showFindBox();
                            break;
                        case OCRActions.PROCESS_TEXT_NOT_FOUND:
                            hideLoadingDialog();
                            showToast(getString(R.string.no_text_found_try_again));
                            onCameraButtonClicked();
                            break;
                        case OCRActions.PROCESS_FAIL:
                            hideLoadingDialog();
                            showToast(getString(R.string.try_again));
                            onCameraButtonClicked();
                            break;
                    }
                }

                @Override
                public void onError(Throwable e) {
                    Log.e(TAG, "onError: ", e);
                }

                @Override
                public void onComplete() {

                }
            });

        ocr.getResultObserveable()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map(new Function<ImageToText.OCRResult, ArrayList<Rect>>() {
                @Override
                public ArrayList<Rect> apply(ImageToText.OCRResult result) throws Exception {
                    ArrayList<Rect> convertedRects = new ArrayList<>();
                    for (Rect rect: result.rects) {
                        convertedRects.add(convertRect(rect, result.width, result.height));
                    }
                    return convertedRects;
                }
            })
            .subscribe(new Observer<ArrayList<Rect>>() {
                @Override
                public void onSubscribe(Disposable d) {
                    compositeDisposable.add(d);
                }

                @Override
                public void onNext(ArrayList<Rect> convertedRects) {
                    if (convertedRects.size() == 0) {
                        showFindBox();
                    }

                    showResultCount(convertedRects.size());
                    imageView.setRects(convertedRects);
                }

                @Override
                public void onError(Throwable e) {
                    Log.e(TAG, "onError: ", e);
                }

                @Override
                public void onComplete() {

                }
            });
    }

    private void markText(String str) {
        ocr.markText(str);
    }

    private void startOCR(Uri image) {
        ocr.startOCR(image);
    }

    private void showSelectAllDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_all);

        final EditText input = new EditText(this);
        input.setText(textResult);
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

        showToast(str);
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

    private void showToast(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, str, Toast.LENGTH_LONG)
                        .show();
            }
        });
    }

    private Rect convertRect(Rect rect, float mlImageWidth, float mlImageHeight) {
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
