# AirPower-Core 性能优化计划

> 本计划基于对项目代码的全面分析，列出了所有可优化的性能问题及改进建议。
> 
> 项目信息：AirPower-Core v6.2.0，共 41 个 Java 文件，Java 17

---

## 一、高优先级问题

### 1. TreeUtil.buildTreeList - O(n²) 递归树构建算法

**文件**: `TreeUtil.java` (第 52-57 行)

**问题描述**: `buildTreeList` 方法使用递归 + 全量扫描的方式构建树结构。对于每个节点，都会遍历整个列表查找其子节点，时间复杂度为 O(n²)。当数据量较大时（如 10,000 条数据），性能会急剧下降。

```java
private static <E extends IEntity<E> & ITree<E>> List<E> buildTreeList(@NotNull List<E> list, long parentId) {
    return list.stream()
            .filter(item -> Objects.equals(parentId, item.getParentId()))  // 每次递归都全量扫描
            .map(item -> item.setChildren(
                    buildTreeList(list, item.getId())  // 递归调用
            ))
            .toList();
}
```

**影响程度**: 🔴 高

**优化建议**:
- 使用 Map 预构建 parentId -> children 的映射，将时间复杂度降为 O(n)
- 避免递归深度过大导致栈溢出

```java
public static <E extends IEntity<E> & ITree<E>> List<E> buildTreeList(List<E> list) {
    Map<Long, List<E>> parentMap = list.stream()
            .collect(Collectors.groupingBy(E::getParentId));
    
    return buildTreeWithMap(parentMap, ROOT_ID);
}

private static <E extends IEntity<E> & ITree<E>> List<E> buildTreeWithMap(Map<Long, List<E>> parentMap, long parentId) {
    List<E> children = parentMap.getOrDefault(parentId, Collections.emptyList());
    for (E child : children) {
        child.setChildren(buildTreeWithMap(parentMap, child.getId()));
    }
    return children;
}
```

---

### 2. DateTimeUtil.format - DateTimeFormatter 未缓存

**文件**: `DateTimeUtil.java` (第 145-149 行)

**问题描述**: 每次调用 `format` 方法时都会创建新的 `DateTimeFormatter` 实例。`DateTimeFormatter.ofPattern()` 是昂贵的操作，涉及模式解析。

```java
public static String format(long milliSecond, String formatter, String zone) {
    Instant instant = Instant.ofEpochMilli(milliSecond);
    ZonedDateTime beijingTime = instant.atZone(ZoneId.of(zone));
    return beijingTime.format(java.time.format.DateTimeFormatter.ofPattern(formatter));  // 每次创建新实例
}
```

**影响程度**: 🔴 高

**优化建议**:
- 使用 `ConcurrentHashMap` 缓存已创建的 `DateTimeFormatter`
- 对常用格式使用预定义的静态常量

```java
private static final ConcurrentHashMap<String, DateTimeFormatter> FORMATTER_CACHE = new ConcurrentHashMap<>();

private static DateTimeFormatter getFormatter(String pattern) {
    return FORMATTER_CACHE.computeIfAbsent(pattern, p -> DateTimeFormatter.ofPattern(p));
}
```

---

### 3. ReflectUtil.getFieldList - 反射字段列表缓存粒度问题

**文件**: `ReflectUtil.java` (第 244-258 行)

**问题描述**: 虽然使用了 `ConcurrentHashMap` 缓存字段列表，但 `getCacheFieldList` 方法在递归获取父类字段时，每次都会创建新的 `LinkedList` 并合并结果。对于深层继承结构，这会导致多次列表创建和复制。

```java
private static List<Field> getCacheFieldList(Class<?> clazz) {
    List<Field> fieldList = new LinkedList<>();  // 每次创建新列表
    // ...
    fieldList.addAll(getCacheFieldList(superClass));  // 递归合并
    return fieldList;
}
```

**影响程度**: 🟡 中

**优化建议**:
- 使用 `Collections.unmodifiableList` 包装缓存结果
- 考虑使用更高效的列表类型（如 `ArrayList` 替代 `LinkedList`）
- 预计算并缓存完整的字段列表，避免递归时的重复合并

---

### 4. RootModel.excludeNotMetaAndDesensitize - 递归反射性能问题

**文件**: `RootModel.java` (第 83-130 行)

**问题描述**: 该方法在处理嵌套对象时，对每个字段都进行反射操作（获取注解、获取值、设置值）。对于包含大量字段或深层嵌套的对象，反射开销显著。

