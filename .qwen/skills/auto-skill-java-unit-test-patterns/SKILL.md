---
name: java-unit-test-patterns
description: 为 Java 工具类编写全面单元测试的策略和模式，包括边界条件、异常处理和实际网络/文件 I/O 测试
source: auto-skill
extracted_at: '2026-07-13T08:57:43.540Z'
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

### 加密/安全类（如 AesUtil）
- 测试：加密、解密、密钥生成
- 边界：null 输入、空字符串、错误密钥
- 注意：同一实例多次加密结果可能相同（Cipher 缓存）

### JSON 处理类（如 Json）
- 测试：序列化、反序列化、解析为 Map/List/对象
- 边界：null、空字符串、非法 JSON、空数组
- 注意：Jackson 等库对 null 输入可能抛出 `IllegalArgumentException` 而非 `ServiceException`
- 内部测试类：创建简单的 POJO（带 getter/setter）用于反序列化测试
- 常量验证：验证 SUCCESS_CODE、ERROR_CODE 等常量值

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
