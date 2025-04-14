import type { Entity } from '../base'
import { Field } from 'airpower'
import { Page } from './Page'
import { QueryRequest } from './QueryRequest'

/**
 * # 查询分页请求类
 *
 * @author Hamm.cn
 */
export class QueryPageRequest<E extends Entity> extends QueryRequest<E> {
  /**
   * ### 分页信息
   */
  @Field({
    type: Page,
  })
  page = new Page()
}
