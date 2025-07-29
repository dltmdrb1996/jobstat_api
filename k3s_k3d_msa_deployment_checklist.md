### **호스트 OS에 k3s/k3d 기반 MSA 배포를 위한 최종 수동 체크리스트**

네, 현재 사용자님의 호스트 OS에 k3s/k3d 기반 MSA를 배포하기 위해 **남은 수동 작업**은 다음과 같습니다. 제가 자동화한 부분들을 제외하고, 사용자께서 직접 확인하고 조치해야 할 핵심 사항들입니다.

---

#### **1. GitHub Repository 설정 (필수)**

*   **GitHub Secrets 등록**: GitHub Repository의 `Settings` > `Secrets and variables` > `Actions`에 아래의 Secret들을 정확한 값으로 등록해야 합니다.
    *   `DOCKERHUB_USERNAME`: **본인의 Docker Hub 사용자 이름**
    *   `DOCKERHUB_TOKEN`: **본인의 Docker Hub Personal Access Token (PAT)**
    *   그 외 `final_manual_configuration_checklist_for_user.md` 파일에 명시된 모든 Secret들 (예: `ADMIN_PASSWORD`, `DB_PASSWORD`, `JWT_SECRET`, `GRAFANA_ADMIN_USER`, `GRAFANA_ADMIN_PASSWORD` 등).

#### **2. Kubernetes YAML 파일 (`k8s` 디렉터리) 수정 (필수)**

*   **이미지 경로 플레이스홀더 교체**: 제가 `k8s/base/**/*.yaml` 파일들의 이미지 경로를 `lsg1996/`으로 변경해 두었습니다. 이 `lsg1996` 플레이스홀더를 **본인의 실제 Docker Hub 사용자 이름**으로 **수동으로 모두 교체**해야 합니다.
    *   **영향받는 파일**: `k8s/base/infra/api-gateway.yaml`, `k8s/base/infra/config-server.yaml`, `k8s/base/infra/eureka.yaml`, `k8s/base/services/auth.yaml`, `k8s/base/services/community.yaml`, `k8s/base/services/community_read.yaml`, `k8s/base/services/notification.yaml`, `k8s/base/services/statistics_read.yaml`

#### **3. `docker-compose-infra.yml` 설정 (선택적, 로컬 인프라 사용 시)**

*   **MySQL 설정 확인**: `MYSQL_ROOT_PASSWORD`가 GitHub Secret의 `DB_PASSWORD`와 동일한 값인지 확인하고 필요시 조정합니다. `MYSQL_DATABASE` 이름도 확인합니다.
*   **MongoDB 사용자/비밀번호 생성**: `docker-compose-infra.yml`을 통해 MongoDB를 띄울 경우, `MONGO_USERNAME`, `MONGO_PASSWORD`에 해당하는 사용자/비밀번호가 MongoDB 컨테이너 내에 생성되는지 확인하거나, 필요시 초기화 스크립트 등을 추가해야 합니다.
*   **Redis 비밀번호 설정**: `docker-compose-infra.yml`을 통해 Redis를 띄울 경우, `REDIS_PASSWORD` 환경 변수를 추가하여 비밀번호를 설정해야 합니다.

#### **4. 호스트 OS 인프라 서비스 설정 (필수)**

*   **방화벽 허용**: 호스트 OS의 방화벽(macOS, Windows, Linux)에서 MySQL(3306), MongoDB(27017), Redis(6379), Kafka(9092) 포트의 인바운드 연결을 허용해야 합니다.
*   **바인딩 주소 확인**: 호스트 OS에 직접 설치된 MySQL, MongoDB, Redis가 `127.0.0.1`(localhost)이 아닌 **`0.0.0.0` (모든 인터페이스) 또는 호스트 머신의 실제 네트워크 IP 주소에 바인딩**되어 외부 연결을 허용하는지 확인해야 합니다.
*   **Kafka `advertised.listeners` 설정**: 호스트 OS의 Docker에 설치된 Kafka 브로커의 `advertised.listeners` 설정이 `PLAINTEXT://host.k3d.internal:9092`와 같이 k3d 클러스터 내부에서 접근 가능한 주소로 설정되어 있는지 **반드시 확인**해야 합니다.

#### **5. 설정 파일 최종 검토 (권장)**

*   **Exporter 설정 확인**: `k8s/base/infra/monitoring` 디렉터리 내의 익스포터 YAML 파일들(`mysql-exporter-deployment.yaml`, `mongodb-exporter-deployment.yaml`, `redis-exporter-deployment.yaml`, `grafana-deployment.yaml`)에서 Secret 참조(`DB_USERNAME`, `MONGO_USERNAME`, `REDIS_USERNAME`, `GRAFANA_ADMIN_USER` 등)가 올바르게 되어 있는지 최종 확인합니다.
*   **`infra/config-repo` 파일 검토**: 제가 변경한 `*-dev.yml` 파일들과 `application.yml` 파일의 `localhost` 참조가 쿠버네티스 서비스 이름 또는 `host.k3d.internal`로 올바르게 변경되었는지 최종 확인합니다.
*   **개별 서비스 `application.yml` 파일 검토**: 제가 변경한 `CONFIG_SERVER_URI`/`CONFIG_SERVER_URI`이 `http://jobstat-config-server:8888`로 올바르게 변경되었는지 최종 확인합니다.

---

이 목록을 모두 완료하시면, k3d 클러스터를 생성하고 MSA를 배포할 준비가 완료됩니다.
