package cn.hamm.airpower.core;

import cn.hamm.airpower.core.annotation.Desensitize;
import cn.hamm.airpower.core.annotation.Meta;
import cn.hamm.airpower.core.annotation.ReadOnly;
import cn.hamm.airpower.core.enums.DesensitizeType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>RootModel 单元测试</h1>
 *
 * @author Hamm.cn
 */
class RootModelTest {

    // ==================== 测试用例模型（顶层内部类）====================

    /**
     * 基础测试模型
     */
    public static class TestModel extends RootModel<TestModel> {
        @Meta
        private String metaField = "meta";

        @ReadOnly
        private String readOnlyField = "readOnly";

        @Desensitize(value = DesensitizeType.MOBILE, head = 3, tail = 4)
        private String mobile = "13800138000";

        @Desensitize(value = DesensitizeType.ID_CARD, head = 6, tail = 4)
        private String idCard = "110101199001011234";

        @Desensitize(value = DesensitizeType.CUSTOM, head = 2, tail = 2, symbol = "#", replace = true)
        private String customReplace = "custom";

        private String normalField = "normal";

        private String nullField = null;

        public String getMetaField() {
            return metaField;
        }

        public void setMetaField(String metaField) {
            this.metaField = metaField;
        }

        public String getReadOnlyField() {
            return readOnlyField;
        }

        public void setReadOnlyField(String readOnlyField) {
            this.readOnlyField = readOnlyField;
        }

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }

        public String getIdCard() {
            return idCard;
        }

        public void setIdCard(String idCard) {
            this.idCard = idCard;
        }

        public String getCustomReplace() {
            return customReplace;
        }

        public void setCustomReplace(String customReplace) {
            this.customReplace = customReplace;
        }

        public String getNormalField() {
            return normalField;
        }

        public void setNormalField(String normalField) {
            this.normalField = normalField;
        }

        public String getNullField() {
            return nullField;
        }

