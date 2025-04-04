import { HttpHeader, HttpStatus } from './enum'

/**
 * # HTTP 配置
 *
 * @author Hamm.cn
 */
export class HttpConfig {
  /**
   * ### 接口根地址
   * 以 `/` 结尾
   */
  static apiUrl = '/api/'

  /**
   * ### `AccessToken` 对应的 `Key`
   */
  static authorizationHeaderKey = HttpHeader.AUTHORIZATION

  /**
   * ### 全局 `http` 请求返回 成功状态码
   */
  static successCode = HttpStatus.OK

  /**
   * ### 全局 `http` 请求返回 登录状态码
   */
  static unAuthorizeCode = HttpStatus.UNAUTHORIZED

  /**
   * ### 超时时间 毫秒
   * 超时后请求会自动断开并抛出异常
   */
  static timeout = 5000
}
