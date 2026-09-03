package com.timetable.box.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.timetable.box.model.Course;
import com.timetable.box.model.Period;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * SharedPreferences 存储层。
 * 课表以 JSON 数组形式序列化持久化；单 APP 重启不丢数据。
 * 零网络请求，无登录。
 */
public class ScheduleStore {

    private static final String PREF_NAME = "schedule_box_prefs";
    private static final String KEY_COURSES = "courses_json";

    private final SharedPreferences sp;

    public ScheduleStore(Context ctx) {
        this.sp = ctx.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /** 读取全部课程 */
    public List<Course> loadAll() {
        List<Course> list = new ArrayList<>();
        String raw = sp.getString(KEY_COURSES, null);
        if (TextUtils.isEmpty(raw)) return list;
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Course c = Course.fromJson(o);
                // 防御性校验
                if (c.day < 0 || c.day > 6) continue;
                if (c.startPeriod < 0 || c.startPeriod >= Period.TOTAL) continue;
                if (c.startPeriod + c.span > Period.TOTAL) {
                    c.span = Period.TOTAL - c.startPeriod;
                }
                if (c.span < 1) c.span = 1;
                list.add(c);
            }
        } catch (JSONException ignored) {}
        return list;
    }

    /** 保存全部课程（整体写回） */
    public void saveAll(List<Course> courses) {
        JSONArray arr = new JSONArray();
        for (Course c : courses) arr.put(c.toJson());
        sp.edit().putString(KEY_COURSES, arr.toString()).apply();
    }

    /**
     * 按 (day, startPeriod) 定位的唯一键，找对应课程块；
     * 支持合并单元格时，返回覆盖此格且处于"合并顶部"的那个块。
     */
    public static String keyOf(int day, int period) {
        return day + "_" + period;
    }

    /**
     * 把课程列表建一个快速索引 map：key -> Course。
     * key 为每个块的 (day, startPeriod)，后续的被合并格也指向同一块。
     */
    public static Map<String, Course> buildIndex(List<Course> courses) {
        Map<String, Course> map = new HashMap<>();
        if (courses == null) return map;
        for (Course c : courses) {
            for (int p = c.startPeriod; p < c.startPeriod + c.span; p++) {
                map.put(keyOf(c.day, p), c);
            }
        }
        return map;
    }

    /** 清空全部 */
    public void clear() {
        sp.edit().remove(KEY_COURSES).apply();
    }
}
