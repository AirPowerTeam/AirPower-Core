import type { HttpHeader, HttpStatus } from './enum'

/**
 * # 请求头KEY类型
 */
export type HttpHeaderKey = HttpHeader | string

/**
 * # 请求头记录
 */
export type HttpHeaderRecord = Record<HttpHeaderKey, unknown>

/**
 * # 状态码类型
 */
export type HttpStatusNumber = HttpStatus | number
