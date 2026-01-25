package com.example.lockretry.config;

import com.example.lockretry.strategy.ExponentialBackoffRetryStrategy;
import com.example.lockretry.strategy.FixedDelayRetryStrategy;
import com.example.lockretry.strategy.RandomBackoffRetryStrategy;
import com.example.lockretry.strategy.RetryStrategy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 재시도 정책 설정을 관리하는 설정 클래스입니다.
 * 애플리케이션 속성 파일(.yml, .properties)에서 재시도 관련 설정을 관리합니다.
 */
@Configuration
@ConfigurationProperties(prefix = "retry.strategy")
@Data
public class RetryStrategyConfig {

    /** 기본 재시도 전략 타입 */
    private StrategyType defaultStrategy = StrategyType.RANDOM_BACKOFF;
    
    /** 최대 재시도 횟수 */
    private int maxRetries = 10;
    
    /** 기본 대기 시간 (밀리초) */
    private long baseDelayMs = 100;
    
    /** 최대 대기 시간 (밀리초) */
    private long maxDelayMs = 2000;
    
    /** 지수 백오프 배수 */
    private double backoffMultiplier = 2.0;
    
    /** 랜덤 지터 최대값 (밀리초) */
    private long maxJitterMs = 200;

    /**
     * 재시도 전략 타입을 정의하는 열거형입니다.
     */
    public enum StrategyType {
        /** 고정 대기 시간 전략 */
        FIXED_DELAY,
        /** 랜덤 백오프 전략 */
        RANDOM_BACKOFF,
        /** 지수 백오프 전략 */
        EXPONENTIAL_BACKOFF
    }

    /**
     * 기본 재시도 전략 Bean을 생성합니다.
     * 설정 파일에 지정된 전략 타입에 따라 적절한 구현체를 반환합니다.
     * 
     * @return 설정된 재시도 전략 구현체
     */
    @Bean
    @Primary
    public RetryStrategy defaultRetryStrategy() {
        log.info("재시도 전략 초기화 - 타입: {}, 최대 재시도: {}회", defaultStrategy, maxRetries);
        
        switch (defaultStrategy) {
            case FIXED_DELAY:
                return new FixedDelayRetryStrategy(maxRetries, baseDelayMs);
            case RANDOM_BACKOFF:
                return new RandomBackoffRetryStrategy(maxRetries, baseDelayMs, maxJitterMs, maxDelayMs);
            case EXPONENTIAL_BACKOFF:
                return new ExponentialBackoffRetryStrategy(maxRetries, baseDelayMs, maxDelayMs, backoffMultiplier);
            default:
                log.warn("알 수 없는 재시도 전략: {}. 기본값인 RANDOM_BACKOFF를 사용합니다.", defaultStrategy);
                return new RandomBackoffRetryStrategy(maxRetries, baseDelayMs, maxJitterMs, maxDelayMs);
        }
    }

    /**
     * 고정 대기 시간 재시도 전략 Bean을 생성합니다.
     * 
     * @return 고정 대기 시간 재시도 전략
     */
    @Bean("configuredFixedDelayRetryStrategy")
    public RetryStrategy fixedDelayRetryStrategy() {
        return new FixedDelayRetryStrategy(maxRetries, baseDelayMs);
    }

    /**
     * 지수 백오프 재시도 전략 Bean을 생성합니다.
     * 
     * @return 지수 백오프 재시도 전략
     */
    @Bean("configuredExponentialBackoffRetryStrategy")
    public RetryStrategy exponentialBackoffRetryStrategy() {
        return new ExponentialBackoffRetryStrategy(maxRetries, baseDelayMs, maxDelayMs, backoffMultiplier);
    }

    /**
     * 랜덤 백오프 재시도 전략 Bean을 생성합니다.
     * 
     * @return 랜덤 백오프 재시도 전략
     */
    @Bean("configuredRandomBackoffRetryStrategy")
    public RetryStrategy randomBackoffRetryStrategy() {
        return new RandomBackoffRetryStrategy(maxRetries, baseDelayMs, maxJitterMs, maxDelayMs);
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RetryStrategyConfig.class);
}