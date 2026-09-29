package cn.hamm.airpower.core;

import cn.hamm.airpower.core.annotation.Description;
import cn.hamm.airpower.core.annotation.Meta;
import cn.hamm.airpower.core.exception.ServiceException;
import cn.hamm.airpower.core.fixture.DemoModel;
import cn.hamm.airpower.core.fixture.DemoTree;
import cn.hamm.airpower.core.fixture.Gender;
import cn.hamm.airpower.core.interfaces.IFunction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>{@link ReflectUtil} 的单元测试</h1>
 *
 * <p>覆盖：Getter 名推导、字段读写、实例化、根类判断、四个 {@code getAnnotation} 重载、
 * 四个 {@code getDescription} 重载、字段列表缓存、声明字段缓存、Lambda 方法名、递归查找字段。</p>
 *
 * @author Hamm.cn
 */
@DisplayName("ReflectUtil 反射工具类")
class ReflectUtilTest {

    /**
     * 反射测试用的方法签名
     */
    private static Method childMethod() throws NoSuchMethodException {
        return Child.class.getDeclaredMethod("childMethod", String.class);
    }

    /**
     * 反射测试用的字段
     */
    private static Field childField() throws NoSuchFieldException {
        return Child.class.getDeclaredField("childField");
    }

    /**
     * 带类级 {@code @Description} 的父类
     */
    @Description("父类")
    static class Parent {
        /**
         * 父类静态字段
         */
        protected static String parentStaticField = "父类静态字段";
        /**
         * 父类字段
         */
        @Description("父类字段")
        protected String parentField;

        /**
         * 父类方法
         */
        @Description("父类方法")
        public void parentMethod() {
        }

        /**
         * 父类无注解方法
         */
        public void parentPlainMethod() {
        }
    }

    /**
     * 子类，继承 {@link Parent}
     */
    static class Child extends Parent {
        /**
         * 子类字段
         */
        @Description("子类字段")
        private String childField;

        /**
         * 子类方法
         *
         * @param name 参数名
         */
        @Description("子类方法")
        public void childMethod(@Description("参数名") String name) {
        }

        /**
         * 子类无注解方法
         *
         * @param age 年龄
         */
        public void childPlainMethod(int age) {
        }
    }

    /**
     * 没有任何注解的类
     */
    static class Plain {
    }

    /**
     * 用于测试字段读写的容器
     */
    static class FieldHolder {
        /**
         * 静态常量字段
         */
        static final int CONST = 5;
        /**
         * 静态字段
         */
        static String stat = "static";
        /**
         * 不可变字段
         */
        private final String finalField = "final";
        /**
         * 瞬态字段
         */
        transient String temp;
        /**
         * 普通字段
         */
        String mutable;
        /**
         * 大写首字母字段
         */
        String URL;
        /**
         * 单字符字段
         */
        String a;
        /**
         * 私有字段
         */
        private String hidden;

        /**
         * 读取私有字段
         *
         * @return 私有字段值
         */
        public String getHidden() {
            return hidden;
        }
    }

    /**
     * 用于测试非 {@code String} 字段读写的容器
     */
    static class RefHolder {
        /**
         * 集合字段
         */
        List<String> list;
        /**
         * 基本类型字段
         */
        int number;
    }

    /**
     * 只有有参构造器的模型
     */
    static class WithArgsModel extends RootModel<WithArgsModel> {
        /**
         * 有参构造器
         *
         * @param name 名称
         */
        WithArgsModel(String name) {
        }
    }

    /**
     * 用于验证字段重载不递归的父类
     */
    static class ShadowParent {
        /**
         * 带注解的字段
         */
        @Description("被遮蔽的字段")
        protected String name;
    }

    /**
     * 用同名字段遮蔽父类字段的子类
     */
    static class ShadowChild extends ShadowParent {
        /**
         * 与父类同名的字段，自身没有注解
         */
        @SuppressWarnings("unused")
        private String name;
    }

    /**
     * 抽象模型
     */
    abstract static class AbstractModel extends RootModel<AbstractModel> {
    }

    /**
     * 方法名中带有 "get" 的目标类，用于验证 Lambda 方法名只去掉 get 前缀
     */
    static class Targeter {
        /**
         * 以 get 为前缀的方法名
         *
         * @return 固定值
         */
        public String getTarget() {
            return "target";
        }

        /**
         * 去掉 get 前缀后，剩余部分仍含 get 字样的方法名
         *
         * @return 固定值
         */
        public String getForgetLabel() {
            return "forgetLabel";
        }
    }

