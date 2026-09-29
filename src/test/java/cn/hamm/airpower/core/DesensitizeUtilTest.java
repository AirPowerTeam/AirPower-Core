package cn.hamm.airpower.core;

import cn.hamm.airpower.core.enums.DesensitizeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>DesensitizeUtil 单元测试</h1>
 *
 * <p>断言严格对齐 {@link DesensitizeUtil} 的实际实现行为，包括各类脱敏类型的最小保留长度抬升逻辑。</p>
 *
 * @author Hamm.cn
 */
@DisplayName("DesensitizeUtil 脱敏工具类单元测试")
class DesensitizeUtilTest {

    @Nested
    @DisplayName("replace 通用字符串替换")
    class ReplaceTest {

        @Test
        @DisplayName("正常路径：保留头部与尾部，中间用符号填充")
        void testReplaceWithHeadAndTail() {
            assertEquals("138****8000", DesensitizeUtil.replace("13800138000", 3, 4, "*"),
                    "脱敏结果应保留前三后四位");
        }

        @Test
        @DisplayName("正常路径：头尾均为 0 时整串被符号替换")
        void testReplaceWithZeroHeadAndTail() {
            assertEquals("*****", DesensitizeUtil.replace("abcde", 0, 0, "*"),
                    "头尾都为 0 时应把全部 5 个字符替换为符号");
        }

        @Test
        @DisplayName("正常路径：只保留头部")
        void testReplaceOnlyKeepHead() {
            assertEquals("abcd**", DesensitizeUtil.replace("abcdef", 4, 0, "*"),
                    "只保留头部 4 位时尾部 2 位应被替换");
        }

        @Test
        @DisplayName("正常路径：只保留尾部")
        void testReplaceOnlyKeepTail() {
            assertEquals("****ef", DesensitizeUtil.replace("abcdef", 0, 2, "*"),
                    "只保留尾部 2 位时头部 4 位应被替换");
        }

        @Test
        @DisplayName("多字符符号：按原长度重复拼接")
        void testReplaceWithMultiCharSymbol() {
            assertEquals("aabababe", DesensitizeUtil.replace("abcde", 1, 1, "ab"),
                    "符号为 ab 时应重复 3 次后再拼接头尾");
        }

        @Test
        @DisplayName("空符号：回退为默认符号而不是清空原文")
        void testReplaceWithEmptySymbol() {
            // 原实现直接拼接空串，中间 3 个字符被静默删除，返回的 "ae" 长度与原文不符
            assertEquals("a***e", DesensitizeUtil.replace("abcde", 1, 1, ""),
                    "符号为空串时回退为默认符号 *，不能把原文静默删掉");
            assertEquals(DesensitizeUtil.replace("abcde", 1, 1, "*"),
                    DesensitizeUtil.replace("abcde", 1, 1, ""),
                    "空符号的结果应与显式传 * 完全一致");
        }

        @Test
        @DisplayName("空串输入：返回空串")
        void testReplaceEmptyText() {
            assertEquals("", DesensitizeUtil.replace("", 0, 0, "*"), "空串输入应返回空串");
        }

        @Test
        @DisplayName("边界值：head + tail 恰好等于长度时整串替换")
        void testReplaceWhenHeadPlusTailEqualsLength() {
            assertEquals("****", DesensitizeUtil.replace("abcd", 2, 2, "*"),
                    "head + tail 等于长度时应整串替换为 4 个符号");
        }

        @Test
        @DisplayName("边界值：head 等于长度时整串替换")
        void testReplaceWhenHeadEqualsLength() {
            assertEquals("***", DesensitizeUtil.replace("abc", 3, 0, "*"),
                    "head 等于长度时应整串替换为 3 个符号");
        }

        @Test
        @DisplayName("边界值：tail 等于长度时整串替换")
        void testReplaceWhenTailEqualsLength() {
            assertEquals("***", DesensitizeUtil.replace("abc", 0, 3, "*"),
                    "tail 等于长度时应整串替换为 3 个符号");
        }

        @Test
        @DisplayName("边界值：head + tail 大于长度时整串替换")
        void testReplaceWhenHeadPlusTailGreaterThanLength() {
            assertEquals("***", DesensitizeUtil.replace("abc", 5, 5, "*"),
                    "head + tail 超过长度时应整串替换为 3 个符号");
        }

        @Test
        @DisplayName("负数分支：head 为负时整串替换")
        void testReplaceWithNegativeHead() {
            assertEquals("***", DesensitizeUtil.replace("abc", -1, 1, "*"),
                    "head 为负数时应整串替换为 3 个符号");
        }

