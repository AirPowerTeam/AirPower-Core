package cn.hamm.airpower.core;

import cn.hamm.airpower.core.enums.DateTimeFormatter;
import cn.hamm.airpower.core.exception.ServiceException;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;

import static cn.hamm.airpower.core.enums.DateTimeFormatter.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link DateTimeUtil} 单元测试。
 *
 * <p>为规避时区歧义，测试期间统一把 JVM 默认时区设置为 {@code Asia/Shanghai}，并在每个用例结束后还原。</p>
 */
@DisplayName("时间日期工具类测试")
class DateTimeUtilTest {
    /**
     * 测试期间统一使用的时区
     */
    private static final ZoneId TEST_ZONE = ZoneId.of("Asia/Shanghai");
    /**
     * 固定时间：2024-01-31 12:34:56.789（东八区）
     */
    private static final long FIXED_MILLI = milliOf(2024, 1, 31, 12, 34, 56, 789);
    /**
     * 进入测试前的 JVM 默认时区
     */
    private TimeZone originalTimeZone;

    /**
     * 构造毫秒时间戳（按东八区解释各字段）
     *
     * @param year   年
     * @param month  月
     * @param day    日
     * @param hour   时
     * @param minute 分
     * @param second 秒
     * @param milli  毫秒
     * @return 毫秒时间戳
     */
    private static long milliOf(int year, int month, int day, int hour, int minute, int second, int milli) {
        return LocalDateTime.of(year, month, day, hour, minute, second, milli * 1_000_000)
                .atZone(TEST_ZONE).toInstant().toEpochMilli();
    }

    /**
     * 构造毫秒时间戳（秒级精度）
     *
     * @param year   年
     * @param month  月
     * @param day    日
     * @param hour   时
     * @param minute 分
     * @param second 秒
     * @return 毫秒时间戳
     */
    private static long milliOf(int year, int month, int day, int hour, int minute, int second) {
        return milliOf(year, month, day, hour, minute, second, 0);
    }

    /**
     * 构造固定日期对象
     *
     * @param year   年
     * @param month  月
     * @param day    日
     * @param hour   时
     * @param minute 分
     * @param second 秒
     * @return 日期对象
     */
    private static Date dateOf(int year, int month, int day, int hour, int minute, int second) {
        return new Date(milliOf(year, month, day, hour, minute, second));
    }

    /**
     * 把日期对象转成本地时间，便于断言
     *
     * @param date 日期对象
     * @return 本地时间
     */
    private static LocalDateTime localOf(Date date) {
        return DateTimeUtil.getLocalDateTime(date.getTime());
    }

    /**
     * 取当前秒级时间戳
     *
     * @return 当前秒级时间戳
     */
    private static long currentSecond() {
        return System.currentTimeMillis() / 1000L;
    }