    @Nested
    @DisplayName("getFieldGetter 推导 Getter 名")
    class GetFieldGetterTest {

        @Test
        @DisplayName("首字母小写的字段名首字母大写并加 get 前缀")
        void normalFieldName() throws NoSuchFieldException {
            assertEquals("getMutable", ReflectUtil.getFieldGetter(FieldHolder.class.getDeclaredField("mutable")),
                    "普通字段名应转换为 get + 首字母大写");
        }

        @Test
        @DisplayName("首字母已大写的字段名保持原样拼接")
        void upperCaseFieldName() throws NoSuchFieldException {
            assertEquals("getURL", ReflectUtil.getFieldGetter(FieldHolder.class.getDeclaredField("URL")),
                    "首字母已大写的字段名不应重复大写");
        }

        @Test
        @DisplayName("单字符字段名也能正确推导")
        void singleCharFieldName() throws NoSuchFieldException {
            assertEquals("getA", ReflectUtil.getFieldGetter(FieldHolder.class.getDeclaredField("a")),
                    "单字符字段名应转换为 getA");
        }

        @Test
        @DisplayName("静态字段同样可以推导 Getter 名")
        void staticFieldName() throws NoSuchFieldException {
            assertEquals("getStat", ReflectUtil.getFieldGetter(FieldHolder.class.getDeclaredField("stat")),
                    "静态字段名同样按 Getter 规则推导");
        }

        @Test
        @DisplayName("实际存在的 Getter 可与推导结果对应")
        void getterMatchesRealMethod() throws NoSuchFieldException, NoSuchMethodException {
            Field field = FieldHolder.class.getDeclaredField("hidden");
            String getter = ReflectUtil.getFieldGetter(field);
            assertNotNull(FieldHolder.class.getMethod(getter), "推导出的 Getter 名应能对应到真实方法");
        }
    }

    @Nested
    @DisplayName("getFieldValue 读取字段值")
    class GetFieldValueTest {

        @Test
        @DisplayName("可读取私有字段的值")
        void readPrivateField() throws NoSuchFieldException, IllegalAccessException {
            FieldHolder holder = new FieldHolder();
            Field field = FieldHolder.class.getDeclaredField("hidden");
            field.setAccessible(true);
            field.set(holder, "隐藏值");
            assertEquals("隐藏值", ReflectUtil.getFieldValue(holder, field), "私有字段的值应能被读取");
        }

        @Test
        @DisplayName("可读取继承自父类的字段")
        void readInheritedField() throws NoSuchFieldException {
            Child child = new Child();
            Field field = Parent.class.getDeclaredField("parentField");
            ReflectUtil.setFieldValue(child, field, "父类值");
            assertEquals("父类值", ReflectUtil.getFieldValue(child, field), "子类实例上的父类字段应能读取");
        }

        @Test
        @DisplayName("可读取静态字段且无需传入实例")
        void readStaticField() throws NoSuchFieldException {
            Field field = FieldHolder.class.getDeclaredField("stat");
            assertEquals("static", ReflectUtil.getFieldValue(null, field), "静态字段传 null 实例也应能读取");
            assertEquals("static", ReflectUtil.getFieldValue(new FieldHolder(), field), "静态字段传实例同样能读取");
        }

        @Test
        @DisplayName("未赋值的字段返回 null")
        void readNullValue() throws NoSuchFieldException {
            assertNull(ReflectUtil.getFieldValue(new FieldHolder(), FieldHolder.class.getDeclaredField("mutable")),
                    "未赋值的字段应返回 null");
        }

        @Test
        @DisplayName("对象为 null 且字段是实例字段时抛空指针（源码未做判空）")
        void readInstanceFieldOnNullObjectThrowsNpe() throws NoSuchFieldException {
            Field field = FieldHolder.class.getDeclaredField("mutable");
            assertThrows(NullPointerException.class, () -> ReflectUtil.getFieldValue(null, field),
                    "实例字段传 null 对象时源码未做判空，应抛空指针而不是返回 null");
        }
    }

    @Nested
    @DisplayName("setFieldValue 与 clearFieldValue 写入字段值")
    class SetFieldValueTest {

        @Test
        @DisplayName("写入私有字段后可读回")
        void setPrivateField() throws NoSuchFieldException {
            FieldHolder holder = new FieldHolder();
            Field field = FieldHolder.class.getDeclaredField("hidden");
            ReflectUtil.setFieldValue(holder, field, "新值");
            assertEquals("新值", holder.getHidden(), "写入私有字段后应能通过真实 Getter 读到");
        }

