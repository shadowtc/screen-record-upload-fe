# Usage Guide

## Quick Start

### 1. Initial Setup

1. Install the APK on your Android device (API 21+)
2. Launch the app
3. Configure backend URL in Settings section (default: `http://192.168.0.245:8080`)

### 2. Recording Your First Video

**Step 1: Start Recording**
- Tap the "Start Recording" button
- Grant screen capture permission when prompted
- If audio recording is enabled, grant microphone permission
- A notification will appear indicating recording is active

**Step 2: Perform Actions**
- Navigate through your device
- Open apps, demonstrate features, etc.
- Recording continues even if you minimize the app

**Step 3: Stop Recording**
- Return to the app (or open from notification)
- Tap "Stop Recording"
- Wait for the video to be saved
- Preview will appear automatically

### 3. Uploading Videos

**Step 1: Review Recording**
- Check the video preview to ensure quality
- Review metadata: duration, size, resolution
- Play the video to verify content

**Step 2: Upload to Server**
- Ensure backend API is accessible
- Tap "Upload to Server"
- Monitor progress with real-time statistics:
  - Upload percentage
  - Current speed (KB/s or MB/s)
  - Estimated time remaining
  - Bytes uploaded / total bytes

**Step 3: Wait for Completion**
- Upload completes when progress reaches 100%
- Success message displays with file ID
- Verify upload in MinIO bucket if needed

### 4. Handling Interruptions

**Scenario 1: App Minimized During Upload**
- Upload continues in background
- Return to app to view progress
- No action needed

**Scenario 2: Network Connection Lost**
- Upload automatically retries failed chunks
- Exponential backoff prevents server overload
- Progress resumes when connection restored

**Scenario 3: App Killed During Upload**
- Upload state saved to local storage
- Reopen app
- Navigate to "Pending Uploads" section
- Tap "Resume" on the pending task
- Upload continues from last completed chunk

**Scenario 4: Device Restarted**
- Same as Scenario 3
- Pending uploads persist across restarts
- Ensure recorded file still exists on device

### 5. Advanced Configuration

**Adjusting Recording Settings**

Edit `utils/config.js` to change default recording parameters:

```javascript
recording: {
  resolution: '1080p',    // Change to '480p', '720p', or '1080p'
  fps: 60,                // Increase frame rate for smoother video
  bitrate: 5000000,       // Increase bitrate for better quality (5 Mbps)
  withMic: false          // Disable audio recording
}
```

**Adjusting Upload Settings**

Optimize for your network conditions:

```javascript
upload: {
  chunkSize: 4 * 1024 * 1024,  // Reduce to 4MB for slower networks
  concurrency: 2,               // Reduce concurrent uploads for stability
  maxRetries: 10,               // Increase retries for unreliable networks
  retryDelayBase: 2000          // Increase base delay between retries
}
```

**Changing Backend Server**

In the app's Settings section or in `utils/config.js`:

```javascript
backend: {
  baseURL: 'http://your-server-ip:8080',
  endpoints: {
    init: '/api/uploads/init',
    status: '/api/uploads/status',
    complete: '/api/uploads/complete'
  }
}
```

## Common Use Cases

### Use Case 1: Tutorial Videos

**Goal**: Record app demonstration for tutorial

1. Set resolution to 1080p for clarity
2. Enable microphone for narration
3. Set bitrate to 5 Mbps for quality
4. Record demonstration
5. Upload to share with team

### Use Case 2: Bug Reporting

**Goal**: Capture bug reproduction steps

1. Set resolution to 720p (balance quality/size)
2. Disable microphone if not needed
3. Record bug occurrence
4. Upload immediately for developers
5. Include file ID in bug report

### Use Case 3: Automated Testing

**Goal**: Record test execution

1. Set resolution to 480p (smaller files)
2. Disable microphone
3. Set bitrate to 2 Mbps
4. Record test suite execution
5. Upload for archival

### Use Case 4: Remote Support

**Goal**: Capture user issue for support

1. Use default settings (720p, 30fps)
2. Enable microphone for user explanation
3. Record issue demonstration
4. Upload and share file ID with support
5. Support team reviews video

## Troubleshooting

### Problem: Permission Denied

**Error**: "Screen capture permission denied"

**Solution**:
1. Tap "Start Recording" again
2. When permission dialog appears, tap "Start now"
3. Ensure "Don't show again" is NOT checked
4. If permission was previously denied, go to Settings > Apps > Screen Recorder > Permissions

### Problem: Recording Not Starting

**Error**: Recording status stays "Ready"

