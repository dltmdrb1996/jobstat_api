# 최종 마이그레이션 가이드 V5: GitOps CI/CD on k3d (프로젝트 맞춤형)

**문서 목표:** 이 문서는 `jobstat-api` 프로젝트를 로컬 `k3d` 클러스터에 배포하기 위한 **자동화된 CI/CD 파이프라인 구축 가이드**입니다. GitHub Actions와 GitHub Secrets를 활용하여, `main` 브랜치 푸시부터 쿠버네티스 배포까지의 과정을 자동화하는 방법을 상세히 설명합니다.

**핵심 아키텍처:**
1.  **Secret 관리:** 모든 민감 정보는 **GitHub Secrets**에 중앙 저장하고, 배포 시 동적으로 쿠버네티스 Secret 객체를 생성합니다.
2.  **컨테이너 레지스트리:** 빌드된 이미지는 **GitHub Container Registry(GHCR)**에 저장 및 관리합니다.
3.  **CI/CD 파이프라인:** GitHub Actions 워크플로우가 **빌드, 테스트, 이미지 푸시, 쿠버네티스 배포**의 전 과정을 책임집니다.
4.  **모니터링 스택:** Prometheus, Grafana 등 모니터링 스택은 k3d 클러스터 내부에 배포하여 애플리케이션과 함께 관리합니다.

---

## Part 1: 사전 준비 및 환경 설정

### **1단계: GitHub Repository 설정**

1.  **GitHub Container Registry(GHCR) 활성화:**
    *   Repository 우측의 "Packages" 탭으로 이동하여 GHCR을 활성화합니다.

2.  **GitHub Secrets 등록:**
    *   Repository의 `Settings` > `Secrets and variables` > `Actions`로 이동하여 아래의 Secret들을 등록합니다.
    *   **`DOCKER_USERNAME`**: 본인의 GitHub 사용자 이름을 입력합니다.
    *   **`DOCKER_PASSWORD`**: GHCR에 접근하기 위한 Personal Access Token(PAT)을 생성하여 입력합니다.
        *   PAT 생성 가이드: [GitHub Docs](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens)
        *   PAT 생성 시 `write:packages`, `read:packages` 스코프를 반드시 포함해야 합니다.
    *   **`DB_PASSWORD`**: 데이터베이스 비밀번호를 입력합니다. (예: `password`)
    *   **`JWT_SECRET`**: JWT 시크릿 키를 입력합니다. (예: `your-super-strong-jwt-secret-key`)
    *   **`MONGO_USERNAME`**: MongoDB 사용자 이름을 입력합니다. (예: `dltmdrb1996`)
    *   **`MONGO_PASSWORD`**: MongoDB 비밀번호를 입력합니다. (예: `4181016aA!`)
    *   **`REDIS_PASSWORD`**: Redis 비밀번호를 입력합니다. (예: `4181016aA!`)
    *   **`GMAIL_ID`**: Gmail ID를 입력합니다. (예: `your-gmail-id@gmail.com`)
    *   **`GMAIL_PASSWORD`**: Gmail 앱 비밀번호를 입력합니다. (일반 비밀번호가 아님)
    *   **`ADMIN_USERNAME`**: 관리자 사용자 이름을 입력합니다.
    *   **`ADMIN_PASSWORD`**: 관리자 비밀번호를 입력합니다.

### **2단계: 로컬 환경 준비**

