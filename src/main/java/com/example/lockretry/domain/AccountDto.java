package com.example.lockretry.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 계좌 정보를 담는 DTO (Data Transfer Object) 클래스입니다.
 * 직접 getter, setter, toString, equals, hashCode 메서드를 구현합니다.
 */
public class AccountDto {
    /** 계좌번호 */
    private String accountNo;
    /** 소유주 이름 */
    private String userName;
    /** 잔액 */
    private BigDecimal balance;
    /** 최종 업데이트 일시 */
    private LocalDateTime updateDate;

    // Getter 메서드들
    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
    }

    @Override
    public String toString() {
        return "AccountDto{" +
                "accountNo='" + accountNo + '\'' +
                ", userName='" + userName + '\'' +
                ", balance=" + balance +
                ", updateDate=" + updateDate +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AccountDto that = (AccountDto) o;

        if (accountNo != null ? !accountNo.equals(that.accountNo) : that.accountNo != null) return false;
        if (userName != null ? !userName.equals(that.userName) : that.userName != null) return false;
        if (balance != null ? !balance.equals(that.balance) : that.balance != null) return false;
        return updateDate != null ? updateDate.equals(that.updateDate) : that.updateDate == null;
    }

    @Override
    public int hashCode() {
        int result = accountNo != null ? accountNo.hashCode() : 0;
        result = 31 * result + (userName != null ? userName.hashCode() : 0);
        result = 31 * result + (balance != null ? balance.hashCode() : 0);
        result = 31 * result + (updateDate != null ? updateDate.hashCode() : 0);
        return result;
    }
}