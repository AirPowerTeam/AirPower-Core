import type { ClassConstructor } from 'airpower'
import type { Ref } from 'vue'
import type { AbstractHttp } from '../http/AbstractHttp.ts'
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
  abstract baseUrl: string

  /**
   * ### `Loading`
   * 你可以将这个传入的对象绑定到你需要 `Loading` 的 `DOM` 上
   */
  loading!: Ref<boolean>

  /**
   * ### 获取一个 `API` 服务实例
   * @param loading `可选` Loading
   */
  constructor(loading?: Ref<boolean>) {
    super()
    if (loading) {
      this.loading = loading
    }
  }

  /**
   * ### 静态创建一个 `API` 服务实例
   * @param loading `可选` Loading
   */
  static create<S extends AbstractService>(this: ClassConstructor<S>, loading?: Ref<boolean>): S {
    const service = Object.assign(new this()) as S
    if (loading) {
      service.loading = loading
    }
    return service
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
  abstract createHttp(url: string): AbstractHttp
}
