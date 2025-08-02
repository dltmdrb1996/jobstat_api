# CQRS 동기화 문제 해결 계획

**목표:** `jobstat-community` (Write)와 `jobstat-community_read` (Read) 서비스 간의 데이터 동기화 과정에서 발생할 수 있는 데이터 불일치 및 유실 위험을 최소화하고, 운영 가시성을 확보합니다.

---

## Phase 1: 데이터 일관성 및 가시성 확보 (최우선 순위)

### 1.1. Read 서비스 이벤트 핸들러 멱등성 강화
*   **문제점:** `AbstractEventConsumer`의 `markAsProcessed` 실패 시, 이미 처리된 이벤트가 재시도되어 Read 모델에 데이터 중복을 야기할 수 있습니다.
*   **해결 방안:** `jobstat-community_read` 서비스 내의 모든 이벤트 핸들러가 완벽한 멱등성을 갖도록 구현합니다.
    *   **세부 계획:**
        1.  **영향 분석:** `jobstat-community_read` 서비스의 `src/main/kotlin` 디렉토리 내에서 `EventHandlerRegistryService`에 등록된 모든 이벤트 핸들러 클래스를 식별합니다.
        2.  **핸들러별 멱등성 구현:** 각 핸들러의 비즈니스 로직을 검토하여, 동일한 이벤트가 여러 번 수신되더라도 Read 모델의 최종 상태가 일관되도록 수정합니다.
            *   **예시:**
                *   **생성(Create) 이벤트:** `INSERT` 대신 `UPSERT` (또는 `INSERT ... ON CONFLICT UPDATE`)를 사용하거나, 레코드 존재 여부를 먼저 확인 후 생성합니다.
                *   **업데이트(Update) 이벤트:** 레코드의 버전 필드를 추가하여, 수신된 이벤트의 버전이 현재 저장된 레코드의 버전보다 높거나 같을 때만 업데이트를 수행합니다.
                *   **증가/감소(Increment/Decrement) 이벤트:** Redis의 `INCRBY`와 같은 원자적 연산을 사용하거나, 데이터베이스에서 `UPDATE ... SET count = count + 1 WHERE ...`와 같이 조건부 업데이트를 사용합니다.
        3.  **테스트 코드 추가:** 각 이벤트 핸들러에 대해 멱등성을 검증하는 단위/통합 테스트를 추가합니다. 동일한 이벤트를 여러 번 발행하여 Read 모델의 최종 상태가 예상대로 한 번만 반영되었는지 확인합니다.
    *   **예상 변경 파일:** `service/jobstat-community_read/src/main/kotlin/**/*.kt` (이벤트 핸들러 파일들)

### 1.2. DLT 모니터링 및 알림 시스템 구축
*   **문제점:** Write 서비스의 Outbox DLT 및 Read 서비스 컨슈머 DLT에 메시지가 쌓여도 알림이 없어 운영상 사각지대가 발생합니다.
*   **해결 방안:** 모든 DLT에 대한 중앙 집중식 모니터링 및 알림 시스템을 구축합니다.
    *   **세부 계획:**
        1.  **DLT 토픽 식별:**
            *   `jobstat-community`의 Outbox DLT (예: `community-command.DLT`)
            *   `jobstat-community_read`의 컨슈머 DLT (예: `community-command.DLT`, `community-read.DLT`)
        2.  **모니터링 연동:** Prometheus, Grafana, Sentry 등 현재 사용 중인 모니터링 스택에 Kafka DLT의 메시지 수를 모니터링하는 지표를 추가합니다.
        3.  **알림 설정:**
            *   DLT에 메시지가 일정 임계치(예: 1개 이상) 이상 쌓이거나, 특정 기간 동안 처리되지 않은 메시지가 감지될 경우 즉각적인 알림(Slack, Email 등)을 발생시키도록 Alertmanager를 설정합니다.
            *   `OutboxProcessor`에서 DLT 전송 실패 로그(`치명적 오류: Outbox 실패 메시지 DLT 전송 실패!`)가 발생할 경우 Sentry를 통해 즉시 알림이 오도록 설정합니다.
        4.  **운영 프로세스 정의:** DLT 알림 발생 시 메시지를 분석하고 재처리하거나 수동 개입하는 운영 프로세스를 문서화합니다.
    *   **예상 변경 파일:** 모니터링 시스템 설정 파일 (프로젝트 외부에 있을 수 있음), `jobstat-community` 및 `jobstat-community_read`의 로깅/모니터링 관련 설정 (필요시).

---

## Phase 2: Write 서비스 견고성 강화 (높은 우선순위)

