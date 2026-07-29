# AGENTS.md — AirPower-Core 项目分析与代理指南

> 本文档面向在该项目上协作的 AI 代理（opencode / 其他）。
> 基于仓库 `main` 分支当前快照（version `6.4.0`）编写。

---

## 1. 项目概况

| 项目          | 内容                                                            |
| ----------- | ------------------------------------------------------------- |
| 名称          | `airpower-core`                                                |
| GroupId     | `cn.hamm`                                                      |
| ArtifactId  | `airpower-core`                                                |
| Version     | `6.4.0`                                                        |
| 语言 / JDK    | Java 17 (`maven.compiler.source/target = 17`)                 |
| 构建系统       | Maven (发布到 Maven Central via `central-publishing-maven-plugin`) |
| 许可证        | MIT                                                            |
| 仓库          | GitHub `AirPowerTeam/AirPower-Core` / Gitee `air-power/AirPower-Core` |
| 角色          | **核心工具包（无 Spring、无 Web 依赖）**，为 `airpower-web` 等上层框架提供通用能力 |

定位：**与框架无关的基础工具库**。不依赖 Spring / 数据库 ORM / Web 容器，只使用 JDK + Jackson + Jakarta Validation + Servlet API + SLF4J / Logback。

---

## 2. 目录结构

```
src/main/java/cn/hamm/airpower/core/
├── RootModel.java              # 所有数据模型的基类（核心抽象）
├── Json.java                   # 统一响应包装 {code,message,data,traceId}
├── AccessTokenUtil.java        # HMAC-SHA256 自签名 AccessToken
├── AesUtil.java                # AES/CBC/PKCS5Padding 加解密（Cipher 缓存）
├── RsaUtil.java                # RSA 加解密 + 签名验签，支持分段
├── CollectionUtil.java         # 集合 / CSV 导出
├── DateTimeUtil.java           # 时间日期格式化、友好格式、加减
├── DesensitizeUtil.java        # 字符串脱敏（内置 9 种类型）
├── DictionaryUtil.java         # 枚举字典反射查询 / Lambda 方法名获取
├── FileUtil.java               # 文件读写、目录、ZIP 压缩
├── HostUtil.java               # 跨平台主机名获取
├── HttpUtil.java               # JDK11 HttpClient 封装
├── Json.java                   # JSON 序列化 + 统一返回结构
├── NumberUtil.java             # BigDecimal/BigInteger 安全算术
├── RandomUtil.java             # 线程安全随机数/字符串/字节
├── ReflectUtil.java            # 字段读取 / 注解递归 / Lambda 解析
├── StringUtil.java             # 字符串工具（Spring StringUtils 风格）
├── TaskUtil.java               # 守护线程池异步任务
├── TraceUtil.java              # SLF4J MDC TraceID
├── TreeUtil.java               # O(n) 树构建（Map 优化）
├── ValidateUtil.java           # Jakarta Validation + 身份证正则
├── annotation/                 # 9 个注解
├── constant/                   # 3 个常量类
├── enums/                      # 3 个枚举
├── exception/ServiceException.java
└── interfaces/                 # 5 个核心 SPI 接口

src/main/resources/logback.xml  # 默认日志格式（含 TRACE_ID）
src/test/java/...               # 21 个测试类，覆盖所有公共类
```

---

## 3. 设计原则（务必遵守）

### 3.1 静态工具优先

> 所有 `XxxUtil` 类：

- **私有构造（`private XxxUtil() {}`）**，禁止实例化。
- 方法全部 `public static`。
- 使用 `@Contract` 标注 JetBrains 注解以提升 IDE 提示。
- 例外：**有状态工具**仍采用"工厂 + 链式"模式（`create().setX().setY()`），例如 `AesUtil`、`RsaUtil`、`HttpUtil`、`AccessTokenUtil`。

### 3.2 不可变性默认

- 所有公开集合返回值都标注 `@NotNull` + `@Unmodifiable` / `@UnmodifiableView`。
- `ReflectUtil.getFieldList` 已使用 `Collections.unmodifiableList` 包装。
- 反射结果通过 `ConcurrentHashMap` 缓存，**不要在工具方法中手动 new 出可变集合暴露给调用方**。

