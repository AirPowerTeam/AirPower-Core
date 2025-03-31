import { AirColor, AirEnum } from 'airpower'

/**
 * # 禁用状态枚举
 *
 * @author Hamm.cn
 */
export class DisableStatusEnum extends AirEnum<boolean> {
  /**
   * ### 禁用
   */
  static readonly DISABLE = new DisableStatusEnum(true, '禁用', AirColor.DANGER)

  /**
   * ### 正常
   */
  static readonly ENABLE = new DisableStatusEnum(false, '正常', AirColor.SUCCESS)
}
