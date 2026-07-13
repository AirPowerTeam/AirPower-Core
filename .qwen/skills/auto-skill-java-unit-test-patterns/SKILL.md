---
name: java-unit-test-patterns
description: 为 Java 工具类编写全面单元测试的策略和模式，包括边界条件、异常处理和实际网络/文件 I/O 测试
source: auto-skill
extracted_at: '2026-07-13T09:22:27.325Z'
---

# Java 单元测试编写策略

## 核心原则

1. **逐个类编写**：按字母顺序（A-Z）逐个类完成测试，每完成一个类让用户确认后再继续下一个
2. **边界条件优先**：每个公开方法都必须测试正常路径和异常/边界路径
3. **实际依赖测试**：对于网络请求、文件操作等，使用真实环境而非全部 Mock

## 测试结构模板

```java
class XxxUtilTest {
    // 按方法分组，使用注释分隔
    // ==================== methodName 方法测试 ====================

    // 正常路径
    @Test void testMethodNameWithNormalCase() { }

    // 边界条件
    @Test void testMethodNameWithNull() { }
    @Test void testMethodNameWithEmpty() { }
    @Test void testMethodNameWithNegative() { }

    // 异常路径
    @Test void testMethodNameWithInvalidInput() { }
}
```

## 按类类型分类策略

### 纯工具类（如 DateTimeUtil、StringUtil）
- 测试所有公开方法
- 重点：null、空字符串、负数、零值、极大/极小值
- 常量验证（如果有公共常量）

### 文件操作类（如 FileUtil）
- 使用 `@TempDir` 创建临时目录
- 测试：创建、读取、写入、删除、覆盖
- 清理：JUnit 自动处理 `@TempDir`

### 网络请求类（如 HttpUtil）
- 使用真实 URL 进行测试（如 https://ip.hamm.cn）
- 测试：GET、POST、自定义 Header、Cookie
- 错误处理：无效 URL、null URL、不支持的方法
- 注意：日志输出会很多，但测试应该通过

### 枚举/字典类（如 DictionaryUtil）
- 在测试目录创建测试用的枚举实现
- 测试：按 key 查找、按自定义属性查找、获取列表
- 边界：空枚举、不存在值、null 值

### 任务执行类（如 TaskUtil）
- 测试：同步执行、异步执行、异常处理
- 边界：null 任务、空任务、多任务、大量任务
- 注意：
  - `run()` 和 `runAsync()` 内部都捕获了异常，不会向外抛出
  - 传入 `null` 时内部会抛出 `NullPointerException`，但被 try-catch 捕获并记录日志，所以外部不会收到异常
  - 同步执行按顺序完成，异步执行通过线程池并行
  - 异常测试应使用 `assertDoesNotThrow()` 而非 `assertThrows()`
  - 并行验证测试不要依赖精确的时间计算（受系统调度影响），而是验证所有任务最终都能完成
  - 异步测试需要 `Thread.sleep()` 等待任务完成，时间要足够长（建议 200ms+）
  - 大量任务测试（如100个）验证线程池能正常处理

### Trace/日志类（如 TraceUtil）
- 测试：设置、获取、重置、MDC 集成
- 边界：null、空字符串、空白字符串、特殊字符、Unicode、长字符串
- 注意：
  - 使用 `@BeforeEach` 和 `@AfterEach` 清除 MDC 状态，避免测试间互相影响
  - `setTraceId(null)` 和 `setTraceId("")` 都会自动生成 UUID
  - 验证 UUID 格式使用 `assertDoesNotThrow(() -> UUID.fromString(traceId))`
  - 连续重置应生成不同的 UUID
  - 值直接存储在 MDC 中，可以通过 `MDC.get(key)` 验证

### 加密/安全类（如 AesUtil, RsaUtil）
- 边界：null 输入、空字符串、错误密钥
- 注意：同一实例多次加密结果可能相同（Cipher 缓存）
- RSA 特有：
  - 测试公钥加密/私钥解密、私钥加密/公钥解密
  - 测试签名和验签（正确签名、错误签名、错误内容）
  - 测试长文本（超过一个 block 大小）
  - 测试 PEM 格式转换（包含换行符）
  - 密钥缓存测试（两次获取同一实例应返回相同对象）
  - 异常信息优化：建议在 `getPublicKey()`/`getPrivateKey()` 中检查 null，抛出明确的 `ServiceException`（如"RSA 公钥未设置，请先调用 setPublicKey() 方法设置公钥"），而不是让 `Base64.decode(null)` 抛出模糊的 `NullPointerException`
  - 错误签名测试：使用有效 Base64 编码但内容错误的签名（如用不同内容生成的签名），而不是任意字符串，否则会在 Base64 解码后抛出 `ServiceException: Bad signature length`
- 异常测试：未设置密钥时应抛出 `ServiceException`

### 树结构工具类（如 TreeUtil）
- 测试：空列表、单节点、简单层级、深层嵌套、多根节点、无序输入
- 边界：null 父节点（视为根节点）、节点无父节点（孤立节点）、循环引用
- 注意：
  - 返回的列表是 `Collections.unmodifiableList()`，不可修改
  - 输入顺序不影响输出结果（使用 Map 预构建）
  - 所有节点都应该被包含在树中（可通过递归计数验证）
  - `findByParentId` 的 function 参数返回 null 时应返回空列表
  - `ensureNoChildrenBeforeDelete` 在 function 返回 null 时不抛异常
  - `getChildrenIdList` 递归获取所有嵌套子节点 ID，使用 Set 去重

