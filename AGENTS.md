# AGENTS.md — AirPower-Core

> 面向 OpenCode 代理的项目指南。基于 `dev` 分支快照（`6.4.0`）。

## 1. 项目定位

**框架无关的 Java 17 工具库**，发布到 Maven Central。是 `airpower-web` 等上层框架的基础。

- 包名根：`cn.hamm.airpower.core`
- 不依赖 Spring / ORM / Web 容器；只用 JDK + Jackson + Jakarta Validation + Servlet API + SLF4J / Logback
- 工具类风格（`XxxUtil` + 链式工厂 `XxxUtil.create().setX()`）被上层继承 / 复用， **不要轻易改公共方法签名**

## 2. 常用命令

```bash
mvn -q compile                                     # 编译
mvn -q test                                        # 全部单测
mvn -q test -Dtest=RootModelTest                   # 跑单个测试类
mvn -q test -Dtest=RootModelTest#testExcludeNotMeta # 跑单个方法
mvn -q package -DskipTests                         # 打 jar
mvn -q javadoc:javadoc                             # 生 Javadoc
```

- **不要本地跑 `mvn deploy`** —— 发布需要 GPG（`gpg.keyname` 已固化在 `pom.xml`），由 CI / 维护者处理
- Javadoc 随 `maven-javadoc-plugin` 在打包阶段自动产出（`doclint=none`）
- `maven-compiler-plugin` 开启 `-parameters`，Jackson 可读到方法参数名

## 3. 目录结构

```
src/main/java/cn/hamm/airpower/core/
├── RootModel.java                # 所有数据模型基类（核心抽象）
├── Json.java                     # {code,message,data,traceId} 统一响应包装
├── *Util.java（20 个）            # 静态工具 + 部分链式工厂
├── annotation/    （9 个注解：@Meta / @ReadOnly / @Desensitize / @Description / @Dictionary / @Export / @Phone / @DesensitizeIgnore / @ExposeAll）
├── constant/      （3 个常量类：Constant / HttpConstant / PatternConstant）
├── enums/         （3 个枚举：DateTimeFormatter / DesensitizeType / HttpMethod）
├── exception/ServiceException.java
└── interfaces/    （5 个 SPI：IDictionary / IEntity / IException / IFunction / ITree）
src/main/resources/logback.xml    # 默认日志格式（带 TRACE_ID 占位）
src/test/java/...                  # 21 个 *Test.java，与公共类一一对应
```

## 4. 三个核心抽象（最容易被外部继承/依赖，禁止改签名）

| 类型            | 角色                                                                                   | 关键约束                                                                                                                          |
|-----------------|----------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| `RootModel<M>`  | 数据模型基类，提供 `excludeReadOnly()` / `desensitize()` / `excludeNotMeta(whiteList)` | 集合里元素若是 `RootModel` 子类，会**就地修改**；`whiteList` 必须包含当前类自身，否则识别失败                                     |
| `Json`          | 既是响应包装（`code/message/data/traceId`），又是 Jackson 封装                         | 常量 `SUCCESS_CODE=200` / `SERVICE_ERROR=500` / `UNAUTHORIZED_CODE=401`；不要 `new ObjectMapper()`，统一 `Json.getObjectMapper()` |
| `IException<T>` | 枚举式异常的 SPI，提供 `when / whenNull / whenEmpty / whenEquals / show()` 等链式断言  | 上层框架会按此接口实现各自的错误枚举                                                                                              |

## 5. 编码约定（强制）

1. **JDK 17**：可用 `sealed` / pattern matching / `var`
2. **静态工具类**：`private XxxUtil() {}` + `@Contract(pure = true)`
3. **Lombok**：常用 `@Slf4j` / `@Getter` / `@Setter` / `@Accessors(chain=true)` / `@Data`； **禁止 `@SneakyThrows`**
4. **JetBrains 注解**：所有公开方法参数 / 返回值标 `@NotNull` / `@Nullable` / `@Contract`
5. **中文 Javadoc + 中文注释**：公共 API 必带 `<h1>` 标题；`doclint=none` 已开启，标点无需转义
6. **返回集合**：用 `Collections.unmodifiableList` 或 `@Unmodifiable`
7. **不要新增运行时依赖** —— 依赖受控；需新增先与维护者确认（保持库精简）

## 6. 异常与安全

- **业务失败统一抛 `cn.hamm.airpower.core.exception.ServiceException`**（继承 `RuntimeException` 实现
  `IException<ServiceException>`）；不允许直接抛 `IllegalArgumentException` 等
