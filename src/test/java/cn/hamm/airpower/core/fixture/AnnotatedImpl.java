package cn.hamm.airpower.core.fixture;

/**
 * {@link AnnotatedInterface} 的实现，故意不重复标注 {@code @Description}
 *
 * @author Hamm.cn
 */
public class AnnotatedImpl implements AnnotatedInterface {

    @Override
    public String getName() {
        return "实现";
    }
}
