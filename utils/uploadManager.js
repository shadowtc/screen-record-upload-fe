import config from './config.js'

class UploadManager {
  constructor() {
    this.activeUploads = new Map()
    this.pendingTasks = []
    this.loadPendingTasks()
  }

  loadPendingTasks() {
    try {
      const tasksStr = uni.getStorageSync(config.storage.pendingTasksKey)
      if (tasksStr) {
        this.pendingTasks = JSON.parse(tasksStr)
      }
    } catch (e) {
      console.error('Failed to load pending tasks:', e)
    }
  }

  savePendingTasks() {
    try {
      uni.setStorageSync(config.storage.pendingTasksKey, JSON.stringify(this.pendingTasks))
    } catch (e) {
      console.error('Failed to save pending tasks:', e)
    }
  }

  async initUpload(filePath, fileName, fileSize) {
    try {
      const response = await this.request({
        url: `${config.backend.baseURL}${config.backend.endpoints.init}`,
        method: 'POST',
        data: {
          fileName: fileName,
          fileSize: fileSize,
          contentType: 'video/mp4',
          bucket: 'remote-consent'
        }
      })

      if (response.statusCode === 200 && response.data) {
        return {
          uploadId: response.data.uploadId,
          fileId: response.data.fileId,
          key: response.data.key
        }
      }
      throw new Error('Init upload failed')
    } catch (error) {
      console.error('Init upload error:', error)
      throw error
    }
  }

  async getPresignedUrls(fileId, uploadId, partNumbers) {
    try {
      const response = await this.request({
        url: `${config.backend.baseURL}/api/uploads/${fileId}/parts`,
        method: 'POST',
        data: {
          uploadId: uploadId,
          partNumbers: partNumbers
        }
      })

      if (response.statusCode === 200 && response.data && response.data.urls) {
        return response.data.urls
      }
      throw new Error('Get presigned URLs failed')
    } catch (error) {
      console.error('Get presigned URLs error:', error)
      throw error
    }
  }

  async uploadPart(presignedUrl, chunk, onProgress) {
    let retries = 0
    const maxRetries = config.upload.maxRetries

    while (retries <= maxRetries) {
      try {
        const result = await new Promise((resolve, reject) => {
          const uploadTask = uni.uploadFile({
            url: presignedUrl,
            filePath: chunk.filePath,
            name: 'file',
            header: {
              'Content-Type': 'application/octet-stream'
            },
            success: (res) => {
              if (res.statusCode === 200) {
                const etag = res.header?.ETag || res.header?.etag
                resolve({ etag, partNumber: chunk.partNumber })
              } else {
                reject(new Error(`Upload failed with status ${res.statusCode}`))
              }
            },
            fail: (error) => {
              reject(error)
            }
          })

          if (onProgress) {
            uploadTask.onProgressUpdate((res) => {
              onProgress({
                progress: res.progress,
                totalBytesSent: res.totalBytesSent,
                totalBytesExpectedToSend: res.totalBytesExpectedToSend
              })
            })
          }
        })

        return result
      } catch (error) {
        retries++
        if (retries > maxRetries) {
          throw error
        }
        const delay = config.upload.retryDelayBase * Math.pow(2, retries - 1)
        await this.sleep(delay)
      }
    }
  }

  async completeUpload(fileId, uploadId, parts) {
    try {
      const response = await this.request({
        url: `${config.backend.baseURL}${config.backend.endpoints.complete}`,
        method: 'POST',
        data: {
          fileId: fileId,
          uploadId: uploadId,
          parts: parts.map(p => ({
            partNumber: p.partNumber,
            etag: p.etag
          }))
        }
      })

      if (response.statusCode === 200) {
        return response.data
      }
      throw new Error('Complete upload failed')
    } catch (error) {
      console.error('Complete upload error:', error)
      throw error
    }
  }

