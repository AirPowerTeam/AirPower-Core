package cn.hamm.airpower.core;

import cn.hamm.airpower.core.enums.DateTimeFormatter;
import cn.hamm.airpower.core.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>DateTimeUtil 单元测试</h1>
 *
 * @author Hamm.cn
 */
class DateTimeUtilTest {

    private static final long TEST_TIMESTAMP = 1704067200000L; // 2024-01-01 00:00:00 UTC
    private static final String TEST_DATE_STR = "2024-01-01 00:00:00";
    private static final String TEST_FORMAT = "yyyy-MM-dd HH:mm:ss";

    // ==================== format 方法测试 ====================

    @Test
    void testFormatWithDefaultFormatter() {
        String result = DateTimeUtil.format(TEST_TIMESTAMP);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testFormatWithStringFormatter() {
        String result = DateTimeUtil.format(TEST_TIMESTAMP, TEST_FORMAT);
        assertNotNull(result);
        assertEquals("2024-01-01 08:00:00", result); // 东八区时间
    }

    @Test
    void testFormatWithEnumFormatter() {
        String result = DateTimeUtil.format(TEST_TIMESTAMP, DateTimeFormatter.FULL_DATETIME);
        assertNotNull(result);
        assertEquals("2024-01-01 08:00:00", result); // 东八区时间
    }

    @Test
    void testFormatWithZoneId() {
        String result = DateTimeUtil.format(TEST_TIMESTAMP, TEST_FORMAT, ZoneId.of("UTC"));
        assertNotNull(result);
        assertEquals("2024-01-01 00:00:00", result);
    }

    @Test
    void testFormatWithDifferentZoneId() {
        String result = DateTimeUtil.format(TEST_TIMESTAMP, TEST_FORMAT, ZoneId.of("America/New_York"));
        assertNotNull(result);
        assertEquals("2023-12-31 19:00:00", result);
    }

    // ==================== formatCurrent 方法测试 ====================

    @Test
    void testFormatCurrentWithDefaultFormatter() {
        String result = DateTimeUtil.formatCurrent();
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testFormatCurrentWithStringFormatter() {
        String result = DateTimeUtil.formatCurrent(TEST_FORMAT);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testFormatCurrentWithEnumFormatter() {
        String result = DateTimeUtil.formatCurrent(DateTimeFormatter.FULL_DATETIME);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    // ==================== parse 方法测试 ====================

    @Test
    void testParseWithLong() {
        Date result = DateTimeUtil.parse(TEST_TIMESTAMP);
        assertNotNull(result);
        assertEquals(TEST_TIMESTAMP, result.getTime());
    }

    @Test
    void testParseWithStringAndDefaultFormatter() {
        Date result = DateTimeUtil.parse(TEST_DATE_STR);
        assertNotNull(result);
        // 解析后应该是东八区时间
        assertTrue(result.getTime() > 0);
    }

    @Test
    void testParseWithStringAndCustomFormatter() {
        // 使用包含日期和时间的格式
        Date result = DateTimeUtil.parse("2024-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss");
        assertNotNull(result);
        assertTrue(result.getTime() > 0);
    }

    @Test
    void testParseWithInvalidFormat() {
        // 格式不匹配应该抛出 ServiceException
        assertThrows(ServiceException.class, () ->
                DateTimeUtil.parse("2024-01-01", "yyyy-MM-dd HH:mm:ss")
        );
    }

    @Test
    void testParseWithInvalidDateString() {
        // 无效的日期字符串应该抛出 ServiceException
        assertThrows(ServiceException.class, () ->
                DateTimeUtil.parse("invalid-date", "yyyy-MM-dd HH:mm:ss")
        );
    }

    @Test
    void testParseWithEmptyString() {
        // 空字符串应该抛出 ServiceException
        assertThrows(ServiceException.class, () ->
                DateTimeUtil.parse("", "yyyy-MM-dd HH:mm:ss")
        );
    }

    // ==================== friendlyFormat 方法测试 ====================

    @Test
    void testFriendlyFormatMillisecond() {
        long now = System.currentTimeMillis();
        String result = DateTimeUtil.friendlyFormatMillisecond(now);
        assertNotNull(result);
        // 可能是"刚刚"或"0秒前"，取决于时间差
        assertTrue(result.equals("刚刚") || result.equals("0秒前"));
    }

    @Test
    void testFriendlyFormatSecond() {
        long now = System.currentTimeMillis() / 1000;
        String result = DateTimeUtil.friendlyFormatSecond(now);
        assertNotNull(result);
        // 可能是"刚刚"或"0秒前"，取决于时间差
        assertTrue(result.equals("刚刚") || result.equals("0秒前"));
    }

    @Test
    void testFriendlyFormatWithNegativeSecond() {
        assertThrows(ServiceException.class, () ->
                DateTimeUtil.friendlyFormatSecond(-1)
        );
    }

    @Test
    void testFriendlyFormatWithPastTime() {
        long pastSecond = System.currentTimeMillis() / 1000 - 120; // 2分钟前
        String result = DateTimeUtil.friendlyFormatSecond(pastSecond);
        assertNotNull(result);
        assertTrue(result.contains("前"));
    }

    @Test
    void testFriendlyFormatWithFutureTime() {
        long futureSecond = System.currentTimeMillis() / 1000 + 120; // 2分钟后
        String result = DateTimeUtil.friendlyFormatSecond(futureSecond);
        assertNotNull(result);
        assertTrue(result.contains("后"));
    }

    // ==================== getYear/getMonth/getDay 方法测试 ====================

    @Test
    void testGetYear() {
        Date date = DateTimeUtil.parse(TEST_TIMESTAMP);
        int year = DateTimeUtil.getYear(date);
        assertTrue(year >= 2024);
    }

    @Test
    void testGetCurrentYear() {
        int year = DateTimeUtil.getCurrentYear();
        assertTrue(year >= 2024);
    }

    @Test
    void testGetMonth() {
        Date date = DateTimeUtil.parse(TEST_TIMESTAMP);
        int month = DateTimeUtil.getMonth(date);
        assertTrue(month >= 1 && month <= 12);
    }

    @Test
    void testGetCurrentMonth() {
        int month = DateTimeUtil.getCurrentMonth();
        assertTrue(month >= 1 && month <= 12);
    }

    @Test
    void testGetDay() {
        Date date = DateTimeUtil.parse(TEST_TIMESTAMP);
        int day = DateTimeUtil.getDay(date);
        assertTrue(day >= 1 && day <= 31);
    }

    @Test
    void testGetCurrentDay() {
        int day = DateTimeUtil.getCurrentDay();
        assertTrue(day >= 1 && day <= 31);
    }

    // ==================== getHour/getMinute/getSecond 方法测试 ====================

    @Test
    void testGetHour() {
        Date date = DateTimeUtil.parse(TEST_TIMESTAMP);
        int hour = DateTimeUtil.getHour(date);
        assertTrue(hour >= 0 && hour <= 23);
    }

    @Test
    void testGetCurrentHour() {
        int hour = DateTimeUtil.getCurrentHour();
        assertTrue(hour >= 0 && hour <= 23);
    }

    @Test
    void testGetMinute() {
        Date date = DateTimeUtil.parse(TEST_TIMESTAMP);
        int minute = DateTimeUtil.getMinute(date);
        assertTrue(minute >= 0 && minute <= 59);
    }

    @Test
    void testGetCurrentMinute() {
        int minute = DateTimeUtil.getCurrentMinute();
        assertTrue(minute >= 0 && minute <= 59);
    }

    @Test
    void testGetSecond() {
        Date date = DateTimeUtil.parse(TEST_TIMESTAMP);
        int second = DateTimeUtil.getSecond(date);
        assertTrue(second >= 0 && second <= 59);
    }

    @Test
    void testGetCurrentSecond() {
        int second = DateTimeUtil.getCurrentSecond();
        assertTrue(second >= 0 && second <= 59);
    }

    // ==================== getLocalDateTime 方法测试 ====================

    @Test
    void testGetLocalDateTime() {
        var result = DateTimeUtil.getLocalDateTime(TEST_TIMESTAMP);
        assertNotNull(result);
    }

    // ==================== add 方法测试 ====================

    @Test
    void testAddDays() {
        Date now = new Date();
        Date result = DateTimeUtil.addDays(now, 1);
        assertNotNull(result);
        assertTrue(result.after(now));
    }

    @Test
    void testAddDaysWithNegative() {
        Date now = new Date();
        Date result = DateTimeUtil.addDays(now, -1);
        assertNotNull(result);
        assertTrue(result.before(now));
    }

    @Test
    void testAddHours() {
        Date now = new Date();
        Date result = DateTimeUtil.addHours(now, 1);
        assertNotNull(result);
        assertTrue(result.after(now));
    }

    @Test
    void testAddMinutes() {
        Date now = new Date();
        Date result = DateTimeUtil.addMinutes(now, 1);
        assertNotNull(result);
        assertTrue(result.after(now));
    }

    @Test
    void testAddSeconds() {
        Date now = new Date();
        Date result = DateTimeUtil.addSeconds(now, 1);
        assertNotNull(result);
        assertTrue(result.after(now));
    }

    @Test
    void testAddMilliseconds() {
        Date now = new Date();
        Date result = DateTimeUtil.addMilliseconds(now, 100);
        assertNotNull(result);
        assertTrue(result.after(now));
    }

    @Test
    void testAddMonths() {
        Date now = new Date();
        Date result = DateTimeUtil.addMonths(now, 1);
        assertNotNull(result);
        assertTrue(result.after(now));
    }

    @Test
    void testAddWeeks() {
        Date now = new Date();
        Date result = DateTimeUtil.addWeeks(now, 1);
        assertNotNull(result);
        assertTrue(result.after(now));
    }

    @Test
    void testAddYears() {
        Date now = new Date();
        Date result = DateTimeUtil.addYears(now, 1);
        assertNotNull(result);
        assertTrue(result.after(now));
    }

    // ==================== getStartOf 方法测试 ====================

    @Test
    void testGetStartOfDay() {
        Date now = new Date();
        Date result = DateTimeUtil.getStartOfDay(now);
        assertNotNull(result);
        assertTrue(result.getTime() <= now.getTime());
    }

    @Test
    void testGetStartOfDayWithNoArgs() {
        Date result = DateTimeUtil.getStartOfDay();
        assertNotNull(result);
    }

    @Test
    void testGetStartOfMonth() {
        Date now = new Date();
        Date result = DateTimeUtil.getStartOfMonth(now);
        assertNotNull(result);
        assertTrue(result.getTime() <= now.getTime());
    }

    @Test
    void testGetStartOfMonthWithNoArgs() {
        Date result = DateTimeUtil.getStartOfMonth();
        assertNotNull(result);
    }

    @Test
    void testGetStartOfYear() {
        Date now = new Date();
        Date result = DateTimeUtil.getStartOfYear(now);
        assertNotNull(result);
        assertTrue(result.getTime() <= now.getTime());
    }

    @Test
    void testGetStartOfYearWithNoArgs() {
        Date result = DateTimeUtil.getStartOfYear();
        assertNotNull(result);
    }

    // ==================== 常量测试 ====================

    @Test
    void testConstants() {
        assertEquals(24, DateTimeUtil.HOUR_PER_DAY);
        assertEquals(1000, DateTimeUtil.MILLISECONDS_PER_SECOND);
        assertEquals(365, DateTimeUtil.DAY_PER_YEAR);
        assertEquals(30, DateTimeUtil.DAY_PER_MONTH);
        assertEquals(7, DateTimeUtil.DAY_PER_WEEK);
        assertEquals(60, DateTimeUtil.SECOND_PER_MINUTE);
        assertEquals(3600, DateTimeUtil.SECOND_PER_HOUR);
        assertEquals(86400, DateTimeUtil.SECOND_PER_DAY);
    }
}
