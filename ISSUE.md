# AirPower-Core 全局缺陷扫描报告

- **分支**：`fix/global-defect-scan`
- **基线**：`dev` @ `23d52c8`（版本 7.0.0）
- **扫描范围**：`src/main/java` 全部 42 个文件 / 约 5800 行
- **扫描方式**：逐文件人工审读 + 40 组可执行探针（`ScratchProbe*Test`，验证后已删除）+ 全量单测基线
- **结论**：共发现 **47 个缺陷**，其中 P0 × 3、P1 × 22、P2 × 22
  （首轮 41 个 + 复查阶段新发现 6 个，见第二节之二·补）

> 本文档只记录"确认存在的问题"。每条都标注了复现方式与影响面，修复记录见 [CHANGELOG.md](./CHANGELOG.md)。

---

## 一、P0 — 严重（安全 / 数据错误 / 并发崩溃）

### P0-1 `RootModel.desensitize()` 对嵌套模型与模型集合完全失效

**位置**：`RootModel.java:90-124`

`excludeNotMetaAndDesensitize` 的白名单判定用的是 **`this.getClass()`**（第 97 行），
而递归时 `this` 已经是子对象。`desensitize()` 传入的白名单只含**自身类**，
于是递归到类型不同的嵌套模型 / 集合元素时，条件
`!whiteNameList.contains(this.getClass())` 成立，代码直接走
`excludeFieldValueNotMeta(...)` 并 `return`——**`isDesensitize` 分支永远不会到达**。

结果：只有最外层模型自身的 `@Desensitize` 字段被脱敏，嵌套对象里的手机号、身份证、
银行卡**以明文返回给前端**。这是脱敏形同虚设。

**复现**（探针 PROBE-1）：`Holder`（白名单类）持有 `OtherModel`（不同类），
`holder.desensitize()` 后 `OtherModel.mobile` 仍为 `13812348000`（期望 `138****8000`）。
现有单测之所以通过，是因为夹具里嵌套模型与外层**都是 `DemoModel`**，恰好落在白名单内。

**影响**：任何"订单 → 收货人"、"用户 → 实名信息"这类嵌套结构都会泄露敏感数据。

---

### P0-2 `ReflectUtil` 在 `finally` 里 `setAccessible(false)`，并发下随机崩溃

**位置**：`ReflectUtil.java:65-92`

```java
try {
    field.setAccessible(true);
    return field.get(object);
} catch (IllegalAccessException e) { ... }
finally {
    field.setAccessible(false);   // ← 问题所在
}
```

`Field` 的 `override` 标志是**全局共享**的，不是线程内状态。线程 A 执行
`setAccessible(true)` 之后、调用 `get` 之前，线程 B 的 `finally` 把标志清了，
A 的 `field.get` 就抛 `IllegalAccessException`，被包装成 `ServiceException` 抛出。

**复现**（探针 PROBE-M）：8 线程 × 2000 次 `getFieldValue`，稳定出现
`获取对象指定属性的值失败, class ... cannot access a member of class ... with modifiers "private"`。

这同时让 `getCacheFieldList()`（第 254 行）里的 `field.setAccessible(true)` **完全白做**——
每次读写都要重新走一遍 JDK 的 `AccessibleObject` 安全检查（涉及 `Reflection` 的
模块/包访问校验），高并发下是实打实的热点。

**影响**：`RootModel` 每次接口返回都要遍历全部字段，上层 QPS 一上来必然抖动；
且异常是随机出现的，极难排查。

---

### P0-3 `ReflectUtil.setFieldValue` 静默吞掉 `IllegalAccessException`

**位置**：`ReflectUtil.java:83-92`

`getFieldValue` 抛异常，`setFieldValue` 却只 `log.error` 就返回。写失败会被
上层当作"设置成功"继续走，最终**响应体里带着本该被清空的敏感字段**返回给前端。

对 `final` 字段、只读字段、模块化 JDK 内部类都会触发。

**复现**（探针 PROBE-L）：对 `private final String fixed` 赋值，无任何异常，`fixed` 保持原值。

**影响**：`RootModel.excludeNotMeta` / `desensitize` 依赖 `setFieldValue` 置空字段，
一旦失败就是**脱敏/字段过滤静默失效**——与 P0-1 叠加后果更严重。

---

## 二、P1 — 重要（功能性错误 / 崩溃 / 安全）

