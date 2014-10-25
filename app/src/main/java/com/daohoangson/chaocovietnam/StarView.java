package com.daohoangson.chaocovietnam;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

public class StarView extends RelativeLayout implements View.OnClickListener {

    private static final float STAR_OUTTER_LENGTH = 1.0f;
    private static final float STAR_INNER_LENGTH = 0.38f;
    private Paint paintStar;
    private int heightLyric;

    private OnStarInteraction mListener;

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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public StarView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        setupStarView();
    }

    public void setListener(OnStarInteraction listener) {
        mListener = listener;
    }

    protected void setupStarView() {
        paintStar = new Paint();
        paintStar.setColor(getResources().getColor(R.color.ccvn_star));
        paintStar.setStyle(Paint.Style.FILL);
        paintStar.setFlags(Paint.ANTI_ALIAS_FLAG);

        heightLyric = getResources().getDimensionPixelSize(R.dimen.ccvn_lyric);

        setOnClickListener(this);
    }

    protected void onDraw(Canvas canvas) {
        Path path = new Path();

        final float canvasWidth = canvas.getWidth();
        final float canvasHeight = canvas.getHeight() - heightLyric;

        final float starRadius = Math.min(canvasWidth, canvasHeight) * 0.3f;
        final float starHeight = (1 + Math.abs((float) Math.cos(324f / 180 * Math.PI)))
                * STAR_OUTTER_LENGTH * starRadius;

        final float pivotX = canvasWidth / 2.0f;
        final float pivotY = canvasHeight / 2.0f;
        float xFirst = 0;
        float yFirst = 0;

        float x, y, radian;
        for (int i = 0; i < 360; i += 36) {
            radian = (float) ((1.0 * i + 180) / 180 * Math.PI);

            if (i % 72 == 0) {
                x = (float) (pivotX + Math.sin(radian) * STAR_OUTTER_LENGTH * starRadius);
                y = (float) (pivotY + Math.cos(radian) * STAR_OUTTER_LENGTH * starRadius);
            } else {
                x = (float) (pivotX + Math.sin(radian) * STAR_INNER_LENGTH * starRadius);
                y = (float) (pivotY + Math.cos(radian) * STAR_INNER_LENGTH * starRadius);
            }

            if (i == 0) {
                path.moveTo(x, y);
                xFirst = x;
                yFirst = y;
            } else {
                path.lineTo(x, y);
            }
        }
        path.lineTo(xFirst, yFirst);

        canvas.drawPath(path, paintStar);
    }

    @Override
    public void onClick(View view) {
        if (mListener != null) {
            mListener.onStarClick();
        }
    }

    public interface OnStarInteraction {
        public void onStarClick();
    }
}
