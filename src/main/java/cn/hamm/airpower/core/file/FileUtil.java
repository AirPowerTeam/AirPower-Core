package cn.hamm.airpower.core.file;

import cn.hamm.airpower.core.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static cn.hamm.airpower.core.datetime.DateTimeFormatter.FULL_DATE;

/**
 * <h2>文件工具类</h2>
 *
 * @author Hamm.cn
 */
@Slf4j
public class FileUtil {
    /**
     * 文件大小进制
     */
    public static final int FILE_SCALE = 1024;

    /**
     * 文件单位
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
     */
    private static final long MAX_ZIP_TOTAL_BYTES = 10L * 1024 * 1024 * 1024;

    /**
     * 压缩的最大递归深度
     *
     * @apiNote 每层递归都要压多个栈帧，病态深的目录树会在默认栈大小下直接
     * {@link StackOverflowError}——而它继承 {@link Error}，逃逸 {@code catch (Exception)}
     */
    private static final int MAX_ZIP_DEPTH = 200;

    /**
     * 复制时的分块大小
     */
    private static final int ZIP_COPY_CHUNK = 64 * 1024;

    /**
     * 文件大小格式化的 {@link DecimalFormat}，按线程复用
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
            return "";
        }
        if (index == 0) {
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
            // 0 字节是合法的空文件，与负数（非法）区别对待
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
        if (directory.isEmpty()) {
            throw new ServiceException("文件目录不能为空");
        }
        return directory.endsWith(File.separator) ? directory : directory + File.separator;
    }

    /**
     * 校验作为写入/删除<b>根目录</b>使用的路径是否安全
     *
     * @param directory 目录路径
     * @return 规范化后的目录路径
     * @apiNote 要求非空、绝对、且不是文件系统根目录。
     */
    public static @NotNull Path assertSafeBaseDirectory(@NotNull String directory) {
        if (directory.isBlank()) {
            throw new ServiceException("文件目录不能为空");
        }
        Path p = Paths.get(directory);
        Path path = p.toAbsolutePath().normalize();
        if (!p.isAbsolute()) {
            throw new ServiceException("文件目录必须是绝对路径，当前为：" + directory);
        }
        if (path.getNameCount() == 0) {
            throw new ServiceException("文件目录不能是文件系统根目录：" + directory);
        }
        return path;
    }