**影响程度**: 🟡 中

**优化建议**:
- 考虑使用缓存的字段元数据（预计算哪些字段需要处理）
- 对 Collection 类型的处理，避免不必要的类型转换
- 使用更高效的循环替代 `forEach`

---

## 二、中优先级问题

### 5. CollectionUtil.getExportFieldList - 重复反射获取注解

**文件**: `CollectionUtil.java` (第 94-121 行)

**问题描述**: 在遍历字段时，对每个字段都通过反射获取 `Export` 注解。虽然 `ReflectUtil.getAnnotation` 有缓存，但 `getMethod` 调用仍然昂贵。

```java
ReflectUtil.getFieldList(itemClass).forEach(field -> {
    // 对每个字段都通过反射获取 Getter 方法
    Method getter = itemClass.getMethod(fieldGetter);
    export = ReflectUtil.getAnnotation(Export.class, getter);
    // ...
});
```

**影响程度**: 🟡 中

**优化建议**:
- 预计算并缓存类的导出字段元数据
- 避免在循环中重复调用 `getMethod`

---

### 6. HttpUtil - HttpClient 单例模式线程安全问题

**文件**: `HttpUtil.java` (第 145-153 行)

**问题描述**: `HttpClient` 使用懒加载单例模式，但存在潜在的线程安全问题（虽然 `HttpClient` 本身是线程安全的，但初始化逻辑不是原子的）。

```java
private HttpClient getHttpClient() {
    if (Objects.isNull(httpClient)) {  // 非原子检查
        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder();
        httpClientBuilder.connectTimeout(Duration.ofSeconds(5));
        httpClient = httpClientBuilder.build();
    }
    return httpClient;
}
```

**影响程度**: 🟡 中

**优化建议**:
- 使用 `volatile` 关键字或双重检查锁定
- 或者使用静态初始化块提前初始化

```java
private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
```

---

### 7. Json.getObjectMapper - ObjectMapper 单例非线程安全初始化

**文件**: `Json.java` (第 236-248 行)

**问题描述**: `ObjectMapper` 的懒加载初始化存在竞态条件，多个线程可能同时创建实例。

```java
private static ObjectMapper getObjectMapper() {
    if (Objects.isNull(objectMapper)) {  // 非原子检查
        objectMapper = new ObjectMapper();
        // ...
    }
    return objectMapper;
}
```

**影响程度**: 🟡 中

**优化建议**:
- 使用静态初始化块
- 或使用 `volatile` + 双重检查锁定

```java
private static final ObjectMapper OBJECT_MAPPER;

static {
    OBJECT_MAPPER = new ObjectMapper();
    OBJECT_MAPPER.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    OBJECT_MAPPER.configOverride(Map.class)
            .setInclude(JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, null));
    OBJECT_MAPPER.configure(FAIL_ON_EMPTY_BEANS, false);
}
```

---

### 8. AesUtil - 每次加解密都创建 Cipher 实例

**文件**: `AesUtil.java` (第 83-96 行)

**问题描述**: 每次加密/解密都创建新的 `Cipher` 实例。`Cipher.getInstance()` 涉及安全提供者查找和算法初始化，是昂贵的操作。

```java
private Cipher getCipher(int type) {
    SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(UTF_8), algorithm);
    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes(UTF_8));
    Cipher cipher = Cipher.getInstance(algorithm + "/" + mode + "/" + padding);  // 每次创建
    cipher.init(type, secretKeySpec, ivParameterSpec);
    return cipher;
}
```

**影响程度**: 🟡 中

**优化建议**:
- 对于相同的 key/iv/algorithm 组合，缓存 `Cipher` 实例
- 注意 `Cipher` 不是线程安全的，需要使用 `ThreadLocal` 或对象池

---

### 9. RsaUtil - RSA 密钥重复解析

**文件**: `RsaUtil.java` (第 65-73 行, 第 165-173 行)

**问题描述**: `getPublicKey` 和 `getPrivateKey` 方法每次调用都重新解析密钥。在加密/解密操作中，这些方法被频繁调用。

```java
public PublicKey getPublicKey(String publicKeyString) throws Exception {
    KeyFactory keyFactory = KeyFactory.getInstance(cryptAlgorithm);  // 每次创建
    // ...
}
```

**影响程度**: 🟡 中

**优化建议**:
- 缓存解析后的 `PublicKey` 和 `PrivateKey` 实例
- 缓存 `KeyFactory` 实例

---

