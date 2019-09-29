package com.hasbis.findonpage.features.ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.IOException;
import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.subjects.PublishSubject;

public class ImageToText {
    private static final String TAG = "ImageToText";

    private Context context;
    private FirebaseVisionText firebaseVisionText = null;
    private int mlImageHeight = -1;
    private int mlImageWidth = -1;

    PublishSubject<Integer> actionSubject = PublishSubject.create();
    PublishSubject<OCRResult> resultSubject = PublishSubject.create();

    public Observable<Integer> getActionObserveable() {
        return actionSubject;
    }

    public Observable<OCRResult> getResultObserveable() {
        return resultSubject;
    }

    public ImageToText(Context context) {
        this.context = context;
    }

    public void startOCR(Uri uri) {
        FirebaseVisionImage image;
        try {
            image = FirebaseVisionImage.fromFilePath(context, uri);
            mlImageWidth = image.getBitmap().getWidth();
            mlImageHeight = image.getBitmap().getHeight();
        } catch (IOException e) {
            Log.e(TAG, "findText: ", e);
            return;
        }

        sendAction(OCRActions.PROCESS_STARTED);
        runOCR(image);
    }

    public void markText(String text) {
        if (firebaseVisionText == null) {
            sendAction(OCRActions.PROCESS_TEXT_NOT_FOUND);
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
                        rects.add(elementFrame);
                    }

                }
            }
        }

        sendResult(rects);
    }

    public String getAllText() {
        return (firebaseVisionText == null) ? null : firebaseVisionText.getText();
    }

    private void sendAction(@OCRActions int action) {
        actionSubject.onNext(action);
    }

    private void sendResult(ArrayList<Rect> rects) {
        resultSubject.onNext(new OCRResult(rects, mlImageWidth, mlImageHeight));
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
                        firebaseVisionText = local_firebaseVisionText;
                        if (firebaseVisionText == null ||
                                TextUtils.isEmpty(firebaseVisionText.getText())) {
                            if (!rotatedLeft) {
                                // rotate left
                                runOCR(rotateImage(image, true),
                                        true, false);
                            } else if (rotatedLeft && !rotatedRight) {
                                // rotate right
                                runOCR(rotateImage(image, false),
                                        true, true);
                            } else {
                                //fail
                                sendAction(OCRActions.PROCESS_FAIL);
                            }
                        } else {
                            sendAction(OCRActions.PROCESS_FINISHED);
                        }
                    }
                })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                sendAction(OCRActions.PROCESS_FAIL);
                                Log.e(TAG, "onFailure: ", e);
                            }
                        });
    }

    private FirebaseVisionImage rotateImage(final FirebaseVisionImage image, final boolean rotateLeft) {
        float angle = 270;
        if (rotateLeft) {
            angle = 90;
        }

        Bitmap bitmap = rotateBitmap(image.getBitmap(), angle);
        return FirebaseVisionImage.fromBitmap(bitmap);
    }

    private Bitmap rotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0,
                source.getWidth(), source.getHeight(), matrix, true);
    }

    public class OCRResult {
        public ArrayList<Rect> rects;
        public int height;
        public int width;

        public OCRResult(ArrayList<Rect> rects,
                         int width,
                         int height) {
            this.rects = rects;
            this.height = height;
            this.width = width;
        }
    }

}