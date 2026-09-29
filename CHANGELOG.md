# CHANGELOG

本文件记录 AirPower-Core 的缺陷修复明细，编号与 [ISSUE.md](./ISSUE.md) 一一对应。

---

## [7.0.1] — 全局缺陷修复

- **基线**：`7.0.0`（`dev` @ `23d52c8`）
- **分支**：`fix/global-defect-scan`
- **规模**：修复 **47 个缺陷**（P0 × 3、P1 × 22、P2 × 22），测试用例 1132 个
- **验证**：`mvn clean test` 与 `mvn test`（IDEA 插桩产物）均全绿；
  8 种 Locale（zh_CN / de_DE / tr_TR / ar_EG / ja_JP / th_TH / lt_LT / he_IL）× 对应时区零失败；
  `clean test` 连跑 3 遍零抖动；`HttpUtilTest` 压测 20 遍、并发用例压测 15 遍零抖动；
  `mvn package`（含 javadoc）通过

### 修复清单

| 编号 | 文件 | 修复内容 | 状态 |
|------|------|----------|------|
| **P0** | | | |
| P0-1 | `RootModel` | 嵌套模型与模型集合脱敏失效（白名单用 `this.getClass()` 导致 `isDesensitize` 分支不可达） | 已修复 |
| P0-2 | `ReflectUtil` | `finally` 里 `setAccessible(false)` 导致多线程下随机 `IllegalAccessException` | 已修复 |
| P0-3 | `ReflectUtil` | `setFieldValue` 静默吞异常，字段过滤/脱敏失效无感知 | 已修复 |
| **P1** | | | |
| P1-1 | `NumberUtil` | `long` 重载静默溢出（`multiply(MAX,4)` → `-4`） | 已修复 |
| P1-2 | `NumberUtil` | `floor`/`ceil` 用 `DOWN`/`UP`，负数结果全错 | 已修复 |
| P1-3 | `ReflectUtil` | `getFieldList(接口/基本类型)` 抛 NPE | 已修复 |
| P1-4 | `ReflectUtil` | 字段缓存以类名为 key，同名类跨 ClassLoader 串号 | 已修复 |
| P1-5 | `TreeUtil` | 根可达的环形数据导致 `StackOverflowError` | 已修复 |
| P1-6 | `TreeUtil` | 孤儿节点被静默丢弃，调用方无感知 | 已修复（补告警） |
| P1-7 | `TreeUtil` | ID 为空的节点其子树被整体丢弃 | 部分修复（补告警说明中断原因） |
| P1-8 | `DictionaryUtil` | 传入非枚举类抛 NPE | 已修复 |
| P1-9 | `ValidateUtil` | 多违规时错误消息不确定（`Set` 迭代顺序） | 已修复 |
| P1-10 | `ValidateUtil` | `ValidatorFactory` 永不关闭 | 已修复（新增 `close()`） |
| P1-11 | `RsaUtil` | PEM 文本无法直接回填使用（Base64 解码失败） | 已修复 |
| P1-12 | `RsaUtil` | `KeyFactory` 缓存不随 `cryptAlgorithm` 失效 | 已修复 |
| P1-13 | `RsaUtil` | 加解密用平台默认字符集，与 `sign`/`verify` 不一致 | 已修复 |
| P1-14 | `HttpUtil` | 无请求级超时，服务端不响应会永久阻塞 | 已修复 |
| P1-15 | `HttpUtil` | `create(0/-1)` 泄漏 JDK 的 `IllegalArgumentException` | 已修复 |
| P1-16 | `HttpUtil` | `HttpMethod.PATCH` 枚举存在但未实现 | 已修复 |
| **P2** | | | |
| P2-1 | `HttpUtil` | `body` 为 null 时 NPE 被包装成费解信息 | 已修复 |
| P2-2 | `HttpUtil` | `get()` 无条件改写 `method`，链式调用被静默篡改 | 已修复 |
| P2-3 | `HttpUtil` | `connectFailed` 空实现，代理失败无日志 | 已修复 |
| P2-4 | `IException` | 忽略大小写比较用无 Locale 的 `toLowerCase`（土耳其 I 问题） | 已修复 |
| P2-5 | `PatternConstant` | `NORMAL_CODE` 缺 `+` 量词，只匹配单字符 | 已修复 |
| P2-6 | `PatternConstant` | `CHINESE` 用 `*` 量词，空串被判为中文 | 已修复 |
| P2-7 | `RsaUtil` | 异常二次包装，消息重复（"RSA 私钥加密失败，RSA 私钥未设置"） | 已修复 |
| P2-8 | `AesUtil` | 密钥 / IV 长度不在设置时校验，错误延后且晦涩 | 已修复 |
| P2-9 | `AesUtil` | `setKey("")` 空串通过校验后在加密时才失败 | 已修复 |
| P2-10 | `AccessTokenUtil` | `hmacSha256` 逐字节 `String.format`，热路径开销大 | 已修复 |
| P2-11 | `DesensitizeUtil` | `replace` 的 `text`/`symbol` 为 null 时 NPE 或静默删原文 | 已修复 |
| P2-12 | `CollectionUtil` | `getCollectWithoutNull` 名不副实，不去 null | 已修复 |
| P2-13 | `CollectionUtil` | CSV 未做公式注入转义（`=`/`+`/`-`/`@` 开头） | 已修复 |
| P2-14 | `CollectionUtil` | 导出字段缓存冗余全限定名 | 已修复 |
| P2-15 | `FileUtil` | `getExtension("noext")` 返回整个文件名 | 已修复 |
| P2-16 | `FileUtil` | `getExtension` 的 `toLowerCase` 未指定 Locale | 已修复 |
| P2-17 | `FileUtil` | `formatSize(0)` 拒绝 0 字节的合法空文件 | 已修复 |
| P2-18 | `FileUtil` | `deleteDirectory` 忽略删除返回值，失败无感知 | 已修复 |
| P2-19 | `FileUtil` | ZIP 条目名用 `File.separator`，Windows 下非法 | 已修复 |
| P2-20 | `StringUtil` | 三处 Javadoc 写成"去除空格"，实为"判断空白" | 已修复 |
| P2-21 | `DateTimeUtil` | `SECOND_PER_HOUR` 靠巧合正确（`MINUTE * MINUTE`） | 已修复 |
| P2-22 | `DateTimeUtil` | `getCurrentDay` 与其余 `getCurrentXxx` 时间源不一致 | 已修复 |
| P2-23 | `TaskUtil` | 异步任务失败只打 `getMessage()`，丢堆栈 | 已修复 |
| P2-24 | `RandomUtil` | `randomString` 负长度静默变成 1 | 已修复 |
| P2-25 | `Json` | `objectMapper` 非 volatile 却用双重检查锁 | 已修复 |
| P2-26 | `Json` | 注释与 `NON_EMPTY` 实际行为不符 | 已修复 |
| P2-27 | `HttpConstant` | 常量内部类未声明 `final` | 已修复 |
| P2-28 | `RsaUtil` | 三个纯函数写成实例方法 | 已修复 |
| P2-29 | `NumberUtil` | `round` 把负 `scale` 静默改成 0 | 已修复 |
| P2-30 | `NumberUtil` | `calculate` 中恒为真的 null 判断 | 已修复 |
| P2-31 | `TaskUtil` | `CallerRunsPolicy` 与"异步"文档矛盾 | 已修复（补说明） |
| P2-32 | `ServiceException` | `data` 未标 `transient`，缺 `serialVersionUID` | 已修复 |

