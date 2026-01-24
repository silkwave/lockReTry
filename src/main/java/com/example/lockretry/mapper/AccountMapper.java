package com.example.lockretry.mapper;

import com.example.lockretry.domain.AccountDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AccountMapper {
    AccountDto selectAccountForUpdate(@Param("accountNo") String accountNo);
    int updateBalance(AccountDto account);
}