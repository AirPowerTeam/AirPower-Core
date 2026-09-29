package cn.hamm.airpower.core.fixture;

import cn.hamm.airpower.core.RootModel;
import cn.hamm.airpower.core.annotation.Description;
import cn.hamm.airpower.core.annotation.Dictionary;
import cn.hamm.airpower.core.annotation.Export;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <h1>测试用的导出模型</h1>
 *
 * <p>覆盖 {@code @Export} 的各种 {@code Type}、排序、移除等场景。</p>
 *
 * @author Hamm.cn
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class ExportDemoModel extends RootModel<ExportDemoModel> {
    /**
     * 主键
     */
    @Description("主键")
    @Export(sort = 10)
    private Long id;

    /**
     * 姓名
     */
    @Description("姓名")
    @Export(sort = 20)
    private String name;

    /**
     * 性别
     */
    @Description("性别")
    @Dictionary(Gender.class)
    @Export(value = Export.Type.DICTIONARY, sort = 30)
    private Integer gender;

    /**
     * 创建时间
     */
    @Description("创建时间")
    @Export(value = Export.Type.DATETIME, sort = 40)
    private Long createTime;

    /**
     * 是否启用
     */
    @Description("是否启用")
    @Export(value = Export.Type.BOOLEAN, sort = 50)
    private Boolean enabled;

    /**
     * 金额
     */
    @Description("金额")
    @Export(value = Export.Type.NUMBER, sort = 60)
    private Double amount;

    /**
     * 数量
     */
    @Description("数量")
    @Export(value = Export.Type.NUMBER, sort = 70)
    private Long count;

    /**
     * 比率
     */
    @Description("比率")
    @Export(value = Export.Type.NUMBER, sort = 80)
    private Float ratio;

    /**
     * 备注（可能包含逗号与换行）
     */
    @Description("备注")
    @Export(value = Export.Type.TEXT, sort = 5)
    private String remark;

    /**
     * 需要移除的导出列
     */
    @Description("被移除的列")
    @Export(remove = true, sort = 90)
    private String removedColumn;

    /**
     * 未标记 {@code @Export} 的列
     */
    @Description("未导出的列")
    private String notExportColumn;
}
