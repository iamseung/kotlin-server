# Jenkins CI 파이프라인 구축 가이드 (서버 배포)

클라우드 서버에 Jenkins를 배포하여 PR 생성 시 ktlint, detekt, 테스트를 자동으로 검사하는 CI 파이프라인 구축 가이드입니다.

## 목차
- [개요](#개요)
- [서버 선택](#서버-선택)
- [1. AWS EC2로 Jenkins 배포](#1-aws-ec2로-jenkins-배포)
- [2. 보안 설정](#2-보안-설정)
- [3. Jenkins 설치 및 설정](#3-jenkins-설치-및-설정)
- [4. GitHub 설정](#4-github-설정)
- [5. Jenkins Job 생성](#5-jenkins-job-생성)
- [6. Jenkinsfile 작성](#6-jenkinsfile-작성)
- [7. SSL 인증서 설정 (HTTPS)](#7-ssl-인증서-설정-https)
- [8. 모니터링 및 유지보수](#8-모니터링-및-유지보수)
- [9. 비용 최적화](#9-비용-최적화)
- [10. 백업 및 복구](#10-백업-및-복구)

---

## 개요

### 서버 배포의 장점

```
GitHub (인터넷)
    ↓ Webhook 전송
    ↓ https://jenkins.yourdomain.com
Jenkins Server (AWS/GCP/Azure)
    ↓ 24/7 가동
    ↓ 고정 URL
    ✅ 안정적인 CI/CD
```

### 로컬 vs 서버 비교

| 항목 | 로컬 | 서버 |
|-----|------|------|
| **URL** | ngrok (변경됨) | 고정 URL ✅ |
| **가동시간** | PC 켤 때만 | 24/7 ✅ |
| **성능** | PC 성능 의존 | 전용 리소스 ✅ |
| **비용** | 무료 | 월 $5-50 |
| **SSL** | ngrok 제공 | 직접 설정 필요 |
| **팀 협업** | 어려움 | 용이 ✅ |

---

## 서버 선택

### 추천 서비스 및 비용

#### AWS EC2
- **t3.small** (2 vCPU, 2GB RAM): **~$15/월**
- **t3.medium** (2 vCPU, 4GB RAM): **~$30/월**
- 장점: 유연한 설정, 많은 레퍼런스
- 단점: 초기 설정 복잡

#### Google Cloud Platform (GCP)
- **e2-small** (2 vCPU, 2GB RAM): **~$13/월**
- **e2-medium** (2 vCPU, 4GB RAM): **~$27/월**
- 장점: $300 무료 크레딧 (3개월)
- 단점: 무료 크레딧 후 자동 과금

#### DigitalOcean
- **Basic Droplet** (1 vCPU, 2GB RAM): **$12/월**
- **Basic Droplet** (2 vCPU, 4GB RAM): **$24/월**
- 장점: 간단한 설정, 초보자 친화적
- 단점: AWS보다 적은 기능

#### Lightsail (AWS 간편 버전)
- **2GB 인스턴스**: **$10/월**
- **4GB 인스턴스**: **$20/월**
- 장점: AWS보다 간단, 고정 가격
- 단점: 확장성 제한

### 최소 요구사항

```yaml
CPU: 2 vCPU 이상
RAM: 2GB 이상 (4GB 권장)
Storage: 20GB 이상
OS: Ubuntu 22.04 LTS (권장)
```

---

## 1. AWS EC2로 Jenkins 배포

### 1.1 EC2 인스턴스 생성

1. **AWS Console 로그인** → **EC2** → **Launch Instance**

2. **인스턴스 설정:**
   - **Name**: `jenkins-ci-server`
   - **AMI**: `Ubuntu Server 22.04 LTS`
   - **Instance type**: `t3.small` (2GB RAM)
   - **Key pair**: 새로 생성 또는 기존 사용
     - `jenkins-key.pem` 다운로드 및 안전하게 보관

3. **Network settings:**
   - ✅ Allow SSH traffic from: `My IP` (보안상 본인 IP만)
   - ✅ Allow HTTP traffic from: `Anywhere`
   - ✅ Allow HTTPS traffic from: `Anywhere`

4. **Configure storage:**
   - `30 GB` gp3

5. **Launch instance**

### 1.2 Elastic IP 할당 (고정 IP)

1. EC2 Dashboard → **Elastic IPs** → **Allocate Elastic IP address**
2. **Allocate**
3. 생성된 IP 선택 → **Actions** → **Associate Elastic IP address**
4. **Instance**: 위에서 생성한 `jenkins-ci-server` 선택
5. **Associate**

이제 고정 Public IP를 얻었습니다!

### 1.3 SSH 접속

```bash
# 키 파일 권한 설정
chmod 400 jenkins-key.pem

# SSH 접속
ssh -i jenkins-key.pem ubuntu@YOUR_ELASTIC_IP
```

### 1.4 서버 초기 설정

```bash
# 시스템 업데이트
sudo apt update && sudo apt upgrade -y

# 필수 패키지 설치
sudo apt install -y \
    apt-transport-https \
    ca-certificates \
    curl \
    software-properties-common \
    git \
    vim
```

### 1.5 Docker 설치

```bash
# Docker 설치
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# 현재 사용자를 docker 그룹에 추가
sudo usermod -aG docker ubuntu

# Docker Compose 설치
sudo curl -L "https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 재접속 (docker 그룹 적용)
exit
ssh -i jenkins-key.pem ubuntu@YOUR_ELASTIC_IP
```

### 1.6 Jenkins 설치 (Docker)

```bash
# Jenkins 홈 디렉토리 생성
mkdir -p ~/jenkins_home

# Jenkins 컨테이너 실행
docker run -d \
  --name jenkins \
  --restart=always \
  -p 8080:8080 \
  -p 50000:50000 \
  -v ~/jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  jenkins/jenkins:lts

# 초기 비밀번호 확인
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

초기 비밀번호를 복사해둡니다.

---

## 2. 보안 설정

### 2.1 Security Group 설정

EC2 Dashboard → 인스턴스 선택 → **Security** 탭 → **Security groups** 클릭

#### Inbound rules 설정

| Type | Protocol | Port | Source | Description |
|------|----------|------|--------|-------------|
| SSH | TCP | 22 | My IP | SSH 접속 (본인 IP만) |
| HTTP | TCP | 80 | 0.0.0.0/0 | HTTP (나중에 HTTPS로 리다이렉트) |
| HTTPS | TCP | 443 | 0.0.0.0/0 | HTTPS |
| Custom TCP | TCP | 8080 | 0.0.0.0/0 | Jenkins (임시, 나중에 제거) |

### 2.2 방화벽 설정 (UFW)

```bash
# UFW 설치 및 활성화
sudo apt install -y ufw

# 기본 정책 설정
sudo ufw default deny incoming
sudo ufw default allow outgoing

# 필요한 포트 열기
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw allow 8080/tcp  # Jenkins (임시)

# 방화벽 활성화
sudo ufw enable

# 상태 확인
sudo ufw status
```

### 2.3 Fail2Ban 설치 (SSH 보호)

```bash
# Fail2Ban 설치
sudo apt install -y fail2ban

# 설정 파일 복사
sudo cp /etc/fail2ban/jail.conf /etc/fail2ban/jail.local

# SSH 보호 활성화
sudo systemctl enable fail2ban
sudo systemctl start fail2ban

# 상태 확인
sudo fail2ban-client status
```

---

## 3. Jenkins 설치 및 설정

### 3.1 Jenkins 초기 설정

1. 브라우저에서 `http://YOUR_ELASTIC_IP:8080` 접속
2. 복사한 초기 비밀번호 입력
3. **Install suggested plugins** 선택
4. Admin 계정 생성
5. Jenkins URL: `http://YOUR_ELASTIC_IP:8080` (나중에 도메인으로 변경)

### 3.2 필수 플러그인 설치

**Manage Jenkins** → **Plugins** → **Available plugins**

검색 후 설치:
- ✅ **GitHub Integration**
- ✅ **GitHub Pull Request Builder**
- ✅ **Pipeline**
- ✅ **Gradle Plugin**
- ✅ **HTML Publisher**
- ✅ **Docker Pipeline** (선택)

설치 후 재시작:
```bash
docker restart jenkins
```

### 3.3 Gradle 설정

**Manage Jenkins** → **Tools** → **Gradle installations**

- **Name**: `Gradle 8.5`
- ✅ **Install automatically**
- **Version**: `8.5`
- **Save**

---

## 4. GitHub 설정

### 4.1 Personal Access Token 생성

1. GitHub → **Settings** → **Developer settings** → **Personal access tokens** → **Tokens (classic)**
2. **Generate new token (classic)**
3. 권한 선택:
   - ✅ `repo` (전체)
   - ✅ `admin:repo_hook`
4. Token 복사 및 저장

### 4.2 Jenkins에 Credentials 등록

1. Jenkins → **Manage Jenkins** → **Credentials**
2. **Global credentials** → **Add Credentials**
3. 정보 입력:
   - **Kind**: `Username with password`
   - **Username**: GitHub 사용자명
   - **Password**: Personal Access Token
   - **ID**: `github-credentials`
4. **Create**

### 4.3 GitHub Webhook 설정

GitHub Repository → **Settings** → **Webhooks** → **Add webhook**

```yaml
Payload URL: http://YOUR_ELASTIC_IP:8080/github-webhook/
Content type: application/json
Events:
  - Pull requests
  - Pushes
Active: ✅
```

**Add webhook** 클릭

### 4.4 Branch Protection Rule

GitHub Repository → **Settings** → **Branches** → **Add rule**

```yaml
Branch name pattern: main

✅ Require status checks to pass before merging
  Status checks: jenkins/pr-check

✅ Require pull request reviews before merging
  Required reviews: 1
```

---

## 5. Jenkins Job 생성

### 5.1 Multibranch Pipeline 생성

1. Jenkins Dashboard → **New Item**
2. Name: `kotlin-server-pr-check`
3. Type: **Multibranch Pipeline**
4. **OK**

### 5.2 Branch Sources 설정

1. **Add source** → **GitHub**
2. **Credentials**: `github-credentials`
3. **Repository HTTPS URL**: `https://github.com/your-username/kotlin-server`
4. **Behaviors**:
   - ✅ Discover branches
   - ✅ Discover pull requests from origin
   - ✅ Discover pull requests from forks
5. **Build Configuration**: `by Jenkinsfile`
6. **Save**

---

## 6. Jenkinsfile 작성

프로젝트 루트에 `Jenkinsfile`:

```groovy
pipeline {
    agent any

    tools {
        gradle 'Gradle 8.5'
    }

    environment {
        GITHUB_CREDENTIALS = credentials('github-credentials')
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
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
                    echo "==================== Build Info ===================="
                    echo "Job: ${env.JOB_NAME}"
                    echo "Build: #${env.BUILD_NUMBER}"
                    if (env.CHANGE_ID) {
                        echo "PR: #${env.CHANGE_ID}"
                        echo "Branch: ${env.CHANGE_BRANCH} → ${env.CHANGE_TARGET}"
                    }
                    echo "===================================================="
                }
            }
        }

        stage('Quality Checks') {
            parallel {
                stage('ktlint') {
                    steps {
                        echo '📝 Running ktlint...'
                        sh './gradlew ktlintCheck --no-daemon'
                    }
                }
                stage('detekt') {
                    steps {
                        echo '🔍 Running detekt...'
                        sh './gradlew detekt --no-daemon'
                    }
                }
            }
        }

        stage('Test') {
            steps {
                echo '🧪 Running tests...'
                sh './gradlew test --no-daemon'
            }
            post {
                always {
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
                echo '🏗️  Building...'
                sh './gradlew clean build -x test --no-daemon'
            }
        }
    }

    post {
        success {
            echo '✅ All checks passed!'
            setBuildStatus("Build succeeded", "SUCCESS")
        }
        failure {
            echo '❌ Build failed!'
            setBuildStatus("Build failed", "FAILURE")
        }
        always {
            cleanWs()
        }
    }
}

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

## 7. SSL 인증서 설정 (HTTPS)

### 7.1 도메인 연결 (선택사항)

도메인이 있다면 A 레코드 추가:
```
jenkins.yourdomain.com → YOUR_ELASTIC_IP
```

### 7.2 Nginx + Let's Encrypt 설정

```bash
# Nginx 설치
sudo apt install -y nginx

# Certbot 설치
sudo apt install -y certbot python3-certbot-nginx

# SSL 인증서 발급 (도메인이 있는 경우)
sudo certbot --nginx -d jenkins.yourdomain.com

# 또는 IP만 사용하는 경우 (자체 서명 인증서)
sudo openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout /etc/ssl/private/jenkins-selfsigned.key \
  -out /etc/ssl/certs/jenkins-selfsigned.crt
```

### 7.3 Nginx 설정

`/etc/nginx/sites-available/jenkins`:

```nginx
upstream jenkins {
    server 127.0.0.1:8080;
}

server {
    listen 80;
    server_name jenkins.yourdomain.com;  # 또는 YOUR_ELASTIC_IP
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name jenkins.yourdomain.com;

    ssl_certificate /etc/letsencrypt/live/jenkins.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/jenkins.yourdomain.com/privkey.pem;

    location / {
        proxy_pass http://jenkins;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_redirect off;
    }
}
```

```bash
# 설정 활성화
sudo ln -s /etc/nginx/sites-available/jenkins /etc/nginx/sites-enabled/

# Nginx 재시작
sudo nginx -t
sudo systemctl restart nginx
```

이제 `https://jenkins.yourdomain.com` 으로 접속 가능!

---

## 8. 모니터링 및 유지보수

### 8.1 Jenkins 로그 확인

```bash
# 실시간 로그
docker logs -f jenkins

# 최근 100줄
docker logs --tail 100 jenkins
```

### 8.2 디스크 사용량 모니터링

```bash
# 디스크 사용량 확인
df -h

# Jenkins 홈 디렉토리 크기
du -sh ~/jenkins_home

# 오래된 빌드 정리 (Jenkins에서)
# Manage Jenkins → System Configuration → 각 Job 설정
# Discard old builds: 10개 유지
```

### 8.3 자동 백업 스크립트

`/home/ubuntu/backup-jenkins.sh`:

```bash
#!/bin/bash

BACKUP_DIR="/home/ubuntu/jenkins-backups"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="jenkins_backup_$DATE.tar.gz"

mkdir -p $BACKUP_DIR

echo "Creating Jenkins backup..."
docker exec jenkins tar czf /tmp/backup.tar.gz /var/jenkins_home

docker cp jenkins:/tmp/backup.tar.gz $BACKUP_DIR/$BACKUP_FILE

# 30일 이상 된 백업 삭제
find $BACKUP_DIR -name "jenkins_backup_*.tar.gz" -mtime +30 -delete

echo "Backup completed: $BACKUP_FILE"
```

```bash
chmod +x /home/ubuntu/backup-jenkins.sh

# 매일 새벽 3시 자동 백업
crontab -e
# 추가:
0 3 * * * /home/ubuntu/backup-jenkins.sh >> /var/log/jenkins-backup.log 2>&1
```

### 8.4 CloudWatch 모니터링 (AWS)

AWS Console → **CloudWatch** → **Alarms** → **Create alarm**

**모니터링 항목:**
- CPU 사용률 > 80%
- 디스크 사용률 > 80%
- 메모리 사용률 > 80%

알람 발생 시 이메일/SMS 전송

---

## 9. 비용 최적화

### 9.1 인스턴스 크기 조정

**사용량이 적다면:**
```
t3.small → t3.micro
월 $15 → 월 $7.5 절약
```

**빌드가 자주 실패한다면:**
```
t3.small → t3.medium
2GB RAM → 4GB RAM
```

### 9.2 예약 인스턴스 (1년 약정)

1년 약정 시 최대 40% 할인:
```
t3.small: $15/월 → $9/월
```

### 9.3 Spot 인스턴스 (권장하지 않음)

- 70-90% 할인
- 언제든지 종료될 수 있음
- CI/CD에는 부적합

### 9.4 스케줄링

밤/주말에 인스턴스 중지:

```bash
# 매일 밤 11시 중지
0 23 * * * aws ec2 stop-instances --instance-ids i-1234567890abcdef0

# 매일 아침 8시 시작
0 8 * * * aws ec2 start-instances --instance-ids i-1234567890abcdef0
```

---

## 10. 백업 및 복구

### 10.1 백업 대상

1. **Jenkins 홈 디렉토리**: `~/jenkins_home`
2. **Nginx 설정**: `/etc/nginx/sites-available/jenkins`
3. **SSL 인증서**: `/etc/letsencrypt/`

### 10.2 S3 백업 (AWS)

```bash
# AWS CLI 설치
sudo apt install -y awscli

# S3 버킷 생성 (AWS Console 또는 CLI)
aws s3 mb s3://my-jenkins-backups

# 백업 스크립트 수정
# /home/ubuntu/backup-jenkins.sh 마지막에 추가:
aws s3 cp $BACKUP_DIR/$BACKUP_FILE s3://my-jenkins-backups/
```

### 10.3 복구 방법

```bash
# 1. 백업 다운로드
aws s3 cp s3://my-jenkins-backups/jenkins_backup_20240101_030000.tar.gz ./

# 2. Jenkins 컨테이너 중지
docker stop jenkins
docker rm jenkins

# 3. 백업 복원
tar xzf jenkins_backup_20240101_030000.tar.gz -C ~/jenkins_home

# 4. Jenkins 재시작
docker run -d \
  --name jenkins \
  --restart=always \
  -p 8080:8080 \
  -p 50000:50000 \
  -v ~/jenkins_home:/var/jenkins_home \
  jenkins/jenkins:lts
```

---

## 서버 배포 체크리스트

설치 완료 후 확인:

- [ ] EC2 인스턴스 실행 중
- [ ] Elastic IP 할당됨
- [ ] Security Group 설정 완료
- [ ] Jenkins 접속 가능 (HTTP/HTTPS)
- [ ] GitHub Webhook 연결됨
- [ ] PR 테스트 성공
- [ ] SSL 인증서 설정 (선택)
- [ ] 자동 백업 설정
- [ ] CloudWatch 알람 설정
- [ ] 비용 최적화 적용

---

## 다음 단계

### 고급 설정

1. **멀티 노드 구성**
   - Jenkins Master + Slave 구성
   - 병렬 빌드 속도 향상

2. **Docker 빌드 추가**
   - 애플리케이션 Docker 이미지 빌드
   - ECR/Docker Hub에 푸시

3. **배포 자동화 (CD)**
   - ECS/EKS로 자동 배포
   - Blue-Green 배포

4. **Slack 알림**
   - 빌드 성공/실패 알림
   - PR 리뷰 요청 알림

---

## 참고 자료

- [AWS EC2 문서](https://docs.aws.amazon.com/ec2/)
- [Jenkins 공식 문서](https://www.jenkins.io/doc/)
- [Let's Encrypt](https://letsencrypt.org/)
- [Nginx 문서](https://nginx.org/en/docs/)
