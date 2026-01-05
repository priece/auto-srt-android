# 视频转字幕Android应用

## 项目概述
这是一个Android应用，用于将视频文件转换为SRT格式的字幕文件。应用通过提取视频中的音频，然后使用火山引擎的语音识别API将音频转换为文字，最后生成SRT格式的字幕文件。

## 功能特性
- 从设备中选择视频文件
- 提取视频中的音频轨道
- 使用火山引擎API进行语音转文字
- 生成与视频同名的SRT字幕文件
- API密钥配置界面

## 项目结构
```
app/
├── src/main/
│   ├── java/com/example/autosrt/
│   │   ├── MainActivity.kt          # 主界面Activity
│   │   ├── SettingsActivity.kt      # API配置界面
│   │   ├── AudioExtractor.kt        # 音频提取工具类
│   │   ├── VolcEngineAPI.kt         # 火山引擎API调用类
│   │   └── SRTGenerator.kt          # SRT字幕生成工具类
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml    # 主界面布局
│   │   │   └── activity_settings.xml # 配置界面布局
│   │   └── values/
│   │       └── strings.xml          # 字符串资源
│   └── AndroidManifest.xml          # 应用配置文件
├── build.gradle                     # 模块构建配置
├── build.gradle                     # 项目构建配置
├── settings.gradle                  # 项目设置
├── gradle.properties                # Gradle配置
└── PROJECT_OVERVIEW.md              # 项目概览
```

## 依赖库
- OkHttp: HTTP请求处理
- Gson: JSON序列化/反序列化
- AndroidX: Android支持库
- Material Design: UI组件

## 核心流程
1. 用户选择视频文件
2. 提取视频中的音频（转换为MP3格式）
3. 使用火山引擎API将音频转换为文字
4. 将API返回的识别结果转换为SRT格式
5. 保存SRT文件到应用数据目录

## API配置
应用需要火山引擎的API Key和Access Key，用户可在"API配置"界面中设置。

## 权限
- INTERNET: 网络访问
- WRITE_EXTERNAL_STORAGE: 写入外部存储
- READ_EXTERNAL_STORAGE: 读取外部存储