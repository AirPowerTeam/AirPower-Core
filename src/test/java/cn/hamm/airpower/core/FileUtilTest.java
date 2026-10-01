package cn.hamm.airpower.core;

import cn.hamm.airpower.core.enums.DateTimeFormatter;
import cn.hamm.airpower.core.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link FileUtil} 单元测试。
 *
 * <p>所有文件读写均在 {@link TempDir} 提供的临时目录内进行，测试结束后由框架自动清理。</p>
 *
 * <p>注意：{@code formatSize} 已固定使用 {@code Locale.ROOT} 格式化小数，
 * 因此在任意语言环境下都应输出 {@code 1.00B} 这类英文小数点格式。</p>
 */
@DisplayName("文件工具类测试")
class FileUtilTest {
    /**
     * 测试用临时目录
     */
    @TempDir
    Path tempDir;

    /**
     * ZIP 累计大小上限的测试句柄
     * <p>上限本身是 10GB，直接造 10GB 的目录不现实，
     * 因此测试期间允许临时下调，结束后必须恢复</p>
     */
    private static final ThreadLocal<Long> zipTotalBytesLimit = FileUtil.ZIP_TOTAL_LIMIT;

    /**
     * 读取文件全部字节
     *
     * @param path 文件路径
     * @return 文件字节
     * @throws IOException 读取异常
     */
    private static byte[] readBytes(Path path) throws IOException {
        return Files.readAllBytes(path);
    }

    @Nested
    @DisplayName("常量")
    class ConstantsTest {
        @Test
        @DisplayName("各常量取值应符合定义")
        void constants() {
            assertEquals(1024L, FileUtil.FILE_SCALE, "文件大小进制应为 1024");
            assertEquals(".", FileUtil.EXTENSION_SEPARATOR, "后缀分隔符应为英文句点");
            assertEquals(9, FileUtil.UNITS.size(), "文件单位应有 9 个");
            assertEquals("B", FileUtil.UNITS.get(0), "第 1 个单位应为 B");
            assertEquals("KB", FileUtil.UNITS.get(1), "第 2 个单位应为 KB");
            assertEquals("MB", FileUtil.UNITS.get(2), "第 3 个单位应为 MB");
            assertEquals("GB", FileUtil.UNITS.get(3), "第 4 个单位应为 GB");
            assertEquals("TB", FileUtil.UNITS.get(4), "第 5 个单位应为 TB");
            assertEquals("PB", FileUtil.UNITS.get(5), "第 6 个单位应为 PB");
            assertEquals("EB", FileUtil.UNITS.get(6), "第 7 个单位应为 EB");
            assertEquals("ZB", FileUtil.UNITS.get(7), "第 8 个单位应为 ZB");
            assertEquals("YB", FileUtil.UNITS.get(8), "第 9 个单位应为 YB");
            // 00128：UNITS 曾是 public 可变数组，final 只保证引用不可重新赋值，
            // 任何调用方都能 UNITS[3] = "G" 改掉全进程的单位表且无任何告警
            assertThrows(UnsupportedOperationException.class,
                    () -> FileUtil.UNITS.set(3, "G"),
                    "UNITS 必须是不可变的：篡改它会导致所有 formatSize() 的单位集体错乱，"
                            + "而且只在特定量级的文件上出现，极难归因");
            assertEquals("GB", FileUtil.UNITS.get(3), "篡改尝试不应影响实际内容");
        }
    }

    @Nested
    @DisplayName("getExtension 获取后缀")
    class GetExtensionTest {
        @Test
        @DisplayName("带后缀的文件名应取到小写后缀")
        void normal() {
            assertEquals("txt", FileUtil.getExtension("a.TXT"), "大写后缀应被转成小写");
            assertEquals("md", FileUtil.getExtension("readme.md"), "小写后缀应原样返回");
        }

        @Test
        @DisplayName("多段后缀应取最后一段")
        void multipleDots() {
            assertEquals("c", FileUtil.getExtension("a.b.c"), "多点文件名应取最后一段");
            assertEquals("gz", FileUtil.getExtension("backup.tar.gz"), "压缩包名应取最后一段");
        }

