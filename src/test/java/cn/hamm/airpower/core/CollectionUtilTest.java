package cn.hamm.airpower.core;

import cn.hamm.airpower.core.annotation.Description;
import cn.hamm.airpower.core.annotation.Export;
import cn.hamm.airpower.core.exception.ServiceException;
import cn.hamm.airpower.core.fixture.ExportDemoModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>CollectionUtil 单元测试</h1>
 *
 * <p>本测试以<b>源码实际行为</b>为断言依据，刻意记录以下几处与注释/方法名不一致的行为：</p>
 * <ul>
 *     <li>{@code getCollectWithoutNull} 并不移除 null 元素，且只有 {@code Set.class} 会被特殊处理</li>
 *     <li>{@code getExportFieldList} 的排序注释写“从小到大”，实现却是 {@code Comparator.reversed()}（从大到小）</li>
 *     <li>{@code getCsvColumnValue} 内部吞掉所有异常，类型不匹配时静默回退为原值</li>
 * </ul>
 *
 * @author Hamm.cn
 */
@DisplayName("CollectionUtil 内置集合工具类单元测试")
class CollectionUtilTest {

    /**
     * 固定的时间戳（毫秒）
     */
    private static final long CREATE_TIME = 1700000000000L;

    /**
     * 期望的表头行（按 sort 倒序）
     */
    private static final String EXPECTED_HEADER =
            "比率,数量,金额,是否启用,创建时间,性别,姓名,主键,备注";

    /**
     * 期望的数据行（按 sort 倒序）
     */
    private static final String EXPECTED_ROW =
            "0.5,1000,12.5,是,\t" + DateTimeUtil.format(CREATE_TIME) + ",男,\tHamm,\t1,\t第一行 第二行 第三行";

    /**
     * 构造一个字段齐全的导出模型
     *
     * @return 导出模型
     */
    private static ExportDemoModel fullModel() {
        return new ExportDemoModel()
                .setId(1L)
                .setName("Hamm")
                .setGender(1)
                .setCreateTime(CREATE_TIME)
                .setEnabled(true)
                .setAmount(12.5D)
                .setCount(1000L)
                .setRatio(0.5F)
                .setRemark("第一行,第二行\n第三行")
                .setRemovedColumn("不该出现的列")
                .setNotExportColumn("不该出现的列");
    }

    /**
     * 读取输入流的全部内容
     *
     * @param inputStream 输入流
     * @return UTF-8 字符串
     * @throws IOException 读取异常
     */
    /**
     * 按 UTF-8 读取 CSV 内容，并剥掉开头的 UTF-8 BOM
     *
     * @param inputStream 文件流
     * @return 去掉 BOM 的 CSV 文本
     * @throws IOException 读取异常
     * @apiNote 与表格软件的实际行为一致：BOM 只是编码标记，不属于表头内容。
     * BOM 本身的存在由 {@code toCsvInputStream} 的字节级用例单独断言
     */
    private static String readUtf8(InputStream inputStream) throws IOException {
        String csv = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        return csv.startsWith(CollectionUtil.UTF8_BOM) ? csv.substring(CollectionUtil.UTF8_BOM.length()) : csv;
    }

    /**
     * <h1>覆盖 @Export.Type 边界分支的辅助导出模型</h1>
     *
     * <p>仅用于验证“类型不匹配 / 缺少 @Dictionary”等分支，均被源码的异常吞噬逻辑回退为原值。</p>
     */
    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    static class BranchModel extends RootModel<BranchModel> {
        /**
         * 纯文本列
         */
        @Description("纯文本")
        @Export(value = Export.Type.TEXT, sort = 1)
        private String plainText;

        /**
         * 非时间戳的时间列
         */
        @Description("非时间戳")
        @Export(value = Export.Type.DATETIME, sort = 2)
        private String datetimeAsString;

        /**
         * 非数字类型的数字列
         */
        @Description("非数字")
        @Export(value = Export.Type.NUMBER, sort = 3)
        private String numberAsString;

        /**
         * 未标记 {@code @Dictionary} 的字典列
         */
        @Description("无字典注解")
        @Export(value = Export.Type.DICTIONARY, sort = 4)
        private Integer dictionaryWithoutDictionary;

        /**
         * 未标记 {@code @Description} 的文本列
         */
        @Export(value = Export.Type.TEXT, sort = 0)
        private String noDescription;
    }

    @Nested
    @DisplayName("CSV 分隔符常量")
    class ConstantTest {

        @Test
        @DisplayName("正常路径：列分隔符为英文逗号")
        void testColumnDelimiter() {
            assertEquals(",", CollectionUtil.CSV_COLUMN_DELIMITER, "列分隔符应为英文逗号");
        }

        @Test
        @DisplayName("正常路径：行分隔符为换行符")
        void testRowDelimiter() {
            assertEquals("\n", CollectionUtil.CSV_ROW_DELIMITER, "行分隔符应为换行符");
        }
    }

    @Nested
    @DisplayName("writeCsv 逐行流式写出")
    class WriteCsvTest {

        /**
         * 收集 writeCsv 的输出字节
         *
         * @param list      数据
         * @param itemClass 元素类型
         * @return 输出字节
         * @throws IOException 写出异常
         */
        private static byte[] writeToBytes(List<ExportDemoModel> list, Class<ExportDemoModel> itemClass) throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CollectionUtil.writeCsv(list, itemClass, out);
            return out.toByteArray();
        }

