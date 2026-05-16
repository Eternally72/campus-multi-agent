const apiBase = "/api";
let token = localStorage.getItem("campusToken") || "";
let currentUser = JSON.parse(localStorage.getItem("campusUser") || "null");
let activeSessionId = null;

const $ = (selector) => document.querySelector(selector);
const state = $("#authState");
const avatar = $("#avatar");

function setState(message) {
  state.textContent = message;
}

function setAvatar(name) {
  const source = (name || "").trim();
  avatar.textContent = source ? Array.from(source)[0].toUpperCase() : "?";
}

function headers(json = true) {
  const value = {};
  if (json) value["Content-Type"] = "application/json";
  if (token) value.Authorization = `Bearer ${token}`;
  return value;
}

async function request(path, options = {}) {
  const useJson = options.json !== false;
  const response = await fetch(`${apiBase}${path}`, {
    ...options,
    headers: { ...headers(useJson), ...(options.headers || {}) }
  });
  const payload = await response.json();
  if (!response.ok || !payload.success) {
    throw new Error(payload.message || "请求失败");
  }
  return payload.data;
}

function rememberAuth(data) {
  token = data.token;
  currentUser = data;
  localStorage.setItem("campusToken", token);
  localStorage.setItem("campusUser", JSON.stringify(data));
  setAvatar(data.displayName || data.username);
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

$("#materialFileForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = new FormData(event.target);
  if (!form.get("title")) {
    form.delete("title");
  }
  if (!form.get("courseId")) {
    form.delete("courseId");
  }
  await request("/materials/upload", { method: "POST", body: form, json: false });
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

$("#refreshMemoryBtn").addEventListener("click", loadMemory);

async function loadCourses() {
  if (!token) return;
  const data = await request("/courses");
  $("#courseList").innerHTML = data.length ? data.map((item) => `
    <article class="item">
      <strong>#${item.id} ${escapeHtml(item.name)}</strong>
      <div class="meta">${escapeHtml(item.term || "未设置学期")} · ${escapeHtml(item.teacher || "未设置教师")}</div>
    </article>
  `).join("") : `<div class="empty">还没有课程</div>`;
}

async function loadMaterials() {
  if (!token) return;
  const data = await request("/materials");
  $("#materialList").innerHTML = data.length ? data.map((item) => `
    <article class="item">
      <strong>${escapeHtml(item.title)}</strong>
      <div class="meta">状态：${item.status}${item.courseId ? ` · 课程 #${item.courseId}` : ""}${item.errorMessage ? ` · ${escapeHtml(item.errorMessage)}` : ""}</div>
      <button class="tiny" data-reindex-material-id="${item.id}">重建索引</button>
      <button class="tiny danger" data-material-id="${item.id}">删除</button>
    </article>
  `).join("") : `<div class="empty">还没有资料</div>`;
  document.querySelectorAll("[data-material-id]").forEach((button) => {
    button.addEventListener("click", async () => {
      if (!window.confirm("删除后会同步移除知识库索引，确认删除？")) {
        return;
      }
      try {
        await request(`/materials/${button.dataset.materialId}`, { method: "DELETE" });
        loadMaterials();
      } catch (error) {
        window.alert(error.message);
      }
    });
  });
  document.querySelectorAll("[data-reindex-material-id]").forEach((button) => {
    button.addEventListener("click", async () => {
      try {
        await request(`/materials/${button.dataset.reindexMaterialId}/reindex`, { method: "POST" });
        loadMaterials();
      } catch (error) {
        window.alert(error.message);
      }
    });
  });
}

async function loadTodos() {
  if (!token) return;
  const data = await request("/todos");
  $("#todoList").innerHTML = data.length ? data.map((item) => `
    <article class="item">
      <strong>${escapeHtml(item.title)}</strong>
      <div class="meta">${item.status} · ${item.dueDate || "未设置日期"} · ${escapeHtml(item.description || "")}</div>
    </article>
  `).join("") : `<div class="empty">还没有待办</div>`;
}

async function loadMemory() {
  if (!token) return;
  const [memories, candidates] = await Promise.all([
    request("/memory"),
    request("/memory/candidates")
  ]);
  $("#memoryCandidateList").innerHTML = candidates.length ? candidates.map((item) => `
    <article class="item">
      <strong>${escapeHtml(item.type)} · ${escapeHtml(item.key)}</strong>
      <div class="meta">${escapeHtml(item.value)} · 置信度 ${item.confidence ?? ""} · ${escapeHtml(item.reason || "")}</div>
      <button class="tiny" data-confirm-candidate-id="${item.id}">确认</button>
      <button class="tiny danger" data-reject-candidate-id="${item.id}">拒绝</button>
    </article>
  `).join("") : `<div class="empty">没有待确认记忆</div>`;
  $("#memoryList").innerHTML = memories.length ? memories.map((item) => `
    <article class="item">
      <strong>${escapeHtml(item.type)} · ${escapeHtml(item.key)}</strong>
      <div class="meta">${escapeHtml(item.value)} · 置信度 ${item.confidence ?? ""}${item.expiresAt ? ` · 过期 ${escapeHtml(item.expiresAt)}` : ""}</div>
      <button class="tiny danger" data-memory-type="${item.type}" data-memory-id="${item.id}">遗忘</button>
    </article>
  `).join("") : `<div class="empty">还没有长期记忆</div>`;
  document.querySelectorAll("[data-confirm-candidate-id]").forEach((button) => {
    button.addEventListener("click", async () => {
      await request(`/memory/candidates/${button.dataset.confirmCandidateId}/confirm`, { method: "POST" });
      loadMemory();
    });
  });
  document.querySelectorAll("[data-reject-candidate-id]").forEach((button) => {
    button.addEventListener("click", async () => {
      await request(`/memory/candidates/${button.dataset.rejectCandidateId}/reject`, { method: "POST" });
      loadMemory();
    });
  });
  document.querySelectorAll("[data-memory-id]").forEach((button) => {
    button.addEventListener("click", async () => {
      const path = button.dataset.memoryType === "preference"
        ? `/memory/preferences/${button.dataset.memoryId}`
        : `/memory/facts/${button.dataset.memoryId}`;
      await request(path, { method: "DELETE" });
      loadMemory();
    });
  });
}

function refreshAll() {
  Promise.allSettled([loadCourses(), loadMaterials(), loadTodos(), loadMemory()]);
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

if (token && currentUser) {
  setAvatar(currentUser.displayName || currentUser.username);
  setState(`已登录：${currentUser.displayName || currentUser.username}`);
  refreshAll();
} else {
  setAvatar("");
}