### 测试补强（第二轮）

对照 ISSUE.md 逐条复查后发现，上一轮有 **5 个修复点缺少回归用例**——包括最隐蔽的两个。
已补齐并逐个验证「注入缺陷 → 用例变红 → 恢复 → 用例变绿」：

| 测试类 | 新增用例 | 覆盖的缺陷 |
|--------|----------|------------|
| `ReflectUtilTest.ConcurrencyTest` | 4 | **P0-2**：8 线程 × 2000 次并发 `getFieldValue`、读写混用、`accessible` 标志不被重置、并发构建缓存结果一致 |
| `ReflectUtilTest.TurkishLocaleTest` | 3 | **`getFieldGetter` 的土耳其语问题**：tr_TR 下仍生成 `getId`、按 Getter 查 `@Export` 仍有效 |
| `ReflectUtilTest.cacheIsKeyedByClassNotName` | 1 | **P1-4**：用自定义 `ClassLoader` 重复加载同名类，验证缓存以 `Class` 为键 |
| `ValidateUtilTest.MultiViolationStabilityTest` | 3 | **P1-9**：同一模型连续 300 次校验，消息唯一且等于属性路径字典序最小的那条 |
| `ValidateUtilTest.LifecycleTest` | 3 | **P1-10**：`close()` 后自动重建、重复关闭安全、关闭前后报错一致 |
| `HttpUtilTest.RequestTimeoutTest` | 2 | **P1-14**：用只接受连接不响应的服务器验证请求级超时真生效，含正常响应对照组 |
| `fixture/SameNameProbe` | 夹具 | 供同名类隔离加载测试使用 |

