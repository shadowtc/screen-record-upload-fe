# Contributing Guide

Thank you for considering contributing to the Screen Recording and Chunked Upload Demo project!

## How to Contribute

### Reporting Bugs

If you find a bug, please open an issue with:
- Clear description of the bug
- Steps to reproduce
- Expected behavior
- Actual behavior
- Device information (Android version, device model)
- Screenshots or screen recordings if applicable
- Logs from logcat if available

### Suggesting Features

Feature suggestions are welcome! Please open an issue with:
- Clear description of the feature
- Use case and motivation
- Proposed implementation (if you have ideas)
- Any relevant examples from other apps

### Pull Requests

1. **Fork the repository** and create your branch from `main`

2. **Make your changes**:
   - Follow existing code style and conventions
   - Add comments for complex logic
   - Update documentation if needed
   - Test thoroughly on Android devices

3. **Test your changes**:
   - Test recording at different resolutions
   - Test upload with various file sizes
   - Test resume functionality
   - Test error scenarios (network loss, permissions denied, etc.)
   - Verify on multiple Android versions if possible

4. **Commit your changes**:
   - Use clear, descriptive commit messages
   - Reference issue numbers if applicable
   - Keep commits focused and atomic

5. **Submit a pull request**:
   - Describe what your PR does
   - Link to related issues
   - Include screenshots/videos of changes
   - List any breaking changes

## Development Setup

1. Clone the repository
2. Install dependencies (if using CLI)
3. Open in HBuilderX or use uni-app CLI
4. Connect Android device or emulator
5. Run the app and test

See [DEVELOPMENT.md](DEVELOPMENT.md) for detailed setup instructions.

## Code Style Guidelines

### JavaScript/Vue
- Use ES6+ features
- camelCase for variables and functions
- PascalCase for component names
- 2 spaces for indentation
- Single quotes for strings
- Semicolons optional but be consistent
- Meaningful variable names

Example:
```javascript
async function uploadFile(filePath, fileName) {
  try {
    const result = await uploadManager.uploadFile(filePath, fileName)
    return result
  } catch (error) {
    console.error('Upload failed:', error)
    throw error
  }
}
```

### Java (Android)
- PascalCase for class names
- camelCase for methods and variables
- UPPER_SNAKE_CASE for constants
- 4 spaces for indentation
- Follow Android conventions
- Add JavaDoc for public methods

Example:
```java
public class ScreenRecorderModule extends UniModule {
    private static final String TAG = "ScreenRecorder";
    
    /**
     * Start screen recording with specified options
     */
    @UniJSMethod(uiThread = true)
    public void startRecord(JSONObject options, UniJSCallback callback) {
        // Implementation
    }
}
```

### Vue Components
- Single File Component structure
- Options API style
- Props validation
- Scoped styles
- Descriptive class names

Example:
```vue
<template>
  <view class="container">
    <text class="title">{{ title }}</text>
  </view>
</template>

<script>
export default {
  data() {
    return {
      title: 'Screen Recorder'
    }
  }
}
</script>

<style scoped>
.container {
  padding: 20px;
}
</style>
```

## Documentation

- Update README.md for user-facing changes
- Update DEVELOPMENT.md for developer-facing changes
- Update USAGE.md for usage instructions
- Add JSDoc/JavaDoc comments for new functions
- Keep CHANGELOG.md updated

## Testing

### Manual Testing Checklist

- [ ] Recording starts successfully
- [ ] Recording stops and saves correctly
- [ ] Video preview works
- [ ] Upload initializes
- [ ] Upload progress updates in real-time
- [ ] Upload completes successfully
- [ ] Resume works after app kill
- [ ] Permissions are handled correctly
- [ ] Errors display appropriate messages
- [ ] Settings persist across sessions

### Testing on Multiple Devices

Please test on:
- Different Android versions (API 21-33)
- Different screen sizes (phone and tablet)
- Different manufacturers (Samsung, Pixel, Xiaomi, etc.)
- Different network conditions (Wi-Fi, mobile data, slow connections)

## Questions?

If you have questions or need help:
1. Check existing documentation
2. Search existing issues
3. Open a new issue with your question
4. Be patient and respectful

## Code of Conduct

- Be respectful and inclusive
- Help others learn and grow
- Focus on constructive feedback
- Assume good intentions
- No harassment or discrimination

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
