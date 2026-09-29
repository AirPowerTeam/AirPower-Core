# AirPower-Core 项目记忆

## 2026-09-30 全局缺陷扫描与修复（分支 fix/global-defect-scan）

扫描全部 42 个主源码文件 + 30 组可执行探针，共发现并修复 **41 个缺陷**（P0×3 / P1×16 / P2×22），
另有 4 个是多 Locale 交叉验证阶段新发现的。交付物 `ISSUE.md`（扫描报告）与 `CHANGELOG.md`
（修复明细 + 25 条行为变更提示），7 次提交，尚未 push。

### 三个 P0

| 缺陷 | 根因 | 后果 |
|------|------|------|
| `RootModel.desensitize()` 嵌套模型完全失效 | 白名单判定用 `this.getClass()`，递归时 `this` 已是子对象，条件不成立直接 return，`isDesensitize` 分支不可达 | 嵌套结构中的手机号/身份证**以明文返回**；原单测通过是因为夹具里嵌套模型与外层同类，恰好落在白名单内 |
| `ReflectUtil` `finally` 里 `setAccessible(false)` | 该标志是 `Field` 的全局状态，线程 A 设 true 后被线程 B 清掉 | 多线程下随机 `IllegalAccessException`（8 线程 × 2000 次稳定复现）；同时让 `getCacheFieldList` 的 `setAccessible(true)` 完全白做 |
| `setFieldValue` 只 `log.error` 吞异常 | 写入失败无感知 | 字段过滤/脱敏静默失效，敏感字段照常返回 |

### 多 Locale 交叉验证揪出的隐蔽缺陷

`ReflectUtil.getFieldGetter` 用无 Locale 的 `toUpperCase()`，土耳其语环境下 `getId`
被拼成 `getİd`，导不到方法 → `@Export`/`@Meta` 注解查找**全部失效**，tr_TR 下
导出的 9 列掉到 8 列。`RandomUtil` 的 `BASE_CHAR.toUpperCase()` 同类问题。
**只跑默认 Locale 永远发现不了**，多 Locale × 多时区跑全量是性价比最高的检查手段。

### 扫描方法论（可复用）

1. **先写临时探针 @Test 打印实际行为**，再决定改不改。避免"看起来是 bug"改错方向。验证完删掉探针。
2. **测试固化缺陷行为时**，改动前先问"这个断言在保护什么"。本次 20+ 个用例固化的是缺陷
   行为（如 `floor(-1.5,0)` 断言为 `-1`），修复后要逐条改为断言正确行为，并在
   `@DisplayName` 写明"不再……"的原因。
3. 全量测试在 zh_CN / de_DE / tr_TR / ar_EG / ja_JP / th_TH / lt_LT 七种 Locale 下验证。

### 踩过的坑

- `RootModel` 跨实例调用 private 方法编译不过 → 改 `private static void handleNested(RootModel<?>, ...)`。
- `RsaUtil.wrapException` 用 `Supplier` 无法抛受检异常 → 必须 `Callable` + `.call()`。
- `AesUtil.iv` 有 `@Setter`，误删会导致测试编译失败。
- 同一测试类方法名不能重复，加用例前先 grep。
- AES Base64 密钥 `MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=` 解码后 23 字节（非法），
  测密钥长度要用 `new byte[16]`。

### 关键坑：`getFieldGetter` 的 Locale 依赖

任何"字段名 → Getter 名"的转换都必须固定 `Locale.ROOT`。同类风险点已在本次全部处理：
`IException` 忽略大小写比较、`RandomUtil` 字符集、`FileUtil.getExtension`。
**后续新增此类转换时务必带 Locale.ROOT。**

### 待用户确认（未 push）

25 条行为变更中风险最高的三条：
1. `HttpUtil.get()` 方法冲突时抛异常（原先静默改回 GET），上层若有
   `setMethod(PUT).get()` 链式写法必须改。
2. `NumberUtil` 的 long 重载溢出抛异常（原先 `multiply(MAX,4)` 静默返回 `-4`），金额场景需评估。
3. CSV 新增公式注入防护，以 `= + - @` 开头的值加单引号前缀，下游解析需同步调整。

---

## 2026-09-29 修复测试暴露的缺陷

### 修复清单（18 个主源码文件，+275/-63）

