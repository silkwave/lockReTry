package com.example.lockretry.controller;

import com.example.lockretry.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/api/deposit")
    public ResponseEntity<String> deposit(@RequestParam String accountNo, @RequestParam BigDecimal amount) {
        try {
            accountService.deposit(accountNo, amount);
            return ResponseEntity.ok("입금 성공: " + amount + "원 (계좌: " + accountNo + ")");
        } catch (Exception e) {
            // 락 획득 실패 또는 계좌 없음 등의 예외 처리
            return ResponseEntity.status(500).body("입금 실패: " + e.getMessage());
        }
    }
}
