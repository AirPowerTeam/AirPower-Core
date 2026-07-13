package cn.hamm.airpower.core;

import cn.hamm.airpower.core.annotation.Description;
import cn.hamm.airpower.core.annotation.ReadOnly;
import cn.hamm.airpower.core.exception.ServiceException;
import cn.hamm.airpower.core.interfaces.IFunction;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>ReflectUtil 单元测试</h1>
 *
 * @author Hamm.cn
 */
class ReflectUtilTest {

    // ==================== 测试用例模型 ====================

    /**
     * 基础测试模型
     */
    public static class TestModel extends RootModel<TestModel> {
        @Description("用户ID")
        private Long id;

        @ReadOnly
        @Description("用户名")
        private String username;

        @Description("年龄")
        private Integer age;

        private String noDescriptionField;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public String getNoDescriptionField() {
            return noDescriptionField;
        }

        public void setNoDescriptionField(String noDescriptionField) {
            this.noDescriptionField = noDescriptionField;
        }
    }

    /**
     * 子类测试模型
     */
    public static class ChildModel extends TestModel {
        @Description("子类字段")
        private String childField;

        public String getChildField() {
            return childField;
        }

        public void setChildField(String childField) {
            this.childField = childField;
        }
    }

    /**
     * 带注解的方法测试类
     */
    public static class MethodAnnotationClass {
        @Description("测试方法")
        public void annotatedMethod() {
        }

        public void noAnnotationMethod() {
        }
    }

    // ==================== getFieldGetter 方法测试 ====================

    @Test
    void testGetFieldGetter() throws NoSuchFieldException {
        Field field = TestModel.class.getDeclaredField("username");
        String getter = ReflectUtil.getFieldGetter(field);
        assertEquals("getUsername", getter);
    }

    @Test
    void testGetFieldGetterSingleChar() throws NoSuchFieldException {
        // 测试单字符字段名
        class SingleCharModel {
            private int x;

            @SuppressWarnings("unused")
            public int getX() {
                return x;
            }
        }
        Field field = SingleCharModel.class.getDeclaredField("x");
        String getter = ReflectUtil.getFieldGetter(field);
        assertEquals("getX", getter);
    }

    // ==================== getFieldValue / setFieldValue 方法测试 ====================

    @Test
    void testGetFieldValue() throws NoSuchFieldException {
        TestModel model = new TestModel();
        model.setId(123L);

        Field field = TestModel.class.getDeclaredField("id");
        Object value = ReflectUtil.getFieldValue(model, field);
        assertEquals(123L, value);
    }

    @Test
    void testGetFieldValueWithNullObject() throws NoSuchFieldException {
        // getFieldValue 传入 null 对象会抛出 NullPointerException
        Field field = TestModel.class.getDeclaredField("id");
        assertThrows(NullPointerException.class, () ->
            ReflectUtil.getFieldValue(null, field)
        );
    }

    @Test
    void testSetFieldValue() throws NoSuchFieldException {
        TestModel model = new TestModel();
        Field field = TestModel.class.getDeclaredField("username");

        ReflectUtil.setFieldValue(model, field, "testUser");
        assertEquals("testUser", model.getUsername());
    }

    @Test
    void testSetFieldValueWithNull() throws NoSuchFieldException {
        TestModel model = new TestModel();
        model.setUsername("original");

        Field field = TestModel.class.getDeclaredField("username");
        ReflectUtil.setFieldValue(model, field, null);
        assertNull(model.getUsername());
    }

    // ==================== clearFieldValue 方法测试 ====================

    @Test
    void testClearFieldValue() throws NoSuchFieldException {
        TestModel model = new TestModel();
        model.setAge(25);

        Field field = TestModel.class.getDeclaredField("age");
        ReflectUtil.clearFieldValue(model, field);
        assertNull(model.getAge());
    }

    // ==================== newInstance 方法测试 ====================

    @Test
    void testNewInstance() {
        TestModel instance = ReflectUtil.newInstance(TestModel.class);
        assertNotNull(instance);
        assertTrue(instance instanceof TestModel);
    }

    @Test
    void testNewInstanceWithNoDefaultConstructor() {
        class NoDefaultConstructor extends RootModel<NoDefaultConstructor> {
            public NoDefaultConstructor(String arg) {
            }
        }
        assertThrows(ServiceException.class, () ->
            ReflectUtil.newInstance(NoDefaultConstructor.class)
        );
    }

    // ==================== isTheRootClass 方法测试 ====================

    @Test
    void testIsTheRootClass() {
        assertTrue(ReflectUtil.isTheRootClass(Object.class));
        assertFalse(ReflectUtil.isTheRootClass(TestModel.class));
        assertFalse(ReflectUtil.isTheRootClass(String.class));
    }

