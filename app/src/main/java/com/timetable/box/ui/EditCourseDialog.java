package com.timetable.box.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.timetable.box.R;
import com.timetable.box.model.Course;
import com.timetable.box.model.Period;

import java.util.ArrayList;
import java.util.List;

/**
 * 课程编辑弹窗。
 * 只输入【课程名称】，可选【背景标记色】，可选【合并课时数】。
 * 移除老师、教室等多余项。
 */
public class EditCourseDialog {

    private static final String[] COLOR_PALETTE = {
            "",
            "#FDECEC", "#FFF3E0", "#FFF8DC", "#E8F5E9", "#E0F7FA",
            "#E3F2FD", "#F3E5F5", "#FCE4EC", "#ECEFF1"
    };

    private final AlertDialog dialog;
    private final Course original;
    private final int targetDay;
    private final int targetPeriod;

    private EditText etName;
    private Spinner spMerge;
    private RadioGroup rgColor;
    private TextView btnDelete;
    private TextView btnSave;
    private TextView btnCancel;

    private OnSaveListener saveListener;
    private OnDeleteListener deleteListener;

    public interface OnSaveListener {
        void onSave(Course course);
    }

    public interface OnDeleteListener {
        void onDelete();
    }

    public EditCourseDialog(@NonNull Context context,
                            int day, int period,
                            @NonNull Course editing) {
        this.targetDay = day;
        this.targetPeriod = period;
        this.original = editing;

        View v = LayoutInflater.from(context).inflate(R.layout.dialog_edit_course, null);
        etName = v.findViewById(R.id.et_course_name);
        spMerge = v.findViewById(R.id.sp_merge_count);
        rgColor = v.findViewById(R.id.rg_color);
        btnDelete = v.findViewById(R.id.btn_delete);
        btnSave = v.findViewById(R.id.btn_save);
        btnCancel = v.findViewById(R.id.btn_cancel);

        setupMergeSpinner(context, editing);
        setupColorGroup(context, editing.color);
        prefill(editing);

        dialog = new AlertDialog.Builder(context)
                .setView(v)
                .setCancelable(true)
                .create();

        btnDelete.setOnClickListener(v1 -> {
            if (deleteListener != null) deleteListener.onDelete();
            dialog.dismiss();
        });
        btnSave.setOnClickListener(v1 -> {
            Course c = buildCourse();
            if (saveListener != null) saveListener.onSave(c);
            dialog.dismiss();
        });
        btnCancel.setOnClickListener(v1 -> dialog.dismiss());
    }

    private void prefill(Course c) {
        if (c != null) {
            etName.setText(c.name);
            // 合并课时 spinner 选中项
            int maxSpan = Period.TOTAL - c.startPeriod;
            // Spinner 项已按 1..maxSpan 构造
            int pos = c.span - 1;
            if (pos >= 0 && pos < maxSpan) spMerge.setSelection(pos);
            // 删除按钮仅在编辑已有课程时显示
            if (!TextUtils.isEmpty(c.name) || !TextUtils.isEmpty(c.color)) {
                btnDelete.setVisibility(View.VISIBLE);
            }
            // 颜色选中
            for (int i = 0; i < COLOR_PALETTE.length; i++) {
                if (COLOR_PALETTE[i].equalsIgnoreCase(c.color)) {
                    RadioButton rb = findRadioButton(i);
                    if (rb != null) rb.setChecked(true);
                    break;
                }
            }
        }
    }

    private void setupMergeSpinner(Context ctx, Course c) {
        int startPeriod = c != null ? c.startPeriod : targetPeriod;
        int maxSpan = Math.max(1, Period.TOTAL - startPeriod);

        List<String> items = new ArrayList<>();
        for (int i = 1; i <= maxSpan; i++) items.add(String.valueOf(i));

        spMerge.setAdapter(new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, items));
    }

    private void setupColorGroup(Context ctx, String current) {
        for (int i = 0; i < COLOR_PALETTE.length; i++) {
            RadioButton rb = findRadioButton(i);
            if (rb == null) continue;
            // 给每个颜色设置 buttonTint
            try {
                int color;
                if (COLOR_PALETTE[i].isEmpty()) {
                    color = Color.WHITE;
                } else {
                    color = Color.parseColor(COLOR_PALETTE[i]);
                }
                rb.getButtonDrawable().setTint(color);
            } catch (Exception ignored) {}
        }
        // 默认选中 index 0 (无色)
        findRadioButton(0).setChecked(true);
    }

    private RadioButton findRadioButton(int index) {
        switch (index) {
            case 0: return rgColor.findViewById(R.id.rb_color_0);
            case 1: return rgColor.findViewById(R.id.rb_color_1);
            case 2: return rgColor.findViewById(R.id.rb_color_2);
            case 3: return rgColor.findViewById(R.id.rb_color_3);
            case 4: return rgColor.findViewById(R.id.rb_color_4);
            case 5: return rgColor.findViewById(R.id.rb_color_5);
            case 6: return rgColor.findViewById(R.id.rb_color_6);
            case 7: return rgColor.findViewById(R.id.rb_color_7);
            case 8: return rgColor.findViewById(R.id.rb_color_8);
            case 9: return rgColor.findViewById(R.id.rb_color_9);
        }
        return null;
    }

    private Course buildCourse() {
        Course c = original == null ? new Course() : original;
        c.day = targetDay;
        c.startPeriod = targetPeriod;
        c.name = etName.getText().toString().trim();

        int spanPos = spMerge.getSelectedItemPosition();
        c.span = spanPos >= 0 ? spanPos + 1 : 1;

        // 选中颜色
        int checkedId = rgColor.getCheckedRadioButtonId();
        int idx = indexOfChecked(checkedId);
        c.color = idx >= 0 ? COLOR_PALETTE[idx] : "";

        return c;
    }

    private static int indexOfChecked(int id) {
        if (id == R.id.rb_color_0) return 0;
        if (id == R.id.rb_color_1) return 1;
        if (id == R.id.rb_color_2) return 2;
        if (id == R.id.rb_color_3) return 3;
        if (id == R.id.rb_color_4) return 4;
        if (id == R.id.rb_color_5) return 5;
        if (id == R.id.rb_color_6) return 6;
        if (id == R.id.rb_color_7) return 7;
        if (id == R.id.rb_color_8) return 8;
        if (id == R.id.rb_color_9) return 9;
        return -1;
    }

    public void setOnSaveListener(OnSaveListener l) { this.saveListener = l; }
    public void setOnDeleteListener(OnDeleteListener l) { this.deleteListener = l; }
    public void show() { dialog.show(); }
}
