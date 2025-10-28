# Development Guide

## Development Environment Setup

### Prerequisites

1. **HBuilderX** (recommended) or **uni-app CLI**
   - Download from: https://www.dcloud.io/hbuilderx.html
   - Or install CLI: `npm install -g @dcloudio/uvm`

2. **Android Development**
   - Android Studio (for SDK and emulator)
   - JDK 8 or higher
   - Android SDK API 21 or higher

3. **Node.js**
   - Version 12.x or higher
   - npm or yarn

### Project Setup

```bash
# Install dependencies (if using CLI)
npm install

# For HBuilderX, open the project folder directly
```

## Building the Project

### Using HBuilderX

1. Open the project in HBuilderX
2. Click "Run" > "Run to Phone/Emulator" > "Android Device/Emulator"
3. Or "Release" > "Native App-Cloud Package" for production builds

### Using CLI

```bash
# Development build with watch mode
npm run dev:app-android

# Production build
npm run build:app-android
```

## Native Plugin Development

### Android Plugin Structure

```
nativeplugins/ScreenRecorder/android/
├── build.gradle              # Gradle build configuration
├── src/main/
│   ├── AndroidManifest.xml  # Android manifest
│   └── java/com/screenrecorder/
│       ├── ScreenRecorderModule.java    # Main module (uni-app interface)
│       ├── ScreenRecordService.java     # Foreground service
│       └── RecordConfig.java            # Configuration model
```

### Key Components

#### ScreenRecorderModule
- Entry point for JavaScript calls
- Handles permission requests
- Manages MediaProjection setup
- Bridges between JS and native code

#### ScreenRecordService
- Foreground service for recording
- Uses MediaRecorder API
- Manages VirtualDisplay for screen capture
- Handles recording lifecycle

#### RecordConfig
- Data model for recording configuration
- Stores resolution, fps, bitrate, etc.

### Testing Native Changes

1. Modify the Java code
2. Rebuild the Android project
3. Test on a physical device (emulator may not support screen recording)

## Upload Manager Architecture

### Flow Diagram

```
User Action
    ↓
initUpload() → Backend /api/uploads/init
    ↓
Split file into chunks (8MB each)
    ↓
getPresignedUrls() → Backend /api/uploads/{fileId}/parts
    ↓
uploadChunksWithConcurrency() → Upload 4 chunks in parallel
    ↓           ↓
  Success    Failure → Retry with exponential backoff (max 5)
    ↓
All chunks uploaded
    ↓
completeUpload() → Backend /api/uploads/complete
    ↓
Upload Complete!
```

### Key Functions

- `initUpload()`: Initialize multipart upload session
- `uploadChunksWithConcurrency()`: Manage parallel chunk uploads
- `uploadPart()`: Upload single chunk with retry logic
- `completeUpload()`: Finalize the multipart upload
- `resumeUpload()`: Resume interrupted upload

### State Management

Pending uploads are stored in local storage with this structure:

```javascript
{
  taskId: "1234567890",
  fileId: "uuid-from-backend",
  uploadId: "minio-upload-id",
  key: "object-key",
  filePath: "/path/to/file.mp4",
  fileName: "recording.mp4",
  fileSize: 52428800,
  completedParts: [
    { partNumber: 1, etag: "etag1" },
    { partNumber: 2, etag: "etag2" }
  ],
  createdAt: 1234567890000
}
```

## Error Handling

### Network Errors

- Automatic retry with exponential backoff
- Max 5 retries per chunk
- Delays: 1s, 2s, 4s, 8s, 16s

### Storage Errors

- Check available space before recording
- Alert user if storage is low
- Handle write permission errors

### Permission Errors

- Request permissions before recording
- Show clear error messages
- Guide user to settings if needed

## Testing Checklist

### Recording Tests

- [ ] Start recording successfully
- [ ] Stop recording and save file
- [ ] Recording continues when app goes to background
- [ ] Recording stops cleanly when app is killed
- [ ] Audio recording works (if enabled)
- [ ] Different resolutions (480p, 720p, 1080p)
- [ ] Permission denied handling

### Upload Tests

- [ ] Upload small file (<10MB)
- [ ] Upload large file (100MB+)
- [ ] Pause and resume upload
- [ ] Kill app and resume upload
- [ ] Network interruption during upload
- [ ] Concurrent uploads
- [ ] Retry on failure
- [ ] Complete upload verification

### UI Tests

- [ ] Progress bar updates correctly
- [ ] Speed calculation accurate
- [ ] Remaining time estimate reasonable
- [ ] Video preview plays correctly
- [ ] Settings persist across sessions
- [ ] Error messages display properly

## Debugging

### Android Logs

```bash
# View logcat for screen recorder
adb logcat -s ScreenRecordService

# View all app logs
adb logcat | grep com.your.app
```

### JavaScript Debugging

Enable developer tools in HBuilderX or use Chrome DevTools for H5 platform.

```javascript
// Add debug logs in uploadManager.js
console.log('Upload progress:', progress)
console.log('Chunk uploaded:', partNumber, etag)
```

### Network Debugging

Use a proxy like Charles or Fiddler to inspect HTTP requests:

1. Configure proxy on Android device
2. Monitor requests to backend API
3. Verify presigned URLs and ETags

## Performance Optimization

### Recording

- Adjust bitrate based on resolution
- Use hardware acceleration when available
- Minimize background operations during recording

### Upload

- Tune chunk size for network conditions
  - Slower networks: smaller chunks (4MB)
  - Faster networks: larger chunks (16MB)
- Adjust concurrency based on device capabilities
- Use compression if bandwidth is limited

## Common Issues

### Recording Fails to Start

**Problem**: MediaProjection permission denied

**Solution**: Check if screen capture permission was granted. Some devices require additional permissions.

### Upload Stalls

**Problem**: No progress updates during upload

**Solution**: Check network connectivity. Verify backend is responding. Review retry logic.

### App Crashes During Recording

**Problem**: Out of memory or storage

**Solution**: Check available RAM and storage before recording. Reduce resolution or bitrate.

### Resume Doesn't Work

**Problem**: Upload doesn't resume after app restart

**Solution**: Verify pending tasks in local storage. Check if file still exists. Ensure fileId is valid.

## Best Practices

1. **Always test on real devices** - Emulators may not support screen recording
2. **Handle all permissions gracefully** - Show clear instructions to users
3. **Validate backend responses** - Check ETags and status codes
4. **Save state frequently** - Persist upload progress regularly
5. **Clean up resources** - Release MediaRecorder and VirtualDisplay properly
6. **Monitor memory usage** - Recording can be memory-intensive
7. **Test network scenarios** - Slow connections, interruptions, etc.

## Resources

- [uni-app Documentation](https://uniapp.dcloud.io/)
- [Android MediaProjection API](https://developer.android.com/reference/android/media/projection/MediaProjection)
- [Android MediaRecorder](https://developer.android.com/reference/android/media/MediaRecorder)
- [MinIO Multipart Upload](https://docs.min.io/docs/minio-server-configuration-guide.html)

## Contributing

When contributing to the project:

1. Follow existing code style
2. Add comments for complex logic
3. Test thoroughly on Android devices
4. Update documentation as needed
5. Submit pull request with clear description
