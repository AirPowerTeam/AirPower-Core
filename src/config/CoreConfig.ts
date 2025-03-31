/**
 * # 全局配置
 * @author Hamm.cn
 */
export class CoreConfig {
  /**
   * ### 接口根地址
   * 以 `/` 结尾
   */
  static apiUrl = '/api/'

  /**
   * ### `AccessToken` 对应的 `Key`
   * `缓存的名称` 和 `Api传输的Header` 都叫这个名字
   */
  static authorizationHeaderKey = 'Authorization'

  /**
   * ### 全局 `http` 请求返回 成功状态码
   */
  static successCode = 200

  /**
   * ### 全局 `http` 请求返回 错误状态码
   */
  static defaultErrorCode = 500

  /**
   * ### 全局 `http` 请求返回 登录状态码
   */
  static unAuthorizeCode = 401

  /**
   * ### 超时时间 毫秒
   * 超时后请求会自动断开并抛出异常
   */
  static timeout = 5000
}
