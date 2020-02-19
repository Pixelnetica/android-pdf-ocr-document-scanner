package com.pixelnetica.cropdemo;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

/**
 * Created by Denis on 17.09.2016.
 */
public class AppParamsFragment extends Fragment implements ISettingsFragment {
	private CheckBox mForceManualCrop;
	private CheckBox mAutoCropOnOpen;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final View root = inflater.inflate(R.layout.fragment_app_params, container, false);

		mForceManualCrop = root.findViewById(R.id.settings_app_force_manual_crop);
		mAutoCropOnOpen = root.findViewById(R.id.settings_app_auto_crop_on_open);

		// Initialize from preferences
		if (savedInstanceState == null) {
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());

			mForceManualCrop.setChecked(preferences.getBoolean(MainIdentity.PREFS_FORCE_MANUAL_CROP, false));
			mAutoCropOnOpen.setChecked(preferences.getBoolean(MainIdentity.PREFS_AUTO_CROP_ON_OPEN, false));

			mAutoCropOnOpen.setVisibility(mForceManualCrop.isChecked() ? View.GONE : View.VISIBLE);
			mForceManualCrop.setOnCheckedChangeListener((button, checked) -> {
				mAutoCropOnOpen.setVisibility(checked ? View.GONE : View.VISIBLE);
			});
		}

		return root;
	}

	@Override
	public boolean save(@NonNull SharedPreferences.Editor editor) {
		editor.putBoolean(MainIdentity.PREFS_FORCE_MANUAL_CROP, mForceManualCrop.isChecked());
		editor.putBoolean(MainIdentity.PREFS_AUTO_CROP_ON_OPEN, mAutoCropOnOpen.isChecked());
		return true;
	}
}
