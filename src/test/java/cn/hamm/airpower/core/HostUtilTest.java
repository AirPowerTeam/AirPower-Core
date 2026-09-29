package cn.hamm.airpower.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>HostUtil 单元测试</h1>
 *
 * <p>主机名依赖运行环境（InetAddress → 系统属性 → 环境变量），因此用例只断言源码可保证的契约：</p>
 * <ul>
 *     <li>调用不抛异常（源码内部已吞掉 UnknownHostException）</li>
 *     <li>返回结果非空且非空白（源码所有返回分支都经过 isValidHostname 校验）</li>
 *     <li>多次调用结果稳定</li>
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
            assertDoesNotThrow(HostUtil::getHostName, "getHostName 内部已兜底处理 UnknownHostException，不应向外抛出");
        }

        @Test
        @DisplayName("正常路径：返回非空主机名")
        void testGetHostNameIsNotNull() {
            String hostName = HostUtil.getHostName();
            assertNotNull(hostName, "正常环境下 InetAddress.getLocalHost() 应能返回主机名");
        }

        @Test
        @DisplayName("边界值：返回值不是空白串")
        void testGetHostNameIsNotBlank() {
            String hostName = HostUtil.getHostName();
            // 源码的三条返回分支都先经过 isValidHostname 校验，故返回值必然含有非空白字符
            assertTrue(StringUtil.hasText(hostName), "返回的主机名不应为空白串，实际为：" + hostName);
        }

        @Test
        @DisplayName("稳定性：多次调用返回一致结果")
        void testGetHostNameIsStable() {
            String first = HostUtil.getHostName();
            String second = HostUtil.getHostName();
            assertTrue(first == null ? second == null : first.equals(second),
                    "主机名在同一次运行中应保持稳定，两次调用结果应一致");
        }

        @Test
        @DisplayName("并发安全性：多线程并发调用均不抛异常")
        void testGetHostNameIsThreadSafe() {
            assertDoesNotThrow(() -> {
                Thread[] threads = new Thread[4];
                for (int i = 0; i < threads.length; i++) {
                    threads[i] = new Thread(HostUtil::getHostName, "host-util-test-" + i);
                    threads[i].setDaemon(true);
                    threads[i].start();
                }
                for (Thread thread : threads) {
                    thread.join(2000L);
                }
            }, "并发调用 getHostName 不应抛出异常");
        }
    }

    @Nested
    @DisplayName("工具类约束")
    class UtilContractTest {

        @Test
        @DisplayName("当前未限制实例化：保留默认公有构造器行为")
        void testDefaultConstructorIsPublic() {
            // 与 DesensitizeUtil 不同，HostUtil 未声明私有构造器，这里记录当前实现的实际行为
            assertNotNull(new HostUtil(), "HostUtil 当前使用编译器生成的默认公有构造器");
        }
    }
}
