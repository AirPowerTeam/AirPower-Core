package cn.hamm.airpower.core;

import cn.hamm.airpower.core.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static cn.hamm.airpower.core.enums.DateTimeFormatter.FULL_DATE;

/**
 * <h1>文件工具类</h1>
 *
 * @author Hamm.cn
 */
@Slf4j
public class FileUtil {
    /**
     * 文件大小进制
     */
    public static final long FILE_SCALE = 1024L;

    /**
     * 文件单位
     * <p>用 {@link List} 而非数组：{@code final} 数组的内容仍可被任何调用方改写，
     * 且改动全进程生效、无任何告警</p>
     */
    public static final List<String> UNITS =
            List.of("B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB");

    /**
     * 文件名分隔符
     */
    public static final String EXTENSION_SEPARATOR = ".";

    /**
     * ZIP 条目名的目录分隔符（规范固定为 {@code /}，与平台无关）
     */
    private static final String ZIP_SEPARATOR = "/";

    /**
     * 单个 ZIP 的累计输出字节上限（10GB）
     * <p>逐文件流式拷贝本身是对的（不会把单个文件读进堆），但整个 zip 原本
     * <b>没有累计上限</b>：一个 50GB 的目录就能生成 50GB 的 zip 撑爆磁盘。
     * 而且默认压缩级别对已压缩数据（图片/视频/已有的 .gz/.zip）几乎不压缩，
     * 目录占用会被完整复制一遍</p>
     */
    private static final long MAX_ZIP_TOTAL_BYTES = 10L * 1024 * 1024 * 1024;

    /**
     * ZIP 累计大小上限的可覆盖句柄
     * <p>默认取 {@link #MAX_ZIP_TOTAL_BYTES}。10GB 的上限在测试里无法直接构造，
     * 因此提供这个受控入口让测试能下调阈值；包外无法写入（无 setter）</p>
     */
    static final ThreadLocal<Long> ZIP_TOTAL_LIMIT =
            ThreadLocal.withInitial(() -> MAX_ZIP_TOTAL_BYTES);

    /**
     * 复制时的分块大小
     */
    private static final int ZIP_COPY_CHUNK = 64 * 1024;

    /**
     * 文件大小格式化的 {@link DecimalFormat}，按线程复用
     * <p>{@code DecimalFormat} 非线程安全，不能做成共享静态常量；
     * 但每次调用都新建一个的分配量对「列表页每行调一次」的场景也偏高</p>
     */
    private static final ThreadLocal<DecimalFormat> SIZE_FORMAT = ThreadLocal.withInitial(
            // 固定使用 ROOT Locale，避免德语等环境下输出 1,00KB 导致调用方解析失败
            () -> new DecimalFormat("#.00", DecimalFormatSymbols.getInstance(Locale.ROOT))
    );

    /**
     * 未知文件大小
     */
    private static final String UNKNOWN_FILE_SIZE = "错误的文件大小: %s";

    /**
     * 禁止外部实例化
     */
    @Contract(pure = true)
    private FileUtil() {

    }

    /**
     * 获取文件名后缀
     *
     * @param fileName 文件名
     * @return 后缀
     */
    public static @NotNull String getExtension(String fileName) {
        if (Objects.isNull(fileName)) {
            throw new ServiceException("文件名不能为空");
        }
        int index = fileName.lastIndexOf(EXTENSION_SEPARATOR);
        if (index < 0 || index == fileName.length() - EXTENSION_SEPARATOR.length()) {
            // 原实现会把整个文件名当成扩展名返回
            return "";
        }
        if (index == 0) {
            // 整个文件名就是一个点开头的隐藏文件：按 git / nginx / apache 的惯例，
            // .gitignore、.env、.htaccess 都<b>没有</b>扩展名
            return "";
        }
        // 固定 Locale.ROOT：土耳其语环境下 "TXT".toLowerCase() 会得到 "tхt"
        return fileName.substring(index + EXTENSION_SEPARATOR.length()).toLowerCase(Locale.ROOT);
    }

    /**
     * 格式化文件大小
     *
     * @param size 文件大小
     * @return 格式化后的文件大小
     */
    public static @NotNull String formatSize(long size) {
        if (size < 0) {
            throw new ServiceException(String.format(UNKNOWN_FILE_SIZE, size));
        }
        if (size == 0) {
            // 0 字节是合法的空文件，原实现与负数一起拒绝
            return "0.00" + UNITS.get(0);
        }
        double fileSize = size;
        // DecimalFormat 非线程安全，不能做成共享静态常量；用 ThreadLocal 复用
        DecimalFormat decimalFormat = SIZE_FORMAT.get();
        for (String unit : UNITS) {
            if (fileSize < FILE_SCALE) {
                return decimalFormat.format(fileSize) + unit;
            }
            fileSize /= FILE_SCALE;
        }
        throw new ServiceException(String.format(UNKNOWN_FILE_SIZE, size));
    }

