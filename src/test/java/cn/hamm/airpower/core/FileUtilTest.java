package cn.hamm.airpower.core;

import cn.hamm.airpower.core.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>FileUtil 单元测试</h1>
 *
 * @author Hamm.cn
 */
class FileUtilTest {

    @TempDir
    Path tempDir;

    // ==================== getExtension 方法测试 ====================

    @Test
    void testGetExtensionWithNormalFile() {
        String result = FileUtil.getExtension("test.txt");
        assertEquals("txt", result);
    }

    @Test
    void testGetExtensionWithUpperCase() {
        String result = FileUtil.getExtension("test.TXT");
        assertEquals("txt", result);
    }

    @Test
    void testGetExtensionWithMixedCase() {
        String result = FileUtil.getExtension("test.TxT");
        assertEquals("txt", result);
    }

    @Test
    void testGetExtensionWithMultipleDots() {
        String result = FileUtil.getExtension("archive.tar.gz");
        assertEquals("gz", result);
    }

    @Test
    void testGetExtensionWithPath() {
        String result = FileUtil.getExtension("/path/to/file.txt");
        assertEquals("txt", result);
    }

    @Test
    void testGetExtensionWithNoExtension() {
        String result = FileUtil.getExtension("filename");
        assertEquals("filename", result);
    }

    @Test
    void testGetExtensionWithEmptyString() {
        String result = FileUtil.getExtension("");
        assertEquals("", result);
    }

    @Test
    void testGetExtensionWithDotOnly() {
        String result = FileUtil.getExtension(".");
        assertEquals("", result);
    }

    // ==================== formatSize 方法测试 ====================

    @Test
    void testFormatSizeWithBytes() {
        String result = FileUtil.formatSize(512);
        assertEquals("512.00B", result);
    }

    @Test
    void testFormatSizeWithKB() {
        String result = FileUtil.formatSize(1536);
        assertEquals("1.50KB", result);
    }

    @Test
    void testFormatSizeWithMB() {
        String result = FileUtil.formatSize(2 * 1024 * 1024);
        assertEquals("2.00MB", result);
    }

    @Test
    void testFormatSizeWithGB() {
        String result = FileUtil.formatSize(3L * 1024 * 1024 * 1024);
        assertEquals("3.00GB", result);
    }

    @Test
    void testFormatSizeWithTB() {
        String result = FileUtil.formatSize(4L * 1024 * 1024 * 1024 * 1024);
        assertEquals("4.00TB", result);
    }

    @Test
    void testFormatSizeWithExactlyOneKB() {
        String result = FileUtil.formatSize(1024);
        assertEquals("1.00KB", result);
    }

    @Test
    void testFormatSizeWithZero() {
        ServiceException exception = assertThrows(ServiceException.class, () ->
                FileUtil.formatSize(0)
        );
        assertTrue(exception.getMessage().contains("错误的文件大小"));
    }

    @Test
    void testFormatSizeWithNegative() {
        assertThrows(ServiceException.class, () ->
                FileUtil.formatSize(-1)
        );
    }

    @Test
    void testFormatSizeWithVeryLargeNumber() {
        // Long.MAX_VALUE 约 8EB，在 UNITS 范围内（B, KB, MB, GB, TB, PB, EB, ZB, YB）
        // 8EB 转换为 EB 单位后约为 8.00EB，不会溢出
        String result = FileUtil.formatSize(Long.MAX_VALUE);
        assertNotNull(result);
        assertTrue(result.endsWith("EB"));
    }

    // ==================== createDirectories 方法测试 ====================

    @Test
    void testCreateDirectoriesWithNewDirectory() {
        String newDir = tempDir.resolve("new_folder").toString();
        FileUtil.createDirectories(newDir);
        assertTrue(Files.exists(Paths.get(newDir)));
    }

