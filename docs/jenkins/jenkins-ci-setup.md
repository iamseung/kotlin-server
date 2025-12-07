# Jenkins CI 파이프라인 구축 가이드

PR 생성 시 ktlint, detekt, 테스트를 자동으로 검사하는 Jenkins CI 파이프라인 구축 가이드입니다.

## 목차
- [전체 플로우](#전체-플로우)
- [1. Jenkins 설치 및 초기 설정](#1-jenkins-설치-및-초기-설정)
- [2. GitHub 설정](#2-github-설정)
- [3. Branch Protection Rule 설정](#3-branch-protection-rule-설정)
- [4. Jenkinsfile 작성](#4-jenkinsfile-작성)
- [5. Jenkins Job 생성](#5-jenkins-job-생성)
- [6. detekt 설정](#6-detekt-설정)
- [7. 실행 흐름](#7-실행-흐름)
- [8. PR 머지 조건](#8-pr-머지-조건)
- [9. 로컬에서 미리 체크하기](#9-로컬에서-미리-체크하기)
- [10. 추가 개선사항](#10-추가-개선사항)

---

## 전체 플로우

```
PR 생성/업데이트 → GitHub Webhook → Jenkins → ktlint/detekt/test 실행 → 결과를 GitHub에 전달 → PR 상태 업데이트
```

---

## 1. Jenkins 설치 및 초기 설정

### Docker로 Jenkins 실행 (추천)

```bash
docker run -d \
  --name jenkins \
  -p 8080:8080 \
  -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  jenkins/jenkins:lts
```

### 초기 비밀번호 확인

```bash
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

### 필요한 플러그인 설치

Jenkins 대시보드 → Manage Jenkins → Plugins → Available plugins

설치할 플러그인:
- ✅ **GitHub Integration Plugin**
- ✅ **GitHub Pull Request Builder**
- ✅ **Pipeline**
- ✅ **Gradle Plugin**
- ✅ **Kotlin Plugin** (선택사항)
- ✅ **HTML Publisher Plugin** (테스트 리포트용)

---

## 2. GitHub 설정

### 2.1 Personal Access Token 생성

1. GitHub 로그인 → **Settings**
2. **Developer settings** → **Personal access tokens** → **Tokens (classic)**
3. **Generate new token (classic)** 클릭
4. 필요한 권한 선택:
   - ✅ `repo` (전체)
   - ✅ `admin:repo_hook`
5. Token 생성 후 **복사해서 안전한 곳에 보관**

### 2.2 Jenkins에 GitHub Credentials 등록

1. Jenkins → **Manage Jenkins** → **Credentials**
2. **System** → **Global credentials** → **Add Credentials**
3. 다음 정보 입력:
   - **Kind**: `Username with password`
   - **Username**: GitHub 사용자명
   - **Password**: Personal Access Token (위에서 생성한 것)
   - **ID**: `github-credentials` (나중에 Jenkinsfile에서 사용)
   - **Description**: `GitHub Personal Access Token`
4. **Create** 클릭

### 2.3 Repository에 Webhook 추가

1. GitHub Repository → **Settings** → **Webhooks** → **Add webhook**
2. 다음 정보 입력:
   - **Payload URL**: `http://your-jenkins-url:8080/github-webhook/`
     - 로컬 테스트: `http://localhost:8080/github-webhook/`
     - 실제 서버: `http://your-server-ip:8080/github-webhook/`
   - **Content type**: `application/json`
   - **Which events would you like to trigger this webhook?**
     - ✅ Let me select individual events
       - ✅ Pull requests
       - ✅ Pushes
3. **Add webhook** 클릭

---

## 3. Branch Protection Rule 설정

GitHub Repository → **Settings** → **Branches** → **Add branch protection rule**

### 설정 내용

```yaml
Branch name pattern: main  # 또는 master

✅ Require status checks to pass before merging
  ✅ Require branches to be up to date before merging
  Status checks that are required:
    - jenkins/pr-check  # Jenkins 파이프라인에서 설정할 이름

✅ Require pull request reviews before merging
  Required approving reviews: 1

✅ Require conversation resolution before merging

□ Do not allow bypassing the above settings
```

**저장** 클릭

---

## 4. Jenkinsfile 작성

프로젝트 루트 디렉토리에 `Jenkinsfile` 생성:

```groovy
pipeline {
    agent any

    tools {
        gradle 'Gradle 8.5' // Jenkins에 설정된 Gradle 이름
    }

    environment {
        GITHUB_CREDENTIALS = credentials('github-credentials')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Setup') {
            steps {
                script {
                    // PR 정보 확인
                    echo "Building PR #${env.CHANGE_ID}"
                    echo "Branch: ${env.CHANGE_BRANCH}"
                    echo "Target: ${env.CHANGE_TARGET}"
                }
            }
        }

        stage('ktlint Check') {
            steps {
                script {
                    try {
                        sh './gradlew ktlintCheck --no-daemon'
                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        error("ktlint check failed")
                    }
                }
            }
        }

        stage('detekt Check') {
            steps {
                script {
                    try {
                        sh './gradlew detekt --no-daemon'
                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        error("detekt check failed")
                    }
                }
            }
        }

        stage('Test') {
            steps {
                script {
                    try {
                        sh './gradlew test --no-daemon'
                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        error("Tests failed")
                    }
                }
            }
            post {
                always {
                    junit '**/build/test-results/test/*.xml'
                    publishHTML([
                        reportDir: 'build/reports/tests/test',
                        reportFiles: 'index.html',
                        reportName: 'Test Report'
                    ])
                }
            }
        }

        stage('Build') {
            steps {
                sh './gradlew clean build -x test --no-daemon'
            }
        }
    }

    post {
        success {
            echo 'All checks passed! ✅'
            // GitHub PR에 성공 상태 전달
            setBuildStatus("Build succeeded", "SUCCESS")
        }
        failure {
            echo 'Checks failed! ❌'
            // GitHub PR에 실패 상태 전달
            setBuildStatus("Build failed", "FAILURE")
        }
        always {
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

## 5. Jenkins Job 생성

### 5.1 Multibranch Pipeline 생성

1. Jenkins Dashboard → **New Item**
2. **Enter an item name**: `kotlin-server-pr-check`
3. **Type**: `Multibranch Pipeline` 선택
4. **OK** 클릭

### 5.2 Branch Sources 설정

**Branch Sources** 섹션:

1. **Add source** → **GitHub** 선택

2. 다음 정보 입력:
   - **Credentials**: `github-credentials` (앞서 생성한 것)
   - **Repository HTTPS URL**: `https://github.com/your-username/kotlin-server`
     - 실제 레포지토리 URL로 변경

3. **Behaviors** 설정:
   - ✅ **Discover branches**
   - ✅ **Discover pull requests from origin**
     - Strategy: `Merging the pull request with the current target branch revision`
   - ✅ **Discover pull requests from forks**
     - Strategy: `Merging the pull request with the current target branch revision`
     - Trust: `From users with Admin or Write permission`

4. **Build Configuration**:
   - Mode: `by Jenkinsfile`
   - Script Path: `Jenkinsfile`

5. **Scan Multibranch Pipeline Triggers**:
   - ✅ Periodically if not otherwise run
     - Interval: `1 minute`

6. **저장** 클릭

### 5.3 Gradle 설정 (필요한 경우)

Jenkins → **Manage Jenkins** → **Tools** → **Gradle installations**

- **Name**: `Gradle 8.5`
- **Install automatically**: ✅
- **Version**: `8.5`

**저장** 클릭

---

## 6. detekt 설정

### 6.1 build.gradle.kts에 detekt 플러그인 추가

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

`config/detekt/detekt.yml` 파일 생성:

```yaml
build:
  maxIssues: 0
  excludeCorrectable: false

config:
  validation: true
  warningsAsErrors: false

complexity:
  active: true
  LongMethod:
    threshold: 60
  LongParameterList:
    functionThreshold: 6
    constructorThreshold: 7

style:
  active: true
  MaxLineLength:
    maxLineLength: 120
    excludeCommentStatements: true

formatting:
  active: true
  autoCorrect: true

potential-bugs:
  active: true

performance:
  active: true

exceptions:
  active: true
```

### 6.3 baseline 파일 생성 (선택사항)

기존 위반사항을 무시하고 새로운 위반만 검사:

```bash
./gradlew detektBaseline
```

---

## 7. 실행 흐름

### CI 파이프라인 실행 순서

```
1. PR 생성/업데이트
   ↓
2. GitHub Webhook이 Jenkins에 알림
   ↓
3. Jenkins가 코드 Checkout
   ↓
4. ktlint Check 실행
   ↓ (통과 시)
5. detekt Check 실행
   ↓ (통과 시)
6. 테스트 실행
   ↓ (통과 시)
7. 빌드 실행
   ↓
8. 결과를 GitHub PR에 전달
   - ✅ 성공: PR 머지 가능
   - ❌ 실패: PR 머지 불가
```

### 각 단계별 실패 시 동작

- **ktlint 실패** → 파이프라인 중단, GitHub에 실패 상태 전달
- **detekt 실패** → 파이프라인 중단, GitHub에 실패 상태 전달
- **테스트 실패** → 파이프라인 중단, GitHub에 실패 상태 전달
- **빌드 실패** → GitHub에 실패 상태 전달

---

## 8. PR 머지 조건

GitHub Branch Protection이 설정되어 있으면 다음 조건을 모두 만족해야 PR 머지 가능:

### 필수 조건
- ✅ **ktlint check 통과**
- ✅ **detekt check 통과**
- ✅ **모든 테스트 통과**
- ✅ **빌드 성공**
- ✅ **코드 리뷰 승인** (1명 이상)
- ✅ **모든 대화 해결** (Conversation resolution 설정 시)

### PR 상태 예시

**성공 시:**
```
✅ jenkins/pr-check — Build succeeded
✅ All checks have passed
```

**실패 시:**
```
❌ jenkins/pr-check — ktlint check failed
❌ Some checks were not successful
```

---

## 9. 로컬에서 미리 체크하기

### Git Hook 설정

Push 전에 자동으로 검사를 실행하도록 설정:

#### pre-push hook 생성

`.git/hooks/pre-push` 파일 생성:

```bash
#!/bin/bash

echo "🔍 Running pre-push checks..."
echo ""

echo "📝 Running ktlint..."
./gradlew ktlintCheck
if [ $? -ne 0 ]; then
    echo ""
    echo "❌ ktlint check failed"
    echo "Run './gradlew ktlintFormat' to auto-fix formatting issues"
    exit 1
fi
echo "✅ ktlint check passed"
echo ""

echo "🔍 Running detekt..."
./gradlew detekt
if [ $? -ne 0 ]; then
    echo ""
    echo "❌ detekt check failed"
    echo "Check build/reports/detekt/detekt.html for details"
    exit 1
fi
echo "✅ detekt check passed"
echo ""

echo "🧪 Running tests..."
./gradlew test
if [ $? -ne 0 ]; then
    echo ""
    echo "❌ Tests failed"
    echo "Check build/reports/tests/test/index.html for details"
    exit 1
fi
echo "✅ All tests passed"
echo ""

echo "🎉 All checks passed! Proceeding with push..."
```

#### 실행 권한 부여

```bash
chmod +x .git/hooks/pre-push
```

### 수동 체크 스크립트

`scripts/check-pr.sh` 생성:

```bash
#!/bin/bash

set -e

echo "🔍 Running PR checks..."
echo ""

# ktlint
echo "📝 Checking code style with ktlint..."
./gradlew ktlintCheck
echo "✅ ktlint passed"
echo ""

# detekt
echo "🔍 Running static analysis with detekt..."
./gradlew detekt
echo "✅ detekt passed"
echo ""

# tests
echo "🧪 Running tests..."
./gradlew test
echo "✅ Tests passed"
echo ""

# build
echo "🏗️  Building project..."
./gradlew build -x test
echo "✅ Build succeeded"
echo ""

echo "🎉 All checks passed! You're ready to create a PR."
```

```bash
chmod +x scripts/check-pr.sh
```

**실행:**
```bash
./scripts/check-pr.sh
```

---

## 10. 추가 개선사항

### 10.1 병렬 실행으로 속도 향상

ktlint와 detekt를 병렬로 실행하여 시간 절약:

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

### 10.2 Gradle 캐싱으로 빌드 속도 향상

```groovy
stage('Setup') {
    steps {
        script {
            // Gradle 캐시 활용
            sh '''
                mkdir -p ~/.gradle
                echo "org.gradle.daemon=false" >> ~/.gradle/gradle.properties
                echo "org.gradle.caching=true" >> ~/.gradle/gradle.properties
            '''
        }
    }
}
```

### 10.3 코드 커버리지 리포트 추가

`build.gradle.kts`에 JaCoCo 추가:

```kotlin
plugins {
    jacoco
}

jacoco {
    toolVersion = "0.8.11"
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
```

Jenkinsfile에 커버리지 단계 추가:

```groovy
stage('Test Coverage') {
    steps {
        sh './gradlew jacocoTestReport'
        publishHTML([
            reportDir: 'build/reports/jacoco/test/html',
            reportFiles: 'index.html',
            reportName: 'Code Coverage Report'
        ])
    }
}
```

### 10.4 Slack 알림 추가

```groovy
post {
    success {
        slackSend(
            color: 'good',
            message: "✅ PR Check Passed: ${env.JOB_NAME} #${env.BUILD_NUMBER}\n${env.BUILD_URL}"
        )
    }
    failure {
        slackSend(
            color: 'danger',
            message: "❌ PR Check Failed: ${env.JOB_NAME} #${env.BUILD_NUMBER}\n${env.BUILD_URL}"
        )
    }
}
```

---

## 트러블슈팅

### Jenkins에서 GitHub에 상태를 전달할 수 없는 경우

**증상:** PR에 Jenkins 상태가 표시되지 않음

**해결방법:**
1. GitHub Personal Access Token 권한 확인
2. Webhook URL이 올바른지 확인
3. Jenkins 로그 확인: `Manage Jenkins` → `System Log`

### ktlint 자동 수정

```bash
./gradlew ktlintFormat
```

### detekt baseline 재생성

기존 위반사항을 baseline으로 설정:

```bash
./gradlew detektBaseline
```

### Gradle Daemon 이슈

Jenkins에서 Gradle Daemon 사용 시 메모리 이슈가 발생할 수 있음:

```bash
# 모든 Gradle 명령에 --no-daemon 옵션 사용
./gradlew test --no-daemon
```

---

## 참고 자료

- [Jenkins 공식 문서](https://www.jenkins.io/doc/)
- [GitHub Webhook 가이드](https://docs.github.com/en/webhooks)
- [ktlint 공식 문서](https://pinterest.github.io/ktlint/)
- [detekt 공식 문서](https://detekt.dev/)
- [Branch Protection Rules](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches)
