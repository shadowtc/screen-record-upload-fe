# API Documentation

## Native Plugin API

### ScreenRecorder Module

The ScreenRecorder module provides native screen recording capabilities for Android.

#### Importing the Module

```javascript
const screenRecorder = uni.requireNativePlugin('ScreenRecorder')
```

#### Methods

##### startRecord(options, callback)

Starts screen recording with the specified options.

**Parameters:**

- `options` (Object): Recording configuration
  - `resolution` (String): Video resolution - '480p', '720p', or '1080p'
  - `fps` (Number): Frame rate in frames per second (e.g., 30, 60)
  - `bitrate` (Number): Video bitrate in bits per second (e.g., 3000000 for 3 Mbps)
  - `withMic` (Boolean): Whether to record audio from microphone

- `callback` (Function): Callback function called when recording starts
  - Receives result object with:
    - `success` (Boolean): Whether the operation succeeded
    - `message` (String): Error message if failed

**Example:**

```javascript
screenRecorder.startRecord({
  resolution: '720p',
  fps: 30,
  bitrate: 3000000,
  withMic: true
}, (result) => {
  if (result.success) {
    console.log('Recording started')
  } else {
    console.error('Failed to start:', result.message)
  }
})
```

**Behavior:**
- Requests screen capture permission if not already granted
- Requests microphone permission if `withMic` is true
- Starts a foreground service to maintain recording
- Shows notification while recording
- Recording continues even if app is backgrounded

**Errors:**
- Permission denied by user
- MediaProjection not supported
- Activity context not available

##### stopRecord(callback)

Stops the current screen recording and saves the video file.

**Parameters:**

- `callback` (Function): Callback function called when recording stops
  - Receives result object with:
    - `success` (Boolean): Whether the operation succeeded
    - `filePath` (String): Path to the saved video file
    - `duration` (Number): Recording duration in milliseconds
    - `width` (Number): Video width in pixels
    - `height` (Number): Video height in pixels
    - `size` (Number): File size in bytes
    - `message` (String): Error message if failed

**Example:**

```javascript
screenRecorder.stopRecord((result) => {
  if (result.success) {
    console.log('Recording saved:', result.filePath)
    console.log('Duration:', result.duration, 'ms')
    console.log('Size:', result.size, 'bytes')
  } else {
    console.error('Failed to stop:', result.message)
  }
})
```

**File Location:**
- Android: `/storage/emulated/0/Android/data/{package}/files/Movies/ScreenRecordings/`
- Filename format: `screen_recording_{timestamp}.mp4`

**Errors:**
- No active recording
- Failed to save file
- Storage permission denied

## Upload Manager API

### UploadManager Class

The UploadManager handles chunked multipart uploads with resume capability.

#### Importing the Module

```javascript
import uploadManager from '@/utils/uploadManager.js'
```

#### Methods

##### uploadFile(filePath, fileName, fileSize, callbacks)

Uploads a file using chunked multipart upload.

**Parameters:**

- `filePath` (String): Full path to the file to upload
- `fileName` (String): Name for the uploaded file
- `fileSize` (Number): Size of the file in bytes
- `callbacks` (Object): Callback functions (optional)
  - `onProgress` (Function): Called with progress updates
    - `progress.percent` (String): Upload percentage
    - `progress.uploadedBytes` (Number): Bytes uploaded so far
    - `progress.totalBytes` (Number): Total bytes to upload
    - `progress.speed` (String): Current upload speed (formatted)
    - `progress.remaining` (String): Estimated time remaining (formatted)
  - `onStatusChange` (Function): Called when status changes
    - Receives status message string
  - `onError` (Function): Called on error
    - Receives error object

**Returns:** Promise that resolves with upload result

**Example:**

```javascript
try {
  const result = await uploadManager.uploadFile(
    '/path/to/video.mp4',
    'my-video.mp4',
    52428800, // 50 MB
    {
      onProgress: (progress) => {
        console.log(`Upload: ${progress.percent}%`)
        console.log(`Speed: ${progress.speed}`)
        console.log(`Remaining: ${progress.remaining}`)
      },
      onStatusChange: (status) => {
        console.log('Status:', status)
      },
      onError: (error) => {
        console.error('Error:', error)
      }
    }
  )
  console.log('Upload complete:', result)
} catch (error) {
  console.error('Upload failed:', error)
}
```

**Upload Flow:**
1. Initialize multipart upload
2. Split file into chunks
3. Get presigned URLs for chunks
4. Upload chunks in parallel
5. Complete multipart upload

