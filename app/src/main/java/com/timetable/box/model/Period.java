package com.timetable.box.model;

/**
 * 课时/节次定义。
 * 合计 11 节：1 节早读、上午 4 节、下午 3 节、晚自习 3 节。
 */
public final class Period {

    public static final int TOTAL = 11;

    /** 课时索引：0~10 */
    public static final int ZAO_DU = 0;   // 早读
    public static final int AM_1   = 1;
    public static final int AM_2   = 2;
    public static final int AM_3   = 3;
    public static final int AM_4   = 4;
    public static final int PM_1   = 5;
    public static final int PM_2   = 6;
    public static final int PM_3   = 7;
    public static final int WD_1   = 8;   // 晚自习 1
    public static final int WD_2   = 9;
    public static final int WD_3   = 10;

    /** 分组名称（用于表头第一列左侧的标签） */
    public static final String[] GROUP_LABELS = new String[]{
            "早读", "上午", "", "", "", "下午", "", "", "晚自习", "", ""
    };

    /** 课时号（显示在每一行左侧） */
    public static final String[] PERIOD_LABELS = new String[]{
            "早读", "1", "2", "3", "4", "5", "6", "7", "1", "2", "3"
    };

    private Period() {}
}