### P1-1 `NumberUtil` 的 `long` 重载静默溢出

**位置**：`NumberUtil.java:72-77, 102-107, 132-137`

内部用 `BigInteger` 精确计算，最后 `.longValue()` **直接截断**，不检查是否越界。

**复现**（探针 PROBE-5）：`multiply(Long.MAX_VALUE, 4)` = `-4`；`add(Long.MAX_VALUE, 1)` = `-9223372036854775808`。
金额计算场景下会直接算出负数。

### P1-2 `NumberUtil.floor/ceil` 语义与命名不符

**位置**：`NumberUtil.java:258-271`

`floor` 用 `RoundingMode.DOWN`（**向零截断**），`ceil` 用 `RoundingMode.UP`（**远离零**）。
对负数全部算错：`floor(-1.5, 0)` = `-1`（正确应为 `-2`），`ceil(-1.5, 0)` = `-2`（正确应为 `-1`）。

### P1-3 `ReflectUtil.getFieldList(接口/数组)` 抛 NPE

**位置**：`ReflectUtil.java:241-260`

`getCacheFieldList` 的循环条件是 `while (!isTheRootClass(currentClass))`，
而 `isTheRootClass` 内部直接 `clazz.equals(...)`。传接口时 `getSuperclass()` 返回 `null`，
下一轮 `isTheRootClass(null)` → **NPE**。

**复现**（探针 PROBE-2）：`ReflectUtil.getFieldList(Runnable.class)` → NPE。

### P1-4 `ReflectUtil.DECLARED_FIELD_LIST_MAP` 用类名做 key，可跨类加载器串号

**位置**：`ReflectUtil.java:45, 268-271`

缓存 key 是 `clazz.getName()`（`String`）而不是 `Class<?>`。
同名的类被两个 `ClassLoader` 各自加载时，**后加载者会命中前者的 `Field[]`**，
反射读到的字段属于错误的类，可能抛 `IllegalArgumentException` 或读到错值。
`FIELD_LIST_MAP` 用 `Class` 做 key 反而是正确的，两处策略不一致。

### P1-5 `TreeUtil.buildTreeList` 遇到环状数据栈溢出

**位置**：`TreeUtil.java:66-73`

`buildTreeWithMap` 无访问标记。父 ID 构成环且从 `ROOT_ID` 可达时无限递归。

**复现**（探针 PROBE-3b）：`0 → 11 → 10 → 11` 的数据直接 **StackOverflowError**。
注意 `getChildrenIdList` 上一轮已经加了防环，`buildTreeList` 却没加，两处不一致。

### P1-6 `TreeUtil.buildTreeList` 静默丢弃孤儿节点

**位置**：`TreeUtil.java:49-56`

父 ID 指向不存在的节点时，这些数据永远不会出现在结果里，调用方**无任何提示**，
表现为"数据莫名少了一截"。**复现**（探针 PROBE-D）：输入 2 条，返回 1 条。

### P1-7 `TreeUtil.getChildrenIdList` 中 ID 为 null 的节点，其整棵子树被丢弃

**位置**：`TreeUtil.java:147-153`

`children.stream().map(IEntity::getId).filter(Objects::nonNull)` 把 ID 为 null 的
子节点**连同其递归入口一起过滤掉**了——后面的 `forEach` 永远不会对它调用递归，
挂在它下面的所有后代都收不到。

**复现**（探针 PROBE-E）：`1 → (id=null) → 2 → 3`，期望至少收集到 `{3}`，实际返回 `{}`。

### P1-8 `DictionaryUtil` 传入非枚举类抛 NPE

**位置**：`DictionaryUtil.java:51, 86`

`Class#getEnumConstants()` 对非枚举返回 `null`，`.stream()` 直接 NPE。
对外工具类应给出可读的业务异常。

**复现**（探针 PROBE-11）：普通类实现 `IDictionary` 后调用 → NPE。

### P1-9 `ValidateUtil.valid` 多违规时抛出的错误信息不确定

**位置**：`ValidateUtil.java:264, 270`

只取 `violations.iterator().next()`，而 `Set` 的迭代顺序不保证稳定，
导致同一个对象多次校验可能报出不同的字段错误，**上层无法据此做字段级回显**。

**复现**（探针 PROBE-A）：同一个 `ViolationModel` 连续校验 200 次，
出现过 `手机号不能为空` 和 `名称至少5个字符` 两种消息。

