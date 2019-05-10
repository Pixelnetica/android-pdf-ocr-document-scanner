package com.pixelnetica.cropdemo.util;

import android.app.Activity;
import android.os.Binder;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.BundleCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * A set of Activity data. It extends android.os.Binder to load and save to activity state
 * Created by Denis on 25.03.2018.
 */

public class Identity<A extends Activity> extends Binder implements Parcelable {

	public interface Action<A extends Activity> {
		void run(A activity);
	}

	/**
	 * Current visible activity.
	 */
	private A activity;

	private final List<Action<A>> actions = new ArrayList<>();

	public void attach(A activity) {
		this.activity = activity;
		if (this.activity != null) {
			// Execute all pending callbacks
			for (Action<A> action : actions) {
				action.run(this.activity);
			}
			// All callbacks done
			actions.clear();
		}
	}

	// Short version
	public void execute(Action<A> action) {
		execute(action, false);
	}

	public void execute(Action<A> action, boolean collapse) {
		if (activity != null) {
			action.run(activity);
		} else {
			// Add only one action (e.g. Update UI)
			if (collapse) {
				actions.remove(action);
			}
			actions.add(action);
		}
	}

	public A getActivity() {
		return activity;
	}

	public static Identity readBundle(@NonNull Bundle bundle, @NonNull String key) {
		try {
			return  (Identity) BundleCompat.getBinder(bundle, key);
		} catch (ClassCastException e) {
			return null;
		}
	}

	public void writeBundle(@NonNull Bundle bundle, @NonNull String key) {
		BundleCompat.putBinder(bundle, key,this);
	}

	// To store as Parcelables (e.g. in Intents)

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeStrongBinder(this);
	}

	public static final Creator<Identity> CREATOR = new Creator<Identity>() {
		@Override
		public Identity createFromParcel(Parcel source) {
			return (Identity) source.readStrongBinder();
		}

		@Override
		public Identity[] newArray(int size) {
			return null;
		}
	};
}
