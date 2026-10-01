package cn.hamm.airpower.core.enums;

import cn.hamm.airpower.core.DateTimeUtil;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link DateTimeFormatter} 单元测试。
 *
 * <p>该枚举依赖 {@link DateTimeUtil} 的系统默认时区，测试期间统一固定为 {@code Asia/Shanghai}。</p>
 */
@DisplayName("格式化模板枚举测试")
class DateTimeFormatterTest {
    /**
     * 测试期间统一使用的时区
     */
    private static final ZoneId TEST_ZONE = ZoneId.of("Asia/Shanghai");

    /**
     * 固定时间：2024-01-31 12:34:56.789（东八区）
     */
    private static final long FIXED_MILLI = LocalDateTime.of(2024, 1, 31, 12, 34, 56, 789_000_000)
            .atZone(TEST_ZONE).toInstant().toEpochMilli();

    /**
     * 进入测试前的 JVM 默认时区
     */
    private TimeZone originalTimeZone;

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
    @DisplayName("枚举定义")
    class DefinitionTest {
        @Test
        @DisplayName("应恰好包含 10 个模板常量")
        void valuesLength() {
            assertEquals(10, DateTimeFormatter.values().length, "模板枚举应恰好包含 10 个常量");
        }

        @Test
        @DisplayName("按序号应能取到全部常量")
        void ordinals() {
            assertSame(DateTimeFormatter.YEAR, DateTimeFormatter.values()[0], "序号 0 应为 YEAR");
            assertSame(DateTimeFormatter.MONTH, DateTimeFormatter.values()[1], "序号 1 应为 MONTH");
            assertSame(DateTimeFormatter.DAY, DateTimeFormatter.values()[2], "序号 2 应为 DAY");
            assertSame(DateTimeFormatter.HOUR, DateTimeFormatter.values()[3], "序号 3 应为 HOUR");
            assertSame(DateTimeFormatter.MINUTE, DateTimeFormatter.values()[4], "序号 4 应为 MINUTE");
            assertSame(DateTimeFormatter.SECOND, DateTimeFormatter.values()[5], "序号 5 应为 SECOND");
            assertSame(DateTimeFormatter.FULL_DATE, DateTimeFormatter.values()[6], "序号 6 应为 FULL_DATE");
            assertSame(DateTimeFormatter.FULL_TIME, DateTimeFormatter.values()[7], "序号 7 应为 FULL_TIME");
            assertSame(DateTimeFormatter.FULL_DATETIME, DateTimeFormatter.values()[8], "序号 8 应为 FULL_DATETIME");
            assertSame(DateTimeFormatter.SHORT_DATETIME, DateTimeFormatter.values()[9], "序号 9 应为 SHORT_DATETIME");
        }

        @Test
        @DisplayName("每个模板的取值都不应为 null 或空串")
        void valueNotEmpty() {
            for (DateTimeFormatter formatter : DateTimeFormatter.values()) {
                assertNotNull(formatter.getValue(), "模板 " + formatter.name() + " 的取值不应为 null");
                assertTrue(!formatter.getValue().isEmpty(), "模板 " + formatter.name() + " 的取值不应为空串");
            }
        }
    }

    @Nested
    @DisplayName("getValue 取模板字符串")
    class GetValueTest {
        @Test
        @DisplayName("各模板字符串应与定义一致")
        void values() {
            assertEquals("yyyy", DateTimeFormatter.YEAR.getValue(), "YEAR 模板应为 yyyy");
            assertEquals("MM", DateTimeFormatter.MONTH.getValue(), "MONTH 模板应为 MM");
            assertEquals("dd", DateTimeFormatter.DAY.getValue(), "DAY 模板应为 dd");
            assertEquals("HH", DateTimeFormatter.HOUR.getValue(), "HOUR 模板应为 HH");
            assertEquals("mm", DateTimeFormatter.MINUTE.getValue(), "MINUTE 模板应为 mm");
            assertEquals("ss", DateTimeFormatter.SECOND.getValue(), "SECOND 模板应为 ss");
            assertEquals("yyyy-MM-dd", DateTimeFormatter.FULL_DATE.getValue(), "FULL_DATE 模板应为 yyyy-MM-dd");
            assertEquals("HH:mm:ss", DateTimeFormatter.FULL_TIME.getValue(), "FULL_TIME 模板应为 HH:mm:ss");
            assertEquals("yyyy-MM-dd HH:mm:ss", DateTimeFormatter.FULL_DATETIME.getValue(),
                    "FULL_DATETIME 模板应为 yyyy-MM-dd HH:mm:ss");
            assertEquals("MM-dd HH:mm", DateTimeFormatter.SHORT_DATETIME.getValue(),
                    "SHORT_DATETIME 模板应为 MM-dd HH:mm");
        }
    }

    @Nested
    @DisplayName("format 格式化时间戳")
    class FormatTest {
        @Test
        @DisplayName("年模板")
        void year() {
            assertEquals("2024", DateTimeFormatter.YEAR.format(FIXED_MILLI), "YEAR 应输出 2024");
        }

        @Test
        @DisplayName("月模板")
        void month() {
            assertEquals("01", DateTimeFormatter.MONTH.format(FIXED_MILLI), "MONTH 应输出 01");
        }

        @Test
        @DisplayName("日模板")
        void day() {
            assertEquals("31", DateTimeFormatter.DAY.format(FIXED_MILLI), "DAY 应输出 31");
        }

        @Test
        @DisplayName("时模板")
        void hour() {
            assertEquals("12", DateTimeFormatter.HOUR.format(FIXED_MILLI), "HOUR 应输出 12");
        }

        @Test
        @DisplayName("分模板")
        void minute() {
            assertEquals("34", DateTimeFormatter.MINUTE.format(FIXED_MILLI), "MINUTE 应输出 34");
        }

