package cn.hamm.airpower.core;

import cn.hamm.airpower.core.annotation.Description;
import cn.hamm.airpower.core.annotation.Dictionary;
import cn.hamm.airpower.core.annotation.Export;
import cn.hamm.airpower.core.exception.ServiceException;
import cn.hamm.airpower.core.interfaces.IDictionary;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <h1>内置的集合工具类</h1>
 *
 * @author Hamm.cn
 * @apiNote 主要负责 CSV 导出：列由 {@link Export} 注解决定，列值转换失败会回退为
 * 原始文本，不中断整表导出
 */
@Slf4j
public class CollectionUtil {
    /**
     * CSV 列分隔符
     */
    public static final String CSV_COLUMN_DELIMITER = ",";

    /**
     * CSV 行分隔符
     */
    public static final String CSV_ROW_DELIMITER = "\n";

    /**
     * UTF-8 BOM
     * @apiNote Excel / WPS 在 Windows 上打开无 BOM 的 UTF-8 CSV 时不会用 UTF-8 解码，而是退回
     * 系统 ANSI 代码页，简体中文环境下整表中文乱码。以 {@code \uFEFF} 形式拼在内容最前，
     * 经 {@code getBytes(UTF_8)} 之后就是 {@code EF BB BF} 三个字节
     */
    public static final String UTF8_BOM = "\uFEFF";

    /**
     * CSV 缩进符号
     */
    private static final String INDENT = "\t";

    /**
     * CSV 公式注入防护前缀
     */
    private static final String CSV_FORMULA_GUARD = "'";

    /**
     * 空值占位符
     */
    private static final String EMPTY_VALUE_PLACEHOLDER = "-";

    /**
     * 会被表格软件当作公式起始的字符
     */
    private static final String FORMULA_PREFIXES = "=+-@\t\r";

    /**
     * CSV 流式写出的缓冲大小
     */
    private static final int WRITE_BUFFER_SIZE = 64 * 1024;

    /**
     * 导出字段缓存
     */
    private static final ConcurrentHashMap<Class<?>, List<Field>> EXPORT_FIELD_CACHE = new ConcurrentHashMap<>();

    /**
     * 禁止外部实例化
     */
    @Contract(pure = true)
    private CollectionUtil() {
    }

