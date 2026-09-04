# ProGuard 规则 - timetable-box

# 保留模型类（JSON 序列化/反序列化依赖反射）
-keep class com.timetable.box.model.** { *; }

# 保留 R 文件（防止资源 ID 被混淆）
-keep class com.timetable.box.R$* { *; }

# 保留 View 的构造方法（XML 布局实例化需要）
-keep class com.timetable.box.ui.ScheduleGridView {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
