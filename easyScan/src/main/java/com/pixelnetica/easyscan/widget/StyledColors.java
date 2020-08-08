package com.pixelnetica.easyscan.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.DrawableCompat;
import android.util.SparseArray;

/**
 * Get colors from styled attributes
 * Created by Denis on 02.04.2018.
 */
class StyledColors {

	// Some default value to show error
	// Can be changed
	int DEFAULT = Color.RED;

	private final Context mContext;
	private final SparseArray<Integer> mColorMap = new SparseArray<>();

	// Initialize empty
	StyledColors(@NonNull Context context) {
		mContext = context;
	}

	// Initialize with attribute set
	StyledColors(@NonNull Context context, @NonNull int [] attributes) {
		this(context);

		readAttributes(attributes);
	}

	// Read specified attributes
	private void readAttributes(@NonNull int [] attributes) {
		TypedArray arr = mContext.obtainStyledAttributes(attributes);

		for (int i = 0; i < attributes.length; i++) {
			int color = arr.getColor(i, DEFAULT);
			mColorMap.put(attributes[i], color);
		}

		arr.recycle();
	}

	int getColor(int attr) {
		Integer color = mColorMap.get(attr);
		if (color == null) {
			// ineffective case
			readAttributes(new int[]{attr});
			color = mColorMap.get(attr);
		}

		if (color != null) {
			return color;
		} else {
			// Something wrong
			return DEFAULT;
		}
	}

	/**
	 *
	 * @param context
	 * @param attr
	 * @return
	 */
	static Drawable loadStyledDrawable(@NonNull Context context, int attr) {
		final int [] attrs = {attr};
		TypedArray arr = context.obtainStyledAttributes(attrs);
		Drawable drawable = arr.getDrawable(0);
		arr.recycle();
		return drawable;
	}

	public static Drawable tintDrawable(Drawable drawable, int color) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			drawable = DrawableCompat.wrap(drawable);
			DrawableCompat.setTint(drawable, color);
			DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_IN);
		}
		return drawable;
	}
}
