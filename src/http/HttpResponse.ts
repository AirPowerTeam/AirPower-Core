/**
 * # Http请求的响应
 *
 * @author Hamm.cn
 */
export class HttpResponse {
  /**
   * ### 业务状态码
   */
  code!: number

  /**
   * ### 返回的数据
   */
  data!: unknown

  /**
   * ### 错误信息
   */
  message!: string
}