        @Test
        @DisplayName("无后缀的文件名返回空串")
        void noExtension() {
            // 原实现无句点时会把整个文件名当成扩展名返回（"noext"）
            assertEquals("", FileUtil.getExtension("noExt"), "无句点时应返回空串而不是整个文件名");
            assertEquals("", FileUtil.getExtension("README"), "无后缀的文件名不应返回自身");
        }

        @Test
        @DisplayName("点开头的隐藏文件应视为无扩展名")
        void hiddenFile() {
            // git / nginx / apache 都把 .gitignore / .env / .htaccess 当作没有扩展名
            for (String hidden : List.of(".gitignore", ".env", ".htaccess")) {
                assertEquals("", FileUtil.getExtension(hidden),
                        "点开头的隐藏文件按惯例没有扩展名：" + hidden);
            }
            assertEquals("txt", FileUtil.getExtension(".gitignore.txt"),
                    "点开头但后面还有扩展名时，仍应正确取到最后一个扩展名");
        }

        @Test
        @DisplayName("末尾为点时应返回空串")
        void trailingDot() {
            assertEquals("", FileUtil.getExtension("a."), "以点结尾应返回空后缀");
        }

        @Test
        @DisplayName("短文件名也能正确取到后缀")
        void shortFileName() {
            assertEquals("txt", FileUtil.getExtension("x.txt"), "短文件名应正确取到后缀");
            assertEquals("", FileUtil.getExtension("x"), "无后缀的单字符文件名应返回空串");
        }

        @Test
        @DisplayName("传 null 应抛出 ServiceException")
        void nullFileName() {
            assertThrows(ServiceException.class, () -> FileUtil.getExtension(null),
                    "文件名为 null 时应抛出 ServiceException");
        }
    }

    @Nested
    @DisplayName("formatSize 格式化文件大小")
    class FormatSizeTest {

        @Test
        @DisplayName("小数点固定为英文句点，不受 JVM 默认 Locale 影响")
        void localeIndependent() {
            // 源码已固定使用 Locale.ROOT，德语等环境下也应输出 1.00B 而不是 1,00B
            assertEquals("1.00B", FileUtil.formatSize(1L), "文件大小固定使用英文小数点，不应随 Locale 变化");
        }

        @Test
        @DisplayName("小于 1KB 时按字节输出")
        void bytes() {
            assertEquals("1.00B", FileUtil.formatSize(1L), "1 字节应输出 1.00B");
            assertEquals("512.00B", FileUtil.formatSize(512L), "512 字节应输出 512.00B");
            // 1023 字节仍然小于 1024，因此结果不带小数进位
            assertEquals("1023.00B", FileUtil.formatSize(1023L), "1023 字节应输出 1023.00B");
        }

        @Test
        @DisplayName("整除边界应升级单位")
        void scaleUp() {
            assertEquals("1.00KB", FileUtil.formatSize(1024L), "1024 字节应输出 1.00KB");
            assertEquals("1.00MB", FileUtil.formatSize(1048576L), "1MB 应输出 1.00MB");
            assertEquals("1.00GB", FileUtil.formatSize(1073741824L), "1GB 应输出 1.00GB");
            assertEquals("1.00TB", FileUtil.formatSize(1099511627776L), "1TB 应输出 1.00TB");
            assertEquals("1.00PB", FileUtil.formatSize(1125899906842624L), "1PB 应输出 1.00PB");
        }

        @Test
        @DisplayName("非整除时应保留两位小数")
        void decimal() {
            assertEquals("1.50KB", FileUtil.formatSize(1536L), "1536 字节应输出 1.50KB");
            assertEquals("1.50GB", FileUtil.formatSize(1073741824L + 536870912L),
                    "1.5GB 应按两位小数输出 1.50GB");
        }

        @Test
        @DisplayName("刚好低于升级线时四舍五入后可能出现与单位同值的显示")
        void justBelowScale() {
            // 1048575 / 1024 = 1023.999…，按两位小数四舍五入后显示为 1024.00KB
            assertEquals("1024.00KB", FileUtil.formatSize(1024L * 1024L - 1L),
                    "小于 1MB 的最大值会因四舍五入显示为 1024.00KB");
        }

