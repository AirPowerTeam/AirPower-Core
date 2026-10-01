package cn.hamm.airpower.core;

import cn.hamm.airpower.core.annotation.Desensitize;
import cn.hamm.airpower.core.annotation.Meta;
import cn.hamm.airpower.core.enums.DesensitizeType;
import cn.hamm.airpower.core.fixture.DemoModel;
import cn.hamm.airpower.core.fixture.ExportDemoModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>RootModel 单元测试</h1>
 *
 * <p>本测试以<b>源码实际行为</b>为断言依据，当前行为要点：</p>
 * <ul>
 *     <li>{@link RootModel#desensitize()} 先排除非元数据字段，再递归对
 *     <b>所有可达模型</b>（含类型不同的嵌套模型与模型集合）执行脱敏</li>
 *     <li>{@link RootModel#excludeNotMetaAndDesensitize} 仍按类白名单语义：
 *     白名单外的类型只排除非元数据字段，不做脱敏</li>
 *     <li>白名单分支下不校验 {@code @Meta}，非元数据字段也会被保留</li>
 *     <li>排除非元数据分支会递归处理嵌套模型与模型集合的元素</li>
 *     <li>白名单为 {@code null} 时按空名单处理，集合中的 {@code null} 元素会被跳过</li>
 *     <li>自引用模型由「已访问集合」拦下，不会栈溢出</li>
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
        @DisplayName("会递归清空嵌套模型与模型集合中的只读字段")
        void testExcludeReadOnlyRecurse() {
            DemoModel child = new DemoModel().setName("子").setCreateTime(123L);
            DemoModel listItem = new DemoModel().setCreateTime(456L);
            model.setChild(child);
            model.setChildren(new ArrayList<>(List.of(listItem)));

            model.excludeReadOnly();

            // 递归后嵌套模型的只读字段也会被清空，否则创建时间会返回给前端，
            // 客户端可据此覆盖服务端数据
            assertNull(child.getCreateTime(), "嵌套模型的只读字段也应被清空");
            assertNull(listItem.getCreateTime(), "模型集合元素的只读字段也应被清空");
            assertEquals("子", child.getName(), "非只读字段不受影响");
        }

        @Test
        @DisplayName("边界值：自引用模型不会栈溢出")
        void testExcludeReadOnlySelfReference() {
            DemoModel self = new DemoModel().setCreateTime(789L);
            self.setChild(self);

            assertDoesNotThrow(self::excludeReadOnly, "自引用模型应由已访问集合拦下");

            assertNull(self.getCreateTime(), "自引用模型自身的只读字段仍应被清空");
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
        @DisplayName("白名单必须 fail-closed：拼不出 getter 名的字段也要被排除")
        void testExcludeNotMetaIsFailClosed() throws Exception {
            // 用反射直接赋值，绕开 @Getter(NONE) 造成的编译期限制
            Field internalFlag = DemoModel.class.getDeclaredField("isInternalFlag");
            internalFlag.setAccessible(true);
            internalFlag.setBoolean(model, true);
            Field hidden = DemoModel.class.getDeclaredField("hidden");
            hidden.setAccessible(true);
            hidden.set(model, "不该出现在响应里");

            model.excludeNotMeta();

            assertFalse(internalFlag.getBoolean(model),
                    "基本类型 boolean isInternalFlag：Lombok 生成的是 isInternalFlag() 而非 "
                            + "getIsInternalFlag()，按字段名硬拼 getter 找不到方法。"
                            + "早先的写法把 NoSuchMethodException 空 catch 掉，"
                            + "字段既不被置空也不被处理，被原样返回前端——白名单 fail-open 等于没有白名单");
            assertNull(hidden.get(model),
                    "@Getter(NONE) 的字段没有任何 getter，同样必须被排除而不是泄露");
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
        @DisplayName("排除非元数据时会递归处理模型集合")
        void testExcludeNotMetaWithChildren() {
            DemoModel child = new DemoModel().setName("子").setRemark("子备注");
            model.setChildren(List.of(child));

            model.excludeNotMeta();

            assertNotNull(model.getChildren(), "标记了 @Meta 的 children 字段本身应被保留");
            assertEquals(1, model.getChildren().size(), "children 集合本身的元素个数不应被改变");
            assertNull(child.getRemark(), "排除非元数据时会遍历集合元素，元素的 remark 应被清空");
            assertEquals("子", child.getName(), "集合元素的元数据字段 name 应被保留");
        }

        @Test
        @DisplayName("正常路径：模型集合中的每个元素都被递归排除非元数据字段")
        void testExcludeNotMetaWithChildrenEachElement() {
            DemoModel first = new DemoModel().setName("一").setRemark("一备注").setTitle("一标题");
            DemoModel second = new DemoModel().setName("二").setRemark("二备注");
            model.setChildren(List.of(first, second));

            model.excludeNotMeta();

            assertNull(first.getRemark(), "集合中第一个元素的非元数据字段 remark 应被递归清空");
            assertNull(second.getRemark(), "集合中第二个元素的非元数据字段 remark 应被递归清空");
            assertEquals("一标题", first.getTitle(), "集合元素 Getter 上标记了 @Meta 的 title 应被保留");
            assertEquals("一", first.getName(), "集合元素标记了 @Meta 的 name 应被保留");
        }

        @Test
        @DisplayName("边界值：集合中的 null 元素被跳过，不抛异常")
        void testExcludeNotMetaWithNullElementInCollection() {
            DemoModel valid = new DemoModel().setName("一").setRemark("子备注");
            model.setChildrenWithNull(Arrays.asList(valid, null));

            assertDoesNotThrow(() -> model.excludeNotMeta(), "排除非元数据分支会先判空再取集合元素的类，含 null 元素不应抛异常");
            assertEquals(2, model.getChildrenWithNull().size(), "childrenWithNull 集合应保持原有元素个数");
            assertNull(model.getChildrenWithNull().get(1), "集合中的 null 元素应被跳过后原样保留");
            assertNull(valid.getRemark(), "非 null 元素仍会被递归排除非元数据字段");
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
        @DisplayName("边界值：白名单分支下集合中的 null 元素被跳过，不抛异常")
        void testExcludeNotMetaWithWhiteListAndNullElement() {
            DemoModel valid = new DemoModel().setName("一").setRemark("子备注").setMobile(MOBILE);
            model.setChildrenWithNull(Arrays.asList(valid, null));

            assertDoesNotThrow(() -> model.excludeNotMeta(WHITE_LIST),
                    "白名单分支会先判空再取集合元素的类，含 null 元素不应抛出 NullPointerException");
            assertEquals(2, model.getChildrenWithNull().size(), "childrenWithNull 集合应保持原有元素个数");
            assertNull(model.getChildrenWithNull().get(1), "集合中的 null 元素应被跳过后原样保留");
            assertEquals("子备注", valid.getRemark(), "白名单分支不排除非元数据字段，元素 remark 应被保留");
            assertEquals(MOBILE, valid.getMobile(), "excludeNotMeta 不做脱敏，元素 mobile 应保持原值");
        }
    }

    @Nested
    @DisplayName("desensitize 脱敏")
    class DesensitizeTest {

        @Test
        @DisplayName("desensitize 应对 @Desensitize 字段生效")
        void testDesensitizeDoesNotMask() {
            model.desensitize();

            assertEquals("138****8000", model.getMobile(), "desensitize 已真正脱敏，mobile 应保留前 3 位、后 4 位");
            assertNull(model.getRemark(), "desensitize 会先排除非元数据字段，remark 应被清空");
            assertEquals("标题", model.getTitle(), "Getter 上有 @Meta 的 title 应被保留");
        }

        @Test
        @DisplayName("正常路径：嵌套模型也会被脱敏")
        void testDesensitizeWithChildNotMask() {
            DemoModel child = new DemoModel().setMobile(MOBILE).setRemark("子备注");
            model.setChild(child);

            model.desensitize();

            assertEquals("138****8000", child.getMobile(), "desensitize 会递归脱敏，嵌套模型的 mobile 应被脱敏");
            assertNull(child.getRemark(), "desensitize 会递归排除非元数据字段，嵌套模型的 remark 应被清空");
        }

        @Test
        @DisplayName("正常路径：desensitize 对全部 @Desensitize 字段生效")
        void testDesensitizeAllAnnotatedFields() {
            model.desensitize();

            assertEquals("138****8000", model.getMobile(), "标记 @Desensitize(MOBILE) 的 mobile 应按手机号规则脱敏");
            assertEquals("*", model.getSecret(), "replace=true 的 secret 应被整体替换为单个脱敏符号");
            assertEquals(1234, model.getSecretNumber(),
                    "Integer 类型的 secretNumber 无法在不破坏类型的前提下脱敏，应保持原值而不是被置空"
                            + "（置空会让原地修改的实体一旦 flush 回库就是真实的字段级数据丢失）");
            assertEquals(EMAIL_MASKED, model.getEmail(), "自定义 head=1/tail=1/symbol=# 的 email 应按自定义规则脱敏");
            assertNull(model.getRemark(), "desensitize 会先排除非元数据字段，remark 应被清空");
            assertEquals("标题", model.getTitle(), "Getter 上有 @Meta 的 title 应被保留");
            assertEquals(1L, model.getId(), "标记了 @Meta 的 id 应被保留");
            assertEquals("Hamm", model.getName(), "标记了 @Meta 的 name 应被保留");
            assertEquals(1700000000000L, model.getCreateTime(), "desensitize 不处理只读字段，createTime 应被保留");
        }

        @Test
        @DisplayName("正常路径：嵌套模型与模型集合的元素都被递归脱敏")
        void testDesensitizeChildAndChildren() {
            DemoModel child = new DemoModel().setMobile(MOBILE).setEmail(EMAIL).setSecret("secret-value");
            DemoModel childInList = new DemoModel().setMobile(MOBILE).setEmail(EMAIL).setSecret("secret-value");
            model.setChild(child).setChildren(List.of(childInList));

            model.desensitize();

            assertEquals("138****8000", child.getMobile(), "嵌套模型的 mobile 应被递归脱敏");
            assertEquals(EMAIL_MASKED, child.getEmail(), "嵌套模型的 email 应被递归脱敏");
            assertEquals("*", child.getSecret(), "嵌套模型的 secret 应被递归替换为脱敏符号");
            assertEquals("138****8000", childInList.getMobile(), "集合元素的 mobile 应被递归脱敏");
            assertEquals(EMAIL_MASKED, childInList.getEmail(), "集合元素的 email 应被递归脱敏");
            assertEquals("*", childInList.getSecret(), "集合元素的 secret 应被递归替换为脱敏符号");
        }

        @Test
        @DisplayName("正常路径：desensitize 与「自身类白名单 + 脱敏」对嵌套模型和集合的脱敏结果一致")
        void testDesensitizeSameAsSelfClassWhiteList() {
            DemoModel child = new DemoModel().setMobile(MOBILE).setEmail(EMAIL).setRemark("子备注");
            DemoModel childInList = new DemoModel().setMobile(MOBILE).setEmail(EMAIL).setRemark("子备注");
            model.setChild(child).setChildren(List.of(childInList));

            model.desensitize();

            // 用结构相同但相互独立的模型，走「自身类白名单 + 脱敏」分支
            DemoModel whiteListChild = new DemoModel().setMobile(MOBILE).setEmail(EMAIL).setRemark("子备注");
            DemoModel whiteListChildInList = new DemoModel().setMobile(MOBILE).setEmail(EMAIL).setRemark("子备注");
            DemoModel other = new DemoModel()
                    .setId(1L)
                    .setName("Hamm")
                    .setMobile(MOBILE)
                    .setRemark("备注")
                    .setTitle("标题")
                    .setSecret("secret-value")
                    .setSecretNumber(1234)
                    .setEmail(EMAIL);
            other.setChild(whiteListChild).setChildren(List.of(whiteListChildInList));

            other.excludeNotMetaAndDesensitize(List.of(DemoModel.class), true);

            assertEquals("138****8000", child.getMobile(), "desensitize 应把嵌套模型的 mobile 脱敏");
            assertEquals(EMAIL_MASKED, child.getEmail(), "desensitize 应把嵌套模型的 email 脱敏");
            assertEquals("138****8000", childInList.getMobile(), "desensitize 应把集合元素的 mobile 脱敏");
            assertEquals(EMAIL_MASKED, childInList.getEmail(), "desensitize 应把集合元素的 email 脱敏");
            assertEquals(child.getMobile(), whiteListChild.getMobile(), "两种方式对嵌套模型 mobile 的脱敏结果应一致");
            assertEquals(child.getEmail(), whiteListChild.getEmail(), "两种方式对嵌套模型 email 的脱敏结果应一致");
            assertEquals(childInList.getMobile(), whiteListChildInList.getMobile(), "两种方式对集合元素 mobile 的脱敏结果应一致");
            assertEquals(childInList.getEmail(), whiteListChildInList.getEmail(), "两种方式对集合元素 email 的脱敏结果应一致");
            assertNull(child.getRemark(), "desensitize 会先排除非元数据字段，嵌套模型的 remark 应被清空");
            assertNull(childInList.getRemark(), "desensitize 会先排除非元数据字段，集合元素的 remark 应被清空");
            assertEquals("子备注", whiteListChild.getRemark(), "白名单分支不排除非元数据字段，嵌套模型的 remark 应被保留");
            assertEquals("子备注", whiteListChildInList.getRemark(), "白名单分支不排除非元数据字段，集合元素的 remark 应被保留");
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
        @DisplayName("集合类型的脱敏字段保持原值而不是被置空")
        void testDesensitizeCollectionKeepsValue() {
            model.setMobileList(new ArrayList<>(List.of("13800138000", "13900139000")));

            model.desensitize();

            assertNotNull(model.getMobileList(), "集合不应被置空");
            assertEquals(2, model.getMobileList().size(), "集合元素不应丢失");
        }

        @Test
        @DisplayName("正常路径：非字符串的脱敏字段保持原值而不是被置空")
        void testDesensitizeNonStringValue() {
            model.excludeNotMetaAndDesensitize(WHITE_LIST, true);

            assertEquals(1234, model.getSecretNumber(),
                    "非 String 类型无法脱敏，应保持原值。置空会静默销毁数据："
                            + "desensitize 是原地修改实体，flush 回库即造成字段级数据丢失");
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
        @DisplayName("边界值：集合中的 null 元素被跳过，不抛异常")
        void testDesensitizeWithNullElementInCollection() {
            DemoModel valid = new DemoModel().setMobile(MOBILE);
            model.setChildrenWithNull(Arrays.asList(valid, null));

            assertDoesNotThrow(() -> model.excludeNotMetaAndDesensitize(WHITE_LIST, true),
                    "白名单分支会先判空再取集合元素的类，含 null 元素不应抛出 NullPointerException");
            assertEquals(2, model.getChildrenWithNull().size(), "childrenWithNull 集合应保持原有元素个数");
            assertNull(model.getChildrenWithNull().get(1), "集合中的 null 元素应被跳过后原样保留");
            assertEquals("138****8000", valid.getMobile(), "非 null 元素仍会被正常脱敏");
        }

        @Test
        @DisplayName("边界值：集合含 null 元素时其余元素仍被完整脱敏")
        void testDesensitizeWithNullElementInCollectionAndValidElement() {
            DemoModel valid = new DemoModel().setMobile(MOBILE).setEmail(EMAIL);
            model.setChildrenWithNull(Arrays.asList(valid, null));

            assertDoesNotThrow(() -> model.excludeNotMetaAndDesensitize(WHITE_LIST, true),
                    "null 元素被跳过后，其余元素应全部处理完成且不抛出异常");
            assertEquals("138****8000", valid.getMobile(), "位于 null 元素之前的元素应已完成脱敏");
            assertEquals(EMAIL_MASKED, valid.getEmail(), "非 null 元素的其他脱敏字段也应被正常脱敏");
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
        @DisplayName("边界值：白名单为 null 时按空名单处理，不抛异常")
        void testNullWhiteListWithValue() {
            assertDoesNotThrow(() -> model.excludeNotMetaAndDesensitize(null, true),
                    "whiteList 为 null 时源码按空名单处理，不应抛出 NullPointerException");
            assertNull(model.getRemark(), "按空名单处理时走排除非元数据分支，remark 应被清空");
            assertEquals(MOBILE, model.getMobile(), "按空名单处理时不做脱敏，mobile 应保持原值");
        }

        @Test
        @DisplayName("边界值：白名单为 null 且模型全为 null 时不抛异常")
        void testNullWhiteListWithEmptyModel() {
            assertDoesNotThrow(() -> new DemoModel().excludeNotMetaAndDesensitize(null, true),
                    "字段值全为 null 时各分支均直接跳过，不会访问 whiteList，不应抛出异常");
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

    @Nested
    @DisplayName("desensitize 对不同类型的嵌套模型同样生效")
    class NestedDesensitizeTest {

        @Test
        @DisplayName("正常路径：类型不同的嵌套模型的脱敏字段被脱敏")
        void testNestedDifferentClassIsDesensitized() {
            NestedHolder holder = new NestedHolder()
                    .setChild(new ContactModel().setMobile(MOBILE).setRemark("子备注"));

            holder.desensitize();

            assertEquals("138****8000", holder.getChild().getMobile(),
                    "嵌套模型类型与外层不同时，@Desensitize 字段同样必须被脱敏");
            assertNull(holder.getChild().getRemark(),
                    "嵌套模型中的非元数据字段同样应被排除");
        }

        @Test
        @DisplayName("正常路径：类型不同的模型集合元素被脱敏")
        void testCollectionDifferentClassIsDesensitized() {
            ContactModel first = new ContactModel().setMobile(MOBILE);
            ContactModel second = new ContactModel().setMobile("13956789001");
            NestedHolder holder = new NestedHolder()
                    .setChildren(new ArrayList<>(List.of(first, second)));

            holder.desensitize();

            assertEquals("138****8000", first.getMobile(), "集合元素的 mobile 应被脱敏");
            assertEquals("139****9001", second.getMobile(), "集合中每个元素的 mobile 都应被脱敏");
        }

        @Test
        @DisplayName("正常路径：多层嵌套的深层模型被脱敏")
        void testDeeplyNestedIsDesensitized() {
            ContactModel deep = new ContactModel().setMobile(MOBILE);
            ContactModel middle = new ContactModel().setMobile(MOBILE);
            middle.setChildren(new ArrayList<>(List.of(deep)));
            NestedHolder holder = new NestedHolder().setChild(middle);

            holder.desensitize();

            assertEquals("138****8000", middle.getMobile(), "第一层嵌套模型应被脱敏");
            assertEquals("138****8000", deep.getMobile(), "第二层嵌套模型也应被脱敏");
        }

        @Test
        @DisplayName("边界值：自引用模型不会栈溢出")
        void testSelfReferenceDoesNotOverflow() {
            ContactModel self = new ContactModel().setMobile(MOBILE);
            self.setChild(self);
            NestedHolder holder = new NestedHolder().setChild(self);

            assertDoesNotThrow(holder::desensitize, "自引用模型应被已访问集合拦住，不应栈溢出");
            assertEquals("138****8000", self.getMobile(), "自引用模型自身仍应完成脱敏");
        }

        @Test
        @DisplayName("边界值：集合中同时存在 null 元素与非模型元素不抛异常")
        void testMixedCollectionElements() {
            ContactModel valid = new ContactModel().setMobile(MOBILE);
            List<ContactModel> mixed = new ArrayList<>(Arrays.asList(null, valid, null));
            NestedHolder holder = new NestedHolder().setChildren(mixed);

            assertDoesNotThrow(holder::desensitize, "集合中的 null 元素应被跳过");
            assertEquals("138****8000", valid.getMobile(), "有效元素仍应被脱敏");
        }
    }

    /**
     * 外层模型：用于验证与自身类型不同的嵌套模型也能被脱敏
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    @Accessors(chain = true)
    static class NestedHolder extends RootModel<NestedHolder> {
        /**
         * 嵌套模型
         */
        @Meta
        private ContactModel child;

        /**
         * 模型集合
         */
        @Meta
        private List<ContactModel> children;
    }

    /**
     * 联系人模型：含脱敏字段与非元数据字段
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    @Accessors(chain = true)
    static class ContactModel extends RootModel<ContactModel> {
        /**
         * 手机号
         */
        @Meta
        @Desensitize(value = DesensitizeType.MOBILE)
        private String mobile;

        /**
         * 非元数据字段
         */
        private String remark;

        /**
         * 嵌套的联系人
         */
        @Meta
        private ContactModel child;

        /**
         * 联系人集合
         */
        @Meta
        private List<ContactModel> children;
    }

    @Nested
    @DisplayName("Map 容器内的模型也必须被过滤")
    class MapValueTest {

        /**
         * Map 的 key 为模型
         */
        @Test
        @DisplayName("Map<String, 模型> 里的实体应被脱敏")
        void mapValueIsDesensitized() {
            DemoModel child = new DemoModel().setMobile(MOBILE);
            DemoModel root = new DemoModel().setChildrenWithNull(null)
                    .setMobileList(null)
                    .setMapOfChild(java.util.Map.of("k", child));

            root.desensitize();

            assertEquals("138****8000", child.getMobile(),
                    "Map 里的实体的 @Desensitize 字段此前不会被处理，会以明文返回前端");
        }

        /**
         * Map 的 value 为模型
         */
        @Test
        @DisplayName("Map 里实体的非 @Meta 字段应被排除")
        void mapValueExcludesNotMeta() {
            DemoModel child = new DemoModel().setMobile(MOBILE).setRemark("内部备注");
            DemoModel root = new DemoModel().setMapOfChild(java.util.Map.of("k", child));

            root.excludeNotMeta();

            assertNull(child.getRemark(), "Map 里实体的非 @Meta 字段此前不会被排除");
            assertEquals(MOBILE, child.getMobile(), "带 @Meta 的字段应保留");
        }

        @Test
        @DisplayName("Map 里实体的只读字段应被清空")
        void mapValueExcludesReadOnly() {
            DemoModel child = new DemoModel().setCreateTime(1700000000000L);
            DemoModel root = new DemoModel().setMapOfChild(java.util.Map.of("k", child));

            root.excludeReadOnly();

            assertNull(child.getCreateTime(), "Map 里实体的 @ReadOnly 字段此前不会被清空");
        }
    }

    @Nested
    @DisplayName("白名单递归的环检测")
    class CycleTest {

        @Test
        @DisplayName("成环模型在白名单分支下不得 StackOverflowError")
        void cyclicModelDoesNotOverflow() {
            CycleModel a = new CycleModel();
            CycleModel b = new CycleModel();
            a.setName("A").setPeer(b);
            b.setName("B").setPeer(a);

            assertDoesNotThrow(() -> a.excludeNotMetaAndDesensitize(List.of(CycleModel.class), true),
                    "A→B→A 成环。白名单分支此前完全没有环检测，会一路递归到栈溢出；"
                            + "而且每次递归都重新 new 一个 visited 集合，环检测根本不起作用");
        }

        @Test
        @DisplayName("自引用模型在白名单分支下不得 StackOverflowError")
        void selfReferenceDoesNotOverflow() {
            CycleModel a = new CycleModel();
            a.setName("A").setPeer(a);

            assertDoesNotThrow(() -> a.excludeNotMetaAndDesensitize(List.of(CycleModel.class), true),
                    "child == this 的自引用模型同样必须被环检测拦下");
        }

        @Test
        @DisplayName("成环模型在非白名单分支下也不得 StackOverflowError")
        void cyclicModelWithoutWhiteList() {
            CycleModel a = new CycleModel();
            CycleModel b = new CycleModel();
            a.setName("A").setPeer(b);
            b.setName("B").setPeer(a);

            assertDoesNotThrow(() -> a.excludeNotMetaAndDesensitize(List.of(), true),
                    "空白名单走 excludeNotMetaAll 分支，同样依赖 visited 拦环");
        }
    }

    /**
     * 用于验证环检测的模型：{@code peer} 可指向自身或另一个同类模型
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    @Accessors(chain = true)
    static class CycleModel extends RootModel<CycleModel> {
        /**
         * 同类模型
         */
        @Meta
        private CycleModel peer;

        /**
         * 姓名
         */
        @Meta
        private String name;
    }
}
