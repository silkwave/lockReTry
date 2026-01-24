package com.example.lockretry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 애플리케이션의 메인 진입점입니다.
 * 이 어노테이션은 Spring Boot의 자동 설정, 컴포넌트 스캔, 구성 클래스 정의를 활성화합니다.
 */
@SpringBootApplication
public class LockRetryApplication {

    /**
     * 애플리케이션의 메인 메서드입니다.
     * SpringApplication.run()을 호출하여 Spring 컨테이너를 부트스트랩하고 애플리케이션을 시작합니다.
     *
     * @param args 명령줄 인자
     */
    public static void main(String[] args) {
        SpringApplication.run(LockRetryApplication.class, args);
    }

}
