import type { ClassConstructor, IJson } from 'airpower'
import type { Entity } from '../base'
import type { QueryRequest } from '../model'
import { ClassTransformer } from 'airpower'
import { QueryPageResponse } from '../model'
import { AbstractService } from './AbstractService'

/**
 * # 实体 `API` 服务超类
 *
 * @author Hamm.cn
 */
export abstract class AbstractEntityService<E extends Entity> extends AbstractService {
  /**
   * ### 为基类提供当前的实体类
   * 请求时会通过这个类进行数据转换
   */
  abstract entityClass: ClassConstructor<E>

  /**
   * ### 分页查询默认 `URL`
   */
  protected urlForGetPage = 'getPage'

  /**
   * ### 不分页查询默认 `URL`
   */
  protected urlForGetList = 'getList'

  /**
   * ### 不分页树查询默认 `URL`
   */
  protected urlForGetTreeList = 'getTreeList'

  /**
   * ### 查询详情默认 `URL`
   */
  protected urlForGetDetail = 'getDetail'

  /**
   * ### 添加默认 `URL`
   */
  protected urlForAdd = 'add'

  /**
   * ### 启用默认 `URL`
   */
  protected urlForEnable = 'enable'

  /**
   * ### 禁用默认 `URL`
   */
  protected urlForDisable = 'disable'

  /**
   * ### 修改默认 `URL`
   */
  protected urlForUpdate = 'update'

  /**
   * ### 删除默认 `URL`
   */
  protected urlForDelete = 'delete'

  /**
   * ### 查询分页数据列表
   * @param request 请求对象
   * @param apiUrl `可选` 自定义请求URL
   */
  async getPage(request: QueryRequest<E>, apiUrl = this.urlForGetPage): Promise<QueryPageResponse<E>> {
    const responsePage: QueryPageResponse<E> = await this.api(apiUrl).postGet(request, QueryPageResponse<E>)
    responsePage.list = ClassTransformer.parseArray(responsePage.list as IJson[], this.entityClass)
    return responsePage
  }

  /**
   * ### 查询不分页数据列表
   * @param request 请求对象
   * @param apiUrl `可选` 自定义请求URL
   */
  async getList(request: QueryRequest<E>, apiUrl = this.urlForGetList): Promise<E[]> {
    return this.api(apiUrl).postGetArray(request, this.entityClass)
  }

  /**
   * ### 查询树结构数据数组
   * @param request 请求对象
   * @param apiUrl `可选` 自定义请求URL
   */
  async getTreeList(request: QueryRequest<E>, apiUrl = this.urlForGetTreeList): Promise<E[]> {
    return this.api(apiUrl).postGetArray(request, this.entityClass)
  }

  /**
   * ### 根据 `ID` 获取详情对象
   * @param id ID
   * @param apiUrl `可选` 自定义请求URL
   */
  async getDetail(id: number, apiUrl = this.urlForGetDetail): Promise<E> {
    return this.api(apiUrl).postGet(this.newEntityInstance(id), this.entityClass)
  }

  /**
   * ### 添加一条新的数据
   * @param data 保存的数据
   * @param message `可选` 添加成功的消息提示内容
   * @param apiUrl `可选` 自定义请求URL
   */
  async add(data: E, message = '添加成功', apiUrl = this.urlForAdd): Promise<number> {
    const saved = await this.api(apiUrl).postGet(data, this.entityClass)
    this.showSuccess(message)
    return saved.id
  }

  /**
   * ### 修改一条数据
   * @param data 修改的数据实体
   * @param message `可选` 修改成功的消息提示内容
   * @param apiUrl `可选` 自定义请求URL
   */
  async update(data: E, message = '修改成功', apiUrl = this.urlForUpdate): Promise<void> {
    await this.api(apiUrl).post(data)
    this.showSuccess(message)
  }

  /**
   * ### 保存一条数据并返回主键 `ID`
   *
   * 如包含 `ID` 则更新 如不包含 则创建
   * @param data 保存的数据实体
   * @param message `可选` 保存成功的消息提示内容
   */
  async save(data: E, message = '保存成功'): Promise<number> {
    if (data.id) {
      await this.update(data, message)
      return data.id
    }
    return this.add(data, message)
  }

  /**
   * ### 根据 `ID` 删除一条数据
   * @param id 删除的数据 `ID`
   * @param message `可选` 删除成功的消息提示内容
   * @param apiUrl `可选` 自定义请求URL
   */
  async delete(id: number, message = '删除成功', apiUrl = this.urlForDelete): Promise<void> {
    const instance = this.newEntityInstance(id)
    try {
      await this.api(apiUrl).throwError().post(instance)
      this.showSuccess(message)
    }
    catch (err) {
      this.showError((err as Error).message)
    }
  }

  /**
   * ### 根据 `ID` 禁用一条数据
   * @param id 禁用的数据 `ID`
   * @param message `可选` 禁用成功的消息提示内容
   * @param apiUrl `可选` 自定义请求URL
   */
  async disable(id: number, message = '禁用成功', apiUrl = this.urlForDisable): Promise<void> {
    const instance = this.newEntityInstance(id)
    try {
      await this.api(apiUrl).throwError().addHttpHeader('a', 'b').post(instance)
      this.showSuccess(message)
    }
    catch (err) {
      this.showError((err as Error).message)
    }
  }

  /**
   * ### 根据 `ID` 启用一条数据
   * @param id 启用的数据 `ID`
   * @param message `可选` 启用成功的消息提示内容
   * @param apiUrl `可选` 自定义请求URL
   */
  async enable(id: number, message = '启用成功', apiUrl = this.urlForEnable): Promise<void> {
    try {
      await this.api(apiUrl).throwError().post(this.newEntityInstance(id))
      this.showSuccess(message)
    }
    catch (err) {
      this.showError((err as Error).message)
    }
  }

  /**
   * ### 显示成功消息
   * @param successMessage 成功消息
   */
  protected abstract showSuccess(successMessage: string): void

  /**
   * ### 显示错误信息
   * @param errorMessage 错误信息
   */
  protected abstract showError(errorMessage: string): void

  /**
   * ### 创建一个实体的实例
   * @param id `可选` `ID`
   */
  protected newEntityInstance(id?: number): E {
    // eslint-disable-next-line new-cap
    const entity = new this.entityClass()
    if (id) {
      entity.id = id
    }
    return entity
  }
}
