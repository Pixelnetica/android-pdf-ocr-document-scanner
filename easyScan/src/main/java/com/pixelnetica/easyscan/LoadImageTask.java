package com.pixelnetica.easyscan;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import android.util.Log;

import com.pixelnetica.imagesdk.MetaImage;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Denis on 23.03.2018.
 */

class LoadImageTask extends AsyncTask<Uri, Void, LoadImageTask.LoadImageResult> {
	static class LoadImageResult extends TaskResult {
		final Uri sourceUri;
		final MetaImage loadedImage;

		LoadImageResult(@NonNull Uri sourceUri, @NonNull MetaImage loadedImage) {
			this.sourceUri = sourceUri;
			this.loadedImage = loadedImage;
		}
		LoadImageResult(@TaskError int error, Uri sourceUri) {
			super(error);
			this.sourceUri = sourceUri;
			this.loadedImage = null;
		}
	}

	interface Listener {
		void onImageLoaded(LoadImageTask task, LoadImageResult result);
	}


	@NonNull private final SdkFactory factory;
	@NonNull private final Listener listener;

	LoadImageTask(@NonNull SdkFactory factory, @NonNull Listener listener) {
		this.factory = factory;
		this.listener = listener;
	}

	@Override
	protected LoadImageResult doInBackground(Uri... uris) {
		final Uri sourceUri = uris[0];

		try (SdkFactory.Routine routine = factory.createRoutine();
		     InputStream inputStream = routine.context.getContentResolver().openInputStream(sourceUri)) {

			Bitmap sourceBitmap = BitmapFactory.decodeStream(inputStream);
			if (sourceBitmap == null) {
				// Cannot decode stream
				return new LoadImageResult(TaskResult.INVALIDFILE, sourceUri);
			}

			if (routine.sdk == null) {
				return new LoadImageResult(sourceUri, new MetaImage(sourceBitmap, routine.context, sourceUri));
			}

			Point sourceSize = new Point(sourceBitmap.getWidth(), sourceBitmap.getHeight());
			Point targetSize = routine.sdk.supportImageSize(sourceSize);
			if (targetSize.x <= 0 || targetSize.y <= 0) {
				return new LoadImageResult(TaskResult.PROCESSING, sourceUri);
			}

			// Returns same image if size is supported
			MetaImage sourceImage;
			if (sourceSize.equals(targetSize)) {
				sourceImage = new MetaImage(sourceBitmap, routine.context, sourceUri);
			} else {
				Log.d(AppLog.TAG, String.format("Image (%d x %d) too large, scale to (%d x %d)",
						sourceSize.x, sourceSize.y,
						targetSize.x, targetSize.y));
				Bitmap scaledBitmap = Bitmap.createScaledBitmap(sourceBitmap, targetSize.x, targetSize.y, true);
				sourceImage = new MetaImage(scaledBitmap, routine.context, sourceUri);
			}

			sourceImage.ensureBitmapMutable();
			return new LoadImageResult(sourceUri, sourceImage);
		} catch (IOException e) {
			return new LoadImageResult(TaskResult.INVALIDFILE, sourceUri);
		} catch (OutOfMemoryError e) {
			return new LoadImageResult(TaskResult.OUTOFMEMORY, sourceUri);
		} catch (Throwable e) {
			return new LoadImageResult(TaskResult.PROCESSING, sourceUri);
		}
	}

	@Override
	protected void onPostExecute(LoadImageResult loadImageResult) {
		listener.onImageLoaded(this, loadImageResult);
	}
}
