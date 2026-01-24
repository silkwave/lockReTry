package com.example.lockretry.service;

import com.example.lockretry.component.LockRetryTemplate;
import com.example.lockretry.domain.AccountDto;
import com.example.lockretry.mapper.AccountMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountMapper accountMapper;
    private final LockRetryTemplate lockRetryTemplate;

    @Transactional
    public void deposit(String accountNo, BigDecimal amount) {
        log.debug("Deposit request - Account: {}, Amount: {}", accountNo, amount);

        // 1. 락 획득 (재시도 전략 적용)
        // 람다식을 통해 비즈니스 로직과 재시도 로직을 분리
        AccountDto account = lockRetryTemplate.execute(() -> 
            accountMapper.selectAccountForUpdate(accountNo)
        );

        if (account == null) {
            log.warn("Account not found: {}", accountNo);
            throw new IllegalArgumentException("Account not found: " + accountNo);
        }

        log.debug("Lock acquired. Current Balance: {}", account.getBalance());

        // 2. 비즈니스 로직 수행
        account.setBalance(account.getBalance().add(amount));
        
        // 3. 업데이트
        accountMapper.updateBalance(account);
        log.debug("Deposit completed. New Balance: {}", account.getBalance());
    }
}