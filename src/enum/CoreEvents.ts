/**
 * # 事件枚举
 * @author Hamm.cn
 */
export enum CoreEvents {
  /**
   * ### 新增成功
   */
  ADD_SUCCESS = 'CORE_ADD_SUCCESS',

  /**
   * ### 删除成功
   */
  DELETE_SUCCESS = 'CORE_DELETE_SUCCESS',

  /**
   * ### 更新成功
   */
  UPDATE_SUCCESS = 'CORE_UPDATE_SUCCESS',

  /**
   * ### 删除失败
   */
  DELETE_FAIL = 'CORE_DELETE_FAIL',

  /**
   * ### 禁用成功
   */
  DISABLE_SUCCESS = 'CORE_DISABLE_SUCCESS',

  /**
   * ### 启用失败
   */
  ENABLE_FAIL = 'CORE_ENABLE_FAIL',

  /**
   * ### 启用成功
   */
  ENABLE_SUCCESS = 'CORE_ENABLE_SUCCESS',

  /**
   * ### http请求失败
   */
  HTTP_ERROR = 'CORE_HTTP_ERROR',

  /**
   * ### 需要登录
   */
  NEED_LOGIN = 'CORE_HTTP_LOGIN',
}
