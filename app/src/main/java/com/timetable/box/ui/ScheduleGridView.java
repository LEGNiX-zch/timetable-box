package com.timetable.box.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.timetable.box.R;
import com.timetable.box.model.Course;
import com.timetable.box.model.Period;
import com.timetable.box.storage.ScheduleStore;

import java.util.List;
import java.util.Map;

/**
 * 自定义课表网格 View。
 *
 * 运行时自动识别屏幕尺寸：
 *   - 小屏/手表（sw < 360dp 或对角线 < 5inch）  -> compact 紧凑布局
 *   - 普通手机                                     -> 标准布局
 *
 * 同时支持外部调用 drawToCanvas(Canvas) 进行图片导出。
 */
public class ScheduleGridView extends View {

    // 列数：7 = 周一~周日
    public static final int COLS = 7;
    // 行数：11 = 11 节
    public static final int ROWS = Period.TOTAL;

    private static final String[] DAY_LABELS = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    private List<Course> courses;
    private Map<String, Course> courseIndex;
    private OnCellClickListener cellClickListener;

    // 尺寸相关
    private boolean compact; // 手表/小屏模式
    private float cellWidth;
    private float rowHeight;
    private float headerHeight;
    private float leftLabelWidth;
    private float pad;

    private int textSizeDay;
    private int textSizeCourse;
    private int textSizePeriod;

    // 位置缓存（用于 hit-testing）
    private RectF[][] cellRects = new RectF[COLS][ROWS];
    private RectF[] headerRects = new RectF[COLS]; // 顶部星期
    private RectF[] labelRects = new RectF[ROWS];  // 左侧课时标签

    // 绘制用 Paint
    private final Paint bgPaint = new Paint();
    private final Paint borderPaint = new Paint();
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint headerBgPaint = new Paint();

    public interface OnCellClickListener {
        void onCellClick(int day, int period);
    }

    public ScheduleGridView(Context context) {
        this(context, null);
    }

    public ScheduleGridView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScheduleGridView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        Resources r = context.getResources();
        DisplayMetrics dm = r.getDisplayMetrics();

        // 运行时判断是否手表/小屏
        compact = detectCompactMode(context, dm);

        if (compact) {
            leftLabelWidth = dp(dm, 26);
            headerHeight = dp(dm, 24);
            pad = dp(dm, 1);
            textSizeDay = sp(dm, 10);
            textSizePeriod = sp(dm, 10);
            textSizeCourse = sp(dm, 11);
        } else {
            leftLabelWidth = dp(dm, 36);
            headerHeight = dp(dm, 38);
            pad = dp(dm, 2);
            textSizeDay = sp(dm, 13);
            textSizePeriod = sp(dm, 12);
            textSizeCourse = sp(dm, 13);
        }

        float minRowHeight = compact ? dp(dm, 34) : dp(dm, 56);
        rowHeight = minRowHeight;

