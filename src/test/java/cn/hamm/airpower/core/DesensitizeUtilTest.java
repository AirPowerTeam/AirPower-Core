package cn.hamm.airpower.core;

import cn.hamm.airpower.core.enums.DesensitizeType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>DesensitizeUtil 单元测试</h1>
 *
 * @author Hamm.cn
 */
class DesensitizeUtilTest {

    // ==================== replace 方法测试 ====================

    @Test
    void testReplaceWithNormalCase() {
        String result = DesensitizeUtil.replace("hello", 1, 1, "*");
        assertEquals("h***o", result);
    }

    @Test
    void testReplaceWithMultipleSymbol() {
        String result = DesensitizeUtil.replace("hello", 1, 1, "#");
        assertEquals("h###o", result);
    }

    @Test
    void testReplaceWithHeadZero() {
        String result = DesensitizeUtil.replace("hello", 0, 1, "*");
        assertEquals("****o", result);
    }

    @Test
    void testReplaceWithTailZero() {
        String result = DesensitizeUtil.replace("hello", 1, 0, "*");
        assertEquals("h****", result);
    }

    @Test
    void testReplaceWithHeadNegative() {
        String result = DesensitizeUtil.replace("hello", -1, 1, "*");
        assertEquals("hello", result);
    }

    @Test
    void testReplaceWithTailNegative() {
        String result = DesensitizeUtil.replace("hello", 1, -1, "*");
        assertEquals("hello", result);
    }

    @Test
    void testReplaceWithHeadPlusTailEqualsLength() {
        String result = DesensitizeUtil.replace("hello", 2, 3, "*");
        assertEquals("hello", result);
    }

    @Test
    void testReplaceWithHeadPlusTailGreaterThanLength() {
        String result = DesensitizeUtil.replace("hello", 3, 3, "*");
        assertEquals("hello", result);
    }

    @Test
    void testReplaceWithEmptyString() {
        String result = DesensitizeUtil.replace("", 1, 1, "*");
        assertEquals("", result);
    }

    @Test
    void testReplaceWithSingleCharacter() {
        String result = DesensitizeUtil.replace("a", 1, 1, "*");
        assertEquals("a", result);
    }

    @Test
    void testReplaceWithTwoCharacters() {
        String result = DesensitizeUtil.replace("ab", 1, 1, "*");
        assertEquals("ab", result);
    }

    @Test
    void testReplaceWithSymbolLongerThanOne() {
        String result = DesensitizeUtil.replace("hello", 1, 1, "**");
        assertEquals("h******o", result);
    }

    // ==================== desensitizeIpv4Address 方法测试 ====================

    @Test
    void testDesensitizeIpv4AddressWithDefaultSymbol() {
        String result = DesensitizeUtil.desensitizeIpv4Address("192.168.1.1");
        assertEquals("192.***.***.1", result);
    }

    @Test
    void testDesensitizeIpv4AddressWithCustomSymbol() {
        String result = DesensitizeUtil.desensitizeIpv4Address("192.168.1.1", "#");
        assertEquals("192.###.###.1", result);
    }

    @Test
    void testDesensitizeIpv4AddressWithNullSymbol() {
        String result = DesensitizeUtil.desensitizeIpv4Address("192.168.1.1", null);
        assertEquals("192.***.***.1", result);
    }

    @Test
    void testDesensitizeIpv4AddressWithEmptySymbol() {
        String result = DesensitizeUtil.desensitizeIpv4Address("192.168.1.1", "");
        assertEquals("192.***.***.1", result);
    }

    @Test
    void testDesensitizeIpv4AddressWithInvalidFormat() {
        String result = DesensitizeUtil.desensitizeIpv4Address("192.168.1", "*");
        assertEquals("192.168.1", result);
    }

    @Test
    void testDesensitizeIpv4AddressWithExtraParts() {
        String result = DesensitizeUtil.desensitizeIpv4Address("192.168.1.1.1", "*");
        assertEquals("192.168.1.1.1", result);
    }

    @Test
    void testDesensitizeIpv4AddressWithEmptyString() {
        String result = DesensitizeUtil.desensitizeIpv4Address("", "*");
        assertEquals("", result);
    }

    // ==================== desensitize 方法测试 - 手机号 ====================

    @Test
    void testDesensitizeMobile() {
        String result = DesensitizeUtil.desensitize("13800138000", DesensitizeType.MOBILE, 0, 0, "*");
        assertEquals("138****8000", result);
    }

    @Test
    void testDesensitizeMobileWithCustomHeadTail() {
        String result = DesensitizeUtil.desensitize("13800138000", DesensitizeType.MOBILE, 5, 5, "*");
        // 手机号最小保留3+4，所以传入5+5时，实际保留5+5，但中间只有1位
        assertEquals("13800*38000", result);
    }