        @Test
        @DisplayName("负数分支：tail 为负时整串替换")
        void testReplaceWithNegativeTail() {
            assertEquals("***", DesensitizeUtil.replace("abc", 1, -1, "*"),
                    "tail 为负数时应整串替换为 3 个符号");
        }

        @Test
        @DisplayName("空串 + 负数 head：仍返回空串")
        void testReplaceEmptyTextWithNegativeHead() {
            assertEquals("", DesensitizeUtil.replace("", -1, -1, "*"),
                    "空串输入叠加负数参数时也应返回空串");
        }

        @Test
        @DisplayName("边界：symbol 为 null 时回退为默认符号")
        void testReplaceWithNullSymbol() {
            assertEquals("a***e", DesensitizeUtil.replace("abcde", 1, 1, null),
                    "symbol 为 null 时回退为默认符号 *，不应抛空指针");
        }

        @Test
        @DisplayName("边界：symbol 为 null 且 head 为负时同样回退为默认符号")
        void testReplaceWithNullSymbolAndNegativeHead() {
            assertEquals("***", DesensitizeUtil.replace("abc", -1, 0, null),
                    "整串替换分支中 symbol 为 null 也应回退为默认符号，重复次数按原文长度");
        }

        @Test
        @DisplayName("异常分支：text 为 null 抛 IllegalArgumentException")
        void testReplaceWithNullText() {
            assertThrows(IllegalArgumentException.class, () -> DesensitizeUtil.replace(null, 1, 1, "*"),
                    "text 为 null 是调用错误，应抛带明确信息的 IllegalArgumentException");
        }
    }

    @Nested
    @DisplayName("desensitizeIpv4Address IPv4 脱敏")
    class DesensitizeIpv4AddressTest {

        @Test
        @DisplayName("正常路径：无参重载保留第一段与最后一段")
        void testIpv4WithoutSymbol() {
            assertEquals("192.***.***.1", DesensitizeUtil.desensitizeIpv4Address("192.168.1.1"),
                    "无参重载应等价于符号为 * 的结果");
        }

        @Test
        @DisplayName("正常路径：广播地址同样只保留首尾两段")
        void testIpv4BroadcastAddress() {
            assertEquals("255.***.***.255", DesensitizeUtil.desensitizeIpv4Address("255.255.255.255"),
                    "广播地址中间两段应被替换为三个符号");
        }

        @Test
        @DisplayName("正常路径：支持自定义符号")
        void testIpv4WithCustomSymbol() {
            assertEquals("192.###.###.1", DesensitizeUtil.desensitizeIpv4Address("192.168.1.1", "#"),
                    "自定义符号 # 时每段应替换为 3 个 #");
        }

        @Test
        @DisplayName("空值分支：symbol 为 null 时回退到默认符号")
        void testIpv4WithNullSymbol() {
            assertEquals("192.***.***.1", DesensitizeUtil.desensitizeIpv4Address("192.168.1.1", null),
                    "symbol 为 null 时应回退到默认符号 *");
        }

        @Test
        @DisplayName("空值分支：symbol 为空串时回退到默认符号")
        void testIpv4WithEmptySymbol() {
            assertEquals("192.***.***.1", DesensitizeUtil.desensitizeIpv4Address("192.168.1.1", ""),
                    "symbol 为空串时应回退到默认符号 *");
        }

        @Test
        @DisplayName("空值分支：symbol 为纯空白时回退到默认符号")
        void testIpv4WithBlankSymbol() {
            assertEquals("192.***.***.1", DesensitizeUtil.desensitizeIpv4Address("192.168.1.1", "   "),
                    "symbol 为纯空白时按 hasText 判定应回退到默认符号 *");
        }

        @Test
        @DisplayName("异常分支：段数为 3 时原样返回")
        void testIpv4WithThreeParts() {
            assertEquals("1.2.3", DesensitizeUtil.desensitizeIpv4Address("1.2.3", "*"),
                    "不是 4 段时应原样返回");
        }

        @Test
        @DisplayName("异常分支：段数为 5 时原样返回")
        void testIpv4WithFiveParts() {
            assertEquals("1.2.3.4.5", DesensitizeUtil.desensitizeIpv4Address("1.2.3.4.5", "*"),
                    "超过 4 段时应原样返回");
        }

        @Test
        @DisplayName("异常分支：非 IP 文本原样返回")
        void testIpv4WithPlainText() {
            assertEquals("abc", DesensitizeUtil.desensitizeIpv4Address("abc"),
                    "不含分隔符的文本应原样返回");
        }

        @Test
        @DisplayName("边界值：空串原样返回")
        void testIpv4WithEmptyText() {
            assertEquals("", DesensitizeUtil.desensitizeIpv4Address(""), "空串应原样返回空串");
        }