**Solution**:
1. Check Android version (requires API 21+)
2. Restart the app
3. Clear app cache and data
4. Reinstall the app if issue persists

### Problem: Upload Fails to Initialize

**Error**: "Init upload failed"

**Solution**:
1. Verify backend URL is correct
2. Check network connectivity
3. Ensure backend API is running
4. Test backend endpoint manually:
   ```bash
   curl -X POST http://192.168.0.245:8080/api/uploads/init \
     -H "Content-Type: application/json" \
     -d '{"fileName":"test.mp4","fileSize":1000,"contentType":"video/mp4","bucket":"remote-consent"}'
   ```

### Problem: Upload Stalls at X%

**Error**: Progress bar stops moving

**Solution**:
1. Check network connection
2. Verify backend is still responding
3. Cancel and retry upload
4. If problem persists, check backend logs
5. Reduce concurrency in config

### Problem: Resume Doesn't Work

**Error**: Upload starts from 0% after resume

**Solution**:
1. Ensure file still exists on device
2. Check backend still has upload session
3. Verify fileId is correct
4. If backend session expired, delete pending task and re-upload

### Problem: Video Preview Not Playing

**Error**: Black screen in preview

**Solution**:
1. Check if file was saved correctly
2. Verify file size is > 0 bytes
3. Try playing file with external player
4. If file is corrupt, record again
5. Check storage permissions

### Problem: Low Quality Video

**Error**: Video is pixelated or laggy

**Solution**:
1. Increase resolution to 1080p
2. Increase bitrate to 5 Mbps or higher
3. Reduce frame rate if device is slow
4. Close background apps before recording
5. Ensure device has sufficient resources

## Performance Tips

### For Better Quality
- Use highest resolution device supports
- Increase bitrate (5-10 Mbps)
- Use 60 fps for smooth motion
- Ensure good lighting (for screen content)
- Close unnecessary apps

### For Smaller Files
- Use 480p resolution
- Reduce bitrate to 1-2 Mbps
- Use 24-30 fps
- Disable audio if not needed
- Limit recording duration

### For Faster Uploads
- Use Wi-Fi instead of mobile data
- Increase chunk concurrency (if stable network)
- Ensure backend is on same network
- Reduce chunk size for faster feedback
- Upload during off-peak hours

### For Better Stability
- Don't minimize app during recording
- Keep device plugged in for long recordings
- Ensure sufficient storage space (at least 2x video size)
- Close memory-intensive apps
- Use newer Android versions if possible

## Integration with Backend

### Verifying Backend Connection

Test the backend API is working:

```bash
# Test initialization
curl -X POST http://192.168.0.245:8080/api/uploads/init \
  -H "Content-Type: application/json" \
  -d '{
    "fileName": "test.mp4",
    "fileSize": 1048576,
    "contentType": "video/mp4",
    "bucket": "remote-consent"
  }'

# Expected response:
# {
#   "uploadId": "...",
#   "fileId": "...",
#   "key": "..."
# }
```

### Verifying MinIO Access

Check the MinIO bucket:

```bash
# Using MinIO client (mc)
mc ls minio/remote-consent

# Or access MinIO console at:
# http://192.168.0.245:9000
```

### Viewing Uploaded Files

After successful upload:

1. Open MinIO console: `http://192.168.0.245:9000`
2. Login with credentials
3. Navigate to `remote-consent` bucket
4. Find uploaded file by key/fileId
5. Download or share link

## FAQ

**Q: Can I record phone calls?**
A: No, Android restricts recording phone call audio for privacy reasons.

**Q: Does recording affect device performance?**
A: Minimal impact on modern devices. Older devices may experience slowdown.

**Q: How long can I record?**
A: Limited by available storage. 1 hour at 720p ≈ 1-2 GB.

**Q: Can I edit videos in the app?**
A: No, this is a recording and upload tool only. Use external video editor.

**Q: Is there a maximum file size?**
A: No hard limit in app, but check backend/MinIO limits.

**Q: Can I upload multiple files simultaneously?**
A: Yes, each upload runs independently. Check device resources.

**Q: What happens if storage is full during recording?**
A: Recording stops and partial file may be saved. Check before recording.

**Q: Can I change video codec?**
A: Currently fixed to H.264. Modify native code to support other codecs.

**Q: Does it work on tablets?**
A: Yes, works on any Android device with API 21+.

**Q: Can I schedule recordings?**
A: Not built-in. Use Android automation tools to trigger the app.

## Support

For issues or questions:
1. Check this guide first
2. Review error messages in app
3. Check backend API logs
4. Review development documentation
5. Open an issue on the project repository