        public void setNullField(String nullField) {
            this.nullField = nullField;
        }
    }

    /**
     * 嵌套模型测试
     */
    public static class ParentModel extends RootModel<ParentModel> {
        @Meta
        private String parentMeta = "parentMeta";

        private TestModel child = new TestModel();

        public String getParentMeta() {
            return parentMeta;
        }

        public void setParentMeta(String parentMeta) {
            this.parentMeta = parentMeta;
        }

        public TestModel getChild() {
            return child;
        }

        public void setChild(TestModel child) {
            this.child = child;
        }
    }

    /**
     * 集合模型测试
     */
    public static class CollectionModel extends RootModel<CollectionModel> {
        @Meta
        private String meta = "meta";

        private List<TestModel> items = List.of(new TestModel(), new TestModel());

        public String getMeta() {
            return meta;
        }

        public void setMeta(String meta) {
            this.meta = meta;
        }

        public List<TestModel> getItems() {
            return items;
        }

        public void setItems(List<TestModel> items) {
            this.items = items;
        }
    }

    /**
     * 无ReadOnly字段模型
     */
    public static class NoReadOnlyModel extends RootModel<NoReadOnlyModel> {
        private String field = "value";

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }
    }

    /**
     * 空字符串脱敏模型
     */
    public static class EmptyDesensitizeModel extends RootModel<EmptyDesensitizeModel> {
        @Desensitize(value = DesensitizeType.MOBILE)
        private String emptyMobile = "";

        public String getEmptyMobile() {
            return emptyMobile;
        }

        public void setEmptyMobile(String emptyMobile) {
            this.emptyMobile = emptyMobile;
        }
    }

    /**
     * 短字符串脱敏模型
     */
    public static class ShortDesensitizeModel extends RootModel<ShortDesensitizeModel> {
        @Desensitize(value = DesensitizeType.MOBILE)
        private String shortMobile = "12";

        public String getShortMobile() {
            return shortMobile;
        }

        public void setShortMobile(String shortMobile) {
            this.shortMobile = shortMobile;
        }
    }

    /**
     * null子模型
     */
    public static class NullChildModel extends RootModel<NullChildModel> {
        @Meta
        private String meta = "meta";

        private TestModel child = null;

        public String getMeta() {
            return meta;
        }

        public void setMeta(String meta) {
            this.meta = meta;
        }

        public TestModel getChild() {
            return child;
        }

        public void setChild(TestModel child) {
            this.child = child;
        }
    }

    /**
     * 空集合模型
     */
    public static class EmptyCollectionModel extends RootModel<EmptyCollectionModel> {
        @Meta
        private String meta = "meta";

        private List<TestModel> items = List.of();

        public String getMeta() {
            return meta;
        }

        public void setMeta(String meta) {
            this.meta = meta;
        }

        public List<TestModel> getItems() {
            return items;
        }

        public void setItems(List<TestModel> items) {
            this.items = items;
        }
    }

    // ==================== isModel 方法测试 ====================

    @Test
    void testIsModelWithRootModel() {
        assertTrue(RootModel.isModel(RootModel.class));
    }

    @Test
    void testIsModelWithSubclass() {
        assertTrue(RootModel.isModel(TestModel.class));
    }

    @Test
    void testIsModelWithNonModel() {
        assertFalse(RootModel.isModel(String.class));
    }

    @Test
    void testIsModelWithNull() {
        assertFalse(RootModel.isModel(null));
    }

    // ==================== excludeReadOnly 方法测试 ====================

    @Test
    void testExcludeReadOnly() {
        TestModel model = new TestModel();
        model.excludeReadOnly();

        assertNull(model.getReadOnlyField());
        assertNotNull(model.getMetaField());
        assertNotNull(model.getNormalField());
    }

    @Test
    void testExcludeReadOnlyWithNoReadOnlyFields() {
        NoReadOnlyModel model = new NoReadOnlyModel();
        model.excludeReadOnly();
        assertEquals("value", model.getField());
    }

    // ==================== desensitize 方法测试 ====================

    @Test
    void testDesensitizeMobile() {
        TestModel model = new TestModel();
        model.desensitize();

        // 手机号：保留前3后4，中间脱敏
        assertEquals("138****8000", model.getMobile());
    }

    @Test
    void testDesensitizeIdCard() {
        TestModel model = new TestModel();
        model.desensitize();

        // 身份证号：保留前6后4，中间脱敏
        assertEquals("110101********1234", model.getIdCard());
    }

    @Test
    void testDesensitizeCustomReplace() {
        TestModel model = new TestModel();
        model.desensitize();

        // 自定义替换：整体替换为符号
        assertEquals("#", model.getCustomReplace());
    }

    @Test
    void testDesensitizeNullField() {
        TestModel model = new TestModel();
        model.desensitize();

        // null字段不脱敏
        assertNull(model.getNullField());
    }

    @Test
    void testDesensitizeNormalField() {
        TestModel model = new TestModel();
        model.desensitize();

        // 非脱敏字段保持不变
        assertEquals("normal", model.getNormalField());
    }

    // ==================== excludeNotMeta 方法测试 ====================

    @Test
    void testExcludeNotMeta() {
        TestModel model = new TestModel();
        model.excludeNotMeta();

        // excludeNotMeta() 的白名单包含当前类，所以非Meta字段不会被清空
        assertNotNull(model.getMetaField());
        assertNotNull(model.getNormalField());
        assertNotNull(model.getReadOnlyField());
    }

    @Test
    void testExcludeNotMetaWithWhitelist() {
        TestModel model = new TestModel();
        model.excludeNotMeta(List.of(TestModel.class));

        // 白名单包含当前类，非Meta字段保留
        assertNotNull(model.getNormalField());
        assertNotNull(model.getReadOnlyField());
    }

    @Test
    void testExcludeNotMetaWithEmptyWhitelist() {
        TestModel model = new TestModel();
        model.excludeNotMeta(List.of());

        // 白名单为空时，代码不进入 excludeFieldValueNotMeta 分支
        // 非Meta字段不会被清空（因为没有触发排除逻辑）
        assertNotNull(model.getMetaField());
        assertNotNull(model.getNormalField());
        assertNotNull(model.getReadOnlyField());
    }

    // ==================== 嵌套模型测试 ====================

    @Test
    void testDesensitizeNestedModel() {
        ParentModel parent = new ParentModel();
        parent.desensitize();

        // 嵌套的TestModel也应该被脱敏
        assertNotNull(parent.getChild());
        assertEquals("138****8000", parent.getChild().getMobile());
    }

    @Test
    void testExcludeNotMetaNestedModel() {
        ParentModel parent = new ParentModel();
        parent.excludeNotMeta();

        // 嵌套模型的非Meta字段应该被清空
        assertNotNull(parent.getParentMeta());
        assertNull(parent.getChild().getNormalField());
    }

    // ==================== 集合模型测试 ====================

    @Test
    void testDesensitizeCollection() {
        CollectionModel model = new CollectionModel();
        model.desensitize();

        // 集合中的每个元素都应该被脱敏
        for (TestModel item : model.getItems()) {
            assertEquals("138****8000", item.getMobile());
        }
    }

    @Test
    void testExcludeNotMetaCollection() {
        CollectionModel model = new CollectionModel();
        model.excludeNotMeta();

        // 集合中的每个元素的非Meta字段应该被清空
        for (TestModel item : model.getItems()) {
            assertNull(item.getNormalField());
        }
    }

    // ==================== 边界条件测试 ====================

    @Test
    void testDesensitizeEmptyString() {
        EmptyDesensitizeModel model = new EmptyDesensitizeModel();
        model.desensitize();
        // 空字符串不脱敏（长度不足）
        assertEquals("", model.getEmptyMobile());
    }

    @Test
    void testDesensitizeShortString() {
        ShortDesensitizeModel model = new ShortDesensitizeModel();
        model.desensitize();
        // 短字符串不脱敏（长度不足）
        assertEquals("12", model.getShortMobile());
    }

    @Test
    void testExcludeNotMetaWithNullChild() {
        NullChildModel model = new NullChildModel();
        // 不应该抛出异常
        assertDoesNotThrow(() -> model.excludeNotMeta());
        assertEquals("meta", model.getMeta());
    }

    @Test
    void testExcludeNotMetaWithEmptyCollection() {
        EmptyCollectionModel model = new EmptyCollectionModel();
        // 不应该抛出异常
        assertDoesNotThrow(() -> model.excludeNotMeta());
        assertEquals("meta", model.getMeta());
    }
}
