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
import java.util.ArrayList;
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
     */
    public final void desensitize() {
        excludeNotMetaAndDesensitize(new ArrayList<>(), true);
    }

    /**
     * 排除非元数据字段
     */
    public final void excludeNotMeta() {
        List<Class<? extends RootModel<?>>> whiteList = List.of((Class<? extends RootModel<?>>) this.getClass());
        excludeNotMeta(whiteList);
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
     * @param whiteList     类白名单
     * @param isDesensitize 是否需要脱敏
     * @apiNote 标记了类白名单的实例，不会忽略非元数据字段
     */
    public final void excludeNotMetaAndDesensitize(List<Class<? extends RootModel<?>>> whiteList, boolean isDesensitize) {
        filterModelFieldValue((instance, field) -> {
            Object value = ReflectUtil.getFieldValue(instance, field);
            if (Objects.isNull(value)) {
                return;
            }
            if (!whiteList.isEmpty() && !whiteList.contains(this.getClass())) {
                excludeFieldValueNotMeta(instance, field);
                return;
            }
            if (value instanceof Collection<?> valueList) {
                // 是对象集合
                valueList.forEach(item -> {
                    if (RootModel.isModel(item.getClass())) {
                        @SuppressWarnings("unchecked")
                        M itemModel = (M) item;
                        itemModel.excludeNotMetaAndDesensitize(whiteList, isDesensitize);
                    }
                });
                return;
            }
            if (RootModel.isModel(value.getClass())) {
                // 如果是模型，则递归脱敏
                @SuppressWarnings("unchecked")
                M payload = ((M) value);
                payload.excludeNotMetaAndDesensitize(whiteList, isDesensitize);
                return;
            }
            if (isDesensitize) {
                desensitizeFieldValue(field, value);
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
        if (isModel(value.getClass())) {
            ((RootModel<?>) value).excludeNotMeta();
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
                    ReflectUtil.setFieldValue(this, field, null);
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
    }

    /**
     * 脱敏字段的值
     *
     * @param field 字段
     * @param value 值
     */
    private void desensitizeFieldValue(Field field, @NotNull Object value) {
        Desensitize desensitize = ReflectUtil.getAnnotation(Desensitize.class, field);
        if (Objects.isNull(desensitize)) {
            return;
        }
        if ((value instanceof String valueString)) {
            if (desensitize.replace()) {
                ReflectUtil.setFieldValue(this, field, desensitize.symbol());
                return;
            }
            // 如果不是字符串，则置空
            ReflectUtil.setFieldValue(this, field,
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
        ReflectUtil.setFieldValue(this, field, null);
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
