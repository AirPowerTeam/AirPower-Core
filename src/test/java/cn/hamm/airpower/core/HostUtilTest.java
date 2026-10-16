package cn.hamm.airpower.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>HostUtil 单元测试</h1>
 *
 * <p>主机名按 {@code InetAddress} → {@code hostname} 系统属性 → 环境变量逐级降级，
 * 三个来源都与运行环境相关，测试只能锁定源码自身保证的契约：</p>
 * <ul>
 *     <li>调用不抛异常（{@code UnknownHostException} 与安全策略异常都在内部吞掉）</li>
 *     <li>返回 {@code null} 或含有效字符，绝不会是空白串</li>
 *     <li>只解析一次并缓存，多次调用返回同一个实例</li>
 * </ul>
 *
 * @author Hamm.cn
 */
@DisplayName("HostUtil 主机名工具类单元测试")
class HostUtilTest {

    @Nested
    @DisplayName("getHostName 获取主机名")
    class GetHostNameTest {

        @Test
        @DisplayName("正常路径：不应抛出异常")
        void testGetHostNameDoesNotThrow() {
            assertDoesNotThrow(HostUtil::getHostName,
                    "getHostName 内部已兜底处理 UnknownHostException，不应向外抛出");
        }

        @Test
        @DisplayName("返回值要么为 null，要么含有效字符")
        void testHostNameIsNeverBlank() {
            String hostName = HostUtil.getHostName();
            // 三个来源全部取不到时按契约返回 null；但凡返回就必然过了 isValidHostname 校验，
            // 不可能是空白串
            if (hostName != null) {
                assertTrue(StringUtil.hasText(hostName), "返回的主机名不应为空白串，实际为：" + hostName);
            }
        }

        @Test
        @DisplayName("稳定性：多次调用返回同一个缓存实例")
        void testGetHostNameIsCached() {
            String first = HostUtil.getHostName();
            String second = HostUtil.getHostName();
            assertSame(first, second,
                    "主机名只解析一次并缓存：每次调用重走一遍降级链会拖慢所有打点调用");
        }

        @Test
        @DisplayName("并发安全性：多线程并发调用均不抛异常且结果一致")
        void testGetHostNameIsThreadSafe() throws InterruptedException {
            String expected = HostUtil.getHostName();
            String[] actual = new String[4];
            Thread[] threads = new Thread[actual.length];
            for (int i = 0; i < threads.length; i++) {
                int index = i;
                threads[i] = new Thread(() -> actual[index] = HostUtil.getHostName(), "host-util-test-" + i);
                threads[i].setDaemon(true);
                threads[i].start();
            }
            for (Thread thread : threads) {
                thread.join(2000L);
            }
            for (String value : actual) {
                assertSame(expected, value, "并发读取必须命中同一份缓存，不得重复解析");
            }
        }
    }

    @Nested
    @DisplayName("工具类约束")
    class UtilContractTest {

        @Test
        @DisplayName("当前未限制实例化：保留默认公有构造器行为")
        void testDefaultConstructorIsPublic() {
            // 与其它工具类不同，HostUtil 未声明私有构造器，这里记录当前实现的实际行为
            assertNotNull(new HostUtil(), "HostUtil 当前使用编译器生成的默认公有构造器");
        }
    }
}