        @Test
        @DisplayName("输出应与 toCsvInputStream 逐字节一致")
        void sameOutputAsToCsvInputStream() throws IOException {
            List<ExportDemoModel> list = List.of(fullModel(), fullModel().setId(2L).setName("李四"));

            assertArrayEquals(
                    CollectionUtil.toCsvInputStream(list, ExportDemoModel.class).readAllBytes(),
                    writeToBytes(list, ExportDemoModel.class),
                    "流式与一次性两个入口必须产出完全相同的字节，否则调用方无从选择");
        }

        @Test
        @DisplayName("堆占用不应随行数增长：5 万行不再需要把整表读进堆")
        void streamsWithoutBufferingWholeTable() throws IOException {
            // 5 万行 × 9 列约 5MB 内容。修复前 toCsvInputStream 在 -Xmx256m 下会 OOM
            List<ExportDemoModel> list = new ArrayList<>(50_000);
            for (int i = 0; i < 50_000; i++) {
                list.add(fullModel().setId((long) i));
            }

            RecordingOutputStream out = new RecordingOutputStream();
            CollectionUtil.writeCsv(list, ExportDemoModel.class, out);

            assertEquals(50_000, out.newlineCount(),
                    "一行表头加 5 万行数据共 5 万零一行，分隔符写在行与行之间，正好 5 万个换行");
            assertTrue(out.maxWriteSize <= 64 * 1024,
                    "单次写入不应超过缓冲区大小（实测 " + out.maxWriteSize
                            + "），说明确实是流式写出而不是整表拼好再一次性写");
            assertFalse(out.closed, "方法不应关闭调用方传入的流");
        }

        @Test
        @DisplayName("空集合只输出表头，且不产生多余的末尾换行")
        void emptyList() throws IOException {
            byte[] raw = writeToBytes(List.of(), ExportDemoModel.class);

            assertEquals(CollectionUtil.UTF8_BOM + EXPECTED_HEADER, new String(raw, StandardCharsets.UTF_8),
                    "空集合应只有表头行");
            assertFalse(new String(raw, StandardCharsets.UTF_8).endsWith(CollectionUtil.CSV_ROW_DELIMITER),
                    "末行不应带换行，与 toCsvInputStream 保持一致");
        }

        @Test
        @DisplayName("集合中的 null 元素应跳过，不影响其余行")
        void skipsNullElements() throws IOException {
            List<ExportDemoModel> list = Arrays.asList(fullModel(), null, fullModel().setId(3L));

            String csv = new String(writeToBytes(list, ExportDemoModel.class), StandardCharsets.UTF_8);
            assertEquals(3, csv.split(CollectionUtil.CSV_ROW_DELIMITER).length,
                    "null 元素应被跳过，只输出一行表头加两行数据");
        }