    // ==================== getAnnotation (Class) 方法测试 ====================

    @Test
    void testGetAnnotationOnClass() {
        @Description("测试类")
        class AnnotatedClass {
        }

        Description annotation = ReflectUtil.getAnnotation(Description.class, AnnotatedClass.class);
        assertNotNull(annotation);
        assertEquals("测试类", annotation.value());
    }

    @Test
    void testGetAnnotationOnClassNotFound() {
        class NotAnnotatedClass {
        }

        Description annotation = ReflectUtil.getAnnotation(Description.class, NotAnnotatedClass.class);
        assertNull(annotation);
    }

    @Test
    void testGetAnnotationOnClassInheritance() {
        // 子类应该能获取父类的注解（递归查找）
        Description annotation = ReflectUtil.getAnnotation(Description.class, ChildModel.class);
        // ChildModel 本身没有 @Description，但它的父类 TestModel 也没有 @Description
        // 所以这里应该是 null
        assertNull(annotation);
    }

    // ==================== getAnnotation (Field) 方法测试 ====================

    @Test
    void testGetAnnotationOnField() throws NoSuchFieldException {
        Field field = TestModel.class.getDeclaredField("username");
        Description annotation = ReflectUtil.getAnnotation(Description.class, field);
        assertNotNull(annotation);
        assertEquals("用户名", annotation.value());
    }

    @Test
    void testGetAnnotationOnFieldNotFound() throws NoSuchFieldException {
        Field field = TestModel.class.getDeclaredField("noDescriptionField");
        Description annotation = ReflectUtil.getAnnotation(Description.class, field);
        assertNull(annotation);
    }

    // ==================== getAnnotation (Method) 方法测试 ====================

    @Test
    void testGetAnnotationOnMethod() throws NoSuchMethodException {
        Method method = MethodAnnotationClass.class.getMethod("annotatedMethod");
        Description annotation = ReflectUtil.getAnnotation(Description.class, method);
        assertNotNull(annotation);
        assertEquals("测试方法", annotation.value());
    }

    @Test
    void testGetAnnotationOnMethodNotFound() throws NoSuchMethodException {
        Method method = MethodAnnotationClass.class.getMethod("noAnnotationMethod");
        Description annotation = ReflectUtil.getAnnotation(Description.class, method);
        assertNull(annotation);
    }

    // ==================== getDescription 方法测试 ====================

    @Test
    void testGetDescriptionForClass() {
        @Description("测试描述类")
        class DescribedClass {
        }

        String description = ReflectUtil.getDescription(DescribedClass.class);
        assertEquals("测试描述类", description);
    }

    @Test
    void testGetDescriptionForClassWithoutAnnotation() {
        class NoDescribedClass {
        }

        String description = ReflectUtil.getDescription(NoDescribedClass.class);
        assertEquals("NoDescribedClass", description);
    }

    @Test
    void testGetDescriptionForField() throws NoSuchFieldException {
        Field field = TestModel.class.getDeclaredField("username");
        String description = ReflectUtil.getDescription(field);
        assertEquals("用户名", description);
    }

    @Test
    void testGetDescriptionForFieldWithoutAnnotation() throws NoSuchFieldException {
        Field field = TestModel.class.getDeclaredField("noDescriptionField");
        String description = ReflectUtil.getDescription(field);
        assertEquals("noDescriptionField", description);
    }

    @Test
    void testGetDescriptionForMethod() throws NoSuchMethodException {
        Method method = MethodAnnotationClass.class.getMethod("annotatedMethod");
        String description = ReflectUtil.getDescription(method);
        assertEquals("测试方法", description);
    }

    @Test
    void testGetDescriptionForMethodWithoutAnnotation() throws NoSuchMethodException {
        Method method = MethodAnnotationClass.class.getMethod("noAnnotationMethod");
        String description = ReflectUtil.getDescription(method);
        assertEquals("noAnnotationMethod", description);
    }

    @Test
    void testGetDescriptionForParameter() throws NoSuchMethodException {
        class ParameterClass {
            public void method(@Description("参数描述") String param) {
            }
        }

        Method method = ParameterClass.class.getMethod("method", String.class);
        Parameter parameter = method.getParameters()[0];
        String description = ReflectUtil.getDescription(parameter);
        assertEquals("参数描述", description);
    }

    @Test
    void testGetDescriptionForParameterWithoutAnnotation() throws NoSuchMethodException {
        class ParameterClass {
            public void method(String param) {
            }
        }

        Method method = ParameterClass.class.getMethod("method", String.class);
        Parameter parameter = method.getParameters()[0];
        String description = ReflectUtil.getDescription(parameter);
        // 使用 -parameters 编译参数时参数名会保留，否则为 arg0
        assertTrue(description.equals("param") || description.equals("arg0"));
    }

