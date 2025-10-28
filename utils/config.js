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
    chunkSize: 8 * 1024 * 1024,
    concurrency: 4,
    maxRetries: 5,
    retryDelayBase: 1000
  },
  recording: {
    resolution: '720p',
    fps: 30,
    bitrate: 3000000,
    withMic: true
  },
  storage: {
    pendingTasksKey: 'pending_upload_tasks',
    recordingsKey: 'recordings_list'
  }
}