用例数从 1098 增至 **1115**。

### 顺带修掉的测试脆弱点

三个既有用例（`isTheRootClass` / `getDeclaredFields` / `getLambdaFunctionName` 的 null 入参）
断言了精确异常类型，但源码的 `@NotNull` 参数在两种编译方式下行为不同：

- Maven `javac` 编译 → 不做运行时检查，null 在方法体内解引用抛 NPE
- IDEA 开启 *Instrument code with @NotNull assertions* 重新编译 → 参数校验提前拦截，抛 `IllegalArgumentException`

因此这些用例在 `mvn clean test` 下全绿，但在 IDEA 编译产物下（典型触发方式：加
`-DargLine` 指定 Locale / 时区重跑）必然 3 个失败。根因是断言绑定了异常类型而非真正的
契约——"未做判空"本身才是要锁住的行为。改为断言 `RuntimeException` 并注释说明差异，
不影响缺陷检测能力：一旦有人补上判空，断言依然会失败。

### 第二轮复查：新发现 6 个缺陷并修复

以"注入缺陷 → 用例变红 → 恢复 → 用例变绿"的方式复查上一轮 41 条修复时，
发现 **6 条上一轮遗漏**的缺陷（详见 [ISSUE.md](./ISSUE.md) 第二节之二·补）：

| 编号 | 文件 | 问题 | 修复 |
|------|------|------|------|
| P1-17 | `NumberUtil` | `divide` 的负 `scale` 静默返回 `0.0`（`round` 已拒绝，两族不一致） | 补负 `scale` 与 `roundingMode` 判空 |
| P1-18 | `FileUtil` | `saveFile` 泄漏 JDK 的 `IllegalArgumentException`（同方法的其它失败却包成了 `ServiceException`） | 捕获后统一包装 |
| P1-19 | `FileUtil` | `zip` 只校验 `exists` 不校验 `isDirectory`，泄漏 `NotDirectoryException`；压缩包写入源目录内部会破坏目录树 | 补两项入口校验 |
| P1-20 | `AccessTokenUtil` | `getPayloadId` 泄漏 `NumberFormatException`，上层按 401 拦截会漏掉畸形令牌 | 按无效令牌处理 |
| P1-21 | `AccessTokenUtil` | `setExpireSecond` 乘法溢出错报"毫秒数"，把排查方向带偏 | 改用 `Math.multiplyExact` |
| P1-22 | `RootModel` | `excludeReadOnly` **不递归**嵌套模型，而 `excludeNotMeta` / `desensitize` 都递归——嵌套模型里的创建时间会被前端拿到并用于覆盖服务端数据 | 补递归 + 自引用保护 |
| P2-26 | `Json` | 上一轮**只改了注释没改行为**：`configOverride(Map.class).setInclude(NON_EMPTY)` 完全无效 | 改用 `setSerializationInclusion(NON_NULL)`，并把错误的注释改回真实语义 |

