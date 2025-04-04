/**
 * # ContentType
 *
 * @author Hamm.cn
 */
export enum HttpContentType {
  /**
   * ### JSON
   */
  JSON = 'application/json',

  /**
   * ### 表单
   */
  FORM_URLENCODED = 'application/x-www-form-urlencoded',

  /**
   * ### 文件上传
   */
  MULTIPART_FORM_DATA = 'multipart/form-data',

  /**
   * ### XML
   */
  XML = 'application/xml',

  /**
   * ### 纯文本
   */
  PLAIN = 'text/plain',
}
