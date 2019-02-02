package com.hasbis.findonpage.features.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ImageView;

import java.util.ArrayList;

public class DrawableImageView extends ImageView {

    ArrayList<Rect> rects;
    Paint paint = new Paint();

    public DrawableImageView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.setWillNotDraw(false);
        rects = new ArrayList<>();
    }

    public DrawableImageView(Context context) {
        super(context);
        this.setWillNotDraw(false);
        rects = new ArrayList<>();
    }

    public void setRects(ArrayList<Rect> rects) {
        this.rects = rects;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Initialize a new Paint instance to draw the Rectangle
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.YELLOW);
        paint.setAntiAlias(true);
        paint.setAlpha(125);

        // Finally, draw the rectangle on the canvas
        for (Rect rec : rects) {
            canvas.drawRect(rec, paint);
        }
    }

}