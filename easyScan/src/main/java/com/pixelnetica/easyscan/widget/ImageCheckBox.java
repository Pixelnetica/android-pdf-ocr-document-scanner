package com.pixelnetica.easyscan.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;

import androidx.appcompat.widget.AppCompatImageButton;

/**
 * Check box using only image, without text
 * Created by Denis on 14.02.2015.
 */
public class ImageCheckBox extends AppCompatImageButton implements Checkable {
	private final CheckableHelper mCheckableHelper = new CheckableHelper(this);

	public ImageCheckBox(Context context) {
		super(context);
		mCheckableHelper.readAttributes(context, null, 0, 0);
        setFocusable(mCheckableHelper.hasFocusable());
	}

	public ImageCheckBox(Context context, AttributeSet attrs) {
		super(context, attrs, android.R.attr.checkboxStyle);
		mCheckableHelper.readAttributes(context, attrs, android.R.attr.checkboxStyle, 0);
        setFocusable(mCheckableHelper.hasFocusable());
	}

    public ImageCheckBox(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mCheckableHelper.readAttributes(context, attrs, defStyleAttr, 0);
        setFocusable(mCheckableHelper.hasFocusable());
	}

	/*@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public ImageCheckBox(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		mCheckableHelper.readAttributes(context, attrs, defStyleAttr, 0);
        setFocusable(mCheckableHelper.hasFocusable());
	}*/

	@Override
	public int [] onCreateDrawableState(int extraSpace) {
		final int [] state = super.onCreateDrawableState(extraSpace + CheckableHelper.CHECKED_STATE_SET.length);
        // NOTE: onCreateDrawableState() calls from base (!) constructor when mCheckableHelper is null
		if (mCheckableHelper != null && mCheckableHelper.isChecked()) {
			mergeDrawableStates(state, CheckableHelper.CHECKED_STATE_SET);
		}
		return state;
	}

	@Override
	public boolean performClick() {
		if (isFocusable()) {
			mCheckableHelper.toggle();
			return super.performClick();
		} else {
			return false;
		}
	}

	@Override
	public boolean isChecked() {
		return mCheckableHelper.isChecked();
	}

	@Override
	public void setChecked(boolean checked) {
		if (mCheckableHelper.setCheckedState(checked)) {
			refreshDrawableState();
			invalidate();
		}
	}

	@Override
	public void toggle() {
		mCheckableHelper.toggle();
	}
}