    // ==================== getFieldList 方法测试 ====================

    @Test
    void testGetFieldList() {
        List<Field> fields = ReflectUtil.getFieldList(TestModel.class);
        assertNotNull(fields);
        assertFalse(fields.isEmpty());

        // 应该包含 id, username, age, noDescriptionField
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("id")));
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("username")));
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("age")));
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("noDescriptionField")));
    }

    @Test
    void testGetFieldListWithInheritance() {
        List<Field> fields = ReflectUtil.getFieldList(ChildModel.class);
        assertNotNull(fields);

        // 应该包含父类和子类的字段
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("id")));
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("childField")));
    }

    @Test
    void testGetFieldListWithNull() {
        ServiceException exception = assertThrows(ServiceException.class, () ->
            ReflectUtil.getFieldList(null)
        );
        assertEquals("无法获取 null 的字段列表", exception.getMessage());
    }

    @Test
    void testGetFieldListCache() {
        // 测试缓存机制 - 两次获取应该是同一个列表
        List<Field> fields1 = ReflectUtil.getFieldList(TestModel.class);
        List<Field> fields2 = ReflectUtil.getFieldList(TestModel.class);
        assertSame(fields1, fields2);
    }

    // ==================== getDeclaredFields 方法测试 ====================

    @Test
    void testGetDeclaredFields() {
        Field[] fields = ReflectUtil.getDeclaredFields(TestModel.class);
        assertNotNull(fields);
        assertTrue(fields.length > 0);
    }

    @Test
    void testGetDeclaredFieldsCache() {
        Field[] fields1 = ReflectUtil.getDeclaredFields(TestModel.class);
        Field[] fields2 = ReflectUtil.getDeclaredFields(TestModel.class);
        assertSame(fields1, fields2);
    }

    // ==================== getLambdaFunctionName 方法测试 ====================

    @Test
    void testGetLambdaFunctionName() {
        IFunction<TestModel, Long> lambda = TestModel::getId;
        String functionName = ReflectUtil.getLambdaFunctionName(lambda);
        assertEquals("Id", functionName);
    }

    @Test
    void testGetLambdaFunctionNameWithBoolean() {
        class BooleanModel {
            private boolean active;

            @SuppressWarnings("unused")
            public boolean isActive() {
                return active;
            }
        }

        IFunction<BooleanModel, Boolean> lambda = BooleanModel::isActive;
        String functionName = ReflectUtil.getLambdaFunctionName(lambda);
        // isActive 的 SerializedLambda 方法名是 isActive，替换 get 后变为 isActive（因为没有 get 前缀）
        assertEquals("isActive", functionName);
    }

    // ==================== getField 方法测试 ====================

    @Test
    void testGetField() {
        Field field = ReflectUtil.getField("username", TestModel.class);
        assertNotNull(field);
        assertEquals("username", field.getName());
    }

    @Test
    void testGetFieldInheritance() {
        Field field = ReflectUtil.getField("id", ChildModel.class);
        assertNotNull(field);
        assertEquals("id", field.getName());
        // id 字段在 TestModel（父类）中定义
        assertEquals(TestModel.class, field.getDeclaringClass());
    }

    @Test
    void testGetFieldNotFound() {
        Field field = ReflectUtil.getField("nonExistentField", TestModel.class);
        assertNull(field);
    }

    @Test
    void testGetFieldWithNullClass() {
        Field field = ReflectUtil.getField("username", null);
        assertNull(field);
    }

    @Test
    void testGetFieldWithObjectClass() {
        Field field = ReflectUtil.getField("username", Object.class);
        assertNull(field);
    }

    // ==================== 边界条件测试 ====================

    @Test
    void testGetFieldValueWithPrivateField() throws NoSuchFieldException {
        TestModel model = new TestModel();
        model.setUsername("privateValue");

        Field field = TestModel.class.getDeclaredField("username");
        Object value = ReflectUtil.getFieldValue(model, field);
        assertEquals("privateValue", value);
    }

    @Test
    void testSetFieldValueWithPrivateField() throws NoSuchFieldException {
        TestModel model = new TestModel();
        Field field = TestModel.class.getDeclaredField("username");

        ReflectUtil.setFieldValue(model, field, "newValue");
        assertEquals("newValue", model.getUsername());
    }

    @Test
    void testGetFieldListExcludesStaticAndTransient() {
        class ModelWithStaticAndTransient {
            private String normalField;
            private static String staticField;
            private transient String transientField;
        }

        List<Field> fields = ReflectUtil.getFieldList(ModelWithStaticAndTransient.class);
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("normalField")));
        assertFalse(fields.stream().anyMatch(f -> f.getName().equals("staticField")));
        assertFalse(fields.stream().anyMatch(f -> f.getName().equals("transientField")));
    }
}
