package com.pixelnetica.easyscan;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import com.pixelnetica.easyscan.util.LiveSignal;
import com.pixelnetica.imagesdk.MetaImage;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import static com.pixelnetica.easyscan.ProcessImageTask.BWBinarization;

/**
 * Identity for MainActivity
 * Contains all "back-end" stuff for MainActivity
 * Created by Denis on 25.03.2018.
 */

public class MainIdentity extends AndroidViewModel {


	// Notify activity for total update
	private LiveSignal<Void> mUpdateUI = new LiveSignal<>();
	LiveData<Void> onUpdateUI = mUpdateUI;

	// Show error message
	private LiveSignal<TaskResult> mErrorMessage = new LiveSignal<>();
	LiveData<TaskResult> onErrorMessage = mErrorMessage;

	private LiveSignal<List<SaveImageTask.ImageFile>> mSaveComplete = new LiveSignal<>();
	LiveData<List<SaveImageTask.ImageFile>> onSaveComplete = mSaveComplete;

	/**
	 * Application state
	 */
	@Retention(RetentionPolicy.SOURCE)
	@IntDef({InitNothing, Source, Target})
	@interface ImageMode { }

	/**
	 * No image selected. Initial state
	 */
	static final int InitNothing = 0;

	/**
	 * Image is source, no processing
	 */
	static final int Source = 1;

	/**
	 * Processing complete, Save result available
	 */
	static final int Target = 2;

	/**
	 * Main application state
	 */
	private @ImageMode int mImageMode = InitNothing;

	private int mWaitCounter;

	/**
	 * Loaded source
	 */
	private Uri mSourceUri;

	/**
	 * Source image orientation: will be initialized on load
	 */
	private CropData mSourceCropData;

	/**
	 * Target orientation
	 */
	private CropData mTargetCropData;

	/**
	 * Processed image
	 */
	private MetaImage mProcessedImage;

	/**
	 * SDK Factory used to create SDK objects uniformly
	 */
	final SdkFactory SdkFactory;

	// Always use manual crop
	private boolean mForceManualCrop;
	static final String PREFS_FORCE_MANUAL_CROP = "FORCE_MANUAL_CROP";

	// Perform crop when open
	private boolean mAutoCropOnOpen;
	static final String PREFS_AUTO_CROP_ON_OPEN = "AUTO_CROP_ON_OPEN";

	// Special mode for some processing
	private boolean mStrongShadows;
	private static final String PREFS_STRONG_SHADOWS = "STRONG_SHADOWS";

	@ProcessImageTask.ProcessingProfile
	private int mProcessingProfile = BWBinarization;
	private static final String PREFS_PROCESSING_PROFILE = "PROCESSING_PROFILE";

	private @SaveImageTask.SaveFormat int mSaveFormat = SaveImageTask.SAVE_TIFF_G4;
	private static final String PREFS_SAVE_FORMAT = "save_format";

	private boolean mSimulatePages = true;
	private static final String PREFS_SIMULATE_PAGES = "simulate-pages";

	private int mPdfConfig;
	private static final String PREFS_PDF_CONFIG = "pdf-config";

	public MainIdentity(@NonNull Application application) {
		super(application);
		SdkFactory = new AppSdkFactory(application);

		// Initialize default settings
		setForceManualCrop(false);
		setAutoCropOnOpen(true);
	}

	void loadSettings() {
		final SharedPreferences prefs = SdkFactory.getPreferences();

		SdkFactory.getLibrary();

		// Main params
		mForceManualCrop = prefs.getBoolean(PREFS_FORCE_MANUAL_CROP, mForceManualCrop);
		mAutoCropOnOpen = prefs.getBoolean(PREFS_AUTO_CROP_ON_OPEN, mAutoCropOnOpen);
		mStrongShadows = prefs.getBoolean(PREFS_STRONG_SHADOWS, mStrongShadows);
		mProcessingProfile = prefs.getInt(PREFS_PROCESSING_PROFILE, mProcessingProfile);

		// App params
		//noinspection ResourceType
		mSaveFormat = prefs.getInt(PREFS_SAVE_FORMAT, mSaveFormat);
		mSimulatePages = prefs.getBoolean(PREFS_SIMULATE_PAGES, mSimulatePages);
		mPdfConfig = prefs.getInt(PREFS_PDF_CONFIG, mPdfConfig);

		// SDK params
		SdkFactory.loadPreferences();
	}

	boolean canPerformManualCrop() {
		return hasCorners();
	}

	boolean getForceManualCrop() {
		return mForceManualCrop;
	}

	void setForceManualCrop(boolean value) {
		if (value != mForceManualCrop) {
			mForceManualCrop = value;

			// Store settings now!
			SharedPreferences preferences = SdkFactory.getPreferences();
			SharedPreferences.Editor editor = preferences.edit();
			editor.putBoolean(PREFS_FORCE_MANUAL_CROP, mForceManualCrop);
			editor.apply();
		}
	}

