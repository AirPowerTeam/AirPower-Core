package cn.hamm.airpower.core;

import cn.hamm.airpower.core.annotation.Desensitize;
import cn.hamm.airpower.core.annotation.Meta;
import cn.hamm.airpower.core.annotation.ReadOnly;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * <h1>数据根模型</h1>
 *
 * @author Hamm.cn
 */
@Getter
@Slf4j
@EqualsAndHashCode
@SuppressWarnings("unchecked")
public class RootModel<M extends RootModel<M>> {
    /**
     * 是否是继承自 RootModel
     *
     * @param clazz 类
     * @return 布尔
     */
    public static boolean isModel(Class<?> clazz) {
        if (Objects.isNull(clazz)) {
            return false;
        }
        if (RootModel.class.equals(clazz)) {
            return true;
        }
        return isModel(clazz.getSuperclass());
    }

    /**
     * 排除只读字段
     */
    public final void excludeReadOnly() {
        ReflectUtil.getFieldList(getClass()).stream()
                .filter(field -> Objects.nonNull(ReflectUtil.getAnnotation(ReadOnly.class, field)))
                .forEach(field -> ReflectUtil.clearFieldValue(this, field));
    }

    /**
     * 脱敏
     *
     * @apiNote 先排除非元数据字段，再对 {@link cn.hamm.airpower.core.annotation.Desensitize}
     * 标记的字段脱敏；自身类自动加入白名单，保证本模型的字段不会被误排除
     */
    public final void desensitize() {
        excludeNotMeta();
        //noinspection unchecked
        Class<? extends RootModel<?>> selfClass = (Class<? extends RootModel<?>>) getClass();
        excludeNotMetaAndDesensitize(List.of(selfClass), true);
    }

    /**
     * 排除非元数据字段
     */
    public final void excludeNotMeta() {
        excludeNotMeta(List.of());
    }

    /**
     * 模型字段值处理
     *
     * @param whiteList 类白名单
     * @apiNote 标记了类白名单的实例，不会忽略非元数据字段
     */
    public final void excludeNotMeta(@NotNull List<Class<? extends RootModel<?>>> whiteList) {
        excludeNotMetaAndDesensitize(whiteList, false);
    }

    /**
     * 模型字段值处理
     *
     * @param whiteList     类白名单，为 {@code null} 时按空名单处理
     * @param isDesensitize 是否需要脱敏
     * @apiNote 标记了类白名单的实例，不会忽略非元数据字段
     */
    public final void excludeNotMetaAndDesensitize(List<Class<? extends RootModel<?>>> whiteList, boolean isDesensitize) {
        List<Class<? extends RootModel<?>>> whiteNameList = Objects.isNull(whiteList) ? List.of() : whiteList;
        filterModelFieldValue((instance, field) -> {
            Object value = ReflectUtil.getFieldValue(instance, field);
            if (Objects.isNull(value)) {
                return;
            }
            if (whiteNameList.isEmpty() || !whiteNameList.contains(this.getClass())) {
                excludeFieldValueNotMeta(instance, field);
                return;
            }
            if (value instanceof Collection<?> valueList) {
                // 是对象集合
                valueList.forEach(item -> {
                    if (Objects.isNull(item) || !RootModel.isModel(item.getClass())) {
                        return;
                    }
                    @SuppressWarnings("unchecked")
                    M itemModel = (M) item;
                    itemModel.excludeNotMetaAndDesensitize(whiteNameList, isDesensitize);
                });
                return;
            }
            if (RootModel.isModel(value.getClass())) {
                // 如果是模型，则递归脱敏
                @SuppressWarnings("unchecked")
                M payload = ((M) value);
                payload.excludeNotMetaAndDesensitize(whiteNameList, isDesensitize);
                return;
            }
            if (isDesensitize) {
                desensitizeFieldValue(instance, field, value);
            }
        });
    }

    /**
     * 排除非元数据字段
     *
     * @param field 字段
     */
    private void excludeFieldValueNotMeta(M instance, @NotNull Field field) {
        Object value = ReflectUtil.getFieldValue(instance, field);
        if (Objects.isNull(value)) {
            return;
        }
        Meta meta = ReflectUtil.getAnnotation(Meta.class, field);
        if (Objects.isNull(meta)) {
            // 判断 Getter 是否被标记
            String fieldGetter = ReflectUtil.getFieldGetter(field);
            try {
                Method getter = instance.getClass().getMethod(fieldGetter);
                meta = ReflectUtil.getAnnotation(Meta.class, getter);
                if (Objects.isNull(meta)) {
                    ReflectUtil.setFieldValue(instance, field, null);
                    return;
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
        if (value instanceof Collection<?> valueList) {
            // 是对象集合，逐个递归排除非元数据字段
            valueList.forEach(item -> {
                if (Objects.nonNull(item) && isModel(item.getClass())) {
                    ((RootModel<?>) item).excludeNotMeta();
                }
            });
            return;
        }
        if (isModel(value.getClass())) {
            ((RootModel<?>) value).excludeNotMeta();
        }
    }

    /**
     * 脱敏字段的值
     *
     * @param instance 模型实例
     * @param field    字段
     * @param value    值
     */
    private void desensitizeFieldValue(M instance, @NotNull Field field, @NotNull Object value) {
        Desensitize desensitize = ReflectUtil.getAnnotation(Desensitize.class, field);
        if (Objects.isNull(desensitize)) {
            return;
        }
        if ((value instanceof String valueString)) {
            if (desensitize.replace()) {
                ReflectUtil.setFieldValue(instance, field, desensitize.symbol());
                return;
            }
            // 如果不是字符串，则置空
            ReflectUtil.setFieldValue(instance, field,
                    DesensitizeUtil.desensitize(
                            valueString,
                            desensitize.value(),
                            desensitize.head(),
                            desensitize.tail(),
                            desensitize.symbol()
                    )
            );
            return;
        }
        ReflectUtil.setFieldValue(instance, field, null);
    }

    /**
     * 过滤模型的字段数据
     *
     * @param consumer 过滤方法
     */
    private void filterModelFieldValue(BiConsumer<M, Field> consumer) {
        Class<M> clazz = (Class<M>) getClass();
        List<Field> allFields = ReflectUtil.getFieldList(clazz);
        for (Field field : allFields) {
            consumer.accept((M) this, field);
        }
    }
}
