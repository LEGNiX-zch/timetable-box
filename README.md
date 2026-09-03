# timetable-box

> 一个轻量化、离线可用的 Android 课程表 App —— 极简、纯净、无网络请求、无账号登录。

## 特性

- **11 节课表**：早读 + 上午 4 节 + 下午 3 节 + 晚自习 3 节；周一 ~ 周日表头，日历网格样式
- **一键编辑**：点击任意单元格弹窗，仅填写「课程名称」，可选背景标记色与合并课时数（rowSpan）
- **离线存储**：所有数据 SharedPreferences 本地持久化，重启 APP 数据不丢失
- **图片导出**：Canvas 渲染整张课表为 PNG，保存路径 `内部存储/Download/timetable-box/`（目录不存在自动创建）
- **权限合规**：导出前主动申请存储权限；拒绝则弹窗提示、不执行导出
- **自适应 UI**：
  - 手表 / 小屏幕（sw < 300dp 或对角线 < 4.5 inch）：自动启用 compact 模式，整体缩放压缩布局，完整显示全部课表，不需要大量滑动
  - 普通手机：使用标准偏大的显示尺寸
- **APK 最小化**：纯 Java，无额外三方依赖，仅 AppCompat + MaterialCore，minify + shrinkResources 开启

## 编译与兼容

| 项目 | 值 |
| --- | --- |
| minSdkVersion | 27（Android 8.1） |
| targetSdkVersion | 34 |
| ABI | `armeabi-v7a` + `arm64-v8a`（单 APK 同时支持 32/64 位） |
| 构建工具 | Gradle 8.4 + AGP 8.1.4 + JDK 11 |

## 构建

```bash
./gradlew assembleDebug   # 或 assembleRelease
# APK 输出：
#   app/build/outputs/apk/debug/app-debug.apk
#   app/build/outputs/apk/release/app-release.apk
```

## 项目结构

```
app/src/main/java/com/timetable/box/
├── MainActivity.java           # 主入口，串联 UI + 存储 + 导出
├── model/
│   ├── Period.java             # 11 节课时常量
│   └── Course.java             # 课程数据模型 + JSON 序列化
├── storage/
│   └── ScheduleStore.java      # SharedPreferences 持久化
└── ui/
    ├── ScheduleGridView.java   # 自定义课表网格 View（绘制 + 触摸 + 导出）
    ├── EditCourseDialog.java   # 课程编辑弹窗
    └── ExportHelper.java       # Canvas → PNG 导出
```

## 关于 Git 仓库

- 项目全部源码、资源均为普通文件提交，**不使用 `git submodule`，不引用外部文件链接**
- **不存在嵌套 `.git` 目录**，全部以普通文件形式提交
- GitHub Actions 仅做编译校验，**不会自动打包 APK 到 Release**

## License

MIT © LEGNiX-zch
