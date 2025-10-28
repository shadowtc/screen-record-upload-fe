<template>
  <view class="container">
    <view class="header">
      <text class="title">Screen Recorder</text>
    </view>

    <view class="content">
      <view class="recording-section">
        <view class="status-badge" :class="recordingStatus">
          <text class="status-text">{{ statusText }}</text>
        </view>

        <view class="recording-info" v-if="currentRecording.duration">
          <text class="info-item">Duration: {{ formatDuration(currentRecording.duration) }}</text>
          <text class="info-item">Size: {{ formatSize(currentRecording.size) }}</text>
          <text class="info-item">Resolution: {{ currentRecording.width }}x{{ currentRecording.height }}</text>
        </view>

        <view class="button-group">
          <button 
            class="btn btn-primary" 
            :disabled="recordingStatus === 'recording' || uploading"
            @tap="startRecording"
          >
            Start Recording
          </button>
          
          <button 
            class="btn btn-danger" 
            :disabled="recordingStatus !== 'recording'"
            @tap="stopRecording"
          >
            Stop Recording
          </button>
        </view>
      </view>

      <view class="video-section" v-if="currentRecording.filePath">
        <text class="section-title">Preview</text>
        <video 
          :src="currentRecording.filePath" 
          class="video-preview"
          controls
          :show-center-play-btn="true"
        ></video>
      </view>

      <view class="upload-section" v-if="currentRecording.filePath && !uploading">
        <button 
          class="btn btn-success btn-block" 
          @tap="startUpload"
        >
          Upload to Server
        </button>
      </view>

      <view class="upload-section" v-if="uploading">
        <view class="upload-progress">
          <text class="section-title">Uploading...</text>
          <view class="progress-bar">
            <view class="progress-fill" :style="{ width: uploadProgress.percent + '%' }"></view>
          </view>
          <text class="progress-text">{{ uploadProgress.percent }}%</text>
          
          <view class="upload-stats">
            <text class="stat-item">Speed: {{ uploadProgress.speed }}</text>
            <text class="stat-item">Remaining: {{ uploadProgress.remaining }}</text>
            <text class="stat-item">{{ formatSize(uploadProgress.uploadedBytes) }} / {{ formatSize(uploadProgress.totalBytes) }}</text>
          </view>

          <text class="status-message">{{ uploadStatus }}</text>

          <button 
            class="btn btn-warning btn-small" 
            @tap="cancelUpload"
          >
            Cancel
          </button>
        </view>
      </view>

      <view class="upload-section" v-if="uploadComplete">
        <view class="success-message">
          <text class="success-icon">✓</text>
          <text class="success-text">Upload Complete!</text>
          <text class="success-detail">File ID: {{ uploadResult.fileId }}</text>
          <button 
            class="btn btn-primary btn-small" 
            @tap="resetUpload"
          >
            New Recording
          </button>
        </view>
      </view>

      <view class="pending-section" v-if="pendingTasks.length > 0">
        <text class="section-title">Pending Uploads</text>
        <view class="pending-list">
          <view 
            class="pending-item" 
            v-for="task in pendingTasks" 
            :key="task.taskId"
          >
            <view class="pending-info">
              <text class="pending-name">{{ task.fileName }}</text>
              <text class="pending-size">{{ formatSize(task.fileSize) }}</text>
            </view>
            <button 
              class="btn btn-primary btn-tiny" 
              @tap="resumeTask(task.taskId)"
            >
              Resume
            </button>
          </view>
        </view>
      </view>

      <view class="error-section" v-if="errorMessage">
        <view class="error-message">
          <text class="error-icon">✕</text>
          <text class="error-text">{{ errorMessage }}</text>
          <button 
            class="btn btn-primary btn-small" 
            @tap="clearError"
          >
            Dismiss
          </button>
        </view>
      </view>

      <view class="settings-section">
        <text class="section-title">Settings</text>
        <view class="setting-item">
          <text class="setting-label">Backend URL:</text>
          <input 
            class="setting-input" 
            v-model="backendUrl" 
            placeholder="http://192.168.0.245:8080"
          />
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import uploadManager from '@/utils/uploadManager.js'
import config from '@/utils/config.js'

