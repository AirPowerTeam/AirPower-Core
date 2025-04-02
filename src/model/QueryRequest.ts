import type { ClassConstructor } from 'airpower'
import type { AirEntity } from '../base'
import { AirClassTransformer, AirModel, Field } from 'airpower'
import { QuerySort } from './QuerySort'

/**
 * # 请求类
 * @author Hamm.cn
 */
export class QueryRequest<E extends AirEntity = AirEntity> extends AirModel {
  /**
   * ### 查询信息
   */
  filter!: E

  /**
   * ### 排序信息
   */
  @Field({
    type: QuerySort,
  })
  sort?: QuerySort

  /**
   * ### 初始化一个请求类
   * @param filterClass 如传入 `filter` 的类 将自动初始化一个空 `filter`
   */
  constructor(filterClass: ClassConstructor<E>) {
    super()
    this.filter = AirClassTransformer.parse({}, filterClass)
  }

  /**
   * ### 设置排序对象
   * @param sort 排序对象
   */
  setSort(sort: QuerySort): this {
    this.sort = sort
    return this
  }
}
