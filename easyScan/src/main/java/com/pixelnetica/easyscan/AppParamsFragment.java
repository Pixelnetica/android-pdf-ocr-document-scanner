package com.pixelnetica.easyscan;

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

			final boolean prefsForceManualCrop = preferences.getBoolean(MainIdentity.PREFS_FORCE_MANUAL_CROP, false);
			final boolean prefsAutoCropOnOpen = preferences.getBoolean(MainIdentity.PREFS_AUTO_CROP_ON_OPEN, false);

			mForceManualCrop.setChecked(prefsForceManualCrop);
			mAutoCropOnOpen.setChecked(prefsAutoCropOnOpen);

			// if "Force manual crop" is set, auto crop is nonsense
			mAutoCropOnOpen.setVisibility(prefsForceManualCrop ? View.INVISIBLE : View.VISIBLE);
			mForceManualCrop.setOnCheckedChangeListener(
					(button, checked) -> mAutoCropOnOpen.setVisibility(checked ? View.INVISIBLE : View.VISIBLE));
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
