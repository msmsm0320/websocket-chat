# Spring WebSocket Chat

Spring Boot의 기본 WebSocket 기능으로 만든 간단한 채팅 예제입니다. 브라우저 쪽은 별도 프레임워크 없이 기본 `WebSocket` API만 사용합니다.

## 구조

- `ChatWebSocketConfig`: `/ws/chat` WebSocket 엔드포인트 등록
- `ChatWebSocketHandler`: 접속, 메시지 수신, 전체 브로드캐스트 처리
- `ChatMessage`: 클라이언트와 서버가 주고받는 메시지 형식
- `src/main/resources/static`: 브라우저 채팅 화면

## 실행

이 폴더에서 Maven으로 실행합니다.

```powershell
mvn spring-boot:run
```

실행 후 브라우저에서 아래 주소를 엽니다.

```text
http://localhost:8080
```

현재 PC에 Maven이 없다면 먼저 Maven을 설치하거나 IDE(IntelliJ IDEA, Eclipse 등)에서 `pom.xml`을 Maven 프로젝트로 열어 실행하면 됩니다.

## 메시지 흐름

1. 브라우저가 `ws://localhost:8080/ws/chat`으로 접속합니다.
2. 접속 직후 클라이언트가 `JOIN` 메시지를 보냅니다.
3. 채팅 입력 시 클라이언트가 `CHAT` 메시지를 보냅니다.
4. 서버는 받은 메시지를 현재 접속 중인 모든 세션에 다시 전송합니다.

예시 메시지:

```json
{
  "type": "CHAT",
  "sender": "홍길동",
  "content": "안녕하세요"
}
```
