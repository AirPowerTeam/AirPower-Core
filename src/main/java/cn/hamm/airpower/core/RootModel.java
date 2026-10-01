package cn.hamm.airpower.core;

import cn.hamm.airpower.core.annotation.Desensitize;
import cn.hamm.airpower.core.annotation.Meta;
import cn.hamm.airpower.core.annotation.ReadOnly;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * <h1>数据根模型</h1>
 *
 * @author Hamm.cn
 */
@Getter
@Slf4j
@EqualsAndHashCode
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
     *
     * @param model   当前模型
     * @param visited 已访问的模型，按对象身份去重
     * @apiNote {@code visited} 防止自引用模型导致栈溢出
     */
    private static void excludeReadOnlyAll(@NotNull RootModel<?> model, @NotNull Set<RootModel<?>> visited) {
        if (!visited.add(model)) {
            return;
        }
        model.filterModelFieldValue((instance, field) -> {
            if (Objects.nonNull(ReflectUtil.getAnnotation(ReadOnly.class, field))) {
                ReflectUtil.clearFieldValue(instance, field);
                return;
            }
            Object value = ReflectUtil.getFieldValue(instance, field);
            if (Objects.isNull(value)) {
                return;
            }
            if (value instanceof Collection<?> valueList) {
                valueList.forEach(item -> {
                    if (Objects.isNull(item) || !RootModel.isModel(item.getClass())) {
                        return;
                    }
                    excludeReadOnlyAll((RootModel<?>) item, visited);
                });
                return;
            }
            if (RootModel.isModel(value.getClass())) {
                excludeReadOnlyAll((RootModel<?>) value, visited);
            }
        });
    }

    /**
     * 按嵌套模型自身类是否在白名单中，决定做"排除非元数据"还是"排除 + 脱敏"
     *
     * @param nested        嵌套模型
     * @param whiteList     类白名单
     * @param isDesensitize 是否需要脱敏
     * @apiNote 嵌套模型不在白名单中时保留"只排除不脱敏"的既有语义；
     * 在白名单中时必须传递白名单继续递归，保证深层嵌套也能脱敏
     */
    private static void handleNested(
            @NotNull RootModel<?> nested,
            @NotNull List<Class<? extends RootModel<?>>> whiteList,
            boolean isDesensitize
    ) {
        if (whiteList.contains(nested.getClass())) {
            nested.excludeNotMetaAndDesensitize(whiteList, isDesensitize);
            return;
        }
        nested.excludeNotMeta();
    }

    /**
     * 对所有可达模型递归脱敏
     *
     * @param visited 已访问的模型，按对象身份去重
     * @apiNote 不使用类白名单，嵌套模型无论类型都会被脱敏；
     * {@code visited} 防止自引用模型（{@code child == this}）导致栈溢出
     */
    private static void desensitizeAll(@NotNull RootModel<?> model, @NotNull Set<RootModel<?>> visited) {
        if (!visited.add(model)) {
            return;
        }
        model.filterModelFieldValue((instance, field) -> {
            Object value = ReflectUtil.getFieldValue(instance, field);
            if (Objects.isNull(value)) {
                return;
            }
            if (value instanceof Collection<?> valueList) {
                valueList.forEach(item -> {
                    if (Objects.isNull(item) || !RootModel.isModel(item.getClass())) {
                        return;
                    }
                    desensitizeAll((RootModel<?>) item, visited);
                });
                return;
            }
            if (RootModel.isModel(value.getClass())) {
                desensitizeAll((RootModel<?>) value, visited);
                return;
            }
            desensitizeFieldValue(instance, field, value);
        });
    }

    /**
     * 排除非元数据字段
     *
     * @param instance 模型实例
     * @param field    字段
     */
    private static void excludeFieldValueNotMeta(@NotNull RootModel<?> instance, @NotNull Field field) {
        excludeFieldValueNotMeta(instance, field, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    /**
     * 排除非元数据字段
     *
     * @param instance 模型实例
     * @param field    字段
     * @param visited  已访问的模型，按对象身份去重
     * @apiNote 自引用模型（{@code child == this}）会形成无限递归，由 {@code visited} 拦下
     */
    private static void excludeFieldValueNotMeta(
            @NotNull RootModel<?> instance, @NotNull Field field, @NotNull Set<RootModel<?>> visited
    ) {
        Object value = ReflectUtil.getFieldValue(instance, field);
        if (Objects.isNull(value)) {
            return;
        }
        Meta meta = ReflectUtil.getAnnotation(Meta.class, field);
        if (Objects.isNull(meta)) {
            // 字段上没标 @Meta 时，回退到看 getter 上是否标了
            meta = findMetaOnGetter(instance.getClass(), field);
        }
        if (Objects.isNull(meta)) {
            // 字段和所有可推断的 getter 上都没有 @Meta：一律排除。
            // 这里必须 fail-closed —— 早先的写法把 getMethod 的 NoSuchMethodException
            // 空 catch 掉，控制流直接落到 if 块之外，于是「拼不出 getter 名」的字段
            // （基本类型 boolean isXxx、非 public getter、@Getter(NONE) 等）
            // 既不被置空也不被处理，被原样返回前端。白名单 fail-open 等于没有白名单
            ReflectUtil.setFieldValue(instance, field, null);
            return;
        }
        if (value instanceof Collection<?> valueList) {
            // 是对象集合，逐个递归排除非元数据字段
            valueList.forEach(item -> {
                if (Objects.isNull(item) || !isModel(item.getClass())) {
                    return;
                }
                excludeNotMetaAll((RootModel<?>) item, visited);
            });
            return;
        }
        if (isModel(value.getClass())) {
            excludeNotMetaAll((RootModel<?>) value, visited);
        }
    }

    /**
     * 在 getter 上查找 {@link Meta}
     *
     * @param clazz 当前类
     * @param field 字段
     * @return 找到返回注解，否则返回 {@code null}
     * @apiNote getter 名有多个候选（见 {@link ReflectUtil#candidateGetterNames}），
     * 任一命中即算找到；全部找不到才返回 {@code null}，由调用方按 fail-closed 处理
     */
    private static @Nullable Meta findMetaOnGetter(@NotNull Class<?> clazz, @NotNull Field field) {
        for (String candidate : ReflectUtil.candidateGetterNames(field)) {
            try {
                Meta meta = ReflectUtil.getAnnotation(Meta.class, clazz.getMethod(candidate));
                if (Objects.nonNull(meta)) {
                    return meta;
                }
            } catch (NoSuchMethodException ignored) {
                // 试下一个候选名
            }
        }
        return null;
    }

    /**
     * 排除非元数据字段
     *
     * @param model   当前模型
     * @param visited 已访问的模型，按对象身份去重
     * @apiNote 自引用模型（{@code child == this}）会形成无限递归，由 {@code visited} 拦下
     */
    private static void excludeNotMetaAll(@NotNull RootModel<?> model, @NotNull Set<RootModel<?>> visited) {
        if (!visited.add(model)) {
            return;
        }
        model.filterModelFieldValue((instance, field) ->
                excludeFieldValueNotMeta(instance, field, visited)
        );
    }

    /**
     * 脱敏字段的值
     *
     * @param instance 模型实例
     * @param field    字段
     * @param value    值
     */
    private static void desensitizeFieldValue(
            @NotNull RootModel<?> instance, @NotNull Field field, @NotNull Object value
    ) {
        Desensitize desensitize = ReflectUtil.getAnnotation(Desensitize.class, field);
        if (Objects.isNull(desensitize)) {
            return;
        }
        if (value instanceof String valueString) {
            if (desensitize.replace()) {
                ReflectUtil.setFieldValue(instance, field, desensitize.symbol());
                return;
            }
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
        // 非 String 一律保持原值并告警，绝不置 null。
        // 置 null 有两个问题：其一，本方法是**原地修改实体**，一旦被 flush 回库
        // 就是真实的字段级数据丢失；其二，前端只看到「数据没了」，
        // 服务端毫无线索。保持原值 + 告警至少让「打码没生效」变成可发现的问题。
        //
        // 集合也没做逐元素脱敏：String 不可变，Collection<?> 又拿不到类型信息，
        // 逐元素替换要么需要 setFieldValue 换掉整个集合（Hibernate 托管集合被替换
        // 可能造成脏数据），要么需要强转 List<String> 后调 set（List.of 之类不可变集合会抛
        // UnsupportedOperationException）。两种都比「不脱敏」更危险，故一并告警。
        log.warn("字段({})声明了 @Desensitize，但值类型为 {}，无法在不破坏类型的前提下脱敏，已保持原值。"
                        + "敏感信息请用 String 字段承载；集合类型的 @Desensitize 目前不生效",
                field.getName(), value.getClass().getSimpleName());
    }

    /**
     * 排除只读字段
     *
     * @apiNote 递归处理嵌套模型与模型集合，与 {@link #excludeNotMeta()} / {@link #desensitize()}
     * 保持一致；否则嵌套模型里的只读字段（如创建时间）仍会返回给前端，
     * 客户端可据此覆盖服务端数据
     */
    public final void excludeReadOnly() {
        excludeReadOnlyAll(this, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    /**
     * 脱敏
     *
     * @apiNote 先排除非元数据字段，再对所有可达模型中
     * {@link cn.hamm.airpower.core.annotation.Desensitize} 标记的字段脱敏。
     * 嵌套模型与模型集合<b>不论类型是否与自身相同</b>都会递归脱敏，
     * 避免"订单 → 收货人"这类结构泄露明文敏感数据
     */
    public final void desensitize() {
        // 先排除非元数据字段：每个模型实例都会走排除分支，自引用由已访问集合拦下
        excludeNotMetaAll(this, Collections.newSetFromMap(new IdentityHashMap<>()));
        // 再对所有可达模型（含类型不同的嵌套模型）递归脱敏
        desensitizeAll(this, Collections.newSetFromMap(new IdentityHashMap<>()));
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
                // 当前类不在白名单中：只做非元数据排除，不触发脱敏
                excludeFieldValueNotMeta(instance, field);
                return;
            }
            if (value instanceof Collection<?> valueList) {
                // 是对象集合
                valueList.forEach(item -> {
                    if (Objects.isNull(item) || !RootModel.isModel(item.getClass())) {
                        return;
                    }
                    // 集合元素按自身类重新判定白名单：
                    // 在白名单内则继续递归（脱敏），否则只排除非元数据
                    handleNested((RootModel<?>) item, whiteNameList, isDesensitize);
                });
                return;
            }
            if (RootModel.isModel(value.getClass())) {
                handleNested((RootModel<?>) value, whiteNameList, isDesensitize);
                return;
            }
            if (isDesensitize) {
                desensitizeFieldValue(instance, field, value);
            }
        });
    }

    /**
     * 过滤模型的字段数据
     *
     * @param consumer 过滤方法
     */
    private void filterModelFieldValue(BiConsumer<RootModel<?>, Field> consumer) {
        List<Field> allFields = ReflectUtil.getFieldList(getClass());
        for (Field field : allFields) {
            consumer.accept(this, field);
        }
    }
}
