/* ================= الحالة العامة ================= */
const state = {
  tasks: [],
  members: [],
  logs: [],
  view: "kanban",
};

const PRIORITY_AR = { HIGH: "عالية", MEDIUM: "متوسطة", LOW: "منخفضة" };
const STATUS_AR = { NEW: "جديد", IN_PROGRESS: "قيد العمل", DONE: "منتهي" };

async function api(path, opts) {
  const res = await fetch(path, opts);
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

/* ================= التنقل بين الصفحات ================= */
document.getElementById("tabs").addEventListener("click", (e) => {
  const btn = e.target.closest(".tab");
  if (!btn) return;
  document.querySelectorAll(".tab").forEach((t) => t.classList.remove("active"));
  btn.classList.add("active");
  state.view = btn.dataset.view;
  document.querySelectorAll(".view").forEach((v) => v.classList.remove("active"));
  document.getElementById("view-" + state.view).classList.add("active");

  if (state.view === "effort") UI.loadEffort();
  if (state.view === "sorting") UI.runSort();
  if (state.view === "report") UI.loadReport();
  if (state.view === "log") UI.loadLog();
});

/* ================= الواجهة ================= */
const UI = {
  async refresh() {
    state.tasks = await api("/api/tasks?projectId=1");
    state.members = await api("/api/members");
    UI.renderKanban();
    if (state.view === "tasks") UI.renderTree();
    UI.fillAssignees();
  },

  fillAssignees() {
    const sel = document.getElementById("fAssignee");
    sel.innerHTML = state.members
      .map((m) => `<option value="${m.id}">${m.name}</option>`)
      .join("");
  },

  /* ---------- كانبان ---------- */
  renderKanban() {
    ["NEW", "IN_PROGRESS", "DONE"].forEach((st) => {
      const body = document.querySelector(`#col-${st} .kbody`);
      const list = state.tasks.filter((t) => t.status === st);
      document.getElementById("cnt-" + st).textContent = list.length;
      body.innerHTML = "";
      list.forEach((t) => body.appendChild(taskCard(t)));
    });
  },

  /* ---------- بطاقة مهمة ---------- */
  async move(id, dir) {
    const order = ["NEW", "IN_PROGRESS", "DONE"];
    const t = state.tasks.find((x) => x.id === id);
    const i = order.indexOf(t.status);
    const next = order[i + dir];
    if (!next) return;
    await api("/api/tasks/" + id, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ status: next }),
    });
    await UI.refresh();
    if (state.view === "tasks") UI.renderTree();
  },

  /* ---------- الشجرة الهرمية ---------- */
  renderTree() {
    const wrap = document.getElementById("taskTree");
    const roots = state.tasks.filter((t) => t.parentId <= 0);
    const kids = (id) => state.tasks.filter((t) => t.parentId === id);
    const build = (t) => {
      const children = kids(t.id);
      return `<li><div class="tnode ${t.parentId <= 0 ? "root" : ""}">
          <b>${esc(t.title)}</b>
          <span class="chip p-${t.priority}">${PRIORITY_AR[t.priority]}</span>
          <span class="chip">${STATUS_AR[t.status]}</span>
          <span class="chip">${t.estimatedHours} ساعة</span>
          <span class="chip">${esc(t.dueDate || "بدون تاريخ")}</span>
          <button class="btn sm ghost" onclick="UI.showTaskForm(${t.id})">+ مهمة فرعية</button>
        </div>
        ${children.length ? "<ul>" + children.map(build).join("") + "</ul>" : ""}</li>`;
    };
    wrap.innerHTML = `<ul class="tree">${roots.map(build).join("")}</ul>`;
  },

  /* ---------- نافذة المهمة ---------- */
  showTaskForm(parentId) {
    document.getElementById("fParent").value = parentId;
    document.getElementById("modalTitle").textContent =
      parentId > 0 ? "مهمة فرعية جديدة" : "مهمة جديدة";
    document.getElementById("taskModal").classList.add("open");
  },

  closeModal() {
    document.getElementById("taskModal").classList.remove("open");
  },

  async submitTask(e) {
    e.preventDefault();
    const body = {
      projectId: 1,
      parentId: parseInt(document.getElementById("fParent").value),
      title: document.getElementById("fTitle").value,
      description: document.getElementById("fDesc").value,
      priority: document.getElementById("fPriority").value,
      dueDate: document.getElementById("fDue").value,
      assigneeId: parseInt(document.getElementById("fAssignee").value),
      estimatedHours: parseFloat(document.getElementById("fHours").value || 0),
      status: "NEW",
    };
    await api("/api/tasks", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    document.getElementById("taskForm").reset();
    document.getElementById("fHours").value = 4;
    UI.closeModal();
    await UI.refresh();
  },

  /* ---------- الترتيب ---------- */
  async runSort() {
    const by = document.getElementById("sortBy").value;
    const data = await api("/api/sort?projectId=1&by=" + by);
    document.getElementById("qMs").textContent = data.quick.ms.toFixed(3);
    document.getElementById("mMs").textContent = data.merge.ms.toFixed(3);
    document.getElementById("qCmp").textContent = "عدد المقارنات: " + data.quick.comparisons.toLocaleString("ar");
    document.getElementById("mCmp").textContent = "عدد المقارنات: " + data.merge.comparisons.toLocaleString("ar");
    fillSortList("qList", data.quick.tasks);
    fillSortList("mList", data.merge.tasks);
  },

  /* ---------- فرّق تسد ---------- */
  async loadEffort() {
    const data = await api("/api/effort?projectId=1");
    document.getElementById("effortTotal").textContent = data.totalHours.toLocaleString("ar");
    document.getElementById("effortNodes").textContent = data.nodeCount.toLocaleString("ar");
    document.getElementById("effortDepth").textContent = data.maxDepth.toLocaleString("ar");
    const maxH = Math.max(...data.roots.map((r) => r.hours), 1);
    document.getElementById("effortRoots").innerHTML = data.roots
      .map(
        (r) => `<div class="rootrow">
          <div style="flex:1">
            <div>${esc(r.title)}</div>
            <div class="rootbar"><i style="width:${(r.hours / maxH) * 100}%"></i></div>
          </div>
          <div style="text-align:left; min-width:120px">
            <div class="hrs">${r.hours.toLocaleString("ar")} ساعة</div>
            <small class="chip">${r.nodeCount} مهمة</small>
          </div>
        </div>`
      )
      .join("");
    document.getElementById("callTree").textContent = data.callTree.join("\n");
  },

  /* ---------- التقرير ---------- */
  async loadReport() {
    const r = await api("/api/report?projectId=1");
    document.getElementById("ring").style.setProperty("--pct", r.percent);
    document.getElementById("ringPct").textContent = r.percent + "%";
    document.getElementById("reportDone").textContent = r.done;
    document.getElementById("reportTotal").textContent = r.total;
    const tb = document.getElementById("memberTable");
    tb.innerHTML =
      "<tr><th>العضو</th><th>المهام</th><th>منجزة</th><th>الجهد (ساعات)</th><th>نسبة الإنجاز</th></tr>" +
      r.members
        .map(
          (m) => `<tr>
            <td><b>${esc(m.name)}</b></td>
            <td>${m.total}</td>
            <td>${m.done}</td>
            <td>${m.hours.toLocaleString("ar")}</td>
            <td><div class="bar"><i style="width:${m.percent}%"></i></div> ${m.percent}%</td>
          </tr>`
        )
        .join("");
  },

  /* ---------- السجل ---------- */
  async loadLog() {
    state.logs = await api("/api/logs");
    document.getElementById("logList").innerHTML = state.logs
      .map((l) => {
        const when = new Date(l.timestamp).toLocaleString("ar-EG");
        if (l.from === l.to)
          return `<li><span class="log-create">➕ أُضيفت مهمة جديدة: <b>${esc(l.taskTitle)}</b></span><span class="when">${when}</span></li>`;
        return `<li><b>${esc(l.taskTitle)}</b>
            <span class="st-${l.from}">${STATUS_AR[l.from]}</span>
            <span class="logarrow">⟵</span>
            <span class="st-${l.to}">${STATUS_AR[l.to]}</span>
            <span class="when">${when}</span></li>`;
      })
      .join("");
  },
};