    @BeforeEach
    @DisplayName("固定 JVM 默认时区为东八区")
    void setUp() {
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone(TEST_ZONE));
    }

    @AfterEach
    @DisplayName("还原 JVM 默认时区")
    void tearDown() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Nested
    @DisplayName("常量")
    class ConstantsTest {
        @Test
        @DisplayName("各换算常量取值应符合定义")
        void constants() {
            assertEquals(24, DateTimeUtil.HOUR_PER_DAY, "一天应为 24 小时");
            assertEquals(1000, DateTimeUtil.MILLISECONDS_PER_SECOND, "一秒应为 1000 毫秒");
            assertEquals(365, DateTimeUtil.DAY_PER_YEAR, "一年应按 365 天计算");
            assertEquals(30, DateTimeUtil.DAY_PER_MONTH, "一个月应按 30 天计算");
            assertEquals(7, DateTimeUtil.DAY_PER_WEEK, "一周应为 7 天");
            assertEquals(60, DateTimeUtil.SECOND_PER_MINUTE, "一分钟应为 60 秒");
            assertEquals(3600, DateTimeUtil.SECOND_PER_HOUR, "一小时应为 3600 秒");
            assertEquals(86400, DateTimeUtil.SECOND_PER_DAY, "一天应为 86400 秒");
        }
    }

    @Nested
    @DisplayName("formatCurrent 格式化当前时间")
    class FormatCurrentTest {
        @Test
        @DisplayName("无参重载应输出 yyyy-MM-dd HH:mm:ss 格式")
        void formatCurrent() {
            String actual = DateTimeUtil.formatCurrent();
            assertTrue(actual.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"),
                    "无参重载应返回 yyyy-MM-dd HH:mm:ss 格式，实际为：" + actual);
        }

        @Test
        @DisplayName("枚举重载应与字符串重载结果一致")
        void formatCurrentWithEnum() {
            String before = FULL_DATETIME.format(System.currentTimeMillis());
            String actual = DateTimeUtil.formatCurrent(FULL_DATETIME);
            String after = FULL_DATETIME.format(System.currentTimeMillis());
            assertTrue(actual.equals(before) || actual.equals(after),
                    "枚举重载应与相同模板的字符串结果一致，实际为：" + actual);
        }

        @Test
        @DisplayName("字符串重载应按自定义模板输出")
        void formatCurrentWithPattern() {
            String actual = DateTimeUtil.formatCurrent("yyyy/MM/dd");
            assertTrue(actual.matches("\\d{4}/\\d{2}/\\d{2}"),
                    "字符串重载应返回 yyyy/MM/dd 格式，实际为：" + actual);
        }

        @Test
        @DisplayName("传 null 模板应抛出空指针异常")
        void formatCurrentWithNullPattern() {
            assertThrows(NullPointerException.class, () -> DateTimeUtil.formatCurrent((String) null),
                    "字符串模板为 null 时应抛出空指针异常");
        }

        @Test
        @DisplayName("传 null 枚举应抛出空指针异常")
        void formatCurrentWithNullEnum() {
            assertThrows(NullPointerException.class, () -> DateTimeUtil.formatCurrent((DateTimeFormatter) null),
                    "枚举模板为 null 时应抛出空指针异常");
        }
    }

    @Nested
    @DisplayName("format 格式化时间戳")
    class FormatTest {
        @Test
        @DisplayName("单参重载应使用默认的完整日期时间模板")
        void formatSingleArg() {
            assertEquals("2024-01-31 12:34:56", DateTimeUtil.format(FIXED_MILLI),
                    "单参重载应按 yyyy-MM-dd HH:mm:ss 格式化");
        }

        @Test
        @DisplayName("枚举重载应按枚举模板格式化")
        void formatWithEnum() {
            assertEquals("2024-01-31", DateTimeUtil.format(FIXED_MILLI, FULL_DATE),
                    "FULL_DATE 枚举应只输出年月日");
            assertEquals("12:34:56", DateTimeUtil.format(FIXED_MILLI, FULL_TIME),
                    "FULL_TIME 枚举应只输出时分秒");
            assertEquals("01-31 12:34", DateTimeUtil.format(FIXED_MILLI, SHORT_DATETIME),
                    "SHORT_DATETIME 枚举应输出月日时分");
        }

        @Test
        @DisplayName("字符串模板重载应支持自定义模板与毫秒")
        void formatWithPattern() {
            assertEquals("2024/01/31 12:34:56.789", DateTimeUtil.format(FIXED_MILLI, "yyyy/MM/dd HH:mm:ss.SSS"),
                    "自定义模板应正确输出毫秒");
            assertEquals("2024-01-31", DateTimeUtil.format(FIXED_MILLI, FULL_DATE.getValue()),
                    "字符串模板重载应与枚举重载结果一致");
        }

        @Test
        @DisplayName("时区重载应按指定时区换算")
        void formatWithZone() {
            assertEquals("2024-01-31 12:34:56", DateTimeUtil.format(FIXED_MILLI, "yyyy-MM-dd HH:mm:ss", TEST_ZONE),
                    "东八区应输出 12:34:56");
            assertEquals("2024-01-31 04:34:56", DateTimeUtil.format(FIXED_MILLI, "yyyy-MM-dd HH:mm:ss", ZoneId.of("UTC")),
                    "UTC 时区应输出 04:34:56");
            assertEquals("2024-01-30 23:34:56",
                    DateTimeUtil.format(FIXED_MILLI, "yyyy-MM-dd HH:mm:ss", ZoneId.of("America/New_York")),
                    "纽约时区（UTC-5）应输出前一天 23:34:56");
        }

        @Test
        @DisplayName("不同模板作用于同一时间戳应产生不同结果")
        void formatWithDifferentPattern() {
            String year = DateTimeUtil.format(FIXED_MILLI, "yyyy");
            String month = DateTimeUtil.format(FIXED_MILLI, "MM");
            assertEquals("2024", year, "年模板应输出 2024");
            assertEquals("01", month, "月模板应输出 01");
            assertTrue(!year.equals(month), "不同模板的结果不应相同");
        }

        @Test
        @DisplayName("时间戳 0 应按东八区输出 1970-01-01 08:00:00")
        void formatEpochZero() {
            assertEquals("1970-01-01 08:00:00", DateTimeUtil.format(0L),
                    "纪元零点按东八区应输出 1970-01-01 08:00:00");
        }

        @Test
        @DisplayName("负数时间戳应格式化到纪元之前")
        void formatNegative() {
            // 纪元前 1 毫秒为 UTC 1969-12-31T23:59:59.999Z，东八区 +8 小时后已跨到 1970-01-01
            assertEquals("1970-01-01 07:59:59", DateTimeUtil.format(-1L),
                    "纪元前 1 毫秒按东八区换算后应输出 1970-01-01 07:59:59");
            assertEquals("1969-12-31 23:59:59", DateTimeUtil.format(-1L, "yyyy-MM-dd HH:mm:ss", ZoneId.of("UTC")),
                    "纪元前 1 毫秒按 UTC 时区应输出 1969-12-31 23:59:59");
        }

        @Test
        @DisplayName("long 上限时间戳应输出带正号的 9 位年份")
        void formatMaxMilli() {
            // 年份位数超过 4 位时，DateTimeFormatter 会补上 "+" 号
            assertEquals("+292278994", DateTimeUtil.format(Long.MAX_VALUE, "yyyy"),
                    "long 上限时间戳的年份应输出 +292278994");
        }

        @Test
        @DisplayName("传 null 模板应抛出空指针异常")
        void formatWithNullPattern() {
            assertThrows(NullPointerException.class, () -> DateTimeUtil.format(FIXED_MILLI, (String) null),
                    "字符串模板为 null 时应抛出空指针异常");
        }

        @Test
        @DisplayName("传 null 枚举应抛出空指针异常")
        void formatWithNullEnum() {
            assertThrows(NullPointerException.class, () -> DateTimeUtil.format(FIXED_MILLI, (DateTimeFormatter) null),
                    "枚举模板为 null 时应抛出空指针异常");
        }

        @Test
        @DisplayName("非法模板应抛出非法参数异常")
        void formatWithIllegalPattern() {
            assertThrows(IllegalArgumentException.class, () -> DateTimeUtil.format(FIXED_MILLI, "yyyy-fff"),
                    "出现未知模式字母时应抛出非法参数异常");
        }

        @Test
        @DisplayName("模板缓存应保证相同模板重复调用结果一致")
        void formatCache() {
            String first = DateTimeUtil.format(FIXED_MILLI, "yyyy'年'MM'月'");
            String second = DateTimeUtil.format(FIXED_MILLI, "yyyy'年'MM'月'");
            assertEquals(first, second, "相同模板重复调用应返回相同结果");
            assertEquals("2024年01月", first, "带中文文字的模板应正确输出");
        }
    }

    @Nested
    @DisplayName("parse 解析时间")
    class ParseTest {
        @Test
        @DisplayName("长整型重载应返回对应的时间戳")
        void parseMilli() {
            Date actual = DateTimeUtil.parse(FIXED_MILLI);
            assertEquals(FIXED_MILLI, actual.getTime(), "长整型重载应原样返回时间戳");
        }

        @Test
        @DisplayName("字符串重载应按完整日期时间模板解析")
        void parseString() {
            Date actual = DateTimeUtil.parse("2024-01-31 12:34:56");
            assertEquals(milliOf(2024, 1, 31, 12, 34, 56), actual.getTime(),
                    "字符串重载应按东八区解析为对应时间戳");
        }

        @Test
        @DisplayName("自定义模板重载应正确解析")
        void parseWithPattern() {
            assertEquals(milliOf(2024, 1, 31, 12, 34, 56),
                    DateTimeUtil.parse("2024/01/31 12:34:56", "yyyy/MM/dd HH:mm:ss").getTime(),
                    "自定义分隔符模板应能正确解析");
            assertEquals(milliOf(2024, 1, 31, 12, 34, 56),
                    DateTimeUtil.parse("2024-01-31 12:34:56", FULL_DATETIME.getValue()).getTime(),
                    "显式传入完整模板应与默认重载一致");
        }

        @Test
        @DisplayName("只有日期没有时间的模板应抛出业务异常")
        void parseDateOnlyPattern() {
            // LocalDateTime.parse 要求模板能解析出完整时间，纯日期模板会被统一包装为业务异常
            assertThrows(ServiceException.class, () -> DateTimeUtil.parse("2024-01-31", "yyyy-MM-dd"),
                    "仅含日期的模板无法解析为 LocalDateTime，应抛出业务异常");
        }

        @Test
        @DisplayName("格式化与解析应可往返还原")
        void parseRoundTrip() {
            String text = DateTimeUtil.format(FIXED_MILLI, FULL_DATETIME);
            assertEquals(FIXED_MILLI / 1000 * 1000, DateTimeUtil.parse(text).getTime(),
                    "秒级文本往返后应保留到秒");
        }

        @Test
        @DisplayName("格式不匹配应抛出业务异常")
        void parseIllegalText() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> DateTimeUtil.parse("2024/01/31 12:34:56"),
                    "分隔符不匹配时应抛出业务异常");
            assertEquals("时间日期格式错误，请检查输入格式", exception.getMessage(),
                    "异常信息应提示检查输入格式");
        }

        @Test
        @DisplayName("空串应抛出业务异常")
        void parseEmptyString() {
            assertThrows(ServiceException.class, () -> DateTimeUtil.parse(""),
                    "空字符串应抛出业务异常");
        }

        @Test
        @DisplayName("纯数字字符串应抛出业务异常")
        void parseNumberText() {
            assertThrows(ServiceException.class, () -> DateTimeUtil.parse("20240131"),
                    "缺少分隔符的字符串应抛出业务异常");
        }

        @Test
        @DisplayName("传 null 应抛出业务异常")
        void parseNull() {
            assertThrows(ServiceException.class, () -> DateTimeUtil.parse(null),
                    "null 输入应被捕获并转换为业务异常");
            assertThrows(ServiceException.class, () -> DateTimeUtil.parse(null, "yyyy-MM-dd"),
                    "null 输入配合自定义模板也应抛出业务异常");
        }

        @Test
        @DisplayName("传 null 模板应抛出业务异常")
        void parseNullPattern() {
            assertThrows(ServiceException.class, () -> DateTimeUtil.parse("2024-01-31", null),
                    "模板为 null 时应抛出业务异常");
        }

        @Test
        @DisplayName("非法模板应抛出业务异常")
        void parseIllegalPattern() {
            assertThrows(ServiceException.class, () -> DateTimeUtil.parse("2024-01-31", "yyyy-fff"),
                    "模板非法时应被捕获并转换为业务异常");
        }
    }

    @Nested
    @DisplayName("friendlyFormat 友好时间")
    class FriendlyFormatTest {
        @Test
        @DisplayName("秒级重载遇负数应抛出业务异常")
        void friendlyFormatSecondNegative() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> DateTimeUtil.friendlyFormatSecond(-1L),
                    "秒级时间戳为负数时应抛出业务异常");
            assertEquals("时间戳不能为负数", exception.getMessage(), "异常信息应提示时间戳不能为负数");
        }

        @Test
        @DisplayName("毫秒级重载先做整数除法，绝对值超过 1 秒才会命中负数校验")
        void friendlyFormatMillisecondNegative() {
            assertThrows(ServiceException.class, () -> DateTimeUtil.friendlyFormatMillisecond(-1000L),
                    "-1000 毫秒整除为 -1 秒后应命中负数校验");
            // -1 毫秒整除为 0 秒，等价于纪元零点，不会触发负数校验，这是源码的既有行为
            assertTrue(DateTimeUtil.friendlyFormatMillisecond(-1L).matches("\\d+年前"),
                    "-1 毫秒被截断为 0 秒，应按纪元零点处理而不是抛异常");
        }

        @Test
        @DisplayName("过去不足一分钟应输出“刚刚”")
        void justNow() {
            assertEquals("刚刚", DateTimeUtil.friendlyFormatSecond(currentSecond() - 10L),
                    "过去 10 秒应输出“刚刚”");
            assertEquals("刚刚", DateTimeUtil.friendlyFormatMillisecond((currentSecond() - 30L) * 1000L),
                    "过去 30 秒应输出“刚刚”");
        }

        @Test
        @DisplayName("与当前秒完全相同时应输出秒级描述")
        void sameSecond() {
            String actual = DateTimeUtil.friendlyFormatSecond(currentSecond());
            assertTrue(actual.equals("刚刚") || actual.matches("\\d+秒前"),
                    "当前秒应输出“刚刚”或“n秒前”，实际为：" + actual);
        }

        @Test
        @DisplayName("过去超过一分钟的时间不会输出“秒前”")
        void secondsBeforeIsUnreachable() {
            // 步长表里 60 秒的档位先于 0 秒被匹配，因此过去的时间只要超过 60 秒就必然进入分钟档，
            // “n秒前”在源码中不可达，这里记录该行为
            String actual = DateTimeUtil.friendlyFormatSecond(currentSecond() - 100L);
            assertTrue(actual.matches("\\d+分钟前"), "过去 100 秒应输出 n 分钟前，实际为：" + actual);
            assertTrue(actual.endsWith("前"), "过去的时间应以“前”结尾，实际为：" + actual);
        }

        @Test
        @DisplayName("未来的时间应输出“秒后”")
        void secondsAfter() {
            String actual = DateTimeUtil.friendlyFormatSecond(currentSecond() + 10L);
            assertTrue(actual.matches("\\d+秒后"), "未来 10 秒应输出 n 秒后，实际为：" + actual);
            assertTrue(!actual.equals("刚刚"), "未来的时间不应输出“刚刚”，这是源码的既有行为");
        }

        @Test
        @DisplayName("分钟级步长")
        void minutes() {
            assertTrue(DateTimeUtil.friendlyFormatSecond(currentSecond() - 90L).matches("\\d+分钟前"),
                    "过去 90 秒应输出 n 分钟前");
            assertTrue(DateTimeUtil.friendlyFormatSecond(currentSecond() + 90L).matches("\\d+分钟后"),
                    "未来 90 秒应输出 n 分钟后");
            assertTrue(DateTimeUtil.friendlyFormatSecond(currentSecond() - 65L).matches("\\d+分钟前"),
                    "过去 65 秒应进入分钟步长");
        }

        @Test
        @DisplayName("小时级步长")
        void hours() {
            assertTrue(DateTimeUtil.friendlyFormatSecond(currentSecond() - 3600L * 2L).matches("\\d+小时前"),
                    "过去 2 小时应输出 n 小时前");
            assertTrue(DateTimeUtil.friendlyFormatSecond(currentSecond() + 3600L * 5L).matches("\\d+小时后"),
                    "未来 5 小时应输出 n 小时后");
        }

        @Test
        @DisplayName("天级步长")
        void days() {
            assertTrue(DateTimeUtil.friendlyFormatSecond(currentSecond() - 86400L * 2L).matches("\\d+天前"),
                    "过去 2 天应输出 n 天前");
            assertTrue(DateTimeUtil.friendlyFormatSecond(currentSecond() + 86400L * 3L).matches("\\d+天后"),
                    "未来 3 天应输出 n 天后");
        }

        @Test
        @DisplayName("周级步长")
        void weeks() {
            assertTrue(DateTimeUtil.friendlyFormatSecond(currentSecond() - 86400L * 7L * 2L).matches("\\d+周前"),
                    "过去 2 周应输出 n 周前");
            assertTrue(DateTimeUtil.friendlyFormatSecond(currentSecond() + 86400L * 7L * 3L).matches("\\d+周后"),
                    "未来 3 周应输出 n 周后");
        }

        @Test
        @DisplayName("月级步长")
        void months() {
            assertTrue(DateTimeUtil.friendlyFormatSecond(currentSecond() - 86400L * 30L * 2L).matches("\\d+月前"),
                    "过去 2 个月应输出 n 月前");
            assertTrue(DateTimeUtil.friendlyFormatSecond(currentSecond() + 86400L * 30L * 2L).matches("\\d+月后"),
                    "未来 2 个月应输出 n 月后");
        }

        @Test
        @DisplayName("年级步长")
        void years() {
            assertTrue(DateTimeUtil.friendlyFormatSecond(currentSecond() - 86400L * 365L * 3L).matches("\\d+年前"),
                    "过去 3 年应输出 n 年前");
            assertTrue(DateTimeUtil.friendlyFormatSecond(currentSecond() + 86400L * 365L * 2L).matches("\\d+年后"),
                    "未来 2 年应输出 n 年后");
        }

        @Test
        @DisplayName("月与年的分界应按 365 天计算")
        void monthYearBoundary() {
            assertTrue(DateTimeUtil.friendlyFormatSecond(currentSecond() + 86400L * 364L).matches("\\d+月后"),
                    "未来 364 天仍属于月级步长");
            assertTrue(DateTimeUtil.friendlyFormatSecond(currentSecond() + 86400L * 365L).matches("\\d+年后"),
                    "未来 365 天进入年级步长");
        }

        @Test
        @DisplayName("毫秒重载应做整数除法截断")
        void friendlyFormatMillisecondTruncate() {
            String actual = DateTimeUtil.friendlyFormatMillisecond((currentSecond() - 100L) * 1000L + 999L);
            assertTrue(actual.matches("\\d+分钟前"), "不足一秒的余数应被截断，实际为：" + actual);
        }

        @Test
        @DisplayName("纪元零点应输出很早以前的年")
        void epochZero() {
            assertTrue(DateTimeUtil.friendlyFormatMillisecond(0L).matches("\\d+年前"),
                    "纪元零点距今超过一年，应输出 n 年前");
        }
    }

    @Nested
    @DisplayName("取当前时间各字段")
    class CurrentPartTest {
        @Test
        @DisplayName("各 getCurrentXxx 应返回当前时间对应字段")
        void currentParts() {
            LocalDateTime before = LocalDateTime.now();
            int year = DateTimeUtil.getCurrentYear();
            int month = DateTimeUtil.getCurrentMonth();
            int day = DateTimeUtil.getCurrentDay();
            int hour = DateTimeUtil.getCurrentHour();
            int minute = DateTimeUtil.getCurrentMinute();
            int second = DateTimeUtil.getCurrentSecond();
            LocalDateTime after = LocalDateTime.now();

            assertTrue(year >= before.getYear() && year <= after.getYear(), "当前年份应落在执行期间范围内");
            assertTrue(month >= before.getMonthValue() && month <= after.getMonthValue(), "当前月份应落在执行期间范围内");
            assertTrue(day >= before.getDayOfMonth() && day <= after.getDayOfMonth(), "当前日期应落在执行期间范围内");
            assertTrue(hour >= before.getHour() && hour <= after.getHour(), "当前小时应落在执行期间范围内");
            assertTrue(minute >= before.getMinute() && minute <= after.getMinute(), "当前分钟应落在执行期间范围内");
            assertTrue(second >= before.getSecond() && second <= after.getSecond(), "当前秒应落在执行期间范围内");
        }
    }

    @Nested
    @DisplayName("getLocalDateTime 转换时间戳")
    class GetLocalDateTimeTest {
        @Test
        @DisplayName("应按系统默认时区转换")
        void normal() {
            LocalDateTime actual = DateTimeUtil.getLocalDateTime(FIXED_MILLI);
            assertEquals(LocalDateTime.of(2024, 1, 31, 12, 34, 56, 789_000_000), actual,
                    "应把时间戳转换成本地时间，保留毫秒");
        }

        @Test
        @DisplayName("时间戳 0 应转换为东八区 8 点整")
        void epochZero() {
            assertEquals(LocalDateTime.of(1970, 1, 1, 8, 0, 0), DateTimeUtil.getLocalDateTime(0L),
                    "纪元零点按东八区应为 1970-01-01T08:00");
        }

        @Test
        @DisplayName("负数时间戳应换算到东八区后仍落在纪元当天")
        void negative() {
            // UTC 纪元前 1 毫秒为 1969-12-31T23:59:59.999Z，东八区 +8 小时后为 1970-01-01T07:59:59.999
            assertEquals(LocalDateTime.of(1970, 1, 1, 7, 59, 59, 999_000_000), DateTimeUtil.getLocalDateTime(-1L),
                    "纪元前 1 毫秒按东八区应为 1970-01-01T07:59:59.999");
        }
    }

    @Nested
    @DisplayName("getYear/Month/Day/Hour/Minute/Second 取值")
    class PartTest {
        @Test
        @DisplayName("按固定日期取值")
        void parts() {
            Date date = dateOf(2024, 2, 29, 23, 59, 58);
            assertEquals(2024, DateTimeUtil.getYear(date), "年份应为 2024");
            assertEquals(2, DateTimeUtil.getMonth(date), "月份应为 2");
            assertEquals(29, DateTimeUtil.getDay(date), "日期应为 29");
            assertEquals(23, DateTimeUtil.getHour(date), "小时应为 23");
            assertEquals(59, DateTimeUtil.getMinute(date), "分钟应为 59");
            assertEquals(58, DateTimeUtil.getSecond(date), "秒应为 58");
        }

        @Test
        @DisplayName("跨年边界取值")
        void newYearParts() {
            Date date = dateOf(2025, 1, 1, 0, 0, 0);
            assertEquals(2025, DateTimeUtil.getYear(date), "跨年后年份应为 2025");
            assertEquals(1, DateTimeUtil.getMonth(date), "跨年后月份应为 1");
            assertEquals(1, DateTimeUtil.getDay(date), "跨年后日期应为 1");
            assertEquals(0, DateTimeUtil.getHour(date), "跨年后小时应为 0");
        }
    }

    @Nested
    @DisplayName("addDays 添加天")
    class AddDaysTest {
        @Test
        @DisplayName("数量为 0 时应保持不变")
        void zero() {
            Date base = dateOf(2024, 1, 31, 10, 20, 30);
            assertEquals(base.getTime(), DateTimeUtil.addDays(base, 0).getTime(), "数量为 0 时应返回相同时间");
        }

        @Test
        @DisplayName("跨月增加")
        void forward() {
            Date actual = DateTimeUtil.addDays(dateOf(2024, 1, 31, 10, 0, 0), 1);
            assertEquals(LocalDateTime.of(2024, 2, 1, 10, 0, 0), localOf(actual), "1 月 31 日加一天应为 2 月 1 日");
        }

        @Test
        @DisplayName("闰年 2 月 28 日加一天应为 2 月 29 日")
        void leapYear() {
            Date actual = DateTimeUtil.addDays(dateOf(2024, 2, 28, 8, 0, 0), 1);
            assertEquals(LocalDateTime.of(2024, 2, 29, 8, 0, 0), localOf(actual), "闰年 2 月 28 日加一天应为 2 月 29 日");
        }

        @Test
        @DisplayName("平年 2 月 28 日加一天应为 3 月 1 日")
        void commonYear() {
            Date actual = DateTimeUtil.addDays(dateOf(2023, 2, 28, 8, 0, 0), 1);
            assertEquals(LocalDateTime.of(2023, 3, 1, 8, 0, 0), localOf(actual), "平年 2 月 28 日加一天应为 3 月 1 日");
        }

        @Test
        @DisplayName("跨年增加")
        void acrossYear() {
            Date actual = DateTimeUtil.addDays(dateOf(2024, 12, 31, 23, 0, 0), 1);
            assertEquals(LocalDateTime.of(2025, 1, 1, 23, 0, 0), localOf(actual), "跨年 12 月 31 日加一天应为次年 1 月 1 日");
        }

        @Test
        @DisplayName("负数表示回退")
        void backward() {
            Date actual = DateTimeUtil.addDays(dateOf(2024, 3, 1, 0, 0, 0), -1);
            assertEquals(LocalDateTime.of(2024, 2, 29, 0, 0, 0), localOf(actual), "负数应回退到前一天");
        }

        @Test
        @DisplayName("当前时间重载")
        void current() {
            long before = System.currentTimeMillis();
            Date actual = DateTimeUtil.addDays(3);
            long after = System.currentTimeMillis();
            assertTrue(actual.getTime() >= before + 3 * 86_400_000L && actual.getTime() <= after + 3 * 86_400_000L,
                    "当前时间加 3 天应落在执行期间对应的区间内");
        }
    }

    @Nested
    @DisplayName("addHours 添加小时")
    class AddHoursTest {
        @Test
        @DisplayName("数量为 0 时应保持不变")
        void zero() {
            Date base = dateOf(2024, 1, 31, 10, 20, 30);
            assertEquals(base.getTime(), DateTimeUtil.addHours(base, 0).getTime(), "数量为 0 时应返回相同时间");
        }

        @Test
        @DisplayName("跨天增加")
        void forward() {
            Date actual = DateTimeUtil.addHours(dateOf(2024, 1, 31, 23, 30, 0), 1);
            assertEquals(LocalDateTime.of(2024, 2, 1, 0, 30, 0), localOf(actual), "23 点加一小时应跨到次日 0 点 30 分");
        }

        @Test
        @DisplayName("负数表示回退")
        void backward() {
            Date actual = DateTimeUtil.addHours(dateOf(2024, 1, 1, 1, 0, 0), -2);
            assertEquals(LocalDateTime.of(2023, 12, 31, 23, 0, 0), localOf(actual), "回退两小时应跨到上一年");
        }

        @Test
        @DisplayName("当前时间重载")
        void current() {
            long before = System.currentTimeMillis();
            Date actual = DateTimeUtil.addHours(2);
            long after = System.currentTimeMillis();
            assertTrue(actual.getTime() >= before + 2 * 3_600_000L && actual.getTime() <= after + 2 * 3_600_000L,
                    "当前时间加 2 小时应落在执行期间对应的区间内");
        }
    }

    @Nested
    @DisplayName("addMilliseconds 添加毫秒")
    class AddMillisecondsTest {
        @Test
        @DisplayName("数量为 0 时应保持不变")
        void zero() {
            Date base = dateOf(2024, 1, 31, 10, 20, 30);
            assertEquals(base.getTime(), DateTimeUtil.addMilliseconds(base, 0).getTime(), "数量为 0 时应返回相同时间");
        }

        @Test
        @DisplayName("加 1000 毫秒等于加一秒")
        void forward() {
            Date actual = DateTimeUtil.addMilliseconds(dateOf(2024, 1, 31, 23, 59, 59), 1000);
            assertEquals(LocalDateTime.of(2024, 2, 1, 0, 0, 0), localOf(actual), "加 1000 毫秒应跨到下一秒");
        }

        @Test
        @DisplayName("负数表示回退")
        void backward() {
            Date actual = DateTimeUtil.addMilliseconds(dateOf(2024, 1, 1, 0, 0, 0), -1);
            assertEquals(LocalDateTime.of(2023, 12, 31, 23, 59, 59, 999_000_000), localOf(actual),
                    "回退 1 毫秒应得到前一刻");
        }

        @Test
        @DisplayName("当前时间重载")
        void current() {
            long before = System.currentTimeMillis();
            Date actual = DateTimeUtil.addMilliseconds(500);
            long after = System.currentTimeMillis();
            assertTrue(actual.getTime() >= before + 500 && actual.getTime() <= after + 500,
                    "当前时间加 500 毫秒应落在执行期间对应的区间内");
        }
    }

    @Nested
    @DisplayName("addMinutes 添加分钟")
    class AddMinutesTest {
        @Test
        @DisplayName("数量为 0 时应保持不变")
        void zero() {
            Date base = dateOf(2024, 1, 31, 10, 20, 30);
            assertEquals(base.getTime(), DateTimeUtil.addMinutes(base, 0).getTime(), "数量为 0 时应返回相同时间");
        }

        @Test
        @DisplayName("跨小时增加")
        void forward() {
            Date actual = DateTimeUtil.addMinutes(dateOf(2024, 1, 31, 23, 50, 0), 20);
            assertEquals(LocalDateTime.of(2024, 2, 1, 0, 10, 0), localOf(actual), "23:50 加 20 分钟应跨到次日");
        }

        @Test
        @DisplayName("负数表示回退")
        void backward() {
            Date actual = DateTimeUtil.addMinutes(dateOf(2024, 1, 1, 0, 30, 0), -60);
            assertEquals(LocalDateTime.of(2023, 12, 31, 23, 30, 0), localOf(actual), "回退一小时应跨到上一年");
        }

        @Test
        @DisplayName("当前时间重载")
        void current() {
            long before = System.currentTimeMillis();
            Date actual = DateTimeUtil.addMinutes(30);
            long after = System.currentTimeMillis();
            assertTrue(actual.getTime() >= before + 30 * 60_000L && actual.getTime() <= after + 30 * 60_000L,
                    "当前时间加 30 分钟应落在执行期间对应的区间内");
        }
    }

    @Nested
    @DisplayName("addMonths 添加月")
    class AddMonthsTest {
        @Test
        @DisplayName("数量为 0 时应保持不变")
        void zero() {
            Date base = dateOf(2024, 1, 31, 10, 20, 30);
            assertEquals(base.getTime(), DateTimeUtil.addMonths(base, 0).getTime(), "数量为 0 时应返回相同时间");
        }

        @Test
        @DisplayName("1 月 31 日加一个月应落到 2 月末（闰年）")
        void leapYearMonthEnd() {
            Date actual = DateTimeUtil.addMonths(dateOf(2024, 1, 31, 10, 0, 0), 1);
            assertEquals(LocalDateTime.of(2024, 2, 29, 10, 0, 0), localOf(actual), "闰年 1 月 31 日加一个月应为 2 月 29 日");
        }

        @Test
        @DisplayName("1 月 31 日加一个月应落到 2 月末（平年）")
        void commonYearMonthEnd() {
            Date actual = DateTimeUtil.addMonths(dateOf(2023, 1, 31, 10, 0, 0), 1);
            assertEquals(LocalDateTime.of(2023, 2, 28, 10, 0, 0), localOf(actual), "平年 1 月 31 日加一个月应为 2 月 28 日");
        }

        @Test
        @DisplayName("跨年增加")
        void acrossYear() {
            Date actual = DateTimeUtil.addMonths(dateOf(2024, 11, 15, 0, 0, 0), 3);
            assertEquals(LocalDateTime.of(2025, 2, 15, 0, 0, 0), localOf(actual), "11 月加 3 个月应到次年 2 月");
        }

        @Test
        @DisplayName("负数表示回退")
        void backward() {
            Date actual = DateTimeUtil.addMonths(dateOf(2024, 3, 31, 0, 0, 0), -1);
            assertEquals(LocalDateTime.of(2024, 2, 29, 0, 0, 0), localOf(actual), "回退一个月应落到 2 月末");
        }

        @Test
        @DisplayName("当前时间重载")
        void current() {
            long before = System.currentTimeMillis();
            Date actual = DateTimeUtil.addMonths(1);
            long after = System.currentTimeMillis();
            assertNotNull(actual, "当前时间加一个月应返回非空日期");
            assertTrue(actual.getTime() >= before + 28L * 86_400_000L
                            && actual.getTime() <= after + 31L * 86_400_000L,
                    "当前时间加一个月应落在 28~31 天之后的区间内");
            assertEquals(1, (localOf(actual).getMonthValue() - localOf(new Date(before)).getMonthValue() + 12) % 12,
                    "月份应向后推进一个月");
        }
    }

    @Nested
    @DisplayName("addSeconds 添加秒")
    class AddSecondsTest {
        @Test
        @DisplayName("数量为 0 时应保持不变")
        void zero() {
            Date base = dateOf(2024, 1, 31, 10, 20, 30);
            assertEquals(base.getTime(), DateTimeUtil.addSeconds(base, 0).getTime(), "数量为 0 时应返回相同时间");
        }

        @Test
        @DisplayName("加 86400 秒等于加一天")
        void forward() {
            Date actual = DateTimeUtil.addSeconds(dateOf(2024, 2, 28, 12, 0, 0), 86400);
            assertEquals(LocalDateTime.of(2024, 2, 29, 12, 0, 0), localOf(actual), "闰年加一天应为 2 月 29 日");
        }

        @Test
        @DisplayName("负数表示回退")
        void backward() {
            Date actual = DateTimeUtil.addSeconds(dateOf(2024, 1, 1, 0, 0, 0), -1);
            assertEquals(LocalDateTime.of(2023, 12, 31, 23, 59, 59), localOf(actual), "回退一秒应跨到上一年");
        }

        @Test
        @DisplayName("当前时间重载")
        void current() {
            long before = System.currentTimeMillis();
            Date actual = DateTimeUtil.addSeconds(10);
            long after = System.currentTimeMillis();
            assertTrue(actual.getTime() >= before + 10_000L && actual.getTime() <= after + 10_000L,
                    "当前时间加 10 秒应落在执行期间对应的区间内");
        }
    }

    @Nested
    @DisplayName("addWeeks 添加周")
    class AddWeeksTest {
        @Test
        @DisplayName("数量为 0 时应保持不变")
        void zero() {
            Date base = dateOf(2024, 1, 31, 10, 20, 30);
            assertEquals(base.getTime(), DateTimeUtil.addWeeks(base, 0).getTime(), "数量为 0 时应返回相同时间");
        }

        @Test
        @DisplayName("加一周等于加七天")
        void forward() {
            Date actual = DateTimeUtil.addWeeks(dateOf(2024, 12, 25, 6, 0, 0), 1);
            assertEquals(LocalDateTime.of(2025, 1, 1, 6, 0, 0), localOf(actual), "12 月 25 日加一周应跨年到 1 月 1 日");
        }

        @Test
        @DisplayName("负数表示回退")
        void backward() {
            Date actual = DateTimeUtil.addWeeks(dateOf(2024, 1, 1, 0, 0, 0), -1);
            assertEquals(LocalDateTime.of(2023, 12, 25, 0, 0, 0), localOf(actual), "回退一周应为上一年 12 月 25 日");
        }

        @Test
        @DisplayName("当前时间重载")
        void current() {
            long before = System.currentTimeMillis();
            Date actual = DateTimeUtil.addWeeks(2);
            long after = System.currentTimeMillis();
            assertTrue(actual.getTime() >= before + 14 * 86_400_000L && actual.getTime() <= after + 14 * 86_400_000L,
                    "当前时间加两周应落在执行期间对应的区间内");
        }
    }

    @Nested
    @DisplayName("addYears 添加年")
    class AddYearsTest {
        @Test
        @DisplayName("数量为 0 时应保持不变")
        void zero() {
            Date base = dateOf(2024, 1, 31, 10, 20, 30);
            assertEquals(base.getTime(), DateTimeUtil.addYears(base, 0).getTime(), "数量为 0 时应返回相同时间");
        }

        @Test
        @DisplayName("闰日加一年应落到 2 月 28 日")
        void leapDay() {
            Date actual = DateTimeUtil.addYears(dateOf(2024, 2, 29, 10, 0, 0), 1);
            assertEquals(LocalDateTime.of(2025, 2, 28, 10, 0, 0), localOf(actual), "2024-02-29 加一年应为 2025-02-28");
        }

        @Test
        @DisplayName("平年加闰年应落到 2 月 29 日")
        void commonToLeap() {
            Date actual = DateTimeUtil.addYears(dateOf(2023, 2, 28, 10, 0, 0), 1);
            assertEquals(LocalDateTime.of(2024, 2, 28, 10, 0, 0), localOf(actual), "2023-02-28 加一年应为 2024-02-28");
        }

        @Test
        @DisplayName("负数表示回退")
        void backward() {
            Date actual = DateTimeUtil.addYears(dateOf(2024, 6, 1, 0, 0, 0), -2);
            assertEquals(LocalDateTime.of(2022, 6, 1, 0, 0, 0), localOf(actual), "回退两年应得到 2022 年");
        }

        @Test
        @DisplayName("当前时间重载")
        void current() {
            long before = System.currentTimeMillis();
            Date actual = DateTimeUtil.addYears(1);
            long after = System.currentTimeMillis();
            assertTrue(actual.getTime() >= before + 300L * 86_400_000L
                            && actual.getTime() <= after + 400L * 86_400_000L,
                    "当前时间加一年应落在 300~400 天之后的区间内");
            assertEquals(1, localOf(actual).getYear() - localOf(new Date(before)).getYear(),
                    "年份应增加一年");
        }
    }

    @Nested
    @DisplayName("getStartOfDay 当天开始")
    class StartOfDayTest {
        @Test
        @DisplayName("指定日期应把时分秒毫秒清零")
        void withDate() {
            LocalDateTime actual = localOf(DateTimeUtil.getStartOfDay(dateOf(2024, 2, 29, 13, 45, 30)));
            assertEquals(LocalDateTime.of(2024, 2, 29, 0, 0, 0), actual, "当天开始应为零点整");
        }

        @Test
        @DisplayName("无参重载应返回今天的零点")
        void withoutDate() {
            LocalDateTime actual = localOf(DateTimeUtil.getStartOfDay());
            LocalDateTime today = localOf(new Date());
            assertEquals(LocalDateTime.of(today.toLocalDate(), java.time.LocalTime.MIDNIGHT), actual,
                    "无参重载应返回今天零点");
        }
    }

    @Nested
    @DisplayName("getStartOfMonth 当月开始")
    class StartOfMonthTest {
        @Test
        @DisplayName("指定日期应回到当月 1 日零点")
        void withDate() {
            LocalDateTime actual = localOf(DateTimeUtil.getStartOfMonth(dateOf(2024, 2, 29, 13, 45, 30)));
            assertEquals(LocalDateTime.of(2024, 2, 1, 0, 0, 0), actual, "当月开始应为 2 月 1 日零点");
        }

        @Test
        @DisplayName("月末日期应同样回到 1 日零点")
        void monthEnd() {
            LocalDateTime actual = localOf(DateTimeUtil.getStartOfMonth(dateOf(2023, 1, 31, 23, 59, 59)));
            assertEquals(LocalDateTime.of(2023, 1, 1, 0, 0, 0), actual, "1 月 31 日应回到 1 月 1 日零点");
        }

        @Test
        @DisplayName("无参重载应返回本月的 1 日零点")
        void withoutDate() {
            LocalDateTime actual = localOf(DateTimeUtil.getStartOfMonth());
            LocalDateTime today = localOf(new Date());
            assertEquals(LocalDateTime.of(today.getYear(), today.getMonthValue(), 1, 0, 0, 0), actual,
                    "无参重载应返回本月 1 日零点");
        }
    }

    @Nested
    @DisplayName("getStartOfYear 当年开始")
    class StartOfYearTest {
        @Test
        @DisplayName("指定日期应回到当年 1 月 1 日零点")
        void withDate() {
            LocalDateTime actual = localOf(DateTimeUtil.getStartOfYear(dateOf(2024, 6, 15, 8, 30, 0)));
            assertEquals(LocalDateTime.of(2024, 1, 1, 0, 0, 0), actual, "当年开始应为 1 月 1 日零点");
        }

        @Test
        @DisplayName("无参重载应返回今年的 1 月 1 日零点")
        void withoutDate() {
            LocalDateTime actual = localOf(DateTimeUtil.getStartOfYear());
            LocalDateTime today = localOf(new Date());
            assertEquals(LocalDateTime.of(today.getYear(), 1, 1, 0, 0, 0), actual,
                    "无参重载应返回今年 1 月 1 日零点");
        }
    }
}
