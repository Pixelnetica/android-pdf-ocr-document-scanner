package com.pixelnetica.easyscan

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class EasyScanSettings {
    object ProfileSerializer: Serializer<UserProcessing> {
        override val defaultValue: UserProcessing
            get() = UserProcessing
                .newBuilder()
                .setProfile(UserProcessing.ColorProfile.BITONAL)
                .setStrongShadows(false)
                .build()

        override suspend fun readFrom(input: InputStream): UserProcessing =
            readProto {
                UserProcessing.parseFrom(input)
            }

        override suspend fun writeTo(t: UserProcessing, output: OutputStream) {
            withContext(Dispatchers.IO) {
                t.writeTo(output)
            }
        }
    }

    object PaperPropertiesSerializer: Serializer<PaperProperties> {
        override val defaultValue: PaperProperties
            get() = PaperProperties
                .newBuilder()
                .setId(-4)  // -4 means Page.Paper.Predefined.Size.A4
                .setOrientation(PaperProperties.Orientation.AUTO)
                .build()

        override suspend fun readFrom(input: InputStream): PaperProperties =
            readProto {
                PaperProperties.parseFrom(input)
            }

        override suspend fun writeTo(t: PaperProperties, output: OutputStream) {
            withContext(Dispatchers.IO) {
                t.writeTo(output)
            }
        }
    }

    object AppSettingsSerializer: Serializer<AppSettings> {
        override val defaultValue: AppSettings
            get() = AppSettings
                .newBuilder()
                .setAppTheme(AppSettings.AppTheme.AUTO)
                .setDynamicColors(false)
                .setStandardTheme(false)
                .setAutoDetectOrientation(true)
                .setShowPdfHiddenText(false)
                .setImageCompression(AppSettings.ImageCompression.LOW)
                .build()
        override suspend fun readFrom(input: InputStream): AppSettings =
            readProto {
                AppSettings.parseFrom(input)
            }

        override suspend fun writeTo(t: AppSettings, output: OutputStream) =
            withContext(Dispatchers.IO) {
                t.writeTo(output)
            }
    }
}

/**
 * Helper function to read settings and wrap exceptions
 */
private suspend fun <T> readProto(block: () -> T) =
    withContext(Dispatchers.IO) {
        try {
            block()
        } catch (e: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", e)
        }
    }

val Context.userProcessingDataStore: DataStore<UserProcessing> by dataStore(
    "userproc.pb",
    EasyScanSettings.ProfileSerializer,
)

val Context.paperPropertiesDataStore: DataStore<PaperProperties> by dataStore(
    "paper_props.pb",
    EasyScanSettings.PaperPropertiesSerializer,
)

val Context.appSettingsDataStore: DataStore<AppSettings> by dataStore(
    "app_settings.pb",
    EasyScanSettings.AppSettingsSerializer,
)