- `AccessTokenUtil` 的 secret **必须通过环境变量 `airpower.accessTokenSecret` 注入**，禁止硬编码
- `AesUtil`：算法 `AES/CBC/PKCS5Padding`，`key` 必须是 16 / 24 / 32 字节；`Cipher` 已缓存
- `RsaUtil`：RSA 2048 + `SHA256withRSA`，支持 PEM（Base64 字符串）密钥注入，分段加解密（`keySize/8 - 11` / `keySize/8`）
- `Json.parse*` 失败统一包装为 `ServiceException`（不是 Jackson 原生异常），便于上层拦截器统一处理

## 7. 测试约定

- 框架： **JUnit 5 (`jupiter`) 5.11.0**；测试类名严格对应公共类（`XxxTest`）
- 修改 / 新增公共方法 **必须**附带测试，覆盖：正常路径、`null`、空集合 / 空串、最大最小边界、嵌套 `RootModel`
- `ValidateUtil.valid(...)` 失败抛 `jakarta.validation.ValidationException`（ **不**是 `ServiceException`），调用方需自行处理
- `RootModelTest` 是参考模板（嵌套 / 集合 / 边界场景齐全）

## 8. 缓存策略速查

| 缓存                | 位置                                  | Key        | 模式              |
|---------------------|---------------------------------------|------------|-------------------|
| 字段列表            | `ReflectUtil.FIELD_LIST_MAP`          | `Class<?>` | `computeIfAbsent` |
| 声明字段            | `ReflectUtil.DECLARED_FIELD_LIST_MAP` | FQN        | `computeIfAbsent` |
| 导出 CSV 字段       | `CollectionUtil.EXPORT_FIELD_CACHE`   | `Class<?>` | `computeIfAbsent` |
| `DateTimeFormatter` | `DateTimeUtil.FORMATTER_CACHE`        | pattern    | 线程安全          |
| AES `Cipher`        | `AesUtil.cipherCache`                 | mode       | 并发安全          |
| `HttpClient`        | `HttpUtil.httpClient`                 | 单例       | volatile + DCL    |
| `KeyFactory`        | `RsaUtil.cachedKeyFactory`            | 单例       | volatile + DCL    |

新增工具如需缓存， **优先 `ConcurrentHashMap.computeIfAbsent`**；单例用 volatile + 双重检查。

## 9. 已知陷阱

- `RootModel.excludeNotMeta(whiteList)`：白名单必须含 `this.getClass()`，否则 `excludeFieldValueNotMeta` 会把非 `@Meta`
  字段清空
- `HttpUtil`：基于 JDK `java.net.http.HttpClient`，仅适合简单 REST，不支持 HTTP/2 流式 / 复杂重试
- `RandomUtil` 使用 `ThreadLocalRandom`， **测试不要假设全局序列**
- `TaskUtil`：守护线程池，`corePoolSize = max(2, availableProcessors())`，JVM 退出前无需 `shutdown`
- `ReflectUtil.getFieldList(null)` 直接抛 `ServiceException`；其他 `getAnnotation` 重载对 `null` 行为不一，调用前自行判空
- `AesUtil` 的 `algorithm` / `mode` 不可变（写死 `AES` / `CBC`），仅 `key` / `iv` / `padding` 可变

## 10. 改动流程

1. 先读 `pom.xml` + `RootModel` + `Json` + `IException`，理解设计再下手
2. 不要改 §4 列出的三个核心抽象的公共方法签名
3. 改缓存结构 → 必回归 `RootModelTest` / `CollectionUtilTest`
4. 完成 → `mvn -q test`，确保 21 个测试类全过
5. 改了公共 API → 更新 Javadoc（含 `<h1>` 标题）

## 11. 文件改动索引

| 任务            | 涉及位置                                                            |
|-----------------|---------------------------------------------------------------------|
| 注解 / 校验     | `annotation/`、`ValidateUtil`、`RootModel`                          |
| 字段过滤 / 脱敏 | `RootModel`、`DesensitizeUtil`、`ReflectUtil`                       |
| HTTP            | `HttpUtil`、`constant/HttpConstant`、`enums/HttpMethod`             |
| 加解密          | `AesUtil`、`RsaUtil`、`AccessTokenUtil`                             |
| 时间            | `DateTimeUtil`、`enums/DateTimeFormatter`                           |
| 树              | `TreeUtil`、`interfaces/ITree`                                      |
| 字典 / 枚举     | `DictionaryUtil`、`interfaces/IDictionary`、`annotation/Dictionary` |
| 异常            | `exception/ServiceException`、`interfaces/IException`               |
| JSON            | `Json`、`constant/HttpConstant.Header.TRACE_ID`                     |
| 文件 / 日志     | `FileUtil`、`resources/logback.xml`                                 |
| 主机名 / Trace  | `HostUtil`、`TraceUtil`                                             |

> **TL;DR**：Java 17 框架无关工具库。改任何东西前确认不会破坏 `RootModel` / `Json` / `IException` 这三个被广泛继承的核心抽象；本地不要跑
> `mvn deploy`。