  async getUploadStatus(fileId) {
    try {
      const response = await this.request({
        url: `${config.backend.baseURL}${config.backend.endpoints.status}/${fileId}`,
        method: 'GET'
      })

      if (response.statusCode === 200) {
        return response.data
      }
      return null
    } catch (error) {
      console.error('Get upload status error:', error)
      return null
    }
  }

  async uploadFile(filePath, fileName, fileSize, callbacks = {}) {
    const taskId = Date.now().toString()
    let uploadInfo = null

    try {
      callbacks.onStatusChange?.('Initializing upload...')

      uploadInfo = await this.initUpload(filePath, fileName, fileSize)
      
      const task = {
        taskId,
        fileId: uploadInfo.fileId,
        uploadId: uploadInfo.uploadId,
        key: uploadInfo.key,
        filePath,
        fileName,
        fileSize,
        completedParts: [],
        createdAt: Date.now()
      }

      this.pendingTasks.push(task)
      this.savePendingTasks()

      const result = await this.resumeUpload(taskId, callbacks)
      
      this.pendingTasks = this.pendingTasks.filter(t => t.taskId !== taskId)
      this.savePendingTasks()

      return result
    } catch (error) {
      callbacks.onError?.(error)
      throw error
    }
  }

  async resumeUpload(taskId, callbacks = {}) {
    const task = this.pendingTasks.find(t => t.taskId === taskId)
    if (!task) {
      throw new Error('Task not found')
    }

    this.activeUploads.set(taskId, { cancelled: false })

    try {
      const totalChunks = Math.ceil(task.fileSize / config.upload.chunkSize)
      const completedPartNumbers = new Set(task.completedParts.map(p => p.partNumber))
      
      callbacks.onStatusChange?.('Preparing chunks...')

      const chunks = []
      for (let i = 0; i < totalChunks; i++) {
        const partNumber = i + 1
        if (!completedPartNumbers.has(partNumber)) {
          chunks.push({
            partNumber,
            start: i * config.upload.chunkSize,
            end: Math.min((i + 1) * config.upload.chunkSize, task.fileSize)
          })
        }
      }

      if (chunks.length === 0) {
        callbacks.onStatusChange?.('Completing upload...')
        return await this.completeUpload(task.fileId, task.uploadId, task.completedParts)
      }

      const chunkFiles = await this.splitFile(task.filePath, chunks)
      
      callbacks.onStatusChange?.('Uploading...')

      const uploadedParts = await this.uploadChunksWithConcurrency(
        task.fileId,
        task.uploadId,
        chunkFiles,
        task,
        callbacks
      )

      task.completedParts.push(...uploadedParts)
      this.savePendingTasks()

      if (this.activeUploads.get(taskId)?.cancelled) {
        throw new Error('Upload cancelled')
      }

      callbacks.onStatusChange?.('Completing upload...')
      const result = await this.completeUpload(task.fileId, task.uploadId, task.completedParts)

      return result
    } finally {
      this.activeUploads.delete(taskId)
    }
  }

