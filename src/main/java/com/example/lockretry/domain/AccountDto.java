package com.example.lockretry.domain;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 계좌 정보를 담는 DTO (Data Transfer Object) 클래스입니다.
 * Lombok의 @Data 어노테이션을 사용하여 getter, setter, toString, equals, hashCode 등을 자동으로 생성합니다.
 */
@Data
public class AccountDto {
    /** 계좌번호 */
    private String accountNo;
    /** 소유주 이름 */
    private String userName;
    /** 잔액 */
    private BigDecimal balance;
    /** 최종 업데이트 일시 */
    private LocalDateTime updateDate;
}