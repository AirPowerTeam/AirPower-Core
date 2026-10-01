package cn.hamm.airpower.core;

import cn.hamm.airpower.core.annotation.Description;
import cn.hamm.airpower.core.exception.ServiceException;
import cn.hamm.airpower.core.interfaces.IFunction;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <h1>反射工具类</h1>
 *
 * @author Hamm.cn
 */
@Slf4j
public class ReflectUtil {
    /**
     * {@code get}
     */
    private static final String GET = "get";

    /**
     * 布尔字段的常见前缀（Lombok 对基本类型 boolean 生成 {@code isXxx()}）
     */
    private static final String IS = "is";

    /**
     * 缓存字段列表
     */
    private final static ConcurrentHashMap<Class<?>, List<Field>> FIELD_LIST_MAP = new ConcurrentHashMap<>();

    /**
     * 缓存属性列表
     *
     * @apiNote 声明属性列表。以 {@code Class} 为键而非类名，避免同名类在不同
     * {@code ClassLoader} 下互相串号
     */
    private final static ConcurrentHashMap<Class<?>, Field[]> DECLARED_FIELD_LIST_MAP = new ConcurrentHashMap<>();

    /**
     * 获取字段的 Getter 方法名
     *
     * @param field 字段
     * @return Getter 方法名
     */
    public static @NotNull String getFieldGetter(@NotNull Field field) {
        return candidateGetterNames(field).get(0);
    }

    /**
     * 获取字段的 Getter 方法名候选列表
     *
     * @param field 字段
     * @return 候选 Getter 方法名，按可能性从高到低
     */
    public static @NotNull List<String> candidateGetterNames(@NotNull Field field) {
        final String fieldName = field.getName();
        // 固定 Locale.ROOT：土耳其语环境下 "i".toUpperCase() 得到 "İ"，
        // 会把 getId 拼成 getİd，导不到方法、注解查找随之全部失效
        final String capitalized = fieldName.substring(0, 1).toUpperCase(Locale.ROOT) + fieldName.substring(1);
        List<String> names = new ArrayList<>(3);
        names.add(GET + capitalized);
        // 基本类型 boolean isHot → Lombok 生成 isHot()，也可能生成 getIsHot()
        if (fieldName.length() > 2 && fieldName.startsWith(IS)) {
            final String tail = fieldName.substring(IS.length());
            names.add(GET + tail.substring(0, 1).toUpperCase(Locale.ROOT) + tail.substring(1));
            names.add(fieldName);
        }
        return names;
    }

    /**
     * 获取对象指定属性的值
     *
     * @param object 对象
     * @param field  属性
     * @return 值
     */
    public static @Nullable Object getFieldValue(Object object, @NotNull Field field) {
        try {
            field.setAccessible(true);
            return field.get(object);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            // 调用方传入 null 对象导致的 NPE 不在此捕获，交由上层按调用错误处理
            throw new ServiceException("获取对象指定属性的值失败, " + e.getMessage());
        }
    }

    /**
     * 设置对象指定属性的值
     *
     * @param object 对象
     * @param field  属性
     * @param value  值
     */
    public static void setFieldValue(Object object, @NotNull Field field, Object value) {
        try {
            field.setAccessible(true);
            // 基本类型字段不接受 null：Field#set 会抛 IllegalArgumentException。
            // 「排除该字段」的语义对基本类型就是清成它的零值，
            // 不能因为字段是 primitive 就抛异常——那会让 @Meta 这类白名单
            // 在遇到 private boolean isXxx 时直接失败
            if (Objects.isNull(value) && field.getType().isPrimitive()) {
                field.set(object, defaultPrimitiveValue(field.getType()));
                return;
            }
            field.set(object, value);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            // 调用方传入 null 对象导致的 NPE 不在此捕获，交由上层按调用错误处理
            throw new ServiceException("设置对象指定属性的值失败, " + e.getMessage());
        }
    }

    /**
     * 获取基本类型的零值
     *
     * @param type 基本类型
     * @return 零值装箱结果
     */
    private static @NotNull Object defaultPrimitiveValue(@NotNull Class<?> type) {
        if (boolean.class == type) {
            return Boolean.FALSE;
        }
        if (char.class == type) {
            return (char) 0;
        }
        if (byte.class == type) {
            return (byte) 0;
        }
        if (short.class == type) {
            return (short) 0;
        }
        if (int.class == type) {
            return 0;
        }
        if (long.class == type) {
            return 0L;
        }
        if (float.class == type) {
            return 0F;
        }
        return 0D;
    }

