# CHANGELOG

本文件记录 AirPower-Core 的缺陷修复明细，与 [ISSUE.md](./ISSUE.md) 的问题编号一一对应。

---

## [7.0.1] — 全局缺陷修复

基线 `7.0.0`（`dev` @ `23d52c8`），分支 `fix/global-defect-scan`。
共修复 41 个缺陷，全部补充回归用例。

### 修复清单（待逐批填充）

| 编号 | 文件 | 修复内容 | 状态 |
|------|------|----------|------|
| P0-1 | `RootModel` | 嵌套模型与模型集合脱敏失效 | 待修复 |
| P0-2 | `ReflectUtil` | `setAccessible(false)` 导致并发崩溃 | 待修复 |
| P0-3 | `ReflectUtil` | `setFieldValue` 静默吞异常 | 待修复 |