export default {
  data() {
    return {
      recordingStatus: 'idle',
      currentRecording: {
        filePath: '',
        duration: 0,
        width: 0,
        height: 0,
        size: 0
      },
      uploading: false,
      uploadComplete: false,
      uploadProgress: {
        percent: 0,
        uploadedBytes: 0,
        totalBytes: 0,
        speed: '0 KB/s',
        remaining: '--'
      },
      uploadStatus: '',
      uploadResult: null,
      currentTaskId: null,
      pendingTasks: [],
      errorMessage: '',
      backendUrl: config.backend.baseURL,
      screenRecorderModule: null
    }
  },
  
  computed: {
    statusText() {
      switch (this.recordingStatus) {
        case 'idle':
          return 'Ready'
        case 'recording':
          return 'Recording...'
        case 'stopped':
          return 'Recording Saved'
        default:
          return 'Ready'
      }
    }
  },
  
  onLoad() {
    this.initScreenRecorder()
    this.loadPendingTasks()
  },
  
  methods: {
    initScreenRecorder() {
      if (uni.getSystemInfoSync().platform === 'android') {
        this.screenRecorderModule = uni.requireNativePlugin('ScreenRecorder')
        console.log('Screen recorder module initialized:', this.screenRecorderModule)
      } else {
        console.log('Screen recording only available on Android')
      }
    },
    
    async startRecording() {
      if (!this.screenRecorderModule) {
        this.showError('Screen recording not available on this platform')
        return
      }

      try {
        this.recordingStatus = 'recording'
        this.errorMessage = ''
        
        const result = await new Promise((resolve, reject) => {
          this.screenRecorderModule.startRecord({
            resolution: config.recording.resolution,
            fps: config.recording.fps,
            bitrate: config.recording.bitrate,
            withMic: config.recording.withMic
          }, (res) => {
            if (res.success) {
              resolve(res)
            } else {
              reject(new Error(res.message || 'Failed to start recording'))
            }
          })
        })

        console.log('Recording started:', result)
      } catch (error) {
        console.error('Start recording error:', error)
        this.recordingStatus = 'idle'
        this.showError(error.message || 'Failed to start recording. Please check permissions.')
      }
    },
    
    async stopRecording() {
      if (!this.screenRecorderModule) {
        return
      }

      try {
        const result = await new Promise((resolve, reject) => {
          this.screenRecorderModule.stopRecord((res) => {
            if (res.success) {
              resolve(res)
            } else {
              reject(new Error(res.message || 'Failed to stop recording'))
            }
          })
        })

        this.recordingStatus = 'stopped'
        this.currentRecording = {
          filePath: result.filePath,
          duration: result.duration || 0,
          width: result.width || 0,
          height: result.height || 0,
          size: result.size || 0
        }

        console.log('Recording stopped:', this.currentRecording)
        
        uni.showToast({
          title: 'Recording saved',
          icon: 'success'
        })
      } catch (error) {
        console.error('Stop recording error:', error)
        this.recordingStatus = 'idle'
        this.showError(error.message || 'Failed to stop recording')
      }
    },
    
    async startUpload() {
      if (!this.currentRecording.filePath) {
        return
      }

      this.uploading = true
      this.uploadComplete = false
      this.uploadProgress = {
        percent: 0,
        uploadedBytes: 0,
        totalBytes: this.currentRecording.size,
        speed: '0 KB/s',
        remaining: '--'
      }

      config.backend.baseURL = this.backendUrl

      const fileName = `recording_${Date.now()}.mp4`

      try {
        const result = await uploadManager.uploadFile(
          this.currentRecording.filePath,
          fileName,
          this.currentRecording.size,
          {
            onProgress: (progress) => {
              this.uploadProgress = progress
            },
            onStatusChange: (status) => {
              this.uploadStatus = status
            },
            onError: (error) => {
              console.error('Upload error:', error)
            }
          }
        )

        this.uploadResult = result
        this.uploadComplete = true
        this.uploading = false

        uni.showToast({
          title: 'Upload complete',
          icon: 'success'
        })
      } catch (error) {
        console.error('Upload failed:', error)
        this.uploading = false
        this.showError(error.message || 'Upload failed. Please try again.')
      }

      this.loadPendingTasks()
    },
    
    cancelUpload() {
      if (this.currentTaskId) {
        uploadManager.cancelUpload(this.currentTaskId)
      }
      this.uploading = false
      this.uploadStatus = 'Cancelled'
      
      uni.showToast({
        title: 'Upload cancelled',
        icon: 'none'
      })
    },
    
    async resumeTask(taskId) {
      this.uploading = true
      this.uploadComplete = false
      this.currentTaskId = taskId

      try {
        const result = await uploadManager.resumeUpload(taskId, {
          onProgress: (progress) => {
            this.uploadProgress = progress
          },
          onStatusChange: (status) => {
            this.uploadStatus = status
          },
          onError: (error) => {
            console.error('Upload error:', error)
          }
        })

        this.uploadResult = result
        this.uploadComplete = true
        this.uploading = false

        uni.showToast({
          title: 'Upload complete',
          icon: 'success'
        })
      } catch (error) {
        console.error('Resume upload failed:', error)
        this.uploading = false
        this.showError(error.message || 'Failed to resume upload')
      }

      this.loadPendingTasks()
    },
    
    resetUpload() {
      this.uploadComplete = false
      this.uploadResult = null
      this.currentRecording = {
        filePath: '',
        duration: 0,
        width: 0,
        height: 0,
        size: 0
      }
      this.recordingStatus = 'idle'
    },
    
    loadPendingTasks() {
      this.pendingTasks = uploadManager.getPendingTasks()
    },
    
    showError(message) {
      this.errorMessage = message
      uni.showToast({
        title: message,
        icon: 'none',
        duration: 3000
      })
    },
    
    clearError() {
      this.errorMessage = ''
    },
    
    formatDuration(ms) {
      const seconds = Math.floor(ms / 1000)
      const minutes = Math.floor(seconds / 60)
      const secs = seconds % 60
      return `${minutes}:${secs.toString().padStart(2, '0')}`
    },
    
    formatSize(bytes) {
      if (bytes < 1024) {
        return `${bytes} B`
      } else if (bytes < 1024 * 1024) {
        return `${(bytes / 1024).toFixed(2)} KB`
      } else if (bytes < 1024 * 1024 * 1024) {
        return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
      } else {
        return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`
      }
    }
  }
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.header {
  text-align: center;
  margin-bottom: 30px;
}

.title {
  font-size: 28px;
  font-weight: bold;
  color: #ffffff;
}

.content {
  background: #ffffff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
}

.recording-section {
  margin-bottom: 20px;
}

.status-badge {
  text-align: center;
  padding: 12px 20px;
  border-radius: 25px;
  margin-bottom: 15px;
}

.status-badge.idle {
  background: #e3f2fd;
}

.status-badge.recording {
  background: #ffebee;
  animation: pulse 1.5s infinite;
}

.status-badge.stopped {
  background: #e8f5e9;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
}

.status-text {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.recording-info {
  background: #f5f5f5;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 15px;
}

.info-item {
  display: block;
  font-size: 14px;
  color: #666;
  margin-bottom: 5px;
}

.button-group {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.btn {
  flex: 1;
  height: 44px;
  line-height: 44px;
  font-size: 16px;
  border-radius: 8px;
  border: none;
  color: #ffffff;
  font-weight: 600;
}

.btn-primary {
  background: #667eea;
}

.btn-primary:disabled {
  background: #cccccc;
  color: #999999;
}

.btn-danger {
  background: #f44336;
}

.btn-danger:disabled {
  background: #cccccc;
  color: #999999;
}

.btn-success {
  background: #4caf50;
}

.btn-warning {
  background: #ff9800;
}

.btn-block {
  width: 100%;
}

.btn-small {
  height: 36px;
  line-height: 36px;
  font-size: 14px;
  margin-top: 10px;
}

.btn-tiny {
  height: 32px;
  line-height: 32px;
  font-size: 12px;
  padding: 0 12px;
  flex: none;
}

.video-section {
  margin-bottom: 20px;
}

.section-title {
  display: block;
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 10px;
}

.video-preview {
  width: 100%;
  height: 400px;
  border-radius: 8px;
  background: #000;
}

.upload-section {
  margin-bottom: 20px;
}

.upload-progress {
  text-align: center;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background: #e0e0e0;
  border-radius: 4px;
  margin: 15px 0;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  transition: width 0.3s ease;
}

.progress-text {
  display: block;
  font-size: 24px;
  font-weight: bold;
  color: #667eea;
  margin-bottom: 10px;
}

.upload-stats {
  background: #f5f5f5;
  border-radius: 8px;
  padding: 15px;
  margin: 15px 0;
}

.stat-item {
  display: block;
  font-size: 14px;
  color: #666;
  margin-bottom: 5px;
}

.status-message {
  display: block;
  font-size: 14px;
  color: #999;
  margin: 10px 0;
}

.success-message {
  text-align: center;
  padding: 30px 20px;
}

.success-icon {
  display: block;
  font-size: 60px;
  color: #4caf50;
  margin-bottom: 15px;
}

.success-text {
  display: block;
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 10px;
}

.success-detail {
  display: block;
  font-size: 14px;
  color: #666;
  margin-bottom: 20px;
  word-break: break-all;
}

.error-section {
  margin-bottom: 20px;
}

.error-message {
  text-align: center;
  padding: 20px;
  background: #ffebee;
  border-radius: 8px;
}

.error-icon {
  display: block;
  font-size: 40px;
  color: #f44336;
  margin-bottom: 10px;
}

.error-text {
  display: block;
  font-size: 14px;
  color: #d32f2f;
  margin-bottom: 15px;
}

.pending-section {
  margin-bottom: 20px;
}

.pending-list {
  background: #f5f5f5;
  border-radius: 8px;
  padding: 10px;
}

.pending-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #ffffff;
  border-radius: 6px;
  margin-bottom: 8px;
}

.pending-info {
  flex: 1;
}

.pending-name {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.pending-size {
  display: block;
  font-size: 12px;
  color: #999;
}

.settings-section {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #e0e0e0;
}

.setting-item {
  margin-bottom: 15px;
}

.setting-label {
  display: block;
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.setting-input {
  width: 100%;
  height: 40px;
  padding: 0 12px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 14px;
  background: #fafafa;
}
</style>
