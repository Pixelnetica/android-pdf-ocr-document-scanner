package com.pixelnetica.easyscan;

import com.pixelnetica.easyscan.widget.CutoutDrawable;
import com.pixelnetica.easyscan.widget.TransformedDrawable;
import com.pixelnetica.imagesdk.Corners;
import com.pixelnetica.imagesdk.DocumentCutout;
import com.pixelnetica.imagesdk.MetaImage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import java.lang.ref.WeakReference;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

public class CropImageView extends AppCompatImageView {

	// Crop points inside original image
	private CropData mCropData;

	// Store corners to revert
	private CropData mInitialCropData;

	private final CutoutDrawable mCutoutDrawable;
	private final Drawable.Callback mCutoutCallback = new Drawable.Callback() {
		@Override
		public void invalidateDrawable(@NonNull Drawable who) {
			invalidate();
		}

		@Override
		public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {

		}

		@Override
		public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {

		}
	};

	private static int maxTextureSize = -1;

	private static int getMaximumTextureSize()
	{
		EGL10 egl = (EGL10) EGLContext.getEGL();
		EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

		// Initialise
		int[] version = new int[2];
		egl.eglInitialize(display, version);

		// Query total number of configurations
		int[] totalConfigurations = new int[1];
		egl.eglGetConfigs(display, null, 0, totalConfigurations);

		// Query actual list configurations
		EGLConfig[] configurationsList = new EGLConfig[totalConfigurations[0]];
		egl.eglGetConfigs(display, configurationsList, totalConfigurations[0], totalConfigurations);

		int[] textureSize = new int[1];
		int maximumTextureSize = 0;

		// Iterate through all the configurations to located the maximum texture size
		for (int i = 0; i < totalConfigurations[0]; i++)
		{
			// Only need to check for width since opengl textures are always squared
			egl.eglGetConfigAttrib(display, configurationsList[i], EGL10.EGL_MAX_PBUFFER_WIDTH, textureSize);

			// Keep track of the maximum texture size
			if (maximumTextureSize < textureSize[0])
			{
				maximumTextureSize = textureSize[0];
			}

			Log.i(AppLog.TAG, Integer.toString(textureSize[0]));
		}

		// Release
		egl.eglTerminate(display);

		//maximumTextureSize = 1024;
		Log.i(AppLog.TAG, "Maximum GL texture size: " + Integer.toString(maximumTextureSize));
		return maximumTextureSize;
	}


	private PointF mScaledSize;
	private DisplayTask mDisplayTask;

	// Async load
	private static class DisplayTask extends AsyncTask<Void, Void, Bitmap> {
		final WeakReference<CropImageView> mImageViewRef;
		final Bitmap mBitmap;
		final Matrix mMatrix;
		final CropData mCropData;

		DisplayTask(@NonNull CropImageView imageView, @NonNull Bitmap bitmap, @NonNull Matrix matrix, @NonNull CropData cropData) {
			this.mImageViewRef = new WeakReference<>(imageView);
			this.mBitmap = bitmap;
			this.mMatrix = matrix;
			this.mCropData = cropData;
		}
		@Override
		protected Bitmap doInBackground(Void... voids) {

			// Check texture size
			if (maxTextureSize == -1) {
				maxTextureSize = getMaximumTextureSize();
			}

			// Scale image to texture
			if (mBitmap.isRecycled()) {
				return null;
			}
			final Point sourceSize = new Point(mBitmap.getWidth(), mBitmap.getHeight());
			final Point targetSize = new Point(sourceSize);
			if (maxTextureSize > 0) {
				if (sourceSize.x > sourceSize.y) {
					if (sourceSize.x > maxTextureSize) {
						targetSize.x = maxTextureSize;
						targetSize.y = maxTextureSize * sourceSize.y / sourceSize.x;
					}
				} else if (sourceSize.y > maxTextureSize) {
					targetSize.y = maxTextureSize;
					targetSize.x = maxTextureSize * sourceSize.x / sourceSize.y;
				}
			}

			if (!sourceSize.equals(targetSize)) {
				try {
					return Bitmap.createScaledBitmap(mBitmap, targetSize.x, targetSize.y, true);
				} catch (RuntimeException e) {
					return null;
				}
			} else {
				return mBitmap;
			}
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			final CropImageView imageView = mImageViewRef.get();
			if (imageView != null && imageView.mDisplayTask == this) {
				imageView.mDisplayTask = null;

				if (bitmap != null) {
					final TransformedDrawable drawable = new TransformedDrawable(new BitmapDrawable(imageView.getContext().getResources(), bitmap), mMatrix);
					imageView.mScaledSize = new PointF(bitmap.getWidth(), bitmap.getHeight());
					imageView.setImageDrawable(drawable);
					imageView.setCropData(mCropData);
				} else {
					imageView.mScaledSize = null;
					imageView.setImageDrawable(null);
					imageView.setCropData(null);
				}
			}
		}
	}

