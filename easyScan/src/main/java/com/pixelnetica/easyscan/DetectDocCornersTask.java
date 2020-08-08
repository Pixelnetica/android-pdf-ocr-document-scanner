package com.pixelnetica.easyscan;

import android.os.AsyncTask;
import androidx.annotation.NonNull;
import android.util.Log;

import com.pixelnetica.imagesdk.Corners;
import com.pixelnetica.imagesdk.MetaImage;


class DetectDocCornersTask extends
		AsyncTask<MetaImage, Integer, DetectDocCornersTask.DocCornersResult> {

	static class DocCornersResult extends TaskResult{
		@NonNull final MetaImage sourceImage;
		Corners corners;
		boolean isSmartCrop;
		DocCornersResult(@NonNull MetaImage sourceImage) {
			this.sourceImage = sourceImage;
		}
		DocCornersResult(@TaskError int error) {
			super(error);
			sourceImage = null;
		}
	};

	interface Listener {
		void onDocCornersDetected(@NonNull DetectDocCornersTask task, @NonNull DocCornersResult result);
	}

	@NonNull
	private final SdkFactory mFactory;
	@NonNull
	private final Listener mListener;

	public DetectDocCornersTask(@NonNull SdkFactory factory, @NonNull Listener listener) {
		this.mFactory = factory;
		this.mListener = listener;
	}

	@Override
	protected DocCornersResult doInBackground(MetaImage... params) {
		MetaImage inputImage = params[0];	// always one param

		try (SdkFactory.Routine routine = mFactory.createRoutine()) {
			final DocCornersResult result = new DocCornersResult(inputImage);

			if (routine.sdk == null) {
				// Special DEBUG case to don't use SDK
				result.isSmartCrop = true;
				result.corners = routine.expandCorners(inputImage);
			} else {
				result.corners = routine.sdk.detectDocumentCorners(inputImage, routine.params);
				if (result.corners == null) {
					return new DocCornersResult(TaskResult.NODOCCORNERS);
				} else {
					// Read and copy result
					result.isSmartCrop = routine.isSmartCropMode();
					Log.d("CropDemo", String.format(
							"Document (%d %d) [%d] corners (%d %d) (%d %d) (%d %d) (%d %d)",
							inputImage.getBitmap().getWidth(), inputImage.getBitmap().getHeight(),
							inputImage.getExifOrientation(),
							result.corners.points[0].x, result.corners.points[0].y,
							result.corners.points[1].x, result.corners.points[1].y,
							result.corners.points[2].x, result.corners.points[2].y,
							result.corners.points[3].x, result.corners.points[3].y));
				}
			}

			return result;
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
			return new DocCornersResult(TaskResult.OUTOFMEMORY);
		} catch (Exception | Error e) {
			e.printStackTrace();
			return new DocCornersResult(TaskResult.PROCESSING);
		}
	}

	@Override
	protected void onPostExecute(DocCornersResult result) {
		mListener.onDocCornersDetected(this, result);
	}
}