        @Test
        @DisplayName("list 为 null 时抛 ServiceException")
        void nullList() {
            assertThrows(ServiceException.class,
                    () -> CollectionUtil.writeCsv(null, ExportDemoModel.class, new ByteArrayOutputStream()),
                    "与 getCsvValueList 保持一致：集合为 null 应抛业务异常而不是 NPE");
        }
    }

    /**
     * <h1>记录单次写入长度的输出流</h1>
     *
     * <p>用于断言「确实是流式写出」：整表拼好再一次性写出的实现，单次
     * {@code write} 的长度会等于整个文件的体积；流式实现的单次写入不会超过缓冲区。</p>
     */
    private static final class RecordingOutputStream extends ByteArrayOutputStream {
        /**
         * 单次 write 的最大字节数
         */
        private int maxWriteSize;

        /**
         * 是否被关闭
         */
        private boolean closed;

        /**
         * 统计已写出的换行符个数
         *
         * @return 换行符个数
         */
        private int newlineCount() {
            int count = 0;
            for (byte b : toByteArray()) {
                if (b == '\n') {
                    count++;
                }
            }
            return count;
        }

        @Override
        public synchronized void write(int b) {
            maxWriteSize = Math.max(maxWriteSize, 1);
            super.write(b);
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) {
            maxWriteSize = Math.max(maxWriteSize, len);
            super.write(b, off, len);
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    @Nested
    @DisplayName("getCollectWithoutNull 获取集合中的非 null 元素")
    class GetCollectWithoutNullTest {

        @Test
        @DisplayName("边界值：fieldClass 为 Set.class 且集合为 null 时返回空 HashSet")
        void testNullWithSetClass() {
            Collection<String> result = CollectionUtil.getCollectWithoutNull(null, Set.class);

            assertInstanceOf(HashSet.class, result, "fieldClass 为 Set.class 时应返回 HashSet");
            assertTrue(result.isEmpty(), "入参为 null 时应返回空集合");
        }

        @Test
        @DisplayName("边界值：fieldClass 非 Set 且集合为 null 时返回空 ArrayList")
        void testNullWithListClass() {
            Collection<String> result = CollectionUtil.getCollectWithoutNull(null, List.class);

            assertInstanceOf(ArrayList.class, result, "fieldClass 非 Set 时应返回 ArrayList");
            assertTrue(result.isEmpty(), "入参为 null 时应返回空集合");
        }

        @Test
        @DisplayName("边界值：fieldClass 为 null 时返回空 ArrayList")
        void testNullFieldClass() {
            Collection<String> result = CollectionUtil.getCollectWithoutNull(null, null);

            assertInstanceOf(ArrayList.class, result, "fieldClass 为 null 时走默认分支，返回 ArrayList");
            assertTrue(result.isEmpty(), "入参为 null 时应返回空集合");
        }

        @Test
        @DisplayName("正常路径：fieldClass 为 Set.class 时返回 Set 类型的集合")
        void testSetTypeIsHonored() {
            Set<String> source = new LinkedHashSet<>(List.of("a", "b"));

            Collection<String> result = CollectionUtil.getCollectWithoutNull(source, Set.class);

            assertInstanceOf(HashSet.class, result, "fieldClass 为 Set.class 时应返回 Set 类型");
            assertEquals(2, result.size(), "元素个数不变");
        }

        @Test
        @DisplayName("正常路径：fieldClass 非 Set 时返回 List 类型的集合")
        void testListTypeIsHonored() {
            List<String> source = List.of("a", "b");

            Collection<String> result = CollectionUtil.getCollectWithoutNull(source, List.class);

            assertInstanceOf(ArrayList.class, result, "fieldClass 非 Set 时应返回 List 类型");
            assertEquals(List.of("a", "b"), new ArrayList<>(result), "元素顺序与内容保持一致");
        }

        @Test
        @DisplayName("边界值：空的入参集合返回同类型空集合")
        void testEmptyCollection() {
            List<String> source = new ArrayList<>();

            Collection<String> result = CollectionUtil.getCollectWithoutNull(source, List.class);

            assertInstanceOf(ArrayList.class, result, "空集合应返回同类型的空集合");
            assertTrue(result.isEmpty(), "空集合返回后仍为空");
        }

        @Test
        @DisplayName("正常路径：null 元素被真正移除")
        void testNullElementRemoved() {
            Set<String> source = new LinkedHashSet<>(Arrays.asList("a", null, "b"));

            Collection<String> result = CollectionUtil.getCollectWithoutNull(source, Set.class);

            // 方法名承诺去掉 null，原实现直接返回原集合，null 元素原样保留
            assertEquals(2, result.size(), "null 元素应被移除");
            assertFalse(result.contains(null), "结果中不应再含有 null 元素");
            assertEquals(Set.of("a", "b"), new HashSet<>(result), "非 null 元素应全部保留");
        }

        @Test
        @DisplayName("边界值：集合中全为 null 时返回空集合")
        void testAllNullElements() {
            List<String> source = new ArrayList<>(Arrays.asList(null, null));

            Collection<String> result = CollectionUtil.getCollectWithoutNull(source, List.class);

            assertTrue(result.isEmpty(), "全为 null 的集合过滤后应为空集合");
        }

        @Test
        @DisplayName("边界值：结果集合与入参集合相互独立")
        void testDoesNotMutateSource() {
            List<String> source = new ArrayList<>(Arrays.asList("a", null));

            Collection<String> result = CollectionUtil.getCollectWithoutNull(source, List.class);

            // 过滤不应改动原入参集合
            assertEquals(2, source.size(), "原入参集合仍应保留全部元素（含 null）");
            // 结果是独立的新集合，改动它不影响入参
            result.add("b");
            assertEquals(2, source.size(), "修改结果集合不应影响原入参集合");
        }

        @Test
        @DisplayName("fieldClass 为 Set 的实现类时也应走 Set 分支")
        void testSubClassOfSet() {
            Collection<String> result = CollectionUtil.getCollectWithoutNull(null, HashSet.class);

            assertInstanceOf(HashSet.class, result,
                    "调用方传的是元素的运行时实际类（AirPower4J 传 data.getClass()），"
                            + "HashSet / TreeSet / Hibernate 的 PersistentSet 都不等于 Set.class。"
                            + "严格相等会让它们全部落到 ArrayList 分支，Set 的去重语义丢失且无告警");
        }

        @Test
        @DisplayName("Set 分支应真正去重")
        void testSetBranchKeepsDeduplication() {
            Collection<String> result = CollectionUtil.getCollectWithoutNull(
                    Arrays.asList("a", "a", "b", null), HashSet.class);

            assertInstanceOf(HashSet.class, result, "Set 实现应走 Set 分支");
            assertEquals(2, result.size(),
                    "Set 的去重语义必须保留：若退化成 ArrayList，重复项会原样出现在响应里");
        }

        @Test
        @DisplayName("TreeSet 等其它 Set 实现同样识别")
        void testOtherSetImplementations() {
            for (Class<?> setClass : List.of(TreeSet.class, LinkedHashSet.class, Set.class)) {
                assertInstanceOf(HashSet.class,
                        CollectionUtil.getCollectWithoutNull(null, setClass),
                        "Set 的所有实现都应被识别为 Set：" + setClass.getSimpleName());
            }
        }
    }

    @Nested
    @DisplayName("getExportFieldList 获取导出字段列表")
    class GetExportFieldListTest {

        @Test
        @DisplayName("正常路径：排除未标记 @Export 与 remove=true 的列")
        void testFieldNames() {
            List<Field> fieldList = CollectionUtil.getExportFieldList(ExportDemoModel.class);

            List<String> names = fieldList.stream().map(Field::getName).toList();
            assertFalse(names.contains("notExportColumn"), "未标记 @Export 的列不应出现在导出列表中");
            assertFalse(names.contains("removedColumn"), "标记了 @Export(remove = true) 的列不应出现在导出列表中");
            assertEquals(9, fieldList.size(), "ExportDemoModel 应导出 9 列");
        }

        @Test
        @DisplayName("当前行为：按 sort 倒序排列（与注释的从小到大相反）")
        void testSortOrder() {
            List<Field> fieldList = CollectionUtil.getExportFieldList(ExportDemoModel.class);

            List<String> names = fieldList.stream().map(Field::getName).toList();
            assertEquals(
                    List.of("ratio", "count", "amount", "enabled", "createTime", "gender", "name", "id", "remark"),
                    names,
                    "源码使用了 Comparator.reversed()，实际是 sort 从大到小，与注释“从小到大”相反");
        }

        @Test
        @DisplayName("缓存：重复获取返回同一个列表实例")
        void testCache() {
            List<Field> first = CollectionUtil.getExportFieldList(ExportDemoModel.class);
            List<Field> second = CollectionUtil.getExportFieldList(ExportDemoModel.class);

            assertSame(first, second, "导出字段列表被缓存，重复获取应返回同一实例");
        }

        @Test
        @DisplayName("边界值：返回的列表不可修改")
        void testUnmodifiable() {
            List<Field> fieldList = CollectionUtil.getExportFieldList(ExportDemoModel.class);

            assertThrows(UnsupportedOperationException.class,
                    () -> fieldList.add(null),
                    "返回的是 Stream.toList() 的不可变列表，添加元素应抛 UnsupportedOperationException");
        }

        @Test
        @DisplayName("异常分支：传入 null 类抛 NullPointerException")
        void testNullClass() {
            assertThrows(NullPointerException.class,
                    () -> CollectionUtil.getExportFieldList(null),
                    "ConcurrentHashMap 不接受 null 键，传入 null 应抛 NullPointerException");
        }
    }

    @Nested
    @DisplayName("getCsvHeaderList 获取 CSV 表头行")
    class GetCsvHeaderListTest {

        @Test
        @DisplayName("正常路径：按 @Description 拼接成一行")
        void testHeader() {
            List<Field> fieldList = CollectionUtil.getExportFieldList(ExportDemoModel.class);

            List<String> headerList = CollectionUtil.getCsvHeaderList(fieldList);

            assertEquals(1, headerList.size(), "表头应只有一行");
            assertEquals(EXPECTED_HEADER, headerList.get(0), "表头应按字段 @Description 用逗号拼接");
        }

        @Test
        @DisplayName("边界值：字段无 @Description 时使用字段名")
        void testHeaderWithoutDescription() {
            Field field = ReflectUtil.getField("noDescription", BranchModel.class);
            assertNotNull(field, "反射应能获取到 noDescription 字段");

            List<String> headerList = CollectionUtil.getCsvHeaderList(List.of(field));

            assertEquals(List.of("noDescription"), headerList, "无 @Description 的字段应回退为字段名");
        }

        @Test
        @DisplayName("边界值：空字段列表返回包含一个空串的列表")
        void testEmptyFieldList() {
            List<String> headerList = CollectionUtil.getCsvHeaderList(List.of());

            assertEquals(1, headerList.size(), "空字段列表仍会返回一行");
            assertEquals("", headerList.get(0), "空字段列表拼接结果应为空串");
        }

        @Test
        @DisplayName("异常分支：传入 null 抛 ServiceException")
        void testNullFieldList() {
            assertThrows(ServiceException.class,
                    () -> CollectionUtil.getCsvHeaderList(null),
                    "源码未对 fieldList 判空，传入 null 应抛 ServiceException");
        }
    }

    @Nested
    @DisplayName("getCsvValueList 获取 CSV 行数据列表")
    class GetCsvValueListTest {

        @Test
        @DisplayName("正常路径：各类列按 @Export.Type 转换")
        void testValueList() {
            List<Field> fieldList = CollectionUtil.getExportFieldList(ExportDemoModel.class);

            List<String> valueList = CollectionUtil.getCsvValueList(List.of(fullModel()), fieldList);

            assertEquals(1, valueList.size(), "一条数据应生成一行");
            assertEquals(EXPECTED_ROW, valueList.get(0), "各列应按 @Export.Type 与类型转换规则输出");
        }

        @Test
        @DisplayName("正常路径：多条数据每条一行")
        void testMultipleRows() {
            List<Field> fieldList = CollectionUtil.getExportFieldList(ExportDemoModel.class);
            List<ExportDemoModel> list = List.of(
                    fullModel().setId(2L).setName("张三"),
                    fullModel().setId(3L).setName("李四")
            );

            List<String> valueList = CollectionUtil.getCsvValueList(list, fieldList);

            assertEquals(2, valueList.size(), "两条数据应生成两行");
            assertTrue(valueList.get(0).contains("张三"), "第一行应包含第一条数据的姓名");
            assertTrue(valueList.get(1).contains("李四"), "第二行应包含第二条数据的姓名");
        }

        @Test
        @DisplayName("边界值：空集合返回空列表")
        void testEmptyList() {
            List<Field> fieldList = CollectionUtil.getExportFieldList(ExportDemoModel.class);

            List<ExportDemoModel> emptyList = List.of();
            List<String> valueList = CollectionUtil.getCsvValueList(emptyList, fieldList);

            assertNotNull(valueList, "返回值不应为 null");
            assertTrue(valueList.isEmpty(), "空集合应返回空列表");
        }

        @Test
        @DisplayName("边界值：空字段列表时每个元素生成一个空行")
        void testEmptyFieldList() {
            List<String> valueList = CollectionUtil.getCsvValueList(
                    List.of(fullModel(), fullModel()), List.of());

            assertEquals(2, valueList.size(), "字段列表为空时每个元素仍生成一行");
            assertEquals("", valueList.get(0), "无列时每行应为空串");
        }

        @Test
        @DisplayName("正常路径：null 值输出为横杠")
        void testNullValueAsDash() {
            Field field = ReflectUtil.getField("name", ExportDemoModel.class);
            assertNotNull(field, "反射应能获取到 name 字段");

            List<String> valueList = CollectionUtil.getCsvValueList(
                    List.of(new ExportDemoModel()), List.of(field));

            assertEquals(List.of("\t-"), valueList, "name 是 TEXT 列，字段值为 null 时输出带制表符前缀的横杠");
        }

        @Test
        @DisplayName("正常路径：空白字符串同样被替换为横杠")
        void testBlankValueAsDash() {
            Field field = ReflectUtil.getField("name", ExportDemoModel.class);
            assertNotNull(field, "反射应能获取到 name 字段");

            List<String> valueList = CollectionUtil.getCsvValueList(
                    List.of(new ExportDemoModel().setName("   ")), List.of(field));

            assertEquals(List.of("\t-"), valueList, "空白字符串不满足 hasText，TEXT 列输出带前缀的横杠");
        }

        @Test
        @DisplayName("正常路径：值中的逗号与换行被替换为空格")
        void testDelimiterReplaced() {
            Field field = ReflectUtil.getField("remark", ExportDemoModel.class);
            assertNotNull(field, "反射应能获取到 remark 字段");

            List<String> valueList = CollectionUtil.getCsvValueList(
                    List.of(new ExportDemoModel().setRemark("a,b\nc")), List.of(field));

            assertEquals(List.of("\ta b c"), valueList, "逗号与换行应被替换为空格，并保留 TEXT 列的制表符前缀");
        }

        @Test
        @DisplayName("当前行为：未标记 @Export 的字段原样输出")
        void testFieldWithoutExportAnnotation() {
            Field field = ReflectUtil.getField("notExportColumn", ExportDemoModel.class);
            assertNotNull(field, "反射应能获取到 notExportColumn 字段");

            List<String> valueList = CollectionUtil.getCsvValueList(
                    List.of(new ExportDemoModel().setNotExportColumn("原样输出")), List.of(field));

            assertEquals(List.of("原样输出"), valueList, "字段上没有 @Export 时应直接返回原值");
        }

        @Test
        @DisplayName("BOOLEAN 列：true 与 false 分别输出“是”“否”")
        void testBooleanColumn() {
            Field field = ReflectUtil.getField("enabled", ExportDemoModel.class);
            assertNotNull(field, "反射应能获取到 enabled 字段");

            List<String> valueList = CollectionUtil.getCsvValueList(
                    List.of(
                            new ExportDemoModel().setEnabled(true),
                            new ExportDemoModel().setEnabled(false)
                    ),
                    List.of(field));

            assertEquals(List.of("是", "否"), valueList, "布尔值应转换为中文的是/否");
        }

        @Test
        @DisplayName("当前缺陷：BOOLEAN 列为 null 时因强制类型转换异常被吞掉")
        void testBooleanNullColumn() {
            Field field = ReflectUtil.getField("enabled", ExportDemoModel.class);
            assertNotNull(field, "反射应能获取到 enabled 字段");

            List<String> valueList = CollectionUtil.getCsvValueList(
                    List.of(new ExportDemoModel()), List.of(field));

            assertEquals(List.of("-"), valueList, "null 值先被替换为横杠，强转 boolean 抛异常后被吞掉，最终仍输出横杠");
        }

        @Test
        @DisplayName("DATETIME 列：输出带制表符前缀的格式化时间")
        void testDateTimeColumn() {
            Field field = ReflectUtil.getField("createTime", ExportDemoModel.class);
            assertNotNull(field, "反射应能获取到 createTime 字段");

            List<String> valueList = CollectionUtil.getCsvValueList(
                    List.of(new ExportDemoModel().setCreateTime(CREATE_TIME)), List.of(field));

            assertEquals(List.of("\t" + DateTimeUtil.format(CREATE_TIME)), valueList,
                    "时间列应前置制表符并按默认模板格式化");
        }

        @Test
        @DisplayName("当前缺陷：DATETIME 列值为 null 时解析异常被吞掉")
        void testDateTimeNullColumn() {
            Field field = ReflectUtil.getField("createTime", ExportDemoModel.class);
            assertNotNull(field, "反射应能获取到 createTime 字段");

            List<String> valueList = CollectionUtil.getCsvValueList(List.of(new ExportDemoModel()), List.of(field));

            assertEquals(List.of("-"), valueList, "横杠无法被 Long.parseLong 解析，异常被吞后回退为原值横杠");
        }

        @Test
        @DisplayName("NUMBER 列：Double、Float、Long 均输出纯数字串")
        void testNumberColumn() {
            List<Field> fieldList = List.of(
                    ReflectUtil.getField("amount", ExportDemoModel.class),
                    ReflectUtil.getField("count", ExportDemoModel.class),
                    ReflectUtil.getField("ratio", ExportDemoModel.class)
            );

            List<String> valueList = CollectionUtil.getCsvValueList(
                    List.of(new ExportDemoModel().setAmount(12.5D).setCount(1000L).setRatio(0.5F)),
                    fieldList);

            assertEquals(List.of("12.5,1000,0.5"), valueList,
                    "数字列应通过 BigDecimal.valueOf().toPlainString() 转换，且 long 不会带上小数点");
        }

        @Test
        @DisplayName("边界值：NUMBER 列使用 toPlainString 而非科学计数法")
        void testNumberPlainString() {
            Field field = ReflectUtil.getField("amount", ExportDemoModel.class);
            assertNotNull(field, "反射应能获取到 amount 字段");

            List<String> valueList = CollectionUtil.getCsvValueList(
                    List.of(new ExportDemoModel().setAmount(0.00001D)), List.of(field));

            assertEquals(List.of("0.000010"), valueList,
                    "Double.toString 会输出 1.0E-5，源码使用 toPlainString 故应输出 0.000010（保留标度）");
        }

        @Test
        @DisplayName("当前缺陷：NUMBER 列为 null 时回退为横杠")
        void testNumberNullColumn() {
            Field field = ReflectUtil.getField("amount", ExportDemoModel.class);
            assertNotNull(field, "反射应能获取到 amount 字段");

            List<String> valueList = CollectionUtil.getCsvValueList(List.of(new ExportDemoModel()), List.of(field));

            assertEquals(List.of("-"), valueList, "null 值不会被任何 instanceof 命中，直接返回横杠");
        }

        @Test
        @DisplayName("DICTIONARY 列：按枚举字典输出描述")
        void testDictionaryColumn() {
            Field field = ReflectUtil.getField("gender", ExportDemoModel.class);
            assertNotNull(field, "反射应能获取到 gender 字段");

            List<String> valueList = CollectionUtil.getCsvValueList(
                    List.of(
                            new ExportDemoModel().setGender(0),
                            new ExportDemoModel().setGender(1),
                            new ExportDemoModel().setGender(2)
                    ),
                    List.of(field));

            assertEquals(List.of("未知", "男", "女"), valueList, "字典列应输出枚举的 label");
        }

        @Test
        @DisplayName("当前缺陷：字典中不存在的值异常被吞掉后回退为原值")
        void testDictionaryNotExistValue() {
            Field field = ReflectUtil.getField("gender", ExportDemoModel.class);
            assertNotNull(field, "反射应能获取到 gender 字段");

            List<String> valueList = CollectionUtil.getCsvValueList(
                    List.of(new ExportDemoModel().setGender(99)), List.of(field));

            assertEquals(List.of("99"), valueList, "字典查不到时 ServiceException 被吞掉，应回退为原始值");
        }

        @Test
        @DisplayName("当前缺陷：字典列为 null 时解析异常被吞掉")
        void testDictionaryNullColumn() {
            Field field = ReflectUtil.getField("gender", ExportDemoModel.class);
            assertNotNull(field, "反射应能获取到 gender 字段");

            List<String> valueList = CollectionUtil.getCsvValueList(List.of(new ExportDemoModel()), List.of(field));

            assertEquals(List.of("-"), valueList, "横杠无法被 Integer.parseInt 解析，异常被吞后回退为横杠");
        }

        @Test
        @DisplayName("当前行为：未标记 @Dictionary 的字典列原样输出")
        void testDictionaryWithoutAnnotation() {
            Field field = ReflectUtil.getField("dictionaryWithoutDictionary", BranchModel.class);
            assertNotNull(field, "反射应能获取到 dictionaryWithoutDictionary 字段");

            List<String> valueList = CollectionUtil.getCsvValueList(
                    List.of(new BranchModel().setDictionaryWithoutDictionary(1)), List.of(field));

            assertEquals(List.of("1"), valueList, "字段未标记 @Dictionary 时应直接返回原值");
        }

        @Test
        @DisplayName("当前行为：非数字类型的 NUMBER 列原样输出")
        void testNumberWithStringValue() {
            Field field = ReflectUtil.getField("numberAsString", BranchModel.class);
            assertNotNull(field, "反射应能获取到 numberAsString 字段");

            List<String> valueList = CollectionUtil.getCsvValueList(
                    List.of(new BranchModel().setNumberAsString("abc")), List.of(field));

            assertEquals(List.of("abc"), valueList, "非 Double/Float/Long 类型应原样返回");
        }

        @Test
        @DisplayName("当前缺陷：DATETIME 列值非数字时解析异常被吞掉")
        void testDateTimeWithStringValue() {
            Field field = ReflectUtil.getField("datetimeAsString", BranchModel.class);
            assertNotNull(field, "反射应能获取到 datetimeAsString 字段");

            List<String> valueList = CollectionUtil.getCsvValueList(
                    List.of(new BranchModel().setDatetimeAsString("不是时间")), List.of(field));

            assertEquals(List.of("不是时间"), valueList, "解析失败后异常被吞掉，应回退为原始值");
        }

        @Test
        @DisplayName("DATETIME 列：字符串数字也能正常格式化")
        void testDateTimeWithNumericString() {
            Field field = ReflectUtil.getField("datetimeAsString", BranchModel.class);
            assertNotNull(field, "反射应能获取到 datetimeAsString 字段");

            List<String> valueList = CollectionUtil.getCsvValueList(
                    List.of(new BranchModel().setDatetimeAsString(String.valueOf(CREATE_TIME))), List.of(field));

            assertEquals(List.of("\t" + DateTimeUtil.format(CREATE_TIME)), valueList,
                    "可解析为数字的字符串同样会走格式化分支");
        }

        @Test
        @DisplayName("TEXT 列：null 值输出带前缀的横杠")
        void testTextNullColumn() {
            Field field = ReflectUtil.getField("plainText", BranchModel.class);
            assertNotNull(field, "反射应能获取到 plainText 字段");

            List<String> valueList = CollectionUtil.getCsvValueList(
                    List.of(new BranchModel()), List.of(field));

            assertEquals(List.of("\t-"), valueList, "TEXT 列会先替换为横杠再前置制表符");
        }

        @Test
        @DisplayName("异常分支：传入 null 集合抛 ServiceException")
        void testNullList() {
            List<Field> fieldList = CollectionUtil.getExportFieldList(ExportDemoModel.class);

            assertThrows(ServiceException.class,
                    () -> CollectionUtil.getCsvValueList(null, fieldList),
                    "源码未对 list 判空，传入 null 应抛 ServiceException");
        }
    }

    @Nested
    @DisplayName("toCsvInputStream 集合转换为 CSV 文件流")
    class ToCsvInputStreamTest {

        @Test
        @DisplayName("编码：输出流应以 UTF-8 BOM 开头，Excel 才不会用系统代码页解码")
        void testUtf8Bom() throws IOException {
            byte[] raw = CollectionUtil.toCsvInputStream(List.of(fullModel()), ExportDemoModel.class)
                    .readAllBytes();

            assertArrayEquals(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF},
                    Arrays.copyOf(raw, 3), "前三字节必须是 UTF-8 BOM（EF BB BF）");
        }

        @Test
        @DisplayName("编码：BOM 只在文件头出现一次，剥离后正文不受影响")
        void testBomOnlyOnceAtHead() throws IOException {
            String text = new String(CollectionUtil.toCsvInputStream(
                            List.of(fullModel(), fullModel()), ExportDemoModel.class).readAllBytes(),
                    StandardCharsets.UTF_8);

            assertTrue(text.startsWith(CollectionUtil.UTF8_BOM), "内容应以 BOM 开头");
            assertEquals(1, text.split(CollectionUtil.UTF8_BOM, -1).length - 1,
                    "全文只能出现一次 BOM，多一个就会在表格中间插入不可见字符");
            assertEquals(EXPECTED_HEADER + "\n" + EXPECTED_ROW + "\n" + EXPECTED_ROW,
                    text.substring(CollectionUtil.UTF8_BOM.length()),
                    "剥离 BOM 后应正好是表头加两行数据");
        }

        @Test
        @DisplayName("编码：空集合也要带 BOM，否则只有表头的文件同样会乱码")
        void testBomOnEmptyList() throws IOException {
            byte[] raw = CollectionUtil.toCsvInputStream(List.of(), ExportDemoModel.class).readAllBytes();

            assertArrayEquals(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF},
                    Arrays.copyOf(raw, 3), "即使没有数据行，表头也必须带 BOM");
            assertEquals(EXPECTED_HEADER, readUtf8(
                    new ByteArrayInputStream(raw)), "剥离 BOM 后应只剩表头行");
        }

        @Test
        @DisplayName("正常路径：表头 + 数据行，行分隔符为换行")
        void testCsvContent() throws IOException {
            InputStream inputStream = CollectionUtil.toCsvInputStream(
                    List.of(fullModel()), ExportDemoModel.class);

            String csv = readUtf8(inputStream);

            assertEquals(EXPECTED_HEADER + "\n" + EXPECTED_ROW, csv, "CSV 应为一行表头加一行数据");
            assertFalse(csv.endsWith(CollectionUtil.CSV_ROW_DELIMITER), "末尾不应存在多余的换行");
        }

        @Test
        @DisplayName("正常路径：多条数据生成多行")
        void testMultipleRows() throws IOException {
            List<ExportDemoModel> list = List.of(
                    fullModel().setId(1L).setName("一"),
                    fullModel().setId(2L).setName("二"),
                    fullModel().setId(3L).setName("三")
            );

            String csv = readUtf8(CollectionUtil.toCsvInputStream(list, ExportDemoModel.class));

            String[] lines = csv.split(CollectionUtil.CSV_ROW_DELIMITER);
            assertEquals(4, lines.length, "一行表头加三行数据");
            assertEquals(EXPECTED_HEADER, lines[0], "首行应为表头");
            assertTrue(lines[1].contains("一"), "第二行应为第一条数据");
            assertTrue(lines[3].contains("三"), "第四行应为第三条数据");
        }

        @Test
        @DisplayName("边界值：空集合只有表头行")
        void testEmptyList() throws IOException {
            String csv = readUtf8(CollectionUtil.toCsvInputStream(List.of(), ExportDemoModel.class));

            assertEquals(EXPECTED_HEADER, csv, "空集合只应输出表头行");
            assertFalse(csv.contains(CollectionUtil.CSV_ROW_DELIMITER), "空集合不应出现行分隔符");
        }

        @Test
        @DisplayName("正常路径：使用 UTF-8 编码且返回字节数组流")
        void testUtf8Encoding() throws IOException {
            InputStream inputStream = CollectionUtil.toCsvInputStream(
                    List.of(fullModel().setName("张三")), ExportDemoModel.class);

            assertInstanceOf(java.io.ByteArrayInputStream.class, inputStream, "源码返回的是 ByteArrayInputStream");
            String csv = readUtf8(inputStream);
            assertTrue(csv.contains("张三"), "中文应按 UTF-8 正确编码与解码");
        }

        @Test
        @DisplayName("边界值：集合中含 null 元素时跳过该行，不中断整份导出")
        void testNullElementInList() {
            List<ExportDemoModel> list = new ArrayList<>();
            list.add(fullModel());
            list.add(null);

            // 一条脏数据导致整份导出失败，对导出场景代价过高
            assertDoesNotThrow(() -> CollectionUtil.toCsvInputStream(list, ExportDemoModel.class),
                    "null 元素应被跳过而不是抛空指针");
        }

        @Test
        @DisplayName("安全：公式注入前缀的单元格被加上防护前缀")
        void testFormulaInjectionGuard() throws IOException {
            List<ExportDemoModel> list = new ArrayList<>();
            list.add(fullModel().setName("=1+1"));
            list.add(fullModel().setName("@SUM(A1)"));

            String csv = readUtf8(CollectionUtil.toCsvInputStream(list, ExportDemoModel.class));

            assertFalse(csv.contains("\n=1+1"), "以 = 开头的值不能原样输出，否则 Excel 会当公式执行");
            assertTrue(csv.contains("'=1+1"), "公式注入值应加单引号前缀：" + csv);
            assertTrue(csv.contains("'@SUM(A1)"), "公式注入值应加单引号前缀：" + csv);
        }

        @Test
        @DisplayName("边界值：模型字段全部为 null 时输出横杠行")
        void testAllNullModel() {
            String csv = assertDoesNotThrow(() -> readUtf8(CollectionUtil.toCsvInputStream(
                            List.of(new ExportDemoModel()), ExportDemoModel.class)),
                    "全部字段为 null 时各列均应回退为横杠，不应抛出异常");

            String[] lines = csv.split(CollectionUtil.CSV_ROW_DELIMITER);
            assertEquals(2, lines.length, "空模型也应生成一行数据");
            assertTrue(lines[1].contains("-"), "所有列均应输出横杠");
        }
    }

    @Nested
    @DisplayName("字典与类型分支覆盖用的辅助模型")
    class BranchModelTest {

        @Test
        @DisplayName("正常路径：辅助模型按 sort 倒序输出")
        void testBranchModelCsv() throws IOException {
            String csv = readUtf8(CollectionUtil.toCsvInputStream(
                    List.of(new BranchModel()
                            .setDictionaryWithoutDictionary(1)
                            .setNumberAsString("123")
                            .setDatetimeAsString("不是时间")
                            .setPlainText("文本")
                            .setNoDescription("无描述")),
                    BranchModel.class));

            assertEquals("无字典注解,非数字,非时间戳,纯文本,noDescription\n1,123,不是时间,\t文本,\t无描述", csv,
                    "辅助模型应按 sort 倒序输出，无 @Description 的列回退为字段名，各分支表现符合预期");
        }

        @Test
        @DisplayName("正常路径：辅助模型同样排除 remove 与未标记 @Export 的列")
        void testBranchModelFieldList() {
            List<Field> fieldList = CollectionUtil.getExportFieldList(BranchModel.class);

            assertEquals(5, fieldList.size(), "辅助模型的 5 个 @Export 列都应被导出");
            assertEquals(
                    List.of("dictionaryWithoutDictionary", "numberAsString", "datetimeAsString", "plainText", "noDescription"),
                    fieldList.stream().map(Field::getName).toList(),
                    "辅助模型应按 sort 从大到小排列");
        }
    }

    @Nested
    @DisplayName("CSV 单元格转义与公式防护")
    class CsvSanitizeTest {

        @Test
        @DisplayName("字段值含双引号时必须被处理，否则标准解析器会多切出列")
        void doubleQuoteIsEscaped() throws IOException {
            ExportDemoModel model = fullModel().setRemark("他说\"这是引号\"，还有 JSON {\"a\":1}");

            String csv = readUtf8(CollectionUtil.toCsvInputStream(List.of(model), ExportDemoModel.class));

            assertFalse(csv.contains("\"这是引号\""),
                    "双引号是 RFC 4180 的引用字符，原样进入 CSV 会被 csv.reader / pandas 切片，"
                            + "导致该行多出列、后续所有列错位");
        }

        @Test
        @DisplayName("字段值含 CR 时必须被处理，否则一行被拆成两行")
        void carriageReturnIsRemoved() throws IOException {
            byte[] raw = CollectionUtil.toCsvInputStream(
                    List.of(fullModel().setRemark("第一行\r\n第二行")), ExportDemoModel.class)
                    .readAllBytes();

            String csv = readUtf8(new ByteArrayInputStream(raw));
            assertFalse(csv.contains("\r"),
                    "输出里不应残留 CR：它会被部分表格软件当作换行，把一行拆成两行"
                            + "（导出行数比数据条数多），且只在 Windows + WPS 上偶现，极难复现");
            assertEquals(2, csv.split("\n", -1).length,
                    "一行表头加一行数据共 2 行，不应因为字段里的 CR 变成 3 行");
            assertTrue(csv.contains("第一行") && csv.contains("第二行"),
                    "CR/LF 两侧的文字都应保留，只是不再携带换行语义");
        }

        @Test
        @DisplayName("以公式字符开头的值必须加防护前缀")
        void formulaPrefixIsGuarded() throws IOException {
            for (String payload : List.of("=1+1", "+1", "-1", "@SUM(A1)")) {
                String csv = readUtf8(CollectionUtil.toCsvInputStream(
                        List.of(fullModel().setName(payload)), ExportDemoModel.class));
                assertTrue(csv.contains("'" + payload),
                        "以公式字符开头的值必须加 ' 前缀，否则 Excel/WPS 会当公式执行：" + payload);
            }
        }

        @Test
        @DisplayName("前导空白后的公式字符同样要防护")
        void formulaAfterLeadingWhitespaceIsGuarded() throws IOException {
            String csv = readUtf8(CollectionUtil.toCsvInputStream(
                    List.of(fullModel().setName("  =cmd|'/c calc'!A1")), ExportDemoModel.class));

            assertTrue(csv.contains("'  =cmd"),
                    "Excel 解析时会忽略前导空白，所以必须跳过前导空白后再判定");
        }

        @Test
        @DisplayName("普通值不应被加防护前缀")
        void normalValueIsNotGuarded() throws IOException {
            String csv = readUtf8(CollectionUtil.toCsvInputStream(
                    List.of(fullModel().setName("张三")), ExportDemoModel.class));

            assertTrue(csv.contains("张三"), "正常值应原样输出");
            assertFalse(csv.contains("'张三"), "正常值不应被加防护前缀");
        }
    }
}