### 2.1. DLT 전송 실패 시 처리 로직 개선
*   **문제점:** DLT 전송 자체마저 실패할 경우, `Outbox` 레코드가 무한 루프에 빠지거나 영구적으로 처리되지 못할 수 있습니다.
*   **해결 방안:** `OutboxProcessor`에서 DLT 전송 실패 시 `Outbox` 레코드의 상태를 명확히 업데이트하고, 스케줄러가 더 이상 재시도하지 않도록 처리합니다.
    *   **세부 계획:**
        1.  **`Outbox` 엔티티 확장:** `Outbox` 엔티티에 `status` 필드(예: `PENDING`, `SENT`, `DLT_FAILED`)와 `dltRetryCount` 필드를 추가합니다.
        2.  **`OutboxProcessor` 수정:**
            *   `attemptDltSendAndUpdateStatus` 메소드에서 DLT 전송 실패 시, `Outbox` 레코드의 `status`를 `DLT_FAILED`로 변경하고 `dltRetryCount`를 증가시킨 후 저장합니다. (현재 `throw RuntimeException` 대신)
            *   `processPendingOutboxMessagesByScheduler` 메소드에서 `Outbox` 레코드를 조회할 때 `status`가 `DLT_FAILED`인 레코드는 제외하도록 쿼리를 수정합니다.
            *   `DLT_FAILED` 상태의 메시지에 대한 별도의 알림 로직을 추가합니다 (Phase 1.2와 연계).
        3.  **수동 개입 프로세스:** `DLT_FAILED` 상태의 메시지에 대해 수동으로 재전송을 시도하거나, 영구 삭제하는 관리 도구를 고려합니다.
    *   **예상 변경 파일:**
        *   `core/core_event/src/main/kotlin/com/wildrew/jobstat/core/core_event/outbox/Outbox.kt`
        *   `core/core_event/src/main/kotlin/com/wildrew/jobstat/core/core_event/outbox/OutboxProcessor.kt`
        *   `core/core_event/src/main/kotlin/com/wildrew/jobstat/core/core_event/outbox/OutboxRepository.kt` (필요시)

### 2.2. Outbox 테이블 클린업 정책 수립
*   **문제점:** Kafka 전송 성공 후 `Outbox` 레코드 삭제 실패 시, 또는 DLT 전송 실패로 인해 `Outbox` 레코드가 테이블에 계속 쌓여 과도하게 커질 수 있습니다.
*   **해결 방안:** `outbox` 테이블에 남아있는 오래된 레코드에 대한 주기적인 클린업 정책을 수립하고 자동화합니다.
    *   **세부 계획:**
        1.  **클린업 스케줄러 구현:** `jobstat-community` 서비스 내에 주기적으로 실행되는 스케줄러를 구현합니다.
        2.  **클린업 로직:**
            *   성공적으로 Kafka로 전송되었으나 삭제되지 않은 레코드 (예: `createdAt`이 오래되었고 `retryCount`가 낮은 레코드)를 삭제합니다.
            *   `DLT_FAILED` 상태로 마킹된 레코드 중 특정 기간(예: 1주일)이 지난 레코드를 삭제합니다. (삭제 전 운영팀의 확인/승인 프로세스 필요)
        3.  **모니터링:** `outbox` 테이블의 크기와 레코드 수를 주기적으로 모니터링하여 클린업 정책의 효과를 검증합니다.
    *   **예상 변경 파일:** `service/jobstat-community/src/main/kotlin/com/wildrew/jobstat/community/config/SchedulerConfig.kt` (새로운 스케줄러 추가), 새로운 클린업 서비스/리포지토리 파일.

---

## Phase 3: 운영 개선 및 문서화 (중간 우선순위)

### 3.1. `idempotencyChecker` 가용성 및 성능 강화
*   **문제점:** `idempotencyChecker` (Redis 기반으로 추정)의 성능이나 가용성 문제가 전체 컨슈머 처리량에 영향을 주거나 중복 처리 시나리오를 더 자주 발생시킬 수 있습니다.
*   **해결 방안:** `idempotencyChecker`의 인프라 및 코드적 개선을 고려합니다.
    *   **세부 계획:**
        1.  **Redis 인프라 검토:** Redis 클러스터링, 고가용성 구성, 성능 모니터링을 강화합니다.
        2.  **코드 최적화:** `idempotencyChecker`의 Redis 접근 로직을 최적화하고, Redis 연결 풀 설정 등을 검토합니다.
        3.  **장애 시나리오 테스트:** Redis 장애 시 `idempotencyChecker`의 동작과 컨슈머의 재시도/DLT 전송이 올바르게 이루어지는지 테스트합니다.
    *   **예상 변경 파일:** Redis 관련 설정 파일, `core/core_event/src/main/kotlin/com/wildrew/jobstat/core/core_event/consumer/IdempotencyChecker.kt` (구현체)

### 3.2. 문서화 및 Runbook 작성
*   **문제점:** 문제 발생 시 대응 절차가 명확하지 않을 수 있습니다.
*   **해결 방안:** 모든 문제 해결 방안 및 운영 절차를 문서화합니다.
    *   **세부 계획:**
        1.  **CQRS 동기화 아키텍처 문서 업데이트:** Outbox Pattern, Kafka, DLT, 멱등성 구현에 대한 상세 아키텍처 문서를 업데이트합니다.
        2.  **운영 Runbook 작성:**
            *   DLT 메시지 발생 시 처리 절차
            *   `outbox` 테이블 이상 감지 시 처리 절차
            *   `idempotencyChecker` 장애 시 대응 절차
            *   각 알림에 대한 대응 가이드라인
    *   **예상 변경 파일:** `docs/` 디렉토리 내 문서 파일 (새로운 파일 생성 또는 기존 파일 업데이트).

---