### P1-10 `ValidateUtil` 的 `ValidatorFactory` 永不关闭

**位置**：`ValidateUtil.java:36-49`

静态持有 `ValidatorFactory` 但没有提供任何关闭入口。工厂内部会持有
`ConstraintValidator` 实例、缓存的元数据与（部分 Provider 的）后台资源，
在热部署 / 容器反复重载场景下无法回收。

### P1-11 `RsaUtil` 无法直接使用自身产出的 PEM 字符串

**位置**：`RsaUtil.java:85-92, 192-200`

`getPublicKey` / `getPrivateKey` 直接 `Base64.getDecoder().decode(入参)`，
不剥离 `-----BEGIN ...-----` 头尾与换行。
而 `getPemPublicKey()` / `getPemPrivateKey()` 的输出恰恰带这些内容，
**"生成密钥对 → 拿到 PEM → 设置回去"这条最自然的链路是断的**。

**复现**（探针 PROBE-H）：`setPublicKey(getPemPublicKey(kp))` 后加密 →
`Illegal base64 character 2d`（`-`）。

### P1-12 `RsaUtil` 的 `KeyFactory` 缓存不随 `cryptAlgorithm` 失效

**位置**：`RsaUtil.java:56-59, 100-106`

`cachedKeyFactory` 一旦生成就固定。`setCryptAlgorithm("其他算法")` 之后
仍然复用旧工厂，行为与配置不符。**复现**（探针 PROBE-J）：改成 `NOPE` 后
仍然能正常加密，直到换了密钥才报 `Cannot find any provider supporting NOPE`。

### P1-13 `RsaUtil` 加解密使用平台默认字符集

**位置**：`RsaUtil.java:304, 318`

`sourceContent.getBytes()` 与 `new String(resultBytes)` 都没指定字符集。
`sign` / `verify` 用了 `StandardCharsets.UTF_8`（第 332、348 行），**同一工具类内部不一致**。
在非 UTF-8 平台上加密中文再跨平台解密会得到乱码。

### P1-14 `HttpUtil` 没有请求级超时，服务端不响应会永久阻塞

**位置**：`HttpUtil.java:103, 204-227`

只设置了 `HttpClient.connectTimeout`（建连超时），`HttpRequest` 上没有 `.timeout(...)`。
服务端接受连接后不返回数据，调用线程会**一直挂起**直到上层网关超时。

### P1-15 `HttpUtil.create` 的超时参数未校验

**位置**：`HttpUtil.java:103`

`Duration.ofSeconds(0/-1)` 抛 JDK 原生 `IllegalArgumentException`，泄漏到调用方。
**复现**（探针 PROBE-N）：`HttpUtil.create(0)` → `Invalid duration: PT0S`。

### P1-16 `HttpMethod.PATCH` 枚举存在但 `getHttpRequest` 未实现

**位置**：`HttpMethod.java:30-34` 与 `HttpUtil.java:209-215`

枚举暴露了 `PATCH`，`getHttpRequest` 的 `switch` 却没有对应分支，
运行到才抛 `不支持的请求方法`。**枚举契约与实现不一致**。

---

## 二·补、修复复查中新发现的缺陷（第二轮扫描）

> 上述 41 条已全部确认修复到位。第二轮以"注入缺陷 → 用例变红"的方式复查时，
> 又发现 6 条**上一轮遗漏**的缺陷，其中 P2-26 属于**改了注释但没改行为**。

### P2-26（修正） `Json` 的 `NON_EMPTY` 配置完全无效，注释改成什么样都是错的

**位置**：`Json.java:294-295`

上一轮只把注释从"忽略值为 null 的属性"改成"Map 中值为 null / 空串 / 空集合的键不参与
序列化"，**但配置本身从未生效**。`configOverride(Map.class)` 只影响 POJO 属性上
`@JsonInclude` 未指定时的默认行为，对**直接序列化的 `Map` 对象**不起作用。

**复现**（探针 PROBE-A）：`{nullV:null, emptyStr:"", emptyList:[], emptyMap:{}, normal:"v"}`
序列化结果是 `{"normal":"v","emptyList":[],"emptyStr":"","emptyMap":{},"nullV":null}`——
**5 个键一个都没被过滤**。POJO 场景同样如此（探针 PROBE-B：`nullField:null` 照常输出）。

上一轮的注释修正因此是**错的**，比原来的错注释更容易误导。本轮改为在 `ObjectMapper`
上直接设置序列化包含策略——这是真正生效且语义明确的写法。