### 3.3 异常统一为 `ServiceException`

- **所有业务/校验失败必须抛 `cn.hamm.airpower.core.exception.ServiceException`**（继承 `RuntimeException` 实现 `IException<ServiceException>`）。
- 错误码默认 `Json.SERVICE_ERROR (500)`；授权类失败用 `Json.UNAUTHORIZED_CODE (401)`（见 `AccessTokenUtil.throwException`）。
- 不允许直接抛 `RuntimeException`、`IllegalArgumentException` 等，除非是 JDK API 限制。

### 3.4 注解驱动 + 反射

- 字段语义通过 **注解** 表达：`@Meta`、`@ReadOnly`、`@Desensitize`、`@Description`、`@Dictionary`、`@Export`、`@Phone`。
- 处理逻辑通过 `RootModel` 的 `excludeReadOnly()` / `desensitize()` / `excludeNotMeta()` 触发。
- 反射工具 `ReflectUtil` 在以下场景使用：
  - 递归读取注解（沿父类向上直到 `Object`）
  - 缓存字段列表与 `getDeclaredFields`
  - 通过 `SerializedLambda` 从 `IDictionary` 实现的 getter lambda 提取字段名

### 3.5 缓存策略

| 缓存                | 类                            | Key             | 备注                          |
| ----------------- | ---------------------------- | --------------- | --------------------------- |
| 字段列表              | `ReflectUtil.FIELD_LIST_MAP` | `Class<?>`      | 一次性遍历父类链                     |
| 声明字段              | `ReflectUtil.DECLARED_FIELD_LIST_MAP` | `String` (FQN) | 调用 `getDeclaredFields` 后缓存 |
| 导出 CSV 字段         | `CollectionUtil.EXPORT_FIELD_CACHE` | `Class<?>`      | 含 `Export` 注解的字段             |
| DateTimeFormatter | `DateTimeUtil.FORMATTER_CACHE` | `String` pattern | 线程安全                        |
| AES Cipher        | `AesUtil.cipherCache`        | `Integer` mode  | 并发安全                        |
| HttpClient        | `HttpUtil.httpClient`        | 单例             | DCL                         |
| RSA KeyFactory    | `RsaUtil.cachedKeyFactory`   | 单例             | DCL                         |

> 新增工具若需要缓存，**优先使用 `ConcurrentHashMap.computeIfAbsent`**；单例用 volatile + 双重检查。

---

## 4. 核心抽象详解

### 4.1 `RootModel<M extends RootModel<M>>`

> 所有数据模型的基类，相当于"自带切面"。

三种核心行为：

| 方法                                          | 作用                                                           |
| ------------------------------------------- | ------------------------------------------------------------ |
| `excludeReadOnly()`                         | 把标了 `@ReadOnly` 的字段置 `null`（防止前端回传覆盖）                          |
| `desensitize()`                             | 对标了 `@Desensitize` 的字段执行脱敏；对嵌套 `RootModel` 递归；集合中的每一项也递归  |
| `excludeNotMeta()`                          | 清掉**没有**标 `@Meta` 的字段（白名单 = 当前类自身）                           |
| `excludeNotMeta(whiteList)`                 | 仅清掉 `whiteList` 之外的模型的非 `@Meta` 字段                              |
| `excludeNotMetaAndDesensitize(w, d)`        | 上述两个的组合入口                                                   |
| `static isModel(Class<?>)`                  | 沿父链判断是否为 `RootModel` 子类                                       |

**注意事项**：

- 集合字段中如果元素本身是 `RootModel`，会**就地修改**（`itemModel.excludeNotMetaAndDesensitize(...)`）。
- `desensitizeFieldValue` 中：如果 `replace() == true`，整字段替换为 `symbol`；否则按 `head/tail` 范围替换。非字符串字段直接置 `null`。
- 没有 setter 的字段（private 但只有 getter）也能正常脱敏，因为反射走 `Field.setAccessible(true)`。

### 4.2 `Json`

> 既是"统一响应包装"又是"Jackson 包装"。