## 三、低优先级问题

### 10. DesensitizeUtil.replace - IntStream 替代简单循环

**文件**: `DesensitizeUtil.java` (第 38-49 行)

**问题描述**: 使用 `IntStream.range` 进行简单的字符串遍历和拼接，引入了不必要的流式操作开销。

```java
IntStream.range(0, text.length()).forEach(i -> {
    if (i >= head && i <= text.length() - tail - 1) {
        stringBuilder.append(symbol);
    } else {
        stringBuilder.append(text.charAt(i));
    }
});
```

**影响程度**: 🟢 低

**优化建议**:
- 使用传统的 `for` 循环替代 `IntStream`
- 或者使用 `StringBuilder` 的 `append` 方法直接拼接子串

```java
StringBuilder sb = new StringBuilder();
sb.append(text, 0, head);
sb.append(symbol.repeat(Math.max(0, text.length() - head - tail)));
sb.append(text, text.length() - tail, text.length());
```

---

### 11. RandomUtil.randomBytes - Math.random() 性能较差

**文件**: `RandomUtil.java` (第 46-51 行)

**问题描述**: 使用 `Math.random()` 生成随机字节，而 `Math.random()` 内部使用 `synchronized` 的 `Random` 实例，在高并发场景下会成为瓶颈。

```java
IntStream.range(0, length).forEach(i -> bytes[i] = (byte) (Math.random() * 256 - 128));
```

**影响程度**: 🟢 低

**优化建议**:
- 使用 `ThreadLocalRandom`（项目中已有 `getRandom()` 方法）

```java
ThreadLocalRandom random = ThreadLocalRandom.current();
for (int i = 0; i < length; i++) {
    bytes[i] = (byte) random.nextInt(256);
}
```

---

### 12. NumberUtil.calculate - 可变参数创建数组开销

**文件**: `NumberUtil.java` (第 160-173 行)

**问题描述**: 每次调用 `add`, `subtract`, `multiply` 等方法时，都会创建 `BigDecimal` 数组和 `BigDecimal` 对象。对于简单的两数运算，这引入了不必要的开销。

```java
public static double add(double first, double second, double... values) {
    return calculate(BigDecimal::add, BigDecimal.valueOf(first), BigDecimal.valueOf(second),
            Arrays.stream(values).mapToObj(BigDecimal::valueOf).toArray(BigDecimal[]::new)
    ).doubleValue();
}
```

**影响程度**: 🟢 低

**优化建议**:
- 为两数运算提供重载方法，避免创建数组
- 或者使用基本类型计算，仅在最后转换为 `BigDecimal`

---

### 13. FileUtil.zipDirectory - 小缓冲区 (1024 bytes)

**文件**: `FileUtil.java` (第 163-168 行)

**问题描述**: ZIP 压缩时使用的缓冲区只有 1024 字节，对于大文件，这会导致频繁的 IO 操作。

```java
try (FileInputStream fis = new FileInputStream(path.toFile())) {
    byte[] buffer = new byte[1024];  // 缓冲区过小
    int length;
    while ((length = fis.read(buffer)) > 0) {
        zos.write(buffer, 0, length);
    }
}
```

**影响程度**: 🟢 低

**优化建议**:
- 将缓冲区大小增加到 8KB 或 16KB（如 8192 或 16384）
- 考虑使用 `BufferedInputStream` 包装 `FileInputStream`

```java
try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path.toFile()), 8192)) {
    byte[] buffer = new byte[8192];
    int length;
    while ((length = bis.read(buffer)) > 0) {
        zos.write(buffer, 0, length);
    }
}
```

---

### 14. ValidateUtil.initValidator - ValidatorFactory 过早关闭

**文件**: `ValidateUtil.java` (第 34-39 行)

**问题描述**: `ValidatorFactory` 在初始化 `Validator` 后就被关闭（try-with-resources）。虽然 `Validator` 在关闭后仍可继续使用，但这不符合 Jakarta Validation 的最佳实践。

```java
private static void initValidator() {
    try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
        validator = validatorFactory.getValidator();
    }
}
```

**影响程度**: 🟢 低

**优化建议**:
- 保持 `ValidatorFactory` 打开，或在应用关闭时统一关闭
- 或者使用静态初始化块一次性初始化

```java
private static final Validator VALIDATOR;

static {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    VALIDATOR = factory.getValidator();
    // 在应用关闭时关闭 factory
}
```

---

### 15. TaskUtil.EXECUTOR - 线程池参数可优化

