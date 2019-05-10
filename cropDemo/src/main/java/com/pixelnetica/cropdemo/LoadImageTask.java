package com.pixelnetica.cropdemo;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import com.pixelnetica.imagesdk.MetaImage;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

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
	@NonNull private final ContentResolver cr;

	LoadImageTask(@NonNull SdkFactory factory, @NonNull ContentResolver cr, @NonNull Listener listener) {
		this.factory = factory;
		this.listener = listener;
		this.cr = cr;
	}

	@Override
	protected LoadImageResult doInBackground(Uri... uris) {
		final Uri sourceUri = uris[0];

		try (InputStream inputStream = cr.openInputStream(sourceUri)) {
			Bitmap sourceBitmap = BitmapFactory.decodeStream(inputStream);
			if (sourceBitmap == null) {
				// Cannot decode stream
				return new LoadImageResult(TaskResult.INVALIDFILE, sourceUri);
			}

			try (SdkFactory.Routine routine = factory.createRoutine()) {
				if (routine.sdk == null) {
					return new LoadImageResult(sourceUri, new MetaImage(sourceBitmap, cr, sourceUri));
				}

				Point sourceSize = new Point(sourceBitmap.getWidth(), sourceBitmap.getHeight());
				Point targetSize = routine.sdk.supportImageSize(sourceSize);
				if (targetSize.x <= 0 || targetSize.y <= 0) {
					return new LoadImageResult(TaskResult.PROCESSING, sourceUri);
				}

				// Check OpenGL texture size.
				/*final int maxTextureSize = getMaximumTextureSize();

				// ImageView cannot show images greater than OpenGL texture
				final int maxTargetSize = (targetSize.x >  targetSize.y) ? targetSize.x : targetSize.y;
				if (maxTargetSize > maxTextureSize) {
					if (maxTargetSize == targetSize.x) {
						targetSize.y = maxTextureSize * targetSize.y / targetSize.x;
						targetSize.x = maxTextureSize;
					} else {
						targetSize.x = maxTextureSize * targetSize.x / targetSize.y;
						targetSize.y = maxTextureSize;
					}
				}*/

				// Returns same image if size is supported
				MetaImage sourceImage;
				if (sourceSize.equals(targetSize)) {
					sourceImage = new MetaImage(sourceBitmap, cr, sourceUri);
				} else {
					Log.d(AppLog.TAG, String.format("Image (%d x %d) too large, scale to (%d x %d)",
							sourceSize.x, sourceSize.y,
							targetSize.x, targetSize.y));
					Bitmap scaledBitmap = Bitmap.createScaledBitmap(sourceBitmap, targetSize.x, targetSize.y, true);
					sourceImage = new MetaImage(scaledBitmap, cr, sourceUri);
				}

				sourceImage.ensureBitmapMutable();
				return new LoadImageResult(sourceUri, sourceImage);
			} catch (Exception|Error e) {
				return new LoadImageResult(TaskResult.PROCESSING, sourceUri);
			}
		} catch (IOException e) {
			return new LoadImageResult(TaskResult.INVALIDFILE, sourceUri);
		} catch (OutOfMemoryError e) {
			return new LoadImageResult(TaskResult.OUTOFMEMORY, sourceUri);
		}
	}

	@Override
	protected void onPostExecute(LoadImageResult loadImageResult) {
		listener.onImageLoaded(this, loadImageResult);
	}
}
