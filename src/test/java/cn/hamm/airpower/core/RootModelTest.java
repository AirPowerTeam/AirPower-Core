package cn.hamm.airpower.core;

import cn.hamm.airpower.core.fixture.DemoModel;
import cn.hamm.airpower.core.fixture.ExportDemoModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>RootModel 单元测试</h1>
 *
 * <p>本测试以<b>源码实际行为</b>为断言依据，刻意记录以下几处与文档/方法名不一致的行为：</p>
 * <ul>
 *     <li>{@link RootModel#desensitize()} 内部传入空白名单，实际等价于
 *     {@link RootModel#excludeNotMeta()}，<b>不会脱敏</b></li>
 *     <li>白名单分支下不校验 {@code @Meta}，非元数据字段也会被保留</li>
 *     <li>排除非元数据分支下不会递归处理模型集合的元素</li>
 *     <li>白名单分支遇到集合中的 {@code null} 元素会抛 {@link NullPointerException}</li>
 * </ul>
 *
 * @author Hamm.cn
 */
@DisplayName("RootModel 数据根模型单元测试")
class RootModelTest {

    /**
     * 未脱敏的手机号
     */
    private static final String MOBILE = "13800138000";

    /**
     * 邮箱（用于自定义 head=1 / tail=1 / symbol=# 的脱敏断言）
     */
    private static final String EMAIL = "ab@cd.com";

    /**
     * 邮箱脱敏后的期望值：保留首尾各 1 位，中间 7 位替换为 {@code #}
     */
    private static final String EMAIL_MASKED = "a#######m";

    /**
     * 包含 DemoModel 自身的白名单，只有白名单命中当前类才会走“递归嵌套模型 / 脱敏”分支
     */
    private static final List<Class<? extends RootModel<?>>> WHITE_LIST = List.of(DemoModel.class);

    /**
     * 待测模型
     */
    private DemoModel model;

    @BeforeEach
    void setUp() {
        model = new DemoModel()
                .setId(1L)
                .setName("Hamm")
                .setMobile(MOBILE)
                .setCreateTime(1700000000000L)
                .setRemark("备注")
                .setTitle("标题")
                .setSecret("secret-value")
                .setSecretNumber(1234)
                .setEmail(EMAIL);
    }

    @Nested
    @DisplayName("isModel 判断是否继承自 RootModel")
    class IsModelTest {

        @Test
        @DisplayName("边界值：null 应返回 false")
        void testIsModelWithNull() {
            assertEquals(false, RootModel.isModel(null), "入参为 null 时应直接返回 false");
        }

        @Test
        @DisplayName("正常路径：RootModel 自身返回 true")
        void testIsModelWithRootModelClass() {
            assertEquals(true, RootModel.isModel(RootModel.class), "RootModel 自身应被识别为模型");
        }

        @Test
        @DisplayName("正常路径：子类返回 true")
        void testIsModelWithSubClass() {
            assertEquals(true, RootModel.isModel(DemoModel.class), "DemoModel 继承自 RootModel，应返回 true");
        }

        @Test
        @DisplayName("正常路径：非模型类返回 false")
        void testIsModelWithStringClass() {
            assertEquals(false, RootModel.isModel(String.class), "String 未继承 RootModel，应返回 false");
        }

        @Test
        @DisplayName("边界值：Object 与基本类型返回 false")
        void testIsModelWithObjectAndPrimitive() {
            assertEquals(false, RootModel.isModel(Object.class), "Object 是递归终点，应返回 false");
            assertEquals(false, RootModel.isModel(int.class), "基本类型的父类为 null，应返回 false");
        }

        @Test
        @DisplayName("正常路径：枚举与集合均返回 false")
        void testIsModelWithEnumAndCollection() {
            assertEquals(false, RootModel.isModel(java.time.DayOfWeek.class), "枚举未继承 RootModel，应返回 false");
            assertEquals(false, RootModel.isModel(ArrayList.class), "集合未继承 RootModel，应返回 false");
        }
    }

    @Nested
    @DisplayName("excludeReadOnly 排除只读字段")
    class ExcludeReadOnlyTest {

        @Test
        @DisplayName("正常路径：@ReadOnly 字段被置空，其余字段保留")
        void testExcludeReadOnly() {
            model.excludeReadOnly();
            assertNull(model.getCreateTime(), "标记了 @ReadOnly 的 createTime 字段应被置空");
            assertEquals(1L, model.getId(), "非只读字段 id 应被保留");
            assertEquals("Hamm", model.getName(), "非只读字段 name 应被保留");
            assertEquals(MOBILE, model.getMobile(), "excludeReadOnly 不做脱敏，mobile 应保持原值");
            assertEquals("备注", model.getRemark(), "excludeReadOnly 不处理非元数据字段，remark 应被保留");
        }

        @Test
        @DisplayName("边界值：全字段为 null 的空模型不抛异常")
        void testExcludeReadOnlyWithEmptyModel() {
            DemoModel empty = new DemoModel();
            assertDoesNotThrow(empty::excludeReadOnly, "空模型执行 excludeReadOnly 不应抛出异常");
            assertNull(empty.getCreateTime(), "空模型处理后各字段仍应为 null");
        }

        @Test
        @DisplayName("当前行为：不会递归处理嵌套模型的只读字段")
        void testExcludeReadOnlyNotRecurse() {
            DemoModel child = new DemoModel().setName("子").setCreateTime(123L);
            model.setChild(child);

            model.excludeReadOnly();

            assertEquals(123L, child.getCreateTime(), "excludeReadOnly 未递归，当前实现下子模型的只读字段不会被清空");
        }

        @Test
        @DisplayName("边界值：只读字段已是 null 时重复调用不抛异常")
        void testExcludeReadOnlyIdempotent() {
            model.excludeReadOnly();
            assertDoesNotThrow(model::excludeReadOnly, "对已清空只读字段的模型重复调用不应抛出异常");
        }
    }

    @Nested
    @DisplayName("excludeNotMeta 排除非元数据字段")
    class ExcludeNotMetaTest {

        @Test
        @DisplayName("正常路径：无 @Meta 的字段被清空")
        void testExcludeNotMeta() {
            model.excludeNotMeta();

            assertNull(model.getRemark(), "既没有字段 @Meta 也没有 Getter @Meta 的 remark 应被置空");
            assertEquals("标题", model.getTitle(), "Getter 上标记了 @Meta 的 title 应被保留");
            assertEquals(1L, model.getId(), "标记了 @Meta 的 id 应被保留");
            assertEquals("Hamm", model.getName(), "标记了 @Meta 的 name 应被保留");
            assertEquals(MOBILE, model.getMobile(), "excludeNotMeta 不做脱敏，mobile 应保持原值");
            assertEquals("secret-value", model.getSecret(), "excludeNotMeta 不做脱敏，secret 应保持原值");
            assertEquals(1234, model.getSecretNumber(), "excludeNotMeta 不做脱敏，secretNumber 应保持原值");
        }

        @Test
        @DisplayName("正常路径：嵌套模型被递归排除非元数据字段")
        void testExcludeNotMetaWithChild() {
            DemoModel child = new DemoModel()
                    .setName("子")
                    .setRemark("子备注")
                    .setTitle("子标题")
                    .setMobile(MOBILE);
            model.setChild(child);

            model.excludeNotMeta();

            assertNull(child.getRemark(), "嵌套模型的非元数据字段应被递归清空");
            assertEquals("子标题", child.getTitle(), "嵌套模型 Getter 上有 @Meta 的字段应被保留");
            assertEquals(MOBILE, child.getMobile(), "排除非元数据分支不做脱敏，子模型 mobile 应保持原值");
            assertEquals("子", child.getName(), "嵌套模型的元数据字段应被保留");
        }

        @Test
        @DisplayName("当前行为：模型集合的元素不会被递归处理")
        void testExcludeNotMetaWithChildrenNotRecurse() {
            DemoModel child = new DemoModel().setName("子").setRemark("子备注");
            model.setChildren(List.of(child));

            model.excludeNotMeta();

            assertNotNull(model.getChildren(), "标记了 @Meta 的 children 字段本身应被保留");
            assertEquals("子备注", child.getRemark(), "排除非元数据分支不遍历集合元素，元素的 remark 当前不会被清空");
        }

        @Test
        @DisplayName("当前行为：集合中的 null 元素在排除非元数据分支下不会抛异常")
        void testExcludeNotMetaWithNullElementInCollection() {
            model.setChildrenWithNull(Arrays.asList(new DemoModel(), null));

            assertDoesNotThrow(() -> model.excludeNotMeta(), "排除非元数据分支不遍历集合元素，含 null 元素不应抛异常");
            assertEquals(2, model.getChildrenWithNull().size(), "childrenWithNull 集合应保持原有元素个数");
        }

        @Test
        @DisplayName("边界值：空模型与全 null 字段不抛异常")
        void testExcludeNotMetaWithEmptyModel() {
            assertDoesNotThrow(() -> new DemoModel().excludeNotMeta(), "空模型执行 excludeNotMeta 不应抛出异常");

            DemoModel allNull = new DemoModel().setId(null).setRemark(null).setTitle(null).setChildren(null);
            assertDoesNotThrow(() -> allNull.excludeNotMeta(), "所有字段为 null 的模型不应抛出异常");
            assertNull(allNull.getRemark(), "全 null 模型处理后字段仍为 null");
        }

        @Test
        @DisplayName("异常分支：集合元素为 null 时白名单分支抛 NullPointerException")
        void testExcludeNotMetaWithWhiteListAndNullElement() {
            model.setChildrenWithNull(Arrays.asList(new DemoModel(), null));

            assertThrows(NullPointerException.class,
                    () -> model.excludeNotMeta(WHITE_LIST),
                    "白名单分支会对集合元素调用 item.getClass()，遇到 null 元素当前会抛 NullPointerException");
        }
    }

    @Nested
    @DisplayName("desensitize 脱敏")
    class DesensitizeTest {

        @Test
        @DisplayName("当前行为：等价于 excludeNotMeta，不会脱敏")
        void testDesensitizeDoesNotMask() {
            model.desensitize();

            assertEquals(MOBILE, model.getMobile(), "当前实现下 desensitize 未真正脱敏，mobile 应保持原值");
            assertEquals("secret-value", model.getSecret(), "当前实现下 secret 不被脱敏，应保持原值");
            assertEquals(1234, model.getSecretNumber(), "当前实现下非字符串密文字段不被置空");
            assertEquals(EMAIL, model.getEmail(), "当前实现下 email 不被脱敏，应保持原值");
            assertNull(model.getRemark(), "desensitize 实际走的是排除非元数据分支，remark 仍会被清空");
            assertEquals("标题", model.getTitle(), "Getter 上有 @Meta 的 title 应被保留");
        }

        @Test
        @DisplayName("当前行为：嵌套模型也不会被脱敏")
        void testDesensitizeWithChildNotMask() {
            DemoModel child = new DemoModel().setMobile(MOBILE).setRemark("子备注");
            model.setChild(child);

            model.desensitize();

            assertEquals(MOBILE, child.getMobile(), "当前实现下嵌套模型的 mobile 不会被脱敏");
            assertNull(child.getRemark(), "嵌套模型仍会走排除非元数据分支，remark 被清空");
        }

        @Test
        @DisplayName("边界值：空模型不抛异常")
        void testDesensitizeWithEmptyModel() {
            assertDoesNotThrow(new DemoModel()::desensitize, "空模型执行 desensitize 不应抛出异常");
        }
    }

    @Nested
    @DisplayName("excludeNotMetaAndDesensitize 模型字段值处理")
    class ExcludeNotMetaAndDesensitizeTest {

        @Test
        @DisplayName("边界值：空白名单等价于排除非元数据字段")
        void testWithEmptyWhiteList() {
            model.excludeNotMetaAndDesensitize(List.of(), true);

            assertNull(model.getRemark(), "白名单为空时走排除非元数据分支，remark 应被清空");
            assertEquals(MOBILE, model.getMobile(), "白名单为空时不做脱敏，mobile 应保持原值");
        }

        @Test
        @DisplayName("边界值：白名单不含当前类时等价于排除非元数据字段")
        void testWithOtherClassWhiteList() {
            List<Class<? extends RootModel<?>>> otherWhiteList = List.of(ExportDemoModel.class);
            model.excludeNotMetaAndDesensitize(otherWhiteList, true);

            assertNull(model.getRemark(), "白名单未命中当前类时走排除非元数据分支，remark 应被清空");
            assertEquals(MOBILE, model.getMobile(), "白名单未命中当前类时不脱敏，mobile 应保持原值");
        }

        @Test
        @DisplayName("正常路径：白名单命中当前类且不脱敏时保留全部字段")
        void testWithWhiteListWithoutDesensitize() {
            model.excludeNotMetaAndDesensitize(WHITE_LIST, false);

            assertEquals("备注", model.getRemark(), "白名单分支不校验 @Meta，非元数据字段 remark 应被保留");
            assertEquals(MOBILE, model.getMobile(), "isDesensitize=false 时不脱敏，mobile 应保持原值");
            assertEquals(1234, model.getSecretNumber(), "isDesensitize=false 时非字符串密文不会被置空");
            assertEquals(1L, model.getId(), "元数据字段 id 应被保留");
        }

        @Test
        @DisplayName("正常路径：手机号按 MOBILE 规则保留前三后四位")
        void testDesensitizeMobile() {
            model.excludeNotMetaAndDesensitize(WHITE_LIST, true);

            assertEquals("138****8000", model.getMobile(), "MOBILE 规则应保留前 3 位、后 4 位");
        }

        @Test
        @DisplayName("正常路径：replace=true 的密文字段被整体替换为单个脱敏符号")
        void testDesensitizeWithReplace() {
            model.excludeNotMetaAndDesensitize(WHITE_LIST, true);

            assertEquals("*", model.getSecret(), "replace=true 时源码直接写入 symbol()，结果是单个星号而非重复填充");
        }

        @Test
        @DisplayName("正常路径：非字符串的脱敏字段被置空")
        void testDesensitizeNonStringValue() {
            model.excludeNotMetaAndDesensitize(WHITE_LIST, true);

            assertNull(model.getSecretNumber(), "Integer 类型的密文字段不支持脱敏，当前实现应被置空");
        }

        @Test
        @DisplayName("正常路径：自定义头尾与符号的邮箱脱敏")
        void testDesensitizeEmailWithCustomSymbol() {
            model.excludeNotMetaAndDesensitize(WHITE_LIST, true);

            assertEquals(EMAIL_MASKED, model.getEmail(), "应保留首尾各 1 位，中间用 # 填充");
        }

        @Test
        @DisplayName("正常路径：长邮箱中间部分全部替换为符号且长度不变")
        void testDesensitizeLongEmail() {
            model.setEmail("hamm@example.com");

            model.excludeNotMetaAndDesensitize(WHITE_LIST, true);

            String masked = model.getEmail();
            assertEquals("hamm@example.com".length(), masked.length(), "非整体替换时脱敏后长度应与原文一致");
            assertTrue(masked.startsWith("h"), "邮箱应保留首字符，实际为：" + masked);
            assertTrue(masked.endsWith("m"), "邮箱应保留尾字符，实际为：" + masked);
            assertTrue(masked.matches("h#+m"), "邮箱中间部分应全部为 #，实际为：" + masked);
        }

        @Test
        @DisplayName("边界值：长度不足头尾之和的字符串被整体替换为符号")
        void testDesensitizeTooShortValue() {
            model.setMobile("138");
            model.setEmail("ab");

            model.excludeNotMetaAndDesensitize(WHITE_LIST, true);

            assertEquals("***", model.getMobile(), "手机号长度不足时按原长度整体重复脱敏符号");
            assertEquals("##", model.getEmail(), "邮箱长度不足时按原长度整体重复脱敏符号");
        }

        @Test
        @DisplayName("边界值：单字符邮箱被替换为一个符号")
        void testDesensitizeSingleCharEmail() {
            model.setEmail("a");

            model.excludeNotMetaAndDesensitize(WHITE_LIST, true);

            assertEquals("#", model.getEmail(), "单字符邮箱脱敏后应只剩一个脱敏符号");
        }

        @Test
        @DisplayName("边界值：脱敏字段本身为 null 时不处理")
        void testDesensitizeNullValue() {
            model.setMobile(null).setSecret(null).setSecretNumber(null).setEmail(null);

            assertDoesNotThrow(() -> model.excludeNotMetaAndDesensitize(WHITE_LIST, true),
                    "脱敏字段为 null 时源码直接跳过，不应抛出异常");
            assertNull(model.getMobile(), "null 值在跳过分支后仍应为 null");
        }

        @Test
        @DisplayName("正常路径：嵌套模型被递归脱敏")
        void testDesensitizeChild() {
            DemoModel grandChild = new DemoModel().setMobile(MOBILE).setRemark("孙备注");
            DemoModel child = new DemoModel().setMobile(MOBILE).setRemark("子备注").setChild(grandChild);
            model.setChild(child);

            model.excludeNotMetaAndDesensitize(WHITE_LIST, true);

            assertEquals("138****8000", child.getMobile(), "嵌套模型应被递归脱敏");
            assertEquals("138****8000", grandChild.getMobile(), "多层嵌套模型应被递归脱敏");
            assertEquals("子备注", child.getRemark(), "白名单分支不排除非元数据字段，子模型 remark 应被保留");
            assertEquals("孙备注", grandChild.getRemark(), "白名单分支不排除非元数据字段，孙模型 remark 应被保留");
        }

        @Test
        @DisplayName("正常路径：模型集合中的每个元素被递归脱敏")
        void testDesensitizeChildren() {
            DemoModel first = new DemoModel().setMobile(MOBILE).setName("一");
            DemoModel second = new DemoModel().setMobile(MOBILE).setName("二");
            model.setChildren(List.of(first, second));

            model.excludeNotMetaAndDesensitize(WHITE_LIST, true);

            assertEquals(2, model.getChildren().size(), "children 集合本身应被保留");
            assertEquals("138****8000", first.getMobile(), "集合中第一个元素应被脱敏");
            assertEquals("138****8000", second.getMobile(), "集合中第二个元素应被脱敏");
            assertEquals("一", first.getName(), "集合元素的非脱敏字段应被保留");
        }

        @Test
        @DisplayName("边界值：空集合不抛异常")
        void testDesensitizeWithEmptyCollection() {
            model.setChildren(List.of());
            model.setChildrenWithNull(List.of());

            assertDoesNotThrow(() -> model.excludeNotMetaAndDesensitize(WHITE_LIST, true),
                    "空的模型集合不应抛出异常");
        }

        @Test
        @DisplayName("当前缺陷：集合含 null 元素时抛 NullPointerException")
        void testDesensitizeWithNullElementInCollection() {
            model.setChildrenWithNull(Arrays.asList(new DemoModel(), null));

            assertThrows(NullPointerException.class,
                    () -> model.excludeNotMetaAndDesensitize(WHITE_LIST, true),
                    "白名单分支对集合元素调用 item.getClass() 未判空，含 null 元素会抛 NullPointerException");
        }

        @Test
        @DisplayName("当前缺陷：集合抛异常前，已处理的元素已被就地修改")
        void testDesensitizePartialWhenNpeThrown() {
            DemoModel valid = new DemoModel().setMobile(MOBILE);
            model.setChildrenWithNull(Arrays.asList(valid, null));

            assertThrows(NullPointerException.class,
                    () -> model.excludeNotMetaAndDesensitize(WHITE_LIST, true),
                    "含 null 元素的集合应抛出 NullPointerException");
            assertEquals("138****8000", valid.getMobile(), "异常抛出前，位于 null 元素之前的元素应已完成脱敏");
        }

        @Test
        @DisplayName("边界值：全 null 的模型不抛异常")
        void testAllNullModel() {
            DemoModel allNull = new DemoModel();

            assertDoesNotThrow(() -> allNull.excludeNotMetaAndDesensitize(WHITE_LIST, true),
                    "所有字段为 null 时各分支均直接跳过，不应抛出异常");
            assertNull(allNull.getMobile(), "全 null 模型处理后字段仍为 null");
        }

        @Test
        @DisplayName("异常分支：白名单为 null 且存在非空字段时抛 NullPointerException")
        void testNullWhiteListWithValue() {
            assertThrows(NullPointerException.class,
                    () -> model.excludeNotMetaAndDesensitize(null, true),
                    "源码未对 whiteList 判空，字段存在非空值时调用 isEmpty() 会抛 NullPointerException");
        }

        @Test
        @DisplayName("边界值：白名单为 null 但字段全为 null 时不抛异常")
        void testNullWhiteListWithEmptyModel() {
            assertDoesNotThrow(() -> new DemoModel().excludeNotMetaAndDesensitize(null, true),
                    "字段值全为 null 时源码在判空之前就 return，不会访问 whiteList");
        }
    }

    @Nested
    @DisplayName("Getter 生成的只读访问器")
    class GetterTest {

        @Test
        @DisplayName("正常路径：title 的自定义 Getter 返回字段值")
        void testGetTitle() {
            assertEquals("标题", model.getTitle(), "自定义 Getter 应返回 title 字段的值");
        }
    }
}
