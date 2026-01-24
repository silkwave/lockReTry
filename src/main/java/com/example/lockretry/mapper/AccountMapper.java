package com.example.lockretry.mapper;

import com.example.lockretry.domain.AccountDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 계좌 관련 데이터베이스 작업을 위한 MyBatis 매퍼 인터페이스입니다.
 * 계좌 정보 조회 및 잔액 업데이트 기능을 제공합니다.
 */
@Mapper
public interface AccountMapper {
    /**
     * 특정 계좌 번호에 해당하는 계좌 정보를 `FOR UPDATE NOWAIT`를 사용하여 조회합니다.
     * 이는 해당 계좌에 락을 걸고, 락 획득이 불가능할 경우 즉시 예외를 발생시킵니다.
     *
     * @param accountNo 조회할 계좌 번호
     * @return 조회된 계좌 정보 (AccountDto), 락 획득 실패 시 예외 발생
     */
    AccountDto selectAccountForUpdate(@Param("accountNo") String accountNo);

    /**
     * 계좌의 잔액을 업데이트합니다.
     *
     * @param account 업데이트할 계좌 정보를 담은 AccountDto 객체
     * @return 업데이트된 행의 수
     */
    int updateBalance(AccountDto account);
}