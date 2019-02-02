package com.hasbis.findonpage.features.ocr;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


@IntDef({
        OCRActions.PROCESS_STARTED,
        OCRActions.PROCESS_FINISHED,
        OCRActions.PROCESS_FAIL,
        OCRActions.PROCESS_TEXT_NOT_FOUND,
})
@Retention(RetentionPolicy.SOURCE)
public @interface OCRActions {
    int PROCESS_STARTED = 0;
    int PROCESS_FINISHED = 1;
    int PROCESS_FAIL = 2;
    int PROCESS_TEXT_NOT_FOUND = 3;
}
