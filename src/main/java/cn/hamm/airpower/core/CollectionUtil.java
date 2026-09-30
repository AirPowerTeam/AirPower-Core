package cn.hamm.airpower.core;

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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * <h1>内置的集合工具类</h1>
 *
 * @author Hamm.cn
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
     * @return InputStream
     */
    @Contract("_, _ -> new")
    public static <M extends RootModel<M>> @NotNull InputStream toCsvInputStream(List<M> list, Class<M> itemClass) {
        return toCsvInputStream(itemClass, (fieldList) -> getCsvValueList(list, fieldList));
    }

    /**
     * 将集合转换为 CSV 文件流
     *
     * @param itemClass         元素的类名
     * @param valueListFunction 列数据列表函数
     * @param <M>               元素类型
     * @return InputStream
     */
    @Contract("_, _ -> new")
    private static <M extends RootModel<M>> @NotNull InputStream toCsvInputStream(Class<M> itemClass, @NotNull Function<List<Field>, List<String>> valueListFunction) {
        List<Field> fieldList = getExportFieldList(itemClass);
        List<String> rowList = getCsvHeaderList(fieldList);
        List<String> valueList = valueListFunction.apply(fieldList);
        rowList.addAll(valueList);
        return new ByteArrayInputStream(String.join(CSV_ROW_DELIMITER, rowList).getBytes(StandardCharsets.UTF_8));
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
        List<String> rowList = new ArrayList<>();
        for (M entity : list) {
            if (Objects.isNull(entity)) {
                // 集合中的 null 元素直接跳过，不让整份导出因一条脏数据失败
                continue;
            }
            List<String> columnList = new ArrayList<>();
            for (Field field : fieldList) {
                Object value = getCsvColumnValue(entity, field);
                columnList.add(value.toString());
            }
            rowList.add(String.join(CSV_COLUMN_DELIMITER, columnList));
        }
        return rowList;
    }

    /**
     * 转义 CSV 单元格中的分隔符与换行
     *
     * @param cell 单元格内容
     * @return 转义后的内容
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
        // 添加表头
        rowList.add(String.join(CSV_COLUMN_DELIMITER, fieldList.stream().map(ReflectUtil::getDescription).toList()));
        return rowList;
    }

    /**
     * 获取导出字段列表
     *
     * @param itemClass 类
     * @param <M>       元素类型
     * @return 字段列表
     */
    public static <M extends RootModel<M>> @Unmodifiable @NotNull List<Field> getExportFieldList(Class<M> itemClass) {
        //noinspection unchecked
        return EXPORT_FIELD_CACHE.computeIfAbsent(itemClass, clazz -> buildExportFieldList((Class<M>) clazz));
    }

    private static <M extends RootModel<M>> @UnmodifiableView @NotNull List<Field> buildExportFieldList(Class<M> itemClass) {
        List<CsvField> fieldList = new ArrayList<>();
        for (Field field : ReflectUtil.getFieldList(itemClass)) {
            Export export = null;
            // 判断 Getter 是否被标记
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
     * CSV列
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
