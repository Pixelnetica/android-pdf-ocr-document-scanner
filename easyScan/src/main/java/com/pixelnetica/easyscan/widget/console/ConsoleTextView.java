package com.pixelnetica.easyscan.widget.console;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.text.TextPaint;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import com.pixelnetica.easyscan.R;

/**
 * NOTE: Vertical (look at "horizontal" gravity doesn't work
 * Created by Denis on 10.06.2016.
 */
public class ConsoleTextView extends AppCompatTextView {

	private int mDockSide = ConsoleView.DOCK_SIDE_INVALID;

	public ConsoleTextView(Context context) {
		super(context);
	}

	public ConsoleTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ConsoleTextView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	private void readAttrs(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		final TypedArray ar = getContext().obtainStyledAttributes(attrs, R.styleable.ConsoleView,
				defStyleAttr, defStyleRes);
		final int dockSide = ar.getInt(R.styleable.ConsoleView_dockSide, ConsoleView.DOCK_SIDE_BOTTOM);
		ar.recycle();

		setDockSide(dockSide);
	}

	private boolean isVertical() {
		return ConsoleView.isVertical(mDockSide);
	}

	private boolean isReverse() {
		return ConsoleView.isReverse(mDockSide);
	}

	public void setDockSide(int dockSide) {
		if (mDockSide != dockSide) {
			mDockSide = dockSide;
			// Draw horizontal items itself
			setRotation( !isVertical() && isReverse()  ? 180 : 0);
			requestLayout();
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (isVertical()) {
			super.onMeasure(heightMeasureSpec, widthMeasureSpec);
			setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
		} else {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (isVertical()) {
			// Setup text color
			final TextPaint textPaint = getPaint();
			textPaint.setColor(getCurrentTextColor());
			textPaint.drawableState = getDrawableState();

			canvas.save();
			if (isReverse()) {
				canvas.translate(getWidth(), 0);
				canvas.rotate(90);
			} else {
				canvas.translate(0, getHeight());
				canvas.rotate(-90);
			}

			canvas.translate(getCompoundPaddingLeft(), getCompoundPaddingTop());
			getLayout().draw(canvas);
			canvas.restore();
		} else {
			super.onDraw(canvas);
		}
	}
}
