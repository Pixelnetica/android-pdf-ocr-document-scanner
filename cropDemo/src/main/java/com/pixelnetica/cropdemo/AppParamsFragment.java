package com.pixelnetica.cropdemo;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatSpinner;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.pixelnetica.cropdemo.util.Identity;

/**
 * Created by Denis on 17.09.2016.
 */
public class AppParamsFragment extends Fragment implements ISettingsFragment {
	private CheckBox mForceManualCrop;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final View root = inflater.inflate(R.layout.fragment_app_params, container, false);

		final MainIdentity identity = (MainIdentity) Identity.readBundle(getArguments(), SettingsActivity.ARG_MAIN_IDENTITY);

		mForceManualCrop = root.findViewById(R.id.settings_app_force_manual_crop);

		// Initialize from preferences
		if (savedInstanceState == null) {
			mForceManualCrop.setChecked(identity.getForceManualCrop());
		}

		return root;
	}

	@Override
	public boolean save(@NonNull MainIdentity identity) {
		identity.setForceManualCrop(mForceManualCrop.isChecked());
		return true;
	}
}
