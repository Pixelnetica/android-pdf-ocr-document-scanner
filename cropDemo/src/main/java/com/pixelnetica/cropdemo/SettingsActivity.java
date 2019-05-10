package com.pixelnetica.cropdemo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.BundleCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;

import com.pixelnetica.cropdemo.util.Identity;

/**
 * Created by Denis on 30.08.2016.
 */
public class SettingsActivity extends AppCompatActivity {
	public static final String ARG_PREVIEW_SIZES = "preview_sizes";
	public static final String ARG_DEFAULT_PREVIEW_SIZE = "default_preview_size";

	public static final String ARG_MAIN_IDENTITY = "main_offstage";

	// Custom Fragments
	private AppParamsFragment mAppParams;
	private SdkParamsFragment mSdkParams;
	private CutoutParamsFragment mCutoutParams;

	static Intent newIntent(@NonNull Context context, @NonNull MainIdentity identity) {
		final Intent in = new Intent(context, SettingsActivity.class);
		in.putExtra(ARG_MAIN_IDENTITY, identity);    // as Parcelable
		return in;
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		// Pass main offstage to fragments argument
		final Bundle args = getIntent().getExtras();

		// Setup app params
		if (savedInstanceState == null) {
			mAppParams = new AppParamsFragment();
			getSupportFragmentManager().beginTransaction().add(R.id.settings_app_params_holder,
					mAppParams).commit();
			mAppParams.setArguments(args);
		} else {
			mAppParams = (AppParamsFragment) getSupportFragmentManager().findFragmentById(
					R.id.settings_app_params_holder);
		}

		// Setup cutout params
		if (savedInstanceState ==  null) {
			mCutoutParams = new CutoutParamsFragment();
			getSupportFragmentManager().beginTransaction().add(R.id.settings_cutout_params_holder,
					mCutoutParams).commit();
			mCutoutParams.setArguments(args);
		} else {
			mCutoutParams = (CutoutParamsFragment) getSupportFragmentManager().findFragmentById(
					R.id.settings_cutout_params_holder);
		}

		// Setup FragmentTabHost
		if (savedInstanceState == null) {
			mSdkParams = new SdkParamsFragment();
			getSupportFragmentManager().beginTransaction().add(R.id.settings_sdk_params_holder,
					mSdkParams).commit();
			mSdkParams.setArguments(args);
		} else {
			mSdkParams = (SdkParamsFragment) getSupportFragmentManager().findFragmentById(
					R.id.settings_sdk_params_holder);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_settings, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final int id = item.getItemId();
		if (id == android.R.id.home) {
			onBackPressed();
			return true;
		} else if (id == R.id.action_apply) {

			final MainIdentity identity = (MainIdentity) Identity.readBundle(getIntent().getExtras(), ARG_MAIN_IDENTITY);

			// Save camera data
			boolean succeeded = identity != null;

			// Save general params
			if (succeeded && mAppParams != null) {
				succeeded = mAppParams.save(identity);
			}

			if (succeeded && mCutoutParams != null) {
				succeeded = mCutoutParams.save(identity);
			}

			// Save SDK params
			if (succeeded && mSdkParams != null) {
				succeeded = mSdkParams.save(identity);
			}

			if (succeeded) {
				setResult(RESULT_OK);
				finish();
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}


	public static void initCheckBox(CheckBox checkBox, boolean value) {
		if (checkBox.isChecked() == value) {
			checkBox.setChecked(!value);
			checkBox.toggle();
		} else {
			checkBox.setChecked(value);
		}
	}
}
