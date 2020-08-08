package com.pixelnetica.easyscan;

import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/**
 * Created by Denis on 13.03.2017.
 */

public class CutoutParamsFragment extends Fragment implements ISettingsFragment {
	// Stub

	@Override
	public boolean save(@NonNull SharedPreferences.Editor editor) {
		return true;
	}
}
