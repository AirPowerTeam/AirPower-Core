import type { AirAny, AirModel, ClassConstructor, IJson } from 'airpower'
import type { HttpResponse } from './HttpResponse.ts'
import { AirClassTransformer, AirConstant, AirEvent } from 'airpower'
import { Constant, CoreConfig } from '../config'
import { CoreEvents } from '../enum'
import { HttpContentType } from './HttpContentType.ts'
import { HttpMethod } from './HttpMethod.ts'
import { HttpResponseError } from './HttpResponseError.ts'

/**
 * # 网络请求类
 * @author Hamm.cn
 */
export abstract class AbstractHttp {
  /**
   * ### 请求方法
   */
  private method = HttpMethod.POST

  /**
   * ### URL
   */
  private url = AirConstant.STRING_EMPTY

  /**
   * ### 请求超时时间
   */
  private timeout = CoreConfig.timeout

  /**
   * ### 是否携带 `Cookies`
   */
  private sendCookie = false

  /**
   * ### 请求头
   */
  private headers: IJson = {}

  /**
   * ### 是否回调错误
   */
  private isCallbackError = false

  /**
   * ### 创建一个客户端
   * @param url 请求的 `URL`
   */
  static create<R extends AbstractHttp>(this: ClassConstructor<R>, url: string): R {
    const service = Object.assign(new this()) as R
    if (url.includes(AirConstant.PREFIX_HTTP) || url.includes(AirConstant.PREFIX_HTTPS)) {
      service.url = url
    }
    else {
      service.url = CoreConfig.apiUrl + url
    }
    service.headers[Constant.CONTENT_TYPE] = HttpContentType.JSON
    const accessToken = service.getAccessToken()
    if (accessToken) {
      service.headers[CoreConfig.authorizationHeaderKey] = accessToken
    }
    return service
  }

  /**
   * ### 获取请求的 `AccessToken`
   */
  abstract getAccessToken(): string

  /**
   * ### 设置请求超时时间
   * @param timeout 超时毫秒数
   */
  setTimeout(timeout: number) {
    this.timeout = timeout
    return this
  }

  /**
   * ### 获取请求超时时间
   */
  getTimeout(): number {
    return this.timeout
  }

  /**
   * ### 设置是否回调错误
   * @param isCallbackError 是否回调错误
   */
  callbackError(isCallbackError = true): this {
    this.isCallbackError = isCallbackError
    return this
  }

  /**
   * ### 获取请求的 `URL`
   */
  getUrl(): string {
    return this.url
  }

  /**
   * ### 设置请求头
   * @param header 请求头
   */
  setHttpHeader(header: IJson): this {
    this.headers = header
    return this
  }

  /**
   * ### 获取请求头
   */
  getHttpHeaders(): IJson {
    return this.headers
  }

  /**
   * ### 允许携带 `Cookies`
   */
  withCookies(): this {
    this.sendCookie = true
    return this
  }

  getWithCookies(): boolean {
    return this.sendCookie
  }

  /**
   * ### 添加一个请求头
   * @param key 请求头 `key`
   * @param value 请求头 `value`
   */
  addHttpHeader(key: string, value: string): this {
    this.headers[key] = value
    return this
  }

  /**
   * ### 设置请求方法
   * @param method 请求方法
   */
  setMethod(method: HttpMethod): this {
    this.method = method
    return this
  }

  /**
   * ### 获取请求方法
   */
  getMethod(): HttpMethod {
    return this.method || HttpMethod.POST
  }

  /**
   * ### 设置请求`content-type`
   * @param contentType `content-type`
   */
  setContentType(contentType: HttpContentType): this {
    return this.addHttpHeader(Constant.CONTENT_TYPE, contentType)
  }

  /**
   * ### 发送 `POST`
   * @param postData 发送的数据模型(数组)
   */
  post<T extends AirModel>(postData?: T | T[]): Promise<IJson | IJson[]> {
    let json = {}
    if (postData) {
      if (Array.isArray(postData)) {
        json = postData.map(item => item.toJson())
      }
      else {
        json = postData.toJson()
      }
    }
    this.setMethod(HttpMethod.POST)
    return this.send(json)
  }

  /**
   * ### 发送请求并获取转换后的模型
   * @param postData 请求的数据
   * @param parseClass 返回的模型
   */
  async postGet<REQ extends AirModel, RES extends AirModel>(
    postData: REQ | REQ[] | undefined,
    parseClass: ClassConstructor<RES>,
  ): Promise<RES> {
    const result = await this.post(postData)
    return AirClassTransformer.parse(result, parseClass)
  }

  /**
   * ### 发送请求并获取转换后的模型列表
   * @param postData 请求的数据
   * @param parseClass 返回的模型列表
   */
  async postGetArray<REQ extends AirModel, RES extends AirModel>(
    postData: REQ | REQ[] | undefined,
    parseClass: ClassConstructor<RES>,
  ): Promise<RES[]> {
    const result = await this.post(postData)
    return AirClassTransformer.parseArray(result as IJson[], parseClass)
  }

  /**
   * ### 发送请求
   *
   * @param body `可选` 请求体
   * @see post() 直接发送 `POST`
   * @see get() 直接发送 `GET`
   */
  abstract request(body?: unknown): Promise<HttpResponse>

  /**
   * ### 开始加载
   */
  protected startLoading(): void {
  }

  /**
   * ### 停止加载
   */
  protected stopLoading(): void {
  }

  /**
   * ### 发送请求
   * @param body 请求体
   */
  private async send(body?: unknown): Promise<IJson | IJson[]> {
    this.startLoading()
    const response = await this.request(body)
    this.stopLoading()
    return new Promise((resolve, reject) => {
      if (response.code === CoreConfig.unAuthorizeCode) {
        // 需要登录
        if (this.isCallbackError) {
          reject(new HttpResponseError(response.message, response.code))
          return
        }
        AirEvent.emit(CoreEvents.NEED_LOGIN, response)
        return
      }
      if (response.code !== CoreConfig.successCode) {
        if (this.isCallbackError) {
          reject(new HttpResponseError(response.message, response.code))
          return
        }
        AirEvent.emit(CoreEvents.HTTP_ERROR, response)
        return
      }
      resolve(response.data as AirAny)
    })
  }
}
