/**
 * Example configuration file
 * Copy this to utils/config.js and customize for your environment
 */

export default {
  // Backend API configuration
  backend: {
    // Base URL of the backend API server
    baseURL: 'http://192.168.0.245:8080',
    
    // API endpoints
    endpoints: {
      init: '/api/uploads/init',          // Initialize multipart upload
      status: '/api/uploads/status',      // Get upload status
      complete: '/api/uploads/complete'   // Complete multipart upload
    }
  },
  
  // Upload settings
  upload: {
    chunkSize: 8 * 1024 * 1024,  // 8MB - size of each chunk
    concurrency: 4,               // Number of parallel uploads
    maxRetries: 5,                // Maximum retry attempts per chunk
    retryDelayBase: 1000         // Base delay in ms (exponential backoff)
  },
  
  // Recording settings
  recording: {
    resolution: '720p',           // Resolution: '480p', '720p', or '1080p'
    fps: 30,                      // Frames per second
    bitrate: 3000000,            // Bitrate in bps (~3 Mbps)
    withMic: true                // Record audio from microphone
  },
  
  // Local storage keys
  storage: {
    pendingTasksKey: 'pending_upload_tasks',  // Key for pending upload tasks
    recordingsKey: 'recordings_list'          // Key for recordings list
  }
}
