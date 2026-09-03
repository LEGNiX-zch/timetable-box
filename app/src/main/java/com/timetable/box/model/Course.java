package com.timetable.box.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 一个课程块。
 * 数据以 (day, startPeriod, span) 定位一个可能跨多课时的矩形块。
 * day: 0=周一, 1=周二, ..., 6=周日
 * startPeriod: 起始课时索引 (0~10)
 * span: 跨越课时数 (1~11)
 */
public class Course {

    public int day;
    public int startPeriod;
    public int span;
    public String name;
    /** 背景标记色，#RRGGBB 或 #AARRGGBB */
    public String color;

    public Course() {
        this.name = "";
        this.color = "";
        this.span = 1;
    }

    public Course(int day, int startPeriod, int span, String name, String color) {
        this.day = day;
        this.startPeriod = startPeriod;
        this.span = Math.max(1, span);
        this.name = name == null ? "" : name;
        this.color = color == null ? "" : color;
    }

    public int endPeriodExclusive() {
        return startPeriod + span;
    }

    /** 这个块是否覆盖到指定 day + period 格 */
    public boolean covers(int d, int p) {
        if (d != day) return false;
        return p >= startPeriod && p < startPeriod + span;
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("d", day);
            o.put("s", startPeriod);
            o.put("n", span);
            o.put("name", name);
            o.put("c", color);
        } catch (JSONException ignored) {}
        return o;
    }

    public static Course fromJson(JSONObject o) {
        Course c = new Course();
        try {
            c.day = o.getInt("d");
            c.startPeriod = o.getInt("s");
            c.span = o.optInt("n", 1);
            c.name = o.optString("name", "");
            c.color = o.optString("c", "");
        } catch (JSONException ignored) {}
        return c;
    }
}