### 验证工具类（如 ValidateUtil）
- 测试：有效值、无效值、边界值
- 边界：null、空字符串、特殊字符、中文、Unicode、超长字符串
- 注意：
  - 所有正则验证方法在传入 null 时会抛出 `NullPointerException`（因为 `Pattern.matcher(null)` 会 NPE）
  - `isChina2Identity` 对15位身份证抛出 `ServiceException`（"暂不支持一代身份证校验"）
  - `isNormalCode` 只匹配**单个字符**（正则 `^...$` 且没有量词），多字符会返回 false
  - `validRegex` 方法直接调用 `pattern.matcher(value).matches()`，传入 null 会 NPE
  - `valid` 方法（Jakarta Validation）需要 Hibernate Validator 等 provider 在 classpath 中
  - 身份证校验位计算：前17位加权求和，模11取余，查表得到校验位
  - 正则常量定义在 `PatternConstant` 中，测试前需了解具体正则表达式含义
### JSON 处理类（如 Json）
- 测试：序列化、反序列化、解析为 Map/List/对象
- 边界：null、空字符串、非法 JSON、空数组
- 注意：Jackson 等库对 null 输入可能抛出 `IllegalArgumentException` 而非 `ServiceException`
- 内部测试类：创建简单的 POJO（带 getter/setter）用于反序列化测试
- 常量验证：验证 SUCCESS_CODE、ERROR_CODE 等常量值

### 反射工具类（如 ReflectUtil）
- 测试：字段访问、方法调用、注解获取、Lambda 解析
- 边界：null 对象、null Class、私有字段、继承链
- 注意：
  - `getFieldValue(null, field)` 会抛出 `NullPointerException`（Java 反射行为）
  - `getFieldList(null)` 可能抛出 `NullPointerException` 或自定义异常（取决于源码实现）
  - 内部类定义在测试方法中可能导致 `NoClassDefFoundError`，建议定义为顶层内部类
- Lambda 测试：`getLambdaFunctionName` 对 `getXxx` 返回 `Xxx`，对 `isXxx` 返回 `isXxx`（因为没有 `get` 前缀可替换）

### 随机生成类（如 RandomUtil）
- 测试：随机字节数组、随机字符串、随机数字、随机整数
- 边界：长度=0、负数长度、极大长度、单字符样本
- 注意：
  - `randomBytes(-1)` 会抛出 `NegativeArraySizeException`（`new byte[-1]` 的行为）
  - `randomString(0)` 被源码修正为长度1（`Math.max(length, 1)`）
  - `randomInt(0)` 会抛出 `IllegalArgumentException`（`ThreadLocalRandom.nextInt(0)` 的行为）
- 随机性验证：两次生成结果不应相等（极大概率）
- 范围验证：使用 `@RepeatedTest` 多次验证随机数在指定范围内

### 数据模型类（如 RootModel）
- 测试：字段过滤、脱敏、递归处理、集合处理
- 边界：null 字段、空字符串、空集合、null 子对象
- 注意：
  - 内部类必须定义为顶层内部类（`public static class`），不能定义在测试方法内部，否则会导致 `NoClassDefFoundError`
  - 白名单逻辑：`excludeNotMeta()` 默认白名单包含当前类，非Meta字段**不会**被清空
  - 只有白名单**非空且不包含当前类**时，才会触发排除逻辑
  - 脱敏对空字符串和短字符串有保护（长度不足时不脱敏）
  - `assertDoesNotThrow(() -> model.method())` 使用 lambda 而非方法引用，避免方法重载歧义

## 常见边界条件清单

| 类型 | 测试值 |
|------|--------|
| 字符串 | null, "", " ", 特殊字符, 中文, 超长字符串 |
| 数字 | 0, -1, Integer.MAX_VALUE, Integer.MIN_VALUE, Long.MAX_VALUE |
| 集合 | null, 空集合, 单元素, 多元素 |
| 日期 | 1970-01-01, 当前时间, 未来时间, 闰年2月29日 |
| 文件路径 | 不存在路径, 无权限路径, 超长路径, 特殊字符 |
| JSON | null, "", "invalid", "[]", "{}" |

## 常见异常类型参考

| 场景 | 可能抛出的异常 |
|------|--------------|
| 参数为 null | `IllegalArgumentException`（框架层）或 `ServiceException`（业务层） |
| 空字符串 | `ServiceException`（业务校验）或 Jackson 的 `MismatchedInputException` |
| 非法 JSON | `ServiceException`（包装后的 Jackson 异常） |
| 格式错误 | `ServiceException`（自定义错误消息） |
| 负数时间戳 | `ServiceException`（自定义错误消息） |

## Maven 测试命令

```bash
# 单个测试类
~/Applications/IntelliJ\ IDEA\ Ultimate.app/Contents/plugins/maven/lib/maven3/bin/mvn test -Dtest=ClassNameTest -q

# 所有测试
~/Applications/IntelliJ\ IDEA\ Ultimate.app/Contents/plugins/maven/lib/maven3/bin/mvn test -q
```

## 注意事项

1. **编译错误**：注意 import 语句，特别是 `java.io.File` 和 `java.nio.file.Path` 等容易混淆的类
2. **时区问题**：日期测试注意系统时区（如 UTC vs 东八区）
3. **网络超时**：网络测试设置合理超时时间
4. **资源清理**：文件/网络资源使用 try-with-resources 或 `@AfterEach`
5. **日志噪音**：网络测试日志输出多，使用 `-q` 参数减少输出
6. **JSON 字符串转义**：Java 字符串中的 `"` 需要转义为 `\"`，避免编译错误
7. **数组 vs 字符串 length**：`byte[].length` 是属性（无括号），`String.length()` 是方法（有括号），不要混淆
