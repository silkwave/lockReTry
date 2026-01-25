package com.example.lockretry.controller;

import com.example.lockretry.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 계좌 관련 REST API 요청을 처리하는 컨트롤러 클래스입니다.
 * 클라이언트로부터 입금 요청을 받아 AccountService를 통해 비즈니스 로직을 수행합니다.
 */
@RestController
@RequiredArgsConstructor
public class AccountController {

    /** 계좌 관련 비즈니스 로직을 처리하는 서비스 */
    private final AccountService accountService;

    /**
     * 계좌에 입금하는 POST 요청을 처리합니다.
     * URL: /api/deposit
     *
     * @param accountNo 입금할 계좌 번호 (Request Parameter)
     * @param amount 입금할 금액 (Request Parameter)
     * @return 입금 성공 또는 실패에 따른 ResponseEntity
     */
    @PostMapping("/api/deposit")
    public ResponseEntity<String> deposit(@RequestParam("accountNo") String accountNo, @RequestParam("amount") BigDecimal amount) {
        try {
            accountService.deposit(accountNo, amount);
            return ResponseEntity.ok("입금 성공: " + amount + "원 (계좌: " + accountNo + ")");
        } catch (Exception e) {
            // 락 획득 실패 또는 계좌 없음 등의 예외 처리
            return ResponseEntity.status(500).body("입금 실패: " + e.getMessage());
        }
    }
}
