# Quick Start Guide

Get up and running with the Screen Recording and Chunked Upload Demo in 5 minutes.

## Prerequisites

✅ Android device or emulator (API 21+)  
✅ HBuilderX or uni-app CLI installed  
✅ Backend API running on `http://192.168.0.245:8080` (or configure your own)  
✅ MinIO accessible at `http://192.168.0.245:9000` with bucket `remote-consent`

## Installation (3 Steps)

### Step 1: Get the Code

```bash
# Clone or download the repository
cd /path/to/project
```

### Step 2: Open in HBuilderX

1. Launch HBuilderX
2. File > Open Directory
3. Select this project folder
4. Wait for indexing to complete

### Step 3: Run on Device

1. Connect Android device via USB (enable USB debugging)
2. Run > Run to Phone > Select your device
3. Wait for build and installation
4. App launches automatically

**Alternative using CLI:**

```bash
npm install
npm run dev:app-android
```

## First Recording (30 seconds)

### 1. Start Recording
- Open the app
- Tap **"Start Recording"**
- When prompted, tap **"Start now"** to grant screen capture permission
- If audio is enabled, grant microphone permission
- You'll see "Recording..." status

### 2. Do Something
- Switch to another app
- Navigate around
- Demonstrate a feature
- Record for 10-30 seconds

### 3. Stop Recording
- Return to the Screen Recorder app
- Tap **"Stop Recording"**
- Video preview appears
- Check the duration, size, and resolution

### 4. Upload Video
- Ensure backend is running
- Tap **"Upload to Server"**
- Watch the progress bar
- Upload completes with success message

🎉 **Done!** Your first screen recording is uploaded to MinIO.

## Configuration (Optional)

### Change Backend URL

In the app, scroll to **Settings** section:
```
Backend URL: http://your-server:8080
```

### Change Recording Quality

Edit `utils/config.js`:

```javascript
recording: {
  resolution: '1080p',    // Higher quality
  fps: 60,                // Smoother video
  bitrate: 5000000,       // Better quality (5 Mbps)
  withMic: true          // Keep audio
}
```

### Adjust Upload Settings

Edit `utils/config.js`:

```javascript
upload: {
  chunkSize: 4 * 1024 * 1024,  // 4MB chunks for slower networks
  concurrency: 2,               // 2 parallel uploads for stability
  maxRetries: 10,               // More retries for unreliable networks
  retryDelayBase: 2000          // 2 second base delay
}
```

## Common Issues

### ❌ "Permission Denied"
**Solution:** Tap "Start Recording" again and allow permission

### ❌ Recording Doesn't Start
**Solution:** Restart app, check Android version (needs API 21+)

### ❌ Upload Fails
**Solution:** Check backend URL, verify network connectivity

### ❌ App Crashes
**Solution:** Check storage space, reduce resolution to 720p

## Test the Resume Feature

1. Start uploading a large video (>50MB)
2. When upload reaches ~20%, kill the app (swipe away)
3. Reopen the app
4. Scroll to **"Pending Uploads"**
5. Tap **"Resume"** on your upload
6. Upload continues from where it left off

## Verify in MinIO

1. Open browser: `http://192.168.0.245:9000`
2. Login to MinIO console
3. Navigate to `remote-consent` bucket
4. Find your uploaded video
5. Download or share link

## Next Steps

- 📖 Read the [User Guide](USAGE.md) for detailed usage
- 🔧 Check [Development Guide](DEVELOPMENT.md) for customization
- 📚 Review [API Documentation](API.md) for integration
- 🏗️ See [Project Structure](PROJECT_STRUCTURE.md) for architecture

## Need Help?

1. Check error messages in the app
2. Review [Troubleshooting](USAGE.md#troubleshooting) section
3. Check backend API logs
4. Review device logcat: `adb logcat`
5. Open an issue on the repository

## Useful Commands

```bash
# View Android logs
adb logcat | grep ScreenRecord

# Check connected devices
adb devices

# Install APK manually
adb install app.apk

# Clear app data
adb shell pm clear <package.name>

# Test backend API
curl -X POST http://192.168.0.245:8080/api/uploads/init \
  -H "Content-Type: application/json" \
  -d '{"fileName":"test.mp4","fileSize":1000,"contentType":"video/mp4","bucket":"remote-consent"}'
```

## Production Checklist

Before deploying to production:

- [ ] Update backend URL in config
- [ ] Test on multiple Android versions
- [ ] Test on different devices
- [ ] Test with large files (>100MB)
- [ ] Test resume functionality
- [ ] Test network interruptions
- [ ] Verify permissions work correctly
- [ ] Check error messages are user-friendly
- [ ] Test with MinIO in production mode
- [ ] Configure proper authentication
- [ ] Set up monitoring and logging
- [ ] Create signed release APK
- [ ] Test release build thoroughly

## Support

For questions or issues:
- 📧 Open an issue on GitHub
- 📖 Check the documentation
- 💬 Review existing issues

Happy recording! 🎥
