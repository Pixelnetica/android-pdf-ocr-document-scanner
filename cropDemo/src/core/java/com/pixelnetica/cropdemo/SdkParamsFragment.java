package com.pixelnetica.cropdemo;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

/**
 * Created by Denis on 17.09.2016.
 */
public class SdkParamsFragment extends Fragment implements ISettingsFragment {
	// Stub

	@Override
	public boolean save(@NonNull MainIdentity identity) {
		return true;
	}
}