        @Test
        @DisplayName("秒模板")
        void second() {
            assertEquals("56", DateTimeFormatter.SECOND.format(FIXED_MILLI), "SECOND 应输出 56");
        }

        @Test
        @DisplayName("年月日模板")
        void fullDate() {
            assertEquals("2024-01-31", DateTimeFormatter.FULL_DATE.format(FIXED_MILLI), "FULL_DATE 应输出 2024-01-31");
        }

        @Test
        @DisplayName("时分秒模板")
        void fullTime() {
            assertEquals("12:34:56", DateTimeFormatter.FULL_TIME.format(FIXED_MILLI), "FULL_TIME 应输出 12:34:56");
        }

        @Test
        @DisplayName("年月日时分秒模板")
        void fullDateTime() {
            assertEquals("2024-01-31 12:34:56", DateTimeFormatter.FULL_DATETIME.format(FIXED_MILLI),
                    "FULL_DATETIME 应输出 2024-01-31 12:34:56");
        }

        @Test
        @DisplayName("月日时分模板")
        void shortDateTime() {
            assertEquals("01-31 12:34", DateTimeFormatter.SHORT_DATETIME.format(FIXED_MILLI),
                    "SHORT_DATETIME 应输出 01-31 12:34");
        }

        @Test
        @DisplayName("时间戳 0 应按东八区输出")
        void epochZero() {
            assertEquals("1970-01-01 08:00:00", DateTimeFormatter.FULL_DATETIME.format(0L),
                    "纪元零点按东八区应输出 1970-01-01 08:00:00");
        }

        @Test
        @DisplayName("闰年 2 月 29 日应正常格式化")
        void leapDay() {
            long milli = LocalDateTime.of(2024, 2, 29, 23, 59, 59).atZone(TEST_ZONE).toInstant().toEpochMilli();
            assertEquals("2024-02-29 23:59:59", DateTimeFormatter.FULL_DATETIME.format(milli),
                    "闰日应完整格式化");
        }

        @Test
        @DisplayName("结果应与 DateTimeUtil 的同名重载一致")
        void sameAsDateTimeUtil() {
            assertEquals(DateTimeUtil.format(FIXED_MILLI, DateTimeFormatter.FULL_DATE),
                    DateTimeFormatter.FULL_DATE.format(FIXED_MILLI),
                    "枚举的 format 应与 DateTimeUtil 的枚举重载结果一致");
        }

        @Test
        @DisplayName("完整日期时间应可被反解析回原时间戳")
        void roundTrip() {
            String text = DateTimeFormatter.FULL_DATETIME.format(FIXED_MILLI);
            Date parsed = DateTimeUtil.parse(text, DateTimeFormatter.FULL_DATETIME.getValue());
            assertEquals(FIXED_MILLI / 1000 * 1000, parsed.getTime(), "秒级文本往返后应还原到同一秒");
        }
    }

    @Nested
    @DisplayName("formatCurrent 格式化当前时间")
    class FormatCurrentTest {
        @Test
        @DisplayName("各模板的当前时间应落在调用前后区间内")
        void allTemplates() {
            for (DateTimeFormatter formatter : DateTimeFormatter.values()) {
                String before = formatter.format(System.currentTimeMillis());
                String actual = formatter.formatCurrent();
                String after = formatter.format(System.currentTimeMillis());
                assertTrue(actual.equals(before) || actual.equals(after),
                        "模板 " + formatter.name() + " 的当前时间应与调用前后的结果之一一致，实际为：" + actual);
            }
        }

        @Test
        @DisplayName("完整日期时间应符合 yyyy-MM-dd HH:mm:ss 格式")
        void fullDateTimePattern() {
            assertTrue(DateTimeFormatter.FULL_DATETIME.formatCurrent()
                            .matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"),
                    "FULL_DATETIME 当前时间应符合 yyyy-MM-dd HH:mm:ss 格式");
        }

        @Test
        @DisplayName("年月日应符合 yyyy-MM-dd 格式")
        void fullDatePattern() {
            assertTrue(DateTimeFormatter.FULL_DATE.formatCurrent().matches("\\d{4}-\\d{2}-\\d{2}"),
                    "FULL_DATE 当前时间应符合 yyyy-MM-dd 格式");
        }

        @Test
        @DisplayName("时分秒应符合 HH:mm:ss 格式")
        void fullTimePattern() {
            assertTrue(DateTimeFormatter.FULL_TIME.formatCurrent().matches("\\d{2}:\\d{2}:\\d{2}"),
                    "FULL_TIME 当前时间应符合 HH:mm:ss 格式");
        }

        @Test
        @DisplayName("月日时分应符合 MM-dd HH:mm 格式")
        void shortDateTimePattern() {
            assertTrue(DateTimeFormatter.SHORT_DATETIME.formatCurrent().matches("\\d{2}-\\d{2} \\d{2}:\\d{2}"),
                    "SHORT_DATETIME 当前时间应符合 MM-dd HH:mm 格式");
        }

        @Test
        @DisplayName("年月日时分秒模板应保持零填充")
        void zeroPadding() {
            assertTrue(DateTimeFormatter.MONTH.formatCurrent().matches("\\d{2}"), "月份应始终两位");
            assertTrue(DateTimeFormatter.DAY.formatCurrent().matches("\\d{2}"), "日期应始终两位");
            assertTrue(DateTimeFormatter.HOUR.formatCurrent().matches("\\d{2}"), "小时应始终两位");
            assertTrue(DateTimeFormatter.MINUTE.formatCurrent().matches("\\d{2}"), "分钟应始终两位");
            assertTrue(DateTimeFormatter.SECOND.formatCurrent().matches("\\d{2}"), "秒应始终两位");
        }
    }
}
