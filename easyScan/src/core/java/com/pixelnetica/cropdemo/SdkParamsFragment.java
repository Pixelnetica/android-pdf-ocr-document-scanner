package com.pixelnetica.easyscan;

import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/**
 * Created by Denis on 17.09.2016.
 */
public class SdkParamsFragment extends Fragment implements ISettingsFragment {
	// Stub

	@Override
	public boolean save(@NonNull SharedPreferences.Editor editor) {
		return true;
	}
}
