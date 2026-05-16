const apiBase = "/api";
let token = localStorage.getItem("campusToken") || "";
let activeSessionId = null;

const $ = (selector) => document.querySelector(selector);
const state = $("#authState");

function setState(message) {
  state.textContent = message;
}

function headers(json = true) {
  const value = {};
  if (json) value["Content-Type"] = "application/json";
  if (token) value.Authorization = `Bearer ${token}`;
  return value;
}

async function request(path, options = {}) {
  const response = await fetch(`${apiBase}${path}`, {
    ...options,
    headers: { ...headers(options.json !== false), ...(options.headers || {}) }
  });
  const payload = await response.json();
  if (!response.ok || !payload.success) {
    throw new Error(payload.message || "请求失败");
  }
  return payload.data;
}

function rememberAuth(data) {
  token = data.token;
  localStorage.setItem("campusToken", token);
  setState(`已登录：${data.displayName}`);
  refreshAll();
}

$("#loginBtn").addEventListener("click", async () => {
  try {
    const data = await request("/auth/login", {
      method: "POST",
      body: JSON.stringify({ username: $("#username").value, password: $("#password").value })
    });
    rememberAuth(data);
  } catch (error) {
    setState(error.message);
  }
});

$("#registerBtn").addEventListener("click", async () => {
  try {
    const data = await request("/auth/register", {
      method: "POST",
      body: JSON.stringify({
        username: $("#username").value,
        password: $("#password").value,
        displayName: $("#displayName").value || $("#username").value
      })
    });
    rememberAuth(data);
  } catch (error) {
    setState(error.message);
  }
});

document.querySelectorAll(".nav button").forEach((button) => {
  button.addEventListener("click", () => {
    document.querySelectorAll(".nav button").forEach((item) => item.classList.remove("active"));
    document.querySelectorAll(".view").forEach((item) => item.classList.remove("active"));
    button.classList.add("active");
    $(`#${button.dataset.view}`).classList.add("active");
  });
});

function addMessage(role, text, agentType = "") {
  const node = document.createElement("div");
  node.className = `message ${role}`;
  node.innerHTML = agentType ? `<span class="tag">${agentType}</span>\n${escapeHtml(text)}` : escapeHtml(text);
  $("#messages").appendChild(node);
  $("#messages").scrollTop = $("#messages").scrollHeight;
}

$("#newSessionBtn").addEventListener("click", () => {
  activeSessionId = null;
  $("#messages").innerHTML = "";
});

$("#sendBtn").addEventListener("click", async () => {
  const input = $("#chatInput");
  const message = input.value.trim();
  if (!message) return;
  input.value = "";
  addMessage("user", message);
  try {
    const data = await request("/agent/chat", {
      method: "POST",
      body: JSON.stringify({ sessionId: activeSessionId, message })
    });
    activeSessionId = data.sessionId;
    addMessage("assistant", data.answer, data.agentType);
  } catch (error) {
    addMessage("assistant", error.message, "ERROR");
  }
});

$("#courseForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = new FormData(event.target);
  await request("/courses", {
    method: "POST",
    body: JSON.stringify(Object.fromEntries(form.entries()))
  });
  event.target.reset();
  loadCourses();
});

$("#materialForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = new FormData(event.target);
  const body = Object.fromEntries(form.entries());
  body.courseId = body.courseId ? Number(body.courseId) : null;
  await request("/materials", { method: "POST", body: JSON.stringify(body) });
  event.target.reset();
  loadMaterials();
});

$("#todoForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = new FormData(event.target);
  const body = Object.fromEntries(form.entries());
  body.dueDate = body.dueDate || null;
  await request("/todos", { method: "POST", body: JSON.stringify(body) });
  event.target.reset();
  loadTodos();
});

async function loadCourses() {
  if (!token) return;
  const data = await request("/courses");
  $("#courseList").innerHTML = data.map((item) => `
    <article class="item">
      <strong>#${item.id} ${escapeHtml(item.name)}</strong>
      <div class="meta">${escapeHtml(item.term || "未设置学期")} · ${escapeHtml(item.teacher || "未设置教师")}</div>
    </article>
  `).join("");
}

async function loadMaterials() {
  if (!token) return;
  const data = await request("/materials");
  $("#materialList").innerHTML = data.map((item) => `
    <article class="item">
      <strong>${escapeHtml(item.title)}</strong>
      <div class="meta">状态：${item.status}${item.courseId ? ` · 课程 #${item.courseId}` : ""}${item.errorMessage ? ` · ${escapeHtml(item.errorMessage)}` : ""}</div>
    </article>
  `).join("");
}

async function loadTodos() {
  if (!token) return;
  const data = await request("/todos");
  $("#todoList").innerHTML = data.map((item) => `
    <article class="item">
      <strong>${escapeHtml(item.title)}</strong>
      <div class="meta">${item.status} · ${item.dueDate || "未设置日期"} · ${escapeHtml(item.description || "")}</div>
    </article>
  `).join("");
}

function refreshAll() {
  Promise.allSettled([loadCourses(), loadMaterials(), loadTodos()]);
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

if (token) {
  setState("已保存登录态");
  refreshAll();
}
