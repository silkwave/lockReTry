package com.example.lockretry.mapper;

import com.example.lockretry.domain.AccountDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AccountQueryMapper {
    AccountDto selectAccount(@Param("accountNo") String accountNo);
}