    /**
     * 获取对象实例
     *
     * @param clazz 类
     * @param <T>   对象类型
     * @return 对象实例
     */
    public static <T extends RootModel<T>> @NotNull T newInstance(Class<T> clazz) {
        try {
            return clazz.getConstructor().newInstance();
        } catch (java.lang.Exception e) {
            throw new ServiceException("创建新实例失败，" + e.getMessage());
        }
    }

    /**
     * 清空对象指定属性的值
     *
     * @param object 对象
     * @param field  属性
     */
    public static void clearFieldValue(Object object, Field field) {
        setFieldValue(object, field, null);
    }

    /**
     * 判断是否是根类
     *
     * @param clazz 类
     * @return 判断结果
     */
    @Contract(pure = true)
    public static boolean isTheRootClass(@NotNull Class<?> clazz) {
        return clazz.equals(Object.class);
    }

    /**
     * 递归获取指定方法的注解
     *
     * @param annotationClass 注解类
     * @param method          方法
     * @param <A>             泛型
     * @return 注解
     */
    public static <A extends Annotation> @Nullable A getAnnotation(Class<A> annotationClass, @NotNull Method method) {
        return getAnnotation(annotationClass, method.getDeclaringClass(), method.getName(), method.getParameterTypes());
    }

    /**
     * 递归获取指定类的注解
     *
     * @param annotationClass 注解类
     * @param clazz           类
     * @param <A>             泛型
     * @return 注解
     */
    public static <A extends Annotation> @Nullable A getAnnotation(Class<A> annotationClass, @Nullable Class<?> clazz) {
        // clazz 为 null 与 clazz.getSuperclass() 为 null 都要挡住：
        // 接口的 getSuperclass() 恒为 null（接口没有 superclass，只有 interfaces），
        // 而递归的终止条件只判断了 Object.class，所以传任何接口进来第二轮就 NPE
        if (Objects.isNull(annotationClass) || Objects.isNull(clazz)) {
            return null;
        }
        A annotation = clazz.getAnnotation(annotationClass);
        if (Objects.nonNull(annotation)) {
            return annotation;
        }
        Class<?> superClass = clazz.getSuperclass();
        if (Objects.isNull(superClass) || isTheRootClass(clazz)) {
            return null;
        }
        return getAnnotation(annotationClass, superClass);
    }

    /**
     * 获取字段的注解
     *
     * @param annotationClass 注解类
     * @param field           字段
     * @param <A>             泛型
     * @return 注解
     */
    @Contract(pure = true)
    public static <A extends Annotation> @Nullable A getAnnotation(Class<A> annotationClass, @NotNull Field field) {
        return field.getAnnotation(annotationClass);
    }

    /**
     * 递归获取类描述
     *
     * @param clazz 类
     * @return 描述
     * @see Description
     */
    public static String getDescription(Class<?> clazz) {
        Description description = getAnnotation(Description.class, clazz);
        return Objects.isNull(description) ? clazz.getSimpleName() : description.value();
    }

    /**
     * 递归获取方法描述
     *
     * @param method 方法
     * @return 描述
     * @see Description
     */
    public static String getDescription(Method method) {
        Description description = getAnnotation(Description.class, method);
        return Objects.isNull(description) ? method.getName() : description.value();
    }

    /**
     * 递归获取字段描述
     *
     * @param field 字段
     * @return 描述
     * @see Description
     */
    public static String getDescription(Field field) {
        Description description = getAnnotation(Description.class, field);
        return Objects.isNull(description) ? field.getName() : description.value();
    }

    /**
     * 获取参数描述
     *
     * @param parameter 参数
     * @return 描述
     */
    public static String getDescription(@NotNull Parameter parameter) {
        Description description = parameter.getAnnotation(Description.class);
        return Objects.isNull(description) ? parameter.getName() : description.value();
    }

