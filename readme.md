# ScreenLife Capture Android App

> [!NOTE]
> ## About this fork
>
> This is a fork of [ScreenLife-Capture-Team/app](https://github.com/ScreenLife-Capture-Team/app),
> adapted for the doctoral dissertation of Sarah Beham.
>
> **Dissertation title:**
> *Social Media: Zwischen Aufmerksamkeit und Wert: Welche Videokurzformate junge Erwachsene auf Instagram nutzen – und was daraus für den Journalismus folgt*
>
> We modified ScreenLife Capture so that it only captures screenshots from Instagram. All credit for the original application goes to the
> ScreenLife Capture Team; the underlying method is described in
> [Chua et al., *Behavior Research Methods* (2023)](https://link.springer.com/article/10.3758/s13428-022-02006-z).
>
> Changes in this fork:
> - Renamed the application package to match the study
> - Restricted screenshot capture to Instagram only
> - Added a screenshot review screen so participants can see and delete captured images
> - Replaced QR-code registration with a per-participant build-time configuration
> - Reworked notifications and various capture/upload fixes
>
> ### Building
>
> Study-specific values are **not** committed. Copy `local.properties.example` to
> `local.properties` and fill in the upload endpoint and the participant key/hash,
> then build one APK per participant:
>
> ```
> cp local.properties.example local.properties
> $EDITOR local.properties
> ./gradlew assembleRelease
> ```
>
> `participantKey` and `participantHash` must each be 64-character hex values.
> The participant configuration is compiled into the APK via `BuildConfig`, so you must
> rebuild and reinstall the app whenever you change these values.
>
> The same values can be supplied as the environment variables `SLC_UPLOAD_ADDRESS`,
> `SLC_PARTICIPANT_KEY` and `SLC_PARTICIPANT_HASH`.

## End-to-end setup

This fork only covers the Android app. A full study setup also needs:

- a backend upload endpoint
- participant-specific keys
- a built APK for each participant
- a researcher-side tool to retrieve and manage uploaded screenshots

The original ScreenLife Capture ecosystem used these companion repositories:

- [cloud-functions](https://github.com/ScreenLife-Capture-Team/cloud-functions): Google Cloud Functions backend for receiving uploads
- [DMPO](https://github.com/ScreenLife-Capture-Team/DMPO): researcher-side tool for onboarding, downloading, and managing screenshots
- [researcher guide](https://andrewzhyee.com/screenlife/): original setup guide for non-programming deployment

If someone opens this repository, the required workflow is:

1. Set up the backend upload endpoint.
2. Generate a participant key and matching participant hash.
3. Build one APK per participant with those values compiled in.
4. Install the APK on the participant's Android device.
5. Let the participant grant permissions and start capture.
6. Receive uploaded screenshots in the backend.
7. Use DMPO to download and manage the uploaded files.

## System overview

- This repository: Android app used by participants to capture and upload screenshots.
- Cloud Functions: receives uploads from the app and stores them in the study backend.
- DMPO: researcher-side desktop tool for participant onboarding, data download, and management.

## How screenshot transfer works

At a high level, the app does the following:

1. Captures a screenshot on the device.
2. Stores a timestamped file locally.
3. Creates an encrypted copy for upload.
4. Uploads batches of screenshots to the configured `uploadAddress`.

In the current app code, uploads are sent by `UploadService` as `multipart/form-data` HTTP POST requests to `UPLOAD_ADDRESS`.
The original `cloud-functions` repository documents an upload function that receives these files on the backend.

## Google Cloud setup

This repository does not contain the Google Cloud deployment itself. For backend setup, consult:

- [cloud-functions](https://github.com/ScreenLife-Capture-Team/cloud-functions) for the original Google Cloud Functions code
- [researcher guide](https://andrewzhyee.com/screenlife/) for the original end-to-end setup documentation

At minimum, you need a deployed upload endpoint URL and must place that URL into `uploadAddress` in `local.properties`.

The original `cloud-functions` repository documents these backend functions:

- `register`
- `upload_file`
- `count_files`

For this fork, the practically relevant one is the upload endpoint used by the app.

> [!IMPORTANT]
> ## ⚠️ REPOSITORY DEPRECATED ⚠️
> 
> **Note:** This repository is no longer actively maintained. All development has moved to the following new repositories:
> 
> - [ScreenLife Capture Collection](https://github.com/ScreenLife-Capture-Team/screenlife-capture-collection) - Mobile app for data collection + Cloud Infrastructure for secure data storage and processing
> 
> Please use these new repositories for the latest code and features.

### Changelog
#### v. 1.1.0

- Fixed multiple bugs regarding QR scanner, starting issue on Pixel devices
- Added a descriptor field in file names

#### v. 1.1.0
#### 28 Feb 2023

- Added notice screens when user intentionally pause/resume recording
- Updated gradle for SDK 32 (Android 12), fixed issue with intent passing for android 12
- Updated gradle and fixed method calls for notification and datetime utils for SDK 25 and below (Android 7 and below)

This repo contains the Android Application used in the ScreenLife Capture study. The application allows participants to record and upload screenshots taken every X number of seconds. The general layout of the code is explained below.

## Usage guide

### Installing via Android Studio (debug mode)

Refer to the [official guide here](https://developer.android.com/studio/run/device) for running an app on a physical device.

Summary of steps:

- Ensure your Android device has USB debugging mode turned on.
- Connect your Android device using data cable and select file transfer mode. Android Studio should automatically register your device.
- In Android Studio, select your device from the device list on the top toolbar. Click "Run app". The app should be installed in your device.

### Packaging APK for distribution

For each participant, repeat this process:

1. Generate a participant key and matching hash.
2. Put both values into `local.properties`.
3. Build the APK.
4. Install that APK on the participant's device.
5. If you change participant values, rebuild and reinstall.

### Starting the app for one participant

This fork no longer uses QR-code registration. Instead, each participant gets their own
build with a fixed participant key and participant hash.

1. Copy `local.properties.example` to `local.properties`.
2. Set `uploadAddress`.
3. Set `participantKey` to the participant's 64-character hex key.
4. Set `participantHash` to the participant's 64-character hex hash.
5. Build the app.
6. Reinstall the app on the device.
7. Open the app and start capture.

Example:

```properties
uploadAddress=https://example.invalid/upload
participantKey=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef
participantHash=abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789
```

On macOS, you can generate a random participant key and derive the matching hash like this:

```sh
key=$(openssl rand -hex 32)
hash=$(printf '%s' "$key" | xxd -r -p | shasum -a 256 | awk '{print $1}')
printf 'participantKey=%s\nparticipantHash=%s\n' "$key" "$hash"
```

`participantKey` is a random 32-byte value encoded as 64 hex characters.
`participantHash` is the SHA-256 hash of that key, also encoded as 64 hex characters.

If `participantKey` or `participantHash` is missing, the app will start but will not store
screenshots for capture.

For a debug build in Android Studio or from the terminal:

```sh
sh gradlew :app:assembleDebug
```

For a release APK:

```sh
sh gradlew :app:assembleRelease
```

## DMPO setup and data retrieval

To download and manage uploaded screenshots, use the original DMPO repository:

- [DMPO](https://github.com/ScreenLife-Capture-Team/DMPO)

According to the DMPO README, the basic startup steps are:

```sh
npm install
npm start
```

or:

```sh
electron .
```

The DMPO README also notes that `bucket_key.json` and `settings.json` must be present and configured.
If you want automated censoring in DMPO, the `censoring-scripts` repository must be placed next to the `DMPO` folder as described in the DMPO README.

## Researcher checklist

Use this order if you want a single checklist for the whole workflow:

1. Prepare or deploy the Google Cloud backend.
2. Note the upload endpoint URL.
3. Generate a participant key.
4. Derive the matching participant hash.
5. Add `uploadAddress`, `participantKey`, and `participantHash` to `local.properties`.
6. Build the participant APK.
7. Install and test the APK on the participant device.
8. Distribute the correct APK to the participant.
9. Run DMPO on the researcher side.
10. Download and manage uploaded screenshots.



## App structure

### Activities

| Activity Name    | Purpose                                                      |
| ---------------- | ------------------------------------------------------------ |
| RegisterActivity | Handles registration for new users.                          |
| MainActivity     | Contains the main interface of the app, allowing participants to start/stop the screen capture. |
| DevToolsActivity | Contains tools to tweak how the app works, including the number of images to send per batch, the number of batches to send in parallel etc. |

### Service

| Service Name   | Purpose                                                      |
| -------------- | ------------------------------------------------------------ |
| CaptureService | Responsible for capturing screenshots every X number of seconds. Runs continously throughout the duration of the study. |
| UploadService  | Responsible for uploading of screenshots to the cloud functions. Is triggered at certain times by `UploadScheduler` |

### Other Files

| File Name          | Purpose                                                      |
| ------------------ | ------------------------------------------------------------ |
| Constants          | Contains the constants used throughout the application.      |
| Batch              | Contains a "batch" of files, used by `UploadService`.        |
| Encryptor          | Used during the encryption process by `CaptureService`.      |
| InfoDialog         | The dialog that is shown when the "information" button is pressed on the main activity. |
| InternetConnection | A set of functions to check if the device is connected to the internet, and through what type of connection (WiFi vs mobile data). |
| Logger             | Utility functions to save logs to SharedPreferences.         |
| UploadScheduler    | Schedules the `UploadService` at certain times a day.        |



## Constants

Most app-related constants are located in the `Constants` file. The constants are explained below.

| Constant Name       | Explanation                                   |
| ------------------- | --------------------------------------------- |
| REGISTER_ADDRESS    | The address of the "register" cloud function. |
| UPLOAD_ADDRESS      | The address of the "upload" cloud function.   |
| COUNT_ADDRESS       | The address of the "count" cloud function.    |
| BATCH_SIZE_DEFAULT  | TODO                                          |
| MAX_TO_SEND_DEFAULT | TODO                                          |
| MAX_BATCHES_TO_SEND | TODO                                          |
| REQ_TIMEOUT         | TODO                                          |


