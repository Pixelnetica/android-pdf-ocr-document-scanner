package com.pixelnetica.easyscan;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;

import com.pixelnetica.easyscan.R;
import com.pixelnetica.easyscan.camera.FindDocCornersThread;
import com.pixelnetica.imagesdk.AutoShotDetector;
import com.pixelnetica.imagesdk.CutoutAverageF;
import com.pixelnetica.imagesdk.DocumentCutout;
import com.pixelnetica.imagesdk.ImageSdkLibrary;
import com.pixelnetica.imagesdk.ImageWriter;
import com.pixelnetica.imagesdk.ImageWriterException;

/**
 * Created by Denis on 26.11.2016.
 */

public class AppSdkFactory extends SdkFactory {

	AppSdkFactory(@NonNull Application application) {
		super(application);
	}

	@Override
	public Routine createRoutine() {
		return new Routine(getLibrary().newProcessingInstance(), new Bundle(), getApplication());
	}

	@Override
	public ImageWriter createImageWriter(@ImageSdkLibrary.ImageWriterType int type) throws ImageWriterException {
		return getLibrary().newImageWriterInstance(type);
	}

	@Override
	public DocumentCutout createDocumentCutout() {
		return getLibrary().newDocumentCutoutInstance();
	}

	@Override
	public void loadPreferences() {
		// Dummy
	}

	public static float queryDocumentGeometryRate(@NonNull String key, @NonNull Bundle params) {
		return Float.MIN_VALUE;
	}

	public static String verboseDetectionFailure(@NonNull Context context, int failure, float failedRate) {
		switch (failure) {
			case FindDocCornersThread.CORNERS_UNCERTAIN:
				return context.getString(R.string.camera_uncertain_detection);
			case FindDocCornersThread.CORNERS_SMALL_AREA:
				return context.getString(R.string.camera_small_area);
			case FindDocCornersThread.CORNERS_DISTORTED:
				return context.getString(R.string.camera_distorted);
			default:
				return null;
		}
	}

	@Override
	public CutoutAverageF createCutoutAverage(int slidingLength) {
		return getLibrary().newCutoutAverageInstance(slidingLength);
	}

	@Override
	public AutoShotDetector createAutoShotDetector() {
		return getLibrary().newAutoShotDetectorInstance();
	}
}