    /**
     * 获取指定类的字段列表
     *
     * @param clazz 类
     * @return 字段数组
     */
    public static @NotNull List<Field> getFieldList(Class<?> clazz) {
        if (Objects.isNull(clazz)) {
            throw new ServiceException("无法获取 null 的字段列表");
        }
        return FIELD_LIST_MAP.computeIfAbsent(clazz, ReflectUtil::getCacheFieldList);
    }

    /**
     * 获取指定类的字段列表
     *
     * @param clazz 类
     * @return 字段数组
     */
    private static @NotNull List<Field> getCacheFieldList(Class<?> clazz) {
        List<Field> fieldList = new ArrayList<>();
        if (Objects.isNull(clazz)) {
            return fieldList;
        }
        // 收集当前类和所有父类的字段，避免递归中的多次列表创建和合并
        Class<?> currentClass = clazz;
        // 接口与基本类型的 getSuperclass() 返回 null，需显式判空，否则空判断自身会抛 NPE
        while (Objects.nonNull(currentClass) && !isTheRootClass(currentClass)) {
            Field[] fields = getDeclaredFields(currentClass);
            for (Field field : fields) {
                // 跳过静态、瞬态与编译器生成的字段（如内部类的 this$0）
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic()) {
                    continue;
                }
                field.setAccessible(true);
                fieldList.add(field);
            }
            currentClass = currentClass.getSuperclass();
        }
        return Collections.unmodifiableList(fieldList);
    }

    /**
     * 获取类的所有属性
     *
     * @param clazz 类
     * @return 属性数组
     */
    @Contract(pure = true)
    public static Field @NotNull [] getDeclaredFields(@NotNull Class<?> clazz) {
        return DECLARED_FIELD_LIST_MAP.computeIfAbsent(clazz, Class::getDeclaredFields);
    }

    /**
     * 获取 Lambda 的 Function 表达式的函数名
     *
     * @param lambda 表达式
     * @return 函数名
     * @apiNote 仅去掉 {@code get} 前缀，方法名中间的 {@code get} 会被保留
     */
    public static @NotNull String getLambdaFunctionName(@NotNull IFunction<?, ?> lambda) {
        String methodName = getSerializedLambda(lambda).getImplMethodName();
        if (methodName.length() > GET.length() && methodName.startsWith(GET)) {
            return methodName.substring(GET.length());
        }
        return methodName;
    }

    /**
     * 获取一个 SerializedLambda
     *
     * @param lambda 表达式
     * @return SerializedLambda
     */
    private static SerializedLambda getSerializedLambda(@NotNull IFunction<?, ?> lambda) {
        try {
            Method replaceMethod = lambda.getClass().getDeclaredMethod("writeReplace");
            replaceMethod.setAccessible(true);
            return (SerializedLambda) replaceMethod.invoke(lambda);
        } catch (Exception e) {
            throw new ServiceException("反射获取 Lambda 方法名失败，" + e.getMessage());
        }
    }

    /**
     * 递归获取方法的注解
     *
     * @param annotationClass 要查找的注解类型
     * @param currentClass    当前类
     * @param methodName      方法名
     * @param paramTypes      方法参数类型数组
     * @return 如果找到注解则返回该注解实例，否则返回 null
     */
    public static <T extends java.lang.annotation.Annotation> @Nullable T getAnnotation(
            Class<T> annotationClass,
            @NotNull Class<?> currentClass,
            String methodName,
            Class<?>[] paramTypes
    ) {
        try {
            // 获取当前类中的方法
            Method method = currentClass.getDeclaredMethod(methodName, paramTypes);
            // 获取注解，避免重复调用 getAnnotation
            T annotation = method.getAnnotation(annotationClass);
            if (annotation != null) {
                return annotation;
            }
        } catch (NoSuchMethodException ignored) {
            // 忽略，继续查找父类或接口
        }

        // 查找父类
        Class<?> superClass = currentClass.getSuperclass();
        if (superClass != null) {
            return getAnnotation(annotationClass, superClass, methodName, paramTypes);
        }
        return null;
    }

    /**
     * 递归获取字段
     *
     * @param fieldName 字段名
     * @param clazz     当前类
     * @return 字段
     */
    public static @Nullable Field getField(String fieldName, Class<?> clazz) {
        if (Objects.isNull(clazz) || Object.class.equals(clazz)) {
            return null;
        }
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            return getField(fieldName, clazz.getSuperclass());
        }
    }
}
