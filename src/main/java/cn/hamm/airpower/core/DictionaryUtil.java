package cn.hamm.airpower.core;

import cn.hamm.airpower.core.exception.ServiceException;
import cn.hamm.airpower.core.interfaces.IDictionary;
import cn.hamm.airpower.core.interfaces.IFunction;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

/**
 * <h1>枚举字典工具类</h1>
 *
 * @author Hamm.cn
 */
@Slf4j
public class DictionaryUtil {
    /**
     * 禁止外部实例化
     */
    @Contract(pure = true)
    private DictionaryUtil() {
    }

    /**
     * 按 Key 查找字典项
     *
     * @param enumClass 枚举字典类
     * @param key       枚举字典值
     * @param <D>       字典类型
     * @return 查到的字典项
     */
    public static <D extends IDictionary> @NotNull D getDictionary(Class<D> enumClass, int key) {
        return getDictionary(enumClass, IDictionary::getKey, key);
    }

    /**
     * 按指定属性查找字典项
     *
     * @param enumClass 枚举字典类
     * @param function  获取比较属性的方法
     * @param value     比较的值
     * @param <D>       字典类型
     * @return 查到的字典项
     */
    public static <D extends IDictionary> @NotNull D getDictionary(
            Class<D> enumClass, Function<D, Object> function, Object value
    ) {
        D found = Arrays.stream(getEnumConstants(enumClass))
                .filter(enumItem -> Objects.equals(function.apply(enumItem), value))
                .findFirst()
                .orElse(null);
        if (Objects.nonNull(found)) {
            return found;
        }
        // 字典列表只在真正「找不到」时才构建：它要遍历全部枚举项并用反射
        // 逐项取值拼成 Map，命中路径上完全用不到，白算一遍是纯浪费
        throw new ServiceException(
                "传入的值(" + enumClass.getSimpleName() + "=" + value + ")不在字典可选范围内",
                getDictionaryList(enumClass)
        );
    }

    /**
     * 获取枚举常量，校验目标确实是枚举类
     *
     * @param enumClass 枚举字典类
     * @param <D>       字典类型
     * @return 枚举常量数组
     */
    private static <D extends IDictionary> D @NotNull [] getEnumConstants(Class<D> enumClass) {
        if (Objects.isNull(enumClass)) {
            throw new ServiceException("字典类不能为空");
        }
        D[] constants = enumClass.getEnumConstants();
        if (Objects.isNull(constants)) {
            throw new ServiceException("字典类(" + enumClass.getName() + ")不是枚举，无法作为字典使用");
        }
        return constants;
    }

    /**
     * 获取指定枚举类的 {@code ListMap} 数据
     *
     * @param clazz 枚举类
     * @return 枚举选项列表
     */
    public static <D extends IDictionary> @NotNull List<Map<String, Object>> getDictionaryList(
            Class<D> clazz
    ) {
        return getDictionaryList(clazz, IDictionary::getKey, IDictionary::getLabel);
    }

    /**
     * 获取指定枚举类的 {@code ListMap} 数据
     *
     * @param clazz   枚举字典类
     * @param lambdas 需要获取的方法表达式
     * @param <D>     字典类型
     * @return 枚举选项列表
     */
    @SafeVarargs
    public static <D extends IDictionary> @NotNull List<Map<String, Object>> getDictionaryList(
            Class<D> clazz, IFunction<D, Object>... lambdas
    ) {
        List<Map<String, Object>> mapList = new ArrayList<>();
        Arrays.stream(getEnumConstants(clazz)).forEach(enumItem -> {
            // 容量按负载因子折算，否则 lambdas 较少时会立刻触发扩容
            Map<String, Object> item = new HashMap<>((int) (lambdas.length / 0.75f) + 1);
            Arrays.stream(lambdas).forEach(lambda -> {
                try {
                    item.put(StringUtil.uncapitalize(ReflectUtil.getLambdaFunctionName(lambda)), lambda.apply(enumItem));
                } catch (Exception e) {
                    log.error("获取字典可选项失败, {}", e.getMessage());
                }
            });
            mapList.add(item);
        });
        return mapList;
    }
}
