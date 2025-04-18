import type { Entity } from '../base'
import { AirPower, Field } from 'airpower'
import { Page } from './Page'
import { QuerySort } from './QuerySort'

/**
 * # 查询分页响应类
 *
 * @author Hamm.cn
 */
export class QueryPageResponse<E extends Entity> extends AirPower {
  /**
   * ### 返回的当前页数据列表
   */
  list: E[] = []

  /**
   * ### 返回的页码信息
   */
  @Field({
    type: Page,
  })
  page = new Page()

  /**
   * ### 返回的排序信息
   */
  @Field({
    type: QuerySort,
  })
  sort = new QuerySort()

  /**
   * ### 返回总条数
   */
  @Field({
    type: Number,
  })
  total = 0

  /**
   * ### 返回总页数
   */
  @Field({
    type: Number,
  })
  pageCount = 0
}
