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
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
            if (value instanceof Map<?, ?> valueMap) {
                forEachModelValue(valueMap, item -> excludeReadOnlyAll(item, visited));
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
            boolean isDesensitize,
            @NotNull Set<RootModel<?>> visited
    ) {
        // 不要在这里 visited.add：两个下游方法（excludeNotMetaAll 与
        // excludeNotMetaAndDesensitize）自己都会 add，提前 add 会让它们
        // 立刻判定「已访问」而直接返回，递归彻底断掉
        if (whiteList.contains(nested.getClass())) {
            excludeNotMetaAndDesensitize(nested, whiteList, isDesensitize, visited);
            return;
        }
        excludeNotMetaAll(nested, visited);
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
            if (value instanceof Map<?, ?> valueMap) {
                forEachModelValue(valueMap, item -> desensitizeAll(item, visited));
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
     * 遍历 Map 中的模型值
     *
     * @param valueMap Map
     * @param action   对每个模型值执行的动作
     */
    private static void forEachModelValue(@NotNull Map<?, ?> valueMap, @NotNull Consumer<RootModel<?>> action) {
        for (Map.Entry<?, ?> entry : valueMap.entrySet()) {
            if (entry.getKey() instanceof RootModel<?> keyModel) {
                action.accept(keyModel);
            }
            if (entry.getValue() instanceof RootModel<?> valueModel) {
                action.accept(valueModel);
            }
        }
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
            // 字段和所有候选 getter 上都没有 @Meta，一律排除。
            // 必须 fail-closed：getter 名拼不出时若不排除，字段会被原样返回前端
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
        if (value instanceof Map<?, ?> valueMap) {
            forEachModelValue(valueMap, item -> excludeNotMetaAll(item, visited));
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
        // 非 String 保持原值：置 null 会在实体被 flush 回库时造成字段级数据丢失。
        // 集合同样不逐元素脱敏：String 不可变、Collection<?> 无类型信息，
        // 逐元素替换要么换掉整个托管集合，要么在不可变集合上抛异常，都更危险
        log.warn("字段({})的 @Desensitize 对类型 {} 不生效，已保持原值；请改用 String 字段",
                field.getName(), value.getClass().getSimpleName());
    }

    /**
     * 模型字段值处理
     *
     * @param model         当前模型
     * @param whiteList     类白名单
     * @param isDesensitize 是否需要脱敏
     * @param visited       已访问的模型，按对象身份去重
     * @apiNote {@code visited} 必须按引用传递：成环模型（{@code A→B→A}）会 StackOverflowError
     */
    private static void excludeNotMetaAndDesensitize(
            @NotNull RootModel<?> model,
            @NotNull List<Class<? extends RootModel<?>>> whiteList,
            boolean isDesensitize,
            @NotNull Set<RootModel<?>> visited
    ) {
        if (!visited.add(model)) {
            return;
        }
        model.filterModelFieldValue((instance, field) -> {
            Object value = ReflectUtil.getFieldValue(instance, field);
            if (Objects.isNull(value)) {
                return;
            }
            if (whiteList.isEmpty() || !whiteList.contains(model.getClass())) {
                // 当前类不在白名单中：只做非元数据排除，不触发脱敏
                excludeFieldValueNotMeta(instance, field, visited);
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
                    handleNested((RootModel<?>) item, whiteList, isDesensitize, visited);
                });
                return;
            }
            if (value instanceof Map<?, ?> valueMap) {
                forEachModelValue(valueMap,
                        item -> handleNested(item, whiteList, isDesensitize, visited));
                return;
            }
            if (RootModel.isModel(value.getClass())) {
                handleNested((RootModel<?>) value, whiteList, isDesensitize, visited);
                return;
            }
            if (isDesensitize) {
                desensitizeFieldValue(instance, field, value);
            }
        });
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
     * @apiNote 只负责脱敏，<b>不</b>排除非元数据字段。脱敏与排除元数据是两个相互独立的功能：
     * 排除会把无 {@link Meta} 的字段整体置空，与脱敏叠加后这类字段只剩「被清空」一种结果，
     * 脱敏规则等于形同虚设，原值也一并丢失。两个功能都要时由调用方依次调用
     * {@link #excludeNotMeta()} 与本方法。
     * 嵌套模型与模型集合<b>不论类型是否与自身相同</b>都会递归脱敏，
     * 避免"订单 → 收货人"这类结构泄露明文敏感数据
     */
    public final void desensitize() {
        // 对所有可达模型（含类型不同的嵌套模型）递归脱敏；自引用由已访问集合拦下
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
        excludeNotMetaAndDesensitize(this,
                Objects.isNull(whiteList) ? List.of() : whiteList,
                isDesensitize,
                Collections.newSetFromMap(new IdentityHashMap<>()));
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
