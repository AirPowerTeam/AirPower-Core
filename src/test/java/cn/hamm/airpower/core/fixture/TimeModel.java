package cn.hamm.airpower.core.fixture;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 含 java.time 字段的模型，用于验证 JSR-310 序列化
 *
 * @author Hamm.cn
 * @apiNote 字段刻意不标 {@code @Description}：本类只服务于 JSR-310 序列化测试，
 * 混入其它注解会让「序列化产物」这条断言难以定位
 */
@Data
@Accessors(chain = true)
public class TimeModel {
    /**
     * 日期时间
     */
    private LocalDateTime dateTime;

    /**
     * 日期
     */
    private LocalDate date;

    /**
     * 时间戳
     */
    private Instant instant;
}
