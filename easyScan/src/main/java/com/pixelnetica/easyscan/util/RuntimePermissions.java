package com.pixelnetica.easyscan.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.collection.SparseArrayCompat;

import com.pixelnetica.easyscan.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Denis on 07.07.2016.
 */
public class RuntimePermissions {

	public interface Callback {
		void permissionRun(String permission, boolean granted);
	}

	// Singleton instance
	private enum Holder {
		HOLDER;
		final RuntimePermissions inst = new RuntimePermissions();
	}

	public static RuntimePermissions instance() {
		return Holder.HOLDER.inst;
	}

	private class PermissionTask {
		public final String permission;
		public final Callback action;
		public PermissionTask(String permission, Callback action) {
			this.permission = permission;
			this.action = action;
		}
	}

	private final static int initRequestValue = 100;
	private final AtomicInteger mRequestCounter = new AtomicInteger(initRequestValue);
	private final SparseArrayCompat<PermissionTask> mPermissionTask = new SparseArrayCompat<>();

	public int runWithPermission(@NonNull final AppCompatActivity activity, @NonNull final String permission,
	                             @Nullable String message, @NonNull Callback action) {

		if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
			// Prepare callback
			final int requestCode = mRequestCounter.incrementAndGet();
			final Runnable requestPermission = new Runnable() {
				@Override
				public void run() {
					ActivityCompat.requestPermissions(activity, new String[] {permission}, requestCode);
				}
			};

			mPermissionTask.put(requestCode, new PermissionTask(permission, action));

			// Show explained dialog if need
			if (message != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
				final ApplicationInfo appInfo = activity.getApplicationInfo();
				final AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
				dialog.setTitle(R.string.app_name);
				dialog.setIcon(appInfo.icon);
				dialog.setMessage(message);
				dialog.setCancelable(true);

				// OK button just for beauty
				dialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
								requestPermission.run();
							}
						});

				// Display dialog
				dialog.show();
			} else {
				requestPermission.run();
			}
			return requestCode;
		} else {
			// Permission is already allowed
			action.permissionRun(permission, true);
			return -1;
		}
	}

	public int runWithPermission(@NonNull final AppCompatActivity activity, @NonNull final String permission,
	                                 @StringRes int messageId, @NonNull Callback action) {
		String message = activity.getString(messageId);
		return runWithPermission(activity, permission, message, action);
	}

	public boolean handleRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		// Get task for required request
		final PermissionTask task = mPermissionTask.get(requestCode);
		if (task == null) {
			return false;
		}

		// Cleanup task list
		mPermissionTask.delete(requestCode);
		if (mPermissionTask.size() == 0) {
			mRequestCounter.set(initRequestValue);
		}

		// Execute task
		int result = findPermissionResult(task.permission, permissions, grantResults);
		if (result == PermissionNotFound) {
			return false;
		}

		task.action.permissionRun(task.permission, result == PackageManager.PERMISSION_GRANTED);
		return true;
	}


	public static final int PermissionNotFound = Integer.MIN_VALUE;
	public static int findPermissionResult(@NonNull String permission, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (permissions.length != grantResults.length) {
			throw new IllegalArgumentException("Permissions and results have different length");
		}

		final List<String> permissionList = Arrays.asList(permissions);
		final int index = permissionList.indexOf(permission);
		if (index != -1) {
			return grantResults[index];
		} else {
			return PermissionNotFound;
		}
	}

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({GRANTED, DENIED, BLOCKED})
	public @interface PermissionStatus {}

	public static final int GRANTED = 0;
	public static final int DENIED = 1;
	public static final int BLOCKED = 2;

	@PermissionStatus
	public static int getPermissionStatus(Activity activity, String androidPermissionName) {
		if(ContextCompat.checkSelfPermission(activity, androidPermissionName) != PackageManager.PERMISSION_GRANTED) {
			/*if(!ActivityCompat.shouldShowRequestPermissionRationale(activity, androidPermissionName)){
				return BLOCKED;
			}*/
			return DENIED;
		}
		return GRANTED;
	}
}
