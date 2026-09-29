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
     * 获取服务器主机名的完整方法
     */
    public static @Nullable String getHostName() {
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

        // 尝试系统属性
        String hostname = System.getProperty("hostname");
        if (isValidHostname(hostname)) {
            return hostname;
        }
        return getHostnameFromEnvironment();
    }

    /**
     * 从环境变量获取主机名
     */
    private static @Nullable String getHostnameFromEnvironment() {
        // Windows
        String hostname = System.getenv("COMPUTERNAME");
        if (isValidHostname(hostname)) {
            return hostname;
        }
        // Linux/Unix/Mac Docker
        hostname = System.getenv("HOSTNAME");
        if (isValidHostname(hostname)) {
            return hostname;
        }
        return null;
    }

    /**
     * 验证主机名是否有效
     */
    @Contract("null -> false")
    private static boolean isValidHostname(@Nullable String hostname) {
        return hostname != null && !hostname.trim().isEmpty();
    }
}
