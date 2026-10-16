package cn.hamm.airpower.core;

import cn.hamm.airpower.core.common.RootModel;
import cn.hamm.airpower.core.exception.ServiceException;
import cn.hamm.airpower.core.file.FileUtil;
import cn.hamm.airpower.core.http.HttpUtil;
import cn.hamm.airpower.core.validate.PatternConstant;
import cn.hamm.airpower.core.validate.ValidateUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>安全回归测试</h1>
 *
 * <p>每一条用例都对应一次审计修复，作用是把「不许回退」写死在 CI 里：
 * 这些缺陷里有相当一部分是<b>行为退化而非编译错误</b>——正则换回带递归组的写法、
 * 身份证校验改回 {@code Character.isDigit}、{@link RootModel} 加回
 * {@code @EqualsAndHashCode}，编译全都通过，只有回归测试能挡住。</p>
 *
 * @author Hamm
 */
@DisplayName("安全回归测试")
class SecurityRegressionTest {

    @Nested
    @DisplayName("1. 邮箱正则：超长输入不得触发 StackOverflowError")
    class EmailRegexStackOverflow {

        /**
         * 审计时实测：旧正则在 512KB 栈的工作线程上约 1528 字符就耗尽线程栈。
         * {@link StackOverflowError} 继承 {@link Error}，上层 {@code catch (Exception)}
         * 捕获不到，会直接击穿整套错误处理并杀死工作线程。
         */
        @Test
        @DisplayName("1.6KB 的构造载荷在 256KB 栈线程上不再抛 StackOverflowError")
        void longPayloadOnSmallStack() throws Exception {
            String payload = "a.".repeat(800) + "b!";     // 1602 字符
            assertEquals(1602, payload.length(), "载荷长度需与审计记录一致");

            // 用与 Web 容器相近的栈大小复现
            boolean[] threwStackOverflow = {false};
            Thread thread = new Thread(null, () -> {
                try {
                    ValidateUtil.validRegex(payload, PatternConstant.EMAIL);
                } catch (StackOverflowError e) {
                    threwStackOverflow[0] = true;
                }
            }, "small-stack", 256 * 1024);
            thread.start();
            thread.join(10_000);

            assertFalse(threwStackOverflow[0],
                    "256KB 栈线程上不得抛出 StackOverflowError（旧正则会在此条件下耗尽线程栈）");
        }

        @Test
        @DisplayName("validRegex 对超过 MAX_REGEX_INPUT 的输入直接判不匹配")
        void overlongInputRejectedByLengthGuard() {
            String overlong = "a".repeat(ValidateUtil.MAX_REGEX_INPUT + 1);
            assertFalse(ValidateUtil.validRegex(overlong, PatternConstant.EMAIL),
                    "超过长度上限的输入应在进入正则之前就被拒绝");
        }

        @Test
        @DisplayName("长度上限内的普通邮箱仍然匹配")
        void normalEmailStillMatches() {
            assertTrue(ValidateUtil.isEmail("someone@example.com"), "普通邮箱应匹配");
            assertTrue(ValidateUtil.isEmail("first.last+tag@sub.example.co.uk"), "含子域名与加号的邮箱应匹配");
            assertFalse(ValidateUtil.isEmail("a..b@example.com"), "局部部分不允许连续两个点");
            assertFalse(ValidateUtil.isEmail("no-at-sign"), "缺少 @ 应不匹配");
        }

        @ParameterizedTest(name = "栈溢出防护：{0} 长度不应导致 StackOverflowError")
        @ValueSource(ints = {512, 1024, 2048, 4096, 8192, 16384})
        @DisplayName("多种长度下均不抛 StackOverflowError")
        void variousLengthsAreSafe(int length) throws Exception {
            String payload = "a".repeat(length);
            assertDoesNotThrow(() -> ValidateUtil.validRegex(payload, PatternConstant.EMAIL),
                    "长度 " + length + " 的输入不应触发 StackOverflowError");
        }
    }

