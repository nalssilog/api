package com.nalssilog.app.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로드밸런서·모니터링용 라이브니스 체크. 의존 상태 점검이 필요하면 Actuator 도입을 고려한다.
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public HealthResponse health() {
        return new HealthResponse("UP");
    }

    public record HealthResponse(String status) {
    }
}
