package com.pixelnetica.easyscan.camera;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

import com.pixelnetica.easyscan.AppLog;
import com.pixelnetica.easyscan.R;
import com.pixelnetica.easyscan.SdkFactory;
import com.pixelnetica.imagesdk.Corners;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.util.Map.Entry;

/**
 * Created by Denis on 04.02.2015.
 */
public class CameraView extends TextureView implements
		TextureView.SurfaceTextureListener,
		Camera.AutoFocusCallback,
		Camera.PictureCallback,
		Camera.PreviewCallback,
		Camera.ErrorCallback,
		FindDocCornersThread.FindDocCornersListener
{
	public static class TouchParams	{
		private List<Camera.Area> mFocusArea;
	}

	// Callbacks
	public interface Callback {
		/**
		 * Calls
		 * @param inst
		 * @param isCameraReady
		 */
		void onCameraReady(CameraView inst, boolean isCameraReady);

		void onCameraOpen(CameraView inst, boolean succeeded);

		// Params
		void onTouchPreview(CameraView inst, TouchParams params);
		void onPictureReady(CameraView inst, byte[] pictureBuffer);

		// Errors
		class Error {
			// Typical error
			static final int AUTO_FOCUS_FAILED = 1;
			// Long focus
			static final int AUTO_FOCUS_TIMEOUT = 2;
			// Very strange error
			static final int SWITCH_MODE_FAILED = 3;
			// Internal camera error
			static final int INTERNAL_ERROR = 4;
		}

		void onPictureError(CameraView inst, int error);

		void onShotReady(CameraView inst);
		void onDetectCorners(CameraView inst, PointF[] points, int failure, float failedRate);
	}

	private Callback mCallback;

	private SdkFactory mSdkFactory;

	// Back camera by default
	private  int cameraID = 0;
	private Camera mCamera;
	private boolean mInAutoFocus;       // camera is on auto focus mode! Don't touch parameters!
	private boolean mWantShot;          // make shot after auto-focus
	private boolean mInPreview;         // camera performs preview
	private boolean mContinuousFocus;   // used to detect document corners

	// Angle between camera preview and current display (0, 90, 180, 270 degree)
	private int displayRotation = 0;

	// Attributes
	private boolean cropMode = false;
	private float extraZoom = 1f;
	// Used to save files
	private int mShutterRotation = -1;  // Invalid value to setup shutter first time
	private int mInternalRotation = 0;  // Camera shutter rotation value. Camera can't retrieve

	// Auto-Shot params
	private int mAutoShotRadius;    // percents of frame width
	private int mAutoShotDelay;
	private int mAutoShotCount;

	// Flash on/off
	private boolean flashMode;
	private boolean flashAvailable;

	// Find document corners on/off
	private boolean mShowDocumentCorners;

	// Surface stuff
	private int surfaceWidth;
	private int surfaceHeight;
	// Transform from surface-scaled preview to natural proportions with crop mode, zoom and rotation
	private Matrix surfaceTransform;

	// Preview buffer to analyze
	private byte [] previewBuffer;
	private int previewFormat;
	private Point previewSize = new Point();
	private Matrix previewTransform;

	// Find document corners thread
	private FindDocCornersThread docCornersThread;

	// Overlay callback
	private ICameraOverlay cameraOverlay;

	/**
	 * Handler to make shots. Workaround strange Android's bug.
	 */
	private final Handler mHandler = new Handler();

	private Runnable mTimeoutChecker;

	private int mFocusTimeout;

	/**
	 * Focus mode for ordinary preview
	 */
	private final String FOCUS_MODE_PREVIEW = Camera.Parameters.FOCUS_MODE_AUTO;

	/**
	 * Focus mode for a shot. Can be FOCUS_MODE_AUTO or FOCUS_MODE_MACRO
	 */
	private final String FOCUS_MODE_SHOT = Camera.Parameters.FOCUS_MODE_AUTO;

	/**
	 * Focus mode for document mode
	 * Can be FOCUS_MODE_CONTINUOUS_VIDEO or FOCUS_MODE_CONTINUOUS_PICTURE
	 */
	private final String FOCUS_MODE_CONTINUOUS = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;

	private boolean mCameraGranted;
	private boolean mSurfaceAvailable;


	// Base constructors
	public CameraView(Context ctx) {
		super(ctx);
	}

	public CameraView(Context ctx, AttributeSet attrs) {
		super(ctx, attrs);
		readCustomAttributes(attrs, 0, 0);
	}

	public CameraView(Context ctx, AttributeSet attrs, int defStyleAttr) {
		super(ctx, attrs, defStyleAttr);
		readCustomAttributes(attrs, defStyleAttr, 0);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public CameraView(Context ctx, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(ctx, attrs, defStyleAttr, defStyleRes);
		readCustomAttributes(attrs, defStyleAttr, defStyleRes);
	}

	private void readCustomAttributes(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		final TypedArray a = getContext().obtainStyledAttributes(
				attrs,
				R.styleable.CameraView,
				defStyleAttr, defStyleRes);

		try {
			cropMode = a.getBoolean(R.styleable.CameraView_cropMode, cropMode);
			extraZoom = a.getFloat(R.styleable.CameraView_extraZoom, extraZoom);
			if (extraZoom == 0) {
				extraZoom = 1.0f;
			}
			mFocusTimeout = a.getInteger(R.styleable.CameraView_focusTimeout, mFocusTimeout);
		} finally {
			a.recycle();
		}

		// Read auto-shot params
		mAutoShotRadius = getContext().getResources().getInteger(R.integer.auto_shot_radius_100);
		mAutoShotDelay = getResources().getInteger(R.integer.auto_shot_delay_ms);
		mAutoShotCount = getResources().getInteger(R.integer.auto_shot_count);
	}

	public boolean getCropMode() {
		return cropMode;
	}

	public void setCropMode(boolean value) {
		if (cropMode != value) {
			cropMode = value;
			if (mCamera != null) {
				setupCameraDisplayOrientation();
				setupPreviewSize((int)(surfaceWidth * extraZoom), (int)(surfaceHeight * extraZoom), cropMode);
			}
		}
	}

	public float getExtraZoom() {
		return extraZoom;
	}

	public void setExtraZoom(float value) {
		if (extraZoom != value) {
			extraZoom = value;
			if (mCamera != null) {
				setupCameraDisplayOrientation();
				setupPreviewSize((int) (surfaceWidth * extraZoom), (int) (surfaceHeight * extraZoom), cropMode);
			}
		}
	}

	public int getFocusTimeout() {
		return mFocusTimeout;
	}

	public void setFocusTimeout(int value)
	{
		mFocusTimeout = value;
	}

	public int getShutterRotation() {
		return mShutterRotation;
	}

	private int mOrientationValue = -1;

	public void setShutterRotation(int orientation) {
		// Store last orientation value
		mOrientationValue = orientation;

		if (orientation != -1 && mCamera != null) {
			// Quantize and normalize orientation
			orientation = CameraUtils.quantizeDegreeTo360(orientation, 90, 45);
			if (mShutterRotation != orientation) {
				mShutterRotation = orientation;

				Camera.CameraInfo info = new Camera.CameraInfo();
				Camera.getCameraInfo(cameraID, info);

				final Camera.Parameters params = mCamera.getParameters();
				if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
					mInternalRotation = CameraUtils.normalizeDegreeTo360(info.orientation - mShutterRotation);
				} else {  // back-facing camera
					mInternalRotation = CameraUtils.normalizeDegreeTo360(info.orientation + mShutterRotation);
				}
				params.setRotation(mInternalRotation);
				mCamera.setParameters(params);
			}
		}
	}

	private final int FOCUS_MODE_CHANGED = 0;
	private final int FOCUS_MODE_UNCHANGED = 1;
	private final int FOCUS_MODE_UNSUPPORTED = 2;

	private static boolean supportsFocusMode(final String mode, final Camera.Parameters params) {
		final List<String> list = params.getSupportedFocusModes();
		return list != null && list.contains(mode);
	}

	private int setFocusMode(final String mode, final Camera.Parameters params) {
		// Check request mode is supported
		if (supportsFocusMode(mode, params)) {
			Log.d(AppLog.TAG, String.format("Set camera to %s focus mode", mode));
			if (TextUtils.equals(params.getFocusMode(), mode)) {
				return FOCUS_MODE_UNCHANGED;
			} else {
				params.setFocusMode(mode);
				return FOCUS_MODE_CHANGED;
			}
		} else {
			// Focus mode doesn't support
			return FOCUS_MODE_UNSUPPORTED;
		}
	}

	/**
	 * Safe clear focus areas
	 * @param params
	 * @return
	 */
	private static boolean resetFocusArea(final Camera.Parameters params) {
		try {
			final int num = params.getMaxNumFocusAreas();
			if (num > 0) {
				final String fm = params.getFocusMode();
				// Setup (and reset) focus area allows only for AUTO and MACRO
				if (TextUtils.equals(fm, Camera.Parameters.FOCUS_MODE_AUTO) ||
						TextUtils.equals(fm, Camera.Parameters.FOCUS_MODE_MACRO)) {
					List<Camera.Area> list = params.getFocusAreas();
					if (list != null) {
						params.setFocusAreas(null);
						return true;
					}
				}
			}
			return false;
		} catch (Exception e) {
			// Workaround strange exception on some phones (e.g. OnePlus)
			return false;
		}
	}

	public void autoFocus() {
		startAutoFocus(mFocusTimeout, false);
	}

	/**
	 * make a shot
	 */
	public boolean makeShot(final boolean wantFocus, TouchParams touchParams) {
		if (!isCameraReady()) {
			// No camera, no shots
			return false;
		}

		// No focus, simple take a picture
		if (!wantFocus) {
			mCamera.takePicture(null, null, null, this);
			mInPreview = false;
			if (mCallback != null) {
				mCallback.onCameraReady(this, false);
			}
			return true;
		}

		// Setup focus area
		boolean paramsModified = false; // setup camera params only if modified
		final Camera.Parameters params = mCamera.getParameters();

		// Setup focus areas
		if (touchParams != null) {
			// focus-area is not working in CONTINUOUS focus-mode
			int res = setFocusMode(FOCUS_MODE_SHOT, params);
			if (res == FOCUS_MODE_CHANGED) {
				paramsModified = true;
			}
			if (res != FOCUS_MODE_UNSUPPORTED ) {

				params.setFocusAreas(touchParams.mFocusArea);

				// TODO: Need setup metering area as focus???
				//params.setMeteringAreas(list);

				// Camera parameters was changed
				paramsModified = true;
			}
		}

		// Check current focus mode is one supports auto focus
		ArrayList<String> supportedModes = new ArrayList<String>();
		supportedModes.add(Camera.Parameters.FOCUS_MODE_AUTO);
		supportedModes.add(Camera.Parameters.FOCUS_MODE_MACRO);
		supportedModes.add(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
		supportedModes.add(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);

		// Force AUTO/MACRO focus mode if it doesn't support auto focus
		final String focusMode = params.getFocusMode();
		if (!supportedModes.contains(focusMode)) {
			int res = setFocusMode(FOCUS_MODE_SHOT, params);
			if (res == FOCUS_MODE_CHANGED) {
				paramsModified = true;
			} else if (res == FOCUS_MODE_UNSUPPORTED) {
				// Auto focus is not supported. Simple take a picture
				if (paramsModified) {
					mCamera.setParameters(params);
				}
				mCamera.takePicture(null, null, null, this);
				mInPreview = false;
				if (mCallback != null) {
					mCallback.onCameraReady(this, false);
				}
				return true;
			}
		}

		// Change camera parameters only if need
		if (paramsModified) {
			try {
				// Cancel auto focus to setup parameters
				resetAutoFocus();
				mCamera.setParameters(params);
			} catch (Exception e) {
				Log.d(AppLog.TAG, "Cannot setup camera parameters", e);
				return false;
			}
		}

		Log.d(AppLog.TAG, "Starting auto focus...");
		startAutoFocus(mFocusTimeout, true);
		return true;
	}

	/**
	 * Camera.AutoFocusCallback implementation
	 */
	@Override
	public void onAutoFocus(boolean success, Camera camera) {
		Log.d(AppLog.TAG, "Auto focus!");

		if (mTimeoutChecker != null) {
			mHandler.removeCallbacks(mTimeoutChecker);
			mTimeoutChecker = null;
		}

		if (success) {
			Log.d(AppLog.TAG, "Auto focus succeeded!");
			if (mWantShot) {
				camera.takePicture(null, null, null, this);
				mInPreview = false;
				if (mCallback != null) {
					mCallback.onCameraReady(this, false);
				}
			}

			// NOTE: Workaround Lenovo P1ma40 continuous onAutoFocus() call.
			// Not need for normal devices
			resetAutoFocus();
		} else {
			// Notify camera ready on failure
			if (mCallback != null) {
				mCallback.onCameraReady(this, isCameraReady());
			}

			// Check focus mode is AUTO or MACRO
			final Camera.Parameters params = mCamera.getParameters();
			final String focusMode = params.getFocusMode();
			if (TextUtils.equals(focusMode, Camera.Parameters.FOCUS_MODE_AUTO) ||
					TextUtils.equals(focusMode, Camera.Parameters.FOCUS_MODE_MACRO)) {

				// Auto focus failed
				Log.d(AppLog.TAG, String.format("Auto focus for %s mode failed", focusMode));
				if (mCallback != null) {
					// Can't take picture, notify owner
					mCallback.onPictureError(this, Callback.Error.AUTO_FOCUS_FAILED);
				}
			} else {

				// Continuous mode! Switch to AUTO/MACRO mode and try again!
				Log.d(AppLog.TAG, "Continuous focus not completed. Try full focus cycle");
				int res = setFocusMode(FOCUS_MODE_SHOT, params);
				if (res == FOCUS_MODE_CHANGED) {
					resetAutoFocus();
					mCamera.setParameters(params);
					startAutoFocus(mFocusTimeout, mWantShot);

					// No restore preview focus mode!!!
					return;
				}

				// Very strange case.
				Log.w(AppLog.TAG, "Cannot start full focus cycle after continuous");
				if (mCallback != null) {
					// Can't take picture, notify owner
					mCallback.onPictureError(CameraView.this, Callback.Error.SWITCH_MODE_FAILED);
				}
			}
		}

		// Restore focus mode
		mInAutoFocus = false;
		setupPreviewFocus();
	}

	private void checkFlash() {
		flashAvailable = false;
		if (mCamera == null) {
			return;
		}

		final Camera.Parameters params = mCamera.getParameters();
		if (params.getFlashMode() == null) {
			return;
		}

		final List<String> supportedFlashModes = params.getSupportedFlashModes();
		if (supportedFlashModes == null) {
			return;
		}

		// Check used flash modes
		flashAvailable = supportedFlashModes.containsAll(Arrays.asList(Camera.Parameters.FLASH_MODE_OFF, Camera.Parameters.FLASH_MODE_TORCH));
	}

	private void setupFlash() {
		if (mCamera != null && flashAvailable) {
			String flashModeStr;
			if (flashMode) {
				flashModeStr = Camera.Parameters.FLASH_MODE_TORCH;
			} else {
				flashModeStr = Camera.Parameters.FLASH_MODE_OFF;
			}

			final Camera.Parameters params = mCamera.getParameters();
			if (!TextUtils.equals(params.getFlashMode(), flashModeStr)) {
				params.setFlashMode(flashModeStr);
				try {
					mCamera.setParameters(params);
				} catch (RuntimeException e) {
					// Device without flash
				}
			}
		}
	}

	public boolean isFlashAvailable() {
		return flashAvailable;
	}

	/**
	 * returns desired flash mode. Real flash may be off
	 * @return
	 */
	public boolean getFlashMode() {
		return flashMode;
	}

	public void setFlashMode(boolean mode) {
		flashMode = mode;
		setupFlash();
	}

	/**
	 * Read real flash mode from camera
	 * @return
	 */
	public boolean readFlashMode() {
		if (mCamera != null && flashAvailable) {
			final Camera.Parameters params = mCamera.getParameters();
			final String strFlashMode = params.getFlashMode();
			// Any flash accepted but not OFF
			return strFlashMode != null && !TextUtils.equals(strFlashMode, Camera.Parameters.FLASH_MODE_OFF);
		}
		// Unknown flash
		return false;
	}

	public int readShutterRotation() {
		return mInternalRotation;
	}

	public boolean isCameraReady() {
		return mCamera != null && mInPreview && !mInAutoFocus;
	}

	public void resetAutoFocus() {
		if (mCamera != null && mInAutoFocus) {
			Log.d(AppLog.TAG, "Cancel auto focus");
			mCamera.cancelAutoFocus();
			mInAutoFocus = false;
			if (mCallback != null) {
				mCallback.onCameraReady(this, isCameraReady());
			}

			if (mTimeoutChecker != null) {
				mHandler.removeCallbacks(mTimeoutChecker);
				mTimeoutChecker = null;
			}
		}
	}

	private void startAutoFocus(int timeout, boolean wantShot) {
		if (mCamera == null) {
			throw new IllegalStateException("Unable to start auto focus. mCamera is null");
		}

		mWantShot = wantShot;
		mCamera.autoFocus(this);
		mInAutoFocus = true;
		if (mCallback != null) {
			mCallback.onCameraReady(this, isCameraReady());
		}

		if (mTimeoutChecker != null) {
			mHandler.removeCallbacks(mTimeoutChecker);
			mTimeoutChecker = null;
		}

		if (timeout > 0) {
			mTimeoutChecker = new Runnable() {
				@Override
				public void run() {
					mTimeoutChecker = null;
					if (mInAutoFocus) {
						mInAutoFocus = false;
						if (mCallback != null) {
							mCallback.onCameraReady(CameraView.this, isCameraReady());
							mCallback.onPictureError(CameraView.this, Callback.Error.AUTO_FOCUS_TIMEOUT);
						}
					}
				}
			};
			mHandler.postDelayed(mTimeoutChecker, mFocusTimeout);
		}
	}

	private void setupPreviewFocus() {
		if (!isCameraReady()) {
			return;
		}

		boolean modified = false;
		final Camera.Parameters params = mCamera.getParameters();

		// Define focus mode
		String focusModeStr;
		if (mContinuousFocus) {
			focusModeStr = FOCUS_MODE_CONTINUOUS;
		} else {
			focusModeStr = FOCUS_MODE_PREVIEW;
		}

		if (setFocusMode(focusModeStr, params) == FOCUS_MODE_CHANGED) {
			modified = true;
		}

		// Always reset focus area
		if (resetFocusArea(params)) {
			modified = true;
		}

		if (modified) {
			try {
				resetAutoFocus();
				mCamera.setParameters(params);
			} catch (Exception e) {
				Log.d(AppLog.TAG, "Cannot set focus mode " + focusModeStr, e);
			}
		}
	}

	public final boolean getShowDocumentCorners() {
		return mShowDocumentCorners;
	}

	public void setShowDocumentCorners(boolean value) {
		if (value != mShowDocumentCorners) {
			mShowDocumentCorners = value;

			// Force hide corners
			if (cameraOverlay != null) {
				cameraOverlay.showCorners(mShowDocumentCorners);
			}

			// Start CONTINUOUS focus mode to find documents corners on good preview
			mContinuousFocus = mShowDocumentCorners;
			setupPreviewFocus();
		}
	}

	@Override
	public void onAttachedToWindow() {
		// Set surface listener BEFORE super call
		setSurfaceTextureListener(this);
		super.onAttachedToWindow();

		// Create always, but pass preview only if flag is set
		docCornersThread = new FindDocCornersThread(mSdkFactory);
		docCornersThread.addListener(this);
		docCornersThread.start();
	}

	@Override
	public void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (docCornersThread != null) {
			docCornersThread.finish();
			docCornersThread = null;
		}
	}

	/**
	 * tap event
	 */
	@Override
	public boolean onTouchEvent( MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_UP) {
			float x = event.getX();
			float y = event.getY();
			float touchMinor = event.getTouchMinor();
			float touchMajor = event.getTouchMajor();

			RectF rect = new RectF(
					(x - touchMajor)/2,
					(y - touchMinor)/2,
					(x + touchMajor)/2,
					(y + touchMinor)/2);
			onPreviewTouch( rect );
		}
		return true;

	}

	/**
	 * calculate control coordinates to cameta coordinates
	 */
	private Rect calculateTapArea(RectF touchRect, float weight) {
		Matrix m = new Matrix();
		getTransform(m);

		Matrix im = new Matrix();
		m.invert(im);
		im.mapRect(touchRect);

		return new Rect(
				(int) (touchRect.left * 2000 / getWidth()) - 1000,
				(int) (touchRect.top * 2000 / getHeight()) - 1000,
				(int) (touchRect.right * 2000 / getWidth()) - 1000,
				(int) (touchRect.bottom * 2000 / getHeight()) - 1000);
	}

	/**
	 * make focus and shot
	 */
	private void onPreviewTouch(RectF touchRect) {
		if (mCamera != null)
		{
			final Camera.Parameters params = mCamera.getParameters();
			if (params.getMaxNumFocusAreas() == 0) {
				// Focus by area don't supports
				Log.d(AppLog.TAG, "Camera doesn't support custom focus areas");
				return;
			}


			Rect focusRect = calculateTapArea(touchRect, 1f);
			List<Camera.Area> list = new ArrayList<Camera.Area>();
			list.add( new Camera.Area(focusRect, 1000));

			if (mCallback != null) {
				TouchParams touchParams = new TouchParams();
				touchParams.mFocusArea = list;

				mCallback.onTouchPreview(this, touchParams);
			}
		}
	}

	/**
	 * Setup camera display orientation
	 */
	private void setupCameraDisplayOrientation() {
		assert mCamera != null;

		Camera.CameraInfo info = new Camera.CameraInfo();
		mCamera.getCameraInfo(cameraID, info);

		int rotation = ((WindowManager)(getContext().getSystemService(Context.WINDOW_SERVICE)
		)).getDefaultDisplay().getRotation();

		int degrees = 0;
		switch (rotation) {
			case Surface.ROTATION_0: degrees = 0; break;
			case Surface.ROTATION_90: degrees = 90; break;
			case Surface.ROTATION_180: degrees = 180; break;
			case Surface.ROTATION_270: degrees = 270; break;
		}

		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360;  // compensate the mirror
		} else {  // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		mCamera.setDisplayOrientation(result);
		displayRotation = result;
	}

	/**
	 * Find nearest preview size for display
	 * General algorithm
	 * For each supported preview size we calculate unused picture space "extra"
	 * We select preview size with minimal unused space, but greater than display area
	 * If any (all preview less than display), we select maximal preview size with minimal unused space
	 * @param previewSizes supported camera preview sizes
	 * @param displayWidth display area width
	 * @param displayHeight display surface height
	 * @param crop select preview inside (false) or outside (true) display area
	 * @return
	 */
	private static Camera.Size selectPreviewSize(final List<Camera.Size> previewSizes, int displayWidth, int displayHeight, boolean crop) {
		// Camera supports many preview size with different aspect ratio
		// Fill map of preview sizes by extra size (width or height)
		Map<Camera.Size, Integer> mapPreviewSizes = new HashMap<Camera.Size, Integer>();
		for (Camera.Size size : previewSizes)
		{
			// Check aspect type
			boolean scaleToWidth = size.width * displayHeight > displayWidth * size.height;
			if (crop) {
				scaleToWidth = !scaleToWidth;
			}

			int extra = 0;
			if (scaleToWidth) {
				extra = size.width - displayWidth;
			}
			else {
				extra = size.height - displayHeight;
			}

			mapPreviewSizes.put(size, extra);
		}

		// Sort all preview sizes by extra
		mapPreviewSizes = CameraUtils.sortMapByValue(mapPreviewSizes, true);

		Map<Camera.Size, Integer> allowedPreviewSizes = new HashMap<Camera.Size, Integer>();

		// Find minimal extra great than with same extra
		int extra = 0;
		for (Entry<Camera.Size, Integer> entry : mapPreviewSizes.entrySet())
		{
			// Skip preview sizes less than display
			if (entry.getValue() < 0) {
				continue;
			}

			// Store first extra value
			if (extra == 0) {
				extra = entry.getValue();
				allowedPreviewSizes.put(entry.getKey(), extra);
				continue;
			}

			// Store all sizes with same extra
			if (entry.getValue() == extra) {
				allowedPreviewSizes.put(entry.getKey(), extra);
				continue;
			}

			// Stop
			break;
		}

		// Bad way, no preview size allowed. Select sizes with maximal (but negative) extra
		if (allowedPreviewSizes.isEmpty()) {
			assert extra == 0;

			// minimal extra first!
			mapPreviewSizes = CameraUtils.reverseMap(mapPreviewSizes);

			for (Entry<Camera.Size, Integer> entry : mapPreviewSizes.entrySet()) {
				if (extra == 0) {
					extra = entry.getValue();
					allowedPreviewSizes.put(entry.getKey(), extra);
					continue;
				}

				if (entry.getValue() == extra) {
					allowedPreviewSizes.put(entry.getKey(), extra);
					continue;
				}

				// Stop
				break;
			}
		}

		assert !allowedPreviewSizes.isEmpty();

		// Second pass. Select preview size with optimal aspect
		Camera.Size previewSize = null;
		int displayMargins = Integer.MAX_VALUE; // to select preview with minimal margins
		for (Entry<Camera.Size, Integer> entry : allowedPreviewSizes.entrySet())
		{
			// Check aspect type
			final Camera.Size size = entry.getKey();
			boolean scaleToWidth = size.width * displayHeight > displayWidth * size.height;
			if (crop) {
				scaleToWidth = !scaleToWidth;
			}
			int margins;
			if (scaleToWidth) {
				int previewHeight = displayWidth * size.width / size.height;
				margins = Math.abs(displayHeight - previewHeight);
			} else {
				int previewWidth = displayHeight * size.height / size.width;
				margins = Math.abs(displayWidth - previewWidth);
			}

			if (margins < displayMargins) {
				displayMargins = margins;
				previewSize = size;
			}
		}

		// Preview size selected!
		assert previewSize != null;
		return previewSize;
	}

	/**
	 * Select maximal (!) picture size for preview aspect ratio
	 * General algorithm
	 * For each supported picture size we calculate area value for preview-aspect picture part
	 * We select for maximal area value
	 * If many, we select one with minimal difference (typically zero)
	 *
	 * @param pictureSizes picture sizes supported by camera
	 * @param previewSize selected preview size. Used only as aspect ration
	 * @return
	 */
	private static Camera.Size selectPictureSize(final List<Camera.Size> pictureSizes, final Camera.Size previewSize) {
		// Fill map of picture sizes by difference preview aspect and picture area
		Map<Camera.Size, Integer> mapPictureSizes = new HashMap<Camera.Size, Integer>();
		for (Camera.Size size : pictureSizes)
		{
			int surf = 0;   // display area value
			if (size.width * previewSize.height > previewSize.width * size.height)
			{
				int width = size.height * previewSize.width / previewSize.height;
				surf = width* size.height;
			} else {
				int height = size.width * previewSize.height / previewSize.width;
				surf = size.width * height;
			}
			mapPictureSizes.put(size, surf);
		}

		// Reverse sort picture sizes map
		mapPictureSizes = CameraUtils.sortMapByValue(mapPictureSizes, false);

		// Remove all entries except first value
		int surf = -1;
		Iterator<Entry<Camera.Size, Integer>> it = mapPictureSizes.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Camera.Size, Integer> entry = it.next();

			if (surf == -1) {
				// First item
				surf = entry.getValue();
			} else if (entry.getValue() != surf) {
				// Remove all entries with area not equal first
				it.remove();
			}
		}

		assert !mapPictureSizes.isEmpty();

		// Calculate extra area for each picture size
		for (Entry<Camera.Size, Integer> entry : mapPictureSizes.entrySet())
		{
			Camera.Size size = entry.getKey();
			int extraSurf = size.width * size.height - entry.getValue();
			assert extraSurf >= 0;
			entry.setValue(extraSurf);
		}

		// Sort minimal difference
		mapPictureSizes = CameraUtils.sortMapByValue(mapPictureSizes, true);

		// First value is selected
		it = mapPictureSizes.entrySet().iterator();
		assert it.hasNext();
		return it.next().getKey();
	}

	/*
	private static List<Camera.Size> supportedPictureSizes(List<Camera.Size> pictureSizes) {

		ImageProcessing sdk = new ImageProcessing();

		final ArrayList<Camera.Size> supportedSizes = new ArrayList<>();
		final Point pictureSize = new Point();
		for (Camera.Size size : pictureSizes) {
			pictureSize.set(size.width, size.height);
			Point supportedSize = sdk.supportImageSize(pictureSize);
			if (pictureSize.equals(supportedSize)) {
				supportedSizes.add(size);
			}
		}

		sdk.destroy();

		// If no size selected, returns original list
		if (supportedSizes.is_empty()) {
			return pictureSizes;
		} else {
			return supportedSizes;
		}
	}
	*/

	private void setupPreviewSize(int width, int height, boolean crop) {
		assert mCamera != null;

		// Select required display size;
		boolean rotate;
		int displayWidth, displayHeight;    // desired preview size. May be great than view size e.g. for a zoom
		int sourceWidth, sourceHeight;      // original surface (view) size with rotation
		if (displayRotation == 90 || displayRotation == 270)
		{
			// Camera default's landscape
			rotate = true;
			displayWidth = height;
			displayHeight = width;

			sourceWidth = surfaceHeight;
			sourceHeight = surfaceWidth;
		}
		else
		{
			rotate = false;
			displayWidth = width;
			displayHeight = height;
			sourceWidth = surfaceWidth;
			sourceHeight = surfaceHeight;
		}

		// Get camera parameters
		final Camera.Parameters params = mCamera.getParameters();

		// Select optimal preview
		final List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
		Camera.Size previewSize = selectPreviewSize(previewSizes, displayWidth, displayHeight, true);

		// Try to set preview
		try {
			params.setPreviewSize(previewSize.width, previewSize.height);
			mCamera.setParameters(params);
		} catch (Exception e) {
			Log.d(AppLog.TAG, "Cannot set camera parameters", e);
		}

		// Update current preview size (e.g. was exception)
		previewSize = params.getPreviewSize();

		// Select appropriate picture size, based on selected preview aspect ratio
		final List<Camera.Size> pictureSizes = params.getSupportedPictureSizes();
		// NOTE: OR NOT?
		// Select only sizes supported by SDK
		final List<Camera.Size> supportedSizes = pictureSizes; //supportedPictureSizes(pictureSizes);
		Camera.Size pictureSize = selectPictureSize(supportedSizes, previewSize);

		// Setup camera picture size
		try {
			params.setPreviewSize(previewSize.width, previewSize.height);   // In case selected preview size wasn't correct
			params.setPictureSize(pictureSize.width, pictureSize.height);
			mCamera.setParameters(params);
		} catch (Exception e) {
			Log.d(AppLog.TAG, "Cannot set camera parameters", e);
		}

		// Now, transform preview

		// Preview/ display aspect scale ratio
		final float aspectRatioScale = (float) (previewSize.width * sourceHeight) / (float) (sourceWidth * previewSize.height);

		// Scale image to display (zoom)
		PointF ratio = new PointF();
		ratio.x = (float) displayWidth / (float) sourceWidth;
		ratio.y = (float) displayHeight / (float) sourceHeight;
		// Correct aspect ratio to natural proportions
		if (aspectRatioScale < 1f) {
			if (crop) {
				ratio.y /= aspectRatioScale;
			} else {
				ratio.x *= aspectRatioScale;
			}
		} else {
			if (crop) {
				ratio.x *= aspectRatioScale;
			} else {
				ratio.y /= aspectRatioScale;
			}
		}

		// Move image to center
		PointF offset = new PointF();
		offset.x = (int) (sourceWidth * (1f- ratio.x) / 2);
		offset.y = (int) (sourceHeight * (1f - ratio.y) / 2);

		// Swap ratio and offset axis for rotated mode
		if (rotate) {
			float t = ratio.x;
			ratio.x = ratio.y;
			ratio.y = t;

			t = offset.x;
			offset.x = offset.y;
			offset.y = t;
		}

		// Setup matrix
		surfaceTransform = new Matrix();
		surfaceTransform.postScale( ratio.x, ratio.y);
		surfaceTransform.postTranslate(offset.x, offset.y);

		// Apply matrix
		setTransform(surfaceTransform);

		// TODO: remove after debug
		// Translate entire frame
		/*
		RectF frame = new RectF(0, 0, surfaceWidth, surfaceHeight);
		surfaceTransform.mapRect(frame);
		Log.d("SharpScan", String.format("Preview (%3d x %3d = %.3f) scaled to %s (%4f x %4f = %.3f)",
				previewSize.width, previewSize.height, (double) previewSize.width / (double) previewSize.height,
				frame.toShortString(),
				frame.width(), frame.height(), (double) frame.width() / (double) frame.height()));
		*/

		// Matrix to translate preview-image coordinates to view coordinates
		previewTransform = new Matrix();
		// Scale from preview size to surface
		previewTransform.postScale((float) sourceWidth / (float) previewSize.width, (float) sourceHeight / (float) previewSize.height);
		// Rotate to camera angle
		previewTransform.postRotate(displayRotation);
		// Calculate offset after rotation (rotation was around top-left corner)
		RectF rect = new RectF(0, 0, previewSize.width, previewSize.height);
		previewTransform.mapRect(rect);
		previewTransform.postTranslate(-Math.min(rect.left, rect.right), -Math.min(rect.top, rect.bottom));
		// And now apply main transform from surface to view
		previewTransform.postConcat(surfaceTransform);

		// Setup preview format
		int previewFormat = params.getPreviewFormat();
		boolean previewFormatFound = previewFormat == ImageFormat.NV21 || previewFormat == ImageFormat.YUY2;
		if (!previewFormatFound)
		{
			// Try to set preview format
			List<Integer> formats = params.getSupportedPreviewFormats();
			for (Integer format : formats) {
				if (format == ImageFormat.NV21 || format == ImageFormat.YUY2) {
					params.setPreviewFormat(format);
					previewFormatFound = true;
					break;
				}
			}
		}

		if (previewFormatFound) {
			mCamera.setParameters(params);

			// Allocate preview buffer
			this.previewSize.x = previewSize.width;
			this.previewSize.y = previewSize.height;
			this.previewFormat = params.getPreviewFormat();
			final int bpp = ImageFormat.getBitsPerPixel(this.previewFormat);
			final int bufferSize = this.previewSize.x * this.previewSize.y * bpp / 8;
			this.previewBuffer = new byte[bufferSize];

			// Setup camera preview
			mCamera.addCallbackBuffer(this.previewBuffer);
			mCamera.setPreviewCallbackWithBuffer(this);

			// Setup auto-shot
			if (docCornersThread != null) {
				docCornersThread.setShotDetectorParams(
						mAutoShotRadius * Math.min(previewSize.width, previewSize.height) / 100,
						mAutoShotDelay, mAutoShotCount);
			}
		}
	}

	private void openCamera(SurfaceTexture surface) {
		// Open camera
		mCamera = null;
		mInAutoFocus = false;
		mInPreview = false;
		try {
			mCamera = Camera.open(cameraID);
		} catch (Exception e) {
			Log.d(AppLog.TAG, "Cannot open camera", e);
		}

		if (mCallback != null) {
			mCallback.onCameraReady(this, isCameraReady());
		}

		if (mCamera != null) {
			mCamera.setErrorCallback(this);

			// In case no device moving after camera initialization (e.g. emulator)
			setShutterRotation(mOrientationValue);

			try {
				mCamera.setPreviewTexture(surface);

				// Set camera display orientation
				setupCameraDisplayOrientation();
				setupPreviewSize((int) (surfaceWidth * extraZoom), (int) (surfaceHeight * extraZoom), cropMode);

				// Check flash
				checkFlash();
				setupFlash();

				mCamera.startPreview();
				mInPreview = true;

				setupPreviewFocus();    // Setup focus when camera ready
				if (mCallback != null) {
					mCallback.onCameraReady(this, isCameraReady());
				}
			} catch (IOException e) {
				Log.d(AppLog.TAG, "Cannot setup camera", e);
			}
		}

		if (mCallback != null) {
			mCallback.onCameraOpen(this, mCamera != null);
		}
	}

	/**
	 * Open camera
	 */
	public void openCamera() {
		mCameraGranted = true;
		if (mSurfaceAvailable && mCamera == null) {
			openCamera(getSurfaceTexture());
		}
	}

	/**
	 * TextureView.SurfaceTextureListener implementation
	 */
	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
		// Store actual surface dimensions
		surfaceWidth = width;
		surfaceHeight = height;

		mSurfaceAvailable = true;
		if (mCameraGranted && mCamera == null) {
			openCamera(surface);
		}
	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		surfaceWidth = 0;
		surfaceHeight = 0;

		mInternalRotation = 0;

		if (mCamera != null) {
			mInPreview = false;
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}

		if (mCallback != null) {
			mCallback.onCameraReady(this, false);
		}

		return true;
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {

	}

	/**
	 * Camera.PictureCallback implementation
	 */
	@Override
	public void onPictureTaken(byte [] pictureBytes, Camera camera) {
		// Restore current focus mode
		setupPreviewFocus();

		// Start preview after
		camera.startPreview();
		mInPreview = true;

		// Delegate picture to callback AFTER start preview!
		if (mCallback != null) {
			mCallback.onCameraReady(this, isCameraReady());
			mCallback.onPictureReady(this, pictureBytes);
		}
	}

	@Override
	public void onError(int error, Camera camera) {
		if (mCallback != null) {
			mCallback.onPictureError(this, Callback.Error.INTERNAL_ERROR);
		}
	}

	void setSdkFactory(SdkFactory factory) {
		mSdkFactory = factory;
	}

	void setCameraOverlay(ICameraOverlay overlay) {
		cameraOverlay = overlay;
	}

	void setCallback(Callback callback) {
		mCallback = callback;
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		// Start to analyze preview frame
		if (mShowDocumentCorners && docCornersThread != null && docCornersThread.isReady()) {
			FindDocCornersThread.Task task = docCornersThread.createTask(camera, data, previewFormat, previewSize);
			docCornersThread.processDocumentCorners(task);
		} else {
			// Simple return buffer
			camera.addCallbackBuffer(data);
		}
	}

	@Override
	public void documentCornersFound(FindDocCornersThread.Task task) {
		if (task.getThread() == docCornersThread) {

			// Force hide corners when state changed
			final Corners corners = task.documentCorners;

			// Corners found on preview
			if (mShowDocumentCorners) {
				PointF [] points;
				if (corners != null) {
					// Convert corners to view
					final float [] cornerPoints = new float[]{
							corners.points[0].x, corners.points[0].y,
							corners.points[1].x, corners.points[1].y,
							corners.points[2].x, corners.points[2].y,
							corners.points[3].x, corners.points[3].y
					};


					previewTransform.mapPoints(cornerPoints);

					points = new PointF[] {
							new PointF(cornerPoints[0], cornerPoints[1]),
							new PointF(cornerPoints[2], cornerPoints[3]),
							new PointF(cornerPoints[4], cornerPoints[5]),
							new PointF(cornerPoints[6], cornerPoints[7])
					};
				} else {
					points = null;
				}

				if (cameraOverlay != null) {
					cameraOverlay.setDocumentCorners(points);
				}

				if (mCallback != null) {
					mCallback.onDetectCorners(this, points, task.cornerFail, task.failedRate);
				}
			}

			// Go to next frame for specified camera
			if (task.refCamera.get() != null) {
				task.refCamera.get().addCallbackBuffer(task.pictureBuffer);
			}

			// Try to perform auto-shot (
			if (task.shotReady && mCallback != null) {
				mCallback.onShotReady(this);
			}
		}
	}
}
