package com.daohoangson.chaocovietnam;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class StarView extends RelativeLayout {
	
	public StarView(Context context) {
		super(context);
		
		setupStarView();
	}

	public StarView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		setupStarView();
	}
	
	protected void setupStarView() {
		setBackgroundColor(Color.RED);
	}

	protected void onDraw(Canvas canvas) {
		Paint paint = new Paint();
		paint.setColor(Color.YELLOW);
		paint.setStyle(Paint.Style.FILL);
		
		Path path = new Path();
		float centerX = canvas.getWidth() / 2.0f;
		float centerY = canvas.getHeight() / 2.0f;
		float ratio = (canvas.getWidth() * 1.0f) / canvas.getHeight();
		float radius = (ratio < 1.0f) ? (canvas.getHeight() * 0.3f) : (canvas.getWidth() * 0.2f);
		float xFirst = 0, yFirst = 0;

		float x, y, radian;
		for (int i = 0; i < 360; i += 36) {
			radian = (float) ((1.0 * i + 180) / 180 * Math.PI);
			
			if (i % 72 == 0) {
				x = (float) (centerX + Math.sin(radian) * radius);
				y = (float) (centerY + Math.cos(radian) * radius);
			} else {
				x = (float) (centerX + Math.sin(radian) * (0.38f*radius));
				y = (float) (centerY + Math.cos(radian) * (0.38f*radius));
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
		
		canvas.drawPath(path, paint);
	}
}