        @Test
        @DisplayName("可写入集合与基本类型字段")
        void setObjectField() throws NoSuchFieldException {
            RefHolder holder = new RefHolder();
            Field listField = RefHolder.class.getDeclaredField("list");
            Field numberField = RefHolder.class.getDeclaredField("number");
            ReflectUtil.setFieldValue(holder, listField, List.of("a", "b"));
            ReflectUtil.setFieldValue(holder, numberField, 7);
            assertEquals(List.of("a", "b"), ReflectUtil.getFieldValue(holder, listField), "集合字段应被正确写入");
            assertEquals(7, ReflectUtil.getFieldValue(holder, numberField), "基本类型字段应被正确写入");
        }

        @Test
        @DisplayName("写入类型不匹配的值抛 IllegalArgumentException（源码仅捕获 IllegalAccessException）")
        void setWrongTypeThrows() throws NoSuchFieldException {
            RefHolder holder = new RefHolder();
            Field listField = RefHolder.class.getDeclaredField("list");
            assertThrows(IllegalArgumentException.class, () -> ReflectUtil.setFieldValue(holder, listField, "不是集合"),
                    "写入类型不匹配的值时源码未做转换，应抛 IllegalArgumentException 而不是被吞掉");
        }

        @Test
        @DisplayName("clearFieldValue 将字段置为 null")
        void clearFieldValue() throws NoSuchFieldException {
            FieldHolder holder = new FieldHolder();
            Field field = FieldHolder.class.getDeclaredField("mutable");
            ReflectUtil.setFieldValue(holder, field, "值");
            ReflectUtil.clearFieldValue(holder, field);
            assertNull(ReflectUtil.getFieldValue(holder, field), "clearFieldValue 后字段值应为 null");
        }

        @Test
        @DisplayName("clearFieldValue 对静态字段同样生效")
        void clearStaticField() throws NoSuchFieldException {
            Field field = FieldHolder.class.getDeclaredField("stat");
            ReflectUtil.setFieldValue(null, field, "待清空");
            ReflectUtil.clearFieldValue(null, field);
            assertNull(ReflectUtil.getFieldValue(null, field), "静态字段也应能被清空");
            ReflectUtil.setFieldValue(null, field, "static");
            assertEquals("static", ReflectUtil.getFieldValue(null, field), "测试结束后应恢复静态字段的原值");
        }

        @Test
        @DisplayName("写入 static final 字段失败时只记录日志不抛异常")
        void setStaticFinalFieldIsSilentlyIgnored() throws NoSuchFieldException {
            Field field = FieldHolder.class.getDeclaredField("CONST");
            assertDoesNotThrow(() -> ReflectUtil.setFieldValue(null, field, 9),
                    "写入 static final 字段失败时源码只记录日志，不应抛异常");
            assertEquals(5, ReflectUtil.getFieldValue(null, field), "写入 static final 字段失败后原值应保持不变");
        }

        @Test
        @DisplayName("写入实例 final 字段在 setAccessible 后可成功")
        void setInstanceFinalField() throws NoSuchFieldException {
            FieldHolder holder = new FieldHolder();
            Field field = FieldHolder.class.getDeclaredField("finalField");
            ReflectUtil.setFieldValue(holder, field, "被修改");
            assertEquals("被修改", ReflectUtil.getFieldValue(holder, field), "setAccessible 之后实例 final 字段可以被修改");
        }
    }

    @Nested
    @DisplayName("newInstance 创建实例")
    class NewInstanceTest {

        @Test
        @DisplayName("无参构造的模型可正常创建")
        void createNormalInstance() {
            DemoModel model = ReflectUtil.newInstance(DemoModel.class);
            assertNotNull(model, "无参构造的模型应能创建出实例");
            assertEquals(DemoModel.class, model.getClass(), "创建出的实例类型应与传入的类一致");
            assertNull(model.getId(), "新建实例的字段应为默认值 null");
        }

