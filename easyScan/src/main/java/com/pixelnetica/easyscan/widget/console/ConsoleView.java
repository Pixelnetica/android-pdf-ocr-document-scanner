package com.pixelnetica.easyscan.widget.console;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.AttributeSet;

import com.pixelnetica.easyscan.R;

/**
 * Created by Denis on 10.06.2016.
 */
public class ConsoleView extends RecyclerView {

	// Orientation-gra
	public static final int DOCK_SIDE_INVALID = -1;
	public static final int DOCK_SIDE_BOTTOM = 0;
	public static final int DOCK_SIDE_LEFT = 1;
	public static final int DOCK_SIDE_RIGHT = 2;
	public static final int DOCK_SIDE_TOP = 3;

	public static final long INFINITE = Long.MAX_VALUE;

	public static abstract class ConsoleLine {
		public final Object tag;
		public final CharSequence text;
		public final long showDelay;
		public final long startTime = System.currentTimeMillis();

		public ConsoleLine(Object tag, CharSequence text, long showDelay) {
			this.tag = tag;
			this.text = text;
			this.showDelay = showDelay;
		}

		public boolean isInfinite() {
			return showDelay == INFINITE;
		}
	}

	public interface IConsole {
		void appendLine(Object tag, @StringRes int textId, long showDelay);
		void appendLine(Object tag, CharSequence text, long showDelay);
		void removeLine(Object tag);
		void clear();
	}

	// To initialize
	private int mDockSide = DOCK_SIDE_INVALID;

	private static class ConsoleLayoutManager extends LinearLayoutManager {

		public ConsoleLayoutManager(Context context, int dockSide) {
			super(context,
					makeLayoutOrientation(dockSide),
					isReverse(dockSide));
		}

		@Override
		public boolean canScrollHorizontally() {
			// Disable scroll
			return false;
		}

		@Override
		public boolean canScrollVertically() {
			return false;
		}
	}

	public ConsoleView(Context context) {
		super(context);
		init(null, 0, 0);
	}

	public ConsoleView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs, 0, 0);
	}

	public ConsoleView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(attrs, defStyleAttr, 0);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public ConsoleView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr);
		init(attrs, defStyleAttr, defStyleRes);
	}

	private void init(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		// Read dock side attribute
		final TypedArray ar = getContext().obtainStyledAttributes(attrs, R.styleable.ConsoleView,
				defStyleAttr, defStyleRes);
		final int dockSide = ar.getInt(R.styleable.ConsoleView_dockSide, DOCK_SIDE_BOTTOM);
		ar.recycle();

		// Attach layout manager
		setLayoutManager(new ConsoleLayoutManager(getContext(), dockSide));

		// Create specific adapter
		setAdapter(new ConsoleAdapter(getContext()));

		// Apply dock side
		setDockSide(dockSide);
	}

	public IConsole getConsole() {
		return (IConsole) getAdapter();
	}

	public void setDockSide(int dockSide) {
		if (dockSide != mDockSide) {
			mDockSide = dockSide;

			// Setup layout
			final ConsoleLayoutManager layoutManager = (ConsoleLayoutManager) getLayoutManager();
			final int orientation = makeLayoutOrientation(mDockSide);
			layoutManager.setOrientation(orientation);
			final boolean reverseLayout = isReverse(mDockSide);
			layoutManager.setReverseLayout(reverseLayout);

			// Notify adapter
			ConsoleAdapter adapter = (ConsoleAdapter) getAdapter();
			adapter.setDockSide(mDockSide);
		}

	}

	static boolean isVertical(int dockSide) {
		return dockSide == DOCK_SIDE_LEFT || dockSide == DOCK_SIDE_RIGHT;
	}

	static boolean isReverse(int dockSide) {
		return dockSide == DOCK_SIDE_TOP || dockSide == DOCK_SIDE_RIGHT;
	}

	private static int makeLayoutOrientation(int dockSide) {
		// NOTE: "Vertical" RecyclerView is stack of horizontal items
		if (isVertical(dockSide)) {
			return RecyclerView.HORIZONTAL;
		} else {
			return RecyclerView.VERTICAL;
		}
	}
}