字段（务必保持兼容）：

```java
int code = Json.SUCCESS_CODE;   // 200 成功 / 500 错误 / 401 未授权
String message = "";
Object data;
@JsonInclude(NON_NULL) String traceId;
```

关键方法：

- 工厂：`Json.success(msg)` / `Json.data(data[, msg])` / `Json.error(...)` 多重载 / `Json.show(code, msg, data)`。
- 序列化：`Json.toString(obj)`、`Json.parse(json, Class)`、`Json.parse(json, TypeReference)`、`Json.parse2Map` / `parse2MapList` / `parseList`。
- `ObjectMapper` 单例（DCL），配置：
  - `FAIL_ON_UNKNOWN_PROPERTIES = false`
  - `Map` 类型 `Include.NON_EMPTY`
  - `FAIL_ON_EMPTY_BEANS = false`

> 不要直接 `new ObjectMapper()`，统一使用 `Json.getObjectMapper()`。

### 4.3 `IException` & `ServiceException`

`IException<T extends IException<T>> extends Supplier<T>` 是枚举式异常的"标配套件"：

- `when(cond[, msg][, data])`：`cond == true` 时 `show`。
- `whenNull / whenEmpty / whenEquals / whenNotEquals` 等一连串便捷断言。
- `get()` 返回自身（用于链式）。
- `show()` 直接抛 `ServiceException(this.getCode(), message, data)`。

调用示例（业务层推荐写法）：

```java
UserError.PASSWORD_INCORRECT.when(!matches);
```

> **`cn.hamm.airpower.core.enums` 包目前未含错误枚举**，错误枚举由上层（如 `airpower-web`）按相同 `IException` 接口实现并维护。

### 4.4 `IDictionary` + `DictionaryUtil`

任何枚举只要实现 `IDictionary`（提供 `int getKey()` / `String getLabel()`），即可：

- 用 `DictionaryUtil.getDictionary(class, key)` 反查枚举实例。
- 用 `DictionaryUtil.getDictionaryList(class, IDictionary::getKey, IDictionary::getLabel, ...)` 拿到 `List<Map<String,Object>>` 输出给前端。
- 通过 `ReflectUtil.getLambdaFunctionName(IFunction)` 把 Lambda 还原成属性名（如 `getKey` -> `key`），配合 `@Dictionary` 注解 `@Export(Type.DICTIONARY)` 自动在 CSV 中渲染中文。

### 4.5 树与实体：`ITree` / `IEntity`

- `IEntity<E>`：所有实体至少有 `Long getId()` / `E setId(Long)`。
- `ITree<E> extends IEntity<E>`：增加 `parentId`、`children`。
- `TreeUtil.buildTreeList(list)`：**O(n)** 实现 — 先 `Map<Long,List<E>> parentMap` 一次扫描，再递归构建子树。

### 4.6 验证：`ValidateUtil`

- 字符串正则（数字、邮箱、手机号、座机、中文、字母、URL 安全字符集等）— 在 `PatternConstant`。
- Jakarta Bean Validation：`valid(model, actions...)` 失败抛 `ValidationException`（注意：这里没有用 `ServiceException` 包裹，调用方需自行处理）。
- 二代身份证校验算法完整实现。

---

## 5. 安全相关

| 工具                | 算法                                     | 备注                                                         |
| ----------------- | -------------------------------------- | ---------------------------------------------------------- |
| `AesUtil`         | AES/CBC/PKCS5Padding                   | `key` 必须 16/24/32 字节；`iv` 默认 `0000000000000000`；**Cipher 已缓存** |
| `RsaUtil`         | RSA 2048 + `SHA256withRSA`             | 分段加解密（`keySize/8 - 11` 加密 / `keySize/8` 解密）；支持 PEM 转换      |
| `AccessTokenUtil` | `HMAC-SHA256` 自签名                      | Token = `Base64(expire.sign(secret).payloadBase64)`       |
| `TraceUtil`       | UUID + SLF4J MDC                       | Key = `HttpConstant.Header.TRACE_ID`                      |

**强制要求**：

