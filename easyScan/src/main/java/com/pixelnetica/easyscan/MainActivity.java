package com.pixelnetica.easyscan;


import com.pixelnetica.easyscan.camera.CameraActivity;
import com.pixelnetica.easyscan.util.RuntimePermissions;
import com.pixelnetica.imagesdk.MetaImage;

import android.Manifest;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

	private static final int PICK_IMAGE = 100;
	private static final int TAKE_PHOTO = 101;
	private static final int SHOW_SETTINGS = 102;

	// Main Identity
	MainIdentity mIdentity;
	static final String BUNDLE_IDENTITY = "BUNDLE_IDENTITY";

	// Support toolbar
	Toolbar mToolbar;

	// Main button bar
	ViewGroup mButtonBar;

	// Crop button bar
	Toolbar mCropToolbar;
	ViewGroup mCropButtonBar;

    // Display and size
    CropImageView mImageView;
    ProgressBar mProgressWait;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

	    // Create and setup Identity
	    mIdentity = new ViewModelProvider(this).get(MainIdentity.class);

	    mIdentity.onUpdateUI.observe(this, (Void value) -> updateUI());
	    mIdentity.onErrorMessage.observe(this, this::showProcessingError);
	    mIdentity.onSaveComplete.observe(this, this::onSaveComplete);


	    if (savedInstanceState == null) {
		    // Setup Identity on startup
	    	mIdentity.loadSettings();
	    }

        // Add buttons to action bar
	    mToolbar = findViewById(R.id.app_toolbar);
	    setSupportActionBar(mToolbar);  // NOTE:

    	// Setup main buttons handlers
        mButtonBar = mToolbar;
        mButtonBar.findViewById(R.id.btn_open_image).setOnClickListener(this);
	    mButtonBar.findViewById(R.id.btn_take_photo).setOnClickListener(this);
        mButtonBar.findViewById(R.id.btn_crop_image).setOnClickListener(this);
        mButtonBar.findViewById(R.id.btn_save_image).setOnClickListener(this);

        // Setup crop button bar
        mCropToolbar = findViewById(R.id.crop_toolbar);
        mCropButtonBar = mCropToolbar.findViewById(R.id.crop_button_bar);
        mCropButtonBar.findViewById(R.id.btn_rotate_left).setOnClickListener(this);
        mCropButtonBar.findViewById(R.id.btn_rotate_right).setOnClickListener(this);
        mCropButtonBar.findViewById(R.id.btn_revert_selection).setOnClickListener(this);
        mCropButtonBar.findViewById(R.id.btn_expand_selection).setOnClickListener(this);

        // Setup display
        mImageView = findViewById(R.id.image_holder);
        mImageView.setSdkFactory(mIdentity.SdkFactory);
		mProgressWait = findViewById(R.id.progress_wait);

		// Initial update
	    updateUI();
    }

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	//super.onPrepareOptionsMenu(menu);
    	MenuItem item;

    	// Force manual crop
    	item = menu.findItem(R.id.action_manual_crop);
	    item.setVisible(mIdentity.canPerformManualCrop());

    	// Strong shadows
    	item = menu.findItem(R.id.action_strong_shadows);
    	item.setChecked(mIdentity.isStrongShadows());

    	// Processing profile
    	switch (mIdentity.getProcessingProfile()) {
		    case ProcessImageTask.NoBinarization:
			    item = menu.findItem(R.id.action_profile_nobinarization);
			    item.setChecked(true);
			    break;
	        case ProcessImageTask.BWBinarization:
	            item = menu.findItem(R.id.action_profile_bwbinarization);
	            item.setChecked(true);
	            break;
	        case ProcessImageTask.GrayBinarization:
	            item = menu.findItem(R.id.action_profile_graybinarization);
	            item.setChecked(true);
	            break;
	        case ProcessImageTask.ColorBinarization:
	            item = menu.findItem(R.id.action_profile_colorbinarization);
	            item.setChecked(true);
	            break;
    	}

    	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        final int id = item.getItemId();
        if (id == R.id.action_manual_crop) {
	        mIdentity.manualCrop();
	        return true;
        } else if (id == R.id.action_settings) {
	        startActivityForResult(new Intent(this, SettingsActivity.class), SHOW_SETTINGS);
	        return true;
        } else if (id == R.id.action_strong_shadows) {
        	// Invert strong shadows
        	mIdentity.setStrongShadows(!mIdentity.isStrongShadows());
            return true;
        }
        else if (id == R.id.action_profile_nobinarization) {
	        mIdentity.setProcessingProfile(ProcessImageTask.NoBinarization);
	        return true;
        }
        else if (id == R.id.action_profile_bwbinarization) {
        	mIdentity.setProcessingProfile(ProcessImageTask.BWBinarization);
        	return true;
        }
        else if (id == R.id.action_profile_graybinarization) {
        	mIdentity.setProcessingProfile(ProcessImageTask.GrayBinarization);
        	return true;
        }
        else if (id == R.id.action_profile_colorbinarization) {
        	mIdentity.setProcessingProfile(ProcessImageTask.ColorBinarization);
        	return true;
        }
        else if (id == R.id.action_about) {
        	// Show advanced processing dialog (old mode)
			onShowAbout();
			return true;
        }
        return super.onOptionsItemSelected(item);
    }

	public static CharSequence getFormattedText(Context context, int id, Object... args) {
		for (int i = 0; i < args.length; ++i) {
			args[i] = (args[i] instanceof String) ? TextUtils.htmlEncode((String) args[i]) : args[i];
		}
		return Html.fromHtml(
				String.format(
						Html.toHtml(
								new SpannableString(
										Html.fromHtml(
												context.getText(id).toString()
										)
								)), args));
	}
	private void onShowAbout() {

		String versionName;
		String applicationId = getApplication().getPackageName();
		final ApplicationInfo appInfo = getApplicationInfo();
		try {
			final PackageInfo pkgInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			versionName = pkgInfo.versionName;
		} catch (PackageManager.NameNotFoundException e) {
			versionName = "";
		}

		// Prepare text view for HTML content
		//final SpannableString msg = new SpannableString(Html.fromHtml(getText(R.string.about_message).toString()));
		final CharSequence msg = new SpannableString(getFormattedText(this,
				R.string.about_message,
				applicationId, versionName,
				BuildConfig.GIT_HASH));

		final AlertDialog dlg = new AlertDialog.Builder(this).create();
		dlg.setTitle(R.string.about_title);
		dlg.setIcon(appInfo.icon);
		dlg.setCancelable(true);
		dlg.setMessage(msg);
		dlg.setButton(AlertDialog.BUTTON_POSITIVE, getText(R.string.about_ok), (DialogInterface dialog, int which) -> {
			dialog.dismiss();
		});

		dlg.show();

		// Show links
		final TextView textView = (TextView)dlg.findViewById(android.R.id.message);
		if (textView != null) {
			textView.setMovementMethod(LinkMovementMethod.getInstance());
		}
	}

	@Override
	public void onClick(View v) {
		switch( v.getId() )
		{
		case R.id.btn_open_image:
			onOpenImage();
			break;
		case R.id.btn_take_photo:
			onTakePhoto();
			break;
		case R.id.btn_crop_image:
			onCropImage();
			break;
		case R.id.btn_save_image:
			onSaveImage();
			break;
		case R.id.btn_rotate_left:
			mImageView.rotateLeft();
			break;
		case R.id.btn_rotate_right:
			mImageView.rotateRight();
			break;
		case R.id.btn_revert_selection:
			mImageView.revertSelection();
			break;
		case R.id.btn_expand_selection:
			mImageView.expandSelection();
			break;
		}
	}

	private void selectPicturesGranted(String title, boolean multiple) {
		final Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_PICK);
		if (multiple && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		}
		startActivityForResult(Intent.createChooser(intent, title), PICK_IMAGE);
	}

	public void selectPictures(final String title, final boolean multiple) {
		RuntimePermissions.instance().runWithPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE,
				R.string.permission_query_read_storage, new RuntimePermissions.Callback() {
					@Override
					public void permissionRun(String permission, boolean granted) {
						if (granted) {
							selectPicturesGranted(title, multiple);
						}
					}
				});
	}


	public void selectPictures(int resID, boolean multiple) {
		selectPictures(getString(resID), multiple);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		RuntimePermissions.instance().handleRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	void onOpenImage() {
		selectPictures(R.string.select_picture_title, false);
    }

	void onTakePhoto() {
		final File fileSink = new File(getFilesDir(), "camera_files");
		fileSink.mkdirs();

		// Query permissions and create directories
		RuntimePermissions.instance().runWithPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE,
				R.string.permission_query_write_storage,
				(permission, granted) -> {
					if (granted && (fileSink.exists() || fileSink.mkdirs())) {
						// Common routine to start camera
						Intent intent = CameraActivity.newIntent(
								MainActivity.this,
								mIdentity.SdkFactory,
								fileSink.getAbsolutePath(),
								"camera-prefs",
								true);
						startActivityForResult(intent, TAKE_PHOTO);
					}
				});
	}

	void onCropImage() {
		// Start crop pipeline or enter to manual mode
		if (mIdentity.hasCorners()) {
			// Manual crop mode
			mIdentity.cropImage(mImageView.getCropData());
		} else {
			mIdentity.detectDocument();
		}
	}

	void onSaveImage() {
    	// Show save dialog
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(true).setTitle(R.string.btn_save_image).setIcon(R.drawable.ic_action_save).setView(R.layout.dialog_save);
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				RuntimePermissions.instance().runWithPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE,
						R.string.permission_query_write_storage, new RuntimePermissions.Callback() {
							@Override
							public void permissionRun(String permission, boolean granted) {
								if (granted) {
									// NOTE: all dialog params is gone through mIdentity
									mIdentity.saveImage(getApplicationContext());
								}
							}
						});
			}
		});
		AlertDialog dialog = builder.create();
		dialog.show();

		// Get controls
		final RadioGroup saveFormat = dialog.findViewById(R.id.save_format_group);
		final View pdfTitle = dialog.findViewById(R.id.save_format_pdf_config_title);
		final RadioGroup pdfConfig = dialog.findViewById(R.id.save_format_pdf_config_group);
		final CheckBox simulatePages = dialog.findViewById(R.id.save_format_simulate_pages);

		// Setup dialog controls
		switch (mIdentity.getSaveFormat()) {
			case SaveImageTask.SAVE_JPEG:
				pdfTitle.setVisibility(View.GONE);
				pdfConfig.setVisibility(View.GONE);
				simulatePages.setVisibility(View.GONE);
				saveFormat.check(R.id.save_format_jpeg);
				break;
			case SaveImageTask.SAVE_TIFF_G4:
				pdfTitle.setVisibility(View.GONE);
				pdfConfig.setVisibility(View.GONE);
				simulatePages.setVisibility(View.VISIBLE);
				saveFormat.check(R.id.save_format_tiff);
				break;
			case SaveImageTask.SAVE_PNG_MONO:
				pdfTitle.setVisibility(View.GONE);
				pdfConfig.setVisibility(View.GONE);
				simulatePages.setVisibility(View.GONE);
				saveFormat.check(R.id.save_format_png);
				break;
			case SaveImageTask.SAVE_PDF:
				pdfTitle.setVisibility(View.VISIBLE);
				pdfConfig.setVisibility(View.VISIBLE);
				simulatePages.setVisibility(View.VISIBLE);
				saveFormat.check(R.id.save_format_pdf);
				break;
			case SaveImageTask.SAVE_PDF_PNG:
				pdfTitle.setVisibility(View.VISIBLE);
				pdfConfig.setVisibility(View.VISIBLE);
				simulatePages.setVisibility(View.VISIBLE);
				saveFormat.check(R.id.save_format_pdf_png);
				break;
		}

		saveFormat.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				switch (checkedId) {
					case R.id.save_format_jpeg:
						pdfTitle.setVisibility(View.GONE);
						pdfConfig.setVisibility(View.GONE);
						simulatePages.setVisibility(View.GONE);
						mIdentity.setSaveFormat(SaveImageTask.SAVE_JPEG);
						break;
					case R.id.save_format_tiff:
						pdfTitle.setVisibility(View.GONE);
						pdfConfig.setVisibility(View.GONE);
						simulatePages.setVisibility(View.VISIBLE);
						mIdentity.setSaveFormat(SaveImageTask.SAVE_TIFF_G4);
						break;
					case R.id.save_format_png:
						pdfTitle.setVisibility(View.GONE);
						pdfConfig.setVisibility(View.GONE);
						simulatePages.setVisibility(View.GONE);
						mIdentity.setSaveFormat(SaveImageTask.SAVE_PNG_MONO);
						break;
					case R.id.save_format_pdf:
						pdfTitle.setVisibility(View.VISIBLE);
						pdfConfig.setVisibility(View.VISIBLE);
						simulatePages.setVisibility(View.VISIBLE);
						mIdentity.setSaveFormat(SaveImageTask.SAVE_PDF);
						break;
					case R.id.save_format_pdf_png:
						pdfTitle.setVisibility(View.VISIBLE);
						pdfConfig.setVisibility(View.VISIBLE);
						simulatePages.setVisibility(View.VISIBLE);
						mIdentity.setSaveFormat(SaveImageTask.SAVE_PDF_PNG);
						break;
				}
			}
		});

		switch (mIdentity.getPdfConfig()) {
			case SaveImageTask.PDF_CONFIG_DEFAULT:
				pdfConfig.check(R.id.save_format_pdf_config_default);
				break;
			case SaveImageTask.PDF_CONFIG_PREFEFINED:
				pdfConfig.check(R.id.save_format_pdf_config_predefined);
				break;
			case SaveImageTask.PDF_CONFIG_CUSTOM:
				pdfConfig.check(R.id.save_format_pdf_config_custom);
				break;
			case SaveImageTask.PDF_CONFIG_EXTENSIBLE:
				pdfConfig.check(R.id.save_format_pdf_config_extensible);
				break;
		}
		pdfConfig.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				switch (checkedId) {
					case R.id.save_format_pdf_config_default:
						mIdentity.setPdfConfig(SaveImageTask.PDF_CONFIG_DEFAULT);
						break;
					case R.id.save_format_pdf_config_predefined:
						mIdentity.setPdfConfig(SaveImageTask.PDF_CONFIG_PREFEFINED);
						break;
					case R.id.save_format_pdf_config_custom:
						mIdentity.setPdfConfig(SaveImageTask.PDF_CONFIG_CUSTOM);
						break;
					case R.id.save_format_pdf_config_extensible:
						mIdentity.setPdfConfig(SaveImageTask.PDF_CONFIG_EXTENSIBLE);
						break;
				}
			}
		});

		simulatePages.setChecked(mIdentity.getSimulatePages());
		simulatePages.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mIdentity.setSimulatePages(isChecked);
			}
		});
	}

	void onSaveComplete(@NonNull List<SaveImageTask.ImageFile> files) {
		Intent shareIntent = null;
		ArrayList<Uri> content = new ArrayList<>();
		if (files.size() == 1) {
		    Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", new File(files.get(0).filePath));
		    shareIntent = new Intent(Intent.ACTION_SEND);
		    shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
		    shareIntent.setType(files.get(0).mimeType);
		    content.add(fileUri);
	    } else if (files.size() > 1) {

			Pair<String, String> mimeType = null;
    		for (SaveImageTask.ImageFile file : files) {
			    Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", new File(file.filePath));
			    content.add(fileUri);

			    // Parse mime types
			    String[] mime = file.mimeType.split("/");
			    if (mimeType == null) {
			    	mimeType = new Pair<>(mime[0], mime[1]);
			    } else {
			    	// Check subtypes
				    if (!mimeType.first.equals(mime[0])) {
					    mimeType = new Pair<>("*", mimeType.second);
				    }
			    	if (!mimeType.second.equals(mime[1])) {
			    		mimeType = new Pair<>(mimeType.first, "*");
				    }
			    }
		    }

			shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
    		shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, content);
    		shareIntent.setType(String.format("%s/%s", mimeType.first, mimeType.second));

	    }

	    if (shareIntent != null) {
			Intent chooserIntent = Intent.createChooser(shareIntent, getString(R.string.share_output_title));
		    List<ResolveInfo> resolveList = getPackageManager().queryIntentActivities(chooserIntent, PackageManager.MATCH_DEFAULT_ONLY);
		    for (ResolveInfo resolve : resolveList) {
			    String packageName = resolve.activityInfo.packageName;
			    for (Uri shareUri : content) {
				    this.grantUriPermission(packageName, shareUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
			    }
		    }

		    startActivity(chooserIntent);
	    }
	}

    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);

    	switch( requestCode) {
		    case PICK_IMAGE:
		    case TAKE_PHOTO:
			    if (resultCode == RESULT_OK) {
				    Uri selectedImage = data.getData();
				    mIdentity.reset();
				    mIdentity.openImage(selectedImage);
			    }
			    break;
		    case SHOW_SETTINGS:
		    	mIdentity.loadSettings();
		    	updateUI();
		    	break;
	    }
    }

	void updateUI() {
		setButtonsState();
		updateWaitState();
		setDisplayImage();
	}

    public void setButtonsState()
    {
		final boolean manualCropAvailable = mIdentity.isReady() && mIdentity.hasSourceImage() && mIdentity.hasCorners();
		final boolean processingAvailable = mIdentity.isReady() && mIdentity.hasSourceImage();
		final boolean resultAvailable = mIdentity.isReady() && mIdentity.hasTargetImage();

        // NOTE: Open & Camera buttons are always available

        // Crop button is enabled when we have a source image and no target image
        // and no current processing
        setupButtonVisible(mButtonBar, R.id.btn_crop_image, processingAvailable);

        // Save button is enabled when we have a target image
        setupButtonVisible(mButtonBar, R.id.btn_save_image, resultAvailable);

	    // Show or hide crop button bar
	    setupButtonVisible(mCropToolbar, 0, processingAvailable);

	    // Corners buttons available only in crop mode
	    setupButtonVisible(mCropButtonBar, R.id.btn_revert_selection, manualCropAvailable);
	    setupButtonVisible(mCropButtonBar, R.id.btn_expand_selection, manualCropAvailable);
    }

    private static void setupButtonVisible(@NonNull View container, int id, boolean visible) {

    	final View button = (id > 0) ? container.findViewById(id) : container;
    	if (button == null) {
		    throw new IllegalStateException("Button is null!");
	    }

    	if (visible) {
    		button.setVisibility(View.VISIBLE);
    	} else {
    		button.setVisibility(View.GONE);
    	}
    }

    public void setDisplayImage() {
	    Bitmap bitmap = mIdentity.queryDisplayBitmap();
    	if (bitmap != null) {
    		mImageView.setCropImage(bitmap, mIdentity.getImageMatrix(), mIdentity.getCropData());
	    } else {
    		mImageView.setCropImage(null, null, null);
	    }
    }

    public void updateWaitState() {
		if( mIdentity.isWaitState()) {
			// TODO: setup a better color filter
			mImageView.setEnabled(false);
			mImageView.setColorFilter(Color.rgb(128, 128, 128), PorterDuff.Mode.LIGHTEN);
			mProgressWait.setVisibility(View.VISIBLE);
		} else {
			mImageView.setEnabled(true);
			mImageView.setColorFilter(0, PorterDuff.Mode.DST);
			mProgressWait.setVisibility(View.INVISIBLE);
		}
    }

    private static String displayUriPath(ContentResolver cr, Uri uri) {
		if (cr == null || uri == null) {
			return "";
		}

		String path = MetaImage.getRealPathFromURI(cr, uri);
		if (path == null) {
			path = uri.toString();
		}
		return path;
	}

	public void showProcessingError(@NonNull TaskResult result) {
    	updateUI();

		// Show error toast
		switch (result.error) {
			case TaskResult.NOERROR:
				Toast.makeText(getApplicationContext(), R.string.msg_processing_complete, Toast.LENGTH_SHORT).show();
				break;
			case TaskResult.PROCESSING:
				Toast.makeText(getApplicationContext(), R.string.msg_processing_error, Toast.LENGTH_LONG).show();
				break;
			case TaskResult.OUTOFMEMORY:
				Toast.makeText(getApplicationContext(), R.string.msg_out_of_memory, Toast.LENGTH_LONG).show();
				break;
			case TaskResult.NODOCCORNERS:
				Toast.makeText(getApplicationContext(), R.string.msg_no_doc_corners, Toast.LENGTH_LONG).show();
				break;
			case TaskResult.INVALIDCORNERS:
				Toast.makeText(getApplicationContext(), R.string.msg_invalid_corners, Toast.LENGTH_LONG).show();
				break;
			case TaskResult.INVALIDFILE:
				Toast.makeText(this, String.format(getString(R.string.msg_cannot_open_file),
						displayUriPath(getContentResolver(), ((LoadImageTask.LoadImageResult) result).sourceUri)), Toast.LENGTH_LONG).show();
				break;
			case TaskResult.CANTSAVEFILE:
				Toast.makeText(this, R.string.msg_cannot_write_image_file, Toast.LENGTH_LONG).show();
				break;
			default:
				Toast.makeText(this,
						String.format(getString(R.string.msg_unknown_error), result.error), Toast.LENGTH_LONG).show();
				break;
		}
	}
}