用例数从 1115 增至 **1132**，其中新增 17 个用例全部经过"注入缺陷 → 变红 → 恢复 → 变绿"验证。

> **P2-26 的教训**：上一轮把注释从"忽略值为 null 的属性"改成"Map 中值为 null / 空串 /
> 空集合的键不参与序列化"，看起来是修正了描述，实际上**把一句错注释改成了另一句错注释**——
> 因为那行配置从来没生效过。实测 `{nullV:null, emptyStr:"", emptyList:[], emptyMap:{}}`
> 五个键一个都没被过滤。**改注释前必须先确认代码的实际行为。**

### 验证

- `mvn clean test` 与 `mvn test`（IDEA 插桩产物）均 1132 全绿
- 8 种 Locale（zh_CN / de_DE / tr_TR / ar_EG / ja_JP / th_TH / lt_LT / he_IL）× 对应时区全绿
- `clean test` 连跑 3 遍零抖动；`HttpUtilTest` 压测 20 遍、并发用例压测 15 遍零抖动
- `mvn package`（含 javadoc）通过

---

### 扫描中新发现并修复的缺陷

以下问题在初版 ISSUE.md 中未列出，是多 Locale 交叉验证阶段发现并修复的：

| 位置 | 问题 | 触发条件 |
|------|------|----------|
| `ReflectUtil.getFieldGetter` | 用无 Locale 的 `toUpperCase()`，土耳其语环境下 `getId` 被拼成 `getİd`，导不到方法导致 `@Export`/`@Meta` 注解查找**全部失效** | tr_TR 环境下导出的 9 列掉到 8 列 |
| `RandomUtil` | `BASE_CHAR.toUpperCase()` 同样问题，随机串混入非 ASCII 字符 | tr_TR 环境 |
| `HostUtil` | `System.getProperty` 可能抛 `SecurityException` 未捕获 | 安全策略限制环境 |
| `TreeUtil.findByParentId` | 直接返回数据源引用，调用方一改就污染数据源 | 任意环境 |

---

## 各批次提交记录

| 提交 | 范围 |
|------|------|
| `671f16e` | 反射层并发与静默失败（P0-1 ~ P0-3、P1-3、P1-4） |
| `e354c83` | 数值溢出与树结构遍历正确性（P1-1、P1-2、P1-5 ~ P1-7） |
| `3f72c3e` | 工具类参数校验与异常语义（P1-8 ~ P1-10、P2-7 ~ P2-9、P2-11、P2-24） |
| `2c9c14c` | RSA 密钥链路与 HTTP 可用性（P1-11 ~ P1-16、P2-1 ~ P2-3、P2-7） |
| `75b841d` | 国际化比较与正则量词（P2-4 ~ P2-6、P2-10、P2-25、P2-26） |
| `e92d047` | CSV 导出、文件工具、Locale 依赖（P2-12 ~ P2-23、P2-27 ~ P2-32） |
| `6e3fb48` | HostUtil 异常降级、`findByParentId` 防御性拷贝 |
| `620107b` | 为 P0-2 / P1-4 / P1-9 / P1-10 / P1-14 与国际化缺陷补回归用例 |
| `652ada1` | 让 null 入参用例不再依赖编译期插桩 |
| `（本轮）` | 第二轮复查新发现并修复 6 个缺陷（P1-17 ~ P1-22、P2-26 修正） |

---

## 行为变更提示（对上层 `airpower-web` 的影响）

