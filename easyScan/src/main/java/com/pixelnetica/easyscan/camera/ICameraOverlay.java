package com.pixelnetica.easyscan.camera;

import android.graphics.PointF;

/**
 * Created by Denis on 28.02.2015.
 */
public interface ICameraOverlay {
	void showCorners(boolean shown);
	void showAlert(boolean alert, int delay);

	// 4 points: top-left, top-right, bottom-left, bottom-right
	void setDocumentCorners(PointF[] points);
}
