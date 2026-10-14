package cn.hamm.airpower.core.interfaces;

import java.io.Serializable;
import java.util.function.Function;

/**
 * <h2>带序列化的 {@code Function}</h2>
 *
 * @author Hamm.cn
 * @param <T> 入参类型
 * @param <R> 返回类型
 * @apiNote 必须可序列化，{@code ReflectUtil.getLambdaFunctionName} 依赖 Lambda 的
 * {@code writeReplace} 钩子反查方法名
 */
@FunctionalInterface
public interface IFunction<T, R> extends Function<T, R>, Serializable {
}