**文件**: `TaskUtil.java` (第 20-25 行)

**问题描述**: 线程池使用固定的核心线程数和最大线程数，但没有根据系统资源动态调整。在 CPU 密集型任务中，过多的线程会导致上下文切换开销。

```java
private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
        5,
        20,
        3600L,
        SECONDS,
        new LinkedBlockingQueue<>(1000)
);
```

**影响程度**: 🟢 低

**优化建议**:
- 根据 `Runtime.getRuntime().availableProcessors()` 动态设置线程数
- 为线程池设置有意义的线程名称，便于监控和调试
- 考虑使用 `Executors.newWorkStealingPool()` 或自定义 `ForkJoinPool`

```java
private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();
private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
        CORE_POOL_SIZE,
        CORE_POOL_SIZE * 2,
        60L,
        SECONDS,
        new LinkedBlockingQueue<>(1000),
        new ThreadFactoryBuilder().setNameFormat("airpower-task-%d").build()
);
```

---

## 四、代码风格与可维护性建议

### 16. 常量字符串拼接

**文件**: `RsaUtil.java`, `AccessTokenUtil.java` 等

**问题描述**: 多处使用字符串拼接构建错误消息，虽然现代 JVM 对此有优化，但在频繁调用的场景下仍建议使用 `StringBuilder` 或格式化方法。

**优化建议**:
- 使用 `String.format()` 或 `MessageFormat`
- 对于固定的错误消息，使用预编译的模板

---

### 17. 日志记录最佳实践

**文件**: 多处

**问题描述**: 部分日志记录没有使用占位符，导致字符串拼接在日志级别不满足时也会执行。

```java
// 当前代码
log.info("请求地址: {} {}", method.name(), url);  // 正确
log.error(e.getMessage(), e);  // 正确

// 但有些地方可能直接拼接（虽然当前代码中未发现明显问题）
```

**优化建议**:
- 始终使用 SLF4J 的占位符语法
- 避免在日志参数中进行复杂的字符串拼接

---

## 五、优化优先级总结

| 优先级 | 问题 | 文件 | 预期收益 |
|--------|------|------|----------|
| 🔴 高 | TreeUtil O(n²) 算法 | TreeUtil.java | 大数据量树构建性能提升 10-100 倍 |
| 🔴 高 | DateTimeFormatter 未缓存 | DateTimeUtil.java | 高频时间格式化性能提升 5-10 倍 |
| 🟡 中 | ReflectUtil 反射缓存优化 | ReflectUtil.java | 减少反射开销 20-30% |
| 🟡 中 | RootModel 递归反射优化 | RootModel.java | 减少反射和对象创建开销 |
| 🟡 中 | CollectionUtil 重复反射 | CollectionUtil.java | 减少注解获取开销 |
| 🟡 中 | HttpClient 线程安全 | HttpUtil.java | 消除潜在的并发问题 |
| 🟡 中 | ObjectMapper 线程安全 | Json.java | 消除潜在的并发问题 |
| 🟡 中 | AesUtil Cipher 缓存 | AesUtil.java | 减少加解密初始化开销 |
| 🟡 中 | RsaUtil 密钥缓存 | RsaUtil.java | 减少密钥解析开销 |
| 🟢 低 | DesensitizeUtil 流式操作 | DesensitizeUtil.java | 减少流式 API 开销 |
| 🟢 低 | RandomUtil 随机数生成 | RandomUtil.java | 提升高并发随机数生成性能 |
| 🟢 低 | NumberUtil 数组创建 | NumberUtil.java | 减少小对象创建 |
| 🟢 低 | FileUtil 缓冲区大小 | FileUtil.java | 提升大文件压缩性能 |
| 🟢 低 | ValidateUtil 工厂管理 | ValidateUtil.java | 符合 Jakarta Validation 规范 |
| 🟢 低 | TaskUtil 线程池优化 | TaskUtil.java | 更好的资源利用和可观测性 |

---

## 六、实施建议

1. **第一阶段（高优先级）**: 先解决 `TreeUtil` 和 `DateTimeUtil` 的问题，这两个改动影响最大
2. **第二阶段（中优先级）**: 处理线程安全和缓存相关问题（HttpUtil、Json、AesUtil、RsaUtil）
3. **第三阶段（低优先级）**: 优化细节问题（缓冲区大小、流式操作、线程池参数等）
4. **测试**: 每个优化点后都要进行性能测试，确保优化有效且没有引入回归问题