        @Test
        @DisplayName("只有有参构造器的类创建失败并抛 ServiceException")
        void createInstanceWithArgsCtorFails() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> ReflectUtil.newInstance(WithArgsModel.class),
                    "没有无参构造器的类应抛出 ServiceException");
            assertTrue(exception.getMessage().startsWith("创建新实例失败，"),
                    "实例化失败异常信息应以「创建新实例失败，」开头");
            assertTrue(exception.getMessage().contains(WithArgsModel.class.getName()),
                    "实例化失败异常信息中应包含类名");
        }

        @Test
        @DisplayName("抽象类创建失败并抛 ServiceException")
        void createAbstractInstanceFails() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> ReflectUtil.newInstance(AbstractModel.class),
                    "抽象类应抛出 ServiceException");
            assertTrue(exception.getMessage().startsWith("创建新实例失败，"),
                    "抽象类实例化失败异常信息应以「创建新实例失败，」开头");
        }
    }

    @Nested
    @DisplayName("isTheRootClass 判断根类")
    class IsTheRootClassTest {

        @Test
        @DisplayName("Object 是根类")
        void objectIsRootClass() {
            assertTrue(ReflectUtil.isTheRootClass(Object.class), "Object 类应被判定为根类");
        }

        @Test
        @DisplayName("其他类都不是根类")
        void otherClassesAreNotRoot() {
            assertFalse(ReflectUtil.isTheRootClass(String.class), "String 不应是根类");
            assertFalse(ReflectUtil.isTheRootClass(DemoModel.class), "业务模型不应是根类");
            assertFalse(ReflectUtil.isTheRootClass(Plain.class), "普通测试类不应是根类");
        }

        @Test
        @DisplayName("传入 null 抛空指针（源码未做判空）")
        void nullClassThrowsNpe() {
            assertThrows(NullPointerException.class, () -> ReflectUtil.isTheRootClass(null),
                    "isTheRootClass(null) 源码未做判空，应抛空指针");
        }
    }

    @Nested
    @DisplayName("getAnnotation(注解, 方法)")
    class GetAnnotationFromMethodTest {

        @Test
        @DisplayName("方法上有注解时可取到")
        void annotationOnMethod() throws NoSuchMethodException {
            Description description = ReflectUtil.getAnnotation(Description.class, childMethod());
            assertNotNull(description, "带 @Description 的方法应能取到注解");
            assertEquals("子类方法", description.value(), "取到的注解值应为「子类方法」");
        }

        @Test
        @DisplayName("方法上没有注解时返回 null")
        void noAnnotationOnMethod() throws NoSuchMethodException {
            Method method = Child.class.getDeclaredMethod("childPlainMethod", int.class);
            assertNull(ReflectUtil.getAnnotation(Description.class, method), "没有 @Description 的方法应返回 null");
        }

        @Test
        @DisplayName("Object 的方法返回 null")
        void objectMethodReturnsNull() throws NoSuchMethodException {
            assertNull(ReflectUtil.getAnnotation(Description.class, Object.class.getDeclaredMethod("toString")),
                    "Object 的方法不应取到任何注解");
        }

        @Test
        @DisplayName("可以取到非 Description 类型的注解")
        void otherAnnotationType() throws NoSuchMethodException {
            assertNotNull(ReflectUtil.getAnnotation(Meta.class, DemoModel.class.getMethod("getTitle")),
                    "Getter 上的 @Meta 应能通过方法重载取到");
        }
    }

    @Nested
    @DisplayName("getAnnotation(注解, 类)")
    class GetAnnotationFromClassTest {

        @Test
        @DisplayName("类上有注解时可取到")
        void annotationOnClass() {
            Description description = ReflectUtil.getAnnotation(Description.class, Parent.class);
            assertNotNull(description, "带 @Description 的类应能取到注解");
            assertEquals("父类", description.value(), "取到的注解值应为「父类」");
        }

        @Test
        @DisplayName("子类可沿继承链取到父类注解")
        void annotationFromSuperClass() {
            Description description = ReflectUtil.getAnnotation(Description.class, Child.class);
            assertNotNull(description, "子类应能沿继承链取到父类的 @Description");
            assertEquals("父类", description.value(), "子类取到的注解值应为父类的描述");
        }

        @Test
        @DisplayName("未标注的类返回 null")
        void noAnnotationOnClass() {
            assertNull(ReflectUtil.getAnnotation(Description.class, Plain.class), "没有 @Description 的类应返回 null");
            assertNull(ReflectUtil.getAnnotation(Description.class, DemoTree.class), "DemoTree 没有 @Description，应返回 null");
        }

        @Test
        @DisplayName("Object 类返回 null")
        void objectClassReturnsNull() {
            assertNull(ReflectUtil.getAnnotation(Description.class, Object.class), "Object 类应返回 null");
        }
    }

    @Nested
    @DisplayName("getAnnotation(注解, 字段)")
    class GetAnnotationFromFieldTest {

        @Test
        @DisplayName("字段上有注解时可取到")
        void annotationOnField() throws NoSuchFieldException {
            Description description = ReflectUtil.getAnnotation(Description.class, childField());
            assertNotNull(description, "带 @Description 的字段应能取到注解");
            assertEquals("子类字段", description.value(), "取到的注解值应为「子类字段」");
        }

        @Test
        @DisplayName("字段上没有注解时返回 null")
        void noAnnotationOnField() throws NoSuchFieldException {
            assertNull(ReflectUtil.getAnnotation(Description.class, FieldHolder.class.getDeclaredField("mutable")),
                    "没有 @Description 的字段应返回 null");
        }

        @Test
        @DisplayName("字段重载不沿父类递归")
        void fieldOverloadDoesNotRecurse() throws NoSuchFieldException {
            Field shadowField = ShadowChild.class.getDeclaredField("name");
            assertNull(ReflectUtil.getAnnotation(Description.class, shadowField),
                    "字段重载只读本字段自身的注解，不应回溯到父类同名字段");
            assertNotNull(ReflectUtil.getAnnotation(Description.class, ShadowParent.class.getDeclaredField("name")),
                    "父类的同名字段自身带注解，应能直接取到");
        }
    }

    @Nested
    @DisplayName("getAnnotation(注解, 类, 方法名, 参数类型)")
    class GetAnnotationByNameTest {

        @Test
        @DisplayName("本类方法上的注解可取到")
        void ownMethod() {
            Description description = ReflectUtil.getAnnotation(Description.class, Child.class,
                    "childMethod", new Class<?>[]{String.class});
            assertNotNull(description, "本类方法上的 @Description 应能通过方法名取到");
            assertEquals("子类方法", description.value(), "取到的注解值应为「子类方法」");
        }

        @Test
        @DisplayName("父类方法上的注解可沿继承链取到")
        void parentMethod() {
            Description description = ReflectUtil.getAnnotation(Description.class, Child.class,
                    "parentMethod", new Class<?>[]{});
            assertNotNull(description, "父类方法上的 @Description 应能沿继承链取到");
            assertEquals("父类方法", description.value(), "取到的注解值应为「父类方法」");
        }

        @Test
        @DisplayName("方法不存在时返回 null")
        void methodNotFound() {
            assertNull(ReflectUtil.getAnnotation(Description.class, Child.class, "notExistMethod", new Class<?>[]{}),
                    "不存在的方法应返回 null");
        }

        @Test
        @DisplayName("方法存在但没有注解时返回 null")
        void methodWithoutAnnotation() {
            assertNull(ReflectUtil.getAnnotation(Description.class, Child.class,
                            "childPlainMethod", new Class<?>[]{int.class}),
                    "存在但没有 @Description 的方法应返回 null");
        }

        @Test
        @DisplayName("以 Object 为起点查找时返回 null")
        void fromObjectClass() {
            assertNull(ReflectUtil.getAnnotation(Description.class, Object.class, "toString", new Class<?>[]{}),
                    "从 Object 开始递归查找应安全返回 null");
        }
    }

    @Nested
    @DisplayName("getDescription 的四个重载")
    class GetDescriptionTest {

        @Test
        @DisplayName("类：有注解取注解值，无注解取类名")
        void describeClass() {
            assertEquals("父类", ReflectUtil.getDescription(Parent.class), "有 @Description 的类应返回注解值");
            assertEquals("父类", ReflectUtil.getDescription(Child.class), "子类应沿继承链取到父类注解值");
            assertEquals("Plain", ReflectUtil.getDescription(Plain.class), "无 @Description 的类应返回简单类名");
            assertEquals("Json", ReflectUtil.getDescription(Json.class), "Json 没有类级注解，应返回简单类名 Json");
        }

        @Test
        @DisplayName("方法：有注解取注解值，无注解取方法名")
        void describeMethod() throws NoSuchMethodException {
            assertEquals("子类方法", ReflectUtil.getDescription(childMethod()), "有 @Description 的方法应返回注解值");
            assertEquals("父类方法", ReflectUtil.getDescription(Parent.class.getDeclaredMethod("parentMethod")),
                    "父类方法应返回其注解值");
            assertEquals("childPlainMethod",
                    ReflectUtil.getDescription(Child.class.getDeclaredMethod("childPlainMethod", int.class)),
                    "无 @Description 的方法应返回方法名");
        }

        @Test
        @DisplayName("字段：有注解取注解值，无注解取字段名")
        void describeField() throws NoSuchFieldException {
            assertEquals("子类字段", ReflectUtil.getDescription(childField()), "有 @Description 的字段应返回注解值");
            assertEquals("mutable", ReflectUtil.getDescription(FieldHolder.class.getDeclaredField("mutable")),
                    "无 @Description 的字段应返回字段名");
            assertEquals("错误代码", ReflectUtil.getDescription(Json.class.getDeclaredField("code")),
                    "Json 的 code 字段应返回其注解值「错误代码」");
            assertEquals("key", ReflectUtil.getDescription(Gender.class.getDeclaredField("key")),
                    "枚举未标注的字段应返回字段名");
        }

        @Test
        @DisplayName("参数：有注解取注解值，无注解取参数名（依赖 -parameters）")
        void describeParameter() throws NoSuchMethodException {
            assertEquals("参数名", ReflectUtil.getDescription(childMethod().getParameters()[0]),
                    "带 @Description 的参数应返回注解值");
            assertEquals("age", ReflectUtil.getDescription(
                            Child.class.getDeclaredMethod("childPlainMethod", int.class).getParameters()[0]),
                    "未标注的参数应返回参数名（需开启 -parameters）");
        }
    }

    @Nested
    @DisplayName("getFieldList 字段列表")
    class GetFieldListTest {

        @Test
        @DisplayName("返回本类与父类的全部实例字段")
        void containsInheritedFields() {
            List<String> names = ReflectUtil.getFieldList(DemoModel.class).stream().map(Field::getName).toList();
            assertEquals(12, names.size(), "DemoModel 应返回 12 个字段");
            assertTrue(names.contains("id"), "字段列表应包含本类字段 id");
            assertTrue(names.contains("children"), "字段列表应包含集合类型字段 children");
        }

        @Test
        @DisplayName("排除 static 与 transient 字段")
        void excludesStaticAndTransient() {
            // 注意：Class#getDeclaredFields 不保证字段顺序，故按集合比较
            Set<String> names = ReflectUtil.getFieldList(FieldHolder.class).stream()
                    .map(Field::getName).collect(Collectors.toSet());
            assertEquals(Set.of("mutable", "hidden", "finalField", "URL", "a"), names,
                    "字段列表应排除 static 与 transient 字段，并保留 final 实例字段");
        }

        @Test
        @DisplayName("子类的字段列表中同时包含父类字段")
        void includesSuperClassFields() {
            List<String> names = ReflectUtil.getFieldList(Child.class).stream().map(Field::getName).toList();
            assertEquals(List.of("childField", "parentField"), names, "子类字段列表应按子类到父类的顺序包含两个字段");
        }

        @Test
        @DisplayName("Object 类返回空列表")
        void objectClassHasNoFields() {
            assertTrue(ReflectUtil.getFieldList(Object.class).isEmpty(), "Object 类没有字段，应返回空列表");
            assertTrue(ReflectUtil.getFieldList(RootModel.class).isEmpty(), "RootModel 自身没有实例字段，应返回空列表");
        }

        @Test
        @DisplayName("数组类的字段列表为空")
        void arrayClassHasNoFields() {
            assertTrue(ReflectUtil.getFieldList(int[].class).isEmpty(), "数组类型的父类是 Object，应返回空列表");
        }

        @Test
        @DisplayName("返回的列表不可修改")
        void listIsUnmodifiable() {
            List<Field> fieldList = ReflectUtil.getFieldList(DemoModel.class);
            assertThrows(UnsupportedOperationException.class, () -> fieldList.add(null),
                    "getFieldList 返回的列表是 unmodifiable，add 应抛 UnsupportedOperationException");
        }

        @Test
        @DisplayName("同一类多次调用返回缓存的同一实例")
        void listIsCached() {
            assertSame(ReflectUtil.getFieldList(DemoModel.class), ReflectUtil.getFieldList(DemoModel.class),
                    "同一 Class 应命中缓存，返回同一个 List 实例");
        }

        @Test
        @DisplayName("传入 null 抛 ServiceException")
        void nullClassThrows() {
            ServiceException exception = assertThrows(ServiceException.class, () -> ReflectUtil.getFieldList(null),
                    "getFieldList(null) 应抛出 ServiceException");
            assertEquals("无法获取 null 的字段列表", exception.getMessage(), "null 字段列表异常信息应为固定文案");
        }

        @Test
        @DisplayName("基本类型与接口抛空指针（源码未处理 getSuperClass() 为 null）")
        void primitiveAndInterfaceThrowNpe() {
            assertThrows(NullPointerException.class, () -> ReflectUtil.getFieldList(int.class),
                    "基本类型没有父类，getFieldList 应抛空指针");
            assertThrows(NullPointerException.class, () -> ReflectUtil.getFieldList(List.class),
                    "接口没有父类，getFieldList 应抛空指针");
        }
    }

    @Nested
    @DisplayName("getDeclaredFields 声明字段")
    class GetDeclaredFieldsTest {

        @Test
        @DisplayName("只返回本类声明的字段")
        void onlyDeclaredFields() {
            Field[] fields = ReflectUtil.getDeclaredFields(FieldHolder.class);
            // 注意：Class#getDeclaredFields 不保证字段顺序，故按集合比较
            Set<String> names = java.util.Arrays.stream(fields).map(Field::getName)
                    .collect(Collectors.toSet());
            assertEquals(Set.of("CONST", "stat", "temp", "mutable", "hidden", "finalField", "URL", "a"), names,
                    "getDeclaredFields 应返回本类声明的全部字段，包含 static 与 transient");
        }

        @Test
        @DisplayName("不包含父类字段")
        void excludesSuperClassFields() {
            List<String> names = java.util.Arrays.stream(ReflectUtil.getDeclaredFields(Child.class))
                    .map(Field::getName).toList();
            assertEquals(List.of("childField"), names, "getDeclaredFields 不应包含父类字段");
        }

        @Test
        @DisplayName("Object 类返回空数组")
        void objectClassHasNoDeclaredFields() {
            assertEquals(0, ReflectUtil.getDeclaredFields(Object.class).length, "Object 没有声明字段，应返回空数组");
        }

        @Test
        @DisplayName("同一类多次调用返回缓存的同一数组")
        void fieldsAreCached() {
            assertSame(ReflectUtil.getDeclaredFields(DemoModel.class), ReflectUtil.getDeclaredFields(DemoModel.class),
                    "同一 Class 应命中缓存，返回同一个数组实例");
        }

        @Test
        @DisplayName("传入 null 抛空指针（源码未做判空）")
        void nullClassThrowsNpe() {
            assertThrows(NullPointerException.class, () -> ReflectUtil.getDeclaredFields(null),
                    "getDeclaredFields(null) 源码未做判空，应抛空指针");
        }
    }

    @Nested
    @DisplayName("getLambdaFunctionName 解析 Lambda 方法名")
    class GetLambdaFunctionNameTest {

        @Test
        @DisplayName("方法引用 getKey 去掉 get 前缀")
        void keyMethodReference() {
            IFunction<Gender, Integer> function = Gender::getKey;
            assertEquals("Key", ReflectUtil.getLambdaFunctionName(function), "getKey 应被解析为「Key」");
        }

        @Test
        @DisplayName("方法引用 getLabel 去掉 get 前缀")
        void labelMethodReference() {
            IFunction<Gender, String> function = Gender::getLabel;
            assertEquals("Label", ReflectUtil.getLambdaFunctionName(function), "getLabel 应被解析为「Label」");
        }

        @Test
        @DisplayName("不含 get 的方法名保持原样")
        void methodWithoutGetPrefix() {
            IFunction<String, Integer> function = String::length;
            assertEquals("length", ReflectUtil.getLambdaFunctionName(function), "不含 get 的方法名应原样返回");
        }

        @Test
        @DisplayName("普通 Lambda 表达式返回编译器生成的方法名")
        void plainLambda() {
            IFunction<Gender, Integer> function = gender -> gender.getKey();
            assertTrue(ReflectUtil.getLambdaFunctionName(function).startsWith("lambda$"),
                    "普通 Lambda 应返回编译器生成的 lambda$ 前缀方法名");
        }

        @Test
        @DisplayName("只去掉方法名开头的 get 前缀")
        void removesOnlyGetPrefix() {
            IFunction<Targeter, String> function = Targeter::getTarget;
            assertEquals("Target", ReflectUtil.getLambdaFunctionName(function),
                    "getTarget 只应去掉开头的 get 前缀，剩余的 Target 应被完整保留");
        }

        @Test
        @DisplayName("去掉 get 前缀后剩余部分仍含 get 时予以保留")
        void keepsGetAfterPrefix() {
            IFunction<Targeter, String> function = Targeter::getForgetLabel;
            assertEquals("ForgetLabel", ReflectUtil.getLambdaFunctionName(function),
                    "getForgetLabel 去掉前缀后应得到 ForgetLabel，中间位置的 get 不应被删除");
        }

        @Test
        @DisplayName("非 Lambda 的匿名内部类实现抛 ServiceException")
        void anonymousClassThrows() {
            IFunction<Gender, Integer> function = new IFunction<>() {
                @Override
                public Integer apply(Gender gender) {
                    return gender.getKey();
                }
            };
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> ReflectUtil.getLambdaFunctionName(function),
                    "匿名内部类没有 writeReplace 方法，应抛出 ServiceException");
            assertTrue(exception.getMessage().startsWith("反射获取 Lambda 方法名失败，"),
                    "Lambda 解析失败异常信息应以「反射获取 Lambda 方法名失败，」开头，实际为 " + exception.getMessage());
        }

        @Test
        @DisplayName("传入 null 抛 ServiceException")
        void nullLambdaThrows() {
            ServiceException exception = assertThrows(ServiceException.class,
                    () -> ReflectUtil.getLambdaFunctionName(null),
                    "getLambdaFunctionName(null) 应抛出 ServiceException");
            assertTrue(exception.getMessage().startsWith("反射获取 Lambda 方法名失败，"),
                    "null 入参的异常信息也应以「反射获取 Lambda 方法名失败，」开头，实际为 " + exception.getMessage());
        }
    }

    @Nested
    @DisplayName("getField 递归查找字段")
    class GetFieldTest {

        @Test
        @DisplayName("本类字段可直接找到")
        void findOwnField() {
            Field field = ReflectUtil.getField("mutable", FieldHolder.class);
            assertNotNull(field, "本类声明的字段应能直接找到");
            assertEquals("mutable", field.getName(), "找到的字段名应为 mutable");
        }

        @Test
        @DisplayName("父类字段可沿继承链找到")
        void findFieldFromSuperClass() {
            Field field = ReflectUtil.getField("parentField", Child.class);
            assertNotNull(field, "子类中未声明时应能沿继承链找到父类字段");
            assertEquals(Parent.class, field.getDeclaringClass(), "找到的字段应声明在父类上");
        }

        @Test
        @DisplayName("字段名不存在时返回 null")
        void fieldNotFound() {
            assertNull(ReflectUtil.getField("notExistField", FieldHolder.class), "不存在的字段名应返回 null");
            assertNull(ReflectUtil.getField("parentField", FieldHolder.class), "无关的类中不存在的字段应返回 null");
        }

        @Test
        @DisplayName("类为 null 时返回 null")
        void nullClassReturnsNull() {
            assertNull(ReflectUtil.getField("mutable", null), "类为 null 时应直接返回 null");
        }

        @Test
        @DisplayName("类为 Object 时返回 null")
        void objectClassReturnsNull() {
            assertNull(ReflectUtil.getField("mutable", Object.class), "Object 是根类，应直接返回 null");
        }

        @Test
        @DisplayName("基本类型作为起点时安全返回 null")
        void primitiveClassReturnsNull() {
            assertNull(ReflectUtil.getField("value", int.class), "基本类型没有父类，递归时应安全返回 null");
        }
    }

    @Nested
    @DisplayName("getDeclaredFields 与 getFieldList 的配合")
    class CombinationTest {

        @Test
        @DisplayName("getFieldList 中的字段均可被 Modifier 正确判断")
        void modifiersOfFieldList() {
            for (Field field : ReflectUtil.getFieldList(FieldHolder.class)) {
                assertFalse(Modifier.isStatic(field.getModifiers()),
                        "getFieldList 中不应出现静态字段：" + field.getName());
                assertFalse(Modifier.isTransient(field.getModifiers()),
                        "getFieldList 中不应出现瞬态字段：" + field.getName());
            }
        }

        @Test
        @DisplayName("getField 找到的字段与 getFieldList 中的字段一致")
        void getFieldMatchesFieldList() {
            Field found = ReflectUtil.getField("childField", Child.class);
            assertNotNull(found, "getField 应能找到本类字段");
            assertTrue(ReflectUtil.getFieldList(Child.class).contains(found),
                    "getField 找到的字段应与 getFieldList 缓存中的字段相等");
        }
    }
}
