package cn.hamm.airpower.core.constant;

import org.jetbrains.annotations.Contract;

/**
 * <h1>HTTP 常量</h1>
 *
 * @author Hamm.cn
 */
public class HttpConstant {
    /**
     * 回环地址
     */
    public static final String LOCAL_IP_ADDRESS = "127.0.0.1";

    /**
     * 本机主机名
     */
    public static final String LOCAL_HOST = "localhost";

    /**
     * 禁止外部实例化
     */
    @Contract(pure = true)
    private HttpConstant() {
    }

    /**
     * 响应状态码
     */
    public static final class Status {
        /**
         * 成功
         */
        public static final int OK = 200;

        /**
         * 服务器内部错误
         */
        public static final int INTERNAL_SERVER_ERROR = 500;

        @Contract(pure = true)
        private Status() {
        }
    }

    /**
     * 授权类型
     */
    public static final class GrantType {
        /**
         * 密码模式
         */
        public static final String PASSWORD = "password";

        /**
         * 刷新令牌
         */
        public static final String REFRESH_TOKEN = "refresh_token";

        /**
         * 客户端凭证模式
         */
        public static final String CLIENT_CREDENTIALS = "client_credentials";

        /**
         * 授权码模式
         */
        public static final String AUTHORIZATION_CODE = "authorization_code";

        /**
         * 隐式模式
         */
        public static final String IMPLICIT = "implicit";

        /**
         * {@code Bearer} 令牌前缀
         */
        public static final String BEARER = "Bearer";

        /**
         * {@code Basic} 认证前缀
         */
        public static final String BASIC = "Basic";

        @Contract(pure = true)
        private GrantType() {
        }
    }

    /**
     * 请求头
     */
    public static final class Header {
        /**
         * 内容类型
         */
        public static final String CONTENT_TYPE = "Content-Type";

        /**
         * Cookie
         */
        public static final String COOKIE = "Cookie";

        /**
         * 请求 ID
         */
        public static final String REQUEST_ID = "X-Request-ID";

        /**
         * 链路追踪 ID，写入 MDC
         */
        public static final String TRACE_ID = "X-Trace-ID";

        /**
         * 认证令牌
         */
        public static final String AUTHORIZATION = "Authorization";

        /**
         * 客户端标识
         */
        public static final String USER_AGENT = "User-Agent";

        @Contract(pure = true)
        private Header() {
        }
    }

    /**
     * 内容类型
     */
    public static final class ContentType {
        /**
         * JSON
         */
        public static final String APPLICATION_JSON = "application/json";

        /**
         * 带 UTF-8 字符集的 JSON
         */
        public static final String APPLICATION_JSON_UTF8 = "application/json;charset=UTF-8";

        /**
         * 表单
         */
        public static final String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";

        /**
         * 多段表单（文件上传）
         */
        public static final String MULTIPART_FORM_DATA = "multipart/form-data";

        /**
         * HTML
         */
        public static final String TEXT_HTML = "text/html";

        /**
         * 纯文本
         */
        public static final String TEXT_PLAIN = "text/plain";

        @Contract(pure = true)
        private ContentType() {
        }
    }

    /**
     * 代理
     */
    public static final class Proxy {
        @Contract(pure = true)
        private Proxy() {
        }

        /**
         * 代理头
         *
         * @apiNote 各家网关（阿里云 / 腾讯云 / F5 等）把真实客户端 IP 放在不同的头里，
         * 反向代理场景需要按实际部署逐个尝试，不能只认一个
         */
        public static final class Header {
            /**
             * {@code RFC 7239} 标准代理头
             */
            public static final String FORWARD = "Forwarded";

            /**
             * Nginx 真实 IP
             */
            public static final String X_REAL_IP = "X-Real-IP";

            /**
             * 代理链，最左侧为最初的客户端
             */
            public static final String X_FORWARDED_FOR = "X-Forwarded-For";

            /**
             * 代理厂商自定义的真实 IP
             */
            public static final String PROXY_CLIENT_IP = "Proxy-Client-IP";

            /**
             * 阿里云 SLB / WAF 的真实 IP
             */
            public static final String WL_PROXY_CLIENT_IP = "WL-Proxy-Client-IP";

            /**
             * 部分硬件负载均衡的变体（头名用下划线）
             */
            public static final String HTTP_CLIENT_IP = "HTTP_CLIENT_IP";

            /**
             * 部分硬件负载均衡的变体（头名用下划线）
             */
            public static final String HTTP_X_FORWARDED_FOR = "HTTP_X_FORWARDED_FOR";

            @Contract(pure = true)
            private Header() {
            }
        }
    }
}
