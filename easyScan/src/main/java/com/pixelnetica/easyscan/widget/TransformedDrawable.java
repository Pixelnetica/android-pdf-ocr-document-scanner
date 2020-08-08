package com.pixelnetica.easyscan.widget;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class TransformedDrawable extends Drawable {

	public interface TransformCallback {
		void onDrawableBoundsChange(Rect bounds);
		boolean onDrawableStateChange(int [] state);
	}

	@NonNull
	private Drawable mInnerDrawable;
	@NonNull
	private final Matrix mInitTransform = new Matrix();
	@NonNull
	private final Matrix mDrawMatrix = new Matrix();
	@NonNull
	private final Point mDrawingSize = new Point();

	private TransformCallback mCallback;

	public TransformedDrawable() {

	}

	public TransformedDrawable(@NonNull Drawable drawable) {
		this.mInnerDrawable = drawable;
		setupTransform();
	}

	public TransformedDrawable(@NonNull Drawable drawable, @NonNull Matrix transform) {
		this.mInnerDrawable = drawable;
		this.mInitTransform.set(transform);
		setupTransform();
	}

	public void setInnerDrawable(@NonNull Drawable innerDrawable) {
		this.mInnerDrawable = innerDrawable;
		setupTransform();
	}

	public Drawable getInnerDrawable() {
		return mInnerDrawable;
	}

	public void setTransform(@NonNull Matrix matrix) {
		mInitTransform.set(matrix);
		setupTransform();
	}

	public void setTransformCallback(TransformCallback callback) {
		this.mCallback = callback;
	}

	private void setupTransform() {
		final int width =  mInnerDrawable.getIntrinsicWidth();
		final int height = mInnerDrawable.getIntrinsicHeight();
		if (width < 0 || height < 0) {
			// Disable transform for unknown inner size
			mDrawMatrix.set(new Matrix());
			mDrawingSize.set(width, height);
		} else {
			mDrawMatrix.set(mInitTransform);

			// Snap inner drawable to [0, 0]
			final RectF rf = new RectF(0, 0, width, height);
			mDrawMatrix.mapRect(rf);
			mDrawMatrix.postTranslate(-rf.left, -rf.top);

			final Rect rc = new Rect();
			rf.roundOut(rc);
			mDrawingSize.set(rc.width(), rc.height());
		}
		invalidateSelf();
	}

	@Override
	public void draw(@NonNull Canvas canvas) {
		final int saveCount = canvas.getSaveCount();
		canvas.save();
		canvas.concat(mDrawMatrix);
		try {
			mInnerDrawable.draw(canvas);
		} catch (RuntimeException e) {
			// Rare case to draw recycled bitmap
		}
		canvas.restoreToCount(saveCount);
	}

	@Override
	public void setAlpha(int alpha) {
		mInnerDrawable.setAlpha(alpha);
	}

	@Override
	public void setColorFilter(@Nullable ColorFilter colorFilter) {
		mInnerDrawable.setColorFilter(colorFilter);
	}

	@Override
	public int getOpacity() {
		return mInnerDrawable.getOpacity();
	}

	@Override
	public int getIntrinsicWidth() {
		return mDrawingSize.x;
	}

	@Override
	public int getIntrinsicHeight() {
		return mDrawingSize.y;
	}

	@Override
	protected void onBoundsChange(Rect bounds) {
		Matrix inverted = new Matrix();
		mDrawMatrix.invert(inverted);

		RectF rf = new RectF(bounds);
		inverted.mapRect(rf);

		Rect rc = new Rect();
		rf.roundOut(rc);
		mInnerDrawable.setBounds(rc);

		if (mCallback != null) {
			mCallback.onDrawableBoundsChange(bounds);
		}
		invalidateSelf();
	}

	@Override
	public boolean isStateful() {
		// Pass state changing to callback
		return mCallback != null;
	}

	@Override
	protected boolean onStateChange(int[] state) {
		if (mCallback != null) {
			return mCallback.onDrawableStateChange(state);
		} else {
			return super.onStateChange(state);
		}
	}
}