        @Test
        @DisplayName("边界值：多个分隔符原样返回")
        void testIpv4WithOnlyDelimiters() {
            assertEquals("...", DesensitizeUtil.desensitizeIpv4Address("..."),
                    "全为分隔符时切分结果不足 4 段，应原样返回");
        }

        @Test
        @DisplayName("边界值：结尾多余分隔符被切分规则忽略")
        void testIpv4WithTrailingDelimiter() {
            assertEquals("1.***.***.4", DesensitizeUtil.desensitizeIpv4Address("1.2.3.4."),
                    "结尾的尾随分隔符会被 split 丢弃，仍按 4 段处理");
        }
    }

    @Nested
    @DisplayName("desensitize 四参重载：按类型抬升最小保留长度")
    class DesensitizeFourArgsTest {

        @Test
        @DisplayName("MOBILE：最少保留前 3 后 4")
        void testMobile() {
            assertEquals("138****8000", DesensitizeUtil.desensitize("13800138000", DesensitizeType.MOBILE, 0, 0),
                    "手机号应抬升到前 3 后 4");
        }

        @Test
        @DisplayName("MOBILE：传入更大保留长度时以传入值为准")
        void testMobileWithLargerHead() {
            assertEquals("13800*38000", DesensitizeUtil.desensitize("13800138000", DesensitizeType.MOBILE, 5, 5),
                    "传入的 head/tail 更大时不应被最小值压低");
        }

        @Test
        @DisplayName("MOBILE：长度不足时整串替换")
        void testMobileTooShort() {
            assertEquals("***", DesensitizeUtil.desensitize("138", DesensitizeType.MOBILE, 0, 0),
                    "手机号过短时 head + tail 超过长度，应整串替换");
        }

        @Test
        @DisplayName("ID_CARD：最少保留前 6 后 4")
        void testIdCard() {
            assertEquals("110101********1234",
                    DesensitizeUtil.desensitize("110101199001011234", DesensitizeType.ID_CARD, 0, 0),
                    "身份证号应抬升到前 6 后 4");
        }

        @Test
        @DisplayName("ID_CARD：长度不足时整串替换")
        void testIdCardTooShort() {
            assertEquals("***", DesensitizeUtil.desensitize("123", DesensitizeType.ID_CARD, 0, 0),
                    "身份证号过短时应整串替换");
        }

        @Test
        @DisplayName("BANK_CARD：最少保留前 4 后 4")
        void testBankCard() {
            assertEquals("6222***********0123",
                    DesensitizeUtil.desensitize("6222021234567890123", DesensitizeType.BANK_CARD, 0, 0),
                    "银行卡号应抬升到前 4 后 4");
        }

        @Test
        @DisplayName("CAR_NUMBER：最少保留前 2 后 1")
        void testCarNumber() {
            assertEquals("京A****5", DesensitizeUtil.desensitize("京A12345", DesensitizeType.CAR_NUMBER, 0, 0),
                    "车牌号应抬升到前 2 后 1");
        }

        @Test
        @DisplayName("EMAIL：最少保留前 2 后 2")
        void testEmail() {
            assertEquals("te************om",
                    DesensitizeUtil.desensitize("test@example.com", DesensitizeType.EMAIL, 0, 0),
                    "邮箱应抬升到前 2 后 2");
        }

        @Test
        @DisplayName("ADDRESS：最少保留前 3，最小尾部为 0")
        void testAddress() {
            assertEquals("北京市***", DesensitizeUtil.desensitize("北京市朝阳区", DesensitizeType.ADDRESS, 0, 0),
                    "地址应抬升到前 3 后 0");
        }

        @Test
        @DisplayName("ADDRESS：传入更大尾部时以传入值为准")
        void testAddressWithTail() {
            assertEquals("北京市*阳区", DesensitizeUtil.desensitize("北京市朝阳区", DesensitizeType.ADDRESS, 3, 2),
                    "传入 tail=2 时应保留末尾 2 个字");
        }

        @Test
        @DisplayName("CHINESE_NAME：两字名会把尾部置 0")
        void testChineseNameTooShort() {
            assertEquals("张*", DesensitizeUtil.desensitize("张三", DesensitizeType.CHINESE_NAME, 0, 0),
                    "两字名长度不超过 head + tail，tail 应被置 0");
        }

        @Test
        @DisplayName("CHINESE_NAME：三字名正常保留首尾")
        void testChineseNameNormal() {
            assertEquals("张*丰", DesensitizeUtil.desensitize("张三丰", DesensitizeType.CHINESE_NAME, 0, 0),
                    "三字名应保留首尾各 1 位");
        }

