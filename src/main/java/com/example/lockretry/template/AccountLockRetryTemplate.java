package com.example.lockretry.template;

import com.example.lockretry.domain.AccountDto;
import com.example.lockretry.mapper.AccountMapper;
import com.example.lockretry.strategy.RetryStrategy;

// AccountService에서 직접 인스턴스화하여 accountNo를 전달할 예정이므로 @Component 주석은 사용하지 않습니다.
// 대신 생성자를 통해 필요한 의존성을 주입받고 accountNo를 필드로 가집니다.
public class AccountLockRetryTemplate extends AbstractLockRetryTemplate<AccountDto> {

    private final AccountMapper accountMapper;
    private final String accountNo;

    // AccountService에서 이 클래스를 인스턴스화할 때 필요한 의존성을 주입하도록 합니다.
    public AccountLockRetryTemplate(RetryStrategy retryStrategy, AccountMapper accountMapper, String accountNo) {
        super(retryStrategy);
        this.accountMapper = accountMapper;
        this.accountNo = accountNo;
    }

    @Override
    protected AccountDto doInLockedSection() {
        // 실제 락 획득 비즈니스 로직
        return accountMapper.selectAccountForUpdate(accountNo);
    }
}
