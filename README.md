# Spring WebSocket Chat

Spring Boot WebSocket으로 만든 간단한 채팅 예제입니다. 브라우저는 별도 프레임워크 없이 기본 `WebSocket` API를 사용합니다.

## 로컬 실행

```powershell
mvn spring-boot:run
```

브라우저에서 `http://localhost:8080`을 엽니다.

## Render 임시 배포

이 프로젝트는 Render Docker 배포용 파일을 포함합니다.

- `Dockerfile`: Maven으로 jar를 빌드한 뒤 Java 17 런타임에서 실행
- `render.yaml`: Render Blueprint 설정
- `/health`: Render 헬스 체크 엔드포인트

Render Dashboard에서 배포하는 경우:

1. `New` > `Blueprint` 또는 `Web Service`를 선택합니다.
2. GitHub 저장소 `msmsm0320/websocket-chat`을 연결합니다.
3. Blueprint를 쓰면 루트의 `render.yaml`을 선택합니다.
4. Web Service로 직접 만들면 Runtime은 `Docker`를 선택합니다.

배포 후 Render가 발급한 `https://...onrender.com` 주소로 접속하면 됩니다.

## 메시지 흐름

1. 브라우저가 `/ws/chat` WebSocket 엔드포인트로 접속합니다.
2. 접속 직후 `JOIN` 메시지를 보냅니다.
3. 채팅 입력 시 `CHAT` 메시지를 보냅니다.
4. 서버는 받은 메시지를 현재 접속 중인 모든 세션에 브로드캐스트합니다.
5. 입장/퇴장 시 서버가 `USERS` 메시지로 현재 접속자 목록을 다시 보냅니다.

사용자 이름은 서버에서 `이름 (IP)` 형태로 저장합니다. Render 배포 환경에서는 프록시 헤더인 `X-Forwarded-For`를 먼저 사용합니다.
