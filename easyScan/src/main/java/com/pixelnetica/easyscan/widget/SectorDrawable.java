package com.pixelnetica.easyscan.widget;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class SectorDrawable extends Drawable {

	private final PointF mCenter;
	private final float mRadius;
	private final float mStartAngle;
	private final float mFinishAngle;
	private final Paint mPaint;

	public SectorDrawable(@NonNull PointF center, float radius, float startAngle, float finishAngle, Paint paint) {
		this.mCenter = center;
		this.mRadius = radius;
		this.mStartAngle = startAngle;
		this.mFinishAngle = finishAngle;
		this.mPaint = new Paint(paint);

		// Set drawable bounds
		Rect bounds = new Rect();
		describedRect().roundOut(bounds);
		setBounds(bounds);
	}

	@Override
	public void draw(@NonNull Canvas canvas) {
		float sweepAngle = mFinishAngle - mStartAngle;
		if (sweepAngle < 0)
		{
			sweepAngle += 360f;
		}
		canvas.drawArc(describedRect(), mStartAngle, sweepAngle, true, mPaint);
	}

	@NonNull
	private RectF describedRect()
	{
		return new RectF(
				mCenter.x - mRadius,
				mCenter.y - mRadius,
				mCenter.x + mRadius,
				mCenter.y + mRadius);
	}


	@Override
	public void setAlpha(int alpha) {
		mPaint.setAlpha(alpha);
	}

	@Override
	public void setColorFilter(@Nullable ColorFilter colorFilter) {
		mPaint.setColorFilter(colorFilter);
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}
}