| 文件 | 修复内容 |
|------|----------|
| `RootModel` | **`desensitize()` 之前完全不生效**（空白名单走错分支）：改为先 `excludeNotMeta()` 再以自身类白名单脱敏；白名单为 null 按空名单处理；集合含 null 元素不再 NPE；`excludeFieldValueNotMeta` 补上模型集合递归；`setFieldValue(this,...)` → `setFieldValue(instance,...)` |
| `AccessTokenUtil` | **过期时间为 0 的令牌永不过期**（`!= 0` 短路漏洞）：改为统一 `expire < now` 判定；畸形令牌（过期时间非数字、负载非 Base64、负载非 JSON、accessToken 为空）统一抛 401 `ServiceException`；拆出 `parseExpireTimestamps` / `parsePayloads` |
| `FileUtil` | `formatSize` 的 `DecimalFormat` 未指定 Locale，德语环境输出 `1,00KB` → 固定 `Locale.ROOT` |
| `RsaUtil` | 私钥 PEM 头尾 `RSA PRIVATE KEY`(PKCS#1) 与内容(PKCS#8) 不符，OpenSSL 解析失败 → 改为 `PRIVATE KEY` |
| `HttpUtil` | 无 Cookie 时也发空 `Cookie` 头（`Objects.nonNull(cookies)` 恒真）→ 改判 `!isEmpty()`；`send()` 用 `initCause` 保留原始异常 |
| `RandomUtil` | 英文异常消息改中文；`randomBytes(-1)` 的 `NegativeArraySizeException` 改 `ServiceException` |
| `ValidateUtil` | `isXxx(null)` 不再 NPE（返回 false）；`valid(model, null)` 按空分组处理；身份证前 17 位数字预检 + 支持小写 `x`；`validator` 改 volatile + synchronized 双检 |
| `DesensitizeUtil` | 脱敏符号为空会静默删除原文（NPE/丢数据）→ 回退为默认 `*` |
| `ReflectUtil` | `getLambdaFunctionName` 用 `replace("get","")` 删掉方法名里所有 get → 只去前缀；异常消息 `Lamba` → `Lambda` |
| `NumberUtil` | 可变参数显式传 `null` 时 `Arrays.stream` NPE → `requireNonNullElse` 兜底 |
| `TreeUtil` | `getChildrenIdList` 子节点 ID 为 null 时拆箱 NPE；环形数据无限递归 → 过滤 null + 已收集集合防环 |
| `Json` | `parse`/`parseList`/`toString` 只捕获 `JsonProcessingException`，传 null 泄漏 `IllegalArgumentException` → 统一捕获 `Exception` |
| `CollectionUtil` | 排序注释与实现相反；导出列异常静默吞掉 → 修正注释 + `log.warn` |
| `AesUtil` | `setKey(String)` 的 NPE/IllegalArgumentException → 统一中文 `ServiceException`；未设 key 报"加密密钥未设置"；加解密失败加中文前缀 |
| `TaskUtil` + `TraceUtil` | 任务只 catch `Exception`（Error 被 FutureTask 吞掉）→ catch `Throwable`；任务结束清理 MDC，新增 `TraceUtil.clearTraceId()` |
| `Constant` / `HttpConstant` | 常量类未禁止实例化 → 补私有构造器 |
| `AGENTS.md` | 版本 6.4.0→7.0.0、删除不存在的 `AesUtil.cipherCache`、补充新的已知陷阱与本机 mvn 路径 |

### 测试同步

1071 个用例（新增/改写 20+ 条），全部中文断言消息。**测试断言的是修复后的正确行为**，
并对每条修复补了回归用例（如 `formatSize` 的 `localeIndependent`、`desensitize` 的全字段行为、
`TreeUtil` 的环形数据用例）。在 zh_CN / de_DE / ar_EG / ja_JP / th_TH 五种 Locale 与多时区下全绿，连续 4 遍零抖动。

### 行为变更提示（对上层 airpower-web 的影响）

1. `desensitize()` 现在真的会脱敏 —— 上层如果依赖"只排除不脱敏"的旧行为，返回的字段值会变。
2. `excludeNotMeta()` 现在会递归清理模型集合元素。
3. `RsaUtil.getPemPrivateKey()` 输出的 PEM 头尾变了（如果上层做字符串精确匹配需要改）。
4. `ValidateUtil.isXxx(null)` 从抛 NPE 变成返回 false；`AccessTokenUtil.verify` 畸形令牌从抛 JDK 异常变成 401。

---

## 2026-09-29 补齐全量单元测试

### 任务目标

为 `src/main` 下所有类的公开方法补齐单元测试，覆盖边界条件，断言失败消息一律使用中文。

### 背景

- `src/test` 目录结构存在但**全部为空**：commit `211b53d`（2026-09-29）以"删除多个工具类的单元测试文件"为
  由删掉了 17 个测试类（6144 行）。本次是从零重写。
- 主源码 42 个文件 / 5814 行，公开方法约 100 个（含重载 200+ 个签名）。

### 完成内容

- 新增 **30 个测试类 + 6 个共享夹具**，13490 行，**1055 个用例**，clean 构建 4.9s，连续多遍零抖动。
- 在 zh_CN / de_DE / ar_EG / ja_JP / th_TH 五种 Locale × 多个时区下全量通过。
- 夹具（`cn.hamm.airpower.core.fixture`）：`DemoModel`（RootModel 全注解场景）、`Gender`（IDictionary 枚举）、
  `DemoTree`（IEntity+ITree）、`DemoError`（IException 枚举）、`ExportDemoModel`（@Export 各类型列）、
  `ValidDemoModel`（jakarta 校验 + 分组）。
- 测试类与主类一一对应（`XxxTest`），另加 2 个横切测试：
  `annotation/AnnotationsTest`（9 个注解的 Retention/Target/默认值）、`interfaces/IEntityTest`（IEntity/ITree/IFunction）。
- `HttpUtilTest` 用 JDK 自带 `com.sun.net.httpserver` 跑真实本地请求，不依赖外网。
- `src/main` **零改动**（一个子代理误改了 `Constant.java`，已 `git checkout` 回滚）。

### 关键决策

1. **断言必须与源码实际行为一致**，包括源码的缺陷行为（如 `RootModel.desensitize()` 实际不脱敏），
   测试如实固化现状并在注释/报告中标注为缺陷，不擅自改源码。
2. **每条断言都带中文失败消息**（`assertEquals(期望, 实际, "中文")`），`@DisplayName` 全中文。
3. `ValidateUtil.valid()` 抛的是 `jakarta.validation.ValidationException`（不是 `ServiceException`），
   测试按此断言。
4. 并行子代理只允许写测试、不允许跑 `mvn`（并发会破坏 `target/`），统一由主流程编译运行。
   注意：子代理可能自行 `git add/commit` 或改 `src/main`，主流程收尾时必须 `git status` 复核。
5. **测试必须与运行环境无关**：`DecimalFormat`/`String.format` 受默认 Locale 影响
   （德语小数点变 `,`、阿拉伯语数字变 `١`），断言用 `DecimalFormatSymbols` 计算期望值、
   正则用 `\p{Nd}` 而不是 `\d`。
6. **不要断言 `Class#getDeclaredFields` 的字段顺序**（JVM 不保证），用 `Set` 比较。

### 环境备忘

- 本机**没有全局 `mvn`**，用 IntelliJ 自带：
  `/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn -o test`（离线可用）。
- 编译期若某个测试文件有语法/注解错误，javac 会跳过 Lombok 注解处理轮次，
  表现为**全项目 Lombok 生成的 getter/setter 全部"找不到符号"**——先修第一个报错即可。

### 遗留问题（已修复，见上一节）

原清单里的 P0/P1/P2 全部已修复并补充了回归测试。仍未处理的**设计层面**问题（需要产品决策，本次未动）：

- `HttpUtil` 只有 `connectTimeout`，没有请求级超时（服务端不响应会一直阻塞）。
- `HttpUtil` 的 `get()/post()` 会无条件改写 `method`，链式 `setMethod(PUT).get()` 会被静默改回 GET；
  枚举里的 `PATCH` 在 `getHttpRequest` 未实现，运行期才报错。
- `ProxySelector.connectFailed` 是空实现，代理不可用时不会回退直连。
- `ValidateUtil` 的 `ValidatorFactory` 从不关闭；`valid()` 只取 `violations.iterator().next()`，多违规时报错字段不固定。
- `RsaUtil` 的 `KeyFactory` 缓存不随 `cryptAlgorithm` 失效；加解密用平台默认字符集而非 UTF-8。
- `TreeUtil.buildTreeList` 无环检测（环形数据仍会栈溢出），孤儿节点静默丢弃。
- `NumberUtil` 的 long 重载溢出无保护（`multiply(MAX,4)` → `-4`）；`floor/ceil` 实际是向 0 截断。
- `FileUtil.deleteDirectory` 用 `File::delete` 忽略返回值，删除失败无感知；`zip` 遍历顺序不确定。
- `HttpConstant`/`Constant` 的内部类现在有了私有构造器，但 `HttpConstant.Status` 等仍是 public static class。

- 用户偏好：
  - 错误信息、断言消息、注释一律中文。
  - 只让写测试，不接受子代理顺手改 `src/main`。

### 提交情况

- `da87cb0` 补齐全量测试（由子代理提交，含 `.agent/` 记忆文件）
- `e80dd15` 测试去除 Locale / 字段顺序依赖
- 尚未 push，`origin/dev` 落后本地 16 个提交，等用户确认。
