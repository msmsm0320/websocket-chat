const statusElement = document.querySelector("#status");
const messagesElement = document.querySelector("#messages");
const connectButton = document.querySelector("#connectButton");
const chatForm = document.querySelector("#chatForm");
const senderInput = document.querySelector("#senderInput");
const messageInput = document.querySelector("#messageInput");
const sendButton = document.querySelector("#sendButton");
const userListElement = document.querySelector("#userList");
const userCountElement = document.querySelector("#userCount");

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

    setConnectedState(false, "\uC5F0\uACB0 \uC911");

    socket.addEventListener("open", () => {
        const sender = getSender();
        socket.send(JSON.stringify({ type: "JOIN", sender }));
        setConnectedState(true, "\uC5F0\uACB0\uB428");
    });

    socket.addEventListener("message", (event) => {
        const message = JSON.parse(event.data);

        if (message.type === "USERS") {
            renderUsers(message.users || []);
            return;
        }

        appendMessage(message);
    });

    socket.addEventListener("close", () => {
        setConnectedState(false, "\uC5F0\uACB0 \uC885\uB8CC");
        renderUsers([]);
    });

    socket.addEventListener("error", () => {
        setConnectedState(false, "\uC5F0\uACB0 \uC624\uB958");
    });
}

function setConnectedState(connected, statusText) {
    statusElement.textContent = statusText;
    connectButton.textContent = connected ? "\uB04A\uAE30" : "\uC5F0\uACB0";
    senderInput.disabled = connected;
    messageInput.disabled = !connected;
    sendButton.disabled = !connected;
}

function appendMessage(message) {
    const sender = getSender();
    const messageElement = document.createElement("article");
    const isSystem = message.type === "SYSTEM";
    const isMine = message.type === "CHAT" && message.sender.startsWith(`${sender} (`);

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

function renderUsers(users) {
    userCountElement.textContent = users.length;
    userListElement.replaceChildren();

    for (const user of users) {
        const item = document.createElement("li");
        item.className = "users__item";
        item.textContent = user;
        userListElement.appendChild(item);
    }
}

function getSender() {
    const value = senderInput.value.trim();
    return value || "\uC775\uBA85";
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