以下改动会改变可观察行为，升级前需确认调用方：

### 数据正确性

1. **`desensitize()` 现在会脱敏嵌套模型**。此前只有最外层模型自身的 `@Desensitize` 字段被处理，
   嵌套结构（订单 → 收货人、用户 → 实名信息）中的手机号 / 身份证 **以明文返回**。
   若上层有"自行对嵌套对象脱敏"的逻辑，现在会重复处理（脱敏是幂等的，结果不变）。

2. **`ReflectUtil.setFieldValue` 现在会抛异常**。此前写入失败只记日志。
   若上层存在依赖"静默失败"的路径（如给 `final` 字段赋值），现在会收到 `ServiceException`。

3. **`NumberUtil` 的 `long` 重载在溢出时抛 `ServiceException`**。此前静默回绕
   （`multiply(MAX,4)` → `-4`）。金额计算若曾依赖回绕行为，需重新评估。

4. **`NumberUtil.floor`/`ceil` 对负数结果改变**。`floor(-1.5,0)` 由 `-1` 变为 `-2`，
   `ceil(-1.5,0)` 由 `-2` 变为 `-1`。正数行为不变。

5. **`round` 的负 `scale` 现在抛异常**。此前静默按 0 处理。

6. **`RandomUtil.randomString` 的非正长度现在抛异常**。此前静默返回 1 个字符。

### 数据丢失风险

7. **`TreeUtil.buildTreeList` 遇到环形数据不再栈溢出**，改为剪断该分支并记 warn。
   若业务数据确实存在环，此前是进程崩溃，现在会返回部分树。

8. **`TreeUtil.getChildrenIdList` 遇到 ID 为空的节点**会记 warn 并中断该分支遍历
   （API 形态决定：以 `parentId` 为键，无 ID 就无法查子节点）。若上层存在 ID 为空的
   脏数据，此前是静默丢弃整棵子树，现在至少能从日志发现。

9. **`TreeUtil.findByParentId` 返回副本**，调用方修改返回值不再影响数据源。

### 正则与校验

10. **`isNormalCode` 现在能匹配多字符**。此前 `isNormalCode("abc")` 恒为 `false`。

11. **`isChinese("")` 由 `true` 变为 `false`**。

12. **`ValidateUtil.valid` 多违规时抛出的消息变得稳定**（按属性路径排序取第一条）。
    若上层已针对具体消息做分支处理，需要重新核对。

13. **`ValidateUtil.close()` 为新增方法**，需在应用关闭钩子中调用以释放
    `ValidatorFactory`。不调用不影响功能（下次 `valid` 会自动重建）。

### 加密

14. **`RsaUtil` 的 PEM 文本现在可直接使用**。此前必须先手动剥离
    `-----BEGIN/END-----` 头尾与换行，否则报 `Illegal base64 character`。
    调用方若已自行剥离，行为不变。

15. **`RsaUtil` 加解密固定使用 UTF-8**。此前用平台默认字符集，
    在非 UTF-8 平台上加密中文再跨平台解密会乱码——现在两端的处理都是 UTF-8。

16. **`AesUtil.setKey` / `setIv` 在设置时就校验长度**。此前错误延后到加解密时，
    以 `初始化密码器失败，Invalid AES key length` 这类晦涩信息暴露。

17. **`RsaUtil` 切换 `cryptAlgorithm` 现在会真正生效**。此前 `KeyFactory` 缓存不失效，
    配置变更形同虚设。

### HTTP

18. **`HttpUtil.get()` 在方法非 GET 时抛异常**，不再静默改回 GET。
    若上层有 `setMethod(PUT).get()` 这类链式写法，必须改为显式 `setMethod(GET)`。

19. **`HttpUtil` 新增请求级超时**，与 `connectTimeout` 同值。服务端不响应时
    会按超时抛出 `ServiceException`，此前会永久阻塞。

20. **`HttpMethod.PATCH` 现在可用**，此前抛"不支持的请求方法"。

