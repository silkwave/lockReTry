package com.example.config;

import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.lang.NonNull;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 트랜잭션 설정 클래스입니다.
 * Spring Boot의 기본 트랜잭션 매니저 설정을 사용하며,
 * 별도의 커스텀 설정 없이 Spring의 선언적 트랜잭션 관리를 활용합니다.
 */
@Configuration
public class TransactionConfig {

    /**
     * 트랜잭션 매니저를 설정합니다.
     * Spring의 기본 동작을 따르므로 별도의 커스텀 설정 없이
     * 자동 롤백 기능을 그대로 사용합니다.
     */
    @Bean
    public PlatformTransactionManager transactionManager(@NonNull DataSource dataSource) {
        DataSourceTransactionManager tm = new DataSourceTransactionManager(dataSource);
        // Spring 기본 동작 사용: 예외 발생 시 자동 롤백
        return tm;
    }
}