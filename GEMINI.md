# Oracle SELECT FOR UPDATE NOWAIT & MyBatis 재시도 전략

본 문서는 **H2 Database(Oracle Mode)** 및 **Oracle** 환경에서 `SELECT FOR UPDATE NOWAIT` 사용 시 발생하는 락 충돌을 **전략 패턴(Strategy Pattern)**으로 해결하는 재시도 로직 구현 가이드입니다.

---

## 🛠 기술 스택
* **Backend**: JDK 21, Spring Boot, MyBatis
* **Database**: H2 Database (Oracle Mode)
* **Design Pattern**: Strategy Pattern

---

## 1. Database & Persistence Layer

### 계좌 테이블 생성 (DDL)
동시성 제어가 필요한 예금 계좌 테이블 설계입니다.

```sql
CREATE TABLE ACCOUNT (
    ACCOUNT_NO   VARCHAR2(20) PRIMARY KEY, -- 계좌번호
    USER_NAME    VARCHAR2(50) NOT NULL,    -- 소유주
    BALANCE      NUMBER(18, 2) DEFAULT 0,  -- 잔액
    UPDATE_DATE  TIMESTAMP DEFAULT SYSDATE -- 최종 수정일
);

-- 테스트 데이터 삽입
INSERT INTO ACCOUNT (ACCOUNT_NO, USER_NAME, BALANCE) VALUES ('123-456', 'Gemini', 1000000);

<select id="selectAccountForUpdate" resultType="AccountDto">
    SELECT 
        ACCOUNT_NO, 
        USER_NAME, 
        BALANCE 
    FROM ACCOUNT
    WHERE ACCOUNT_NO = #{accountNo}
    FOR UPDATE NOWAIT
</select>

. 핵심 장점
    안정성: NOWAIT을 사용하여 DB 세션이 무한 대기(Hang) 상태에 빠지는 것을 방지합니다.
    유연성: 재시도 횟수나 대기 정책을  한 곳에서만 수정하면 전역에 반영됩니다.
    가독성: 비즈니스 로직에서 try-catch 및 재시도 루프가 제거되어 코드가 깔끔해집니다.
    성능: 랜덤 백오프(Random Backoff)를 통해 동시 요청 시 발생하는 재충돌(Collision)을 방지합니다.
    