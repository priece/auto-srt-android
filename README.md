# Auto-SRT Android

[中文文档](README_zh_CN.md)

An Android application that automatically generates SRT subtitle files from video files using Volcengine's speech recognition API.

## Features

- 📹 Select video files from your device
- 🎵 Extract audio track from video
- 🗣️ Convert speech to text using Volcengine API
- 📝 Generate SRT subtitle files automatically
- ⚙️ Easy API key configuration
- 📋 Real-time log display for process monitoring and troubleshooting

## Screenshots

### Main Interface
- Select video files
- Start conversion process
- View conversion progress and results
- Monitor real-time logs with timestamps
- Clear logs as needed

### Settings
- Configure API Key and Access Key
- Save credentials securely

## Requirements

- Android 5.0 (API level 21) or higher
- Internet connection
- Volcengine API credentials (API Key and Access Key)

## How to Get API Credentials

1. Visit [Volcengine Console](https://console.volcengine.com/)
2. Register and create a new application
3. Navigate to the Speech Recognition service
4. Obtain your API Key and Access Key
5. For more information, see [Volcengine Speech Recognition Documentation](https://www.volcengine.com/docs/6561/1354868)

## Installation

1. Clone this repository:
```bash
git clone https://github.com/yourusername/auto-srt-android.git
```

2. Open the project in Android Studio

3. Build and run the application on your device or emulator

## Usage

1. **Configure API Keys**
   - Open the app and tap "API配置" (API Configuration)
   - Enter your Volcengine API Key and Access Key
   - Tap "保存配置" (Save Configuration)

2. **Convert Video to Subtitles**
   - Tap "选择视频文件" (Select Video File)
   - Choose a video from your device
   - Tap "开始转换" (Start Conversion)
   - Monitor the conversion process in the log area
   - Wait for the process to complete

3. **Monitor Process Logs**
   - View detailed step-by-step logs during conversion
   - Check timestamps for each operation
   - Review error messages if conversion fails
   - Tap "清除日志" (Clear Logs) to clear the log display

4. **Access Subtitles**
   - The generated SRT file will be saved in the app's data directory
   - The file name will match the video file name with a `.srt` extension
   - Check the log for the exact file path

## Project Structure

```
app/
├── src/main/
│   ├── java/com/example/autosrt/
│   │   ├── MainActivity.kt          # Main activity
│   │   ├── SettingsActivity.kt      # API configuration
│   │   ├── AudioExtractor.kt        # Audio extraction utility
│   │   ├── VolcEngineAPI.kt         # Volcengine API client
│   │   └── SRTGenerator.kt          # SRT file generator
│   ├── res/
│   │   ├── layout/                  # UI layouts
│   │   └── values/                  # Resources
│   └── AndroidManifest.xml
└── build.gradle
```

## Technical Details

### Audio Extraction
- Uses Android's MediaExtractor and MediaMuxer APIs
- Extracts audio track from video files
- Outputs single-channel audio

### API Integration
- Implements Volcengine's Big Model Speech Recognition API
- Supports base64 audio data upload
- Handles asynchronous task submission and querying

### SRT Generation
- Parses API response with timing information
- Formats output according to SRT specifications
- Handles multiple response formats

## Dependencies

- OkHttp: HTTP client for API requests
- Gson: JSON serialization/deserialization
- AndroidX: Android support libraries
- Material Design Components: UI components

## Permissions

The app requires the following permissions:
- `INTERNET`: For API communication
- `READ_EXTERNAL_STORAGE`: To read video files
- `WRITE_EXTERNAL_STORAGE`: To save subtitle files

## Limitations

- Maximum audio file size depends on API limits
- Processing time varies based on audio length
- Requires stable internet connection

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is open source and available under the [MIT License](LICENSE).

## Acknowledgments

- [Volcengine](https://www.volcengine.com/) for providing the speech recognition API
- Android development community

## Support

If you encounter any issues or have questions, please:
1. Check the [Issues](https://github.com/yourusername/auto-srt-android/issues) page
2. Create a new issue if your problem isn't already listed
3. Provide detailed information about your problem

## Troubleshooting

### Using the Log Feature
The app includes a comprehensive logging system to help diagnose issues:

1. **Log Display Area**
   - Located below the control buttons
   - Shows timestamped messages for each operation
   - Automatically scrolls to show the latest log entries

2. **Log Information Includes**
   - Video selection and file information
   - Audio extraction progress and results
   - API communication details (task ID, polling status)
   - SRT generation status and file paths
   - Detailed error messages with stack traces

3. **Common Issues**
   - **Audio Extraction Failed**: Check if the video file is valid and contains an audio track
   - **API Call Failed**: Verify your API credentials in settings
   - **Network Error**: Ensure you have a stable internet connection
   - **File Save Failed**: Check storage permissions

## Changelog

### Version 1.1.0 (2026-01-04)
- Added real-time log display feature
- Added timestamp for each log entry
- Added clear log functionality
- Improved error reporting with detailed stack traces
- Enhanced troubleshooting capabilities

### Version 1.0.0
- Initial release
- Basic video to subtitle conversion
- API key configuration
- SRT file generation