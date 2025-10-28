# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2024-10-28

### Added
- Initial release of screen recording and chunked upload demo
- Android screen recording using MediaProjection and MediaRecorder APIs
- Native module with Foreground Service for stable recording
- MP4 output with H.264 video codec and AAC audio codec
- Configurable recording settings (resolution, fps, bitrate, audio)
- Permission handling for screen capture and microphone
- Chunked multipart upload with 8MB chunks
- Concurrent upload support (4 parallel connections)
- Exponential backoff retry mechanism (up to 5 retries)
- Upload resume capability using local storage
- Progress tracking with speed and remaining time display
- Video preview after recording
- Pending uploads management
- Backend API integration for MinIO multipart upload
- Configurable backend URL
- iOS stub implementation (placeholders for future ReplayKit integration)
- Comprehensive documentation (README, DEVELOPMENT, USAGE guides)
- Error handling for network failures, storage issues, and permissions

### Features
- **Recording**
  - Resolution support: 480p, 720p, 1080p
  - Frame rate: 30 fps (configurable)
  - Bitrate: ~3 Mbps (configurable)
  - Optional microphone audio recording
  - Background recording support via Foreground Service
  
- **Upload**
  - Multipart upload flow: init → get URLs → upload parts → complete
  - ETag validation for uploaded parts
  - Resume interrupted uploads after app restart
  - Real-time progress with speed and time estimates
  - Network error recovery
  - Configurable chunk size and concurrency
  
- **UI**
  - Clean, modern interface
  - Recording status indicator
  - Video metadata display (duration, size, resolution)
  - Video preview player
  - Upload progress bar with statistics
  - Pending uploads list with resume buttons
  - Settings for backend URL configuration
  - Error messages and user feedback

### Technical Details
- Minimum Android API level: 21 (Android 5.0)
- Target Android API level: 33 (Android 13)
- uni-app framework version: 2.0.0+
- Video format: MP4 (H.264 + AAC)
- Default chunk size: 8MB
- Default concurrency: 4 uploads
- Default max retries: 5
- Retry delay: Exponential backoff starting at 1 second

### Documentation
- README.md: Project overview and quick start
- DEVELOPMENT.md: Developer guide with architecture details
- USAGE.md: End-user guide with troubleshooting
- PROJECT_STRUCTURE.md: Detailed project structure documentation
- config.example.js: Example configuration file

### Known Limitations
- iOS recording not implemented (stubs only)
- Screen recording may not work in emulators
- Audio recording requires microphone permission
- Large files may take time to upload on slow connections
- Upload resume requires file to still exist on device

### Dependencies
- uni-app framework
- Android MediaProjection API
- Android MediaRecorder API
- MinIO-compatible backend API

## [Unreleased]

### Planned
- iOS implementation using ReplayKit framework
- Video compression options
- Multiple quality presets
- Pause/resume recording feature
- Screenshot capture
- Editing capabilities (trim, crop)
- Upload queue management
- Bandwidth throttling options
- Dark mode support
- Localization (multi-language support)