        @Test
        @DisplayName("CHINESE_NAME：单字名整串替换")
        void testChineseNameSingleChar() {
            assertEquals("*", DesensitizeUtil.desensitize("李", DesensitizeType.CHINESE_NAME, 0, 0),
                    "单字名长度不足时应整串替换为 1 个符号");
        }

        @Test
        @DisplayName("CHINESE_NAME：传入更大 head 时同样会重置 tail")
        void testChineseNameWithLargerHead() {
            assertEquals("张三*", DesensitizeUtil.desensitize("张三丰", DesensitizeType.CHINESE_NAME, 2, 1),
                    "head=2 时长度不超过 head + tail，tail 应被置 0");
        }

        @Test
        @DisplayName("TELEPHONE：长度大于 8 时前后各留 4")
        void testTelephoneWithRegionCode() {
            assertEquals("0101***5678", DesensitizeUtil.desensitize("01012345678", DesensitizeType.TELEPHONE, 0, 0),
                    "含区号座机应前后各留 4 位");
        }

        @Test
        @DisplayName("TELEPHONE：长度等于 8 时前后各留 2")
        void testTelephoneLengthEight() {
            assertEquals("12****78", DesensitizeUtil.desensitize("12345678", DesensitizeType.TELEPHONE, 0, 0),
                    "长度 8 不大于 8，应前后各留 2 位");
        }

        @Test
        @DisplayName("TELEPHONE：长度不足 8 时前后各留 2")
        void testTelephoneShort() {
            assertEquals("12***67", DesensitizeUtil.desensitize("1234567", DesensitizeType.TELEPHONE, 0, 0),
                    "短号应前后各留 2 位");
        }

        @Test
        @DisplayName("TELEPHONE：极短号码整串替换")
        void testTelephoneTooShort() {
            assertEquals("****", DesensitizeUtil.desensitize("1234", DesensitizeType.TELEPHONE, 0, 0),
                    "head + tail 不小于长度时应整串替换");
        }

        @Test
        @DisplayName("TELEPHONE：传入更大 head 时以传入值为准")
        void testTelephoneWithLargerHead() {
            assertEquals("01012**5678", DesensitizeUtil.desensitize("01012345678", DesensitizeType.TELEPHONE, 5, 4),
                    "传入 head=5 时应保留前 5 位");
        }

        @Test
        @DisplayName("IP_V4：忽略 head/tail 直接走 IPv4 分支")
        void testIpV4IgnoresHeadTail() {
            assertEquals("192.***.***.1",
                    DesensitizeUtil.desensitize("192.168.1.1", DesensitizeType.IP_V4, 5, 5),
                    "IP_V4 类型应忽略 head/tail 参数");
        }

        @Test
        @DisplayName("CUSTOM：不做最小值调整，按传入值替换")
        void testCustom() {
            assertEquals("ab**ef", DesensitizeUtil.desensitize("abcdef", DesensitizeType.CUSTOM, 2, 2),
                    "自定义类型应严格按传入的 head=2、tail=2 替换");
        }

        @Test
        @DisplayName("CUSTOM：head + tail 等于长度时整串替换")
        void testCustomBoundary() {
            assertEquals("******", DesensitizeUtil.desensitize("abcdef", DesensitizeType.CUSTOM, 2, 4),
                    "head + tail 等于长度时应整串替换");
        }

        @Test
        @DisplayName("空串输入：各类型均返回空串")
        void testEmptyTextForEveryType() {
            assertEquals("", DesensitizeUtil.desensitize("", DesensitizeType.MOBILE, 0, 0), "手机号类型空串应返回空串");
            assertEquals("", DesensitizeUtil.desensitize("", DesensitizeType.CHINESE_NAME, 0, 0), "中文名类型空串应返回空串");
            assertEquals("", DesensitizeUtil.desensitize("", DesensitizeType.CUSTOM, 0, 0), "自定义类型空串应返回空串");
            assertEquals("", DesensitizeUtil.desensitize("", DesensitizeType.IP_V4, 0, 0), "IP 类型空串应返回空串");
        }

        @Test
        @DisplayName("异常分支：text 为 null 抛 IllegalArgumentException")
        void testNullText() {
            assertThrows(IllegalArgumentException.class,
                    () -> DesensitizeUtil.desensitize(null, DesensitizeType.CUSTOM, 1, 1),
                    "text 为 null 是调用错误，应抛带明确信息的异常");
        }

