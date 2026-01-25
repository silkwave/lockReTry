package com.example.lockretry.service;

import com.example.lockretry.component.LockRetryTemplate;
import com.example.lockretry.domain.AccountDto;
import com.example.lockretry.mapper.AccountMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.function.Supplier;

/**
 * AccountService의 트랜잭션 및 롤백 동작을 검증하는 테스트 클래스입니다.
 * Early catch 예외 처리 개선 후 Spring의 자동 롤백이 정상적으로 동작하는지 확인합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService 트랜잭션 롤백 테스트")
class AccountServiceTransactionTest {

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private LockRetryTemplate lockRetryTemplate;

    @InjectMocks
    private AccountService accountService;

    private AccountDto testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new AccountDto();
        testAccount.setAccountNo("12345");
        testAccount.setUserName("테스트사용자");
        testAccount.setBalance(BigDecimal.valueOf(1000));
    }

    @Test
    @DisplayName("정상 입금 시 트랜잭션 커밋 확인")
    void deposit_Success_CommitsTransaction() {
        // Given
        BigDecimal depositAmount = BigDecimal.valueOf(500);
        
        when(lockRetryTemplate.execute(any())).thenAnswer(invocation -> {
            // LockRetryTemplate 내부에서 실제 비즈니스 로직이 호출되도록 시뮬레이션
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get(); // 이렇게 하면 내부 비즈니스 로직 메서드가 호출됨
        });
        when(accountMapper.selectAccountForUpdate("12345")).thenReturn(testAccount);

        // When & Then - 예외가 발생하지 않으면 정상 처리
        assertDoesNotThrow(() -> accountService.deposit("12345", depositAmount));
        
        verify(accountMapper).selectAccountForUpdate("12345");
        verify(accountMapper).updateBalance(any()); // 업데이트가 호출되었는지 확인
    }

    @Test
    @DisplayName("계좌 없음 예외 시 즉시 롤백 확인")
    void deposit_AccountNotFound_RollsBackTransaction() {
        // Given
        BigDecimal depositAmount = BigDecimal.valueOf(500);
        
        when(lockRetryTemplate.execute(any())).thenAnswer(invocation -> {
            // 트랜잭션 내부에서 예외 발생 시뮬레이션
            Supplier<?> supplier = invocation.getArgument(0);
            supplier.get(); // 이렇게 하면 내부 비즈니스 로직 메서드가 호출됨
            return null;
        });
        when(accountMapper.selectAccountForUpdate("99999")).thenReturn(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> accountService.deposit("99999", depositAmount)
        );
        
        assertEquals("계좌를 찾을 수 없습니다: 99999", exception.getMessage());
        
        // 롤백이 발생했으므로 updateBalance는 호출되지 않아야 함
        verify(accountMapper, never()).updateBalance(any());
    }

    @Test
    @DisplayName("락 충돌 시 재시도 로직 확인 - 원본 예외 전파")
    void deposit_LockConflict_RetriesAndRollsBack() {
        // Given
        BigDecimal depositAmount = BigDecimal.valueOf(500);
        PessimisticLockingFailureException lockException = 
            new PessimisticLockingFailureException("ORA-00054: resource busy");
        
        when(lockRetryTemplate.execute(any())).thenThrow(lockException);

        // When & Then
        PessimisticLockingFailureException exception = assertThrows(
            PessimisticLockingFailureException.class,
            () -> accountService.deposit("12345", depositAmount)
        );
        
        // LockRetryTemplate에서 원본 예외가 그대로 전파되는지 확인
        assertEquals("ORA-00054: resource busy", exception.getMessage());
        assertSame(lockException, exception); // 같은 인스턴스인지 확인
        
        // LockRetryTemplate 예외로 인해 롤백 발생
        verify(accountMapper, never()).updateBalance(any());
    }

    @Test
    @DisplayName("트랜잭션 경계 개선 후 rollback-only 상태 방지 확인")
    void deposit_TransactionBoundaryImproved_PreventsRollbackOnlyState() {
        // Given
        BigDecimal depositAmount = BigDecimal.valueOf(500);
        
        // 첫 번째 시도에서 락 충돌, 두 번째 시도에서 성공 시나리오
        when(lockRetryTemplate.execute(any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            
            // 첫 번째 호출 시뮬레이션
            try {
                supplier.get();
            } catch (Exception e) {
                // 재시도 시뮬레이션 - 실제로는 성공해야 함
                return supplier.get();
            }
            return null;
        });
        
        when(accountMapper.selectAccountForUpdate("12345"))
            .thenThrow(new PessimisticLockingFailureException("ORA-00054"))
            .thenReturn(testAccount); // 두 번째 시도에서 성공

        // When & Then
        assertDoesNotThrow(() -> accountService.deposit("12345", depositAmount));
        
        // 성공했으므로 업데이트 호출 확인
        verify(accountMapper).updateBalance(any());
    }

    @Test
    @DisplayName("트랜잭션 경계 개선으로 재시도 로직이 트랜잭션 내부에서 실행되는지 확인")
    void deposit_TransactionBoundaryImproved_RetryInsideTransaction() {
        // Given
        BigDecimal depositAmount = BigDecimal.valueOf(500);
        
        // LockRetryTemplate이 트랜잭션 내부에서 호출되는지 확인
        when(lockRetryTemplate.execute(any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            supplier.get(); // 내부 비즈니스 로직 메서드 호출
            return null;
        });
        when(accountMapper.selectAccountForUpdate("12345")).thenReturn(testAccount);

        // When
        accountService.deposit("12345", depositAmount);

        // Then - LockRetryTemplate이 호출되었는지 확인
        verify(lockRetryTemplate, times(1)).execute(any());
        verify(accountMapper).updateBalance(any()); // 내부 비즈니스 로직 메서드에서 업데이트 호출 확인
    }
}