### P1-17（新增） `NumberUtil.divide` 的负 `scale` 静默返回 `0.0`

**位置**：`NumberUtil.java:244-249`

`round` 已经会拒绝负 `scale`，但 `divide` 没有。`BigDecimal.divide(second, -2, ...)`
在 Java 中合法，返回值被截到百位以上，`10/3` 算出 **`0.0`**——调用方拿到一个
完全错误的结果且毫无提示。

**复现**（探针 PROBE-I）：`NumberUtil.divide(10, 3, -2)` = `0.0`（应为 3 或明确报错）。
两个 `divide` 家族对负 `scale` 的态度必须一致。

### P1-18（新增） `FileUtil.saveFile` 泄漏 JDK 的 `IllegalArgumentException`

**位置**：`FileUtil.java:139-151`

`Files.write` 传入非法的 `OpenOption` 组合（如只给 `READ`）时抛
`IllegalArgumentException`，未被捕获，直接暴露给调用方。
而同方法的目录创建失败却包成了 `ServiceException`——**同一方法内两种错误风格**。

**复现**（探针 PROBE-C）：`saveFile(dir, "a.txt", "x", StandardOpenOption.READ)`
→ `IllegalArgumentException: READ not allowed`。

### P1-19（新增） `FileUtil.zip` 源路径不是目录时泄漏 `NotDirectoryException`

**位置**：`FileUtil.java:172-181`

只校验了 `Files.exists`，没校验 `Files.isDirectory`。传一个普通文件作源时，
`Files.newDirectoryStream` 抛 `NotDirectoryException`（JDK 原生异常，信息里只有路径）。
另外 `zipFilePath` 与 `sourceDirPath` 相同时会用压缩包覆盖源目录。

### P1-20（新增） `AccessTokenUtil.getPayloadId` 泄漏 `NumberFormatException`

**位置**：`AccessTokenUtil.java:322-328`

负载里的 `id` 不是数字时直接抛 `NumberFormatException`，而不是本类一贯的
401 `ServiceException`。上层按"未授权"统一处理时会漏掉这种畸形令牌。

**复现**（探针 PROBE-E / PROBE-2）：负载 `id="not-a-number"` → `NumberFormatException`。

### P1-21（新增） `AccessTokenUtil.setExpireSecond` 溢出后报错信息误导

**位置**：`AccessTokenUtil.java:190-195`

`second * MILLISECONDS_PER_SECOND` 溢出成负数，再传给 `setExpireMillisecond`，
于是报"过期毫秒数必须大于0"——**调用方传的是秒，错误提示却说毫秒**，
排查时会被带偏。

**复现**（探针 PROBE-F）：`setExpireSecond(Long.MAX_VALUE)` → "过期毫秒数必须大于0"。

### P1-22（新增） `RootModel.excludeReadOnly` 不递归嵌套模型，与同族方法不一致

**位置**：`RootModel.java:47-51`

`excludeNotMeta` 与 `desensitize` 都会递归处理嵌套模型和模型集合，
`excludeReadOnly` **只处理自身字段**。

**复现**（探针 PROBE-9）：`parent.excludeReadOnly()` 后
`parent.createTime = null`，但 `parent.child.createTime = 123` 仍在。

后果：嵌套模型（订单 → 明细）里的 `@ReadOnly` 字段（如创建时间）**会被前端拿到**，
客户端可用它覆盖服务端数据。

---

## 三、P2 — 一般（健壮性 / 可维护性 / 规范）