        @Test
        @DisplayName("异常分支：type 为 null 抛 IllegalArgumentException")
        void testNullType() {
            assertThrows(IllegalArgumentException.class,
                    () -> DesensitizeUtil.desensitize("abc", null, 1, 1),
                    "type 为 null 是调用错误，不应靠 switch 落空才抛 NPE");
        }
    }

    @Nested
    @DisplayName("desensitize 五参重载：自定义脱敏符号")
    class DesensitizeFiveArgsTest {

        @Test
        @DisplayName("正常路径：MOBILE 使用自定义符号")
        void testMobileWithCustomSymbol() {
            assertEquals("138####8000",
                    DesensitizeUtil.desensitize("13800138000", DesensitizeType.MOBILE, 0, 0, "#"),
                    "手机号中间 4 位应替换为 4 个 #");
        }

        @Test
        @DisplayName("正常路径：BANK_CARD 使用自定义符号")
        void testBankCardWithCustomSymbol() {
            assertEquals("6222###########0123",
                    DesensitizeUtil.desensitize("6222021234567890123", DesensitizeType.BANK_CARD, 0, 0, "#"),
                    "银行卡号中间 11 位应替换为 11 个 #");
        }

        @Test
        @DisplayName("正常路径：CUSTOM 使用自定义符号")
        void testCustomWithCustomSymbol() {
            assertEquals("######", DesensitizeUtil.desensitize("abcdef", DesensitizeType.CUSTOM, 0, 0, "#"),
                    "自定义类型头尾为 0 时应全部替换为 #");
        }

        @Test
        @DisplayName("正常路径：IP_V4 使用自定义符号")
        void testIpV4WithCustomSymbol() {
            assertEquals("192.###.###.1",
                    DesensitizeUtil.desensitize("192.168.1.1", DesensitizeType.IP_V4, 0, 0, "#"),
                    "IP 类型应把中间两段替换为 3 个 #");
        }

        @Test
        @DisplayName("空值分支：空符号回退为默认符号")
        void testWithEmptySymbol() {
            assertEquals("138****8000",
                    DesensitizeUtil.desensitize("13800138000", DesensitizeType.MOBILE, 0, 0, ""),
                    "符号为空串时应回退为默认符号 *，手机号中间 4 位应被替换而不是被删除");
        }

        @Test
        @DisplayName("空值分支：纯空白符号回退为默认符号")
        void testWithBlankSymbol() {
            assertEquals("138****8000",
                    DesensitizeUtil.desensitize("13800138000", DesensitizeType.MOBILE, 0, 0, "   "),
                    "符号为纯空白时应回退为默认符号 *，手机号中间 4 位应被替换而不是被删除");
        }

        @Test
        @DisplayName("空值分支：IP_V4 传 null 符号时回退到默认符号")
        void testIpV4WithNullSymbol() {
            assertEquals("192.***.***.1",
                    DesensitizeUtil.desensitize("192.168.1.1", DesensitizeType.IP_V4, 0, 0, null),
                    "IP_V4 类型允许符号为 null 并回退到 *");
        }

        @Test
        @DisplayName("空值分支：非 IP 类型传 null 符号时回退到默认符号")
        void testNullSymbolForNonIpType() {
            assertEquals("138****8000",
                    DesensitizeUtil.desensitize("13800138000", DesensitizeType.MOBILE, 0, 0, null),
                    "非 IP 类型传 null 符号时同样应回退为默认符号 *，与显式传 * 的结果一致");
            assertEquals(DesensitizeUtil.desensitize("13800138000", DesensitizeType.MOBILE, 0, 0, "*"),
                    DesensitizeUtil.desensitize("13800138000", DesensitizeType.MOBILE, 0, 0, null),
                    "null 符号与显式传 * 的脱敏结果应完全相同");
        }

        @Test
        @DisplayName("异常分支：五参重载传 null 文本抛 IllegalArgumentException")
        void testNullValueString() {
            assertThrows(IllegalArgumentException.class,
                    () -> DesensitizeUtil.desensitize(null, DesensitizeType.MOBILE, 0, 0, "*"),
                    "valueString 为 null 是调用错误，应抛带明确信息的异常");
        }
    }

    @Nested
    @DisplayName("工具类约束")
    class UtilContractTest {

        @Test
        @DisplayName("构造器私有：禁止外部实例化")
        void testPrivateConstructor() throws Exception {
            Constructor<DesensitizeUtil> constructor = DesensitizeUtil.class.getDeclaredConstructor();
            assertTrue(Modifier.isPrivate(constructor.getModifiers()), "工具类构造器应为 private");
            constructor.setAccessible(true);
            DesensitizeUtil instance = constructor.newInstance();
            assertNotNull(instance, "反射调用私有构造器应能正常创建实例");
        }
    }
}
