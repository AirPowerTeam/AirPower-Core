import type { ClassConstructor } from 'airpower'
import type { AbstractHttp } from '../http'
import { AirModel } from 'airpower'

/**
 * # `API` 服务超类
 * @author Hamm.cn
 */
export abstract class AbstractService extends AirModel {
  /**
   * ### `API` 目录地址
   * 一般对应后端的 `分组/控制器/目录` 等
   */
  protected abstract baseUrl: string

  /**
   * ### 静态创建一个 `API` 服务实例
   */
  static create<S extends AbstractService>(this: ClassConstructor<S>): S {
    return new this()
  }

  /**
   * ### 创建一个 `AirHttp` 实例
   * @param url 请求的接口地址
   * @param baseUrl `可选` 请求的接口目录
   */
  api(url: string, baseUrl?: string): AbstractHttp {
    return this.createHttp(`${baseUrl || this.baseUrl}/${url}`)
  }

  /**
   * ### 请你实现
   * @param url 请求的地址
   */
  protected abstract createHttp(url: string): AbstractHttp
}