    @Test
    void testCreateDirectoriesWithNestedDirectories() {
        String nestedDir = tempDir.resolve("a/b/c").toString();
        FileUtil.createDirectories(nestedDir);
        assertTrue(Files.exists(Paths.get(nestedDir)));
    }

    @Test
    void testCreateDirectoriesWithExistingDirectory() {
        String existingDir = tempDir.toString();
        // 不应该抛出异常
        assertDoesNotThrow(() -> FileUtil.createDirectories(existingDir));
    }

    // ==================== getTodayDirectory 方法测试 ====================

    @Test
    void testGetTodayDirectory() {
        String result = FileUtil.getTodayDirectory();
        assertNotNull(result);
        assertTrue(result.endsWith(File.separator));
        // 格式应该是 yyyyMMdd/
        assertEquals(9, result.length()); // 8位日期 + 1位分隔符
        assertTrue(result.matches("\\d{8}" + File.separator));
    }

    // ==================== formatDirectory 方法测试 ====================

    @Test
    void testFormatDirectoryWithoutSeparator() {
        String result = FileUtil.formatDirectory("/path/to/dir");
        assertEquals("/path/to/dir" + File.separator, result);
    }

    @Test
    void testFormatDirectoryWithSeparator() {
        String result = FileUtil.formatDirectory("/path/to/dir/");
        assertEquals("/path/to/dir/", result);
    }

    @Test
    void testFormatDirectoryWithBackslash() {
        String result = FileUtil.formatDirectory("\\path\\to\\dir");
        assertEquals("\\path\\to\\dir" + File.separator, result);
    }

    @Test
    void testFormatDirectoryWithEmptyString() {
        String result = FileUtil.formatDirectory("");
        assertEquals(File.separator, result);
    }

    // ==================== saveFile 方法测试（字节数组版本） ====================

    @Test
    void testSaveFileWithBytes() throws IOException {
        String fileName = "test.txt";
        byte[] content = "Hello World".getBytes(StandardCharsets.UTF_8);
        FileUtil.saveFile(tempDir.toString(), fileName, content);

        Path savedFile = tempDir.resolve(fileName);
        assertTrue(Files.exists(savedFile));
        assertEquals("Hello World", Files.readString(savedFile));
    }

    @Test
    void testSaveFileWithBytesAndCreateDirectory() {
        String subDir = tempDir.resolve("sub").toString();
        String fileName = "test.txt";
        byte[] content = "Hello".getBytes(StandardCharsets.UTF_8);
        FileUtil.saveFile(subDir, fileName, content);

        assertTrue(Files.exists(Paths.get(subDir, fileName)));
    }

    @Test
    void testSaveFileWithBytesOverwrite() throws IOException {
        String fileName = "overwrite.txt";
        Path filePath = tempDir.resolve(fileName);
        Files.writeString(filePath, "Old Content");

        byte[] newContent = "New Content".getBytes(StandardCharsets.UTF_8);
        FileUtil.saveFile(tempDir.toString(), fileName, newContent, StandardOpenOption.TRUNCATE_EXISTING);

        assertEquals("New Content", Files.readString(filePath));
    }

    // ==================== saveFile 方法测试（字符串版本） ====================

    @Test
    void testSaveFileWithString() throws IOException {
        String fileName = "string_test.txt";
        String content = "String Content 中文";
        FileUtil.saveFile(tempDir.toString(), fileName, content);

        Path savedFile = tempDir.resolve(fileName);
        assertTrue(Files.exists(savedFile));
        assertEquals(content, Files.readString(savedFile, StandardCharsets.UTF_8));
    }

    @Test
    void testSaveFileWithEmptyString() throws IOException {
        String fileName = "empty.txt";
        String content = "";
        FileUtil.saveFile(tempDir.toString(), fileName, content);

        Path savedFile = tempDir.resolve(fileName);
        assertTrue(Files.exists(savedFile));
        assertEquals(0, Files.size(savedFile));
    }

