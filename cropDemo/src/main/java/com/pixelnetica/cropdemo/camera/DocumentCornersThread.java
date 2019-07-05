package com.pixelnetica.cropdemo.camera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.pixelnetica.docimageproc.Corners;
import com.pixelnetica.docimageproc.DocImageProc;
import com.pixelnetica.docimageproc.Metaimage;
import com.pixelnetica.docimageproc.PxDocCorners;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;

/**
 * Created by Denis on 25.06.2015.
 */
public class DocumentCornersThread extends HandlerThread {
	private static final String TAG = "CameraDocDemo";

	private static final int EXIT_THREAD = 0;
	private static final int DETECT_CORNERS = 1;

	private static final int [] mTaskTypes = {EXIT_THREAD, DETECT_CORNERS};

	private DocImageProc mSDK;

	// Handler for main processing
	private Handler mWorkerHandler;

	// To notify UI
	private final Handler mNotifyHandler = new Handler();

	public DocumentCornersThread() {
		super("DocumentCornersThread");
	}

	@Override
	protected void onLooperPrepared() {
		super.onLooperPrepared();

		// Create SDK instance in a thread!
		mSDK = new DocImageProc();

		// Create message handler for the loop
		mWorkerHandler = new Handler(getLooper(), new Handler.Callback() {
			@Override
			public boolean handleMessage(Message msg) {
				if (msg.what == EXIT_THREAD) {
					// Try to close message loop
					if (!quit()) {
						// Hardly interrupt thread if something wrong with looper
						interrupt();
					}
					return true;
				} else if (msg.what == DETECT_CORNERS) {
					final Params params = (Params) msg.obj;
					process(mSDK, params);
					mNotifyHandler.post(new Runnable() {
						@Override
						public void run() {
							params.documentCornersFound();
						}
					});

					return true;
				} else {
					// Something else?
					return false;
				}
			}
		});
	}

	@Override
	public void run() {
		super.run();

		// Need to close SDK
		if (mSDK != null) {
			mSDK.close();
			mSDK = null;
		}
	}

	public void finish()
	{
		// Just in case.
		if (!isAlive()) {
			return;
		}

		synchronized (mWorkerHandler) {
			// Remove all pending messages if any
			for (int type : mTaskTypes) {
				if (type != EXIT_THREAD) {
					mWorkerHandler.removeMessages(type);
				}
			}

			// Send message to quit
			mWorkerHandler.sendEmptyMessage(EXIT_THREAD);
		}

		// Wait for thread termination
		try {
			join();
		} catch (InterruptedException ex)
		{
			// Nothing
		}
	}

	static public abstract class Params
	{
		final byte [] pictureBuffer;
		final int pictureFormat;
		final Point pictureSize;

		// Optional parameters
		int lowThreshold = -1;
		int highThreshold = -1;

		// Will be set after processing
		Corners documentCorners;
		boolean smartCrop;

		public Params(byte [] buffer, int format, Point size)
		{
			this.pictureBuffer = buffer;
			this.pictureFormat = format;
			this.pictureSize = size;
		}

		public void setDocumentThresholds(int low, int high) {
			this.lowThreshold = low;
			this.highThreshold = high;
		}

		public YuvImage createImage()
		{
			if (pictureFormat == ImageFormat.NV21 || pictureFormat == ImageFormat.YUY2) {
				return new YuvImage(pictureBuffer, pictureFormat, pictureSize.x, pictureSize.y, null);
			} else {
				// Unsupported format
				return null;
			}
		}

		public Bitmap decodeBuffer()
		{
			YuvImage image = createImage();
			if (image == null) {
				return null;
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			image.compressToJpeg(new Rect(0, 0, pictureSize.x, pictureSize.y), 100, out);

			ByteArrayInputStream input = new ByteArrayInputStream(out.toByteArray());
			return BitmapFactory.decodeStream(input);
		}

		// Processing complete. {@code #documentCorners} may be null
		public abstract void documentCornersFound();
	}

	public void processDocumentCorners(Params params)
	{
		//addThreadTask(PROCESS_CORNERS, params, SINGLE_TASK, false);
		final int type = DETECT_CORNERS;
		synchronized (mWorkerHandler) {
			// Remove all pending messages if any
			mWorkerHandler.removeMessages(type);

			// Create new message
			final Message msg = mWorkerHandler.obtainMessage(type, params);

			mWorkerHandler.sendMessage(msg);
		}
	}

	// Main task
	private static void process(DocImageProc sdk, Params params) {
		// First, decode picture to bitmap
		Bitmap previewBitmap = params.decodeBuffer();
		if (previewBitmap == null) {
			return;
		}

		if (!sdk.validate()) {
			// Something wrong
			return;
		}

		Bitmap sourceBitmap;
		Point inputSize;
		try {
			// Need scale input?
			inputSize = sdk.supportImageSize(params.pictureSize);
			if (inputSize.equals(params.pictureSize)) {
				// No scale need. Check picture format
				if (previewBitmap.getConfig() == Bitmap.Config.ARGB_8888) {
					sourceBitmap = previewBitmap;
				} else {
					sourceBitmap = previewBitmap.copy(Bitmap.Config.ARGB_8888, true);
				}
			} else {
				// Scale bitmap
				sourceBitmap = Bitmap.createScaledBitmap(previewBitmap.copy(Bitmap.Config.ARGB_8888, true), inputSize.x, inputSize.y, true);
			}

			PxDocCorners corners = new PxDocCorners();
			corners.lowT = params.lowThreshold;
			corners.hiT = params.highThreshold;

			Metaimage image = new Metaimage(sourceBitmap);
			// NOTE: Viewfinder always rotated to 90 degree
			image.setOrientation(Metaimage.ImageOrientation.IO_Right);
			if (sdk.detectDocumentCorners(image, false, corners)) {
				params.smartCrop = corners.isSmartCropMode;
				if (params.smartCrop) {
					params.documentCorners = corners.createCorners();
				} else if (detectOk(corners)) {
					params.documentCorners = corners.createCorners();
				} else {
					params.documentCorners = null;
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "DocImage SDK internal exception", e);
		} catch (Error e) {
 			Log.e(TAG, "DocImage SDK internal error", e);
		}
	}

	private static boolean detectOk(PxDocCorners pd) {
		return true;
		/*return pd.lft >= pd.lowT &&
				pd.top >= pd.lowT &&
				pd.rht >= pd.lowT &&
				pd.btm >= pd.lowT;*/
	}
}

