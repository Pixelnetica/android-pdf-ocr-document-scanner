package com.pixelnetica.easyscan;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.AsyncTask;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pixelnetica.imagesdk.Corners;
import com.pixelnetica.imagesdk.MetaImage;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Denis on 28.10.2016.
 */

class ProcessImageTask extends AsyncTask<MetaImage, Void, ProcessImageTask.ProcessImageResult> {

	static class ProcessImageResult extends TaskResult {
		final MetaImage targetImage;

		ProcessImageResult(@NonNull MetaImage targetImage) {
			this.targetImage = targetImage;
		}

		ProcessImageResult(@TaskError int error) {
			super(error);
			targetImage = null;
		}

	}

	interface Listener {
		void onProcessImageComplete(@NonNull ProcessImageTask task, @NonNull ProcessImageResult result);
	}

	/**
	 * Processing profile
	 */
	@Retention(RetentionPolicy.SOURCE)
	@IntDef(flag = true, value = {NoBinarization, BWBinarization, GrayBinarization, ColorBinarization, SimpleRotate, ProcessingMask, StrongShadows})
	@interface ProcessingProfile {};
	static final int NoBinarization = 0;
	static final int BWBinarization = 1;
	static final int GrayBinarization = 2;
	static final int ColorBinarization = 3;
	static final int SimpleRotate = 4;  // Debug only

	static final int ProcessingMask = 0xFF;
	static final int StrongShadows = 1 << 30;

	@NonNull
	private final SdkFactory factory;

	@Nullable
	protected final Corners corners;

	@ProcessingProfile
	private final int profile;

	// Output
	@NonNull
	private final Listener listener;

	ProcessImageTask(@NonNull SdkFactory factory, @Nullable Corners corners, @ProcessingProfile int profile,
	                 @NonNull Listener listener) {
		this.factory = factory;
		this.corners = corners;
		this.profile = profile;
		this.listener = listener;
	}

	private Bitmap simpleRotate(Bitmap source, int orientation) {
		if (source == null) {
			return null;
		}

		Matrix matrix = new Matrix();
		switch (orientation) {
			case MetaImage.ExifRotate90:
				matrix.postRotate(90);
				break;
			case MetaImage.ExifRotate180:
				matrix.postRotate(180);
				break;
			case MetaImage.ExifRotate270:
				matrix.postRotate(270);
				break;
			// etc... DEBUG only
		}
		return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
	}

	@Override
	protected ProcessImageResult doInBackground(MetaImage... params) {
		try (SdkFactory.Routine routine = factory.createRoutine()) {

			// DEBUG ONLY!!!
			if (routine.sdk == null) {
				// Simple rotate image
				Bitmap rotated = simpleRotate(params[0].getBitmap(), params[0].getExifOrientation());
				return new ProcessImageResult(new MetaImage(rotated));
			}

			// Usually image already cropped
			MetaImage croppedImage;
			if (corners != null) {
				croppedImage = routine.sdk.correctDocument(params[0], corners);
				if (croppedImage == null) {
					return new ProcessImageResult(TaskResult.INVALIDCORNERS);
				}
			} else {
				croppedImage = params[0];
				if (croppedImage == null) {
					return new ProcessImageResult(TaskResult.INVALIDFILE);
				}
			}

			croppedImage.setStrongShadows((profile & StrongShadows) != 0);
			MetaImage targetImage = null;
			switch (profile & ProcessingMask) {
				case NoBinarization:
					targetImage = routine.sdk.imageOriginal(croppedImage);
					break;

				case BWBinarization:
					targetImage = routine.sdk.imageBWBinarization(croppedImage);
					break;

				case GrayBinarization:
					targetImage = routine.sdk.imageGrayBinarization(croppedImage);
					break;

				case ColorBinarization:
					targetImage = routine.sdk.imageColorBinarization(croppedImage);
					break;

				case SimpleRotate:
					targetImage = new MetaImage(simpleRotate(croppedImage.getBitmap(), croppedImage.getExifOrientation()));
					break;

				default:
					throw new IllegalStateException("Unknown processing " + Integer.toString(profile & ProcessingMask));
			}

			return new ProcessImageResult(targetImage);
		} catch (OutOfMemoryError e) {
			return new ProcessImageResult(TaskResult.OUTOFMEMORY);
		} catch (Error | Exception e) {
			e.printStackTrace();
			return new ProcessImageResult(TaskResult.PROCESSING);
		}
	}

	@Override
	protected void onPostExecute(ProcessImageResult result) {
		listener.onProcessImageComplete(this, result);
	}
}
