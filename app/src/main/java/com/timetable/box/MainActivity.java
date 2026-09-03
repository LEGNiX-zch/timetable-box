package com.timetable.box;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.timetable.box.model.Course;
import com.timetable.box.storage.ScheduleStore;
import com.timetable.box.ui.EditCourseDialog;
import com.timetable.box.ui.ExportHelper;
import com.timetable.box.ui.ScheduleGridView;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_WRITE = 1001;

    private ScheduleGridView grid;
    private ScheduleStore store;
    private List<Course> courses;
    private Map<String, Course> index;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        store = new ScheduleStore(this);
        courses = new ArrayList<>(store.loadAll());
        rebuildIndex();

        grid = findViewById(R.id.schedule_grid);
        grid.setCourses(courses);

        grid.setOnCellClickListener((day, period) -> openEditDialog(day, period));

        findViewById(R.id.btn_export).setOnClickListener(v -> requestExport());
    }

    private void rebuildIndex() {
        index = ScheduleStore.buildIndex(courses);
    }

    /** 打开编辑弹窗 */
    private void openEditDialog(int day, int period) {
        // 当前格是否被某个已存在的课程块覆盖（可能是合并单元格的后续位置）
        Course existing = index.get(ScheduleStore.keyOf(day, period));
        boolean hasExisting = existing != null;

        // existing 指向的块（合并单元格场景下，它的 day/startPeriod 就是块真正的起点）
        final Course originBlock = hasExisting ? existing : null;

        Course editing;
        if (hasExisting) {
            editing = new Course(originBlock.day, originBlock.startPeriod, originBlock.span,
                    originBlock.name, originBlock.color);
        } else {
            editing = new Course();
            editing.day = day;
            editing.startPeriod = period;
        }

        // 弹窗里保存的 day 强制锁定为点击目标列（避免用户误改 day）
        final int targetDay = day;

        EditCourseDialog dlg = new EditCourseDialog(this, targetDay, editing.startPeriod, editing);

        dlg.setOnSaveListener(c -> {
            // 新块起止范围（按点击目标 day 和用户选的 startPeriod / span）
            int newDay = targetDay;
            int newStart = editing.startPeriod;
            int newEnd = newStart + c.span;

            // 1. 先把新块会覆盖到的所有已有块清掉（包括 originBlock 本身）
            Iterator<Course> it = courses.iterator();
            while (it.hasNext()) {
                Course x = it.next();
                if (x.day != newDay) continue;
                int xEnd = x.startPeriod + x.span;
                // period 区间有重叠则删
                if (!(xEnd <= newStart || x.startPeriod >= newEnd)) {
                    it.remove();
                }
            }

            // 2. 如果用户填了名字（非纯空）才真正保存新块
            if (c.name != null && !c.name.isEmpty()) {
                courses.add(new Course(newDay, newStart, c.span, c.name, c.color));
            }

            persistAndRefresh();
        });

        dlg.setOnDeleteListener(() -> {
            if (originBlock == null) return; // 空白格没有可删除的
            // 精确删除 originBlock
            Iterator<Course> it = courses.iterator();
            while (it.hasNext()) {
                Course x = it.next();
                if (x.day == originBlock.day && x.startPeriod == originBlock.startPeriod) {
                    it.remove();
                }
            }
            persistAndRefresh();
        });

        dlg.show();
    }

    private void persistAndRefresh() {
        store.saveAll(courses);
        rebuildIndex();
        grid.setCourses(courses);
    }

    // ====================== 导出权限 + 操作 ======================

    private void requestExport() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 分区存储，写 public Downloads 已不需要 WRITE_EXTERNAL_STORAGE
            doExport();
            return;
        }
        if (hasStoragePermission()) {
            doExport();
            return;
        }
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_WRITE);
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return true;
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_WRITE) return;
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            doExport();
        } else {
            Toast.makeText(this, R.string.tip_permission_denied, Toast.LENGTH_LONG).show();
        }
    }

    private void doExport() {
        File out = ExportHelper.export(this, grid);
        if (out != null) {
            Toast.makeText(this,
                    getString(R.string.tip_export_ok) + "\n" + out.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.tip_export_fail, Toast.LENGTH_LONG).show();
        }
    }
}
