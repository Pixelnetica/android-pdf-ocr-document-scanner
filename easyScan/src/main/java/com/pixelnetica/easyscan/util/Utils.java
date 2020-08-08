package com.pixelnetica.easyscan.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.pixelnetica.easyscan.AppLog;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by Denis on 03.10.2016.
 */

public class Utils {

	public static IntPair[] collectRanges(int [] indices, int count) {
		Arrays.sort(indices, 0, count);

		ArrayList<IntPair> pairs = new ArrayList<>();

		int first = 0;
		for (int i = 0; i < count; i++) {
			// Check for Discontinuity
			if ((indices[i] - i) != (indices[first] - first)) {
				pairs.add(new IntPair(indices[first], indices[i-1]));
			}

			// Start next sequence
			first = i;
		}

		// Terminal sequence
		if (count > 0) {
			pairs.add(new IntPair(indices[first], indices[count-1]));
		}

		// Convert list to array
		return pairs.toArray(new IntPair[pairs.size()]);
	}

	/**
	 * helper convert nullable parcel to UUID
	 * @param p ParcelUuid or null
	 * @param defaultValue
	 * @return required or null
	 */
	public static UUID uuidFromParcelable(@Nullable Parcelable p, @Nullable UUID defaultValue) {
		// Accept nullable
		if (p == null) {
			return defaultValue;
		}

		ParcelUuid parcelUuid = (ParcelUuid) (p);
		UUID uuid = parcelUuid.getUuid();
		if (uuid != null) {
			// Just in case
			return uuid;
		} else {
			return defaultValue;
		}
	}

	/**
	 * helper convert nullable parcel to UUID
	 * @param p ParcelUuid or null
	 * @return required or null
	 */
	public static UUID uuidFromParcelable(@Nullable Parcelable p) {
		return uuidFromParcelable(p, null);
	}

	public static AtomicReference<UUID> uuidReferenceFromParcelable(@Nullable Parcelable p) {
		final UUID uuid = uuidFromParcelable(p);
		return uuidReference(uuid);
	}

	public static AtomicReference<UUID> uuidReference(@Nullable UUID uuid) {
		if (uuid != null) {
			return new AtomicReference<UUID>(uuid);
		} else {
			return null;
		}
	}

	/**
	 * safe call toString()
	 */
	public static String toDebugString(Object item) {
		if (item == null) {
			return "<null>";
		} else {
			return item.toString();
		}
	}

	public static IOException safeClose(Closeable foo) {
		try {
			if (foo != null) {
				foo.close();
			}
			return null;
		} catch (IOException e) {
			// Usually skip the exception, but take chance caller to handle it
			return e;
		}
	}

	public static Bitmap loadPicture(@NonNull Context context, @NonNull Uri pictureUri) {
		Bitmap picture = null;
		InputStream pictureStream = null;
		try {
			pictureStream = context.getContentResolver().openInputStream(pictureUri);
			picture = BitmapFactory.decodeStream(pictureStream);
		} catch (FileNotFoundException e) {
			Log.e(AppLog.TAG, "Cannot open file Uri " + pictureUri);
		} catch (OutOfMemoryError e) {
			Log.e(AppLog.TAG, "Out of memory to load image from " + pictureUri);
		} finally {
			safeClose(pictureStream);
		}
		return picture;
	}

	public static Uri scanMediaFile(Context context, File file) {
		MediaScannerConnection.scanFile(context,
				new String[] {file.getPath()},
				new String[] {"image/jpeg"},
				null);

		return getImageContentUri(context, file);
	}

	private static Uri getImageContentUri(Context context, File imageFile) {
		String filePath = imageFile.getAbsolutePath();
		Cursor cursor = context.getContentResolver().query(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
				new String[]{MediaStore.Images.Media._ID},
				MediaStore.Images.Media.DATA + "=? ",
				new String[]{filePath}, null);
		if (cursor != null && cursor.moveToFirst()) {
			int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
			return Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + id);
		} else {
			if (imageFile.exists()) {
				ContentValues values = new ContentValues();
				values.put(MediaStore.Images.Media.DATA, filePath);
				return context.getContentResolver().insert(
						MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
			} else {
				return null;
			}
		}
	}

	public static void matrixMapPoints(@NonNull Matrix matrix, @NonNull PointF[] points) {
		// Skip simple case
		if (matrix.isIdentity() || points.length == 0) {
			return;
		}

		// Convert points to float pairs array
		final int length = points.length;
		float [] pf = new float [length * 2];
		for (int i = 0; i < length; i++) {
			pf[i*2] = points[i].x;
			pf[i*2+1] = points[i].y;
		}

		// Main transform
		matrix.mapPoints(pf);

		// Revert
		for (int i = 0; i < length; i++) {
			points[i].x = pf[i*2];
			points[i].y = pf[i*2 + 1];
		}
	}

	public static boolean checkFloat(float value, float test, float eps) {
		if (eps < 0) {
			throw new IllegalArgumentException("epsilon must be >= 0");
		}
		return Math.abs(value - test) <= eps;
	}
}
