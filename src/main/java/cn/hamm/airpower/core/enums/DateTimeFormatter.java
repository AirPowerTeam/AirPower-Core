package cn.hamm.airpower.core.enums;

import cn.hamm.airpower.core.DateTimeUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * <h1>日期时间格式化模板</h1>
 *
 * @author Hamm.cn
 */
@Getter
@AllArgsConstructor
public enum DateTimeFormatter {
    /**
     * 年，如 {@code 2026}
     */
    YEAR("yyyy"),

    /**
     * 月，如 {@code 10}
     */
    MONTH("MM"),

    /**
     * 日，如 {@code 01}
     */
    DAY("dd"),

    /**
     * 24 小时制小时，如 {@code 23}
     */
    HOUR("HH"),

    /**
     * 分，如 {@code 59}
     */
    MINUTE("mm"),

    /**
     * 秒，如 {@code 59}
     */
    SECOND("ss"),

    /**
     * 年月日
     */
    FULL_DATE("yyyy-MM-dd"),

    /**
     * 时分秒
     */
    FULL_TIME("HH:mm:ss"),

    /**
     * 年月日时分秒
     */
    FULL_DATETIME("yyyy-MM-dd HH:mm:ss"),

    /**
     * 月日时分（<b>不含年份</b>，跨年数据会丢失年份信息）
     */
    SHORT_DATETIME("MM-dd HH:mm"),
    ;

    /**
     * 格式化模板
     */
    private final String value;

    /**
     * 使用这个模板格式化毫秒时间戳
     *
     * @param milliSecond 毫秒时间戳
     * @return 格式化后的字符串
     */
    public final @NotNull String format(long milliSecond) {
        return DateTimeUtil.format(milliSecond, this);
    }

    /**
     * 使用这个模板格式化当前时间
     *
     * @return 格式化后的字符串
     */
    public final @NotNull String formatCurrent() {
        return format(System.currentTimeMillis());
    }
}
