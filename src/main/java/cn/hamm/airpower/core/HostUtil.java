package cn.hamm.airpower.core;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * <h1>Host 工具类</h1>
 *
 * @author Hamm.cn
 */
@Slf4j
public class HostUtil {
    /**
     * 主机名缓存
     */
    private static volatile String cachedHostName;

    /**
     * 是否已缓存过（含「查到 null」这一结果）
     */
    private static volatile boolean hostNameResolved = false;

    /**
     * 获取服务器主机名
     *
     * @return 主机名，所有来源都取不到时为 {@code null}
     * @apiNote 容器里 {@code InetAddress} 常解析不出主机名，因此按
     * {@code InetAddress} → {@code hostname} 系统属性 → 环境变量的顺序逐级降级
     */
    public static @Nullable String getHostName() {
        if (hostNameResolved) {
            return cachedHostName;
        }
        synchronized (HostUtil.class) {
            if (hostNameResolved) {
                return cachedHostName;
            }
            cachedHostName = resolveHostName();
            hostNameResolved = true;
            return cachedHostName;
        }
    }

    /**
     * 实际执行一次主机名查找
     *
     * @return 主机名
     */
    private static @Nullable String resolveHostName() {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            if (isValidHostname(hostname)) {
                return hostname;
            }
        } catch (UnknownHostException e) {
            // 解析失败不代表没有主机名，继续尝试其他来源
            log.debug("通过 InetAddress 获取主机名失败, {}", e.getMessage());
        } catch (SecurityException e) {
            // 安全策略可能禁止读取网络配置，同样降级到后续来源
            log.debug("读取主机名被安全策略拦截, {}", e.getMessage());
        }

        String hostname = System.getProperty("hostname");
        if (isValidHostname(hostname)) {
            return hostname;
        }
        return getHostnameFromEnvironment();
    }

    /**
     * 从环境变量获取主机名
     *
     * @return 主机名，两个环境变量都没有时为 {@code null}
     */
    private static @Nullable String getHostnameFromEnvironment() {
        // Windows
        String hostname = System.getenv("COMPUTERNAME");
        if (isValidHostname(hostname)) {
            return hostname;
        }
        // Linux/Unix/Mac/Docker
        hostname = System.getenv("HOSTNAME");
        if (isValidHostname(hostname)) {
            return hostname;
        }
        return null;
    }

    /**
     * 验证主机名是否有效
     *
     * @param hostname 主机名
     * @return 非 {@code null} 且非空白字符串
     */
    @Contract("null -> false")
    private static boolean isValidHostname(@Nullable String hostname) {
        return hostname != null && !hostname.trim().isEmpty();
    }
}
