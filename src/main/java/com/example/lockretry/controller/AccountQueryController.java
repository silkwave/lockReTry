package com.example.lockretry.controller;

import com.example.lockretry.domain.AccountDto;
import com.example.lockretry.mapper.AccountQueryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AccountQueryController {

    private final AccountQueryMapper accountQueryMapper;

    @GetMapping("/api/account/{accountNo}")
    public ResponseEntity<AccountDto> getAccount(@PathVariable String accountNo) {
        AccountDto account = accountQueryMapper.selectAccount(accountNo);
        if (account != null) {
            return ResponseEntity.ok(account);
        }
        return ResponseEntity.notFound().build();
    }
}