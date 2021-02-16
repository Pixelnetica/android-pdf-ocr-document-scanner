package com.pixelnetica.easyscan.camera;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.PointF;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pixelnetica.easyscan.AppLog;
import com.pixelnetica.easyscan.R;
import com.pixelnetica.easyscan.SdkFactory;
import com.pixelnetica.easyscan.util.ParcelableHolder;
import com.pixelnetica.easyscan.util.RuntimePermissions;
import com.pixelnetica.easyscan.AppSdkFactory;
import com.pixelnetica.easyscan.util.Utils;
import com.pixelnetica.easyscan.widget.ImageCheckBox;
import com.pixelnetica.easyscan.widget.console.ConsoleView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.pixelnetica.easyscan.R.string.permission_query_write_storage;


public class CameraActivity
		extends AppCompatActivity
		implements MotionDetector.MotionDetectorCallbacks, CameraView.Callback
{
	/**
	 * Intent extra key to directory to write files
	 */
	private static final String EXTRA_PICTURE_SINK = "picture-sink";
	private static final String EXTRA_SDK_FACTORY = "sdk-factory";

	/**
	 * Intent extra key for @code:SharedPreferences name
	 * if null, no settings store
	 */
	public static final String PREFERENCES_NAME = "Preferences name";
	private static final String PREFS_FLASH_MODE = "CameraActivity.mFlashMode";
	private static final String PREFS_STAB_MODE = "CameraActivity.mStabMode";
	private static final String PREFS_BATCH_MODE = "CameraActivity.mBatchMode";
	private static final String PREFS_SHOW_DOCUMENT = "CameraActivity.mShowDocument";
	public static final String PREFS_MOTION_THRESHOLD = "CameraActivity.mMotionThreshold";

	/**
	 * Intent extra key to force single shot
	 */
	public static final String FORCE_SINGLE_SHOT = "force-single-shot";

	public static final String PICTURES_LIST = "pictures-list";

	/**
	 * Handle device motion
	 */
	private MotionDetector mMotionDet;

	/**
	 * Device is moving. Shot can be smooth. Disable camera shot
	 */
	private boolean mSmoothPicture;  // if true shot will be smooth

	private boolean mShotReady;     // Need to perform auto-shot inside stabilizer

	private boolean mShotOnTouch;

	/**
	 * Handle orientation changes
	 */
	private OrientationEventListener mOrientationListener;

	/**
	 * Cached angle. Initial value is equivalent of 0 degree, but not zero for initial state
	 */
	private int mOrientationAngle = 360;

	private int mOrientationValue = OrientationEventListener.ORIENTATION_UNKNOWN;

	/**
	 * Flash on/off
	 * Turn on by default
	 */
	private boolean mFlashMode;

	/**
	 * Camera has flash
	 */
	private boolean mFlashAvailable;

	/**
	 * Stabilizer on/off
	 */
	private boolean mStabMode;

	/**
	 * Main camera mode. False for single and true for a batch mode.
	 */
	private boolean mBatchMode;

	/**
	 * Detect and show document corners
	 */
	private boolean mShowDocument = true;

	// Some useful widgets
	private CameraView mCameraView;
	private ImageButton mButtonShot;
	private TextView mTextShotCounter;
	private CameraOverlay mCameraOverlay;
	private ImageCheckBox mCheckStabOverlay;
	private ImageCheckBox mCheckFlashMode;
	private ImageCheckBox mCheckStabilizer;
	private ImageCheckBox mCheckBatchMode;
	private ImageCheckBox mCheckAccept;
	private ImageCheckBox mCheckCorners;

	// Controls to be rotated
	private List<View> mRotatedWidgets;

	/**
	 * Gone from Application
	 */
	private SdkFactory mSdkFactory;

	/**
	 * Count of successful shots in batch mode
	 */
	private int mShotCounter;

	/**
	 * A directory for picture taken.
	 */
	private File mPictureSink;

	/**
	 * name for {code:SharedPreferences}
	 */
	private String mPrefsName;

	/**
	 * Request camera in single mode (mPageUUID specified)
	 */
	private boolean mForcedSingleShot;

	/**
	 * Files
	 */
	private final ArrayList<Uri> mPictureUris = new ArrayList<>();

	/**
	 * Show camera messages
	 */
	private static final long CONSOLE_DELAY_SHORT = 3000;
	private static final long CONSOLE_DELAY_LONG = 5000;
	private ConsoleView mConsoleView;

	private int mMotionThreshold = 40;   // some default

	private final View.OnClickListener mClickListener = new View.OnClickListener() {
		@Override
		public void onClick(final View v) {
			if (v == mButtonShot) {
				onCameraShot(v);
			} else if (v == mCheckFlashMode) {
				onCameraFlash(v);
			} else if (v == mCheckStabilizer) {
				onCameraStabilize(v);
			} else if (v == mCheckBatchMode) {
				onCameraBatch(v);
			} else if (v == mCheckCorners) {
				onCameraCorners(v);
			} else if (v == mCheckAccept) {
				onCameraAccept(v);
			}
		}
	};

	/**
	 * Create intent to start camera
	 * @param context
	 * @param fileSink
	 * @return Camera intent
	 */
	public static Intent newIntent(@NonNull Context context,
	                               @NonNull SdkFactory factory,
	                               @NonNull String fileSink,
	                               @Nullable String prefsName,
	                               boolean forceSingleShot) {
		Intent intent = new Intent(context, CameraActivity.class);
		intent.putExtra(EXTRA_SDK_FACTORY, new ParcelableHolder(factory));
		intent.putExtra(EXTRA_PICTURE_SINK, fileSink);
		intent.putExtra(PREFERENCES_NAME, prefsName);
		intent.putExtra(FORCE_SINGLE_SHOT, forceSingleShot);

		return intent;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);

		// Orientation must be declared in manifest
		if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
			Log.w(AppLog.TAG, "Orientation PORTRAIT must be declared in manifest");
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}

		final Intent in = getIntent();

		mSdkFactory = ((ParcelableHolder)in.getParcelableExtra(EXTRA_SDK_FACTORY)).get();

		// Request directory for pictures
		mPictureSink = new File(in.getStringExtra(EXTRA_PICTURE_SINK));
		if (!mPictureSink.exists() || !mPictureSink.isDirectory() || !mPictureSink.canWrite()) {
			Log.w(AppLog.TAG, "Invalid picture sink specified: " + Utils.toDebugString(mPictureSink));
			finish();
		}

		// preferences name
		mPrefsName = in.getStringExtra(PREFERENCES_NAME);

		// Force Single Shot
		mForcedSingleShot = in.getBooleanExtra(FORCE_SINGLE_SHOT, mForcedSingleShot);

		// Setup orientation listener.
		// NOTE: onConfigurationChanged doesn't call if orientation is fixed
		mOrientationListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_UI) {
			@Override
			public void onOrientationChanged(int orientation) {
				// Call method
				changeOrientation(orientation);
			}
		};

		// Create motion detector
		mMotionDet = new MotionDetector(this);
		mMotionDet.setCallbacks(this);

		// Setup controls
		mCameraView = (CameraView) findViewById(R.id.view_camera);
		mButtonShot = (ImageButton) findViewById(R.id.button_camera_shot);
		mTextShotCounter = (TextView) findViewById(R.id.text_shot_counter);
		mCameraOverlay = (CameraOverlay) findViewById(R.id.view_camera_overlay);
		mCameraOverlay.setSdkFactory(mSdkFactory);

		mCheckStabOverlay = (ImageCheckBox) findViewById(R.id.check_shake_overlay);
		mCheckFlashMode = (ImageCheckBox) findViewById(R.id.check_camera_flash);
		mCheckStabilizer = (ImageCheckBox) findViewById(R.id.check_camera_stabilizer);
		mCheckBatchMode = (ImageCheckBox) findViewById(R.id.check_camera_batch);

		// Select one from two
		//mCheckAccept = (ImageCheckBox) findViewById(R.id.check_camera_accept);  // may be null!
		mCheckCorners = (ImageCheckBox) findViewById(R.id.check_camera_corners);    // may be null!

		// Add click listeners
		mButtonShot.setOnClickListener(mClickListener);
		mCheckFlashMode.setOnClickListener(mClickListener);
		mCheckStabilizer.setOnClickListener(mClickListener);
		mCheckBatchMode.setOnClickListener(mClickListener);
		mCheckCorners.setOnClickListener(mClickListener);

		// Setup camera view
		mCameraView.setSdkFactory(mSdkFactory);
		mCameraView.setCameraOverlay(mCameraOverlay);
		mCameraView.setCallback(this);

		// Open Camera
		RuntimePermissions.instance().runWithPermission(this, Manifest.permission.CAMERA,
				R.string.permission_query_camera, new RuntimePermissions.Callback() {
					@Override
					public void permissionRun(String permission, boolean granted) {
						if (granted) {
							// Workaround continuous focus
							mCameraView.post(new Runnable() {
								@Override
								public void run() {
									mCameraView.openCamera();
								}
							});
						} else {
							finish();
						}
					}
				});


		// Setup rotation controls
		mRotatedWidgets = new ArrayList<View>(10);
		mRotatedWidgets.add(mCheckStabOverlay);
		mRotatedWidgets.add(mCheckFlashMode);
		mRotatedWidgets.add(mCheckStabilizer);
		mRotatedWidgets.add(mCheckBatchMode);
		if (mCheckAccept != null) {
			mRotatedWidgets.add(mCheckAccept);
		}
		if (mCheckCorners != null) {
			mRotatedWidgets.add(mCheckCorners);
		}
		mRotatedWidgets.add(findViewById(R.id.wrapper_camera_shot));

		// Setup message console
		mConsoleView = (ConsoleView) findViewById(R.id.camera_console);

		// Read defaults
		final Resources res = getResources();
		mFlashMode = res.getBoolean(R.bool.camera_flash_mode);
		mStabMode = res.getBoolean(R.bool.camera_stab_mode);
		mShowDocument = res.getBoolean(R.bool.camera_show_document);
		mShotOnTouch = res.getBoolean(R.bool.camera_shot_on_touch);

		// Read all params from preferences
		if (mPrefsName != null) {
			final SharedPreferences prefs = getSharedPreferences(mPrefsName, MODE_PRIVATE);
			mFlashMode = prefs.getBoolean(PREFS_FLASH_MODE, mFlashMode);
			mStabMode = prefs.getBoolean(PREFS_STAB_MODE, mStabMode);
			mBatchMode = prefs.getBoolean(PREFS_BATCH_MODE, mBatchMode);
			mShowDocument = prefs.getBoolean(PREFS_SHOW_DOCUMENT, mShowDocument);
			mMotionThreshold = prefs.getInt(PREFS_MOTION_THRESHOLD, mMotionThreshold);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		RuntimePermissions.instance().handleRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	/**
	 *
	 * @return
	 */
	private boolean isBatchMode() {
		return !mForcedSingleShot && mBatchMode;
	}

	private boolean cameraStable() {
		return !mStabMode || !mSmoothPicture;
	}

	private boolean allowShot()	{
		return mCameraView.isCameraReady() && cameraStable() && (isBatchMode() || mShotCounter <= 1);
	}

	private boolean cameraReady() {
		return mCameraView.isCameraReady();
	}

	/**
	 * Some helper.
	 * @param name
	 * @param value
	 * @return true if value is null or value equals name
	 */
	private boolean check(final String value, final String name) {
		if (value == null || name == null) {
			return true;
		} else {
			return name.equals(value);
		}
	}

	private void savePrefs(String name) {
		if (mPrefsName != null) {
			SharedPreferences prefs = getSharedPreferences(mPrefsName, MODE_PRIVATE);
			SharedPreferences.Editor ed = prefs.edit();

			if (check(PREFS_FLASH_MODE, name)) {
				ed.putBoolean(PREFS_FLASH_MODE, mFlashMode);
			}

			if (check(PREFS_STAB_MODE, name)) {
				ed.putBoolean(PREFS_STAB_MODE, mStabMode);
			}

			if (check(PREFS_BATCH_MODE, name)) {
				ed.putBoolean(PREFS_BATCH_MODE, mBatchMode);
			}

			if (check(PREFS_SHOW_DOCUMENT, name)) {
				ed.putBoolean(PREFS_SHOW_DOCUMENT, mShowDocument);
			}

			ed.apply();
		}
	}

	private void updateFlashMode() {
		// Flash button
		mCameraView.setFlashMode(mFlashMode);
		mFlashAvailable = mCameraView.isFlashAvailable();
		mFlashMode = mCameraView.getFlashMode();

		updateFlashButton();
	}

	private void updateFlashButton() {
		mCheckFlashMode.setVisibility(mFlashAvailable ? View.VISIBLE : View.INVISIBLE);
		mCheckFlashMode.setChecked(mFlashMode);
		mCheckFlashMode.setEnabled(cameraReady());
	}

	private void updateStabMode() {
		// Stabilizer
		mCheckStabilizer.setChecked(mStabMode);
		//mCheckStabilizer.setEnabled(mCameraView.isCameraReady());

		// NOTE: need to clean animation before change visibility!!!
		mCheckStabOverlay.clearAnimation();
		mCheckStabOverlay.setVisibility(mStabMode ? View.VISIBLE : View.INVISIBLE);

		updateSmoothPicture();
	}

	private void updateSmoothPicture() {
		mCheckStabOverlay.setChecked(mSmoothPicture);
		mButtonShot.setEnabled(cameraReady());
	}

	private void updateBatchMode() {
		mCheckBatchMode.setChecked(isBatchMode());
		mCheckBatchMode.setVisibility(mForcedSingleShot ? View.INVISIBLE : View.VISIBLE);
		mTextShotCounter.setVisibility(isBatchMode() ? View.VISIBLE : View.INVISIBLE);

		updateShotCounter();
	}

	void updateShotCounter() {
		// Cannot change batch mode after at least one shot
		mCheckBatchMode.setEnabled(mShotCounter == 0);
		//mCheckBatchMode.setVisibility(mShotCounter == 0 ? View.VISIBLE : View.INVISIBLE);
		mTextShotCounter.setText(Integer.toString(mShotCounter));

		// Accept check box used as multi state button
		// Shows as "done" at lease one shot
		if (mCheckAccept != null) {
			mCheckAccept.setChecked(isBatchMode() && mShotCounter > 0);
		}
	}

	private void updateShowDocument() {
		mCameraView.setShowDocumentCorners(mShowDocument);
		mShowDocument = mCameraView.getShowDocumentCorners();

		if (mCheckCorners != null) {
			mCheckCorners.setChecked(mShowDocument);
			mCheckCorners.setEnabled(mCameraView.isCameraReady());
		}
	}

	private void updateWidgets() {
		updateFlashMode();
		updateStabMode();
		updateBatchMode();
		updateShowDocument();
	}

	@Override
	public void finish() {

		if (mShotCounter > 0) {
			Intent data = new Intent();
			// Put last picture to Intent data
			if (mPictureUris.size() > 0) {
				data.setData(mPictureUris.get(mPictureUris.size() - 1));
			}
			// Put picture list even empty
			data.putParcelableArrayListExtra(PICTURES_LIST, mPictureUris);
			setResult(RESULT_OK, data);
		} else {
			// No pictures
			setResult(RESULT_CANCELED);
		}
		super.finish();
	}

	@Override
	public void onStart() {
		super.onStart();

		// Start to watching motion
		mMotionDet.start();

		// Start to watching orientation
		if (mOrientationListener.canDetectOrientation()) {
			mOrientationListener.enable();
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		// Update ALL widgets
		updateWidgets();
	}

	@Override
	public void onPause() {
		super.onPause();

		// Save all preferences
		savePrefs(null);

		// Turn flash off
		mCameraView.setFlashMode(false);
	}

	@Override
	public void onStop() {
		super.onStop();

		// Stop motion watching
		mMotionDet.release();

		// Stop watching orientation
		if (mOrientationListener.canDetectOrientation()) {
			mOrientationListener.disable();
		}

		mConsoleView.getConsole().clear();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_camera, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void changeOrientation(int orientation) {
		// Validate orientation
		if (orientation != OrientationEventListener.ORIENTATION_UNKNOWN && orientation < 0) {
			throw new IllegalArgumentException("Invalid device orientation value " +
					Integer.toString(orientation));
		}

		//Log.d(AppLog.TAG, "Camera orientation " + orientation);

		if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
			if (mOrientationValue != OrientationEventListener.ORIENTATION_UNKNOWN) {
				// Use previous defined orientation
				orientation = mOrientationValue;
			} else {
				// Nothing to do on unknown orientation (e.g. phone is horizontal)
				return;
			}

		} else {
			// Store last known orientation value
			mOrientationValue = orientation;
		}

		// Setup camera shutter
		mCameraView.setShutterRotation(orientation);

		// Quantize to quadrants
		final int newAngle = CameraUtils.quantizeDegreeTo360(orientation, 90, 45);

		// Trace orientation
		// TODO: remove after debug
		// NOTE: What's strange fluctuations on Samsung Nexus phone?
		//Log.d(SharpScanLog.TAG, String.format("Orientation %3d : %3d", orientation, newAngle));

		if (newAngle != mOrientationAngle) {
			mOrientationAngle = CameraUtils.normalizeDegreeTo360(mOrientationAngle);    // for first call

			final int delta = CameraUtils.normalizeDegreeTo180(mOrientationAngle - newAngle);

			// We rotate controls in other hand than orientation
			final float oldRotationAngle = CameraUtils.normalizeDegreeTo180(360 - mOrientationAngle);
			final float newRotationAngle = oldRotationAngle + delta;
			mOrientationAngle = newAngle;

			// Simple rotate, or animate controls
			final int duration =
					getResources().getInteger(R.integer.camera_buttons_rotate_duration_ms);
			for (final View view : mRotatedWidgets) {

				// NOTE: Invisible items shows on animation
				if (duration <= 0 || view.getVisibility() != View.VISIBLE) {
					// simple set rotation INSTEAD animation
					view.setRotation(newRotationAngle);
				} else {
					// Create animation from current rotation to the delta
					RotateAnimation anim = new RotateAnimation(0, delta,
							Animation.RELATIVE_TO_SELF, 0.5f,
							Animation.RELATIVE_TO_SELF, 0.5f);
					anim.setDuration(duration); // milliseconds, usually 500
					anim.setRepeatCount(0);     // one time animation

					// Animation doesn't change view properties. Restore it after end
					// Reset animation for all views when finished
					anim.setAnimationListener(new Animation.AnimationListener() {
						@Override
						public void onAnimationStart(Animation animation) {
							// Dummy
						}

						@Override
						public void onAnimationEnd(Animation animation) {

							if (view.getAnimation() == animation) {
								view.clearAnimation();
								view.setRotation(newRotationAngle);
							}
						}

						@Override
						public void onAnimationRepeat(Animation animation) {
							// Dummy
						}
					});

					// Now, starts!
					view.startAnimation(anim);
				}
			}

			// Move message console
			int dockSide;
			RelativeLayout.LayoutParams params = null;
			switch (mOrientationAngle) {
				case 90:
					dockSide = ConsoleView.DOCK_SIDE_LEFT;
					params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
					params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
					params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
					params.addRule(RelativeLayout.ABOVE, R.id.pane_camera_shot);
					break;
				case 180:
					dockSide = ConsoleView.DOCK_SIDE_TOP;
					params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
					params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
					break;
				case 270:
					dockSide = ConsoleView.DOCK_SIDE_RIGHT;
					params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
					params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
					params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
					params.addRule(RelativeLayout.ABOVE, R.id.pane_camera_shot);
					break;
				default:
					dockSide = ConsoleView.DOCK_SIDE_BOTTOM;
					params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
					params.addRule(RelativeLayout.ABOVE, R.id.pane_camera_shot);
			}
			mConsoleView.setLayoutParams(params);
			mConsoleView.setDockSide(dockSide);
		}
	}

	@Override
	public void onMotionDetect(MotionDetector source, double motionModule) {
		final boolean smoothPicture = motionModule > (mMotionThreshold / 100.0);
		if (smoothPicture != mSmoothPicture) {
			// Call on changes only
			mSmoothPicture = smoothPicture;
			updateSmoothPicture();
			//Log.v(AppLog.TAG, String.format("Motion module %5g", motionModule));
		}

		// Perform auto-shot
		if (mShotReady && !mSmoothPicture) {
			mShotReady = !takePicture(null, true);
		}
	}

	// Make shot by camera tap or by button click
	private boolean takePicture(CameraView.TouchParams params, boolean silent) {
		if (!silent) {
			if (!mCameraView.isCameraReady()) {
				// Camera in auto focus progress
				mConsoleView.getConsole().appendLine(ConsoleTag.CameraBusy, R.string.camera_shot_busy, CONSOLE_DELAY_SHORT);
				return false;
			}

			if (!cameraStable()) {
				// Camera is shaking
				mConsoleView.getConsole().appendLine(ConsoleTag.CameraShaking, R.string.camera_shot_not_stable, CONSOLE_DELAY_SHORT);
				mShotReady = true;  // Try to shot inside stabilizer
				return false;
			}
		}

		if (!allowShot()) {
			return false;
		}

		// Take a shot
		final boolean shotDone = mCameraView.makeShot(true, params);
		if (shotDone) {
			mCameraOverlay.showAlert(true, 1000);    // display corners in RED
			updateWidgets();
		}
		return shotDone;
	}

	@Override
	public void onCameraReady(CameraView inst, boolean isCameraReady) {
		updateFlashButton();
		updateSmoothPicture();
	}

	@Override
	public void onPictureReady(final CameraView view, final byte[] pictureBuffer) {
		if (view != mCameraView) {
			// WTF???
			throw new IllegalStateException("take a picture from another camera?");
		}

		if (pictureBuffer == null) {
			throw new IllegalStateException("Picture buffer is null");
		}

		// Get system time to use as picture file name and sequence counter
		final long sequenceCounter = System.currentTimeMillis();
		// Build picture name from
		String fileName = String.format("shot-%016X.jpg", sequenceCounter);
		final File pictureFile = new File(mPictureSink, fileName);

		RuntimePermissions.instance().runWithPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE,
				permission_query_write_storage, (permission, granted) -> {
					if (granted) {
						try (FileOutputStream fos = new FileOutputStream(pictureFile)) {
							fos.write(pictureBuffer);
						} catch (IOException e) {
							Log.d(AppLog.TAG, "Cannot process camera picture", e);
						}

						// Some cameras don't write attributes to exif
						ensureExif(pictureFile, view.readFlashMode(), view.readShutterRotation());

						// Only really saved images!
						mShotCounter++;

						// Notify application for a new image
						final Uri pictureUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pictureFile);
						grantUriPermission(getPackageName(), pictureUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
						mPictureUris.add(pictureUri);

						// Close camera activity for single shot mode
						if (!isBatchMode()) {
							finish();
						}
					}
				});

		mCameraOverlay.showAlert(false, 0);
		updateWidgets();
	}

	private static void ensureExif(File pictureFile, boolean flashMode, int rotation) {

		try {
			boolean changed = false;
			ExifInterface exif = new ExifInterface(pictureFile);
			if (flashMode) {
				final int flashFlag = exif.getAttributeInt(ExifInterface.TAG_FLASH, 0);
				if (flashFlag == 0) {
					exif.setAttribute(ExifInterface.TAG_FLASH, "1");
					changed = true;
				}
			}

			int orientation;
			switch (rotation) {
				case 0:
					orientation = ExifInterface.ORIENTATION_NORMAL;
					break;
				case 90:
					orientation = ExifInterface.ORIENTATION_ROTATE_90;
					break;
				case 180:
					orientation = ExifInterface.ORIENTATION_ROTATE_180;
					break;
				case 270:
					orientation = ExifInterface.ORIENTATION_ROTATE_270;
					break;
				default:
					orientation = ExifInterface.ORIENTATION_UNDEFINED;
			}

			if (orientation != exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
				exif.setAttribute(ExifInterface.TAG_ORIENTATION, Integer.toString(orientation));
				changed = true;
			}

			if (changed) {
				exif.saveAttributes();
			}
		} catch (IOException e) {
			// Ignore errors
		}
	}

	@Override
	public void onPictureError(CameraView view, int error) {
		switch (error) {
			case Error.AUTO_FOCUS_FAILED:
			case Error.AUTO_FOCUS_TIMEOUT:
			case Error.SWITCH_MODE_FAILED:
				// Auto focus failed
				mConsoleView.getConsole().appendLine(ConsoleTag.FocusFailed, R.string.camera_shot_focus_failed, CONSOLE_DELAY_SHORT);
				break;
			case Error.INTERNAL_ERROR:
				finish();
				break;
		}

		updateWidgets();
	}

	@Override
	public void onShotReady(CameraView inst) {
		if (cameraStable()) {
			// Simple shot (silent ???)
			takePicture(null, false);
		} else {
			// Preform a shot inside stabilizer
			mShotReady = true;
		}
	}

	private enum ConsoleTag {
		DetectCorners,
		CameraBusy,
		CameraShaking,
		FocusFailed,
		InternalError,
	}

	@Override
	public void onDetectCorners(CameraView inst, PointF[] points, int failure, float failedRate) {
		if (failure == FindDocCornersThread.CORNERS_DETECTED) {
			// Success
			mConsoleView.getConsole().removeLine(ConsoleTag.DetectCorners);
		} else {
			// Corners not detected!
			String text;
			switch (failure) {
				// Don't use failure string
				case FindDocCornersThread.CORNERS_UNCERTAIN:
					text = getString(R.string.camera_looking_for_document);
					break;
				default:
					text = AppSdkFactory.verboseDetectionFailure(getApplicationContext(), failure, failedRate);
			}

			mConsoleView.getConsole().appendLine(ConsoleTag.DetectCorners, text, ConsoleView.INFINITE);
		}
	}

	/**
	 * [Shot] button handler
	 * @param v
	 */
	public void onCameraShot(View v) {
		takePicture(null, false);
	}

	/**
	 * [Flash] button handler
	 * @param v
	 */
	public void onCameraFlash(View v) {
		mFlashMode = !mFlashMode;
		updateFlashMode();
		savePrefs(PREFS_FLASH_MODE);
	}

	public void onCameraStabilize(View v) {
		mStabMode = !mStabMode;
		updateStabMode();
		savePrefs(PREFS_STAB_MODE);
	}

	public void onCameraBatch(View v) {
		// We can change batch mode before any shots
		if (mShotCounter == 0) {
			mBatchMode = !mBatchMode;
		} else {
			mBatchMode = true;
		}

		// To change accept button state
		updateBatchMode();
		savePrefs(PREFS_BATCH_MODE);
	}

	public void onCameraAccept(View v) {
		// Finita la comedia
		finish();
	}

	public void onCameraCorners(View v) {
		mShowDocument = !mShowDocument;
		updateShowDocument();
		savePrefs(PREFS_SHOW_DOCUMENT);
	}

	// Callbacks from CameraView
	@Override
	public void onCameraOpen(CameraView view, boolean succeeded) {
		updateWidgets();
	}

	@Override
	public void onTouchPreview(CameraView inst, CameraView.TouchParams params) {
		if (inst != mCameraView) {
			// WTF???
			throw new IllegalStateException("take a picture from another camera?");
		}

		if (mShotOnTouch) {
			takePicture(params, false);
		} else {
			mCameraView.autoFocus();
		}
	}
}
