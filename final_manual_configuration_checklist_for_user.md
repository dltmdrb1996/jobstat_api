# 최종 수동 설정 체크리스트: jobstat-api k3d 배포

이 문서는 `jobstat-api` 프로젝트를 `k3d` 클러스터에 배포하기 위해 사용자께서 **최종적으로 직접 확인하고 수정해야 할 모든 부분**을 안내합니다. 아래의 항목들을 꼼꼼히 확인하고 본인의 환경에 맞게 변경해야 합니다.

---

### **1. GitHub Repository 설정 (필수 수동 작업)**

*   GitHub Repository의 `Settings` > `Secrets and variables` > `Actions`로 이동하여 아래의 Secret들을 정확한 값으로 등록해야 합니다. 이 Secret들은 CI/CD 파이프라인에서 민감 정보를 안전하게 주입하는 데 사용됩니다.
    *   `DOCKER_USERNAME`
    *   `DOCKER_PASSWORD`
    *   `ADMIN_PASSWORD`
    *   `ADMIN_USERNAME`
    *   `CORS_ALLOWED_ORIGINS`
    *   `DB_PASSWORD`
    *   `DB_USERNAME`
    *   `DDNS_DOMAIN`
    *   `GMAIL_ID`
    *   `GMAIL_PASSWORD`
    *   `JWT_SECRET`
    *   `KAFKA_SERVERS`
    *   `MONGO_HOST`
    *   `MONGO_PASSWORD`
    *   `MONGO_USERNAME`
    *   `REDIS_HOST`
    *   `REDIS_PASSWORD`
    *   `REDIS_USERNAME`
    *   `SENTRY_AUTH_TOKEN`
    *   `SENTRY_DSN`
    *   `GRAFANA_ADMIN_USER`
    *   `GRAFANA_ADMIN_PASSWORD`

#### **3. Kubernetes YAML 파일 (`k8s` 디렉터리) 설정 (수동 확인 및 조정 필요)**

*   **이미지 경로의 `YOUR_GITHUB_USERNAME` 변경 (수동 권장)**:
    *   모든 `Deployment` YAML 파일 (`k8s/base/**/*.yaml`) 내의 `image` 필드에 있는 `ghcr.io/YOUR_GITHUB_USERNAME/` 부분을 **본인의 실제 GitHub 사용자 이름**으로 변경해야 합니다. (예시: `ghcr.io/my-github-user/jobstat-api-gateway:latest`)
    *   **영향받는 파일 목록:**
        *   `k8s/base/infra/api-gateway.yaml`
        *   `k8s/base/infra/config-server.yaml`
        *   `k8s/base/infra/eureka.yaml`
        *   `k8s/base/services/auth.yaml`
        *   `k8s/base/services/community.yaml`
        *   `k8s/base/services/community_read.yaml`
        *   `k8s/base/services/notification.yaml`
        *   `k8s/base/services/statistics_read.yaml`
*   **Exporter 설정 확인 및 조정 (수동 확인 필요)**:
    *   `k8s/base/infra/monitoring/mysql-exporter-deployment.yaml`: `DATA_SOURCE_NAME`의 `value` 필드가 `prod_jobstat`으로 변경되었음을 확인하고, `DB_USERNAME` Secret이 올바르게 설정되었는지 확인합니다.
    *   `k8s/base/infra/monitoring/mongodb-exporter-deployment.yaml`: `--mongodb.uri` 명령줄 인자의 `value` 필드가 `prod_database`로 변경되었음을 확인하고, `MONGO_USERNAME`과 `MONGO_PASSWORD` Secret이 올바르게 설정되었는지 확인합니다.
    *   `k8s/base/infra/monitoring/redis-exporter-deployment.yaml`: `REDIS_USER`와 `REDIS_PASSWORD` 환경 변수가 `common-secrets`에서 주입받도록 설정되었음을 확인하고, `REDIS_USERNAME`과 `REDIS_PASSWORD` Secret이 올바르게 설정되었는지 확인합니다.
    *   `k8s/base/infra/monitoring/grafana-deployment.yaml`: `GF_SECURITY_ADMIN_USER`와 `GF_SECURITY_ADMIN_PASSWORD` 환경 변수가 `common-secrets`에서 주입받도록 설정되었음을 확인하고, `GRAFANA_ADMIN_USER`와 `GRAFANA_ADMIN_PASSWORD` Secret이 올바르게 설정되었는지 확인합니다.
*   **서비스 포트 최종 확인 (수동 확인 필요)**:
    *   각 서비스의 `Deployment` 및 `Service` YAML 파일에 명시된 `containerPort`, `port`, `targetPort`가 해당 서비스의 `application.yml` (또는 `config-repo`의 프로파일별 YML)에 정의된 `server.port`와 일치하는지 **반드시 최종 확인**해야 합니다.
    *   **파악된 포트 정보:**
        *   `jobstat-api-gateway`: 8000
        *   `jobstat-config-server`: 8888
        *   `jobstat-eureka-server`: 8761
        *   `jobstat-auth`: 8082
        *   `jobstat-community`: 8083
        *   `jobstat-community-read`: 8084
        *   `jobstat-notification`: 8086
        *   `jobstat-statistics-read`: 8085

#### **4. `infra/config-repo` 파일 설정 (수동 확인 필요)**

*   **`*-dev.yml` 파일들 (수동 확인 필요)**:
    *   제가 `infra/config-repo` 내의 모든 `*-dev.yml` 파일에서 `localhost` 참조를 쿠버네티스 내부 서비스 이름(예: `jobstat-eureka-server`) 또는 `host.k3d.internal`로 변경했습니다. 이 변경사항이 올바르게 반영되었는지 **최종 확인**해야 합니다.
*   **`application.yml` (수동 확인 필요)**:
    *   제가 `infra/config-repo/application.yml` 파일의 `localhost` 기본값들을 `eureka-service` 또는 `host.k3d.internal`로 변경했습니다. 이 변경사항이 올바르게 반영되었는지 **최종 확인**해야 합니다.
*   **`*-prod.yml` 파일들**: 이 파일들은 이미 쿠버네티스 내부 서비스 이름 또는 `host.k3d.internal`을 참조하도록 설정되어 있으며, 제가 추가적인 변경을 수행하지 않았습니다.

#### **5. 개별 서비스 `application.yml` 파일 (수동 확인 필요)**

*   프로젝트 내의 각 서비스 모듈 (`service/jobstat-*/src/main/resources/application.yml` 및 `infra/jobstat-*/src/main/resources/application.yml`)에 있는 `application.yml` 파일들을 검토하여, `localhost` 또는 하드코딩된 IP 주소 참조가 남아있는지 확인해야 합니다.
*   만약 `localhost`나 특정 IP 주소가 발견된다면, 이를 쿠버네티스 내부 서비스 이름(예: `jobstat-eureka-server`) 또는 `host.k3d.internal`과 같은 동적인 값으로 변경하는 것을 **권장**합니다. 이는 특히 개발 환경이나 테스트 환경에서 유연성을 높이는 데 도움이 됩니다.
