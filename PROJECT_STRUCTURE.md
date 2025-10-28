# Project Structure

```
screen-record-upload-demo/
│
├── App.vue                          # Root Vue component
├── main.js                          # Application entry point
├── pages.json                       # Pages configuration
├── manifest.json                    # uni-app manifest with Android config
├── package.json                     # Node.js dependencies
├── uni.scss                         # Global SCSS variables
├── .gitignore                       # Git ignore rules
│
├── pages/                           # Application pages
│   └── index/
│       └── index.vue               # Main recording & upload page
│
├── utils/                           # Utility modules
│   ├── config.js                   # Configuration settings
│   └── uploadManager.js            # Chunked upload manager
│
├── static/                          # Static assets
│   └── styles/
│       └── common.css              # Common CSS styles
│
├── nativeplugins/                   # Native plugins
│   └── ScreenRecorder/
│       ├── package.json            # Plugin manifest
│       ├── android/                # Android implementation
│       │   ├── build.gradle        # Gradle build config
│       │   └── src/main/
│       │       ├── AndroidManifest.xml
│       │       └── java/com/screenrecorder/
│       │           ├── ScreenRecorderModule.java    # JS bridge
│       │           ├── ScreenRecordService.java     # Recording service
│       │           └── RecordConfig.java            # Config model
│       └── ios/                    # iOS stubs (future implementation)
│           ├── ScreenRecorderModule.h
│           └── ScreenRecorderModule.m
│
└── docs/                            # Documentation
    ├── README.md                   # Main documentation
    ├── DEVELOPMENT.md              # Development guide
    ├── USAGE.md                    # User guide
    └── config.example.js           # Configuration example
```

## Key Files

### Frontend (uni-app)

| File | Purpose |
|------|---------|
| `pages/index/index.vue` | Main UI with recording controls, video preview, and upload progress |
| `utils/uploadManager.js` | Manages chunked multipart upload with resume capability |
| `utils/config.js` | Centralized configuration for backend, upload, and recording settings |

### Native Android Module

| File | Purpose |
|------|---------|
| `ScreenRecorderModule.java` | Bridge between JavaScript and native Android code |
| `ScreenRecordService.java` | Foreground service that handles screen recording |
| `RecordConfig.java` | Data model for recording configuration |
| `AndroidManifest.xml` | Declares service and permissions |

### Configuration

| File | Purpose |
|------|---------|
| `manifest.json` | uni-app configuration with Android permissions |
| `pages.json` | Page routing and navigation configuration |
| `package.json` | Dependencies and build scripts |

### Documentation

| File | Purpose |
|------|---------|
| `README.md` | Project overview and quick start guide |
| `DEVELOPMENT.md` | Developer guide with architecture details |
| `USAGE.md` | End-user guide with troubleshooting |
| `config.example.js` | Example configuration with comments |

## Data Flow

### Recording Flow

```
User taps "Start Recording"
    ↓
pages/index/index.vue
    ↓
Call native module: uni.requireNativePlugin('ScreenRecorder')
    ↓
ScreenRecorderModule.java → startRecord()
    ↓
Request MediaProjection permission
    ↓
Create ScreenRecordService (Foreground)
    ↓
Initialize MediaRecorder with config
    ↓
Start recording to local file
    ↓
User taps "Stop Recording"
    ↓
ScreenRecordService → stopRecording()
    ↓
Save file and return metadata
    ↓
Display video preview in UI
```

### Upload Flow

```
User taps "Upload to Server"
    ↓
pages/index/index.vue
    ↓
uploadManager.uploadFile()
    ↓
POST /api/uploads/init
    ← uploadId, fileId, key
    ↓
Split file into 8MB chunks
    ↓
POST /api/uploads/{fileId}/parts
    ← presigned URLs for chunks
    ↓
Upload chunks in parallel (4 concurrent)
    ← ETags for each chunk
    ↓
POST /api/uploads/complete
    ← Upload complete confirmation
    ↓
Display success message
```

### Resume Flow

```
App restarts
    ↓
uploadManager.loadPendingTasks()
    ↓
Read from localStorage
    ↓
Display pending uploads in UI
    ↓
User taps "Resume"
    ↓
uploadManager.resumeUpload(taskId)
    ↓
Skip already uploaded chunks
    ↓
Continue from last incomplete chunk
    ↓
Complete upload
```

## Module Dependencies

### JavaScript Dependencies

```
pages/index/index.vue
    ↓
    ├── utils/uploadManager.js
    │       ↓
    │       └── utils/config.js
    │
    └── utils/config.js
```

### Native Dependencies

```
ScreenRecorderModule.java
    ↓
    ├── RecordConfig.java
    └── ScreenRecordService.java
            ↓
            └── RecordConfig.java
```

## Build Outputs

```
HBuilderX Build
    ↓
unpackage/
    └── dist/
        └── build/
            └── android/
                ├── app.apk              # Debug APK
                └── app-release.apk      # Release APK (signed)
```

## Storage Locations

### Recorded Videos

```
Android: /storage/emulated/0/Android/data/{package}/files/Movies/ScreenRecordings/
Files: screen_recording_{timestamp}.mp4
```

### Upload State

```
Local Storage Key: pending_upload_tasks
Format: JSON array of task objects
Persistence: Survives app restarts
```

## API Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/uploads/init` | Initialize multipart upload |
| POST | `/api/uploads/{fileId}/parts` | Get presigned URLs |
| POST | `/api/uploads/complete` | Complete upload |
| GET | `/api/uploads/status/{fileId}` | Get upload status |

## Permissions Required

| Permission | Purpose | When Requested |
|------------|---------|----------------|
| Screen Capture | Record screen | On first recording |
| RECORD_AUDIO | Record microphone | On first recording |
| WRITE_EXTERNAL_STORAGE | Save recordings | On app install |
| READ_EXTERNAL_STORAGE | Read recordings | On app install |
| INTERNET | Upload files | On app install |
| FOREGROUND_SERVICE | Keep recording active | On app install |

## Error Handling

| Error Type | Location | Strategy |
|------------|----------|----------|
| Permission Denied | ScreenRecorderModule | Show error message, retry |
| Network Failure | uploadManager | Exponential backoff retry |
| Storage Full | ScreenRecordService | Alert user, stop recording |
| Backend Error | uploadManager | Log error, notify user |
| File Not Found | uploadManager | Remove from pending tasks |