**Errors:**
- Network connection issues
- Backend API errors
- File not found
- Upload cancelled

##### resumeUpload(taskId, callbacks)

Resumes an interrupted upload.

**Parameters:**

- `taskId` (String): ID of the pending upload task
- `callbacks` (Object): Same as uploadFile()

**Returns:** Promise that resolves with upload result

**Example:**

```javascript
const pendingTasks = uploadManager.getPendingTasks()
const taskId = pendingTasks[0].taskId

try {
  const result = await uploadManager.resumeUpload(taskId, {
    onProgress: (progress) => {
      console.log(`Resuming: ${progress.percent}%`)
    }
  })
  console.log('Resume complete:', result)
} catch (error) {
  console.error('Resume failed:', error)
}
```

**Behavior:**
- Skips already uploaded chunks
- Continues from last incomplete chunk
- Uses saved state from local storage

##### cancelUpload(taskId)

Cancels an active upload.

**Parameters:**

- `taskId` (String): ID of the upload task to cancel

**Example:**

```javascript
uploadManager.cancelUpload(taskId)
```

**Behavior:**
- Stops uploading new chunks
- Current chunk uploads may complete
- Task remains in pending list for later resume

##### getPendingTasks()

Gets list of pending upload tasks.

**Returns:** Array of task objects

**Example:**

```javascript
const tasks = uploadManager.getPendingTasks()
tasks.forEach(task => {
  console.log(`Task ${task.taskId}:`, task.fileName, task.fileSize)
})
```

**Task Object:**
```javascript
{
  taskId: "1234567890",
  fileId: "backend-file-id",
  uploadId: "minio-upload-id",
  key: "object-key",
  filePath: "/path/to/file.mp4",
  fileName: "video.mp4",
  fileSize: 52428800,
  completedParts: [
    { partNumber: 1, etag: "etag1" },
    { partNumber: 2, etag: "etag2" }
  ],
  createdAt: 1234567890000
}
```

##### clearPendingTask(taskId)

Removes a task from the pending list.

**Parameters:**

- `taskId` (String): ID of the task to remove

**Example:**

```javascript
uploadManager.clearPendingTask(taskId)
```

**Use Case:**
- User manually deletes a pending upload
- Upload failed permanently and should not be retried

## Backend API Integration

### Expected Backend Endpoints

#### POST /api/uploads/init

Initialize a multipart upload session.

**Request:**
```json
{
  "fileName": "video.mp4",
  "fileSize": 52428800,
  "contentType": "video/mp4",
  "bucket": "remote-consent"
}
```

**Response:**
```json
{
  "uploadId": "unique-upload-id",
  "fileId": "unique-file-id",
  "key": "uploads/video.mp4"
}
```

#### POST /api/uploads/{fileId}/parts

Get presigned URLs for uploading parts.

**Request:**
```json
{
  "uploadId": "unique-upload-id",
  "partNumbers": [1, 2, 3, 4, 5]
}
```

**Response:**
```json
{
  "urls": [
    { "partNumber": 1, "url": "https://presigned-url-1" },
    { "partNumber": 2, "url": "https://presigned-url-2" },
    { "partNumber": 3, "url": "https://presigned-url-3" },
    { "partNumber": 4, "url": "https://presigned-url-4" },
    { "partNumber": 5, "url": "https://presigned-url-5" }
  ]
}
```

#### PUT {presignedUrl}

Upload a part to the presigned URL.

**Request:**
- Binary data of the chunk
- Content-Type: application/octet-stream

**Response:**
- Status: 200 OK
- Headers:
  - `ETag`: Entity tag for the uploaded part

#### POST /api/uploads/complete

Complete the multipart upload.

**Request:**
```json
{
  "fileId": "unique-file-id",
  "uploadId": "unique-upload-id",
  "parts": [
    { "partNumber": 1, "etag": "etag1" },
    { "partNumber": 2, "etag": "etag2" },
    { "partNumber": 3, "etag": "etag3" }
  ]
}
```

**Response:**
```json
{
  "success": true,
  "location": "https://minio-endpoint/bucket/key",
  "fileId": "unique-file-id",
  "key": "uploads/video.mp4"
}
```

#### GET /api/uploads/status/{fileId}

Get the status of an upload (for resume).

**Response:**
```json
{
  "uploadId": "unique-upload-id",
  "fileId": "unique-file-id",
  "completedParts": [
    { "partNumber": 1, "etag": "etag1" },
    { "partNumber": 2, "etag": "etag2" }
  ]
}
```

