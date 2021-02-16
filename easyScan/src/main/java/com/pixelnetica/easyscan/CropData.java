package com.pixelnetica.easyscan;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Pair;

import com.pixelnetica.imagesdk.Corners;
import com.pixelnetica.imagesdk.MetaImage;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class CropData extends Corners {

	// Corners was defined
	boolean hasCorners;

	// Original image total size
	final private PointF nativeBounds;

	// Original image orientation
	@MetaImage.ExifOrientation
	private int orientation;

	// Store original corner reference for each mapped value
	// We cannot use Map because key value is mutable
	private ArrayList<Pair<WeakReference<PointF>, WeakReference<Point>>> cutoutList = new ArrayList<>(4);

	CropData(@NonNull MetaImage image) {
		hasCorners = false;

		final Bitmap bm = image.getBitmap();
		if (bm == null) {
			throw new IllegalArgumentException("MetaImage with null bitmap");
		}
		this.nativeBounds = new PointF(bm.getWidth(), bm.getHeight());
		this.orientation = image.getExifOrientation();
	}

	CropData(@NonNull Corners src, @NonNull MetaImage image) {
		super(src);
		hasCorners = true;

		final Bitmap bm = image.getBitmap();
		if (bm == null) {
			throw new IllegalArgumentException("MetaImage with null bitmap");
		}
		this.nativeBounds = new PointF(bm.getWidth(), bm.getHeight());
		this.orientation = image.getExifOrientation();
	}

	CropData(@NonNull CropData src) {
		super(src);
		this.hasCorners = src.hasCorners;
		this.nativeBounds = src.nativeBounds;
		this.orientation = src.orientation;
	}

	@Override
	public void setCorners(@NonNull Corners src) {
		super.setCorners(src);
		hasCorners = true;
	}

	@Override
	public void setCorners(@NonNull Point[] src) {
		super.setCorners(src);
		hasCorners = true;
	}

	@Override
	public void reset() {
		super.reset();
		hasCorners = false;
	}

	public PointF [] mapCorners(@NonNull Matrix matrix) {
		PointF [] corners = asFloat(this.points);
		translatePoints(matrix, corners);
		return corners;
	}

	/**
	 * Remove pairs with empty references
	 */
	private void cleanupList() {
		Iterator<Pair<WeakReference<PointF>, WeakReference<Point>>> it = cutoutList.iterator();
		while (it.hasNext()) {
			Pair<WeakReference<PointF>, WeakReference<Point>> item = it.next();
			if (item.first.get() == null || item.second.get() == null) {
				it.remove();
			}
		}
	}


	public PointF [] mapCutout(@NonNull Matrix matrix, PointF scaledBounds) {
		PointF [] cutout = asFloat(this.points);

		if (scaledBounds != null && !scaledBounds.equals(nativeBounds)) {
			for (PointF item : cutout) {
				item.set(item.x * scaledBounds.x / nativeBounds.x, item.y * scaledBounds.y / nativeBounds.y);
			}
		}

		translatePoints(matrix, cutout);


		// store references in the map
		cleanupList();
		for (int i = 0; i < cutout.length; ++i) {
			cutoutList.add(new Pair<>(new WeakReference<>(cutout[i]), new WeakReference<>(this.points[i])));
		}

		// Convert corners to cutout
		Collections.swap(Arrays.asList(cutout), 2, 3);

		// Restore order
		selectCutout(cutout);

		return cutout;
	}

	public RectF mapOrigin(@NonNull Matrix matrix, PointF scaledBounds) {
		if (scaledBounds == null) {
			scaledBounds = nativeBounds;
		}

		RectF rf = new RectF(0, 0, scaledBounds.x, scaledBounds.y);
		final Matrix m = buildOrientationMatrix(orientation);
		m.mapRect(rf);
		rf.offset(-rf.left, -rf.top);

		matrix.mapRect(rf);
		return rf;
	}


	public void setMappedItem(@NonNull PointF item, @NonNull PointF value, @Nullable PointF scaledBounds) {
		cleanupList();
		if (scaledBounds == null) {
			scaledBounds = nativeBounds;
		}
		for (Pair<WeakReference<PointF>, WeakReference<Point>> pair : cutoutList) {
			if (pair.first.get() == item) {
				pair.second.get().set(
						Math.round(value.x * nativeBounds.x/scaledBounds.x),
						Math.round(value.y * nativeBounds.y / scaledBounds.y));
			}
		}
	}

	public Object markCutout(PointF [] cutout, PointF item) {
		cleanupList();
		// Lookup pairs only for cutout
		if (cutout != null && item != null && Arrays.asList(cutout).indexOf(item) != -1) {
			for (Pair<WeakReference<PointF>, WeakReference<Point>> pair : cutoutList) {
				if (pair.first.get() == item) {
					return pair.second.get();
				}
			}
		}
		return null;
	}

	public PointF findMark(PointF [] cutout, Object mark) {
		cleanupList();
		if (cutout != null && mark != null) {
			for (PointF pf : cutout) {
				for (Pair<WeakReference<PointF>, WeakReference<Point>> pair : cutoutList) {
					if (pair.first.get() == pf && pair.second.get() == mark) {
						return pf;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Arrange to CW cutout
	 * @return
	 */
	private PointF [] buildCutout() {
		PointF [] cutout = asFloat(this.points);
		PointF tmp = cutout[2];
		cutout[2] = cutout[3];
		cutout[3] = tmp;
		return cutout;
	}

	private void setCutout(PointF[] cutout) {
		putFloat(this.points, cutout);
		Point tmp = this.points[2];
		this.points[2] = this.points[3];
		this.points[3] = tmp;
	}

	@MetaImage.ExifOrientation
	int getOrientation() {
		return orientation;
	}

	/**
	 * Find nearest point and make it first
	 * Make coutout CW oriented
	 * @param cutout
	 */
	private static void selectCutout(@NonNull PointF [] cutout) {
		int first = -1;
		float cross = 0;
		float dist = Float.MAX_VALUE;
		int j = cutout.length-1;
		for (int i = 0; i < cutout.length; j = i++) {
			final PointF p = cutout[i];
			float d = p.x*p.x + p.y*p.y;
			if (d < dist) {
				dist = d;
				first = i;
			}

			cross += cutout[i].x * cutout[j].y;
			cross -= cutout[i].y * cutout[j].x;
		}
		if (first > 0 || cross > 0) {
			List<PointF> list = Arrays.asList(cutout);
			if (first > 0) {
				Collections.rotate(list, first);
			}
			if (cross > 0) {
				Collections.reverse(list);
			}
			list.toArray(cutout);
		}
	}

	void setOrientation(int value) {
		if (value != orientation) {
			// A bit complex case
			// SDK function ImageProcessing.detectDocumentCorners returns corners already rotated
			// to document orientation. To change corners orientation we must first transform corners
			// back to original image and next translate to new orientation

			Matrix m = buildOrientationMatrix(orientation);

			// Store document bounds: translate native image bounds to current orientation
			RectF bounds = new RectF(0, 0, nativeBounds.x, nativeBounds.y);
			m.mapRect(bounds);
			bounds.offset(-bounds.left, -bounds.top);

			// Inverse matrix converts current cutout to native
			Matrix inverse = new Matrix();
			if (!m.invert(inverse)) {
				throw new IllegalStateException("Non-invertible orientation matrix for " + orientation);
			}

			// A new orientation matrix
			m = buildOrientationMatrix(value);
			m.preConcat(inverse);   // Multiple matrices to get transform
			m.mapRect(bounds);
			m.postTranslate(-bounds.left, -bounds.top);

			// To keep right corners order we must convert "corners" to CW oriented "cutout", find nearest item
			// and restore corners order
			final PointF [] cutout = buildCutout();
			translatePoints(m, cutout);

			// Rearrange corners to make nearest corners first
			selectCutout(cutout);
			setCutout(cutout);

			// Orientation changed!
			orientation = value;
		}
	}

	Matrix getMatrix() {
		Matrix m = buildOrientationMatrix(orientation);
		translateBounds(m, nativeBounds, true);
		return m;
	}

	private static PointF translateBounds(@NonNull Matrix m, @NonNull PointF size, boolean snapMatrix) {
		RectF rf = new RectF(0, 0, size.x, size.y);
		m.mapRect(rf);
		if (snapMatrix) {
			m.postTranslate(-rf.left, -rf.top);
		}
		return new PointF(rf.width(), rf.height());
	}

	private static PointF [] asFloat(@NonNull Point[] points) {
		PointF [] pts = new PointF[points.length];
		for (int i = 0; i < points.length; ++i) {
			pts[i] = new PointF(points[i]);
		}
		return pts;
	}

	private static void putFloat(@NonNull Point[] points, @NonNull PointF [] ptfs) {
		if (points.length != ptfs.length) {
			throw new IllegalArgumentException(String.format("Arrays length not equal", points.length, ptfs.length));
		}

		final int length = points.length;
		for (int i = 0; i < length; ++i) {
			final PointF pf = ptfs[i];
			points[i].set(Math.round(pf.x), Math.round(pf.y));
		}
	}

	void putFloatCorner(@NonNull PointF pf, int index) {
		if (index < 0 || index >= points.length) {
			throw new IllegalArgumentException(String.format("Invalid corner index %d", index));
		}
		this.points[index].set(Math.round(pf.x), Math.round(pf.y));
	}

	private static void translatePoints(@NonNull Matrix m, @NonNull PointF[] points) {
		float [] pts = new float[points.length * 2];
		for (int i = 0; i < points.length; ++i) {
			pts[i*2] = points[i].x;
			pts[i*2+1] = points[i].y;
		}

		m.mapPoints(pts);

		// Convert back
		for (int i = 0; i < points.length; ++i) {
			points[i].x = pts[i*2];
			points[i].y = pts[i*2+1];
		}
	}

	private static Matrix buildOrientationMatrix(@MetaImage.ExifOrientation int orientation) {
		Matrix matrix = new Matrix();
		switch (orientation) {
			case MetaImage.ExifFlipHorizontal:
				matrix.setScale(-1f, 1f);
				break;

			case MetaImage.ExifRotate180:
				matrix.setRotate(180f);
				break;

			case MetaImage.ExifFlipVertical:
				matrix.setScale(1f, -1f);
				break;

			case MetaImage.ExifTranspose:
				matrix.setRotate(90f);
				matrix.postScale(1f, -1f);
				break;

			case MetaImage.ExifRotate90:
				// NOTE: Exif means rotation counter-clockwise
				// Android means rotation clockwise
				matrix.setRotate(90f);
				break;

			case MetaImage.ExifTransverse:
				matrix.setRotate(90f);
				matrix.postScale(-1f, 1f);
				break;

			case MetaImage.ExifRotate270:
				matrix.setRotate(-90f);
				break;
		}
		return matrix;
	}


	void expand() {
		PointF bounds = translateBounds(getMatrix(), nativeBounds, false);
		points[0].x = 0;
		points[0].y = 0;
		points[1].x = Math.round(bounds.x);
		points[1].y = 0;
		points[2].x = 0;
		points[2].y = Math.round(bounds.y);
		points[3].x = Math.round(bounds.x);
		points[3].y = Math.round(bounds.y);
	}
}