        @Test
        @DisplayName("size 为 0 时返回 0.00B（0 字节是合法的空文件）")
        void zero() {
            assertEquals("0.00B", FileUtil.formatSize(0L), "0 字节的合法空文件不应被拒绝");
        }

        @Test
        @DisplayName("size 为负数应抛出业务异常")
        void negative() {
            ServiceException exception = assertThrows(ServiceException.class, () -> FileUtil.formatSize(-1L),
                    "文件大小为负数时应抛出业务异常");
            assertEquals("错误的文件大小: -1", exception.getMessage(), "异常信息应带上具体大小");
        }

        @Test
        @DisplayName("size 为 long 下限应抛出业务异常")
        void longMinValue() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> FileUtil.formatSize(Long.MIN_VALUE),
                    "文件大小为 long 下限时应抛出业务异常");
            assertEquals("错误的文件大小: " + Long.MIN_VALUE, exception.getMessage(),
                    "异常信息应带上 long 下限的数值");
        }

        @Test
        @DisplayName("超出最大单位的分支在 long 范围内不可达")
        void unreachableBranch() {
            // 循环用尽 9 个单位后仍需 fileSize < 1024 才返回，
            // 需要 size >= 1024^9 ≈ 1.24E27，超过 Long.MAX_VALUE ≈ 9.22E18，
            // 因此 for 循环末尾的 ServiceException 分支在 long 范围内不可达，此处仅做说明。
            assertTrue(Math.pow(1024, 9) > (double) Long.MAX_VALUE,
                    "1024 的 9 次方超出 long 上限，说明末尾的异常分支不可达");
        }
    }

    @Nested
    @DisplayName("createDirectories 创建文件夹")
    class CreateDirectoriesTest {
        @Test
        @DisplayName("不存在的多级目录应被创建")
        void create() {
            Path target = tempDir.resolve("a/b/c");
            assertFalse(Files.exists(target), "创建前目录不应存在");
            FileUtil.createDirectories(target.toString());
            assertTrue(Files.isDirectory(target), "多级目录应被完整创建");
        }

        @Test
        @DisplayName("已存在的目录应保持幂等")
        void idempotent() throws IOException {
            Path target = Files.createDirectories(tempDir.resolve("exists"));
            assertDoesNotThrow(() -> {
                FileUtil.createDirectories(target.toString());
                FileUtil.createDirectories(target.toString() + File.separator);
            }, "重复创建已存在目录不应抛出异常");
            assertTrue(Files.isDirectory(target), "重复创建后目录仍应存在");
        }

        @Test
        @DisplayName("路径是普通文件时应跳过且不报错")
        void pathIsFile() throws IOException {
            Path file = tempDir.resolve("plain.txt");
            Files.writeString(file, "x");
            assertDoesNotThrow(() -> FileUtil.createDirectories(file.toString()),
                    "路径已存在时直接返回，不应抛出异常");
            assertTrue(Files.isRegularFile(file), "普通文件不应被改动");
        }

        @Test
        @DisplayName("父级是普通文件时应抛出业务异常")
        void parentIsFile() throws IOException {
            Path file = tempDir.resolve("blocker.txt");
            Files.writeString(file, "x");
            Path target = file.resolve("child");
            assertThrows(ServiceException.class, () -> FileUtil.createDirectories(target.toString()),
                    "父路径为普通文件时应抛出业务异常");
        }
    }

    @Nested
    @DisplayName("formatDirectory 格式化文件夹")
    class FormatDirectoryTest {
        @Test
        @DisplayName("不带分隔符时应补充分隔符")
        void appendSeparator() {
            assertEquals("abc" + File.separator, FileUtil.formatDirectory("abc"),
                    "不带分隔符时应补充分隔符");
            assertEquals(tempDir.toString() + File.separator, FileUtil.formatDirectory(tempDir.toString()),
                    "绝对路径也应补充分隔符");
        }

        @Test
        @DisplayName("已带分隔符时应保持不变")
        void keepSeparator() {
            assertEquals("abc" + File.separator, FileUtil.formatDirectory("abc" + File.separator),
                    "已带分隔符时应原样返回");
            assertEquals(File.separator, FileUtil.formatDirectory(File.separator),
                    "根目录应保持不变");
        }

        @Test
        @DisplayName("空串会变成一个分隔符")
        void emptyString() {
            assertEquals(File.separator, FileUtil.formatDirectory(""), "空串应变成一个分隔符");
        }

        @Test
        @DisplayName("中间含分隔符时只补末尾")
        void middleSeparator() {
            assertEquals("a" + File.separator + "b" + File.separator,
                    FileUtil.formatDirectory("a" + File.separator + "b"),
                    "只在末尾补充分隔符");
        }
    }

    @Nested
    @DisplayName("getTodayDirectory 今日文件夹")
    class GetTodayDirectoryTest {
        @Test
        @DisplayName("应为 yyyyMMdd 加分隔符")
        void format() {
            String actual = FileUtil.getTodayDirectory();
            String expected = DateTimeFormatter.FULL_DATE.formatCurrent().replace("-", "") + File.separator;
            // 跨零点时可能取到相邻两天，因此同时校验格式与“当前日期或相邻日期”
            String before = DateTimeFormatter.FULL_DATE.format(System.currentTimeMillis()).replace("-", "");
            String after = DateTimeFormatter.FULL_DATE.format(System.currentTimeMillis()).replace("-", "");
            assertTrue(actual.matches("\\d{8}" + Pattern.quote(File.separator)),
                    "今日文件夹应为 8 位日期加分隔符，实际为：" + actual);
            assertTrue(actual.startsWith(before) || actual.startsWith(after) || actual.equals(expected),
                    "今日文件夹应为今天的 yyyyMMdd，实际为：" + actual);
        }
    }

    @Nested
    @DisplayName("saveFile 保存文件")
    class SaveFileTest {
        @Test
        @DisplayName("字节数组应写入临时目录")
        void saveBytes() throws IOException {
            Path dir = tempDir.resolve("bytes");
            byte[] content = "hello airpower".getBytes(StandardCharsets.UTF_8);
            FileUtil.saveFile(dir.toString(), "a.txt", content);
            Path file = dir.resolve("a.txt");
            assertTrue(Files.exists(file), "文件应被创建");
            assertEquals("hello airpower", new String(readBytes(file), StandardCharsets.UTF_8),
                    "写入的内容应与原始字节一致");
        }

        @Test
        @DisplayName("默认选项应覆盖旧内容")
        void overwrite() throws IOException {
            Path dir = tempDir.resolve("overwrite");
            FileUtil.saveFile(dir.toString(), "a.txt", "first".getBytes(StandardCharsets.UTF_8));
            FileUtil.saveFile(dir.toString(), "a.txt", "second".getBytes(StandardCharsets.UTF_8));
            assertEquals("second", new String(readBytes(dir.resolve("a.txt")), StandardCharsets.UTF_8),
                    "默认写入应覆盖而不是追加");
        }

        @Test
        @DisplayName("APPEND 选项应追加内容")
        void append() throws IOException {
            Path dir = tempDir.resolve("append");
            FileUtil.saveFile(dir.toString(), "a.txt", "AB".getBytes(StandardCharsets.UTF_8));
            FileUtil.saveFile(dir.toString(), "a.txt", "CD".getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
            assertEquals("ABCD", new String(readBytes(dir.resolve("a.txt")), StandardCharsets.UTF_8),
                    "追加模式应在原有内容后继续写入");
        }

        @Test
        @DisplayName("只传 APPEND 时也应创建文件（分页导出的写法）")
        void appendCreatesMissingFile() throws IOException {
            Path dir = tempDir.resolve("append-creates");
            Path file = dir.resolve("a.csv");
            // NIO 的 APPEND 不含创建语义，修复前这里必然抛 NoSuchFileException
            assertDoesNotThrow(() -> FileUtil.saveFile(dir.toString(), "a.csv",
                            "表头A,表头B\n".getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND),
                    "只传 APPEND 时应自动补上 CREATE，否则分页导出的第一次写表头必然失败");
            assertTrue(Files.exists(file), "文件应被创建");

            FileUtil.saveFile(dir.toString(), "a.csv", "1,张三\n".getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
            assertEquals("表头A,表头B\n1,张三\n",
                    new String(readBytes(file), StandardCharsets.UTF_8), "首次创建后应继续正常追加");
        }

        @Test
        @DisplayName("APPEND 与 CREATE 同时传入不应重复创建")
        void appendWithExplicitCreate() throws IOException {
            Path dir = tempDir.resolve("append-and-create");
            Path file = dir.resolve("a.csv");
            FileUtil.saveFile(dir.toString(), "a.csv", "第一行\n".getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            FileUtil.saveFile(dir.toString(), "a.csv", "第二行\n".getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            assertEquals("第一行\n第二行\n", new String(readBytes(file), StandardCharsets.UTF_8),
                    "调用方自己带上 CREATE 时不应改变追加语义");
        }

        @Test
        @DisplayName("输入流重载应边读边写，堆占用与流长度无关")
        void saveInputStream() throws IOException {
            Path dir = tempDir.resolve("input-stream");
            Path file = dir.resolve("big.txt");
            byte[] payload = new byte[1024 * 1024];
            Arrays.fill(payload, (byte) 'A');

            FileUtil.saveFile(dir.toString(), "big.txt", new ByteArrayInputStream(payload));

            assertEquals(payload.length, Files.size(file), "落盘字节数应与源流一致");
            assertEquals('A', readBytes(file)[0], "内容应原样落盘");
        }

        @Test
        @DisplayName("输入流重载不应关闭调用方传入的流")
        void inputStreamNotClosed() throws IOException {
            Path dir = tempDir.resolve("stream-not-closed");
            boolean[] closed = {false};
            InputStream source = new ByteArrayInputStream("中文内容".getBytes(StandardCharsets.UTF_8)) {
                @Override
                public void close() {
                    closed[0] = true;
                }
            };

            FileUtil.saveFile(dir.toString(), "a.txt", source);

            assertFalse(closed[0], "流的所有权属于调用方，工具类不应替它关闭");
        }

        @Test
        @DisplayName("输入流重载应沿用路径穿越与文件名白名单校验")
        void inputStreamRejectsIllegalFileName() {
            Path dir = tempDir.resolve("stream-guard");
            assertThrows(ServiceException.class,
                    () -> FileUtil.saveFile(dir.toString(), "../evil.txt",
                            new ByteArrayInputStream(new byte[]{1})),
                    "与 byte[] 重载一样必须拒绝目录穿越");
            assertThrows(ServiceException.class,
                    () -> FileUtil.saveFile(dir.toString(), "中文名.txt",
                            new ByteArrayInputStream(new byte[]{1})),
                    "与 byte[] 重载一样只允许安全字符的文件名");
        }

        @Test
        @DisplayName("空字节数组应生成空文件")
        void emptyContent() throws IOException {
            Path dir = tempDir.resolve("empty-content");
            FileUtil.saveFile(dir.toString(), "a.txt", new byte[0]);
            assertEquals(0L, Files.size(dir.resolve("a.txt")), "空字节数组应生成 0 字节文件");
        }

        @Test
        @DisplayName("字符串重载应按 UTF-8 写入")
        void saveString() throws IOException {
            Path dir = tempDir.resolve("string");
            String content = "中文内容 - AirPower";
            FileUtil.saveFile(dir.toString(), "utf8.txt", content);
            assertEquals(content, new String(readBytes(dir.resolve("utf8.txt")), StandardCharsets.UTF_8),
                    "字符串应按 UTF-8 编码写入，读取后内容一致");
        }

        @Test
        @DisplayName("字符串重载同样支持追加")
        void appendString() throws IOException {
            Path dir = tempDir.resolve("string-append");
            FileUtil.saveFile(dir.toString(), "utf8.txt", "中文");
            FileUtil.saveFile(dir.toString(), "utf8.txt", "追加", StandardOpenOption.APPEND);
            assertEquals("中文追加", new String(readBytes(dir.resolve("utf8.txt")), StandardCharsets.UTF_8),
                    "字符串追加后应与原内容拼接");
        }

        @Test
        @DisplayName("目录不存在时应自动创建")
        void autoCreateDirectory() {
            Path dir = tempDir.resolve("auto/created");
            assertDoesNotThrow(() -> FileUtil.saveFile(dir.toString(), "a.txt", "x"),
                    "保存文件时应自动创建目标目录");
            assertTrue(Files.isDirectory(dir), "目标目录应被自动创建");
        }

        @Test
        @DisplayName("目录末尾无分隔符也能正确拼接")
        void directoryWithoutSeparator() throws IOException {
            Path dir = tempDir.resolve("no-separator");
            FileUtil.saveFile(dir.toString(), "a.txt", "ok");
            assertTrue(Files.exists(dir.resolve("a.txt")), "目录末尾无分隔符时应自动补齐");
        }

        @Test
        @DisplayName("文件名含子目录但父级不存在时应抛出业务异常")
        void missingParentOfFile() {
            Path dir = tempDir.resolve("missing-parent");
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> FileUtil.saveFile(dir.toString(), "sub/a.txt", "x"),
                    "父目录不存在时应抛出业务异常");
            assertTrue(exception.getMessage().startsWith("文件保存失败，"), "异常信息应以“文件保存失败”开头");
        }
    }

    @Nested
    @DisplayName("zip 压缩文件夹")
    class ZipTest {
        @Test
        @DisplayName("源路径是根目录时不得因 getFileName() 为 null 而 NPE")
        void rootDirectorySource() {
            // Path.getFileName() 对根路径返回 null，原实现 .toString() 直接 NPE
            Path zip = tempDir.resolve("root.zip");

            // 根路径的 getFileName() 返回 null；压缩根目录本身属于危险操作，
            // 但必须给出受控的异常而不是 NPE
            assertThrows(IOException.class, () -> FileUtil.zip("/", zip.toString()),
                    "必须是受控的 IOException，不是 NullPointerException");
        }

        @Test
        @DisplayName("压缩包累计大小超过上限时必须中止")
        void zipTotalSizeLimit() throws IOException {
            // 用一个 20MB 的源目录验证累计上限生效（上限 10GB，此处通过反射下调以便测试）
            Path source = Files.createDirectories(tempDir.resolve("zip-source"));
            byte[] chunk = new byte[1024 * 1024];
            Arrays.fill(chunk, (byte) 'A');
            for (int i = 0; i < 20; i++) {
                Files.write(source.resolve("big-" + i + ".bin"), chunk);
            }
            Path zip = tempDir.resolve("limited.zip");

            long originalLimit = zipTotalBytesLimit.get();
            try {
                // 把上限临时下调到 5MB，让 20MB 的目录必然超限
                zipTotalBytesLimit.set(5L * 1024 * 1024);
                IOException exception = assertThrows(IOException.class,
                        () -> FileUtil.zip(source.toString(), zip.toString()),
                        "整个 zip 原本没有任何累计上限，一个 50GB 的目录就能撑爆磁盘");
                assertTrue(exception.getMessage().contains("超过上限"),
                        "异常信息应说明是累计大小超限，实际：" + exception.getMessage());
            } finally {
                zipTotalBytesLimit.set(originalLimit);
            }
        }

        @Test
        @DisplayName("源文件夹不存在时应抛出 IO 异常")
        void sourceNotExist() {
            Path source = tempDir.resolve("not-exist");
            Path zip = tempDir.resolve("out.zip");
            IOException exception = assertThrows(IOException.class, () -> FileUtil.zip(source.toString(), zip.toString()),
                    "源文件夹不存在时应抛出 IO 异常");
            assertTrue(exception.getMessage().contains("源文件夹不存在:"), "异常信息应提示源文件夹不存在");
        }

        @Test
        @DisplayName("源路径是普通文件时应抛出 IO 异常")
        void sourceIsFile() throws IOException {
            Path source = tempDir.resolve("plain.txt");
            Files.writeString(source, "x");
            Path zip = tempDir.resolve("out.zip");
            assertThrows(IOException.class, () -> FileUtil.zip(source.toString(), zip.toString()),
                    "源路径不是目录时应抛出 IO 异常");
        }

        @Test
        @DisplayName("输出目录不存在时应抛出 IO 异常")
        void outputParentNotExist() throws IOException {
            Path source = Files.createDirectories(tempDir.resolve("src"));
            Path zip = tempDir.resolve("no-such-dir/out.zip");
            assertThrows(IOException.class, () -> FileUtil.zip(source.toString(), zip.toString()),
                    "输出目录不存在时应抛出 IO 异常");
        }

        @Test
        @DisplayName("应包含目录条目、文件条目与子目录内容")
        void zipDirectory() throws IOException {
            Path source = Files.createDirectories(tempDir.resolve("source"));
            Files.writeString(source.resolve("a.txt"), "内容A", StandardCharsets.UTF_8);
            Path sub = Files.createDirectories(source.resolve("sub"));
            Files.writeString(sub.resolve("b.txt"), "内容B", StandardCharsets.UTF_8);
            Path zip = tempDir.resolve("out.zip");

            FileUtil.zip(source.toString(), zip.toString());

            assertTrue(Files.exists(zip), "压缩包应被生成");
            Set<String> names = new HashSet<>();
            try (ZipFile zipFile = new ZipFile(zip.toFile())) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    names.add(entries.nextElement().getName());
                }
                assertEquals(4, names.size(), "压缩包应包含 1 个目录条目、1 个子目录条目和 2 个文件条目");
                assertTrue(names.contains("source/"), "应包含源目录条目");
                assertTrue(names.contains("source/sub/"), "应包含子目录条目");
                assertEquals("内容A", readZipEntry(zipFile, "source/a.txt"), "源目录文件内容应保持不变");
                assertEquals("内容B", readZipEntry(zipFile, "source/sub/b.txt"), "子目录文件内容应保持不变");
            }
        }

        @Test
        @DisplayName("空目录应只产生目录条目")
        void zipEmptyDirectory() throws IOException {
            Path source = Files.createDirectories(tempDir.resolve("empty"));
            Path zip = tempDir.resolve("empty.zip");
            FileUtil.zip(source.toString(), zip.toString());
            try (ZipFile zipFile = new ZipFile(zip.toFile())) {
                assertEquals(1, zipFile.size(), "空目录压缩后应只有一个目录条目");
                ZipEntry entry = zipFile.entries().nextElement();
                assertEquals("empty/", entry.getName(), "目录条目名应为目录名加分隔符");
                assertTrue(entry.isDirectory(), "该条目应为目录条目");
            }
        }

        @Test
        @DisplayName("空文件也能正常压缩")
        void zipEmptyFile() throws IOException {
            Path source = Files.createDirectories(tempDir.resolve("with-empty-file"));
            Files.write(source.resolve("empty.txt"), new byte[0]);
            Path zip = tempDir.resolve("with-empty-file.zip");
            FileUtil.zip(source.toString(), zip.toString());
            try (ZipFile zipFile = new ZipFile(zip.toFile())) {
                assertEquals(2, zipFile.size(), "应包含目录条目与空文件条目");
                assertEquals("", readZipEntry(zipFile, "with-empty-file/empty.txt"), "空文件内容应为空串");
            }
        }

        /**
         * 读取压缩包中的条目内容
         *
         * @param zipFile 压缩包
         * @param name    条目名
         * @return 条目内容
         * @throws IOException 读取异常
         */
        private String readZipEntry(ZipFile zipFile, String name) throws IOException {
            ZipEntry entry = zipFile.getEntry(name);
            assertNotNull(entry, "压缩包中应存在条目 " + name);
            try (var in = zipFile.getInputStream(entry)) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }

    @Nested
    @DisplayName("deleteDirectory 删除文件夹")
    class DeleteDirectoryTest {
        @Test
        @DisplayName("应递归删除子目录与文件")
        void deleteTree() throws IOException {
            Path root = Files.createDirectories(tempDir.resolve("tree"));
            Path deep = Files.createDirectories(root.resolve("a/b/c"));
            Files.writeString(deep.resolve("f.txt"), "x");
            Files.writeString(root.resolve("top.txt"), "y");

            FileUtil.deleteDirectory(root.toString());

            assertFalse(Files.exists(root), "目标目录应被删除");
            assertFalse(Files.exists(root.resolve("a")), "子目录应被递归删除");
        }

        @Test
        @DisplayName("目录不存在时不应抛出异常")
        void notExist() {
            Path target = tempDir.resolve("nothing");
            assertDoesNotThrow(() -> FileUtil.deleteDirectory(target.toString()),
                    "删除不存在的目录应静默返回");
        }

        @Test
        @DisplayName("空目录也应被删除")
        void emptyDirectory() throws IOException {
            Path root = Files.createDirectories(tempDir.resolve("empty-dir"));
            FileUtil.deleteDirectory(root.toString() + File.separator);
            assertFalse(Files.exists(root), "空目录应被删除");
        }

        @Test
        @DisplayName("删除后可以重新创建同名目录")
        void recreateAfterDelete() throws IOException {
            Path root = Files.createDirectories(tempDir.resolve("recreate"));
            Files.writeString(root.resolve("f.txt"), "x");
            FileUtil.deleteDirectory(root.toString());
            FileUtil.createDirectories(root.toString());
            assertTrue(Files.isDirectory(root), "删除后应能重新创建同名目录");
            assertFalse(Files.exists(root.resolve("f.txt")), "重新创建后不应残留旧文件");
            try (var children = Files.list(root)) {
                assertEquals(0L, children.count(), "重新创建后目录应为空");
            }
        }
    }

    /**
     * <h2>异常类型一致性</h2>
     *
     * <p>回归 P1-18 / P1-19：{@code saveFile} 与 {@code zip} 此前会把 JDK 原生异常
     * （{@code IllegalArgumentException} / {@code NotDirectoryException}）直接抛给调用方，
     * 而同方法的其它失败路径却包成了 {@code ServiceException}，错误风格不统一。</p>
     */
    @Nested
    @DisplayName("异常类型一致性")
    class ExceptionConsistencyTest {

        @Test
        @DisplayName("saveFile 传入非法的 OpenOption 时抛 ServiceException 而非 JDK 原生异常")
        void saveFileWithIllegalOptionsThrowsServiceException() throws IOException {
            Path dir = Files.createTempDirectory("airpower-save-");
            try {
                // 只给 READ 选项：Files.write 会抛 IllegalArgumentException
                ServiceException exception = assertThrows(ServiceException.class,
                        () -> FileUtil.saveFile(dir.toString(), "a.txt", "内容",
                                StandardOpenOption.READ),
                        "非法 OpenOption 应包装为业务异常，与目录创建失败的风格保持一致");
                assertTrue(exception.getMessage().startsWith("文件保存失败"),
                        "异常消息应说明是文件保存失败：" + exception.getMessage());
            } finally {
                FileUtil.deleteDirectory(dir.toString());
            }
        }

        @Test
        @DisplayName("zip 的源路径是普通文件时报错说明是文件而非文件夹")
        void zipWithFileSourceIsRejected() throws IOException {
            Path file = Files.createTempFile("airpower-zip-", ".txt");
            try {
                IOException exception = assertThrows(IOException.class,
                        () -> FileUtil.zip(file.toString(), file.toString() + ".zip"),
                        "源路径是文件时应在入口处报错，而不是漏出 NotDirectoryException");
                assertTrue(exception.getMessage().contains("不是文件夹"),
                        "异常信息应说明源路径不是文件夹：" + exception.getMessage());
            } finally {
                Files.deleteIfExists(file);
                Files.deleteIfExists(Path.of(file + ".zip"));
            }
        }

        @Test
        @DisplayName("zip 的压缩文件落在源目录内部时报错")
        void zipIntoSourceDirectoryIsRejected() throws IOException {
            Path dir = Files.createTempDirectory("airpower-zipin-");
            try {
                Path inner = dir.resolve("out.zip");
                IOException exception = assertThrows(IOException.class,
                        () -> FileUtil.zip(dir.toString(), inner.toString()),
                        "压缩包写入源目录内部会破坏正在遍历的目录树，应在入口处拒绝");
                assertTrue(exception.getMessage().contains("不能输出到源文件夹内部"),
                        "异常信息应说明输出位置不合法：" + exception.getMessage());
            } finally {
                FileUtil.deleteDirectory(dir.toString());
            }
        }

        @Test
        @DisplayName("zip 源目录不存在时仍按原有文案报错")
        void zipWithMissingSource() {
            IOException exception = assertThrows(IOException.class,
                    () -> FileUtil.zip("/definitely/not/exists/dir", "/tmp/out.zip"),
                    "源目录不存在应报 IOException");
            assertTrue(exception.getMessage().contains("源文件夹不存在"),
                    "异常信息应说明源文件夹不存在：" + exception.getMessage());
        }
    }
}
