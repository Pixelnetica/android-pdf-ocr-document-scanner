# Scan any paper documents into clear PDF with ease on Android

This is simple document scanner application which could scan any paper documents into clean and legible PDF (and image) files with ease on Android smartphones and tablets.

It has been build with [Document Scanner SDK](https://www.pixelnetica.com/products/document-scanning-sdk/document-scanner-api-features.html?utm_source=EasyScan&utm_medium=src-android&utm_campaign=read_me&utm_content=dssdk-features "Document Scanner SDK: Main Features and Benefits") (_DSSDK_) as a demo application for Android Studio.

__⚠️ Note.__  
Application source code provided __“as is”__ without warranties of any kind. It could be freely used in commercial product __only__ in case of commercial DSSDK license purchase. 

## EasyScan on Google Play
For the sake of convenience this demo application (full featured version) available on Google Play as free application - EasyScan.  

📱 [__"EasyScan: PDF Document Scanner"__](https://play.google.com/store/apps/details?id=com.pixelnetica.easyscan).

## Documentation
The documentation for DSSDK Android version can be found by the link below:

📖 [Pixelnetica Document Scanner SDK for Android documentation](https://pixelnetica.com/docs/document-scanner-sdk/android/introduction.html "Document Scanner SDK for Android Documentation")

## Specify the repository

The Document Scanning SDK for Android is available from the Pixelnetica Maven repository. This
is the **recommended** route: it is anonymous — no account, no access token, nothing to request
— so this project builds from a fresh clone. The declaration is in the **project**-level Gradle
script:

   ```kotlin
   repositories {
     maven {
       // Pixelnetica Document Scanning SDK
       url = uri("https://maven.pixelnetica.com/")
     }
   }
   ```

Every file it serves is signed. The public key is at `https://maven.pixelnetica.com/KEYS`, and
[Verifying the artifacts](https://pixelnetica.com/docs/document-scanner-sdk/android/add-to-android-project.html#4-verifying-the-artifacts)
has the fingerprint, the command that checks a download, and the key-server configuration for
Gradle's dependency verification.

### The GitHub Packages route (still supported, sunsetting)

The SDK is also published to GitHub Packages, which will keep receiving releases for at least
two more versions and from which nothing already published is ever removed. That route needs a
GitHub account and a **classic** access token with the `read:packages` scope — GitHub requires
authentication for every package download, even for public packages. Put the credentials in
`local.properties` (**do not** put that file under version control):

   ```kotlin
   repositories {
     maven {
       // Reference to Scanning SDK library
       url = uri("https://maven.pkg.github.com/Pixelnetica/artifacts")
       credentials {
         username = "<YOUR GITHUB USER NAME>"
         password = "<YOUR GITHUB TOKEN>"
       }
     } 
   }
   ```

New projects should prefer the anonymous repository above. See
[Add to a project](https://pixelnetica.com/docs/document-scanner-sdk/android/add-to-android-project.html)
for which SDK versions each repository serves.


### Auto Crop
To enable or disable AutoCrop just switch `Auto-Crop on open` in `Settings` menu.

### Build variants
In order to build properly working application please utilize only __`debug`__  or __`signed`__ build variants.

## License

By default application source code bundled with _Demo license_ which puts watermarks on resulted images and OCR results. 
To get full featured _Free Trial license_ please [__contact us__](https://www.pixelnetica.com/products/document-scanning-sdk/sdk-support.html?utm_source=EasyScan&utm_medium=src-android&utm_campaign=read_me&utm_content=dssdk-support "Request information or Free Trial DSSDK license").

It is strictly prohibited to distribute, market, publish to application stores like, but not limited, AppStore, Google Play, etc or use other than for development, staging or testing purposes Pixelnetica DSSDK trial or demo licenses.

## About Pixelnetica Document Scanning SDK

[Pixelnetica Document Scanning SDK](https://www.pixelnetica.com/products/document-scanning-sdk/document-scanner-sdk.html?utm_source=EasyScan&utm_medium=src-android&utm_campaign=read_me&utm_content=dssdk-overview "Document Scanning SDK: Overview") (_DSSDK_) provides developers with an intelligent, highly efficient toolkit, which offers an easy way to add image processing features that are optimized for document photos that are taken by a mobile device or document camera to their applications.

For more information about DSSDK main Features and Benefits please visit [Pixelnetica website](https://www.pixelnetica.com/products/document-scanning-sdk/document-scanner-api-features.html?utm_source=EasyScan&utm_medium=src-android&utm_campaign=read_me&utm_content=dssdk-features "Document Scanning SDK: Main Features and Benefits"). 

## Have Questions, need Free Demo license or Quotation?

Feel free to contact us to request free trial license, price quotation or in case of any inquires at [Pixelnetica DSSDK Support](https://www.pixelnetica.com/products/document-scanning-sdk/sdk-support.html?utm_source=EasyScan&utm_medium=src-android&utm_campaign=read_me&utm_content=dssdk-support "Contact Pixelnetica support for Free trial, Quotation or incase of any questions").
