# 编译打包说明

## 前置条件

1. **安装Android Studio**
   - 下载地址: https://developer.android.com/studio
   - 安装最新版本的Android Studio

2. **配置Android SDK**
   - 打开Android Studio
   - 进入 Settings -> Appearance & Behavior -> System Settings -> Android SDK
   - 确保已安装 Android SDK Platform 34 (或项目中指定的版本)
   - 确保已安装 Android SDK Build-Tools

3. **Java开发环境**
   - 确保已安装 JDK 8 或更高版本
   - 配置 JAVA_HOME 环境变量

## 使用Android Studio编译

### 方法一：在Android Studio中编译

1. **打开项目**
   ```
   File -> Open -> 选择项目根目录 (e:\project_java\auto-srt-android)
   ```

2. **等待Gradle同步**
   - Android Studio会自动下载所需的依赖
   - 首次同步可能需要几分钟时间

3. **编译APK**
   - 菜单: Build -> Build Bundle(s) / APK(s) -> Build APK(s)
   - 或者点击工具栏的绿色锤子图标

4. **查找生成的APK**
   - 编译成功后，APK文件位于: `app/build/outputs/apk/debug/app-debug.apk`

### 方法二：使用命令行编译

1. **确保在项目根目录**
   ```bash
   cd e:\project_java\auto-srt-android
   ```

2. **首次使用需要初始化Gradle Wrapper**
   - 在Android Studio中打开项目，它会自动生成wrapper
   - 或者手动下载gradle-wrapper.jar放到 gradle/wrapper/ 目录

3. **编译Debug版本**
   ```bash
   gradlew.bat assembleDebug
   ```

4. **编译Release版本**
   ```bash
   gradlew.bat assembleRelease
   ```

5. **清理项目**
   ```bash
   gradlew.bat clean
   ```

## 常见问题解决

### 问题1: Gradle同步失败
**解决方案:**
- 检查网络连接
- 使用代理或镜像源
- 在 gradle.properties 中添加:
  ```
  systemProp.http.proxyHost=127.0.0.1
  systemProp.http.proxyPort=7890
  systemProp.https.proxyHost=127.0.0.1
  systemProp.https.proxyPort=7890
  ```

### 问题2: SDK未找到
**解决方案:**
- 在项目根目录创建 local.properties 文件
- 添加SDK路径:
  ```
  sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
  ```

### 问题3: 依赖下载慢
**解决方案:**
- 使用阿里云Maven镜像，在项目的 build.gradle 中修改:
  ```gradle
  repositories {
      maven { url 'https://maven.aliyun.com/repository/google' }
      maven { url 'https://maven.aliyun.com/repository/public' }
      google()
      mavenCentral()
  }
  ```

### 问题4: 编译错误
**解决方案:**
- 清理项目: Build -> Clean Project
- 重新构建: Build -> Rebuild Project
- 使缓存失效: File -> Invalidate Caches / Restart

## 生成签名APK（发布版本）

1. **生成签名密钥**
   ```bash
   keytool -genkey -v -keystore auto-srt.keystore -alias auto-srt -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **在app/build.gradle中配置签名**
   ```gradle
   android {
       signingConfigs {
           release {
               storeFile file("path/to/auto-srt.keystore")
               storePassword "your_password"
               keyAlias "auto-srt"
               keyPassword "your_password"
           }
       }
       buildTypes {
           release {
               signingConfig signingConfigs.release
           }
       }
   }
   ```

3. **编译签名APK**
   - 菜单: Build -> Generate Signed Bundle / APK
   - 选择 APK
   - 选择密钥库和密钥
   - 选择 release 构建类型

## 输出文件位置

- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/app-release.apk`
- **AAB (App Bundle)**: `app/build/outputs/bundle/release/app-release.aab`

## 安装到设备

### 通过ADB安装
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 直接安装
- 将APK文件传输到Android设备
- 在设备上打开文件管理器
- 点击APK文件进行安装
- 需要在设置中允许"未知来源"的应用安装

## 注意事项

1. 首次编译会下载大量依赖，建议在网络状况良好时进行
2. Release版本需要签名才能发布到应用商店
3. 建议在真实设备上测试，模拟器可能无法完整测试视频和音频功能
4. 确保设备的Android版本在API 21 (Android 5.0) 以上

## 性能优化建议

- 使用 Release 版本进行性能测试
- 启用 ProGuard 进行代码混淆和优化
- 使用 App Bundle 格式可以减小应用体积

## 技术支持

如果遇到编译问题：
1. 查看Android Studio的 Build 输出窗口
2. 查看 Gradle Console 的详细错误信息
3. 搜索错误信息或在项目Issues中提问
