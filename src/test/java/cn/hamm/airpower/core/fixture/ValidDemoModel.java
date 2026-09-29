package cn.hamm.airpower.core.fixture;

import cn.hamm.airpower.core.RootModel;
import cn.hamm.airpower.core.annotation.Description;
import cn.hamm.airpower.core.annotation.Dictionary;
import cn.hamm.airpower.core.annotation.Phone;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <h1>校验测试用的数据模型</h1>
 *
 * <p>覆盖 {@code ValidateUtil.valid(...)} 需要的各种约束：
 * {@code @NotBlank} / {@code @NotNull} / {@code @Min} / {@code @Max} /
 * {@link Phone} 的三种组合 / {@link Dictionary}，
 * 以及一个只在 {@link Create} 分组生效的 {@code @NotBlank}。</p>
 *
 * @author Hamm.cn
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class ValidDemoModel extends RootModel<ValidDemoModel> {
    /**
     * 名称（非空校验）
     */
    @Description("名称")
    @NotBlank(message = "名称不能为空")
    private String name;
    /**
     * 数量（非空校验）
     */
    @Description("数量")
    @NotNull(message = "数量不能为空")
    private Integer count;
    /**
     * 最小值（下界校验）
     */
    @Description("最小值")
    @Min(value = 1, message = "最小值不能小于1")
    private Integer minValue;
    /**
     * 最大值（上界校验）
     */
    @Description("最大值")
    @Max(value = 100, message = "最大值不能大于100")
    private Integer maxValue;
    /**
     * 只允许手机号
     */
    @Description("仅手机号")
    @Phone(mobile = true, tel = false)
    private String onlyMobile;
    /**
     * 只允许座机
     */
    @Description("仅座机")
    @Phone(mobile = false, tel = true)
    private String onlyTel;
    /**
     * 手机号或座机均可（使用注解默认属性）
     */
    @Description("手机或座机")
    @Phone
    private String mobileOrTel;
    /**
     * 性别字典（使用注解默认错误消息）
     */
    @Description("性别")
    @Dictionary(Gender.class)
    private Integer gender;
    /**
     * 只在 {@link Create} 分组下生效的非空校验
     */
    @Description("创建场景名称")
    @NotBlank(message = "创建场景名称不能为空", groups = Create.class)
    private String createName;

    /**
     * 仅在该分组下生效的校验分组
     *
     * @apiNote 用于验证 {@code ValidateUtil.valid(model, actions)} 的分组校验
     */
    public interface Create {
    }
}