    /**
     * 创建文件夹
     *
     * @param pathString 文件夹路径
     */
    public static void createDirectories(String pathString) {
        Path path = Paths.get(pathString);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new ServiceException("自动创建文件夹失败，" + e.getMessage());
            }
        }
    }

    /**
     * 获取今日文件夹
     *
     * @return 今日文件夹路径
     */
    public static @NotNull String getTodayDirectory() {
        String todayDirectory = FULL_DATE.formatCurrent().replace("-", "");
        return formatDirectory(todayDirectory);
    }

    /**
     * 格式化文件夹
     *
     * @param directory 文件夹
     * @return 格式化后的文件夹
     */
    @Contract(pure = true)
    public static @NotNull String formatDirectory(@NotNull String directory) {
        if (!directory.endsWith(File.separator)) {
            directory += File.separator;
        }
        return directory;
    }

    /**
     * 保存文件
     *
     * @param absoluteDirectory 目录绝对路径
     * @param fileName          文件名
     * @param bytes             文件字节数组
     * @param options           保存选项
     * @apiNote 传了 {@link StandardOpenOption#APPEND} 却没传 {@code CREATE} 时会自动补上
     * {@code CREATE}。NIO 的 {@code APPEND} 自身并不蕴含「文件不存在则创建」，
     * 「首次创建 + 后续追加」这种分页写入的写法只传 {@code APPEND} 时，第一次写就会抛
     * {@link NoSuchFileException}
     */
    public static void saveFile(@NotNull String absoluteDirectory, @NotNull String fileName, byte @NotNull [] bytes, OpenOption @NotNull ... options) {
        Path target = resolveSaveTarget(absoluteDirectory, fileName);
        try {
            Files.write(target, bytes, completeAppendOption(options));
        } catch (IOException | IllegalArgumentException e) {
            // 带上异常类型：IO 异常的 getMessage() 往往只有路径，缺了类型根本无法判断是权限、
            // 目录还是文件不存在
            throw new ServiceException("文件保存失败，[" + e.getClass().getSimpleName() + "] " + e.getMessage());
        }
    }

    /**
     * 校验并解析待写入的目标文件路径
     *
     * @param absoluteDirectory 目录绝对路径
     * @param fileName          文件名
     * @return 规范化后的目标路径
     * @apiNote 目录会自动创建。这里集中做「穿越、符号链接、文件名白名单」三项校验，
     * 让 {@code byte[]} 与 {@link InputStream} 两个重载共享同一套安全规则
     */
    private static @NotNull Path resolveSaveTarget(@NotNull String absoluteDirectory, @NotNull String fileName) {
        absoluteDirectory = formatDirectory(absoluteDirectory);
        Path base = Paths.get(absoluteDirectory).toAbsolutePath().normalize();
        Path target = base.resolve(fileName).normalize();
        // 1) 拒绝穿越：目标必须仍在 base 之下
        if (!target.startsWith(base)) {
            throw new ServiceException("文件保存失败，非法的文件名：" + fileName);
        }
        // 2) 拒绝符号链接
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(target)) {
            throw new ServiceException("文件保存失败，目标路径不允许为符号链接：" + fileName);
        }
        // 3) fileName 只允许安全字符
        if (!fileName.matches("[A-Za-z0-9._\\-]+") || fileName.contains("..")) {
            throw new ServiceException("文件保存失败，非法的文件名：" + fileName);
        }
        createDirectories(base.toString());
        return target;
    }

    /**
     * 为「只传了 APPEND」的调用补上 CREATE
     *
     * @param options 原始保存选项
     * @return 补齐后的保存选项
     * @apiNote {@link StandardOpenOption#APPEND} 只声明「从文件末尾写入」，不含创建语义，
     * 单独使用而文件又不存在时 NIO 会直接抛 {@link NoSuchFileException}。绝大多数调用方
     * 想要的都是「不存在就创建、存在就追加」，所以在这里补齐；已经带了
     * {@code CREATE} / {@code CREATE_NEW} 的原样返回
     */
    private static OpenOption @NotNull [] completeAppendOption(OpenOption @NotNull [] options) {
        boolean append = false;
        boolean create = false;
        for (OpenOption option : options) {
            append |= StandardOpenOption.APPEND == option;
            create |= StandardOpenOption.CREATE == option || StandardOpenOption.CREATE_NEW == option;
        }
        if (!append || create) {
            return options;
        }
        OpenOption[] completed = Arrays.copyOf(options, options.length + 1);
        completed[options.length] = StandardOpenOption.CREATE;
        return completed;
    }

    /**
     * 保存文件
     *
     * @param absoluteDirectory 目录绝对路径
     * @param fileName          文件名
     * @param string            文件字符串内容
     * @param options           保存选项
     */
    public static void saveFile(@NotNull String absoluteDirectory, @NotNull String fileName, @NotNull String string, OpenOption @NotNull ... options) {
        saveFile(absoluteDirectory, fileName, string.getBytes(StandardCharsets.UTF_8), options);
    }

    /**
     * 把输入流保存为文件，边读边写
     *
     * @param absoluteDirectory 目录绝对路径
     * @param fileName          文件名
     * @param inputStream       输入流，方法内部<b>不会</b>关闭它
     * @param options           保存选项
     * @apiNote 供内容大到不能一次性读进堆里的场景使用。用
     * {@code readAllBytes()} 再写会同时驻留「源缓冲 + 目标 byte[]」两份，
     * 这里用固定 8KB 缓冲，堆占用与文件大小无关
     * @apiNote 路径校验、符号链接拒绝、文件名白名单与 {@code byte[]} 重载完全一致；
     * 目录不存在时同样自动创建。{@code APPEND} 仍会被补上 {@code CREATE}
     */
    public static void saveFile(@NotNull String absoluteDirectory, @NotNull String fileName,
                                @NotNull InputStream inputStream, OpenOption @NotNull ... options) {
        Path target = resolveSaveTarget(absoluteDirectory, fileName);
        createDirectories(target.getParent().toString());
        try (OutputStream out = Files.newOutputStream(target, completeAppendOption(options))) {
            inputStream.transferTo(out);
        } catch (IOException | IllegalArgumentException e) {
            // 带上异常类型：IO 异常的 getMessage() 往往只有路径，缺了类型无法判断是权限、
            // 目录还是文件不存在
            throw new ServiceException("文件保存失败，[" + e.getClass().getSimpleName() + "] " + e.getMessage());
        }
    }

    /**
     * 将整个文件夹压缩为 ZIP 文件
     *
     * @param sourceDirPath 源文件夹路径
     * @param zipFilePath   ZIP 文件输出路径
     * @throws IOException IO 异常
     */
    public static void zip(String sourceDirPath, String zipFilePath) throws IOException {
        Path sourceDir = Paths.get(sourceDirPath);
        if (Files.isSymbolicLink(sourceDir)) {
            throw new IOException("源文件夹是符号链接: " + sourceDirPath);
        }
        if (!Files.exists(sourceDir)) {
            throw new IOException("源文件夹不存在: " + sourceDirPath);
        }
        if (!Files.isDirectory(sourceDir)) {
            // 少这一句，传普通文件时会由 newDirectoryStream 抛 NotDirectoryException，
            // 异常信息里只有路径，调用方无从判断是自己传错了参数
            throw new IOException("源路径不是文件夹: " + sourceDirPath);
        }
        Path target = Paths.get(zipFilePath);
        if (sourceDir.equals(target) || (target.startsWith(sourceDir) && !target.equals(sourceDir))) {
            // 压缩包落在源目录内部：写入时会破坏正在遍历的目录树
            throw new IOException("压缩文件不能输出到源文件夹内部: " + zipFilePath);
        }
        // 根路径（如 "/"）的 getFileName() 返回 null，toString() 直接 NPE。
        // 兜底用根分隔符，条目名会是 "/"，这是 ZIP 规范允许的
        Path sourceName = sourceDir.getFileName();
        String rootEntryName = Objects.isNull(sourceName) ? ZIP_SEPARATOR : sourceName.toString();
        try (FileOutputStream fos = new FileOutputStream(zipFilePath);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            zipDirectory(sourceDir, rootEntryName, zos, new ZipSizeLimiter(MAX_ZIP_TOTAL_BYTES));
        }
    }

    /**
     * 递归压缩目录
     *
     * @param dir     要压缩的目录
     * @param dirName 当前目录名称
     * @param zos     ZIP 输出流
     * @throws IOException IO 异常
     */
    private static void zipDirectory(Path dir, String dirName, @NotNull ZipOutputStream zos) throws IOException {
        zipDirectory(dir, dirName, zos, null);
    }

    /**
     * 递归压缩目录
     *
     * @param dir     要压缩的目录
     * @param dirName 当前目录名称
     * @param zos     ZIP 输出流
     * @param limiter 累计字节计数器，为 {@code null} 表示不限制
     * @throws IOException IO 异常，超出上限时同样以 IOException 形式抛出
     */
    private static void zipDirectory(Path dir, String dirName, @NotNull ZipOutputStream zos,
                                     @Nullable ZipSizeLimiter limiter) throws IOException {
        // ZIP 规范要求条目名统一使用 '/'，不能沿用 File.separator（Windows 上是 '\'）
        dirName = dirName.endsWith(ZIP_SEPARATOR) ? dirName : dirName + ZIP_SEPARATOR;

        ZipEntry dirEntry = new ZipEntry(dirName);
        zos.putNextEntry(dirEntry);
        zos.closeEntry();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (Files.isSymbolicLink(path)) {
                    // 日志里必须带上路径，否则线上只知道「跳过了某个符号链接」，
                    // 无从定位跳了什么
                    log.warn("跳过符号链接：{}", path);
                    continue;
                }
                String entryName = dirName + path.getFileName();

                if (Files.isDirectory(path)) {
                    // 递归处理子目录
                    zipDirectory(path, entryName, zos, limiter);
                    continue;
                }
                // 添加文件条目
                ZipEntry fileEntry = new ZipEntry(entryName);
                zos.putNextEntry(fileEntry);

                // 写入文件内容。按块搬运并累计字节数，超上限立刻中止：
                // 一次性 transferTo 会把整个文件灌进去，中途才发现超限就晚了
                long fileSize = Files.size(path);
                if (Objects.nonNull(limiter) && !limiter.tryAcquire(fileSize, ZIP_TOTAL_LIMIT.get())) {
                    throw new IOException("压缩包累计大小超过上限 " + ZIP_TOTAL_LIMIT.get()
                            + " 字节，已在文件(" + path + ")处中止");
                }
                try (InputStream bis = new BufferedInputStream(new FileInputStream(path.toFile()))) {
                    byte[] chunk = new byte[ZIP_COPY_CHUNK];
                    int read;
                    while ((read = bis.read(chunk)) > 0) {
                        zos.write(chunk, 0, read);
                    }
                }
                zos.closeEntry();
            }
        }
    }

    /**
     * <h1>ZIP 累计字节计数器</h1>
     *
     * <p>非线程安全，仅供单次 {@code zip} 递归过程内使用。</p>
     */
    private static final class ZipSizeLimiter {
        /**
         * 上限
         */
        private final long maxBytes;

        /**
         * 已累计字节数
         */
        private long usedBytes;

        /**
         * 构造
         *
         * @param maxBytes 上限
         */
        private ZipSizeLimiter(long maxBytes) {
            this.maxBytes = maxBytes;
        }

        /**
         * 尝试占用配额
         *
         * @param bytes 本次要写入的字节数
         * @return 是否允许
         */
        private boolean tryAcquire(long bytes, long limit) {
            usedBytes += Math.max(bytes, 0L);
            return usedBytes <= limit;
        }
    }

    /**
     * 删除当前文件夹以及当前文件夹内的文件
     *
     * @param directory 文件夹路径
     */
    public static void deleteDirectory(String directory) {
        directory = formatDirectory(directory);
        Path path = Paths.get(directory);
        if (!Files.exists(path)) {
            return;
        }
        // 逆序保证先删子项再删父目录
        try (Stream<Path> walk = Files.walk(path)) {
            List<Path> targets = walk.sorted(Comparator.reverseOrder()).toList();
            List<String> failures = new ArrayList<>();
            for (Path target : targets) {
                try {
                    // 原实现用 File::delete 忽略返回值，删除失败完全无感知
                    Files.deleteIfExists(target);
                } catch (IOException e) {
                    failures.add(target.getFileName() + "(" + e.getMessage() + ")");
                }
            }
            if (!failures.isEmpty()) {
                // 部分删除失败时明确报错，避免调用方误以为目录已清空
                throw new ServiceException("删除文件夹失败，" + failures.size() + " 个条目未删除：" + failures);
            }
        } catch (IOException e) {
            throw new ServiceException("删除文件夹失败，" + e.getMessage());
        }
    }
}
