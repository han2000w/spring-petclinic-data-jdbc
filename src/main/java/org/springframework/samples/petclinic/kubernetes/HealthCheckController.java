package org.springframework.samples.petclinic.kubernetes;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    @GetMapping("/kubernetes/health")
    public String healthCheck() {
        return "ok";
    }
}