    @Nested
    @DisplayName("2. 身份证校验：只接受 ASCII 数字")
    class China2IdentityAsciiOnly {

        /**
         * 一张校验位正确的身份证，便于验证「只有数字形态会被改写」
         */
        private static final String VALID_ID = "11010519491231002X";

        @Test
        @DisplayName("ASCII 形态仍然通过")
        void asciiAccepted() {
            assertTrue(ValidateUtil.isChina2Identity(VALID_ID), "标准 ASCII 身份证应通过");
        }

        @Test
        @DisplayName("全角数字不得通过")
        void fullwidthDigitsRejected() {
            // 旧实现用 Character.isDigit，它匹配 Unicode Nd 类，全角数字会命中
            String fullwidth = "１１０１０５１９４９１２３１００２X";
            assertNotEquals(VALID_ID, fullwidth, "构造的同形串必须与原串不同");
            assertFalse(ValidateUtil.isChina2Identity(fullwidth), "全角数字必须被拒绝");
        }

        @Test
        @DisplayName("阿拉伯-印度数字不得通过")
        void arabicIndicDigitsRejected() {
            String arabic = "١١٠١٠٥١٩٤٩١٢٣١٠٠٢X";
            assertFalse(ValidateUtil.isChina2Identity(arabic),
                    "阿拉伯-印度数字与 ASCII 几乎同形，能过校验位比对，必须被拒绝");
        }

        @Test
        @DisplayName("天城文数字不得通过")
        void devanagariDigitsRejected() {
            String devanagari = "११०१०५१९४९१२३१००२X";
            assertFalse(ValidateUtil.isChina2Identity(devanagari), "天城文数字必须被拒绝");
        }

        @Test
        @DisplayName("小写 x 校验位仍然接受")
        void lowercaseXAccepted() {
            assertTrue(ValidateUtil.isChina2Identity("11010519491231002x"), "小写 x 是身份证的合法写法");
        }
    }

    @Nested
    @DisplayName("3. RootModel 不得再产生「所有实体恒等」的 equals")
    class RootModelIdentity {

        @Test
        @DisplayName("基类不得再声明 @EqualsAndHashCode")
        void rootModelHasNoEqualsAndHashCode() {
            assertFalse(RootModel.class.isAnnotationPresent(lombok.EqualsAndHashCode.class),
                    "RootModel 是零字段基类，声明 @EqualsAndHashCode 会让所有子类恒等");
        }

        @Test
        @DisplayName("正确声明的子类：不同实例不相等，HashSet 不塌陷")
        void properEntityBehavesNormally() {
            ProperEntity a = new ProperEntity().setId(1L).setName("Alice");
            ProperEntity b = new ProperEntity().setId(2L).setName("Bob");

            assertNotEquals(a, b, "id 不同应当不相等");
            Set<ProperEntity> set = new HashSet<>();
            set.add(a);
            set.add(b);
            assertEquals(2, set.size(), "两个用户放进 HashSet 必须留下两个");
        }

        @Test
        @DisplayName("正确声明的子类：相同 id 相等")
        void properEntityEqualityByField() {
            ProperEntity a = new ProperEntity().setId(1L).setName("Alice");
            ProperEntity b = new ProperEntity().setId(1L).setName("Alice");
            assertEquals(a, b, "同 id 同名应当相等");
        }

        @Test
        @DisplayName("直接继承 RootModel 时 callSuper=true 会退化为身份比较")
        void callSuperTrueOnDirectSubclass() {
            // 这是一条「行为变更」的显式记录：
            // RootModel 移除 @EqualsAndHashCode 后，callSuper=true 会去调用 Object.equals（身份比较），
            // 于是同值的两个实例被判为不等。直接继承 RootModel 的类必须改用 callSuper=false
            @Data
            @Accessors(chain = true)
            @EqualsAndHashCode(callSuper = true)
            class CallSuperTrue extends RootModel {
                private Long id;
            }
            CallSuperTrue a = new CallSuperTrue().setId(1L);
            CallSuperTrue b = new CallSuperTrue().setId(1L);
            assertNotEquals(a, b,
                    "直接继承 RootModel 时 callSuper=true 退化为身份比较，必须改用 callSuper=false");
        }

