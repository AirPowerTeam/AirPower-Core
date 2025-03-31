import { CoreConfig } from '../config'

/**
 * # HTTP 异常
 * @author Hamm.cn
 */
export class HttpResponseError extends Error {
  code!: number

  /**
   * ### 初始化一个错误
   * @param message 消息
   * @param code 代码
   */
  constructor(message: string, code = CoreConfig.defaultErrorCode) {
    super(message)
    this.code = code
  }
}
