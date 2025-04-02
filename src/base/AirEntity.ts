import type { IEntity } from 'airpower'
import { AirModel, Field } from 'airpower'

/**
 * # 实体类
 * @author Hamm.cn
 */
export class AirEntity extends AirModel implements IEntity {
  /**
   * ### `id`
   */
  static STRING_ID = 'id'

  /**
   * ### 主键 `ID`
   */
  @Field({
    label: 'ID',
    type: Number,
  })
  id!: number

  /**
   * ### 实例化一个实体
   * @param id `可选` 主键 `ID`
   */
  constructor(id?: number) {
    super()
    if (id) {
      this.id = id
    }
  }

  /**
   * ### 复制一个只包含 `ID` 的实体
   * @returns 仅包含ID的实体
   */
  copyExposeId(): this {
    return this.copy().exposeId()
  }

  /**
   * ### 只暴露 `ID`
   */
  exposeId(): this {
    return this.expose(AirEntity.STRING_ID)
  }

  /**
   * ### 排除 `ID`
   */
  excludeId(): this {
    return this.exclude(AirEntity.STRING_ID)
  }
}
