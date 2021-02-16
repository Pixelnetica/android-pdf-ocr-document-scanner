package com.pixelnetica.easyscan.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Checkable;

import com.pixelnetica.easyscan.AppLog;


/**
 * Helper class to implement Checkable interface
 * Based on {@code #CompoundButton} class
 * Created by Denis on 15.06.2015.
 */
public class CheckableHelper implements Checkable {

	private final Object mOwner;

	public interface OnCheckedChangeListener {
		void onCheckedChanged(Object sender, boolean isChecked);
	}

	/**
	 * Main value
	 */
	private boolean mChecked;

	/**
	 * Enable (by default) or disable be checkable (useful for indicators)
	 */
	private boolean mFocusable = true;

	private boolean mBroadcasting;

	private OnCheckedChangeListener mOnCheckedChangeListener;

	private int mStyleableChecked = android.R.attr.checked;
	private int mStyleableFocusable = android.R.attr.focusable;
	private int [] mStyleableAttrs = {mStyleableChecked, mStyleableFocusable};

	/**
	 * Useful for {@code #onCreateDrawableState()}
	 */
	public static final int[] CHECKED_STATE_SET = {
			android.R.attr.state_checked
	};

	public CheckableHelper() {
		mOwner = this;
	}

	public CheckableHelper(Object owner) {
		if (owner != null) {
			mOwner = owner;
		} else {
			mOwner = this;
		}
	}

	public void readAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		try {
			final TypedArray a = context.obtainStyledAttributes(
					attrs, mStyleableAttrs, defStyleAttr, defStyleRes);

			final boolean checked = a.getBoolean(
					0, mChecked);
			setCheckedState(checked);

			// Store XML "focusable" flag to restore it later
			//mFocusable = a.getBoolean(1, mFocusable);
			// TODO: Why focusable attribute doesn't work through TypedArray?
			mFocusable = attrs.getAttributeBooleanValue(
					"http://schemas.android.com/apk/res/android",
					"focusable", mFocusable);

			a.recycle();
		} catch (Exception e) {
			Log.d(AppLog.TAG, "Cannot get checked attribute", e);
		}
	}

	public boolean setCheckedState(boolean checked) {
		if (mChecked != checked) {

			// Avoid infinite recursions if setChecked() is called from a listener
			if (mBroadcasting) {
				return true;
			}

			mBroadcasting = true;

			// Take caller a chance to update
			onUpdateChecked(checked);

			mChecked = checked;

			if (mOnCheckedChangeListener != null) {
				mOnCheckedChangeListener.onCheckedChanged(mOwner, mChecked);
			}

			mBroadcasting = false;
			return true;
		} else {
			// Check was not changed
			return false;
		}
	}

	@Override
	public void setChecked(boolean checked) {
		setCheckedState(checked);
	}

	@Override
	public boolean isChecked() {
		return mChecked;
	}

	@Override
	public void toggle() {
		setChecked(!mChecked);
	}

	/**
	 * E.g.
	 * refreshDrawableState();
	 * notifyViewAccessibilityStateChangedIfNeeded(
	 * AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED);
	 * @param checked
	 */
	protected void onUpdateChecked(boolean checked) {
		// Checkable owner usually implements their "setChecked" as
		// CheckableHelper.setCheckedState() : returns true to update their view
		if (mOwner != this && mOwner instanceof Checkable) {
			((Checkable) mOwner).setChecked(checked);
		}
	}

	public boolean hasFocusable() {
		return mFocusable;
	}

	public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
		mOnCheckedChangeListener = listener;
	}
}