	boolean getAutoCropOnOpen() {
		return mAutoCropOnOpen;
	}

	void setAutoCropOnOpen(boolean value) {
		if (value != mAutoCropOnOpen) {
			mAutoCropOnOpen = value;

			// Store settings now!
			SharedPreferences preferences = SdkFactory.getPreferences();
			SharedPreferences.Editor editor = preferences.edit();
			editor.putBoolean(PREFS_AUTO_CROP_ON_OPEN, mAutoCropOnOpen);
			editor.apply();
		}
	}

	boolean isStrongShadows() {
		return mStrongShadows;
	}

	void setStrongShadows(final boolean value) {
		if (value != mStrongShadows) {
			mStrongShadows = value;

			// Store settings now!
			SharedPreferences preferences = SdkFactory.getPreferences();
			SharedPreferences.Editor editor = preferences.edit();
			editor.putBoolean(PREFS_STRONG_SHADOWS, mStrongShadows);
			editor.apply();
		}

		// Apply processing if possible
		processImage(mProcessingProfile);
	}

	@ProcessImageTask.ProcessingProfile
	int getProcessingProfile() {
		return mProcessingProfile;
	}

	void setProcessingProfile(@ProcessImageTask.ProcessingProfile final int value) {
		if (value != mProcessingProfile) {
			mProcessingProfile = value;

			// Store settings now!
			SharedPreferences preferences = SdkFactory.getPreferences();
			SharedPreferences.Editor editor = preferences.edit();
			editor.putInt(PREFS_PROCESSING_PROFILE, mProcessingProfile);
			editor.apply();
		}

		// Apply processing if possible
		processImage(mProcessingProfile);
	}

	@SaveImageTask.SaveFormat
	int getSaveFormat() {
		return mSaveFormat;
	}

	void setSaveFormat(int value) {
		if (value != mSaveFormat) {
			mSaveFormat = value;

			// Store settings now!
			SharedPreferences preferences = SdkFactory.getPreferences();
			SharedPreferences.Editor editor = preferences.edit();
			editor.putInt(PREFS_SAVE_FORMAT, mSaveFormat);
			editor.apply();
		}
	}

	boolean getSimulatePages() {
		return mSimulatePages;
	}

	void setSimulatePages(final boolean value) {
		if (mSimulatePages != value) {
			mSimulatePages = value;

			// Store settings now!
			SharedPreferences preferences = SdkFactory.getPreferences();
			SharedPreferences.Editor editor = preferences.edit();
			editor.putBoolean(PREFS_SIMULATE_PAGES, mSimulatePages);
			editor.apply();
		}
	}

	@SaveImageTask.PdfConfig
	int getPdfConfig() {
		return mPdfConfig;
	}

	void setPdfConfig(@SaveImageTask.PdfConfig int value) {
		if (value != mPdfConfig) {
			mPdfConfig = value;

			// Store settings now!
			SharedPreferences preferences = SdkFactory.getPreferences();
			SharedPreferences.Editor editor = preferences.edit();
			editor.putInt(PREFS_PDF_CONFIG, mPdfConfig);
			editor.apply();
		}
	}

	void reset() {
		mWaitCounter = 0;

		mImageMode = InitNothing;
		mSourceUri = null;
		mSourceCropData = null;
		mTargetCropData = null;
	}

	void openImage(Uri imageUri) {
		openImage(imageUri, () -> {
			// Automatically process image
			if (mAutoCropOnOpen) {
				processImage(mProcessingProfile);
			}
		});
	}

	private void openImage(Uri imageUri, final Runnable callback) {
		setWaitStateForceUI(true);

		LoadImageTask task = new LoadImageTask(SdkFactory, (LoadImageTask imageTask, LoadImageTask.LoadImageResult result) -> {
			if (result.hasError()) {
				setWaitStateForceUI(false);
				showError(result);
			} else {
				mImageMode = Source;
				mSourceUri = result.sourceUri;
				mProcessedImage = result.loadedImage;

				// openImage can be use to reload
				// Do not update crop data in this case
				if (mSourceCropData == null && mProcessedImage != null) {
					mSourceCropData = new CropData(mProcessedImage);
				}

				// Force UI to show loaded image even wait state
				setWaitStateForceUI(false);

				// Perform additional processing
				if (callback != null) {
					callback.run();
				}
			}
		});
		task.execute(imageUri);
	}

