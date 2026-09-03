package com.timetable.box.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Canvas 渲染整张课表 -> 一键导出图片。
 *
 * 保存路径：内部存储 Download/timetable-box/
 * 不存在则自动创建。
 */
public final class ExportHelper {

    private ExportHelper() {}

    /**
     * 把 ScheduleGridView 渲染成 PNG 文件。
     *
     * @param ctx   Context
     * @param grid  课表网格 View
     * @return 成功返回 File，失败返回 null
     */
    public static File export(Context ctx, ScheduleGridView grid) {
        // 保存目录
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "timetable-box");
        if (!dir.exists()) {
            boolean ok = dir.mkdirs();
            if (!ok && !dir.exists()) {
                // 回退到应用外部 filesDir
                dir = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                        "timetable-box");
                if (!dir.exists()) dir.mkdirs();
            }
        }
        if (dir == null || !dir.exists()) return null;

        // 画布尺寸：采用 View 原始尺寸的 2x 以提高清晰度
        int w = Math.max(grid.getWidth(), 1) * 2;
        int h = Math.max(grid.getHeight(), 1) * 2;
        if (w < 100) w = 1200;
        if (h < 100) h = 1600;

        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        cv.scale(2f, 2f);
        grid.drawToCanvas(cv);

        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File out = new File(dir, "timetable_" + ts + ".png");

        try (FileOutputStream fos = new FileOutputStream(out)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            return out;
        } catch (IOException e) {
            return null;
        } finally {
            bmp.recycle();
        }
    }
}