21. **`HttpUtil` 新增 `put()` / `patch()` / `delete()` 便捷方法**。

### CSV 导出

22. **`CollectionUtil.getCollectWithoutNull` 现在真正过滤 null 元素**，
    并返回与 `fieldClass` 匹配的新集合（此前直接返回入参引用）。

23. **CSV 单元格新增公式注入防护**：以 `= + - @` 开头的值会加单引号前缀。
    若下游系统依赖这些值以原样解析，需相应调整。

24. **CSV 导出遇到 null 元素跳过该行**，此前整份导出抛 NPE。

### 国际化

25. **以下操作固定使用 `Locale.ROOT`**：忽略大小写比较（`IException`）、
    `ReflectUtil.getFieldGetter`、`RandomUtil` 字符集、`FileUtil.getExtension`。
    这修复了 tr_TR 环境下 `getId` 拼成 `getİd` 导致注解查找全部失效的严重问题。

### 序列化（第二轮新增，影响面较大）

26. **⚠️ `Json` 改为全局忽略 null 字段**。此前那句
    `configOverride(Map.class).setInclude(NON_EMPTY)` 从未生效，**响应体里一直是带
    `data:null` 的**；现在真的会过滤掉。影响：
    - `Json.create()` 序列化结果从 `{"code":200,"message":"","data":null}` 变为 `{"code":200,"message":""}`
    - POJO 中值为 null 的字段不再出现在 JSON 文本中
    - `Map` 中值为 null 的键不再输出
    - **空串、空集合、空 Map 仍然保留**（用的是 `NON_NULL` 不是 `NON_EMPTY`，
      因为 API 响应里的 `data:[]` 需要保留）
    若上层有"必须看到某个 null 字段"的逻辑，需改用显式默认值或检查 `containsKey`。

27. **`RootModel.excludeReadOnly()` 现在递归嵌套模型**。此前只清空自身字段，
    嵌套模型（订单 → 明细）里的 `@ReadOnly` 字段（创建时间等）会返回给前端，
    客户端可据此覆盖服务端数据。升级后这些字段会被一并清空，
    若上层有依赖嵌套只读字段回显的场景需确认。

### 其它（第二轮新增）

28. **`NumberUtil.divide` 的负 `scale` 现在抛异常**，此前静默返回 `0.0`
    这类完全错误的结果。

29. **`FileUtil.saveFile` 传入非法 `OpenOption` 时改抛 `ServiceException`**，
    此前是 JDK 的 `IllegalArgumentException`。

30. **`FileUtil.zip` 新增两项入口校验**：源路径必须是文件夹；
    压缩文件不能输出到源文件夹内部（否则会破坏正在遍历的目录树）。
    此前这两种情况分别漏出 `NotDirectoryException` 和静默损坏数据。

31. **`AccessTokenUtil.getPayloadId` 遇非数字负载改抛 401 `ServiceException`**，
    此前是 `NumberFormatException`，上层按未授权统一拦截时会被漏掉。

32. **`AccessTokenUtil.setExpireSecond` 溢出的报错改为"过期秒数过大"**，
    此前报"过期毫秒数必须大于0"，会让排查方向偏向毫秒。

---

## 不修复项

以下问题技术上确实存在，但改动会破坏对上层 `airpower-web` 的兼容或改变既定契约，
仅在 [ISSUE.md](./ISSUE.md) 第四节记录，未做改动：

- `TreeUtil` 返回的 `children` 为不可变列表
- `getChildrenIdList` 不包含父 ID 自身
- `CollectionUtil.getCollectWithoutNull` 的方法名（语义已修正，名称保留）
- `HttpUtil` 的代理失败回退直连策略
- `RootModel` 的 `@EqualsAndHashCode` 包含敏感字段
- `ValidateUtil.isChina2Identity` 对 15 位身份证抛异常（既有的既定行为）
