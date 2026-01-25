package com.example.lockretry.service;

import com.example.lockretry.template.AbstractLockRetryTemplate;
import com.example.lockretry.domain.AccountDto;
import com.example.lockretry.mapper.AccountMapper;
import com.example.lockretry.strategy.RetryStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy; // Added import for Lazy
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation; // Added import for Propagation

import java.math.BigDecimal;

/**
 * 계좌 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 주로 입금(deposit) 기능을 제공하며, 락 재시도 템플릿을 활용하여 동시성 문제를 관리합니다.
 */
@Slf4j
@Service
public class AccountService {

    private final AccountMapper accountMapper;
    private final RetryStrategy retryStrategy;
    private final AccountService self;

    public AccountService(AccountMapper accountMapper, RetryStrategy retryStrategy, @Lazy AccountService self) {
        this.accountMapper = accountMapper;
        this.retryStrategy = retryStrategy;
        this.self = self;
    }

    /**
     * @Transactional을 제거합니다.
     *                 재시도 루프는 트랜잭션 '밖'에서 돌아야 매번 깨끗한 상태로 시도할 수 있습니다.
     */
    @Transactional
    public void deposit(String accountNo, BigDecimal amount) {
        log.debug("입금 요청 - 계좌: {}, 금액: {}", accountNo, amount);

        new AbstractLockRetryTemplate<Void>(retryStrategy) {
            @Override
            protected Void doInLockedSection() {
                // 여기서 self 호출 시점에만 새 트랜잭션이 시작됨
                return self.doDepositInNewTransaction(accountNo, amount);
            }
        }.execute();
    }

    /**
     * 실제 비즈니스 로직: 각 시도는 완전히 독립적이어야 함.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Void doDepositInNewTransaction(String accountNo, BigDecimal amount) {
        // MyBatis: SELECT FOR UPDATE NOWAIT
        // 락 획득 실패 시 여기서 즉시 Exception 발생 -> Template이 Catch해서 Retry
        AccountDto account = accountMapper.selectAccountForUpdate(accountNo);

        if (account == null) {
            throw new IllegalArgumentException("계좌를 찾을 수 없습니다: " + accountNo);
        }

        account.setBalance(account.getBalance().add(amount));
        accountMapper.updateBalance(account);

        return null;
    }
}