| # | 位置 | 问题 |
|---|------|------|
| P2-1 | `HttpUtil.java:216-222` | `body` 为 `null` 时 `ofString(null)` 抛 NPE，被 `send()` 包装成信息费解的 `发起请求失败，Cannot invoke "String.getBytes..."`（探针 PROBE-N） |
| P2-2 | `HttpUtil.java:155-181` | `get()` / `post()` **无条件改写** `method`，链式 `setMethod(PUT).get()` 会被静默改回 GET（探针 PROBE-O 显示 `setMethod` 后 `getMethod` 仍是 PUT，但 `get()` 内部会覆盖） |
| P2-3 | `HttpUtil.java:98-100` | `ProxySelector.connectFailed` 空实现，代理不可用时不回退直连，请求直接失败 |
| P2-4 | `IException.java:185, 251` | `whenEqualsIgnoreCase` / `whenNotEqualsIgnoreCase` 用无 Locale 的 `toLowerCase()`，土耳其语环境下 `"I"` → `"ı"`，`"I"` 与 `"i` 被判为**不等** |
| P2-5 | `PatternConstant.java:62` | `NORMAL_CODE` 缺 `+` 量词，只匹配**单个字符**。`isNormalCode("abc")` = `false`（探针 PROBE-7），与"允许字符"语义不符 |
| P2-6 | `PatternConstant.java:45` | `CHINESE` 用 `*`，`isChinese("")` = `true`（探针 PROBE-8） |
| P2-7 | `RsaUtil.java:212-214` 等 | `catch (Exception)` 把内部 `ServiceException` 也包一层，产生 `RSA 私钥加密失败，RSA 私钥未设置` 这种重复文案（探针 PROBE-I） |
| P2-8 | `AesUtil.java:97-100` | `setKey` 不校验密钥长度，`setIv` 不校验 IV 长度，错误延后到加密时才以 `初始化密码器失败，Empty key` / `Wrong IV length` 暴露（探针 PROBE-F/G） |
| P2-9 | `AesUtil.java:80-89` | `setKey("")` 空串通过 Base64 解码得到 0 长度数组，不报错 |
| P2-10 | `AccessTokenUtil.java:279-282` | `hmacSha256` 逐字节 `String.format("%02x", ...)`，`String.format` 极慢，令牌签发/校验是热路径 |
| P2-11 | `DesensitizeUtil.java:44-56` | `replace(text, ...)` 的 `text` 与 `symbol` 均为 `null` 时抛 NPE（探针 PROBE-V） |
| P2-12 | `CollectionUtil.java:65-70` | `getCollectWithoutNull` **名不副实**：既不去 null 也不去重，`null` 入参时返回空集（探针 PROBE-P 返回 `[a, b, null]`） |
| P2-13 | `CollectionUtil.java:110-123` | CSV 单元格未做**公式注入**转义，值以 `=`/`+`/`-`/`@` 开头时 Excel 会当公式执行（探针 PROBE-Q） |
| P2-14 | `CollectionUtil.java:48` | `new java.util.concurrent.ConcurrentHashMap<>()` 冗余全限定名，文件已 import |
| P2-15 | `FileUtil.java:65-67` | `getExtension("noext")` 返回整个文件名 `"noext"`（探针 PROBE-U），无扩展名时应返回 `""` |
| P2-16 | `FileUtil.java:66` | `toLowerCase()` 未指定 Locale，土耳其语环境下 `"TXT"` → `"tхt"`（点无点 I 问题） |
| P2-17 | `FileUtil.java:75-78` | `formatSize(0)` 对 0 字节文件抛异常（探针 PROBE-T），空文件是合法场景 |
| P2-18 | `FileUtil.java:231-246` | `deleteDirectory` 用 `File::delete` **忽略返回值**，删除失败静默无感知 |
| P2-19 | `FileUtil.java:193, 202` | ZIP 条目名用 `File.separator` 拼接，Windows 上产生 `\` 分隔的**非法条目名**（探针 PROBE-S） |
| P2-20 | `StringUtil.java:70, 90, 101` | 三处 Javadoc 写成"去除字符串中的空格"，实际是"判断是否包含空白"（探针 PROBE-R） |
| P2-21 | `DateTimeUtil.java:59` | `SECOND_PER_HOUR = SECOND_PER_MINUTE * SECOND_PER_MINUTE` —— 靠巧合正确（60×60），语义应为 `* 60` |
| P2-22 | `DateTimeUtil.java:320-322` | `getCurrentDay()` 用 `ZonedDateTime.now()`，其余 `getCurrentXxx()` 都走 `new Date()`，两套时间源不一致 |
| P2-23 | `TaskUtil.java:71` | 异步任务失败只打印 `e.getMessage()`，**丢弃堆栈**，线上问题无法定位 |
| P2-24 | `RandomUtil.java:119` | `randomString(-5)` 静默返回 1 个字符（`Math.max(length,1)`），调用方的错误参数被吞掉（探针 PROBE-W） |
| P2-25 | `Json.java:49, 286-303` | `objectMapper` **非 volatile** 却用双重检查锁 —— 经典的失效 DCL，JMM 下可能读到半初始化对象（探针 PROBE-B） |
| P2-26 | `Json.java:294-295` | `configOverride(Map.class).setInclude(NON_EMPTY)` **完全无效**，对直接序列化的 Map 与 POJO 都不起作用（探针 PROBE-A/B）。详见第二节之二·补 |
| P2-27 | `HttpConstant.java:22, 34, 51, 67, 83, 91` | 常量内部类未声明 `final`，可被继承实例化 |
| P2-28 | `RsaUtil.java:125, 150, 173` | `convertPublicKeyToPem` / `convertPrivateKeyToPem` / `wrapBase64Text` 是纯函数却写成实例方法 |
| P2-29 | `NumberUtil.java:282-284` | `round()` 把负 `scale` 静默改成 0（探针 PROBE-X：`round(2.5,-3,HALF_UP)` = `3`） |
| P2-30 | `NumberUtil.java:227` | `calculate()` 里的 `Objects.nonNull(values)` 是死判断——入参已被 `requireNonNullElse` 兜底，永不为 null |
| P2-31 | `TaskUtil.java:54` | `CallerRunsPolicy` + 方法注释"异步执行任务"，队列满时任务实际在**调用方线程同步执行**，与文档矛盾 |
| P2-32 | `ServiceException.java:25` | `data` 字段未标 `transient`，异常跨进程传输时若 `data` 不可序列化会二次失败 |

---

## 三·补、修复批次规划更新

原计划 8 个批次已完成。第二轮复查后新增 6 条，合并进已有批次：

| 批次 | 内容 | 对应编号 |
|------|------|----------|
| 第一批 | 反射层并发与静默失败 | P0-2、P0-3、P1-3、P1-4 |
| 第二批 | 脱敏递归（数据安全） | P0-1、**P1-22** |
| 第三批 | 数值与树结构正确性 | P1-1、P1-2、P1-5、P1-6、P1-7、**P1-17** |
| 第四批 | 工具类异常与参数校验 | P1-8、P1-9、P1-10、P2-7 ~ P2-9、P2-11、P2-24、**P1-18 ~ P1-21** |
| 第五批 | HTTP 与加密链路 | P1-11 ~ P1-16、P2-1 ~ P2-3 |
| 第六批 | 校验正则与国际化 | P2-4 ~ P2-6 |
| 第七批 | 序列化与并发 | P2-10、**P2-26**、P2-25 |
| 第八批 | 文件、集合、注释与规范 | P2-12 ~ P2-23、P2-27 ~ P2-32 |

---

## 四、不修复项（需产品决策 / 属设计取舍）

以下问题技术上确实存在，但改动会**破坏对上层 `airpower-web` 的兼容**或改变既定契约，
本次只记录不改动：

| 项 | 说明 |
|----|------|
| `TreeUtil` 返回的 `children` 为 `Collections.unmodifiableList` | 上层若在取到树后追加子节点会 `UnsupportedOperationException`（探针 PROBE-4）。改为可变列表是行为变更，需确认上层是否有依赖 |
| `getChildrenIdList` 不包含父 ID 自身 | 属契约设计，改动会影响删除逻辑 |
| `CollectionUtil.getCollectWithoutNull` 改名或改语义 | 属公开 API 重命名，需评估下游 |
| `HttpUtil` 增加 `put()/delete()/patch()` 便捷方法 | 属新增功能，非缺陷修复 |
| `RootModel` 的 `@EqualsAndHashCode` 包含敏感字段 | 可能引入哈希性能与安全权衡，需产品确认 |

---

## 五、修复批次规划

| 批次 | 内容 | 对应编号 |
|------|------|----------|
| 第一批 | 反射层并发与静默失败 | P0-2、P0-3、P1-3、P1-4 |
| 第二批 | 脱敏递归（数据安全） | P0-1 |
| 第三批 | 数值与树结构正确性 | P1-1、P1-2、P1-5、P1-6、P1-7 |
| 第四批 | 工具类异常与参数校验 | P1-8、P1-9、P1-10、P2-7 ~ P2-9、P2-11、P2-24 |
| 第五批 | HTTP 与加密链路 | P1-11 ~ P1-16、P2-1 ~ P2-3 |
| 第六批 | 校验正则与国际化 | P2-4 ~ P2-6 |
| 第七批 | 序列化与并发 | P2-10、P2-25、P2-26 |
| 第八批 | 文件、集合、注释与规范 | P2-12 ~ P2-23、P2-27 ~ P2-32 |
