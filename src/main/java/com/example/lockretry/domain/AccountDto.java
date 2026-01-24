package com.example.lockretry.domain;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AccountDto {
    private String accountNo;
    private String userName;
    private BigDecimal balance;
    private LocalDateTime updateDate;
}