import type { AirAny, AirModel, ClassConstructor, IJson } from 'airpower'
import { AirClassTransformer, AirConstant } from 'airpower'
import { Constant, CoreConfig } from '../config'
import { HttpContentType } from './HttpContentType'
import { HttpMethod } from './HttpMethod'
import { HttpResponse } from './HttpResponse'

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
   * ### 错误回调
   */
  private errorCallback?: (error: HttpResponse) => void
  /**
   * ### 是否直接抛出错误
   */
  private isThrowError = false

  /**
   * ### 创建一个客户端
   * @param url 请求的 `URL`
   * @param errorCallback  异常回调
   */
  static create<R extends AbstractHttp>(this: ClassConstructor<R>, url: string, errorCallback?: (error: HttpResponse) => void): R {
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
    service.errorCallback = errorCallback
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
   * ### 是否直接抛出错误
   * @param isThrowError 是否回调错误
   */
  throwError(isThrowError = true): this {
    this.isThrowError = isThrowError
    return this
  }

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
    return new Promise((resolve, reject) => {
      this.startLoading()
      this.request(body).then((response) => {
        if (response.code === CoreConfig.unAuthorizeCode) {
          // 需要登录
          if (this.isThrowError || !this.errorCallback) {
            reject(response)
            return
          }
          this.errorCallback(response)
          return
        }
        if (response.code !== CoreConfig.successCode) {
          if (this.isThrowError || !this.errorCallback) {
            reject(response)
            return
          }
          this.errorCallback(response)
          return
        }
        resolve(response.data as AirAny)
      }).catch((e) => {
        const error = new HttpResponse()
        error.message = e.message
        error.code = CoreConfig.defaultErrorCode
        if (this.isThrowError || !this.errorCallback) {
          reject(error)
          return
        }
        this.errorCallback(error)
      }).finally(() => {
        this.stopLoading()
      })
    })
  }
}