    // ==================== zip 方法测试 ====================

    @Test
    void testZipWithExistingDirectory() throws IOException {
        // 创建源目录和文件
        Path sourceDir = tempDir.resolve("source");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("file1.txt"), "Content 1");
        Files.writeString(sourceDir.resolve("file2.txt"), "Content 2");

        Path zipFile = tempDir.resolve("output.zip");
        FileUtil.zip(sourceDir.toString(), zipFile.toString());

        assertTrue(Files.exists(zipFile));
        assertTrue(Files.size(zipFile) > 0);
    }

    @Test
    void testZipWithNestedDirectories() throws IOException {
        // 创建嵌套目录结构
        Path sourceDir = tempDir.resolve("source");
        Path subDir = sourceDir.resolve("sub");
        Files.createDirectories(subDir);
        Files.writeString(sourceDir.resolve("root.txt"), "Root");
        Files.writeString(subDir.resolve("nested.txt"), "Nested");

        Path zipFile = tempDir.resolve("nested.zip");
        FileUtil.zip(sourceDir.toString(), zipFile.toString());

        assertTrue(Files.exists(zipFile));
        assertTrue(Files.size(zipFile) > 0);
    }

    @Test
    void testZipWithNonExistingDirectory() {
        Path zipFile = tempDir.resolve("error.zip");
        assertThrows(IOException.class, () ->
                FileUtil.zip("/non/existing/path", zipFile.toString())
        );
    }

    @Test
    void testZipWithEmptyDirectory() throws IOException {
        Path sourceDir = tempDir.resolve("empty_source");
        Files.createDirectories(sourceDir);

        Path zipFile = tempDir.resolve("empty.zip");
        FileUtil.zip(sourceDir.toString(), zipFile.toString());

        assertTrue(Files.exists(zipFile));
    }

    // ==================== deleteDirectory 方法测试 ====================

    @Test
    void testDeleteDirectoryWithExistingDirectory() throws IOException {
        Path dirToDelete = tempDir.resolve("to_delete");
        Files.createDirectories(dirToDelete);
        Files.writeString(dirToDelete.resolve("file.txt"), "Content");

        FileUtil.deleteDirectory(dirToDelete.toString());
        assertFalse(Files.exists(dirToDelete));
    }

    @Test
    void testDeleteDirectoryWithNestedStructure() throws IOException {
        Path rootDir = tempDir.resolve("nested_delete");
        Path subDir = rootDir.resolve("level1/level2");
        Files.createDirectories(subDir);
        Files.writeString(subDir.resolve("deep.txt"), "Deep");
        Files.writeString(rootDir.resolve("shallow.txt"), "Shallow");

        FileUtil.deleteDirectory(rootDir.toString());
        assertFalse(Files.exists(rootDir));
    }

    @Test
    void testDeleteDirectoryWithNonExistingDirectory() {
        // 删除不存在的目录不应该抛出异常
        assertDoesNotThrow(() ->
                FileUtil.deleteDirectory(tempDir.resolve("non_existing").toString())
        );
    }

    // ==================== 常量测试 ====================

    @Test
    void testConstants() {
        assertEquals(1024L, FileUtil.FILE_SCALE);
        assertEquals(".", FileUtil.EXTENSION_SEPARATOR);
        assertEquals(9, FileUtil.UNITS.length);
        assertEquals("B", FileUtil.UNITS[0]);
        assertEquals("KB", FileUtil.UNITS[1]);
        assertEquals("MB", FileUtil.UNITS[2]);
        assertEquals("GB", FileUtil.UNITS[3]);
        assertEquals("TB", FileUtil.UNITS[4]);
        assertEquals("PB", FileUtil.UNITS[5]);
        assertEquals("EB", FileUtil.UNITS[6]);
        assertEquals("ZB", FileUtil.UNITS[7]);
        assertEquals("YB", FileUtil.UNITS[8]);
    }
}
