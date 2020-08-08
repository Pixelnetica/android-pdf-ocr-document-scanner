package com.pixelnetica.easyscan.widget.console;

import android.content.Context;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pixelnetica.easyscan.util.IntPair;
import com.pixelnetica.easyscan.R;
import com.pixelnetica.easyscan.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Denis on 10.06.2016.
 */
public class ConsoleAdapter extends RecyclerView.Adapter<ConsoleAdapter.ConsoleViewHolder>
		implements ConsoleView.IConsole {

	private final Context mContext;
	private final ArrayList<ConsoleLineImpl> mLines = new ArrayList<>();

	public class ConsoleLineImpl extends ConsoleView.ConsoleLine implements Runnable {
		public ConsoleLineImpl(Object tag, CharSequence text, long showDelay) {
			super(tag, text, showDelay);
		}

		@Override
		public void run() {
			final int [] removed = findLine(this);
			remove(removed, false);
			notifyRemoved(removed);
		}
	}

	class ConsoleViewHolder extends RecyclerView.ViewHolder {
		final ConsoleTextView mConsoleText;
		ConsoleViewHolder(View itemView) {
			super(itemView);
			mConsoleText = (ConsoleTextView) itemView.findViewById(android.R.id.text1);
		}
	}

	private int mDockSide = -1;

	public ConsoleAdapter(@NonNull Context context) {
		mContext = context;
	}

	@Override
	public int getItemCount() {
		return mLines.size();
	}

	@Override
	public ConsoleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		LayoutInflater inflater = LayoutInflater.from(parent.getContext());
		View itemView = inflater.inflate(R.layout.list_row_console, parent, false);
		return new ConsoleViewHolder(itemView);
	}

	@Override
	public void onBindViewHolder(ConsoleViewHolder holder, int position) {
		// Reposition text
		ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
		if (ConsoleView.isVertical(mDockSide)) {
			params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
			params.height = ViewGroup.LayoutParams.MATCH_PARENT;
		} else {
			params.width = ViewGroup.LayoutParams.MATCH_PARENT;
			params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		}
		holder.itemView.setLayoutParams(params);

		final ConsoleLineImpl line = (ConsoleLineImpl) mLines.get(position);
		holder.mConsoleText.setText(line.text);
		holder.mConsoleText.setDockSide(mDockSide);
	}

	// Array adapter simulation
	private Context getContext() {
		return mContext;
	}

	/**
	 * NOTE: No adapter notification!
	 * @param removed
	 * @param fromHandler
	 */
	private void remove(@NonNull int [] removed, boolean fromHandler) {
		for (int index : removed) {
			final ConsoleLineImpl line = mLines.remove(index);
			if (fromHandler) {
				mShowHandler.removeCallbacks(line);
			}
		}
	}

	private int [] findTag(Object tag) {
		// Collect line indices with same tag
		final int [] indices = new int[mLines.size()];
		int count = 0;
		for (int i = mLines.size()-1; i >= 0; i--) {
			ConsoleLineImpl line = mLines.get(i);
			if (line.tag == tag) {
				indices[count++] = i;
			}
		}

		return Arrays.copyOf(indices, count);
	}

	private int [] findLine(ConsoleLineImpl line) {
		// Collect line indices with same tag
		final int [] indices = new int[mLines.size()];
		int count = 0;
		for (int i = mLines.size()-1; i >= 0; i--) {
			ConsoleLineImpl item = mLines.get(i);
			if (line == item) {
				indices[count++] = i;
			}
		}

		return Arrays.copyOf(indices, count);
	}

	private ConsoleView.ConsoleLine getItem(int position) {
		return mLines.get(position);
	}

	private final Handler mShowHandler = new Handler();

	@Override
	public void appendLine(Object tag, @StringRes int textId, long showDelay) {
		appendLine(tag, getContext().getString(textId), showDelay);
	}

	@Override
	public void appendLine(Object tag, CharSequence text, long showDelay) {
		// Remove line with same tag
		int removed [] = findTag(tag);
		remove(removed, true);

		final ConsoleLineImpl line = new ConsoleLineImpl(tag, text, showDelay);
		mLines.add(line);

		if (removed.length > 0) {
			// Entire content changed
			notifyDataSetChanged();
		} else {
			// Only append occurs
			notifyItemInserted(mLines.size() - 1);
		}

		if (showDelay != ConsoleView.INFINITE) {
			mShowHandler.postDelayed(line, showDelay);
		}
	}

	@Override
	public void removeLine(Object tag) {
		int removed [] = findTag(tag);  // Find all indices
		remove(removed, true);          // really remove from list
		notifyRemoved(removed);
	}

	private void notifyRemoved(@NonNull int [] removed) {
		final IntPair[] pairs = Utils.collectRanges(removed, removed.length);
		if (pairs.length == 1) {
			// One range removed
			final IntPair pair = pairs[0];
			notifyItemRangeRemoved(pair.first, pair.second - pair.first + 1);
		} else if (pairs.length > 1) {
			// Notify entire view if removed
			notifyDataSetChanged();
		}
	}

	@Override
	public void clear() {
		mShowHandler.removeCallbacksAndMessages(null);
		mLines.clear();
		notifyDataSetChanged();
	}

	public void setDockSide(int dockSide) {
		if (mDockSide != dockSide) {
			mDockSide = dockSide;
			notifyItemRangeChanged(0, getItemCount());
		}
	}
}
