package cn.hamm.airpower.core.fixture;

import cn.hamm.airpower.core.RootModel;
import cn.hamm.airpower.core.annotation.Description;
import cn.hamm.airpower.core.annotation.Desensitize;
import cn.hamm.airpower.core.annotation.Meta;
import cn.hamm.airpower.core.annotation.ReadOnly;
import cn.hamm.airpower.core.enums.DesensitizeType;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * <h1>测试用的数据模型</h1>
 *
 * <p>覆盖 {@code @Meta} / {@code @ReadOnly} / {@code @Desensitize} / Getter 上标记 {@code @Meta}
 * / 嵌套模型 / 模型集合 等场景。</p>
 *
 * @author Hamm.cn
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class DemoModel extends RootModel<DemoModel> {
    /**
     * 主键
     */
    @Description("主键")
    @Meta
    private Long id;

    /**
     * 姓名
     */
    @Description("姓名")
    @Meta
    private String name;

    /**
     * 手机号（脱敏）
     */
    @Description("手机号")
    @Meta
    @Desensitize(value = DesensitizeType.MOBILE)
    private String mobile;

    /**
     * 只读字段
     */
    @Description("创建时间")
    @Meta
    @ReadOnly
    private Long createTime;

    /**
     * 非元数据字段（{@code excludeNotMeta} 时应被清空）
     */
    @Description("备注")
    private String remark;

    /**
     * 字段上无 {@code @Meta}，但 Getter 上有
     */
    @Description("标题")
    private String title;

    /**
     * 整体替换为脱敏符号的字段
     */
    @Description("密钥")
    @Meta
    @Desensitize(value = DesensitizeType.CUSTOM, head = 2, tail = 2, replace = true)
    private String secret;

    /**
     * 非字符串类型的脱敏字段（脱敏后应被置空）
     */
    @Description("数字密文")
    @Meta
    @Desensitize(value = DesensitizeType.CUSTOM, head = 1, tail = 1)
    private Integer secretNumber;

    /**
     * 自定义头尾保留
     */
    @Description("邮箱")
    @Meta
    @Desensitize(value = DesensitizeType.CUSTOM, head = 1, tail = 1, symbol = "#")
    private String email;

    /**
     * 嵌套模型
     */
    @Description("子模型")
    @Meta
    private DemoModel child;

    /**
     * 模型集合
     */
    @Description("子模型集合")
    @Meta
    private List<DemoModel> children;

    /**
     * 包含 null 元素的模型集合
     */
    @Description("含空元素的子模型集合")
    @Meta
    private List<DemoModel> childrenWithNull;

    /**
     * Getter 上标记 {@code @Meta}，字段未标记
     *
     * @return 标题
     */
    @Meta
    public String getTitle() {
        return title;
    }

    /**
     * 基本类型 boolean，未标 {@code @Meta}
     *
     * <p>Lombok 对基本类型 boolean 生成的是 {@code isInternalFlag()} 而不是
     * {@code getIsInternalFlag()}，按字段名硬拼 getter 会找不到方法。</p>
     */
    @Description("内部标记")
    private boolean isInternalFlag;

    /**
     * 无 getter 的字段，未标 {@code @Meta}
     */
    @Description("隐藏字段")
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String hidden;

    /**
     * key 为字符串、value 为模型的 Map
     * <p>用来验证 {@code @ReadOnly} / {@code @Meta} / {@code @Desensitize}
     * 三条递归路径是否都覆盖了 Map 形态</p>
     */
    @Description("按 key 索引的子模型")
    @Meta
    private java.util.Map<String, DemoModel> mapOfChild;

    /**
     * 集合类型的脱敏字段
     */
    @Description("手机号列表")
    @Meta
    @Desensitize(value = DesensitizeType.MOBILE)
    private List<String> mobileList;
}
