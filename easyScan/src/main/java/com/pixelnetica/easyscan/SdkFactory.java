package com.pixelnetica.easyscan;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;

import com.pixelnetica.imagesdk.AutoShotDetector;
import com.pixelnetica.imagesdk.Corners;
import com.pixelnetica.imagesdk.CutoutAverageF;
import com.pixelnetica.imagesdk.DocumentCutout;
import com.pixelnetica.imagesdk.ImageProcessing;
import com.pixelnetica.imagesdk.ImageSdkLibrary;
import com.pixelnetica.imagesdk.ImageWriter;
import com.pixelnetica.imagesdk.ImageWriterException;
import com.pixelnetica.imagesdk.MetaImage;

import java.lang.ref.WeakReference;

/**
 * Created by Denis on 15.07.2017.
 */

public abstract class SdkFactory {

	/**
	 * Current SDK with loaded params
	 */
	public class Routine implements AutoCloseable {
		public final ImageProcessing sdk;
		public final Bundle params;
		public final Context context;

		Routine(ImageProcessing sdk, Bundle params, Context context) {
			this.sdk = sdk;
			this.params = params;
			this.context = context;
		}

		@Override
		public void close() {
			if (sdk != null) {
				sdk.destroy();
			}

			// Try to free memory
			System.gc();
			System.runFinalization();
		}

		public boolean isSmartCropMode() {
				return params != null &&
						params.getBoolean("isSmartCropMode");
		}

		public CropData createCropData(@NonNull MetaImage image, Corners corners) {
			if (corners == null) {
				return null;
			}

			return new CropData(corners, image);
		}

		/**
		 * Helper method
		 * @param image
		 * @return
		 */
		public Corners expandCorners(@NonNull MetaImage image) {
			Bitmap bitmap = image.getBitmap();
			if (bitmap == null) {
				throw new IllegalArgumentException("MetaImage with null Bitmap");
			}

			int width, height;
			switch (image.getExifOrientation()) {
				// Swap with and height for rotated image
				case MetaImage.ExifRotate90:
				case MetaImage.ExifRotate270:
				case MetaImage.ExifTranspose:
				case MetaImage.ExifTransverse:
					width = bitmap.getHeight();
					height = bitmap.getWidth();
					break;
				default:
					width = bitmap.getWidth();
					height = bitmap.getHeight();
			}

			return new Corners(new Point[] {
					new Point(0, 0), new Point(width, 0),
					new Point(0, height), new Point(width, height)
			});
		}
	}

	private final WeakReference<Application> mApplicationRef;
	private ImageSdkLibrary mLibrary;   // Using lazy initialization

	SdkFactory(Application application) {
		mApplicationRef = new WeakReference<>(application);
	}

	ImageSdkLibrary getLibrary() {
		ImageSdkLibrary library = mLibrary;
		final Application application = mApplicationRef.get();
		if (library == null && application != null) {
			synchronized (this) {
				// Demo mode: first load
				ImageSdkLibrary.load(application);
				library = new ImageSdkLibrary();
				mLibrary = library;
			}
		}
		return library;
	}

	Application getApplication() {
		return mApplicationRef.get();
	}
	SharedPreferences getPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(getApplication());
	}


	public abstract void loadPreferences();
	public abstract Routine createRoutine();
	public abstract DocumentCutout createDocumentCutout();
	public abstract CutoutAverageF createCutoutAverage(int slidingLength);
	public abstract AutoShotDetector createAutoShotDetector();
	public abstract ImageWriter createImageWriter(@ImageSdkLibrary.ImageWriterType int type) throws ImageWriterException;
}
