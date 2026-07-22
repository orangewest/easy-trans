package io.github.orangewest.easytrans.demo;

import io.github.orangewest.easytrans.demo.dto.UserDto;
import io.github.orangewest.trans.service.TransService;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 easy-trans 的翻译指标经 Spring Boot 的 Observation 管线自动发布为 Meter，
 * 从而可被 Actuator（/actuator/metrics、/actuator/prometheus）观测。
 * <p>
 * 不依赖 HTTP：直接断言 {@link MeterRegistry} 中存在 {@code easytrans.translate} /
 * {@code easytrans.repository} / {@code easytrans.field} 三个 timer，且 translate 已有采样。
 */
@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
class DemoActuatorMetricsTest {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private TransService transService;

    @Test
    void easytransMetricsAreExposedViaActuator() {
        UserDto u = new UserDto();
        u.setId(1);
        u.setName("张三");
        u.setSex(1);
        u.setDictSex("2");
        u.setTeacherId(2);
        transService.trans(u);

        List<Meter> meters = meterRegistry.getMeters();
        Set<String> names = meters.stream()
            .map(m -> m.getId().getName())
            .collect(Collectors.toSet());

        assertThat(names)
            .as("easy-trans 指标应以 easytrans.* 之名发布；当前 MeterRegistry 含: %s", names)
            .contains("easytrans.translate", "easytrans.repository", "easytrans.field");

        Timer translate = meters.stream()
            .filter(m -> "easytrans.translate".equals(m.getId().getName()))
            .findFirst()
            .map(m -> (Timer) m)
            .orElseThrow();
        assertThat(translate.count())
            .as("translate timer 应已有采样")
            .isGreaterThan(0);
    }
}
