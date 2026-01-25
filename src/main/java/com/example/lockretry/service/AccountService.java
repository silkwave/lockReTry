package com.example.lockretry.service;

import com.example.lockretry.component.LockRetryTemplate;
import com.example.lockretry.domain.AccountDto;
import com.example.lockretry.mapper.AccountMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 계좌 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 주로 입금(deposit) 기능을 제공하며, 락 재시도 템플릿을 활용하여 동시성 문제를 관리합니다.
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    /** 계좌 데이터베이스 접근을 위한 매퍼 */
    private final AccountMapper accountMapper;
    /** 락 충돌 시 재시도 로직을 실행하는 템플릿 */
    private final LockRetryTemplate lockRetryTemplate;

    public AccountService(AccountMapper accountMapper, LockRetryTemplate lockRetryTemplate) {
        this.accountMapper = accountMapper;
        this.lockRetryTemplate = lockRetryTemplate;
    }

    /**
     * 특정 계좌에 금액을 입금합니다.
     * 락 충돌 발생 시 재시도하며, 각 시도마다 새로운 트랜잭션을 사용합니다.
     *
     * @param accountNo 입금할 계좌 번호
     * @param amount 입금할 금액
     * @throws IllegalArgumentException 계좌를 찾을 수 없을 경우
     */
    public void deposit(String accountNo, BigDecimal amount) {
        log.debug("입금 요청 - 계좌: {}, 금액: {}", accountNo, amount);

        // 재시도 로직 수행 (트랜잭션 외부에서)
        lockRetryTemplate.execute(() -> {
            // 비즈니스 로직 실행 (별도 메서드로 분리)
            return depositLogic(accountNo, amount);
        });
    }

    /**
     * 실제 입금 비즈니스 로직을 수행합니다.
     * 각 재시도마다 새로운 트랜잭션을 시작합니다.
     *
     * @param accountNo 입금할 계좌 번호
     * @param amount 입금할 금액
     * @return 처리 결과
     * @throws IllegalArgumentException 계좌를 찾을 수 없을 경우
     */
    @Transactional
    public Void depositLogic(String accountNo, BigDecimal amount) {
        log.debug("락 획득 시도 (DB 조회) - 계좌: {}", accountNo);
        
        // 1. 락 획득
        AccountDto account = accountMapper.selectAccountForUpdate(accountNo);

        if (account == null) {
            log.warn("계좌를 찾을 수 없습니다: {}", accountNo);
            throw new IllegalArgumentException("계좌를 찾을 수 없습니다: " + accountNo);
        }

        log.debug("락 획득 성공. 현재 잔액: {}", account.getBalance());

        // 2. 비즈니스 로직 수행: 잔액 증가
        account.setBalance(account.getBalance().add(amount));

        // 3. 업데이트: 변경된 잔액을 데이터베이스에 반영
        accountMapper.updateBalance(account);
        log.debug("입금 완료. 새로운 잔액: {}", account.getBalance());
        
        return null;
    }
}