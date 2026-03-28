package com.example.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
@Slf4j
public class PaymentService {
    
    public boolean authorize(BigDecimal amount, String cardToken) {
        log.info("Authorizing payment of {} with token {}", amount, cardToken);
        // Mocking payment failure for a specific token
        if ("fail_token".equals(cardToken)) {
            return false;
        }
        return true;
    }
}