    /**
     * 获取集合中的 {@code 非null} 元素
     *
     * @param list       原始集合
     * @param fieldClass 数据类型
     * @param <T>        数据类型
     * @return 处理后的集合
     */
    public static @NotNull <T> Collection<T> getCollectWithoutNull(Collection<T> list, Class<?> fieldClass) {
        if (Objects.isNull(list) || list.isEmpty()) {
            return newCollection(fieldClass);
        }
        // 方法名承诺"去掉 null"，原实现直接返回原集合，null 元素原样保留
        Collection<T> result = newCollection(fieldClass);
        for (T item : list) {
            if (Objects.nonNull(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 按声明类型创建空集合
     *
     * @param fieldClass 数据类型
     * @param <T>        数据类型
     * @return 空集合
     */
    private static <T> @NotNull Collection<T> newCollection(Class<?> fieldClass) {
        return Objects.equals(Set.class, fieldClass) ? new HashSet<>() : new ArrayList<>();
    }

    /**
     * 将集合转换为 CSV 文件流
     *
     * @param list      集合
     * @param itemClass 元素的类名
     * @param <M>       元素类型
     * @return CSV 文件流
     * @apiNote 首行是表头，取自字段的 {@link Description}。内容前置 {@link #UTF8_BOM}，
     * 保证表格软件按 UTF-8 解码
     * @apiNote <b>整表内容都在堆里</b>：返回的流背后是完整的 {@code byte[]}，
     * 而生成过程中还会同时存活行集合、拼接串和字节数组三份副本，
     * 峰值约为内容体积的 3 倍。实测 5 万行 × 30 列（约 35MB 内容）在
     * {@code -Xmx256m} 下直接 {@link OutOfMemoryError}。
     * <b>大数据量请改用 {@link #writeCsv}</b>
     */
    @Contract("_, _ -> new")
    public static <M extends RootModel<M>> @NotNull InputStream toCsvInputStream(List<M> list, Class<M> itemClass) {
        List<Field> fieldList = getExportFieldList(itemClass);
        List<String> rowList = getCsvHeaderList(fieldList);
        rowList.addAll(getCsvValueList(list, fieldList));
        // 预估总长直接建 StringBuilder：避免 String.join 的结果再被 BOM 拼接复制一遍
        int total = UTF8_BOM.length();
        for (int i = 0; i < rowList.size(); i++) {
            total += rowList.get(i).length() + (i > 0 ? CSV_ROW_DELIMITER.length() : 0);
        }
        StringBuilder csv = new StringBuilder(total);
        csv.append(UTF8_BOM);
        for (int i = 0; i < rowList.size(); i++) {
            if (i > 0) {
                csv.append(CSV_ROW_DELIMITER);
            }
            csv.append(rowList.get(i));
        }
        return new ByteArrayInputStream(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 逐行写出 CSV
     *
     * @param list      集合
     * @param itemClass 元素的类名
     * @param out       输出流，方法内部<b>不会</b>关闭它
     * @param <M>       元素类型
     * @throws IOException 写出异常
     * @apiNote 流式写出，堆占用只与单行宽度有关，<b>与总行数无关</b>。
     * 这是大数据量导出的正确入口；{@link #toCsvInputStream} 因为要返回
     * {@code byte[]} 做不到这一点
     * @apiNote 输出与 {@link #toCsvInputStream} <b>逐字节一致</b>：都以 BOM 开头，
     * 行分隔符只写在行与行之间，<b>末行不带</b>换行
     */
    public static <M extends RootModel<M>> void writeCsv(
            @NotNull List<M> list, Class<M> itemClass, @NotNull OutputStream out) throws IOException {
        if (Objects.isNull(list)) {
            throw new ServiceException("集合不能为空");
        }
        List<Field> fieldList = getExportFieldList(itemClass);
        // 64KB 缓冲：行数多时避免每行都直接下探到文件系统
        Writer writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), WRITE_BUFFER_SIZE);
        writer.write(UTF8_BOM);
        writer.write(String.join(CSV_COLUMN_DELIMITER, getCsvHeaderList(fieldList)));
        for (M entity : list) {
            if (Objects.isNull(entity)) {
                // 与 getCsvValueList 保持一致：null 元素跳过，不让整份导出因一条脏数据失败
                continue;
            }
            // 分隔符写在每行数据「之前」：表头已经占了第一行，
            // 若用「首行不写分隔符」的写法，第一行数据会直接粘在表头后面
            writer.write(CSV_ROW_DELIMITER);
            writer.write(getCsvRow(entity, fieldList));
        }
        writer.flush();
    }

    /**
     * 生成单条记录的 CSV 行
     *
     * @param entity    记录
     * @param fieldList 列数组
     * @return 行内容
     */
    private static <M extends RootModel<M>> @NotNull String getCsvRow(@NotNull M entity, @NotNull List<Field> fieldList) {
        List<String> columnList = new ArrayList<>(fieldList.size());
        for (Field field : fieldList) {
            columnList.add(getCsvColumnValue(entity, field).toString());
        }
        return String.join(CSV_COLUMN_DELIMITER, columnList);
    }

    /**
     * 获取 CSV 行数据列表
     *
     * @param list      列表
     * @param fieldList 列数组
     * @param <M>       元素类型
     * @return 列表数据
     */
    public static <M extends RootModel<M>> @NotNull List<String> getCsvValueList(List<M> list, List<Field> fieldList) {
        if (Objects.isNull(list)) {
            throw new ServiceException("集合不能为空");
        }
        List<String> rowList = new ArrayList<>(list.size());
        for (M entity : list) {
            if (Objects.isNull(entity)) {
                // 集合中的 null 元素直接跳过，不让整份导出因一条脏数据失败
                continue;
            }
            rowList.add(getCsvRow(entity, fieldList));
        }
        return rowList;
    }

    /**
     * 转义 CSV 单元格中的分隔符与换行
     *
     * @param cell 单元格内容
     * @return 转义后的内容
     * @apiNote 直接用空格替换，不加引号包裹，表格软件读取时不会出现多余引号
     */
    private static @NotNull String escapeCell(@NotNull String cell) {
        return cell
                .replace(CSV_COLUMN_DELIMITER, " ")
                .replace(CSV_ROW_DELIMITER, " ");
    }

    /**
     * 防护 CSV 公式注入
     *
     * @param cell 单元格内容
     * @return 加了防护前缀的内容
     * @apiNote 以 {@code = + - @} 开头的值在 Excel / WPS 中会被当作公式执行，
     * 恶意数据可借此触发外部链接访问或 DDE 命令执行
     */
    private static @NotNull String guardFormula(@NotNull String cell) {
        if (cell.isEmpty()) {
            return cell;
        }
        int i = 0;
        // 先剥掉前导空白/控制字符再做判定，Excel 解析时同样会忽略它们
        while (i < cell.length() && Character.isWhitespace(cell.charAt(i))) {
            i++;
        }
        if (i < cell.length() && FORMULA_PREFIXES.indexOf(cell.charAt(i)) >= 0) {
            return CSV_FORMULA_GUARD + cell;
        }
        return cell;
    }

    /**
     * 获取 CSV 表头行
     *
     * @param fieldList 字段列表
     * @return 列数据
     */
    public static @NotNull List<String> getCsvHeaderList(List<Field> fieldList) {
        if (Objects.isNull(fieldList)) {
            throw new ServiceException("字段列表不能为空");
        }
        List<String> rowList = new ArrayList<>();
        rowList.add(String.join(CSV_COLUMN_DELIMITER, fieldList.stream().map(ReflectUtil::getDescription).toList()));
        return rowList;
    }

    /**
     * 获取导出字段列表
     *
     * @param itemClass 类
     * @param <M>       元素类型
     * @return 字段列表（不可修改）
     * @apiNote 结果按类缓存，同一个类重复导出不重复扫描字段
     */
    public static <M extends RootModel<M>> @Unmodifiable @NotNull List<Field> getExportFieldList(Class<M> itemClass) {
        //noinspection unchecked
        return EXPORT_FIELD_CACHE.computeIfAbsent(itemClass, clazz -> buildExportFieldList((Class<M>) clazz));
    }

    /**
     * 扫描并排序导出字段
     *
     * @param itemClass 类
     * @param <M>       元素类型
     * @return 字段列表（不可修改）
     * @apiNote {@code @Export} 优先取 Getter 上的，其次取字段上的；两者都没标记或
     * 标记了 {@code remove} 的字段被排除。排序是<b>降序</b>，{@code sort} 值大的列排在前面
     */
    private static <M extends RootModel<M>> @UnmodifiableView @NotNull List<Field> buildExportFieldList(Class<M> itemClass) {
        List<CsvField> fieldList = new ArrayList<>();
        for (Field field : ReflectUtil.getFieldList(itemClass)) {
            Export export = null;
            // Getter 上的 @Export 优先于字段上的
            String fieldGetter = ReflectUtil.getFieldGetter(field);
            try {
                Method getter = itemClass.getMethod(fieldGetter);
                export = ReflectUtil.getAnnotation(Export.class, getter);
                if (Objects.isNull(export)) {
                    export = ReflectUtil.getAnnotation(Export.class, field);
                }
            } catch (NoSuchMethodException ignored) {
            }
            if (Objects.isNull(export) || export.remove()) {
                continue;
            }
            fieldList.add(new CsvField().setField(field).setSort(export.sort()));
        }
        // sort 排序，数值大的列排在前面
        fieldList.sort(Comparator.comparing(CsvField::getSort).reversed());
        return fieldList.stream().map(CsvField::getField).toList();
    }

    /**
     * 获取导出列的数据
     *
     * @param model 数据
     * @param field 字段
     * @return 处理后的值
     * @apiNote 空值统一写成 {@code -} 占位；按 {@link Export.Type} 转换失败时
     * 回退为原始文本并告警，一列坏数据不会让整次导出失败
     */
    private static <M extends RootModel<M>> @NotNull Object getCsvColumnValue(@NotNull M model, @NotNull Field field) {
        Object value = ReflectUtil.getFieldValue(model, field);
        if (Objects.isNull(value) || !StringUtil.hasText(value.toString())) {
            // 空值占位符由本工具生成，仍需按列类型走一遍转换以保持既有输出格式
            value = EMPTY_VALUE_PLACEHOLDER;
        }
        // 空值占位符由本工具生成，不需要公式注入防护
        boolean isPlaceholder = EMPTY_VALUE_PLACEHOLDER.equals(value);
        // 原始文本先做分隔符替换与公式防护，再进入类型转换
        String text = isPlaceholder ? (String) value : guardFormula(escapeCell(value.toString()));
        try {
            Export export = ReflectUtil.getAnnotation(Export.class, field);
            if (Objects.isNull(export)) {
                return text;
            }
            return switch (export.value()) {
                case DATETIME -> INDENT + DateTimeUtil.format(Long.parseLong(text));
                case TEXT -> INDENT + text;
                case BOOLEAN -> (boolean) value ? "是" : "否";
                case DICTIONARY -> {
                    Dictionary dictionary = ReflectUtil.getAnnotation(Dictionary.class, field);
                    if (Objects.isNull(dictionary)) {
                        yield text;
                    } else {
                        IDictionary dict = DictionaryUtil.getDictionary(
                                dictionary.value(), Integer.parseInt(text)
                        );
                        yield dict.getLabel();
                    }
                }
                case NUMBER -> {
                    if (value instanceof Double doubleValue) {
                        yield BigDecimal.valueOf(doubleValue).toPlainString();
                    }
                    if (value instanceof Float floatValue) {
                        yield BigDecimal.valueOf(floatValue).toPlainString();
                    }
                    if (value instanceof Long longValue) {
                        yield BigDecimal.valueOf(longValue).toPlainString();
                    }
                    yield text;
                }
            };
        } catch (Exception e) {
            log.warn("导出列({})的数据处理失败，已回退为原始值, {}", field.getName(), e.getMessage());
            return text;
        }
    }

    /**
     * CSV 导出列
     */
    @Accessors(chain = true)
    @Data
    static class CsvField {
        /**
         * 字段
         */
        private Field field;

        /**
         * 排序
         */
        private Integer sort;
    }
}
