# Jenkins CI 파이프라인 구축 가이드 (로컬 환경)

로컬 환경에서 Jenkins를 사용하여 PR 생성 시 ktlint, detekt, 테스트를 자동으로 검사하는 CI 파이프라인 구축 가이드입니다.

## 목차
- [개요](#개요)
- [사전 준비물](#사전-준비물)
- [1. Jenkins 로컬 설치](#1-jenkins-로컬-설치)
- [2. ngrok 설정](#2-ngrok-설정)
- [3. GitHub 설정](#3-github-설정)
- [4. Jenkins Job 생성](#4-jenkins-job-생성)
- [5. Jenkinsfile 작성](#5-jenkinsfile-작성)
- [6. detekt 설정](#6-detekt-설정)
- [7. 테스트 실행](#7-테스트-실행)
- [8. 자동화 스크립트](#8-자동화-스크립트)
- [9. 트러블슈팅](#9-트러블슈팅)

---

## 개요

### 로컬 환경의 문제점

```
GitHub (인터넷)
    ↓ Webhook 전송
    ↓ http://localhost:8080  ← ❌ GitHub가 접근 불가!
로컬 Jenkins (내 컴퓨터)
```

GitHub는 인터넷 상의 서비스이므로 로컬 환경(`localhost`)에 직접 접근할 수 없습니다.

### 해결 방법: ngrok

```
GitHub (인터넷)
    ↓ Webhook 전송
    ↓ https://abc123.ngrok-free.app
ngrok (터널)
    ↓ 포워딩
로컬 Jenkins (localhost:8080)
```

ngrok을 사용하면 로컬 Jenkins를 외부에서 접근 가능한 HTTPS URL로 노출할 수 있습니다.

---

## 사전 준비물

- ✅ Docker Desktop 설치
- ✅ ngrok 계정 (무료)
- ✅ GitHub 계정
- ✅ 8080, 50000 포트 사용 가능

---

## 1. Jenkins 로컬 설치

### 1.1 Docker로 Jenkins 실행

```bash
# Jenkins 컨테이너 실행
docker run -d \
  --name jenkins \
  -p 8080:8080 \
  -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  jenkins/jenkins:lts
```

### 1.2 초기 비밀번호 확인

```bash
# 초기 Admin 비밀번호 확인
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

출력된 비밀번호를 복사해둡니다.

### 1.3 Jenkins 초기 설정

1. 브라우저에서 `http://localhost:8080` 접속
2. 복사한 비밀번호 입력
3. **Install suggested plugins** 선택
4. Admin 계정 생성
5. Jenkins URL: `http://localhost:8080` (나중에 ngrok URL로 변경)

### 1.4 필요한 플러그인 설치

Jenkins 대시보드 → **Manage Jenkins** → **Plugins** → **Available plugins**

검색 후 설치:
- ✅ **GitHub Integration**
- ✅ **GitHub Pull Request Builder**
- ✅ **Pipeline**
- ✅ **Gradle Plugin**
- ✅ **HTML Publisher** (테스트 리포트용)

설치 후 Jenkins 재시작:
```bash
docker restart jenkins
```

---

## 2. ngrok 설정

### 2.1 ngrok 설치

#### macOS
```bash
brew install ngrok
```

#### Windows
```bash
# Chocolatey 사용
choco install ngrok

# 또는 https://ngrok.com/download 에서 다운로드
```

#### Linux
```bash
# Snap 사용
snap install ngrok

# 또는 수동 설치
wget https://bin.equinox.io/c/bNyj1mQVY4c/ngrok-v3-stable-linux-amd64.tgz
tar xvzf ngrok-v3-stable-linux-amd64.tgz
sudo mv ngrok /usr/local/bin
```

### 2.2 ngrok 계정 연동

1. https://ngrok.com 에서 무료 계정 생성
2. Dashboard에서 **Your Authtoken** 복사
3. 터미널에서 인증:

```bash
ngrok config add-authtoken YOUR_AUTH_TOKEN
```

### 2.3 ngrok 실행

```bash
# Jenkins 포트(8080)를 외부에 노출
ngrok http 8080
```

실행 화면:
```
ngrok

Session Status                online
Account                       your-email@example.com
Version                       3.5.0
Region                        Asia Pacific (ap)
Latency                       25ms
Web Interface                 http://127.0.0.1:4040
Forwarding                    https://1234-abcd-5678.ngrok-free.app -> http://localhost:8080

Connections                   ttl     opn     rt1     rt5     p50     p90
                              0       0       0.00    0.00    0.00    0.00
```

**중요:** `https://1234-abcd-5678.ngrok-free.app` 이 URL을 복사해둡니다.

### 2.4 ngrok URL 고정 (선택사항 - 유료)

무료 플랜에서는 ngrok을 재실행할 때마다 URL이 바뀝니다.

**고정 URL을 원하면:**
- ngrok Pro 플랜 ($8/month) 구독
- Static Domain 사용

---

## 3. GitHub 설정

### 3.1 Personal Access Token 생성

1. GitHub 로그인 → **Settings**
2. **Developer settings** → **Personal access tokens** → **Tokens (classic)**
3. **Generate new token (classic)**
4. Token 설정:
   - **Note**: `Jenkins CI Local`
   - **Expiration**: `90 days` (필요에 따라 조정)
   - **Select scopes**:
     - ✅ `repo` (전체)
     - ✅ `admin:repo_hook`
5. **Generate token** 클릭
6. **토큰을 복사해서 안전한 곳에 저장** (다시 볼 수 없음!)

### 3.2 Jenkins에 GitHub Credentials 등록

1. Jenkins → **Manage Jenkins** → **Credentials**
2. **System** → **Global credentials (unrestricted)** → **Add Credentials**
3. 정보 입력:
   - **Kind**: `Username with password`
   - **Username**: GitHub 사용자명
   - **Password**: Personal Access Token (위에서 생성한 것)
   - **ID**: `github-credentials`
   - **Description**: `GitHub Personal Access Token for Local Jenkins`
4. **Create** 클릭

### 3.3 Repository에 Webhook 추가

1. GitHub Repository → **Settings** → **Webhooks** → **Add webhook**
2. Webhook 설정:
   - **Payload URL**: `https://1234-abcd-5678.ngrok-free.app/github-webhook/`
     - ⚠️ ngrok URL 사용 (localhost 아님!)
     - ⚠️ 마지막에 `/` 꼭 포함
   - **Content type**: `application/json`
   - **Secret**: (비워둠)
   - **Which events would you like to trigger this webhook?**
     - ✅ `Let me select individual events`
       - ✅ `Pull requests`
       - ✅ `Pushes`
   - ✅ `Active`
3. **Add webhook** 클릭

#### Webhook 테스트

1. Webhook 추가 후 **Recent Deliveries** 탭 확인
2. 초록색 체크 표시 → 성공
3. 빨간색 X 표시 → 실패 (ngrok URL 확인)

### 3.4 Branch Protection Rule 설정

GitHub Repository → **Settings** → **Branches** → **Add branch protection rule**

```yaml
Branch name pattern: main

✅ Require status checks to pass before merging
  ✅ Require branches to be up to date before merging
  Status checks that are required:
    - jenkins/pr-check

✅ Require pull request reviews before merging
  Required approving reviews: 1

✅ Require conversation resolution before merging
```

**Save changes** 클릭

---

## 4. Jenkins Job 생성

### 4.1 Gradle 설정

Jenkins → **Manage Jenkins** → **Tools**

**Gradle installations** 섹션:
1. **Add Gradle**
2. 설정:
   - **Name**: `Gradle 8.5`
   - ✅ **Install automatically**
   - **Version**: `8.5`
3. **Save**

### 4.2 Multibranch Pipeline 생성

1. Jenkins Dashboard → **New Item**
2. 설정:
   - **Enter an item name**: `kotlin-server-pr-check`
   - **Type**: `Multibranch Pipeline` 선택
3. **OK** 클릭

### 4.3 Branch Sources 설정

**Branch Sources** 탭:

1. **Add source** → **GitHub** 선택

2. **Credentials**: `github-credentials` 선택

3. **Repository HTTPS URL**:
   ```
   https://github.com/your-username/kotlin-server
   ```
   (본인의 레포지토리 URL로 변경)

4. **Behaviors** - **Add** 클릭하여 다음 추가:
   - ✅ **Discover branches**
   - ✅ **Discover pull requests from origin**
     - Strategy: `Merging the pull request with the current target branch revision`
   - ✅ **Discover pull requests from forks**
     - Strategy: `Merging the pull request with the current target branch revision`
     - Trust: `From users with Admin or Write permission`

5. **Build Configuration**:
   - Mode: `by Jenkinsfile`
   - Script Path: `Jenkinsfile`

6. **Scan Multibranch Pipeline Triggers**:
   - ✅ `Periodically if not otherwise run`
   - Interval: `1 minute`

7. **Save** 클릭

---

## 5. Jenkinsfile 작성

프로젝트 루트에 `Jenkinsfile` 생성:

```groovy
pipeline {
    agent any

    tools {
        gradle 'Gradle 8.5'
    }

    environment {
        GITHUB_CREDENTIALS = credentials('github-credentials')
    }

    stages {
        stage('Checkout') {
            steps {
                echo '📥 Checking out code...'
                checkout scm
            }
        }

        stage('Environment Info') {
            steps {
                script {
                    echo "==================== Environment Info ===================="
                    echo "Building: ${env.JOB_NAME}"
                    echo "Build Number: ${env.BUILD_NUMBER}"
                    if (env.CHANGE_ID) {
                        echo "PR Number: ${env.CHANGE_ID}"
                        echo "PR Branch: ${env.CHANGE_BRANCH}"
                        echo "Target Branch: ${env.CHANGE_TARGET}"
                    } else {
                        echo "Branch: ${env.BRANCH_NAME}"
                    }
                    echo "========================================================"
                }
            }
        }

        stage('ktlint Check') {
            steps {
                echo '📝 Running ktlint check...'
                script {
                    try {
                        sh './gradlew ktlintCheck --no-daemon'
                        echo '✅ ktlint check passed'
                    } catch (Exception e) {
                        echo '❌ ktlint check failed'
                        currentBuild.result = 'FAILURE'
                        error("ktlint check failed")
                    }
                }
            }
        }

        stage('detekt Check') {
            steps {
                echo '🔍 Running detekt check...'
                script {
                    try {
                        sh './gradlew detekt --no-daemon'
                        echo '✅ detekt check passed'
                    } catch (Exception e) {
                        echo '❌ detekt check failed'
                        currentBuild.result = 'FAILURE'
                        error("detekt check failed")
                    }
                }
            }
        }

        stage('Test') {
            steps {
                echo '🧪 Running tests...'
                script {
                    try {
                        sh './gradlew test --no-daemon'
                        echo '✅ All tests passed'
                    } catch (Exception e) {
                        echo '❌ Tests failed'
                        currentBuild.result = 'FAILURE'
                        error("Tests failed")
                    }
                }
            }
            post {
                always {
                    // 테스트 결과 publish
                    junit '**/build/test-results/test/*.xml'
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'build/reports/tests/test',
                        reportFiles: 'index.html',
                        reportName: 'Test Report'
                    ])
                }
            }
        }

        stage('Build') {
            steps {
                echo '🏗️  Building project...'
                sh './gradlew clean build -x test --no-daemon'
                echo '✅ Build completed'
            }
        }
    }

    post {
        success {
            echo '🎉 All checks passed!'
            setBuildStatus("Build succeeded", "SUCCESS")
        }
        failure {
            echo '💥 Build failed!'
            setBuildStatus("Build failed", "FAILURE")
        }
        always {
            echo '🧹 Cleaning workspace...'
            cleanWs()
        }
    }
}

// GitHub PR Status 업데이트 함수
void setBuildStatus(String message, String state) {
    step([
        $class: "GitHubCommitStatusSetter",
        reposSource: [$class: "ManuallyEnteredRepositorySource", url: env.GIT_URL],
        contextSource: [$class: "ManuallyEnteredCommitContextSource", context: "jenkins/pr-check"],
        errorHandlers: [[$class: "ChangingBuildStatusErrorHandler", result: "UNSTABLE"]],
        statusResultSource: [
            $class: "ConditionalStatusResultSource",
            results: [[$class: "AnyBuildResult", message: message, state: state]]
        ]
    ])
}
```

---

## 6. detekt 설정

### 6.1 build.gradle.kts에 detekt 추가

```kotlin
plugins {
    // 기존 플러그인들...
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/config/detekt/detekt.yml")
    baseline = file("$projectDir/config/detekt/baseline.xml")
}

dependencies {
    // 기존 의존성들...
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.4")
}
```

### 6.2 detekt 설정 파일 생성

`config/detekt/detekt.yml`:

```yaml
build:
  maxIssues: 0

complexity:
  active: true
  LongMethod:
    threshold: 60

style:
  active: true
  MaxLineLength:
    maxLineLength: 120
```

---

## 7. 테스트 실행

### 7.1 테스트 PR 생성

1. 새 브랜치 생성:
```bash
git checkout -b test/jenkins-ci
```

2. 작은 변경사항 커밋:
```bash
echo "# Test" >> README.md
git add README.md
git commit -m "Test: Jenkins CI"
git push origin test/jenkins-ci
```

3. GitHub에서 PR 생성

### 7.2 Jenkins에서 확인

1. Jenkins Dashboard → `kotlin-server-pr-check` 클릭
2. PR 브랜치가 자동으로 감지되어야 함
3. 빌드가 자동으로 시작됨

### 7.3 GitHub에서 확인

PR 페이지에서:
- ✅ `jenkins/pr-check` 상태가 표시됨
- 모든 체크가 통과하면 초록색 체크 표시
- 실패하면 빨간색 X 표시 및 머지 불가

---

## 8. 자동화 스크립트

### 8.1 Jenkins + ngrok 시작 스크립트

`scripts/start-jenkins-local.sh`:

```bash
#!/bin/bash

set -e

echo "🚀 Starting Jenkins with ngrok for local development..."
echo ""

# Jenkins 컨테이너 확인 및 시작
echo "📦 Checking Jenkins container..."
if docker ps -a | grep -q jenkins; then
    if ! docker ps | grep -q jenkins; then
        echo "Starting existing Jenkins container..."
        docker start jenkins
    else
        echo "Jenkins is already running"
    fi
else
    echo "Creating new Jenkins container..."
    docker run -d \
      --name jenkins \
      -p 8080:8080 \
      -p 50000:50000 \
      -v jenkins_home:/var/jenkins_home \
      jenkins/jenkins:lts
fi

echo ""
echo "⏳ Waiting for Jenkins to be ready..."
sleep 15

# Jenkins 상태 확인
until curl -s http://localhost:8080/login > /dev/null; do
    echo "Waiting for Jenkins..."
    sleep 5
done

echo "✅ Jenkins is ready!"
echo "📍 Local URL: http://localhost:8080"
echo ""

# ngrok 실행 확인
if pgrep -f "ngrok http 8080" > /dev/null; then
    echo "⚠️  ngrok is already running"
    echo "To get the URL, check: http://localhost:4040"
else
    echo "🌐 Starting ngrok..."
    echo ""
    echo "⚠️  IMPORTANT: Copy the ngrok URL and update your GitHub Webhook!"
    echo ""
    ngrok http 8080
fi
```

```bash
chmod +x scripts/start-jenkins-local.sh
```

### 8.2 사용 방법

```bash
# Jenkins + ngrok 시작
./scripts/start-jenkins-local.sh

# ngrok URL 확인 (별도 터미널)
curl http://localhost:4040/api/tunnels | jq '.tunnels[0].public_url'
```

### 8.3 종료 스크립트

`scripts/stop-jenkins-local.sh`:

```bash
#!/bin/bash

echo "🛑 Stopping Jenkins and ngrok..."

# ngrok 종료
pkill -f ngrok
echo "✅ ngrok stopped"

# Jenkins 컨테이너 중지 (삭제하지 않음)
docker stop jenkins
echo "✅ Jenkins stopped"

echo ""
echo "💡 Tip: Use './scripts/start-jenkins-local.sh' to start again"
```

```bash
chmod +x scripts/stop-jenkins-local.sh
```

---

## 9. 트러블슈팅

### 문제 1: ngrok URL이 계속 바뀜

**증상:** ngrok을 재실행할 때마다 URL이 바뀌어서 GitHub Webhook을 매번 업데이트해야 함

**해결방법:**

**옵션 A: ngrok 무료 플랜 사용 (URL 변경됨)**
- 매번 GitHub Webhook URL을 업데이트

**옵션 B: ngrok Pro 구독 ($8/month)**
- Static domain 사용
- URL이 고정됨

**옵션 C: 폴링 방식으로 전환**
- Webhook 대신 Jenkins가 주기적으로 GitHub 체크
- Jenkinsfile에 추가:
```groovy
properties([
    pipelineTriggers([
        pollSCM('H/5 * * * *')  // 5분마다 체크
    ])
])
```

### 문제 2: Jenkins가 GitHub에 상태를 전달하지 못함

**증상:** PR에 `jenkins/pr-check` 상태가 표시되지 않음

**해결방법:**

1. **GitHub Token 권한 확인**
```bash
# Jenkins credentials 다시 생성
# repo, admin:repo_hook 권한 확인
```

2. **Jenkins 로그 확인**
```bash
docker logs jenkins | grep -i github
```

3. **GitHub Webhook 확인**
- GitHub Repository → Settings → Webhooks
- Recent Deliveries 탭에서 응답 확인

### 문제 3: Jenkins 빌드가 너무 느림

**증상:** 빌드가 5분 이상 걸림

**해결방법:**

1. **Gradle Daemon 비활성화 확인**
- Jenkinsfile에서 `--no-daemon` 사용 중인지 확인

2. **병렬 실행**
```groovy
stage('Quality Checks') {
    parallel {
        stage('ktlint') {
            steps {
                sh './gradlew ktlintCheck --no-daemon'
            }
        }
        stage('detekt') {
            steps {
                sh './gradlew detekt --no-daemon'
            }
        }
    }
}
```

3. **Gradle 캐싱**
```groovy
stage('Setup') {
    steps {
        sh '''
            mkdir -p ~/.gradle
            echo "org.gradle.caching=true" >> ~/.gradle/gradle.properties
        '''
    }
}
```

### 문제 4: Docker 컨테이너 권한 문제

**증상:** Jenkins에서 gradlew 실행 시 permission denied

**해결방법:**

```bash
# gradlew 실행 권한 추가
chmod +x gradlew
git add gradlew
git commit -m "Add execute permission to gradlew"
```

또는 Jenkinsfile에서:
```groovy
stage('Setup') {
    steps {
        sh 'chmod +x gradlew'
    }
}
```

### 문제 5: ngrok이 자주 끊김

**증상:** ngrok 연결이 몇 시간 후 끊어짐

**해결방법:**

무료 플랜은 세션 시간 제한이 있습니다.

**옵션 A: ngrok 재시작 스크립트**
```bash
# crontab 등록
crontab -e

# 매 시간마다 ngrok 재시작
0 * * * * pkill ngrok && ngrok http 8080 --log=stdout > /tmp/ngrok.log 2>&1 &
```

**옵션 B: 서버 배포 사용**
- 로컬 대신 클라우드 서버에 Jenkins 배포
- `docs/jenkins-ci-server.md` 참고

---

## 로컬 환경의 한계

### 장점
- ✅ 무료 (ngrok 무료 플랜)
- ✅ 빠른 테스트 및 디버깅
- ✅ 네트워크 비용 없음

### 단점
- ❌ ngrok URL이 자주 바뀜 (무료 플랜)
- ❌ 컴퓨터가 꺼지면 Jenkins 중단
- ❌ ngrok 세션 시간 제한
- ❌ 프로덕션 환경에 부적합

### 다음 단계

프로덕션 환경 또는 팀 협업을 위해서는 **서버 배포**를 권장합니다.

👉 **[Jenkins CI 서버 배포 가이드](jenkins-ci-server.md)** 참고

---

## 참고 자료

- [Jenkins 공식 문서](https://www.jenkins.io/doc/)
- [ngrok 공식 문서](https://ngrok.com/docs)
- [GitHub Webhooks 가이드](https://docs.github.com/en/webhooks)
- [ktlint](https://pinterest.github.io/ktlint/)
- [detekt](https://detekt.dev/)
