package com.daohoangson.chaocovietnam.widget;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

import com.daohoangson.chaocovietnam.R;
import com.daohoangson.chaocovietnam.adapter.Config;

public class Display extends View {

    private Config mConfig;
    final private Paint mPaint = new Paint();

    public Display(Context context) {
        super(context);

        setupView();
    }

    public Display(Context context, AttributeSet attrs) {
        super(context, attrs);

        setupView();
    }

    public Display(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setupView();
    }

    @SuppressWarnings("UnusedDeclaration")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Display(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        setupView();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int minW = getResources().getDimensionPixelSize(R.dimen.ccvn_display_width);
        final int minH = getResources().getDimensionPixelSize(R.dimen.ccvn_display_height);

        final int w = ViewCompat.resolveSizeAndState(minW, widthMeasureSpec, 1);
        final int h = ViewCompat.resolveSizeAndState(minH, heightMeasureSpec, 0);

        setMeasuredDimension(w, h);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mConfig != null) {
            final int canvasW = canvas.getWidth();
            final int canvasH = canvas.getHeight();
            final int maxW = mConfig.getMaxDisplayWidth();
            final int maxH = mConfig.getMaxDisplayHeight();
            final float maxRatio = maxH > 0 ? (maxW * 1.0f / maxH) : 1;
            final float canvasRatio = canvasH > 0 ? canvasW * 1.0f / canvasH : 1;
            final int scaledW, scaledH;
            if (maxRatio > canvasRatio) {
                scaledW = canvasW;
                scaledH = (int) (scaledW / maxRatio);
            } else {
                scaledH = canvasH;
                scaledW = (int) (scaledH * maxRatio);
            }
            final float fw = maxW > 0 ? (mConfig.getDisplayWidth() * 1.0f / maxW) : 0;
            final float fh = maxH > 0 ? (mConfig.getDisplayHeight() * 1.0f / maxH) : 0;
            final int w = (int) (fw * scaledW);
            final int h = (int) (fh * scaledH);
            final int x = (canvasW - w) / 2;
            final int y = (canvasH - h) / 2;

            canvas.drawRect(x, y, x + w, y + h, mPaint);
        }
    }

    public void setConfig(Config config) {
        mConfig = config;
    }

    private void setupView() {
        mPaint.setColor(getResources().getColor(R.color.ccvn_star));
        mPaint.setStyle(Paint.Style.FILL);
    }
}
