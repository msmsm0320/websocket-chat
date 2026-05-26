const statusElement = document.querySelector("#status");
const messagesElement = document.querySelector("#messages");
const connectButton = document.querySelector("#connectButton");
const chatForm = document.querySelector("#chatForm");
const senderInput = document.querySelector("#senderInput");
const messageInput = document.querySelector("#messageInput");
const sendButton = document.querySelector("#sendButton");

let socket;

connectButton.addEventListener("click", () => {
    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.close();
        return;
    }

    connect();
});

chatForm.addEventListener("submit", (event) => {
    event.preventDefault();

    const sender = getSender();
    const content = messageInput.value.trim();

    if (!content || !socket || socket.readyState !== WebSocket.OPEN) {
        return;
    }

    socket.send(JSON.stringify({
        type: "CHAT",
        sender,
        content
    }));

    messageInput.value = "";
    messageInput.focus();
});

function connect() {
    const protocol = window.location.protocol === "https:" ? "wss" : "ws";
    socket = new WebSocket(`${protocol}://${window.location.host}/ws/chat`);

    setConnectedState(false, "연결 중");

    socket.addEventListener("open", () => {
        const sender = getSender();
        socket.send(JSON.stringify({ type: "JOIN", sender }));
        setConnectedState(true, "연결됨");
    });

    socket.addEventListener("message", (event) => {
        const message = JSON.parse(event.data);
        appendMessage(message);
    });

    socket.addEventListener("close", () => {
        setConnectedState(false, "연결 종료");
    });

    socket.addEventListener("error", () => {
        setConnectedState(false, "연결 오류");
    });
}

function setConnectedState(connected, statusText) {
    statusElement.textContent = statusText;
    connectButton.textContent = connected ? "끊기" : "연결";
    senderInput.disabled = connected;
    messageInput.disabled = !connected;
    sendButton.disabled = !connected;
}

function appendMessage(message) {
    const sender = getSender();
    const messageElement = document.createElement("article");
    const isSystem = message.type === "SYSTEM";
    const isMine = message.type === "CHAT" && message.sender === sender;

    messageElement.className = "message";
    if (isSystem) {
        messageElement.classList.add("message--system");
        messageElement.textContent = message.content;
    } else {
        if (isMine) {
            messageElement.classList.add("message--mine");
        }

        const metaElement = document.createElement("div");
        metaElement.className = "message__meta";
        metaElement.textContent = `${message.sender} · ${formatTime(message.sentAt)}`;

        const contentElement = document.createElement("div");
        contentElement.className = "message__content";
        contentElement.textContent = message.content;

        messageElement.append(metaElement, contentElement);
    }

    messagesElement.appendChild(messageElement);
    messagesElement.scrollTop = messagesElement.scrollHeight;
}

function getSender() {
    const value = senderInput.value.trim();
    return value || "익명";
}

function formatTime(value) {
    if (!value) {
        return "";
    }

    return new Intl.DateTimeFormat("ko-KR", {
        hour: "2-digit",
        minute: "2-digit"
    }).format(new Date(value));
}
