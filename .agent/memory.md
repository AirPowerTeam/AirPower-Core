# AirPower-Core 项目记忆

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

### 遗留问题（均为源码缺陷，本次未改）

- P0：`RootModel.desensitize()` 传空白名单 → 走"排除非元数据"分支，**脱敏完全不生效**（RootModel.java:57）。
- P0：`AccessTokenUtil.verify()` 中 `!= 0` 短路 → 过期时间为 0 的令牌**永不过期**（AccessTokenUtil.java:225）。
- P1：`RootModel` 白名单分支对含 `null` 元素的集合抛 NPE（RootModel.java:98）。
- P1：`FileUtil.formatSize` 用 `new DecimalFormat("#.00")` 依赖默认 Locale，德语环境输出 `1,00B`。
- P1：`HttpUtil` 每个请求都发空 `Cookie` 头（`Objects.nonNull(cookies)` 恒真）。
- P1：`RsaUtil` 私钥 PEM 头写 `RSA PRIVATE KEY`(PKCS#1) 但内容是 PKCS#8，OpenSSL 解析失败。
- P2：`RandomUtil` 异常消息 `"baseString is empty"` 是英文，与项目中文风格不一致。
- P2：`CollectionUtil` 注释"sort 从小到大"与实现 `reversed()` 相反（实际从大到小）。
- P2：`ReflectUtil.getLambdaFunctionName` 用 `replace("get","")` 删掉方法名里**所有** get。
- 其他见各测试类顶部注释。

- 用户偏好：
  - 错误信息、断言消息、注释一律中文。
  - 只让写测试，不接受子代理顺手改 `src/main`。

### 提交情况

- `da87cb0` 补齐全量测试（由子代理提交，含 `.agent/` 记忆文件）
- `e80dd15` 测试去除 Locale / 字段顺序依赖
- 尚未 push，`origin/dev` 落后本地 16 个提交，等用户确认。
