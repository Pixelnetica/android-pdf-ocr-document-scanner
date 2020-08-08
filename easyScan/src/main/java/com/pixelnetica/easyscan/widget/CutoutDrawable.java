package com.pixelnetica.easyscan.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pixelnetica.easyscan.BuildConfig;
import com.pixelnetica.easyscan.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CutoutDrawable extends Drawable {

	private final Context mContext;

	/**
	 * Crop corners in View's coordinated (used to cache)
	 */
	private PointF [] mCutout;

	// Edges lines
	private final Path mEdgeLines = new Path();

	// Corners screen (!) coordinates
	private boolean mHasCutout;

	// Show cutout as 'invalid' (e.g. red)
	private boolean mInvalidCutout;

	// Show item with index as active
	private PointF mActiveItem;

	// Thumbs (with sectors)
	private final List<Drawable> mThumbList = new ArrayList<>();

	// edges lines paint
	private final Paint mEdgePaint;
	// active lines paint
	private final Paint mActiveEdgePaint;
	// invalid lines paint
	private final Paint mInvalidEdgePaint;
	// corners inactive paint
	private final Paint mCornerPaint;
	// invalid corners paint
	private final Paint mInvalidCornerPaint;
	// active corner paint
	private final Paint mActiveCornerPaint;

	private static final int EMPTY_CORNER = -1;

	private double mTouchRadius = Double.MAX_VALUE;

	public CutoutDrawable(@NonNull Context context) {
		this.mContext = context;

		final Resources res = mContext.getResources();

		// Zero touch radius means any closest corner
		int touchRadius = res.getDimensionPixelSize(R.dimen.crop_touch_radius);
		if (touchRadius > 0) {
			mTouchRadius = touchRadius;
		}

		// Get colors
		final StyledColors colors = new StyledColors(context,
				new int [] {
						R.attr.color_crop_edge,
						R.attr.color_crop_edge_active,
						R.attr.color_crop_edge_invalid,
						R.attr.color_crop_corner,
						R.attr.color_crop_corner_active,
						R.attr.color_crop_corner_invalid,
						R.attr.color_crop_thumb,
						R.attr.color_crop_thumb_active,
						R.attr.color_crop_thumb_invalid,
						R.attr.color_crop_thumb_border,
						R.attr.color_crop_thumb_border_active,
						R.attr.color_crop_thumb_border_invalid,
				});
		// Setup painters

		// Edges paint
		mEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mEdgePaint.setColor(colors.getColor(R.attr.color_crop_edge));
		mEdgePaint.setStyle(Paint.Style.STROKE);
		mEdgePaint.setStrokeWidth(res.getDimension(R.dimen.crop_edge_width));

		mActiveEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mActiveEdgePaint.setColor(colors.getColor(R.attr.color_crop_edge_active));
		mActiveEdgePaint.setStyle(Paint.Style.STROKE);
		mActiveEdgePaint.setStrokeWidth(res.getDimension(R.dimen.crop_edge_width));

		// Same as edges but red
		mInvalidEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mInvalidEdgePaint.setColor(colors.getColor(R.attr.color_crop_edge_invalid));
		mInvalidEdgePaint.setStyle(Paint.Style.STROKE);
		mInvalidEdgePaint.setStrokeWidth(res.getDimension(R.dimen.crop_edge_width));
		if (BuildConfig.DEBUG) {
			// To show corner index
			mInvalidEdgePaint.setTextSize(50f);
		}

		// Corners paint
		mCornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mCornerPaint.setColor(colors.getColor(R.attr.color_crop_corner));
		mCornerPaint.setStyle(Paint.Style.FILL_AND_STROKE);

		// Invalid corners paint
		mInvalidCornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mInvalidCornerPaint.setColor(colors.getColor(R.attr.color_crop_corner_invalid));
		mCornerPaint.setStyle(Paint.Style.FILL_AND_STROKE);

		// Active corner paint
		mActiveCornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mActiveCornerPaint.setColor(colors.getColor(R.attr.color_crop_corner_active));
		mActiveCornerPaint.setStyle(Paint.Style.FILL_AND_STROKE);
	}

	private static float clamp(float value, float range1, float range2) {
		final float minRange = Math.min(range1, range2);
		final float maxRange = Math.max(range1, range2);
		return Math.min(maxRange, Math.max(minRange, value));
	}

	// Angle relative X-axis
	private static float calcAngle(PointF p1, PointF p2)
	{
		float angle = (float) (180/Math.PI * Math.atan2(p2.y - p1.y, p2.x - p1.x));

		// Normalize angle to [0..360]
		if (angle < 0)
		{
			angle += 360f;
		}

		return angle;
	}

	public void setupCutout(PointF [] cutout, PointF activeItem, boolean invalid) {
		if (cutout != null && activeItem != null && Arrays.asList(cutout).indexOf(activeItem) == -1) {
			throw new IllegalArgumentException("activeItem must be contained in cutout");
		}
		mCutout = cutout;
		mActiveItem = activeItem;
		mInvalidCutout = invalid;
		updateCutout();
	}

	public PointF [] getCutout() {
		return mCutout;
	}

	public PointF findCutoutItem(@NonNull PointF position) {
		PointF item = null;
		// Find nearest item to position, but not far than 'mTouchRadius'
		double dist = mTouchRadius;
		for (PointF pf : mCutout) {
			double dx = position.x - pf.x;
			double dy = position.y - pf.y;
			double d = Math.sqrt(dx*dx + dy*dy);
			if (d < dist) {
				dist = d;
				item = pf;
			}
		}
		return item;
	}

	private void updateCutout() {
		final int length = (mCutout != null) ? mCutout.length : 0;
		mHasCutout = length > 0;
		if (!mHasCutout) {
			invalidateSelf();
			return;
		}

		// Check margins
		final RectF bounds = new RectF();
		bounds.set(getBounds());

		// Corners edges
		mThumbList.clear();
		mEdgeLines.rewind();

		// start from last point
		mEdgeLines.moveTo(mCutout[length - 1].x, mCutout[length - 1].y);

		// Draw edges
		for (PointF item : mCutout) {
			mEdgeLines.lineTo(item.x, item.y);
		}
		// finish
		mEdgeLines.close();

		// Don't show corners in disabled mode
		final int [] state = getState();
		boolean enabled = false;
		for (int s : state) {
			if (s == android.R.attr.state_enabled) {
				enabled = true;
				break;
			}
		}
		if (!enabled) {
			invalidateSelf();
			return;
		}

		// Corners
		final float radius = mContext.getResources().getDimension(R.dimen.crop_corner_radius);
		Paint activeCornerPaint = mInvalidCutout ? mInvalidCornerPaint : mActiveCornerPaint;
		Paint cornerPaint = mInvalidCutout ? mInvalidCornerPaint : mCornerPaint;
		for (int i = 0; i < length; i++) {
			final int prev = (i - 1 + length) % length;
			final int next = (i + 1) % length;
			mThumbList.add(new SectorDrawable(mCutout[i], radius,
					calcAngle(mCutout[i], mCutout[prev]),
					calcAngle(mCutout[i], mCutout[next]),
					mCutout[i] == mActiveItem ? activeCornerPaint : cornerPaint));
		}


		// Thumbs
		for (int i = 0; i < length; i++) {
			final PointF point = mCutout[i];
			// Do not draw thumb for dragging corner
			if ( point!= mActiveItem) {
				Drawable thumb = mContext.getResources().getDrawable(R.drawable.crop_thumb);
				if (thumb != null) {
					final int width = thumb.getIntrinsicWidth();
					final int height = thumb.getIntrinsicHeight();
					Rect rect = new Rect(0, 0, width, height);
					rect.offset(
							Math.round(point.x - width * .5f),
							Math.round(point.y - height * 0.5f));
					thumb.setBounds(rect);
					mThumbList.add(thumb);
				}
			}
		}

		invalidateSelf();
	}


	@Override
	public void draw(@NonNull Canvas canvas) {
		if (mHasCutout) {
			// Check margins
			final RectF bounds = new RectF();
			bounds.set(getBounds());

			canvas.drawPath(mEdgeLines, mInvalidCutout ? mInvalidEdgePaint : mEdgePaint);
			for (Drawable item : mThumbList) {
				item.draw(canvas);
			}

			// Show corner indexes
			if (BuildConfig.DEBUG) {
				for (int i = 0; i < mCutout.length; ++i) {
					PointF pf = mCutout[i];
					canvas.drawText(Integer.toString(i), pf.x, pf.y, mInvalidEdgePaint);
				}
			}
		}
	}

	@Override
	public void setAlpha(int alpha) {

	}

	@Override
	public void setColorFilter(@Nullable ColorFilter colorFilter) {

	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}

	@Override
	protected void onBoundsChange(Rect bounds) {
		super.onBoundsChange(bounds);
	}

	@Override
	protected boolean onStateChange(int[] state) {
		return super.onStateChange(state);
	}
}
