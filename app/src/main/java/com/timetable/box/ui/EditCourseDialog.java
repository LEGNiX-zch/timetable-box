package com.timetable.box.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.DrawableCompat;

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
        setupColorGroup();
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
        if (c == null) return;
        etName.setText(c.name);
        int maxSpan = Period.TOTAL - c.startPeriod;
        int pos = c.span - 1;
        if (pos >= 0 && pos < maxSpan) spMerge.setSelection(pos);
        if (!TextUtils.isEmpty(c.name) || !TextUtils.isEmpty(c.color)) {
            btnDelete.setVisibility(View.VISIBLE);
        }
        for (int i = 0; i < COLOR_PALETTE.length; i++) {
            if (COLOR_PALETTE[i].equalsIgnoreCase(c.color)) {
                RadioButton rb = findRadioButton(i);
                if (rb != null) rb.setChecked(true);
                break;
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

    private void setupColorGroup() {
        for (int i = 0; i < COLOR_PALETTE.length; i++) {
            RadioButton rb = findRadioButton(i);
            if (rb == null) continue;
            try {
                int color;
                if (COLOR_PALETTE[i].isEmpty()) {
                    color = Color.WHITE;
                } else {
                    color = Color.parseColor(COLOR_PALETTE[i]);
                }
                // 兼容方案：用 DrawableCompat 设置 tint
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Drawable d = rb.getButtonDrawable();
                    if (d != null) {
                        d.setTint(color);
                    }
                } else {
                    Drawable d = rb.getButtonDrawable();
                    if (d != null) {
                        DrawableCompat.setTint(d, color);
                    }
                }
            } catch (Exception ignored) {}
        }
        RadioButton rb0 = findRadioButton(0);
        if (rb0 != null) rb0.setChecked(true);
    }

    private RadioButton findRadioButton(int index) {
        int id;
        switch (index) {
            case 0: id = R.id.rb_color_0; break;
            case 1: id = R.id.rb_color_1; break;
            case 2: id = R.id.rb_color_2; break;
            case 3: id = R.id.rb_color_3; break;
            case 4: id = R.id.rb_color_4; break;
            case 5: id = R.id.rb_color_5; break;
            case 6: id = R.id.rb_color_6; break;
            case 7: id = R.id.rb_color_7; break;
            case 8: id = R.id.rb_color_8; break;
            case 9: id = R.id.rb_color_9; break;
            default: return null;
        }
        return rgColor.findViewById(id);
    }

    private Course buildCourse() {
        Course c = original == null ? new Course() : original;
        c.day = targetDay;
        c.startPeriod = targetPeriod;
        c.name = etName.getText().toString().trim();

        int spanPos = spMerge.getSelectedItemPosition();
        c.span = spanPos >= 0 ? spanPos + 1 : 1;

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