- 不要在代码里硬编码 secret / 密钥，**必须**通过环境变量 `airpower.accessTokenSecret` 传入（参见 `AccessTokenUtil.verify()` 的错误信息）。
- RSA 私钥/公钥通过 `setPrivateKey(String)` / `setPublicKey(String)` 注入（Base64 后的字符串）。

---

## 6. 编码约定（强制）

1. **JDK 基线 17** — 可使用 `sealed`、pattern matching、`var` 等。
2. **编辑器参数 `-parameters`** — 已开启，调用 Jackson 时可读出方法参数名（pom.xml 配置）。
3. **Lombok**：
   - `@Slf4j`、`@Getter`、`@Setter`、`@Accessors(chain = true)`、`@Data`、`@RequiredArgsConstructor` 等均可。
   - 不要在源代码出现 `@SneakyThrows`。
4. **JetBrains 注解**：所有公开方法参数与返回值标注 `@NotNull` / `@Nullable` / `@Contract`。这是项目风格的一部分。
5. **中文注释** + **中文 Javadoc `<h1>` 标题**。新增公共 API 必须保留中文 Javadoc；Javadoc `<h1>` 标签代表 API 名称。
6. **不可变性**：方法返回集合时使用 `Collections.unmodifiableList` 或 `@Unmodifiable` 注解。
7. **私有构造函数**：静态工具类必须 `private XxxUtil() {}` 并加 `@Contract(pure = true)`。
8. **不要新增运行时依赖**：依赖在 `pom.xml` 中受控；若需新增必须先与维护者确认（理由：本库需保持精简，常作为上层框架基础）。
9. **JavaDoc 的 `doclint=none`**：可在 `@param` / `@return` 中使用中文 + 标点而无需转义；但请保持 markdown 风格整洁。
10. **构建 & 签名**：发布到 Central 时需要 GPG 签名（`gpg.keyname` 已固化在 pom）；**本地开发不要跑 `mvn deploy`**，`clean package` + 单测即可。

---

## 7. 测试约定

- 测试框架：**JUnit 5 (`jupiter`)** 5.11.0。
- 每个公共类有一个对应的 `XxxUtilTest.java`（21 个测试类，且**已实现** RootModel 的覆盖率达到嵌套/集合/边界条件）。
- `RootModelTest` 值得参考的模型有：`TestModel` / `ParentModel`（嵌套） / `CollectionModel`（集合） / `EmptyDesensitizeModel` / `ShortDesensitizeModel` / `NullChildModel` / `EmptyCollectionModel`。

**新增/修改公共方法时必须附带 JUnit 5 测试**，覆盖：

- 正常路径
- 空值 (`null`)
- 空集合 / 空字符串
- 边界（最大 / 最小）
- 嵌套 `RootModel`

运行测试：

```bash
mvn test                        # 全部
mvn test -Dtest=RootModelTest   # 单类
```

---

## 8. 常用工作流速查

### 8.1 添加新字段类型注解

1. 在 `cn.hamm.airpower.core.annotation` 新建 `@Xxx`。
2. 如果要配合 `RootModel` 行为，需在 `RootModel` 的 `filterModelFieldValue` 中扩展；若仅作为 Bean Validation 用，新增 `ConstraintValidator` 即可（参考 `@Phone` / `@Dictionary`）。
3. 在 `ReflectUtil.getDescription(...)` 系列外无需其他改动，注解默认 `RUNTIME` 可见。
4. 写测试。

### 8.2 添加新枚举（字典）

```java
public enum Sex implements IDictionary {
    UNKNOWN(0, "未知"),
    MALE(1, "男"),
    FEMALE(2, "女");

    private final int key;
    private final String label;
    Sex(int key, String label) { this.key = key; this.label = label; }
    @Override public int getKey() { return key; }
    @Override public String getLabel() { return label; }
}
```

在 `RootModel` 子类字段加：`@Dictionary(Sex.class)`（自动校验） + `@Export(value = DICTIONARY, sort=1)`（自动渲染中文）。

### 8.3 输出响应

控制器层（或其他调用方）：