## Configuration API

### Config Object

The configuration object controls all aspects of the app.

**Location:** `utils/config.js`

**Structure:**

```javascript
{
  backend: {
    baseURL: String,              // Backend API base URL
    endpoints: {
      init: String,               // Init upload endpoint path
      status: String,             // Status check endpoint path
      complete: String            // Complete upload endpoint path
    }
  },
  upload: {
    chunkSize: Number,            // Chunk size in bytes
    concurrency: Number,          // Number of parallel uploads
    maxRetries: Number,           // Maximum retry attempts
    retryDelayBase: Number        // Base delay in ms for exponential backoff
  },
  recording: {
    resolution: String,           // '480p', '720p', or '1080p'
    fps: Number,                  // Frame rate
    bitrate: Number,              // Bitrate in bps
    withMic: Boolean              // Record audio
  },
  storage: {
    pendingTasksKey: String,      // Local storage key for pending tasks
    recordingsKey: String         // Local storage key for recordings list
  }
}
```

**Accessing Configuration:**

```javascript
import config from '@/utils/config.js'

console.log('Backend URL:', config.backend.baseURL)
console.log('Chunk size:', config.upload.chunkSize)
console.log('Resolution:', config.recording.resolution)
```

**Modifying Configuration:**

```javascript
// Runtime modification
config.backend.baseURL = 'http://new-server:8080'

// Or edit utils/config.js directly
```

## Events and Callbacks

### Recording Events

| Event | Trigger | Data |
|-------|---------|------|
| Recording Started | Recording begins | `{ success: true }` |
| Recording Stopped | Recording ends | `{ filePath, duration, width, height, size }` |
| Recording Error | Error occurs | `{ success: false, message }` |

### Upload Events

| Event | Trigger | Data |
|-------|---------|------|
| Progress Update | Chunk upload progresses | `{ percent, uploadedBytes, totalBytes, speed, remaining }` |
| Status Change | Upload phase changes | Status message string |
| Upload Complete | Upload finishes | `{ fileId, location, key }` |
| Upload Error | Error occurs | Error object |

## Error Codes

### Native Module Errors

| Code | Message | Cause | Solution |
|------|---------|-------|----------|
| PERMISSION_DENIED | Screen capture permission denied | User denied permission | Request permission again |
| NO_ACTIVITY | Activity not found | Context unavailable | Restart app |
| NOT_SUPPORTED | MediaProjection not supported | Old Android version | Upgrade device |
| RECORDING_FAILED | Failed to start recording | Various | Check logs, retry |

### Upload Errors

| Code | Message | Cause | Solution |
|------|---------|-------|----------|
| NETWORK_ERROR | Network connection failed | No internet | Check connection |
| INIT_FAILED | Init upload failed | Backend error | Check backend |
| UPLOAD_FAILED | Chunk upload failed | Network/backend issue | Retry automatically |
| COMPLETE_FAILED | Complete upload failed | Backend error | Check backend logs |
| FILE_NOT_FOUND | File not found | File deleted | Re-record |

## Best Practices

### Using the Native Module

```javascript
// Good: Handle all possible outcomes
screenRecorder.startRecord(options, (result) => {
  if (result.success) {
    // Success handling
    this.updateUI('recording')
  } else {
    // Error handling
    this.showError(result.message)
    this.handlePermissionError()
  }
})

// Bad: No error handling
screenRecorder.startRecord(options, (result) => {
  this.updateUI('recording')
})
```

### Using Upload Manager

```javascript
// Good: Comprehensive callbacks
await uploadManager.uploadFile(filePath, fileName, fileSize, {
  onProgress: (progress) => {
    this.updateProgressBar(progress)
    this.updateStats(progress)
  },
  onStatusChange: (status) => {
    this.showStatus(status)
  },
  onError: (error) => {
    this.handleError(error)
    this.logError(error)
  }
})

// Bad: No callbacks
await uploadManager.uploadFile(filePath, fileName, fileSize)
```

### Error Handling

```javascript
// Good: Try-catch with specific error handling
try {
  const result = await uploadManager.uploadFile(...)
  this.handleSuccess(result)
} catch (error) {
  if (error.message.includes('network')) {
    this.showNetworkError()
  } else if (error.message.includes('permission')) {
    this.requestPermission()
  } else {
    this.showGenericError(error)
  }
}

// Bad: No error handling
const result = await uploadManager.uploadFile(...)
this.handleSuccess(result)
```