	// Initial tap
	private PointF initPoint;
	private PointF deltaMove;

	// Current drag corner
	private PointF movingItem;

	// Sdk Factory
	SdkFactory mSdkFactory;
	DocumentCutout mDocumentCutout;

	public CropImageView(Context context) {
		super(context);
		mCutoutDrawable = new CutoutDrawable(context);
		init();
	}

	public CropImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mCutoutDrawable = new CutoutDrawable(context);
		init();
	}

	public CropImageView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mCutoutDrawable = new CutoutDrawable(context);
		init();
	}

	private void init() {
		// NOTE: Drawable callback is weak reference. We need to have strong reference
		mCutoutDrawable.setCallback(mCutoutCallback);
	}

	public void setSdkFactory(SdkFactory mSdkFactory) {
		this.mSdkFactory = mSdkFactory;
		this.mDocumentCutout = mSdkFactory.createDocumentCutout();
	}

	private void setBaseMatrix(@NonNull Matrix matrix) {
		final Drawable d = getDrawable();
		if (d instanceof TransformedDrawable) {
			TransformedDrawable t = (TransformedDrawable) d;
			t.setTransform(matrix);
			if (android.os.Build.VERSION.SDK_INT >= 23) {
				// Why don't work on old API
				invalidateDrawable(d);
				update();
			} else {
				// Workaround old Android
				setImageDrawable(null);
				setImageDrawable(d);
			}
		}
	}

	private boolean isZoomValid() {
		return true;
	}

	private PointF getDisplayPoint(@NonNull PointF picturePoint) {
		final float [] pts = {picturePoint.x, picturePoint.y};
		getImageMatrix().mapPoints(pts);
		return new PointF(pts[0], pts[1]);
	}

	private RectF getDisplayBounds() {
		final RectF rf = new RectF(getDrawable().getBounds());
		getImageMatrix().mapRect(rf);
		return rf;
	}

	private PointF getPicturePoint(@NonNull PointF displayPoint) {
		final Matrix inverse = new Matrix();
		getImageMatrix().invert(inverse);

		final float [] pts = {displayPoint.x, displayPoint.y};
		inverse.mapPoints(pts);
		return new PointF(pts[0], pts[1]);
	}

	public void setCropImage(Bitmap bitmap, Matrix matrix, CropData cropData) {
		if (bitmap != null) {
			// Async call
			mDisplayTask = new DisplayTask(this, bitmap, matrix, cropData);
			mDisplayTask.execute();
		} else {
			mDisplayTask = null;
			mScaledSize = null;
			setImageDrawable(null);
			setCropData(null);
		}
	}

	private void setCropData(CropData cropData) {
		this.mCropData = cropData;
		if (cropData != null) {
			mInitialCropData = new CropData(this.mCropData);
			// NOTE: update() inside
			setBaseMatrix(cropData.getMatrix());
		} else {
			mInitialCropData = null;
			update();
		}
	}

	public CropData getCropData() {
		return mCropData;
	}

	public void setScaleZoom(float scale, PointF screenPoint) {
		if (scale <= 0) {
			setScaleType(ScaleType.FIT_CENTER);
			scrollTo(0, 0);
			//invalidate();
		} else {

			// Define display
			final RectF displayBounds = new RectF(0, 0, getWidth(), getHeight());
			displayBounds.left += getPaddingLeft();
			displayBounds.top += getPaddingTop();
			displayBounds.right -= getPaddingRight();
			displayBounds.bottom -= getPaddingBottom();

			// Set focus to view center
			if (screenPoint == null) {
				screenPoint = new PointF(displayBounds.centerX(), displayBounds.centerY());
			}

			Matrix imageMatrix = getImageMatrix();

			// Translate screen point to image
			Matrix inverse = new Matrix();
			imageMatrix.invert(inverse);

			final float pts [] = {screenPoint.x + getScrollX(), screenPoint.y + getScrollY()};
			inverse.mapPoints(pts);
			//PointF pictureFocus = new PointF(pts[0], pts[1]);

			// Create scale matrix
			Matrix matrix = new Matrix();
			matrix.setScale(scale, scale);

			// Snap to origin
			RectF bounds = new RectF(getDrawable().getBounds());
			matrix.mapRect(bounds);
			matrix.postTranslate(-bounds.left, -bounds.top);

			// Scroll to new point
			matrix.mapPoints(pts);

			//matrix.postTranslate(screenPoint.x - pts[0], screenPoint.y - pts[1]);
			setScaleType(ScaleType.MATRIX);
			setImageMatrix(matrix);
			scrollTo(Math.round(pts[0] - screenPoint.x), Math.round(pts[1] - screenPoint.y));
		}
	}

	public void rotateRight() {
		if (mCropData != null) {
			int orientation = mCropData.getOrientation();
			switch (orientation) {
				case MetaImage.ExifUndefined:
				case MetaImage.ExifNormal:
					orientation = MetaImage.ExifRotate90;
					break;
				case MetaImage.ExifRotate90:
					orientation = MetaImage.ExifRotate180;
					break;
				case MetaImage.ExifRotate180:
					orientation = MetaImage.ExifRotate270;
					break;
				case MetaImage.ExifRotate270:
					orientation = MetaImage.ExifNormal;
					break;
				case MetaImage.ExifFlipHorizontal:
					orientation = MetaImage.ExifTranspose;
					break;

				case MetaImage.ExifTranspose:
					orientation = MetaImage.ExifFlipVertical;
					break;

				case MetaImage.ExifFlipVertical:
					orientation = MetaImage.ExifTransverse;
					break;

				case MetaImage.ExifTransverse:
					orientation = MetaImage.ExifFlipHorizontal;
					break;

				default:
					throw new IllegalStateException("Unknown orientation " + orientation);
			}

			mCropData.setOrientation(orientation);
			setBaseMatrix(mCropData.getMatrix());
		}
	}

	public void rotateLeft() {
		if (mCropData != null) {
			@MetaImage.ExifOrientation int orientation = mCropData.getOrientation();
			switch (orientation) {
				case MetaImage.ExifUndefined:
				case MetaImage.ExifNormal:
					orientation = MetaImage.ExifRotate270;
					break;

				case MetaImage.ExifRotate270:
					orientation = MetaImage.ExifRotate180;
					break;

				case MetaImage.ExifRotate180:
					orientation = MetaImage.ExifRotate90;
					break;

				case MetaImage.ExifRotate90:
					orientation = MetaImage.ExifNormal;
					break;

				case MetaImage.ExifFlipHorizontal:
					orientation = MetaImage.ExifTransverse;
					break;

				case MetaImage.ExifTransverse:
					orientation = MetaImage.ExifFlipVertical;
					break;

				case MetaImage.ExifFlipVertical:
					orientation = MetaImage.ExifTranspose;
					break;

				case MetaImage.ExifTranspose:
					orientation = MetaImage.ExifFlipHorizontal;
					break;

				default:
					throw new IllegalStateException("Unknown orientation " + orientation);
			}

			mCropData.setOrientation(orientation);
			setBaseMatrix(mCropData.getMatrix());
			//update();
		}
	}

	public void revertSelection() {
		if (mCropData != null) {
			// Translate initial corners to current orientation
			CropData cd = new CropData(mInitialCropData);
			cd.setOrientation(mCropData.getOrientation());

			mCropData.setCorners(cd.points);
			update();
		}
	}

	public void expandSelection() {
		if (mCropData != null) {
			mCropData.expand();
			update();
		}
	}

	@Override
	public void setImageDrawable(@Nullable Drawable drawable) {

		// Callback to cutout
		if (drawable instanceof TransformedDrawable) {
			TransformedDrawable t = (TransformedDrawable) drawable;
			t.setTransformCallback(new TransformedDrawable.TransformCallback() {
				@Override
				public void onDrawableBoundsChange(Rect bounds) {
					// Really a trash value
					mCutoutDrawable.setBounds(bounds);
				}

				@Override
				public boolean onDrawableStateChange(int[] state) {
					return mCutoutDrawable.setState(state);
				}
			});
		}

		super.setImageDrawable(drawable);
		setScaleZoom(0, null);

		// NOTE: Update in onLayout()
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		update();
	}

	private boolean hasCorners() {
		return mCropData != null && mCropData.hasCorners;
	}

	private boolean isMoving() {
		return movingItem != null && deltaMove != null;
	}

	private PointF[] buildCutout() {
		if (!hasCorners()) {
			return null;
		}

		// Store mark of moving item
		PointF [] oldCutout = mCutoutDrawable.getCutout();
		final Object movingMark = mCropData.markCutout(oldCutout, movingItem);
		if (movingItem != null && movingMark == null) {
			Log.d(AppLog.TAG, "Undefined moving mark");
		}
		movingItem = null;

		PointF [] corners = mCropData.mapCutout(getImageMatrix(), mScaledSize);
		if (corners == null || corners.length != 4) {
			throw new IllegalStateException("Invalid corners to build cutout");
		}

		// Reassign 'movingItem' from old cutout to new
		movingItem = mCropData.findMark(corners, movingMark);
		// Move current item
		if (isMoving()) {
			movingItem.offset(deltaMove.x, deltaMove.y);
		}

		if (deltaMove != null && movingItem == null) {
			Log.d(AppLog.TAG, "Undefined movingItem");
		}

		return corners;
	}

	private RectF buildBounds() {
		if (!hasCorners()) {
			return null;
		}

		return mCropData.mapOrigin(getImageMatrix(), mScaledSize);
	}

	private boolean validateCutout(PointF [] cutout, RectF bounds) {
		if (cutout == null || mDocumentCutout == null) {
			// Don't validate
			return true;
		}

		// Build display corners
		final Corners corners = new Corners();
		final int [] indices = new int [] {0, 1, 3, 2};
		for (int i = 0; i < indices.length; ++i) {
			PointF p = cutout[i];
			corners.points[indices[i]].set(Math.round(p.x-bounds.left), Math.round(p.y - bounds.top));
		}

		Rect rc = new Rect();
		bounds.roundOut(rc);
		return DocumentCutout.validateCorners(corners, new Point(rc.width(), rc.height()));
	}

	// Prepare all members to draw
	private void update() {
		final PointF[] cutout = buildCutout();
		mCutoutDrawable.setupCutout(cutout, movingItem, !validateCutout(cutout, buildBounds()));
		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		mCutoutDrawable.draw(canvas);
	}

	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		mCutoutDrawable.setState(getDrawableState());
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (super.onTouchEvent(event)) {
			return true;
		}

		// Skip any interactivity when disabled
		if (!isEnabled()) {
			return false;
		}

		switch (event.getActionMasked()) {
		case MotionEvent.ACTION_DOWN:
			// Store initial point
			initPoint = new PointF(event.getX(), event.getY());

			if (mCropData != null && mCropData.hasCorners) {
				// Detect active corner to move
				// Select closest corner
				final PointF checkPoint = new PointF(initPoint.x + getScrollX(), initPoint.y + getScrollY());
				movingItem = mCutoutDrawable.findCutoutItem(checkPoint);
				if (movingItem != null) {
					PointF focus = new PointF();    // create a copy
					focus.set(movingItem);
					focus.offset(-getScrollX(), -getScrollY());
					setScaleZoom(1.0f, focus);
					update();
				}
			} else {
				update();
			}

			// Notify listeners
			performClick();
			break;

		case MotionEvent.ACTION_MOVE:
			deltaMove = new PointF(event.getX() - initPoint.x, event.getY() - initPoint.y);
			update();
			break;

		case MotionEvent.ACTION_UP:
			// Store corner position
			if (isMoving()) {
				// Calculate coordinates in display bitmap
				PointF picturePoint = getPicturePoint(movingItem);

				// Calculate coordinates in source bitmap
				mCropData.setMappedItem(movingItem, picturePoint, mScaledSize);
			}
			// NOTE: No break! It is not an error.

		case MotionEvent.ACTION_CANCEL:
			initPoint = null;
			deltaMove = null;
			movingItem = null;

			setScaleZoom(0, null);
			update();
			break;
		}

		return true;
	}

	@Override
	public boolean performClick() {
		return super.performClick();
	}
}