```java
return Json.data(service.list());           // 200 + 数据
return Json.success("操作成功");                // 200 + 提示
throw new ServiceException(404, "未找到");      // 业务异常
```

---

## 9. 已知约束与陷阱

- `RootModel.excludeNotMeta(whiteList)` 中，**白名单类型必须使用包含自身类型的列表**，否则不会被识别（来自 `RootModel:65`：`List.of((Class<? extends RootModel<?>>) this.getClass())`）。
- `AesUtil` 的 `key.set(...)` 在 setter 上没有 `@Setter(AccessLevel.NONE)`，但 `iv`/`padding` 可变。`algorithm` 与 `mode` 不可变（写死 `AES` / `CBC`）。
- `HttpUtil` 使用 JDK `java.net.http.HttpClient`，不支持 HTTP/2 流式，仅适合简单 REST 调用。需要重试 / 复杂场景请使用外部 HTTP 库。
- `RandomUtil` 使用 `ThreadLocalRandom`（线程安全），**不要在测试中假设全局序列**。
- `Json.parse*` 失败时统一抛 `ServiceException`（不是 Jackson 原生异常），以便与上层拦截器兼容。
- `TaskUtil` 的线程池是守护线程、默认最大核心 = `2 * Runtime.getRuntime().availableProcessors()`、`CallerRunsPolicy` 兜底；**关闭 JVM 前无需显式 shutdown**。
- `ReflectUtil.getFieldList` 在传入 `null` 时直接抛 `ServiceException`（异常提示"无法获取 null 的字段列表"）。其他 `getAnnotation*` 重载对 `null` 的处理不一，调用前自行 `Objects.nonNull(...)` 校验。

---

## 10. 推荐的修改流程

1. 通读 `pom.xml` 和 `RootModel`，理解整体设计再下手。
2. **不要修改** `Json` / `RootModel` / `IException` / `IDictionary` / `ITree` / `IEntity` 的公共方法签名 — 上层框架可能依赖它们。
3. 任何对内部缓存结构的修改都需要回归 `RootModelTest` 与 `CollectionUtilTest`。
4. 修改完成后：`mvn -q -DskipTests=false test`，确保 21 个测试类全部通过。
5. 如果改了公共方法，更新对应的 Javadoc，并保持方法级 Javadoc 的 `<h1>` 标题。

---

## 11. 参考命令

```bash
mvn -q clean compile             # 编译
mvn -q test                      # 全部单测
mvn -q test -Dtest=XxxTest       # 单测某个类
mvn -q package -DskipTests       # 打 jar（不含单测）
mvn -q javadoc:javadoc           # 生成 Javadoc
```

Javadoc 已随 `maven-javadoc-plugin` 在打包阶段同时发布，无需手动触发。

---

## 12. 文件改动参考索引

| 任务           | 涉及位置                                                                 |
| ------------ | -------------------------------------------------------------------- |
| 注解 / 校验       | `annotation/`、`ValidateUtil`、`RootModel`                           |
| 字段过滤 / 脱敏    | `RootModel`、`DesensitizeUtil`、`ReflectUtil`                       |
| HTTP         | `HttpUtil`、`constant/HttpConstant`、`enums/HttpMethod`              |
| 加解密          | `AesUtil`、`RsaUtil`、`AccessTokenUtil`                              |
| 时间           | `DateTimeUtil`、`enums/DateTimeFormatter`                            |
| 树             | `TreeUtil`、`interfaces/ITree`                                       |
| 字典 / 枚举      | `DictionaryUtil`、`interfaces/IDictionary`、`annotation/Dictionary` |
| 异常           | `exception/ServiceException`、`interfaces/IException`               |
| JSON         | `Json`、`constant/HttpConstant.Header.TRACE_ID`                      |
| 文件 / 日志      | `FileUtil`、`resources/logback.xml`                                  |
| 主机名 / Trace  | `HostUtil`、`TraceUtil`                                              |

---

> **TL;DR for the agent**：这是一个"无框架侵入"的 Java 17 工具库。改任何东西之前，先确认不会破坏 `RootModel`、`Json`、`IException` 这三个被广泛继承/使用的核心抽象。