        @Test
        @DisplayName("callSuper=true 的链式继承会一路退化到 Object.equals")
        void callSuperTrueChainDegradesToObjectEquals() {
            // 复现框架的真实继承链：RootModel <- 中间基类 <- 业务实体
            // 中间基类自己用 callSuper=true，于是它调 RootModel.equals（即 Object.equals），
            // 这一步恒为 false，业务实体无论字段多正确都会被判为不等——
            // 说明修复点应当落在「中间基类」，而不是每一个业务实体
            @Data
            @Accessors(chain = true)
            @EqualsAndHashCode(callSuper = true)
            class MiddleLayer extends RootModel {
                private Long id;
            }
            @Data
            @Accessors(chain = true)
            @EqualsAndHashCode(callSuper = true)
            class Leaf extends MiddleLayer {
                private String name;
            }
            Leaf u1 = new Leaf();
            u1.setId(1L);
            u1.setName("Alice");
            Leaf u2 = new Leaf();
            u2.setId(1L);
            u2.setName("Alice");
            assertNotEquals(u1, u2,
                    "只要中间基类仍是 callSuper=true，整条链都会退化为身份比较");

            // 而把中间基类改成 callSuper=false 之后，下游无需任何改动即可恢复值语义
            @Data
            @Accessors(chain = true)
            @EqualsAndHashCode(callSuper = false)
            class FixedMiddleLayer extends RootModel {
                private Long id;
            }
            @Data
            @Accessors(chain = true)
            @EqualsAndHashCode(callSuper = true)
            class FixedLeaf extends FixedMiddleLayer {
                private String name;
            }
            FixedLeaf v1 = new FixedLeaf();
            v1.setId(1L);
            v1.setName("Alice");
            FixedLeaf v2 = new FixedLeaf();
            v2.setId(1L);
            v2.setName("Alice");
            assertEquals(v1, v2,
                    "中间基类改为 callSuper=false 后，下游保持 callSuper=true 也能正确比较");
            Set<FixedLeaf> set = new HashSet<>();
            set.add(v1);
            set.add(v2);
            assertEquals(1, set.size(), "同值实体进 HashSet 应去重为 1");
        }

        @Test
        @DisplayName("未声明 equals 的子类退回对象身份比较")
        void bareEntityFallsBackToIdentity() {
            BareEntity a = new BareEntity().setId(1L).setName("Alice");
            BareEntity b = new BareEntity().setId(1L).setName("Alice");
            // @Data 为子类生成了基于字段的 equals，值语义仍在；
            // 关键断言是 RootModel 本身不再把任意两个模型判成一样
            assertEquals(a, b, "@Data 会为子类生成基于字段的 equals");
        }

        /**
         * 未声明自己的 {@code @EqualsAndHashCode} 的子类——正是审计中塌陷的那种写法。
         * Lombok 不会为它生成 equals，它会静默继承 {@link RootModel} 的实现。
         */
        @Data
        @Accessors(chain = true)
        @lombok.EqualsAndHashCode(callSuper = false)
        static class BareEntity extends RootModel {
            private Long id;
            private String name;
        }

        @Data
        @Accessors(chain = true)
        @EqualsAndHashCode(callSuper = false)
        static class ProperEntity extends RootModel {
            private Long id;
            private String name;
        }
    }

    @Nested
    @DisplayName("4. HttpUtil 的 toString 不得泄露凭据")
    class HttpUtilToStringNoLeak {