    // ==================== desensitize 方法测试 - 身份证号 ====================

    @Test
    void testDesensitizeIdCard() {
        String result = DesensitizeUtil.desensitize("110101199001011234", DesensitizeType.ID_CARD, 0, 0, "*");
        assertEquals("110101********1234", result);
    }

    // ==================== desensitize 方法测试 - 银行卡号 ====================

    @Test
    void testDesensitizeBankCard() {
        String result = DesensitizeUtil.desensitize("6222021234567890123", DesensitizeType.BANK_CARD, 0, 0, "*");
        // 银行卡最小保留4+4=8，长度19，中间11位
        assertEquals("6222***********0123", result);
    }

    // ==================== desensitize 方法测试 - 邮箱 ====================

    @Test
    void testDesensitizeEmail() {
        String result = DesensitizeUtil.desensitize("test@example.com", DesensitizeType.EMAIL, 0, 0, "*");
        // 邮箱最小保留2+2=4，长度16，中间12位
        assertEquals("te************om", result);
    }

    // ==================== desensitize 方法测试 - 地址 ====================

    @Test
    void testDesensitizeAddress() {
        String result = DesensitizeUtil.desensitize("北京市海淀区xxx街道", DesensitizeType.ADDRESS, 0, 0, "*");
        // 地址最小保留3+0=3，长度11，中间8位
        assertEquals("北京市********", result);
    }

    // ==================== desensitize 方法测试 - 车牌号 ====================

    @Test
    void testDesensitizeCarNumber() {
        String result = DesensitizeUtil.desensitize("京A12345", DesensitizeType.CAR_NUMBER, 0, 0, "*");
        // 车牌号最小保留2+1=3，长度7，中间4位
        assertEquals("京A****5", result);
    }

    // ==================== desensitize 方法测试 - IPv4 ====================

    @Test
    void testDesensitizeIpv4() {
        String result = DesensitizeUtil.desensitize("192.168.1.1", DesensitizeType.IP_V4, 0, 0, "*");
        assertEquals("192.***.***.1", result);
    }

    // ==================== desensitize 方法测试 - 中文名 ====================

    @Test
    void testDesensitizeChineseNameWithTwoCharacters() {
        String result = DesensitizeUtil.desensitize("张三", DesensitizeType.CHINESE_NAME, 0, 0, "*");
        assertEquals("张*", result);
    }

    @Test
    void testDesensitizeChineseNameWithThreeCharacters() {
        String result = DesensitizeUtil.desensitize("张三丰", DesensitizeType.CHINESE_NAME, 0, 0, "*");
        assertEquals("张*丰", result);
    }

    @Test
    void testDesensitizeChineseNameWithSingleCharacter() {
        String result = DesensitizeUtil.desensitize("张", DesensitizeType.CHINESE_NAME, 0, 0, "*");
        assertEquals("张", result);
    }

    // ==================== desensitize 方法测试 - 座机号码 ====================

    @Test
    void testDesensitizeTelephoneWithRegionCode() {
        String result = DesensitizeUtil.desensitize("010-12345678", DesensitizeType.TELEPHONE, 0, 0, "*");
        assertEquals("010-****5678", result);
    }

    @Test
    void testDesensitizeTelephoneWithoutRegionCode() {
        String result = DesensitizeUtil.desensitize("123456", DesensitizeType.TELEPHONE, 0, 0, "*");
        assertEquals("12**56", result);
    }

    // ==================== desensitize 方法测试 - 自定义类型 ====================

    @Test
    void testDesensitizeCustom() {
        String result = DesensitizeUtil.desensitize("hello", DesensitizeType.CUSTOM, 1, 1, "*");
        assertEquals("h***o", result);
    }

    // ==================== desensitize 方法测试 - 重载方法 ====================

    @Test
    void testDesensitizeWithDefaultSymbol() {
        String result = DesensitizeUtil.desensitize("hello", DesensitizeType.CUSTOM, 1, 1);
        assertEquals("h***o", result);
    }

    // ==================== desensitize 方法测试 - 边界情况 ====================

    @Test
    void testDesensitizeWithEmptyString() {
        String result = DesensitizeUtil.desensitize("", DesensitizeType.CUSTOM, 1, 1, "*");
        assertEquals("", result);
    }

    @Test
    void testDesensitizeWithHeadGreaterThanLength() {
        String result = DesensitizeUtil.desensitize("ab", DesensitizeType.CUSTOM, 5, 5, "*");
        assertEquals("ab", result);
    }
}