    /**
     * 保存文件
     *
     * @param absoluteDirectory 目录绝对路径
     * @param fileName          文件名
     * @param bytes             文件字节数组
     * @param options           保存选项
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
            throw new IOException("源路径不是文件夹: " + sourceDirPath);
        }
        Path target = Paths.get(zipFilePath).toAbsolutePath().normalize();
        String rootEntryName = getRootEntryName(zipFilePath, sourceDir, target);
        try (FileOutputStream fos = new FileOutputStream(zipFilePath);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            // 真正的累计上限在这里生效：旧实现有个 10GB 的常量与 ThreadLocal 句柄，
            // 但从未被任何代码引用过，读源码的人会以为压缩有大小限制，实际没有
            zipDirectory(sourceDir, rootEntryName, zos, new long[]{0L}, 0);
        }
    }

    /**
     * 删除当前文件夹以及当前文件夹内的文件
     *
     * @param directory 文件夹路径
     * @apiNote <b>递归删除不可撤销，调用方必须确保入参可信。</b>
     */
    public static void deleteDirectory(String directory) {
        Path path = assertSafeBaseDirectory(directory);
        if (!Files.exists(path)) {
            return;
        }
        // 逆序保证先删子项再删父目录
        try (Stream<Path> walk = Files.walk(path)) {
            List<Path> targets = walk.sorted(Comparator.reverseOrder()).toList();
            List<String> failures = new ArrayList<>();
            for (Path target : targets) {
                try {
                    // 用 Files 而非 File#delete：后者忽略返回值，删除失败不会抛异常
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

    /**
     * 断言待删除目录是否落在允许的基准目录之内
     *
     * @param target        待删除的目标目录
     * @param baseDirectory 允许删除的基准目录（绝对路径）
     * @return 规范化后的目标目录
     */
    public static @NotNull Path assertDeletableDirectory(@NotNull String target, @NotNull String baseDirectory) {
        Path base = Paths.get(baseDirectory).toAbsolutePath().normalize();
        Path normalized = Paths.get(target).toAbsolutePath().normalize();
        if (normalized.equals(base)) {
            throw new ServiceException("禁止删除基准目录本身：" + target);
        }
        if (!normalized.startsWith(base)) {
            throw new ServiceException("禁止删除基准目录之外的路径：" + target);
        }
        return normalized;
    }


    /**
     * 获取根条目名
     *
     * @param zipFilePath ZIP 文件路径
     * @param sourceDir   源文件夹
     * @param target      压缩文件输出路径
     * @return 根条目名
     * @throws IOException IO 异常
     */
    private static @NotNull String getRootEntryName(String zipFilePath, @NotNull Path sourceDir, @NotNull Path target) throws IOException {
        Path normalizedSource = sourceDir.toAbsolutePath().normalize();
        if (target.equals(normalizedSource) || target.startsWith(normalizedSource)) {
            throw new IOException("压缩文件不能输出到源文件夹内部: " + zipFilePath);
        }
        Path sourceName = sourceDir.getFileName();
        return Objects.isNull(sourceName) ? ZIP_SEPARATOR : sourceName.toString();
    }

    /**
     * 递归压缩目录
     *
     * @param dir     要压缩的目录
     * @param dirName 当前目录名称
     * @param zos     ZIP 输出流
     * @param written 累计写出字节数的累加器（单元素数组，供递归回写）
     * @param depth   当前递归深度
     * @throws IOException IO 异常，超出上限或深度上限时同样以 IOException 形式抛出
     */
    private static void zipDirectory(Path dir, String dirName, @NotNull ZipOutputStream zos,
                                     long @NotNull [] written, int depth) throws IOException {
        // 递归深度守卫：每层递归要压多个栈帧（lambda、DirectoryStream、局部变量），
        // 病态深的目录树会在默认栈大小下 StackOverflowError
        if (depth > MAX_ZIP_DEPTH) {
            throw new IOException("目录层级超过上限 " + MAX_ZIP_DEPTH + "，已中止压缩: " + dir);
        }
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
                    zipDirectory(path, entryName, zos, written, depth + 1);
                    continue;
                }
                // 添加文件条目
                ZipEntry fileEntry = new ZipEntry(entryName);
                zos.putNextEntry(fileEntry);
                try (InputStream bis = new BufferedInputStream(new FileInputStream(path.toFile()))) {
                    byte[] chunk = new byte[ZIP_COPY_CHUNK];
                    int read;
                    while ((read = bis.read(chunk)) > 0) {
                        // 真正执行上限：写之前就累加并比对，超限立刻中止，
                        // 避免「压缩写爆磁盘」这种不可逆的后果
                        written[0] += read;
                        if (written[0] > MAX_ZIP_TOTAL_BYTES) {
                            throw new IOException("压缩输出超过上限 " + MAX_ZIP_TOTAL_BYTES + " 字节，已中止");
                        }
                        zos.write(chunk, 0, read);
                    }
                }
                zos.closeEntry();
            }
        }
    }

    /**
     * 校验并解析待写入的目标文件路径
     *
     * @param absoluteDirectory 目录绝对路径
     * @param fileName          文件名
     * @return 规范化后的目标路径
     */
    private static @NotNull Path resolveSaveTarget(@NotNull String absoluteDirectory, @NotNull String fileName) {
        // 必须是安全的绝对根目录：base 为 "/" 时下面 startsWith 的穿越校验会恒真
        Path base = assertSafeBaseDirectory(absoluteDirectory);
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
        //noinspection AlibabaUndefineMagicConstant
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
}
