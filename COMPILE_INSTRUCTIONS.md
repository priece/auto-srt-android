# 项目编译说明

## 当前状态

项目代码已经完成，包含以下内容：

### ✅ 已完成的内容

1. **核心功能代码**
   - MainActivity.kt - 主界面和业务逻辑
   - SettingsActivity.kt - API配置界面
   - AudioExtractor.kt - 音频提取工具
   - VolcEngineAPI.kt - 火山引擎API客户端
   - SRTGenerator.kt - SRT字幕生成器

2. **UI布局文件**
   - activity_main.xml - 主界面布局
   - activity_settings.xml - 设置界面布局

3. **资源文件**
   - strings.xml - 字符串资源
   - colors.xml - 颜色资源
   - styles.xml - 样式主题

4. **配置文件**
   - AndroidManifest.xml - 应用配置
   - build.gradle (项目级) - 项目构建配置
   - build.gradle (app级) - 模块构建配置
   - settings.gradle - Gradle设置
   - gradle.properties - Gradle属性
   - proguard-rules.pro - 代码混淆规则

5. **文档**
   - README.md - 英文说明文档
   - README_zh_CN.md - 中文说明文档
   - BUILD_GUIDE.md - 编译打包指南
   - .gitignore - Git忽略文件

### ⚠️ 编译前需要准备

由于Android项目的特殊性，直接命令行编译需要完整的环境配置。建议使用以下方式：

## 推荐编译方式：使用Android Studio

### 步骤 1: 安装Android Studio

1. 下载Android Studio: https://developer.android.com/studio
2. 安装并首次启动，会自动下载Android SDK

### 步骤 2: 打开项目

1. 启动Android Studio
2. 选择 "Open an Existing Project"
3. 选择项目目录: `e:\project_java\auto-srt-android`
4. 等待Gradle同步完成（首次会下载依赖，需要几分钟）

### 步骤 3: 编译APK

**方法一：使用菜单**
- Build -> Build Bundle(s) / APK(s) -> Build APK(s)

**方法二：使用快捷键**
- Windows/Linux: Ctrl + F9
- Mac: Cmd + F9

### 步骤 4: 查找生成的APK

编译成功后，APK位于：
```
app/build/outputs/apk/debug/app-debug.apk
```

## 备选方案：命令行编译

如果您熟悉命令行工具，可以按以下步骤操作：

### 前提条件
1. 已安装Android SDK
2. 已配置ANDROID_HOME环境变量
3. 已安装JDK 17+

### 步骤

1. **配置SDK路径**
   
   在项目根目录创建 `local.properties` 文件，内容如下：
   ```
   sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
   ```
   （根据实际SDK安装位置修改）

2. **初始化Gradle Wrapper**
   
   在Android Studio中打开一次项目，它会自动生成gradle-wrapper.jar

3. **编译项目**
   ```bash
   cd e:\project_java\auto-srt-android
   gradlew.bat assembleDebug
   ```

## 常见问题

### Q1: Gradle同步失败
**A:** 可能是网络问题，建议：
- 使用VPN或代理
- 使用国内镜像源（见BUILD_GUIDE.md）

### Q2: SDK未找到
**A:** 需要创建local.properties文件配置SDK路径

### Q3: 依赖下载慢
**A:** 修改build.gradle使用阿里云镜像

### Q4: 编译错误
**A:** 
1. 清理项目: Build -> Clean Project
2. 重新构建: Build -> Rebuild Project

## 项目结构

```
auto-srt-android/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/autosrt/     # Kotlin源代码
│   │   ├── res/                          # 资源文件
│   │   └── AndroidManifest.xml          # 应用配置
│   ├── build.gradle                      # 模块构建配置
│   └── proguard-rules.pro               # 混淆规则
├── gradle/wrapper/                       # Gradle包装器
├── build.gradle                          # 项目构建配置
├── settings.gradle                       # 项目设置
├── gradle.properties                     # Gradle属性
├── gradlew.bat                          # Windows Gradle脚本
├── .gitignore                           # Git忽略文件
├── README.md                            # 英文说明
├── README_zh_CN.md                      # 中文说明
└── BUILD_GUIDE.md                       # 详细编译指南
```

## 下一步

1. **使用Android Studio打开项目** - 这是最简单的方式
2. **等待依赖下载完成** - 首次可能需要5-10分钟
3. **连接Android设备或启动模拟器**
4. **点击运行按钮** - 直接在设备上运行测试
5. **或者编译APK** - 生成可安装的APK文件

## 技术栈

- **语言**: Kotlin
- **最低Android版本**: Android 5.0 (API 21)
- **目标Android版本**: Android 14 (API 34)
- **构建工具**: Gradle 8.0
- **主要依赖**:
  - OkHttp 4.12.0 - HTTP客户端
  - Gson 2.10.1 - JSON处理
  - AndroidX - Android支持库
  - Material Components - UI组件

## 联系与支持

如果遇到编译问题：
1. 查看详细的编译指南: BUILD_GUIDE.md
2. 检查Android Studio的错误输出
3. 查看项目Issues或创建新issue

---

**提示**: 第一次使用Android Studio编译Android项目是最简单的方式，IDE会自动处理大部分配置工作。
