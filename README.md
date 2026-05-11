# ☕ 커피 주문 시스템

> 다수 서버 환경에서도 안정적으로 동작하는 커피숍 주문 시스템

---

## 📌 목차

1. [프로젝트 소개](#프로젝트-소개)
2. [기술 스택](#기술-스택)
3. [설계 내용](#설계-내용)
4. [설계의 의도](#설계의-의도)
5. [문제 해결 전략](#문제-해결-전략)
6. [기술적 선택 이유](#기술적-선택-이유)

---

## 프로젝트 소개

커피 주문에 필요한 메뉴 조회, 포인트 충전, 주문/결제, 인기 메뉴 추천 기능을 제공하는 시스템입니다.
다중 서버 환경에서도 동시성 제어와 데이터 일관성을 보장하도록 설계하였습니다.

### 구현 기능

| 기능 | 설명 |
|---|---|
| 커피 메뉴 목록 조회 | 판매 중인 메뉴 목록 조회 (Redis 캐싱 적용) |
| 포인트 충전 | 사용자 포인트 충전 (1원 = 1P) |
| 커피 주문/결제 | 포인트로 주문/결제, 실시간 데이터 수집 플랫폼 전송 |
| 인기 메뉴 조회 | 최근 7일간 인기 메뉴 Top 3 조회 (Redis ZSET) |

---

## 기술 스택

| 분류 | 기술 |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.0.6 |
| Database | MySQL 8.0 |
| Cache / Lock | Redis, Redisson |
| Message Broker | Apache Kafka |
| Load Balancer | Nginx |
| Container | Docker, Docker Compose |
| Load Test | K6 |

---

## 설계 내용

### ERD
<img width="875" height="729" alt="Image" src="https://github.com/user-attachments/assets/aca7ed8e-f79c-4dbc-9473-ba2dd3b391e0" />


### API 명세서

#### 1. 커피 메뉴 목록 조회

| 항목 | 내용 |
|---|---|
| Method | GET |
| URL | /menus |
| 설명 | 판매 중인 메뉴 목록 조회 |

**Response**

```json
{
  "success": true,
  "data": [
    {
      "menuId": 1,
      "name": "아메리카노",
      "price": 4500
    },
    {
      "menuId": 2,
      "name": "카페라떼",
      "price": 5000
    },
    ...
  ]
}

```

---

#### 2. 포인트 충전

| 항목 | 내용 |
|---|---|
| Method | POST |
| URL | /users/{userId}/points |
| 설명 | 사용자 포인트 충전 |

**Request**

```json
{
  "point": 10000
}
```

**Response**

```json
{
  "success": true,
  "data": {
    "userId": 1,
    "point": 10000
  }
  "error": null
}
```

---

#### 3. 커피 주문/결제

| 항목 | 내용 |
|---|---|
| Method | POST |
| URL | /users/{userId}/order |
| 설명 | 포인트로 커피 주문/결제 (User·Menu 비관적 락) |

#### 3-1. 커피 주문/결제 (분산 락)

| 항목 | 내용 |
|---|---|
| Method | POST |
| URL | /users/{userId}/order/redisson |
| 설명 | 포인트로 커피 주문/결제 (Redisson 분산 락 + Menu 비관적 락) |

**Request**

```json
{
  "menuId": 1,
  "quantity": 5
}
```

**Response**

```json
{
  "success": true,
  "data": {
    "orderId": 1,
    "userId": 1,
    "menuId": 1,
    "menuName": "아메리카노",
    "amount": 4500,
    "remainBalance": 5500,
    "status": "PAYMENT_COMPLETED",
    "orderedAt": "2025-05-06T10:30:00"
  }
  "error": null
}
```

---

#### 4. 인기 메뉴 조회

| 항목 | 내용 |
|---|---|
| Method | GET |
| URL | /menus/popular |
| 설명 | 최근 7일간 인기 메뉴 Top 3 조회 |

**Response**

```json
{
  "success": true,
  "data": [
    {
      "rank": 1,
      "menuId": 1,
      "name": "아메리카노",
      "orderCount": 1524
    },
    {
      "rank": 2,
      "menuId": 5,
      "name": "라떼",
      "orderCount": 983
    },
    {
      "rank": 3,
      "menuId": 3,
      "name": "콜드브루",
      "orderCount": 742
    }
  ],
  "error": null
}
```

---

## 설계의 의도

### 1. 다중 인스턴스 환경 대응

여러 서버가 동시에 동작하는 환경에서도 데이터의 정확성이 보장되어야 합니다.
상태(State)는 반드시 단일 저장소(RDB)가 소유하며, 여러 서버가 각자 메모리에서 연산하는 구조를 허용하지 않습니다.

**아키텍처 구성**

```
Client
  ↓
Nginx (Load Balancer)
  ↓              ↓
app-1          app-2
  ↓              ↓
MySQL (공유 DB)
Redis (캐시 / 분산 락 / 랭킹 공유)
Kafka (비동기 이벤트 공유)
```

**원칙**

- `users.point` 의 진실의 원천은 RDB 하나
- Redis는 캐싱 / 락 / 랭킹 보조 역할만 담당
- 여러 서버가 각자 메모리에서 포인트를 계산하는 구조 불허

---

### 2. 동시성 이슈

동일한 사용자가 동시에 여러 기기에서 주문하거나 포인트를 충전/사용하는 경우 Race Condition이 발생할 수 있습니다.
포인트는 현금과 동일한 가치를 가지므로 단 한 건의 정합성 오류도 허용하지 않습니다.

**Race Condition 재현 (락 없음, 100건 동시 요청)**

```
총 요청 수             : 100건
성공 건수              : 14건
차감될 포인트           : 63,000원  (14건 × 4,500원)
정상 차감 시 예상 포인트 : 37,000원
실제 남은 포인트         : 55,000원
정합성 여부            : 💥 불일치 (18,000원 차감 누락)
```

**전략**

- 단일 서버 → 비관적 락 (`SELECT ... FOR UPDATE`)
  - User 락 → Menu 락 순서 고정 (데드락 방지)
- 다중 서버 → 분산 락 (Redisson) : DB 커넥션 점유 없이 Redis에서 User-level 락 관리
  - Redisson 락은 `lock:order:user:{userId}` 키로 동일 사용자의 중복 주문만 차단
  - 서로 다른 사용자가 같은 메뉴를 동시 주문하는 경우는 Redisson 락으로 보호되지 않으므로, Menu 재고에는 DB 비관적 락을 추가 적용

---

### 3. 데이터 일관성

주문/결제 과정에서 포인트 차감, 재고 차감, 주문 생성, 포인트 이력 기록이 모두 하나의 단위로 처리되어야 합니다.
또한 DB 트랜잭션과 Kafka 이벤트 발행 간의 정합성도 보장되어야 합니다.

**트랜잭션 처리 흐름**

```
@Transactional 시작
    ├── 재고 차감
    ├── 포인트 차감
    ├── 주문 생성
    ├── 포인트 이력 기록
    └── publishEvent() → 이벤트 큐에 예약
커밋 성공 → AFTER_COMMIT 감지 → Kafka 발행 ✅
커밋 실패 → 롤백 → 이벤트 버려짐 → Kafka 발행 안 됨 ✅
```

**Kafka Consumer 장애 대응 (DLT)**

```
Consumer 메시지 수신
    ↓
예외 발생 💥
    ↓
재시도 1회 (1초 후)
재시도 2회 (1초 후)
재시도 3회 (1초 후)
    ↓
3회 모두 실패
    ↓
order-completed.DLT 토픽으로 이동 → 메시지 보존 ✅
```

---

## 문제 해결 전략

### 1. 비관적 락 vs 분산 락 (단일 서버)

**테스트 환경**

| 항목 | 내용 |
|---|---|
| 환경 | 단일 인스턴스 (Spring Boot 로컬 실행) |
| DB | MySQL 8.0 (REPEATABLE_READ) |
| 동시 요청 수 | 100건 |
| 스레드 풀 | 32개 |
| 초기 포인트 | 1,000,000원 |
| 메뉴 가격 | 4,500원 |

**결과**

| | 락 없음 | 비관적 락 | 분산 락 |
|---|---|---|---|
| 요청 수 | 100건 | 100건 | 100건 |
| 성공 건수 | 14건 | 100건 | 100건 |
| 차감 누락 | 18,000원 💥 | 0원 ✅ | 0원 ✅ |
| 정합성 | 💥 불일치 | ✅ 정상 | ✅ 정상 |
| 소요 시간 | 5,526ms | 5,948ms | 6,520ms |
| 락 위치 | 없음 | DB | Redis |

**분석**

단일 서버 환경에서 비관적 락과 분산 락 모두 100건 전부 성공하며 정합성을 완벽하게 보장했습니다.
소요 시간은 비관적 락(5,948ms)이 분산 락(6,520ms)보다 약 10% 빠른 결과를 보였습니다.
이는 분산 락이 Redis 네트워크 왕복 오버헤드를 가지기 때문입니다.

---

### 2. 비관적 락 vs 분산 락 (다중 서버)

**테스트 환경**

| 항목 | 내용 |
|---|---|
| 환경 | 다중 인스턴스 (app-1, app-2 + Nginx) |
| 부하 도구 | K6 |
| 동시 사용자 | 최대 50명 |
| 설정 | 워밍업 30s → 본 테스트 1m → 종료 30s |

**결과**

| | 비관적 락 | 분산 락 |
|---|---|---|
| 총 요청 수 | 4,499건 | 4,490건 |
| TPS | 37.36건/s | 37.22건/s |
| p(90) | 16.80ms | 20.08ms |
| p(95) | 20.84ms | 28.54ms |
| 최대 응답시간 | 102.96ms | 211.82ms |
| 에러율 | 0% | 0% |
| 정합성 | ✅ | ✅ |

**분석**

소규모 다중 서버 환경(앱 2대, HikariCP 기본 10개)에서는 두 방식 모두 에러율 0%, 정합성을 보장했습니다.
비관적 락이 p(95) 기준 20.84ms로 분산 락(28.54ms)보다 약 37% 빠른 결과를 보였습니다.
이는 Redis 네트워크 왕복 오버헤드 때문이며, 서버 수가 증가하고 DB 커넥션 풀이 병목이 되는 대규모 환경에서는 분산 락이 더 효율적입니다.

---

### 3. 분산 락 - 단일 서버 vs 다중 서버

**테스트 환경**

| 항목 | 내용 |
|---|---|
| 부하 도구 | K6 |
| 동시 사용자 | 100명 |
| 테스트 시간 | 10s |
| 락 방식 | 분산 락 (Redisson) |

**결과**

| | 단일 서버 | 다중 서버 (2대) |
|---|---|---|
| 총 요청 수 | 633건 | 806건 |
| TPS | 30.33건/s | 73.36건/s ✅ |
| 평균 응답시간 | 55.48ms | 283.63ms |
| p(95) | 454.39ms | 1.37s |
| 에러율 | 0% | 0% |

**분석**

다중 서버(2대)에서 TPS가 단일 서버 대비 약 2.4배(30.33 → 73.36건/s) 향상되었습니다.
다만 Redis 락 경쟁으로 인해 p(95) 응답시간은 다중 서버가 더 길게 측정되었습니다.
이는 동시 요청 100명이 단일 Redis 락을 경쟁하는 구조적 특성이며, 실제 대규모 트래픽 환경에서는 단일 서버가 CPU, 메모리, DB 커넥션 한계에 도달하는 반면 다중 서버는 수평 확장(Scale Out)으로 처리량과 응답시간 모두 우위를 가집니다.

---

## 기술적 선택 이유

### 1. @TransactionalEventListener 선택

결제 트랜잭션 안에서 Kafka 이벤트를 직접 발행하면 커밋 실패 시 DB는 롤백되지만 Kafka 메시지는 이미 발행된 상태가 됩니다.
이를 방지하기 위해 `@TransactionalEventListener(AFTER_COMMIT)` 을 활용하여 커밋이 확인된 후에만 Kafka 이벤트를 발행합니다.

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handle(OrderCompletedEvent event) {
    orderProducer.send(event);
}
```

| 상황 | 결과 |
|---|---|
| 커밋 성공 | AFTER_COMMIT 감지 → Kafka 발행 ✅ |
| 커밋 실패 (롤백) | 이벤트 버려짐 → Kafka 발행 안 됨 ✅ |

---

### 2. Redis ZSET 선택 (인기 메뉴 랭킹)

RDB `GROUP BY / ORDER BY` 집계 쿼리는 주문이 쌓일수록 성능이 저하됩니다.
Redis ZSET을 활용하여 주문 완료 시 score를 증가시키고 조회 시 바로 Top 3를 반환합니다.

```
주문 완료 시 → ZINCRBY menu:ranking:{날짜} 1 {menuId}
7일 조회 시  → ZUNIONSTORE tempKey [7일치 키]
              → ZREVRANGE tempKey 0 2 → Top 3 반환
```

| | RDB 집계 쿼리 | Redis ZSET |
|---|---|---|
| 응답 속도 | 주문 증가 시 느려짐 | O(log N) 일정 |
| 실시간성 | 쿼리 실행 시점 | 주문 즉시 반영 |
| 서버 부하 | DB 부하 높음 | DB 부하 없음 |

---

### 3. tempKey TTL 10분 설정

7일치 ZSET을 `ZUNIONSTORE` 로 합산하는 연산은 매 요청마다 수행하면 부하가 발생합니다.
합산 결과를 tempKey에 저장하고 TTL 10분을 설정하여 10분 동안 동일한 결과를 재사용합니다.

```
첫 조회 → ZUNIONSTORE 연산 → tempKey 저장 (TTL 10분)
10분 내 재조회 → tempKey에서 바로 반환 (연산 없음)
10분 후 → tempKey 만료 → 다시 ZUNIONSTORE 연산
```

> 10분마다 최신 주문이 반영되어 정확도와 성능의 균형을 맞췄습니다. 트래픽에 따라 TTL 조절이 가능합니다.

---

### 4. Redis 캐시 (메뉴 목록)

변경 빈도가 낮은 메뉴 목록에 Redis 캐싱을 적용했습니다.
카페인(로컬 캐시) 대신 Redis를 선택한 이유는 다중 서버 환경에서 모든 인스턴스가 동일한 캐시를 공유하기 위함입니다.

```
카페인 (로컬 캐시)
→ app-1 캐싱 → app-2는 모름 → DB 중복 조회 

Redis 캐시
→ app-1 캐시 미스(DB 조회) → app-2 Redis 캐시 히트 (DB 조회X) 
```

**다중 서버 캐시 공유 확인**

```
app-1 첫 조회 → [Cache MISS] DB 조회 → Redis에 menus::all 저장
app-1 두 번째 조회 → [Cache HIT] Redis에서 바로 반환 (DB 조회 없음) ✅
app-2 세 번째 조회 → [Cache HIT] Redis에서 바로 반환 (DB 조회 없음) ✅
```

---

### 5. DLT (Dead Letter Topic)

Consumer 메시지 처리 실패 시 메시지 유실을 방지하기 위해 DLT를 적용했습니다.

**처리 흐름**

```
정상
order-completed → Consumer → 외부 플랫폼 전송 ✅

실패
order-completed → Consumer → 예외 💥
                           → 재시도 1회 (1초 후)
                           → 재시도 2회 (1초 후)
                           → 재시도 3회 (1초 후)
                           → order-completed.DLT → 메시지 보존 ✅
```

| | DLT 없음 | DLT 있음 |
|---|---|---|
| 처리 실패 시 | 메시지 유실 💥 | DLT로 이동 ✅ |
| 추적 가능 여부 | ❌ | ✅ |
| 재처리 가능 여부 | ❌ | ✅ |
