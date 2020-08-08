package com.pixelnetica.easyscan.util;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * notify each observer only one time
 * @param <T>
 */
public class LiveSignal<T> extends MediatorLiveData<T> {
	private static class ObserverWrapper<T> implements Observer<T> {
		@NonNull
		final private Observer<? super T> observer;
		private final AtomicBoolean pending = new AtomicBoolean(false);

		ObserverWrapper(@NonNull final Observer<? super T> observer) {
			this.observer = observer;
		}

		@Override
		public void onChanged(final T value) {
			if (pending.compareAndSet(true, false)) {
				observer.onChanged(value);
			}
		}

		void newValue() {
			pending.set(true);
		}
	}

	private final Set<ObserverWrapper<T>> wrappers = new HashSet<>();

	@MainThread
	@Override
	public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
		final ObserverWrapper<T> wrapper = new ObserverWrapper<T>(observer);
		wrappers.add(wrapper);
		super.observe(owner, wrapper);
	}

	@MainThread
	@Override
	public void removeObserver(@NonNull Observer<? super T> observer) {
		if (observer instanceof ObserverWrapper && wrappers.remove(observer)) {
			// Remove wrapper itself
			super.removeObserver(observer);
		} else {
			final Iterator<ObserverWrapper<T>> it = wrappers.iterator();
			while (it.hasNext()) {
				final ObserverWrapper<T> wrapper = it.next();
				if (wrapper.observer.equals(observer)) {
					it.remove();
					super.removeObserver(wrapper);
					break;
				}
			}
		}
		super.removeObserver(observer);
	}

	@MainThread
	@Override
	public void setValue(T value) {
		final Iterator<ObserverWrapper<T>> it = wrappers.iterator();
		for (ObserverWrapper<T> wrapper : wrappers) {
			wrapper.newValue();
		}
		super.setValue(value);
	}

	@MainThread
	public void call() {
		setValue(null);
	}
}