/* ================= أدوات مساعدة ================= */
function esc(s) {
  return String(s ?? "").replace(/[&<>"']/g, (c) =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c])
  );
}

function taskCard(t) {
  const member = state.members.find((m) => m.id === t.assigneeId);
  const subCount = state.tasks.filter((x) => x.parentId === t.id).length;
  const card = document.createElement("div");
  card.className = "tcard";
  card.innerHTML = `
    <div class="tactions">
      <button title="مهمة فرعية" onclick="UI.showTaskForm(${t.id})">＋</button>
    </div>
    <h4>${esc(t.title)} ${subCount ? `<span class="subcount">(${subCount} فرعية)</span>` : ""}</h4>
    ${t.description ? `<div class="tdesc">${esc(t.description)}</div>` : ""}
    <div class="tmeta">
      <span class="chip p-${t.priority}">${PRIORITY_AR[t.priority]}</span>
      ${member ? `<span class="chip member">👤 ${esc(member.name)}</span>` : ""}
      <span class="chip">${esc(t.dueDate || "بدون تاريخ")}</span>
      <span class="chip">${t.estimatedHours}س</span>
    </div>
    <div class="tmove">
      ${t.status !== "NEW" ? `<button onclick="UI.move(${t.id}, -1)">⟶ للخلف</button>` : ""}
      ${t.status !== "DONE" ? `<button onclick="UI.move(${t.id}, 1)">للأمام ⟵</button>` : ""}
    </div>`;
  return card;
}

function fillSortList(elId, tasks) {
  document.getElementById(elId).innerHTML = tasks
    .map(
      (t) => `<li>
        <span><b>${esc(t.title)}</b></span>
        <span>
          <span class="chip p-${t.priority}">${PRIORITY_AR[t.priority]}</span>
          <span class="chip">${esc(t.dueDate || "")}</span>
        </span>
      </li>`
    )
    .join("");
}

/* ================= تشغيل ================= */
document.getElementById("taskModal").addEventListener("click", (e) => {
  if (e.target.id === "taskModal") UI.closeModal();
});
UI.refresh();
