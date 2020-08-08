package com.pixelnetica.easyscan.camera;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import com.pixelnetica.easyscan.SdkFactory;
import com.pixelnetica.easyscan.R;
import com.pixelnetica.imagesdk.CutoutAverageF;

/**
 * Created by Denis on 28.02.2015.
 */
public class CameraOverlay extends View implements ICameraOverlay {

	private boolean mShowCorners;
	private boolean mAlertMode;

	private class Task implements Runnable {
		private final CutoutAverageF average;
		private PointF [] documentCorners;

		public Task(CutoutAverageF average) {
			this.average = average;
		}

		/**
		 * Starts executing the active part of the class' code. This method is
		 * called when a thread is started that has been created with a class which
		 * implements {@code Runnable}.
		 */
		@Override
		public void run() {
			average.duplicate();
			buildCorners(mShowCorners);
		}

		void reset() {
			average.reset(true);
		}

		private void updateCorners(PointF [] points, boolean animate) {
			average.append(points); // NOTE: null to reset
			buildCorners(animate);
		}

		void buildCorners(boolean animate) {
			documentCorners = average.average();
			if (animate) {
				postDelayed(this, 50);
			}

			invalidate();
		}

		void setPoints(PointF [] points, boolean animate) {
			removeCallbacks(this);
			updateCorners(points, animate);
		}

		// Drawing
		private final Path edgeLines = new Path();

		void drawEdges(Canvas canvas, Paint paint, int fade) {
			if (mShowCorners && documentCorners != null) {
				edgeLines.reset();
				// Top-left start point
				edgeLines.moveTo(documentCorners[0].x, documentCorners[0].y);
				// Top-right
				edgeLines.lineTo(documentCorners[1].x, documentCorners[1].y);
				// Bottom-RIGHT
				edgeLines.lineTo(documentCorners[3].x, documentCorners[3].y);
				// Bottom-left
				edgeLines.lineTo(documentCorners[2].x, documentCorners[2].y);
				// Return to start point
				edgeLines.lineTo(documentCorners[0].x, documentCorners[0].y);
				// finish
				edgeLines.close();

				if (fade != 0) {
					paint.setAlpha(average.fullness(fade));
				}

				canvas.drawPath(edgeLines, paint);
			}
		}

	}

	private SdkFactory mSdkFactory;

	// Current corners
	private Task mCornersTask;

	// Drawing
	private final Paint mCornersPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

	private float mFrameWidth = 5;
	private int mFrameColor = Color.WHITE;
	private int mBoundsColor = Color.GRAY;
	private int mAlertColor = Color.BLUE;

	private final Runnable mCancelAlert = new Runnable() {
		@Override
		public void run() {
			mAlertMode = false;
			invalidate();
		}
	};

	public CameraOverlay(Context context) {
		super(context);
		initPaint(null, 0, 0);
	}

	public CameraOverlay(Context context, AttributeSet attrs) {
		super(context, attrs);
		initPaint(attrs, 0, 0);
	}

	public CameraOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initPaint(attrs, defStyleAttr, 0);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public CameraOverlay(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		initPaint(attrs, defStyleAttr, defStyleRes);
	}

	@Override
	public void onDraw(Canvas canvas) {
		if (mCornersTask != null) {
			int color = (mAlertMode) ? mAlertColor : mFrameColor;
			mCornersPaint.setColor(color);

			mCornersTask.drawEdges(canvas, mCornersPaint, Color.alpha(color));
		}
	}

	@Override
	public void showCorners(boolean shown) {
		if (shown != mShowCorners) {
			mShowCorners = shown;
			if (mShowCorners && mCornersTask != null) {
				mCornersTask.reset();
			}
			invalidate();
		}
	}

	@Override
	public void showAlert(boolean alert, int delay) {
		removeCallbacks(mCancelAlert);
		mAlertMode = alert;
		invalidate();

		if (alert && delay > 0) {
			postDelayed(mCancelAlert, delay);
		}
	}

	@Override
	public void setDocumentCorners(PointF[] points) {
		if (mCornersTask != null) {
			mCornersTask.setPoints(points, mShowCorners);
		}
	}

	private void initPaint(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		final TypedArray ar = getContext().obtainStyledAttributes(attrs, R.styleable.CameraOverlay, defStyleAttr, defStyleRes);
		mFrameWidth = ar.getDimension(R.styleable.CameraOverlay_frameWidth, mFrameWidth);
		mFrameColor = ar.getColor(R.styleable.CameraOverlay_frameColor, mFrameColor);
		mBoundsColor = ar.getColor(R.styleable.CameraOverlay_boundsColor, mBoundsColor);
		mAlertColor = ar.getColor(R.styleable.CameraOverlay_alertColor, mAlertColor);
		ar.recycle();

		mCornersPaint.setStyle(Paint.Style.STROKE);
		mCornersPaint.setStrokeWidth(mFrameWidth);
		mCornersPaint.setStrokeJoin(Paint.Join.ROUND);
		mCornersPaint.setStrokeCap(Paint.Cap.ROUND);
	}

	void setSdkFactory(SdkFactory factory) {
		mSdkFactory = factory;
		mCornersTask = new Task(mSdkFactory.createCutoutAverage(20));
	}
}
