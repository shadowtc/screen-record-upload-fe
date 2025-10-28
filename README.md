# Screen Recording and Chunked Upload Demo (uni-app)

A uni-app demonstration application for Android that records the screen locally and uploads videos using resumable multipart upload to a MinIO backend.

## Features

### Recording (Android)
- **Native Module**: Uses MediaProjection + MediaRecorder APIs
- **Video Format**: MP4 with H.264 video codec and AAC audio codec
- **Foreground Service**: Ensures stable recording even when app is in background
- **Configurable Settings**:
  - Resolution: 480p, 720p (default), 1080p
  - Frame rate: 30 fps (default)
  - Bitrate: ~3 Mbps (default)
  - Audio recording: Optional microphone input
- **JS Bridge API**:
  - `startRecord(options)`: Start screen recording
  - `stopRecord()`: Stop recording and return file metadata
- **Permissions**: Handles screen capture and microphone permissions

### UI (uni-app)
- Simple, intuitive interface with:
  - Start/Stop recording buttons
  - Video preview player
  - Upload progress with real-time statistics
  - Speed and remaining time display
  - Retry capability for failed uploads
  - Pending uploads list with resume functionality

### Chunked Upload Client
- **Default Settings**:
  - Chunk size: 8 MB
  - Concurrency: 4 parallel uploads
  - Max retries: 5 with exponential backoff
- **Upload Flow**:
  1. POST `/api/uploads/init` - Initialize multipart upload
  2. POST `/api/uploads/{fileId}/parts` - Get presigned URLs for parts
  3. PUT to presigned URLs - Upload each chunk
  4. POST `/api/uploads/complete` - Complete upload with ETag validation
- **Resume Support**:
  - Saves upload state to local storage
  - Can resume after app restart or crash
  - Tracks completed parts per upload
- **Error Handling**:
  - Network loss recovery
  - Low storage alerts
  - Background/foreground transition handling

### Configuration
- Backend base URL configurable in settings
- Default backend: `http://192.168.0.245:8080`
- MinIO bucket: `remote-consent`
- MinIO endpoint: `http://192.168.0.245:9000`

## Project Structure

```
.
├── manifest.json                 # uni-app manifest with Android config
├── pages.json                    # Pages configuration
├── main.js                       # App entry point
├── App.vue                       # Root component
├── pages/
│   └── index/
│       └── index.vue            # Main UI page
├── utils/
│   ├── config.js                # Configuration settings
│   └── uploadManager.js         # Chunked upload manager
├── static/
│   └── styles/
│       └── common.css           # Common styles
└── nativeplugins/
    └── ScreenRecorder/
        ├── package.json         # Plugin manifest
        └── android/
            ├── build.gradle     # Android build config
            ├── src/main/
            │   ├── AndroidManifest.xml
            │   └── java/com/screenrecorder/
            │       ├── ScreenRecorderModule.java
            │       ├── ScreenRecordService.java
            │       └── RecordConfig.java
```

## Requirements

- **Platform**: Android 5.0 (API 21) or higher
- **Development**: HBuilderX or uni-app CLI
- **Backend**: MinIO multipart upload demo API running on `http://192.168.0.245:8080`
- **MinIO**: Bucket `remote-consent` accessible at `http://192.168.0.245:9000`

## Installation

1. Open the project in HBuilderX or use uni-app CLI
2. Build for Android:
   - HBuilderX: Run > Run to Phone/Emulator > Android
   - CLI: `npm run build:app-android`
3. Install the generated APK on your Android device

## Usage

1. **Start Recording**:
   - Tap "Start Recording"
   - Grant screen capture permission when prompted
   - Grant microphone permission if audio recording is enabled
   - Recording starts with a foreground notification

2. **Stop Recording**:
   - Tap "Stop Recording"
   - Video is saved to local storage
   - Preview appears with video metadata

3. **Upload Video**:
   - Configure backend URL if different from default
   - Tap "Upload to Server"
   - Monitor progress with speed and time remaining
   - Upload can be cancelled and resumed later

4. **Resume Uploads**:
   - Pending uploads are listed at the bottom
   - Tap "Resume" to continue an interrupted upload
   - Completed parts are skipped automatically

## Configuration

Edit `utils/config.js` to customize:

```javascript
export default {
  backend: {
    baseURL: 'http://192.168.0.245:8080',
    endpoints: {
      init: '/api/uploads/init',
      status: '/api/uploads/status',
      complete: '/api/uploads/complete'
    }
  },
  upload: {
    chunkSize: 8 * 1024 * 1024,  // 8MB
    concurrency: 4,                // 4 parallel uploads
    maxRetries: 5,                 // Max retry attempts
    retryDelayBase: 1000          // 1 second base delay
  },
  recording: {
    resolution: '720p',
    fps: 30,
    bitrate: 3000000,             // 3 Mbps
    withMic: true
  }
}
```

## iOS Support

Currently, iOS recording is not implemented. Placeholder stubs are in place for future implementation using ReplayKit framework.

## Permissions

The app requires the following permissions:
- `INTERNET` - Network access for uploads
- `ACCESS_NETWORK_STATE` - Check network status
- `WRITE_EXTERNAL_STORAGE` - Save recordings
- `READ_EXTERNAL_STORAGE` - Read recordings
- `RECORD_AUDIO` - Record audio with screen
- `FOREGROUND_SERVICE` - Keep recording service alive
- `FOREGROUND_SERVICE_MEDIA_PROJECTION` - Screen recording service (Android 14+)
- `POST_NOTIFICATIONS` - Show recording notification

## Backend API

This app requires a compatible backend API. See the paired task "MinIO multipart upload demo API" for backend implementation.

### Expected Endpoints

1. **Initialize Upload**
   - POST `/api/uploads/init`
   - Body: `{ fileName, fileSize, contentType, bucket }`
   - Response: `{ uploadId, fileId, key }`

2. **Get Presigned URLs**
   - POST `/api/uploads/{fileId}/parts`
   - Body: `{ uploadId, partNumbers }`
   - Response: `{ urls: [{ partNumber, url }] }`

3. **Complete Upload**
   - POST `/api/uploads/complete`
   - Body: `{ fileId, uploadId, parts: [{ partNumber, etag }] }`
   - Response: `{ success, location }`

4. **Get Upload Status**
   - GET `/api/uploads/status/{fileId}`
   - Response: `{ uploadId, completedParts }`

## Testing

Test the app with recordings of 1-5 minutes:

1. Record a short video (1-2 minutes)
2. Verify video plays correctly in preview
3. Upload and verify progress updates
4. Kill app during upload
5. Restart app and verify upload resumes
6. Check uploaded file in MinIO bucket

## Troubleshooting

**Recording fails to start:**
- Check screen capture permission is granted
- Ensure microphone permission is granted (if withMic is true)
- Verify storage permissions

**Upload fails:**
- Check backend URL is correct and reachable
- Verify MinIO is running and accessible
- Check network connectivity
- Review error messages in app

**Upload doesn't resume:**
- Check local storage for pending tasks
- Verify fileId exists in backend
- Ensure file still exists on device

## License

MIT