	void detectDocument() {
		setWaitStateForceUI(true);
		DetectDocCornersTask task = new DetectDocCornersTask(SdkFactory, (DetectDocCornersTask docCornersTask, @NonNull DetectDocCornersTask.DocCornersResult result) -> {
			if (result.hasError()) {
				setWaitStateForceUI(false);
				showError(result);
			} else if (result.isSmartCrop) {
				// Update corners
				mSourceCropData.setCorners(result.corners);
				// Start processing
				if (mForceManualCrop) {
					mImageMode = Source;
				} else {
					processSource(mProcessingProfile);
				}
				setWaitStateForceUI(false);
			} else {
				// Switch back to source mode to show corners
				mSourceCropData.setCorners(result.corners);
				mImageMode = Source;
				setWaitStateForceUI(false);
			}
		});
		mProcessedImage.setExifOrientation(mSourceCropData.getOrientation());
		task.execute(mProcessedImage);
	}

	private void processImage(@ProcessImageTask.ProcessingProfile final int processing) {
		if (mImageMode == Source) {
			if (mSourceCropData.hasCorners) {
				processSource(processing);
			} else {
				detectDocument();
			}
		} else if (mImageMode == Target) {
			// Reload source and perform full processing
			openImage(mSourceUri, () -> processSource(processing));
		}
	}

	private void processSource(@ProcessImageTask.ProcessingProfile int processing) {
		if (mImageMode != Source) {
			throw new IllegalStateException("Invalid mode " + mImageMode);
		}

		setWaitStateForceUI(true);
		if (mStrongShadows) {
			processing |= ProcessImageTask.StrongShadows;
		}
		ProcessImageTask task = new ProcessImageTask(SdkFactory, mSourceCropData, processing, (ProcessImageTask processImageTask, @NonNull ProcessImageTask.ProcessImageResult result) -> {
			if (result.hasError()) {
				showError(result);
			} else {
				mImageMode = Target;
				mProcessedImage = result.targetImage;
				mTargetCropData = new CropData(mProcessedImage);
			}
			setWaitStateForceUI(false);
		});

		mProcessedImage.setExifOrientation(mSourceCropData.getOrientation());
		task.execute(mProcessedImage);
	}

	void manualCrop() {
		if (mImageMode == Target) {
			openImage(mSourceUri, null);
		}
	}

	void cropImage(CropData cropData) {
		mSourceCropData = cropData;
		processSource(mProcessingProfile);
	}

	private void setWaitState(boolean state) {
		setWaitStateInternal(state, false);
	}

	private void setWaitStateForceUI(boolean state) {
		setWaitStateInternal(state, true);
	}

	private void setWaitStateInternal(boolean state, boolean forceUpdateUI) {
		if (mWaitCounter == 0 && !state) {
			throw new IllegalStateException("Unbounded wait state");
		}

		boolean prevState = mWaitCounter != 0;
		mWaitCounter += state ? 1 : -1;

		boolean newState = mWaitCounter != 0;
		if (forceUpdateUI) {
			mUpdateUI.call();
		} else if (prevState != newState) {
			// Update UI only if wait state was changed
			mUpdateUI.call();
		}
	}

	boolean isWaitState() {
		return mWaitCounter != 0;
	}

	boolean isReady() {
		return mWaitCounter == 0;
	}

	private void showError(final TaskResult error) {
		// Show only last error
		mErrorMessage.setValue(error);
	}

	Bitmap queryDisplayBitmap() {
		if (mImageMode != InitNothing && mProcessedImage != null && !mProcessedImage.isBitmapRecycled()) {
			return mProcessedImage.getBitmap();
		} else {
			return null;
		}
	}

	CropData getCropData() {
		switch (mImageMode) {
			case Source:
				return mSourceCropData;
			case Target:
				return mTargetCropData;

			case InitNothing:
			default:
				return null;
		}
	}

	Matrix getImageMatrix() {
		switch (mImageMode) {
			case InitNothing:
				return new Matrix();
			case Source:
				return mSourceCropData.getMatrix();
			case Target:
				return new Matrix();
			default:
				throw new IllegalStateException("Unknown image mode " + mImageMode);
		}
	}

	boolean hasCorners() {
		return mSourceCropData != null && mSourceCropData.hasCorners;
	}

	boolean hasSourceImage() {
		return mImageMode == Source && mProcessedImage != null;
	}

	boolean hasTargetImage() {
		return mImageMode == Target && mProcessedImage != null;
	}

	void saveImage(@NonNull Context context) {
		// No block UI!
		SaveImageTask task = new SaveImageTask(SdkFactory, (@NonNull SaveImageTask _task, @NonNull final SaveImageTask.SaveImageResult result) -> {
				if (result.hasError()) {
					showError(result);
				} else {
					mSaveComplete.setValue(result.imageFiles);
				}
		});

		// Build file name
		final File file = new File(context.getCacheDir(), context.getResources().getString(R.string.processed_images_file,
				System.currentTimeMillis(), "xxx"));

		task.execute(new SaveImageTask.SaveImageParam(file.getAbsolutePath(), mProcessedImage, mSaveFormat, mPdfConfig, mSimulatePages));
	}
}
