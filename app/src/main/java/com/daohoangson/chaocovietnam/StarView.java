package com.daohoangson.chaocovietnam;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class StarView extends RelativeLayout {

    private static final float STAR_OUTER_LENGTH = 1.0f;
    private static final float STAR_INNER_LENGTH = 0.38f;

    private Path mPath;
    private Paint mPaint;
    private int mHeightLyric;

    public StarView(Context context) {
        super(context);

        setupStarView();
    }

    public StarView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setupStarView();
    }

    public StarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setupStarView();
    }

    @SuppressWarnings("UnusedDeclaration")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public StarView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        setupStarView();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mPath = new Path();

        //noinspection UnnecessaryLocalVariable
        final float canvasWidth = w;
        final float canvasHeight = h - mHeightLyric;

        final float starRadius = Math.min(canvasWidth, canvasHeight) * 0.3f;
        final float starBottomHeight = Math.abs((float) Math.cos(324f / 180 * Math.PI))
                * STAR_OUTER_LENGTH * starRadius;
        final float starHeight = STAR_OUTER_LENGTH * starRadius + starBottomHeight;

        final float pivotX = canvasWidth / 2.0f;
        final float pivotY = (canvasHeight - starHeight) / 2.0f + starBottomHeight;
        float xFirst = 0;
        float yFirst = 0;

        float x, y, radian;
        for (int i = 0; i < 360; i += 36) {
            radian = (float) ((1.0 * i + 180) / 180 * Math.PI);

            if (i % 72 == 0) {
                x = (float) (pivotX + Math.sin(radian) * STAR_OUTER_LENGTH * starRadius);
                y = (float) (pivotY + Math.cos(radian) * STAR_OUTER_LENGTH * starRadius);
            } else {
                x = (float) (pivotX + Math.sin(radian) * STAR_INNER_LENGTH * starRadius);
                y = (float) (pivotY + Math.cos(radian) * STAR_INNER_LENGTH * starRadius);
            }

            if (i == 0) {
                mPath.moveTo(x, y);
                xFirst = x;
                yFirst = y;
            } else {
                mPath.lineTo(x, y);
            }
        }
        mPath.lineTo(xFirst, yFirst);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawPath(mPath, mPaint);
    }

    private void setupStarView() {
        mPaint = new Paint();
        mPaint.setColor(getResources().getColor(R.color.ccvn_star));
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        mHeightLyric = getResources().getDimensionPixelSize(R.dimen.ccvn_lyric);
    }
}
