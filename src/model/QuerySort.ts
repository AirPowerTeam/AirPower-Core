import type { SortType } from './type'
import { AirModel } from 'airpower'
import { AirEntity } from '../base'

/**
 * # 查询排序类
 * @author Hamm.cn
 */
export class QuerySort extends AirModel {
  /**
   * ### 排序字段 默认 `id`
   */
  field = AirEntity.STRING_ID

  /**
   * ### 排序方式 默认 `desc`
   */
  direction: SortType = 'desc'

  /**
   * ### 设置排序字段名
   * @param field 字段名
   */
  setField(field: string): this {
    this.field = field
    return this
  }

  /**
   * ### 设置排序方向
   * @param direction 方向
   */
  setDirection(direction: SortType): this {
    this.direction = direction
    return this
  }
}