        // 画笔基础配置
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dp(dm, 0.5f));
        borderPaint.setColor(r.getColor(R.color.colorDivider, null));

        headerBgPaint.setColor(r.getColor(R.color.colorPrimary, null));
        headerBgPaint.setStyle(Paint.Style.FILL);

        bgPaint.setStyle(Paint.Style.FILL);
    }

    private static boolean detectCompactMode(Context ctx, DisplayMetrics dm) {
        // 方案1：使用 screenWidthDp
        float widthDp = dm.widthPixels / dm.density;
        if (widthDp < 300) return true;

        // 方案2：使用对角线估算（手表一般 < 4.5inch）
        WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            int w = dm.widthPixels;
            int h = dm.heightPixels;
            double diagInches = Math.sqrt(w * w + h * h) / dm.densityDpi;
            if (diagInches < 4.5) return true;
        }
        return false;
    }

    private static float dp(DisplayMetrics dm, float v) {
        return v * dm.density;
    }

    private static int sp(DisplayMetrics dm, int v) {
        return (int) (v * dm.scaledDensity + 0.5f);
    }

    public void setCourses(List<Course> courses) {
        this.courses = courses;
        this.courseIndex = ScheduleStore.buildIndex(courses);
        requestLayout();
        invalidate();
    }

    public List<Course> getCourses() {
        return courses;
    }

    public void setOnCellClickListener(OnCellClickListener l) {
        this.cellClickListener = l;
    }

    // ====================== 测量/布局 ======================

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);

        Resources r = getResources();
        DisplayMetrics dm = r.getDisplayMetrics();

        // 可用宽度 = w - leftLabelWidth
        float availW = w - leftLabelWidth;
        cellWidth = availW / COLS;

        // 可用高度 = h - headerHeight
        float availH = h - headerHeight;
        // 根据 ROWS 反算 rowHeight，保证整体填满
        float fitRow = availH / ROWS;
        rowHeight = Math.max(rowHeight, fitRow);

        // 让高度自适应（wrap 所有内容）
        int contentHeight = (int) (headerHeight + rowHeight * ROWS);
        int contentWidth = (int) (leftLabelWidth + cellWidth * COLS);

        setMeasuredDimension(
                resolveSize(contentWidth, widthMeasureSpec),
                resolveSize(contentHeight, heightMeasureSpec)
        );
    }

    // ====================== 绘制 ======================

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawContent(canvas, 0, 0, getWidth(), getHeight());
    }

    /**
     * 把课表渲染到外部 Canvas，供导出图片复用。
     */
    public void drawToCanvas(Canvas canvas) {
        drawContent(canvas, 0, 0, canvas.getWidth(), canvas.getHeight());
    }

    private void drawContent(Canvas canvas, int offsetX, int offsetY, int totalW, int totalH) {
        Resources r = getResources();
        DisplayMetrics dm = r.getDisplayMetrics();

        float left = offsetX + leftLabelWidth;
        float top = offsetY + headerHeight;

        // 背景
        canvas.drawColor(Color.WHITE);

        // 1) 顶部星期表头
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(textSizeDay);
        textPaint.setColor(Color.WHITE);
        float headerTextY = offsetY + headerHeight / 2f + (textPaint.getTextSize() / 2f);
        for (int c = 0; c < COLS; c++) {
            float x0 = left + c * cellWidth;
            float x1 = x0 + cellWidth;
            headerRects[c] = new RectF(x0, offsetY, x1, offsetY + headerHeight);
            canvas.drawRect(headerRects[c], headerBgPaint);
            canvas.drawText(DAY_LABELS[c], (x0 + x1) / 2f, headerTextY, textPaint);
            // 分隔线
            canvas.drawLine(x1, offsetY, x1, offsetY + headerHeight, borderPaint);
        }
        // 表头下横线
        canvas.drawLine(offsetX, offsetY + headerHeight, offsetX + leftLabelWidth + cellWidth * COLS, offsetY + headerHeight, borderPaint);

        // 左侧纵轴竖线
        canvas.drawLine(left, offsetY, left, top + rowHeight * ROWS, borderPaint);

        // 2) 绘制每一行（先画默认边框 + 左侧课时标记）
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(textSizePeriod);
        textPaint.setColor(r.getColor(R.color.colorTextSecondary, null));
        for (int row = 0; row < ROWS; row++) {
            float y0 = top + row * rowHeight;
            float y1 = y0 + rowHeight;

            // 左侧课时标签
            RectF labelRect = new RectF(offsetX, y0, offsetX + leftLabelWidth, y1);
            labelRects[row] = labelRect;
            // 分隔：早读/上午/下午/晚自习 浅色背景
            int group = rowGroup(row);
            if (group >= 0 && labelGroupStart(group) == row) {
                bgPaint.setColor(0xFFF0F2F5);
                canvas.drawRect(labelRect, bgPaint);
            }

            String label = Period.PERIOD_LABELS[row];
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float cy = (y0 + y1) / 2f - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(label, offsetX + leftLabelWidth / 2f, cy, textPaint);

            // 横线
            canvas.drawLine(offsetX, y1, offsetX + leftLabelWidth + cellWidth * COLS, y1, borderPaint);
        }

        // 3) 画课程块（先填充背景，再画文字，再画外框）
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(textSizeCourse);
        textPaint.setColor(r.getColor(R.color.colorTextPrimary, null));

        // 先初始化所有 cellRects 为空；后续被课程覆盖的只画外框
        for (int col = 0; col < COLS; col++) {
            for (int row = 0; row < ROWS; row++) {
                float x0 = left + col * cellWidth;
                float x1 = x0 + cellWidth;
                float y0 = top + row * rowHeight;
                float y1 = y0 + rowHeight;
                cellRects[col][row] = new RectF(x0, y0, x1, y1);
            }
        }

        if (courses != null) {
            for (Course c : courses) {
                // 只在块的"起点"处画（合并单元格的主块）
                if (c.startPeriod < 0 || c.startPeriod >= ROWS) continue;
                if (c.day < 0 || c.day >= COLS) continue;

                float x0 = left + c.day * cellWidth + pad;
                float x1 = x0 + cellWidth - pad * 2;
                float y0 = top + c.startPeriod * rowHeight + pad;
                float y1 = top + Math.min(c.startPeriod + c.span, ROWS) * rowHeight - pad;

                bgPaint.setColor(parseColorSafe(c.color));
                canvas.drawRect(x0, y0, x1, y1, bgPaint);
                // 边框
                borderPaint.setColor(r.getColor(R.color.colorDivider, null));
                canvas.drawRect(x0, y0, x1, y1, borderPaint);

                // 文字
                if (c.name != null && !c.name.isEmpty()) {
                    drawCenteredText(canvas, c.name, x0, y0, x1, y1, textPaint);
                }
            }
        }

        // 最后画一个最外层框
        borderPaint.setColor(r.getColor(R.color.colorDivider, null));
        canvas.drawRect(
                offsetX + leftLabelWidth,
                offsetY,
                offsetX + leftLabelWidth + cellWidth * COLS,
                offsetY + headerHeight + rowHeight * ROWS,
                borderPaint
        );
    }

    private void drawCenteredText(Canvas canvas, String text,
                                  float x0, float y0, float x1, float y1, Paint p) {
        float availableW = x1 - x0 - pad * 2;
        float availableH = y1 - y0 - pad * 2;

        // 自动缩小字号适应
        p.setTextSize(textSizeCourse);
        while (p.measureText(text) > availableW && p.getTextSize() > dp(getResources().getDisplayMetrics(), 8)) {
            p.setTextSize(p.getTextSize() - 2f);
        }

        Paint.FontMetrics fm = p.getFontMetrics();
        float textH = fm.descent - fm.ascent;

        // 单行放不下就换行
        String[] lines = wrapText(text, p, availableW);
        int lineCount = lines.length;

        if (textH * lineCount > availableH) {
            // 空间不够就截断只显示第一行
            lineCount = 1;
            lines[0] = truncate(text, p, availableW);
        }

        float startY = (y0 + y1) / 2f - (lineCount * textH) / 2f - fm.ascent;
        for (int i = 0; i < lineCount; i++) {
            float cy = startY + i * textH;
            canvas.drawText(lines[i], (x0 + x1) / 2f, cy, p);
        }
    }

    private String[] wrapText(String text, Paint p, float maxW) {
        // 简单按字符换行
        java.util.List<String> out = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String tentative = cur.toString() + c;
            if (p.measureText(tentative) > maxW && cur.length() > 0) {
                out.add(cur.toString());
                cur.setLength(0);
            }
            cur.append(c);
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    private String truncate(String text, Paint p, float maxW) {
        String ellipsis = "…";
        if (p.measureText(text) <= maxW) return text;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String tentative = sb.toString() + text.charAt(i) + ellipsis;
            if (p.measureText(tentative) > maxW) break;
            sb.append(text.charAt(i));
        }
        return sb.toString() + ellipsis;
    }

    private static int parseColorSafe(String c) {
        if (c == null || c.isEmpty()) return 0xFFFFFFFF;
        try {
            return Color.parseColor(c);
        } catch (IllegalArgumentException e) {
            return 0xFFFFFFFF;
        }
    }

    /** 课时属于哪个组：0 早读, 1 上午, 2 下午, 3 晚自习 */
    private static int rowGroup(int row) {
        if (row == 0) return 0;
        if (row >= 1 && row <= 4) return 1;
        if (row >= 5 && row <= 7) return 2;
        if (row >= 8 && row <= 10) return 3;
        return -1;
    }

    private static int labelGroupStart(int group) {
        switch (group) {
            case 0: return 0;
            case 1: return 1;
            case 2: return 5;
            case 3: return 8;
        }
        return -1;
    }

    // ====================== 点击事件 ======================

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) return true;
        if (event.getAction() != MotionEvent.ACTION_UP) return false;

        float x = event.getX();
        float y = event.getY();

        // 点击了哪个单元格
        for (int col = 0; col < COLS; col++) {
            for (int row = 0; row < ROWS; row++) {
                RectF r = cellRects[col][row];
                if (r == null) continue;
                if (r.contains(x, y)) {
                    if (cellClickListener != null) {
                        cellClickListener.onCellClick(col, row);
                    }
                    return true;
                }
            }
        }
        return false;
    }
}
