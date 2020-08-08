package com.pixelnetica.easyscan;


import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import android.util.Log;

import com.pixelnetica.imagesdk.ImageFileWriter;
import com.pixelnetica.imagesdk.ImageSdkLibrary;
import com.pixelnetica.imagesdk.ImageWriter;
import com.pixelnetica.imagesdk.ImageWriterException;
import com.pixelnetica.imagesdk.MetaImage;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

class SaveImageTask extends AsyncTask<SaveImageTask.SaveImageParam, Void, SaveImageTask.SaveImageResult> {

	// Save format
	@Retention(RetentionPolicy.SOURCE)
	@IntDef({SAVE_JPEG, SAVE_TIFF_G4, SAVE_PNG_MONO, SAVE_PDF, SAVE_PDF_PNG})
	@interface SaveFormat {}
	static final int SAVE_JPEG = 0;
	static final int SAVE_TIFF_G4 = 1;
	static final int SAVE_PNG_MONO = 2;
	static final int SAVE_PDF = 3;
	static final int SAVE_PDF_PNG = 4;

	// PDF configurations
	@Retention(RetentionPolicy.SOURCE)
	@IntDef({PDF_CONFIG_DEFAULT, PDF_CONFIG_PREFEFINED, PDF_CONFIG_CUSTOM, PDF_CONFIG_EXTENSIBLE})
	@interface PdfConfig {}
	static final int PDF_CONFIG_DEFAULT = 0;
	static final int PDF_CONFIG_PREFEFINED = 1;
	static final int PDF_CONFIG_CUSTOM = 2;
	static final int PDF_CONFIG_EXTENSIBLE = 3;


	static class SaveImageParam {
		@NonNull final String path;
		@NonNull final MetaImage image;
		@SaveFormat final int format;
		@PdfConfig final int config;
		final boolean simulatePages;

		SaveImageParam(@NonNull String path, @NonNull MetaImage image, @SaveFormat int format, @PdfConfig int config, boolean simulatePages) {
			this.path = path;
			this.image = image;
			this.format = format;
			this.simulatePages = simulatePages;
			this.config = config;
		}
	}

	static class ImageFile {
		@NonNull final String filePath;
		@NonNull final String mimeType;

		ImageFile(@NonNull String filePath, @NonNull String mimeType) {
			this.filePath = filePath;
			this.mimeType = mimeType;
		}
	}

	static class SaveImageResult extends TaskResult {
		@NonNull final ArrayList<ImageFile> imageFiles = new ArrayList<>();
		SaveImageResult() {
			super();
		}

		SaveImageResult(int error) {
			super(error);
		}

	}

	interface Listener {
		void onSaveImageComplete(@NonNull    SaveImageTask task, @NonNull SaveImageResult result);
	}


	@NonNull private final SdkFactory mFactory;
	@NonNull private final Listener mListener;
	SaveImageTask(@NonNull SdkFactory factory, @NonNull Listener listener) {
		this.mFactory = factory;
		this.mListener = listener;
	}

	// Helper
	@NonNull
	private static String changeFileExtension(@NonNull String file, @NonNull String ext) {
		int slashPos = file.lastIndexOf(File.pathSeparator);
		int dotPos = file.lastIndexOf('.');
		if (dotPos > slashPos) {
			return file.substring(0, dotPos) + ext;
		} else {
			return file + ext;
		}
	}

	@Override
	protected SaveImageResult doInBackground(SaveImageParam... params) {
		SaveImageResult result = new SaveImageResult();
		for (SaveImageParam param : params) {
			// Define image writer
			String fileExt, mimeType;
			boolean subFile = false;
			Bundle writerParams = null;
			@ImageSdkLibrary.ImageWriterType int type;
			int pageCount = 1;
			switch (param.format) {
				case SAVE_JPEG:
					type = ImageSdkLibrary.IMAGE_WRITER_JPEG;
					fileExt = ".jpg";
					mimeType = "image/jpeg";
					break;
				case SAVE_TIFF_G4:
					type = ImageSdkLibrary.IMAGE_WRITER_TIFF;
					fileExt = ".tif";
					mimeType = "image/tiff";
					pageCount = param.simulatePages ? 3 : 1;
					break;
				case SAVE_PNG_MONO:
					type = ImageSdkLibrary.IMAGE_WRITER_PNG_EXT;
					fileExt = ".png";
					mimeType = "image/png";
					break;
				case SAVE_PDF_PNG:
					subFile = true;
					// NOTE: no break!
				case SAVE_PDF:
					type = ImageSdkLibrary.IMAGE_WRITER_PDF;
					fileExt = ".pdf";
					mimeType = "application/pdf";
					pageCount = param.simulatePages ? 3 : 1;

					// Configure PDF paper size
					writerParams = new Bundle();
					switch (param.config) {
						case PDF_CONFIG_DEFAULT:
							// Stay default
							break;
						case PDF_CONFIG_PREFEFINED:
							// Overwrite default
							writerParams.putInt(ImageWriter.CONFIG_PAGE_PAPER, ImageWriter.PAPER_HALF_LETTER);
							break;
						case PDF_CONFIG_CUSTOM:
							// Setup custom page size (A5)
							writerParams.putFloat(ImageWriter.CONFIG_PAGE_WIDTH, 148);
							writerParams.putFloat(ImageWriter.CONFIG_PAGE_HEIGHT, 210);
							break;
						case PDF_CONFIG_EXTENSIBLE:
							// Setup horizontal page size as extensible
							writerParams.putFloat(ImageWriter.CONFIG_PAGE_WIDTH, ImageWriter.Extensible);
							writerParams.putFloat(ImageWriter.CONFIG_PAGE_HEIGHT, 210);
							break;
					}

					break;

				default:
					Log.e(AppLog.TAG, "Unknown save format " + param.format);
					continue;
			}

			// Build file path
			String filePath = changeFileExtension(param.path, fileExt);

			try (ImageWriter writer = mFactory.createImageWriter(type)) {

				writer.open(filePath);
				for (int i = 0; i < pageCount; ++i) {
					if (writerParams != null) {
						writer.configure(writerParams);
					}
					if (subFile) {
						// Special case for PDF/PNG
						String subFilePath;
						try (ImageWriter subWriter = mFactory.createImageWriter(ImageSdkLibrary.IMAGE_WRITER_PNG_EXT)) {
							subWriter.open(changeFileExtension(filePath, ".png"));
							subFilePath = subWriter.write(param.image);
						}
						ImageFileWriter fileWriter = (ImageFileWriter) writer;
						fileWriter.writeFile(subFilePath, ImageFileWriter.IMAGE_FILE_PNG, param.image.getExifOrientation());
					} else {
						writer.write(param.image);
					}
				}

			} catch (ImageWriterException e) {
				Log.e(AppLog.TAG, "Error on save file", e);
				return new SaveImageResult(TaskResult.CANTSAVEFILE);
			}

			result.imageFiles.add(new ImageFile(filePath, mimeType));
		}
		return result;
	}

	@Override
	protected void onPostExecute(SaveImageResult saveImageResult) {
		mListener.onSaveImageComplete(this, saveImageResult);
	}
}