1.  **필수 도구 설치:**
    *   **Docker:** `k3d`의 기반이 되는 컨테이너 런타임입니다. [공식 설치 가이드](https://docs.docker.com/get-docker/)
    *   **kubectl:** 쿠버네티스 클러스터와 상호작용하기 위한 CLI 도구입니다. [공식 설치 가이드](https://kubernetes.io/docs/tasks/tools/install-kubectl/)
    *   **k3d:** 도커를 사용하여 경량 쿠버네티스 클러스터를 관리하는 핵심 도구입니다. [공식 설치 가이드](https://k3d.io/v5.6.0/#installation)
    *   **Git:** 모든 코드와 설정 파일을 관리합니다.

2.  **쿠버네티스 설정 디렉터리 구조 생성:**
    프로젝트 루트에서 다음 명령어를 실행하여, 쿠버네티스 리소스 파일을 체계적으로 관리할 디렉터리 구조를 생성합니다.

    ```bash
    # 이 명령어는 k8s 리소스의 base와 환경별 overlay를 분리하는 Kustomize의 표준 구조를 따릅니다.
    mkdir -p k8s/base/infra/monitoring k8s/base/services k8s/overlays/local-k3d
    ```

3.  **데이터 인프라 실행 (Host OS + Docker Compose):**
    프로젝트 루트에 `docker-compose-infra.yml` 파일을 생성하고 아래 내용을 붙여넣습니다. 이 파일은 상태를 가지는 모든 데이터 저장소(DB, Redis, MongoDB, Zookeeper, Kafka)를 정의합니다. **모니터링 스택은 이제 k3d 클러스터에서 관리됩니다.**

    ```yaml
    # File: docker-compose-infra.yml
    version: '3.8'
    services:
      mysql:
        image: mysql:8.0
        container_name: mysql-host
        ports: ["3306:3306"]
        volumes: ["mysql_data:/var/lib/mysql"]
        environment:
          - MYSQL_ROOT_PASSWORD=password
          - MYSQL_DATABASE=dev_jobstat
        restart: unless-stopped

      redis:
        image: redis:7
        container_name: redis-host
        ports: ["6379:6379"]
        restart: unless-stopped

      mongodb:
        image: mongo:6.0
        container_name: mongodb-host
        ports: ["27017:27017"]
        volumes: ["mongo_data:/data/db"]
        restart: unless-stopped

      zookeeper:
        image: confluentinc/cp-zookeeper:7.4.0
        container_name: zookeeper-host
        ports: ["2181:2181"]
        environment:
          ZOOKEEPER_CLIENT_PORT: 2181
          ZOOKEEPER_TICK_TIME: 2000
        restart: unless-stopped

      kafka:
        image: confluentinc/cp-kafka:7.4.0
        container_name: kafka-host
        ports: ["9092:9092"]
        environment:
          KAFKA_BROKER_ID: 1
          KAFKA_ZOOKEEPER_CONNECT: 'zookeeper-host:2181'
          KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://host.k3d.internal:9092
          KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
        depends_on: [zookeeper]
        restart: unless-stopped

    volumes:
      mysql_data:
      mongo_data:
    ```

    이제 터미널에서 아래 명령어로 모든 데이터 인프라를 백그라운드에서 실행합니다.

    ```bash
    docker-compose -f docker-compose-infra.yml up -d
    ```

    `docker ps` 명령어로 모든 컨테이너(`mysql-host`, `redis-host`, `mongodb-host`, `zookeeper-host`, `kafka-host`)가 정상 실행 중인지 확인합니다.

4.  **k3d 클러스터 생성 (애플리케이션 계층):**
    1개의 컨트롤 플레인과 2개의 워커 노드를 가진 쿠버네티스 클러스터를 생성합니다.

    ```bash
    # --port "8080:80@loadbalancer": 호스트의 8080 포트를 k3d의 내장 로드밸런서 80 포트로 포워딩합니다.
    # --k3s-arg "--disable=traefik@server:0": 기본 Ingress Controller인 Traefik을 비활성화합니다. (Nginx Ingress를 직접 설치하기 위함)
k3d cluster create my-msa-cluster --servers 1 --agents 2 --port "8080:80@loadbalancer" --k3s-arg "--disable=traefik@server:0"
    ```

    `kubectl get nodes` 명령어로 3개의 노드가 모두 `Ready` 상태인지 확인합니다.

---

## Part 2: CI/CD 파이프라인 구축

### **3단계: GitHub Actions 워크플로우 파일 작성**

`.github/workflows/` 디렉터리에 아래의 두 워크플로우 파일을 생성합니다.

*   **`.github/workflows/ci.yml` (Continuous Integration)**
    *   이 워크플로우는 `main` 브랜치에 코드가 푸시될 때마다, 프로젝트 전체를 빌드하고 테스트를 실행하여 코드의 정합성을 검증합니다.

    ```yaml
    # File: .github/workflows/ci.yml
    name: CI - Build and Test

    on:
      push:
        branches: [ main ]
      pull_request:
        branches: [ main ]

    jobs:
      build:
        runs-on: ubuntu-latest
        steps:
        - name: Checkout code
          uses: actions/checkout@v4

        - name: Set up JDK 17
          uses: actions/setup-java@v4
          with:
            java-version: '17'
            distribution: 'temurin'

        - name: Setup Gradle
          uses: gradle/actions/setup-gradle@v3

        - name: Build with Gradle
          run: ./gradlew build

    ```

*   **`.github/workflows/cd.yml` (Continuous Deployment)**
    *   이 워크플로우는 수동으로 실행(`workflow_dispatch`)할 수 있으며, **빌드, 이미지 푸시, 쿠버네티스 배포**의 모든 과정을 자동화합니다.

    ```yaml
    # File: .github/workflows/cd.yml
    name: CD - Build, Push and Deploy to k3d

    on:
      workflow_dispatch:

    jobs:
      build-and-deploy:
        runs-on: ubuntu-latest
        steps:
        - name: Checkout code
          uses: actions/checkout@v4

        - name: Set up JDK 17
          uses: actions/setup-java@v4
          with:
            java-version: '17'
            distribution: 'temurin'

        - name: Setup Gradle
          uses: gradle/actions/setup-gradle@v3

        - name: Login to GitHub Container Registry
          uses: docker/login-action@v3
          with:
            registry: ghcr.io
            username: ${{ secrets.DOCKER_USERNAME }}
            password: ${{ secrets.DOCKER_PASSWORD }}

        - name: Build and Push Images
          run: |
            # 이미지 태그를 위한 커밋 해시
            IMAGE_TAG=${GITHUB_SHA::7}
            
            # 서비스 모듈 목록
            MODULES=(
              "infra/jobstat-eureka-server" "infra/jobstat-api-gateway" "infra/jobstat-config-server"
              "service/jobstat-auth" "service/jobstat-community" "service/jobstat-community_read"
              "service/jobstat-notification" "service/jobstat-statistics_read"
            )

            for module in "${MODULES[@]}"; do
              service_name=$(basename "$module" | sed 's/jobstat-//')
              image_name="ghcr.io/${{ secrets.DOCKER_USERNAME }}/jobstat-${service_name}:${IMAGE_TAG}"
              
              echo "Building and pushing ${image_name}"
              ./gradlew ":${module}:bootBuildImage" --imageName="${image_name}"
            done

        - name: Install k3d
          run: |
            curl -s https://raw.githubusercontent.com/k3d-io/k3d/main/install.sh | bash

        - name: Install kubectl
          run: |
            curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
            sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

        - name: Deploy to k3d
          run: |
            # 이 부분은 실제 운영 환경에서는 클라우드 프로바이더의 kubeconfig를 설정하는 로직으로 대체됩니다.
            # 여기서는 로컬 k3d 클러스터에 배포하는 시나리오를 가정합니다.
            # GitHub Actions Runner에서 k3d 클러스터를 생성하고 배포합니다.
            k3d cluster create my-msa-cluster --servers 1 --agents 2 --port "8080:80@loadbalancer" --k3s-arg "--disable=traefik@server:0"
            kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.10.0/deploy/static/provider/k3s/deploy.yaml
            
            # GitHub Secrets를 사용하여 쿠버네티스 Secret 동적 생성
            kubectl create secret generic common-secrets \
              --from-literal=DB_PASSWORD='${{ secrets.DB_PASSWORD }}' \
              --from-literal=JWT_SECRET='${{ secrets.JWT_SECRET }}' \
              --from-literal=MONGO_USERNAME='${{ secrets.MONGO_USERNAME }}' \
              --from-literal=MONGO_PASSWORD='${{ secrets.MONGO_PASSWORD }}' \
              --from-literal=REDIS_PASSWORD='${{ secrets.REDIS_PASSWORD }}' \
              --from-literal=GMAIL_ID='${{ secrets.GMAIL_ID }}' \
              --from-literal=GMAIL_PASSWORD='${{ secrets.GMAIL_PASSWORD }}' \
              --from-literal=ADMIN_USERNAME='${{ secrets.ADMIN_USERNAME }}' \
              --from-literal=ADMIN_PASSWORD='${{ secrets.ADMIN_PASSWORD }}'

            # Kustomize로 전체 애플리케이션 배포
            # 이미지 태그를 최신 버전으로 변경
            IMAGE_TAG=${GITHUB_SHA::7}
            find ./k8s/base -type f -name "*.yaml" -exec sed -i -E "s|image: jobstat/([a-zA-Z0-9_-]+):latest|image: ghcr.io/${{ secrets.DOCKER_USERNAME }}/jobstat-\1:${IMAGE_TAG}|g" {} +
            
            kubectl apply -k k8s/overlays/local-k3d
    ```

---

## Part 3: 쿠버네티스 리소스 수정

GitHub Secrets를 사용하도록 기존 쿠버네티스 YAML 파일들을 수정합니다.

### **4단계: `common-secrets.yaml` 삭제**

로컬에 민감 정보를 저장하던 `k8s/overlays/local-k3d/common-secrets.yaml` 파일은 더 이상 필요 없으므로 삭제합니다. 이 역할은 CD 워크플로우의 `kubectl create secret` 명령어가 대체합니다.

### **5단계: `kustomization.yaml` 수정**

`k8s/overlays/local-k3d/kustomization.yaml` 파일에서 `common-secrets.yaml` 리소스 참조를 제거합니다.

```yaml
# File: k8s/overlays/local-k3d/kustomization.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
bases:
- ../../base/infra
- ../../base/services
resources:
# - common-secrets.yaml # 이 라인을 삭제하거나 주석 처리
- common-configmap.yaml
- ingress.yaml
```

### **6단계: `Deployment` 이미지 경로 수정**

모든 `Deployment` YAML 파일 (`k8s/base/**/*.yaml`)의 `image` 경로를 GHCR을 바라보도록 수정해야 합니다. CD 워크플로우가 `sed` 명령어로 이 부분을 동적으로 변경해주지만, 기본 템플릿을 아래와 같이 수정해두는 것이 좋습니다.

*   **수정 전:** `image: jobstat/auth:latest`
*   **수정 후:** `image: ghcr.io/YOUR_GITHUB_USERNAME/jobstat-auth:latest`

---

## Part 4: 실행 및 확인

1.  **코드 변경 및 푸시:** 로컬에서 코드를 수정한 뒤 `main` 브랜치에 푸시합니다.
2.  **CI 워크플로우 확인:** GitHub Repository의 "Actions" 탭에서 `CI - Build and Test` 워크플로우가 자동으로 실행되고 성공하는지 확인합니다.
3.  **CD 워크플로우 실행:** "Actions" 탭에서 `CD - Build, Push and Deploy to k3d` 워크플로우를 선택하고 "Run workflow" 버튼을 눌러 수동으로 실행합니다.
4.  **배포 확인:** 워크플로우의 로그를 통해 모든 단계(이미지 빌드/푸시, Secret 생성, 배포)가 성공적으로 완료되었는지 확인합니다.
5.  **외부 접속:** 로컬 환경에서 `http://localhost:8080`으로 접속하여 서비스가 정상 동작하는지 확인합니다.

이로써, 보안과 자동화 수준을 한 단계 높인 GitOps 기반의 CI/CD 파이프라인 구축이 완료되었습니다.