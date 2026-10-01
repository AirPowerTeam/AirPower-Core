package cn.hamm.airpower.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <h1>脱敏方式</h1>
 *
 * @author Hamm.cn
 * @apiNote {@code minHead} / {@code minTail} 是<b>保留下限</b>：即使调用方传入更小的值，
 * 也会被抬到该下限，避免脱敏后仍泄露过多内容
 */
@AllArgsConstructor
@Getter
public enum DesensitizeType {
    /**
     * 座机号码
     */
    TELEPHONE(0, 0),

    /**
     * 手机号码
     */
    MOBILE(3, 4),

    /**
     * 身份证号
     */
    ID_CARD(6, 4),

    /**
     * 银行卡号
     */
    BANK_CARD(4, 4),

    /**
     * 车牌号
     */
    CAR_NUMBER(2, 1),

    /**
     * 邮箱（本地部分与域名各留 {@code 2} 位）
     */
    EMAIL(2, 2),

    /**
     * 中文名
     */
    CHINESE_NAME(1, 1),

    /**
     * 地址（只留前 {@code 3} 位）
     */
    ADDRESS(3, 0),

    /**
     * IPv4 地址（保留首尾两段，中间两段整体替换）
     */
    IP_V4(0, 0),

    /**
     * 自定义（完全由调用方传入的 head / tail 决定）
     */
    CUSTOM(0, 0);

    /**
     * 头部至少保留的字符数
     */
    private final int minHead;

    /**
     * 尾部至少保留的字符数
     */
    private final int minTail;
}