  async uploadChunksWithConcurrency(fileId, uploadId, chunkFiles, task, callbacks) {
    const uploadedParts = []
    const partNumbers = chunkFiles.map(c => c.partNumber)
    
    const batchSize = 10
    const allUrls = []
    
    for (let i = 0; i < partNumbers.length; i += batchSize) {
      const batch = partNumbers.slice(i, i + batchSize)
      const urls = await this.getPresignedUrls(fileId, uploadId, batch)
      allUrls.push(...urls)
    }

    const urlMap = new Map()
    allUrls.forEach(item => {
      urlMap.set(item.partNumber, item.url)
    })

    let uploadedBytes = task.completedParts.reduce((sum, p) => {
      const chunkSize = p.partNumber === Math.ceil(task.fileSize / config.upload.chunkSize) 
        ? task.fileSize % config.upload.chunkSize || config.upload.chunkSize
        : config.upload.chunkSize
      return sum + chunkSize
    }, 0)

    const startTime = Date.now()
    const partProgress = new Map()

    const updateProgress = () => {
      let currentBytes = uploadedBytes
      partProgress.forEach(progress => {
        currentBytes += progress
      })

      const percent = (currentBytes / task.fileSize) * 100
      const elapsed = (Date.now() - startTime) / 1000
      const speed = currentBytes / elapsed
      const remaining = (task.fileSize - currentBytes) / speed

      callbacks.onProgress?.({
        percent: percent.toFixed(2),
        uploadedBytes: currentBytes,
        totalBytes: task.fileSize,
        speed: this.formatSpeed(speed),
        remaining: this.formatTime(remaining)
      })
    }

    const uploadQueue = [...chunkFiles]
    const concurrency = config.upload.concurrency
    const activeUploads = []

    while (uploadQueue.length > 0 || activeUploads.length > 0) {
      if (this.activeUploads.get(task.taskId)?.cancelled) {
        throw new Error('Upload cancelled')
      }

      while (activeUploads.length < concurrency && uploadQueue.length > 0) {
        const chunk = uploadQueue.shift()
        const url = urlMap.get(chunk.partNumber)

        if (!url) {
          console.error(`No URL for part ${chunk.partNumber}`)
          continue
        }

        const uploadPromise = this.uploadPart(url, chunk, (progress) => {
          const chunkBytes = (chunk.end - chunk.start) * (progress.progress / 100)
          partProgress.set(chunk.partNumber, chunkBytes)
          updateProgress()
        }).then(result => {
          partProgress.delete(chunk.partNumber)
          uploadedBytes += chunk.end - chunk.start
          uploadedParts.push(result)
          updateProgress()
          return result
        })

        activeUploads.push(uploadPromise)
      }

      if (activeUploads.length > 0) {
        await Promise.race(activeUploads)
        activeUploads.splice(
          activeUploads.findIndex(p => p === Promise.resolve(p)),
          1
        )
      }
    }

    await Promise.all(activeUploads)

    return uploadedParts
  }

  async splitFile(filePath, chunks) {
    const chunkFiles = []
    
    for (const chunk of chunks) {
      chunkFiles.push({
        filePath: filePath,
        partNumber: chunk.partNumber,
        start: chunk.start,
        end: chunk.end
      })
    }

    return chunkFiles
  }

  cancelUpload(taskId) {
    const upload = this.activeUploads.get(taskId)
    if (upload) {
      upload.cancelled = true
    }
  }

  getPendingTasks() {
    return this.pendingTasks
  }

  clearPendingTask(taskId) {
    this.pendingTasks = this.pendingTasks.filter(t => t.taskId !== taskId)
    this.savePendingTasks()
  }

  formatSpeed(bytesPerSecond) {
    if (bytesPerSecond < 1024) {
      return `${bytesPerSecond.toFixed(0)} B/s`
    } else if (bytesPerSecond < 1024 * 1024) {
      return `${(bytesPerSecond / 1024).toFixed(2)} KB/s`
    } else {
      return `${(bytesPerSecond / (1024 * 1024)).toFixed(2)} MB/s`
    }
  }

  formatTime(seconds) {
    if (isNaN(seconds) || seconds === Infinity) {
      return '--'
    }
    if (seconds < 60) {
      return `${Math.ceil(seconds)}s`
    } else if (seconds < 3600) {
      const minutes = Math.floor(seconds / 60)
      const secs = Math.ceil(seconds % 60)
      return `${minutes}m ${secs}s`
    } else {
      const hours = Math.floor(seconds / 3600)
      const minutes = Math.floor((seconds % 3600) / 60)
      return `${hours}h ${minutes}m`
    }
  }

  request(options) {
    return new Promise((resolve, reject) => {
      uni.request({
        ...options,
        header: {
          'Content-Type': 'application/json',
          ...options.header
        },
        success: (res) => {
          resolve(res)
        },
        fail: (error) => {
          reject(error)
        }
      })
    })
  }

  sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms))
  }
}

export default new UploadManager()