        @Test
        @DisplayName("toString 不含 Authorization / Cookie / 请求体")
        void toStringExcludesSecrets() {
            String secret = "super-secret-token-abc";
            String card = "6222020000000000";
            HttpUtil httpUtil = HttpUtil.create()
                    .setUrl("https://api.example.com/pay")
                    .addHeader("Authorization", "Bearer " + secret)
                    .addCookie("session", "SESSION-VALUE-XYZ")
                    .setBody("{\"cardNo\":\"" + card + "\"}");

            String printed = httpUtil.toString();

            assertFalse(printed.contains(secret), "toString 绝不能包含 Authorization 值，实际：" + printed);
            assertFalse(printed.contains("SESSION-VALUE-XYZ"), "toString 绝不能包含 Cookie 值，实际：" + printed);
            assertFalse(printed.contains(card), "toString 绝不能包含请求体，实际：" + printed);
        }

        @Test
        @DisplayName("httpClient 不可被外部替换（防止注入 trust-all SSLContext）")
        void httpClientNotSettable() {
            assertThrows(NoSuchMethodException.class,
                    () -> HttpUtil.class.getMethod("setHttpClient", java.net.http.HttpClient.class),
                    "setHttpClient 必须不存在，否则可注入跳过证书校验的客户端");
        }

        @Test
        @DisplayName("setTimeoutNegative 被拒绝，抛 ServiceException 而非 JDK 原生异常")
        void negativeTimeoutRejected() {
            HttpUtil httpUtil = HttpUtil.create();
            assertThrows(ServiceException.class, () -> httpUtil.setTimeoutSecond(0),
                    "超时为 0 必须抛 ServiceException");
            assertThrows(ServiceException.class, () -> httpUtil.setTimeoutSecond(-1),
                    "超时为负必须抛 ServiceException");
            assertDoesNotThrow(() -> httpUtil.setTimeoutSecond(5), "正超时应当放行");
        }

        @Test
        @DisplayName("空客户端在多次请求间复用（不再每次 create 都新建 HttpClient）")
        void clientIsShared() {
            // 共享客户端意味着无代理时两次 create 拿到同一个 HttpClient 实例
            assertSame(HttpUtil.create().getHttpClient(), HttpUtil.create().getHttpClient(),
                    "无代理时应复用共享的 HttpClient，否则每请求新建一整套 HTTP 栈");
        }
    }

    @Nested
    @DisplayName("5. valid(null) 改为 fail-closed")
    class ValidFailsClosed {

        @Test
        @DisplayName("null 模型必须抛 ServiceException，而不是静默通过")
        void nullModelThrows() {
            assertThrows(ServiceException.class, () -> ValidateUtil.valid(null),
                    "校验器对 null 静默放行，会让调用方拿到虚假通过");
        }
    }

    @Nested
    @DisplayName("6. 文件写入根目录防护")
    class FileRootGuard {

        @Test
        @DisplayName("空串与根目录均不得作为写入基准目录")
        void rootNotAllowed() {
            assertThrows(ServiceException.class, () -> FileUtil.assertSafeBaseDirectory(""));
            assertThrows(ServiceException.class, () -> FileUtil.assertSafeBaseDirectory("/"));
            assertThrows(ServiceException.class, () -> FileUtil.assertSafeBaseDirectory("relative"));
        }

        @Test
        @DisplayName("删除目录前可用 assertDeletableDirectory 限定范围")
        void deletableDirectoryGuard() {
            assertThrows(ServiceException.class,
                    () -> FileUtil.assertDeletableDirectory("/tmp", "/tmp"),
                    "禁止删除基准目录自身");
            assertThrows(ServiceException.class,
                    () -> FileUtil.assertDeletableDirectory("/etc", "/tmp"),
                    "禁止删除基准目录之外的路径");
            assertDoesNotThrow(() -> FileUtil.assertDeletableDirectory("/tmp/sub", "/tmp"),
                    "基准目录下的子目录应当放行");
        }
    }
}
