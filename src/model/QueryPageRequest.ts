import type { AirEntity } from 'airpower'
import { Field } from 'airpower'
import { Page } from './Page.ts'
import { QueryRequest } from './QueryRequest.ts'

/**
 * # 查询分页请求类
 * @author Hamm.cn
 */
export class QueryPageRequest<E extends AirEntity> extends QueryRequest<E> {
  /**
   * ### 分页信息
   */
  @Field({
    type: Page,
  })
  page = new Page()
}
