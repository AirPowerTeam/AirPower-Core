package cn.hamm.airpower.core.constant;

import org.jetbrains.annotations.Contract;

/**
 * <h1>HTTP 常量</h1>
 *
 * @author Hamm.cn
 */
public class HttpConstant {
    public static final String LOCAL_IP_ADDRESS = "127.0.0.1";

    public static final String LOCAL_HOST = "localhost";

    /**
     * 禁止外部实例化
     */
    @Contract(pure = true)
    private HttpConstant() {
    }

    public static final class Status {
        public static final int OK = 200;
        public static final int INTERNAL_SERVER_ERROR = 500;

        @Contract(pure = true)
        private Status() {
        }
    }

    /**
     * 授权类型
     */
    public static final class GrantType {
        public static final String PASSWORD = "password";
        public static final String REFRESH_TOKEN = "refresh_token";
        public static final String CLIENT_CREDENTIALS = "client_credentials";
        public static final String AUTHORIZATION_CODE = "authorization_code";
        public static final String IMPLICIT = "implicit";
        public static final String BEARER = "Bearer";
        public static final String BASIC = "Basic";

        @Contract(pure = true)
        private GrantType() {
        }
    }

    /**
     * 请求头
     */
    public static final class Header {
        public static final String CONTENT_TYPE = "Content-Type";
        public static final String COOKIE = "Cookie";
        public static final String REQUEST_ID = "X-Request-ID";
        public static final String TRACE_ID = "X-Trace-ID";
        public static final String AUTHORIZATION = "Authorization";
        public static final String USER_AGENT = "User-Agent";

        @Contract(pure = true)
        private Header() {
        }
    }

    /**
     * 内容类型
     */
    public static final class ContentType {
        public static final String APPLICATION_JSON = "application/json";
        public static final String APPLICATION_JSON_UTF8 = "application/json;charset=UTF-8";
        public static final String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
        public static final String MULTIPART_FORM_DATA = "multipart/form-data";
        public static final String TEXT_HTML = "text/html";
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
         */
        public static final class Header {
            public static final String FORWARD = "Forwarded";
            public static final String X_REAL_IP = "X-Real-IP";
            public static final String X_FORWARDED_FOR = "X-Forwarded-For";
            public static final String PROXY_CLIENT_IP = "Proxy-Client-IP";
            public static final String WL_PROXY_CLIENT_IP = "WL-Proxy-Client-IP";
            public static final String HTTP_CLIENT_IP = "HTTP_CLIENT_IP";
            public static final String HTTP_X_FORWARDED_FOR = "HTTP_X_FORWARDED_FOR";

            @Contract(pure = true)
            private Header() {
            }
        }
    }
}
