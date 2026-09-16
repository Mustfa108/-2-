package app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * خادم HTTP مبني على مكتبة JDK القياسية — يقدم REST API + ملفات الواجهة الثابتة.
 * الخوارزميات كلها تعمل داخل Java (الخادم) والواجهة تعرض النتائج فقط.
 */
public class Main {

    static final Store store = new Store();
    static Path webRoot;

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        webRoot = Paths.get("web").toAbsolutePath();
        if (!Files.exists(webRoot)) webRoot = Paths.get("src", "..", "web").toAbsolutePath().normalize();
        store.seed();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(8));

        server.createContext("/api/", Main::handleApi);
        server.createContext("/", Main::handleStatic);

        server.start();
        System.out.println("✓ الخادم يعمل على المنفذ " + port);
        System.out.println("✓ افتح المتصفح على: http://localhost:" + port);
    }

    // ============================ API ============================

    static void handleApi(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();
        String query = ex.getRequestURI().getQuery();
        Map<String, String> q = parseQuery(query);

        try {
            String body = method.equals("POST") || method.equals("PUT") ? readBody(ex) : "";

            if (path.equals("/api/projects") && method.equals("GET")) {
                send(ex, 200, jsonList(new ArrayList<>(store.projects.values())));
            } else if (path.equals("/api/projects") && method.equals("POST")) {
                Project p = new Project(store.nextProjectId(),
                        Json.str(body, "name", "مشروع جديد"),
                        Json.str(body, "description", ""),
                        Json.str(body, "startDate", ""),
                        Json.str(body, "endDate", ""));
                store.projects.put(p.id, p);
                send(ex, 200, p.toJson());
            } else if (path.equals("/api/members") && method.equals("GET")) {
                send(ex, 200, jsonList(new ArrayList<>(store.members.values())));

            } else if (path.equals("/api/tasks") && method.equals("GET")) {
                int pid = Integer.parseInt(q.getOrDefault("projectId", "1"));
                send(ex, 200, tasksJson(pid));

            } else if (path.equals("/api/tasks") && method.equals("POST")) {
                Task t = Task.fromJson(body, store.nextTaskId());
                store.tasks.put(t.id, t);
                store.addLog(t.id, t.title, t.status, t.status);
                send(ex, 200, t.toJson());

            } else if (path.startsWith("/api/tasks/") && method.equals("PUT")) {
                int id = Integer.parseInt(path.substring("/api/tasks/".length()));
                Task t = store.tasks.get(id);
                if (t == null) { send(ex, 404, "{\"error\":\"task not found\"}"); return; }
                Task.Status old = t.status;
                String st = Json.str(body, "status", null);
                if (st != null) {
                    try { t.status = Task.Status.valueOf(st); } catch (Exception ignored) {}
                    if (old != t.status) store.addLog(t.id, t.title, old, t.status);
                }
                send(ex, 200, t.toJson());

            } else if (path.equals("/api/logs") && method.equals("GET")) {
                synchronized (store.logs) {
                    List<LogEntry> sorted = new ArrayList<>(store.logs);
                    sorted.sort(Comparator.comparingLong((LogEntry l) -> l.timestamp).reversed());
                    send(ex, 200, jsonList(sorted));
                }

            } else if (path.equals("/api/sort") && method.equals("GET")) {
                handleSort(ex, q);

            } else if (path.equals("/api/effort") && method.equals("GET")) {
                handleEffort(ex, q);

            } else if (path.equals("/api/report") && method.equals("GET")) {
                handleReport(ex, q);

            } else {
                send(ex, 404, "{\"error\":\"not found\"}");
            }
        } catch (Exception e) {
            send(ex, 500, "{\"error\":" + Json.esc(String.valueOf(e.getMessage())) + "}");
        }
    }

    static String tasksJson(int projectId) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Task t : store.tasks.values()) {
            if (t.projectId != projectId) continue;
            if (!first) sb.append(",");
            sb.append(t.toJson());
            first = false;
        }
        return sb.append("]").toString();
    }

    /** خوارزميات الترتيب: Quick Sort وMerge Sort مع قياس زمن التنفيذ لكل منهما */
    static void handleSort(HttpExchange ex, Map<String, String> q) throws IOException {
        int pid = Integer.parseInt(q.getOrDefault("projectId", "1"));
        String by = q.getOrDefault("by", "priority"); // priority | due

        Comparator<Task> cmp;
        if (by.equals("due")) {
            // الأقرب موعداً أولاً (التواريخ بصيغة yyyy-MM-dd فالمقارنة النصية تكفي)
            cmp = Comparator.comparing(t -> t.dueDate == null || t.dueDate.isEmpty() ? "9999-12-31" : t.dueDate);
        } else {
            // الأولوية الأعلى أولاً: HIGH ثم MEDIUM ثم LOW
            Map<Task.Priority, Integer> rank = Map.of(
                    Task.Priority.HIGH, 1, Task.Priority.MEDIUM, 2, Task.Priority.LOW, 3);
            cmp = Comparator.comparingInt((Task t) -> rank.get(t.priority));
        }

        List<Task> base = new ArrayList<>();
        for (Task t : store.tasks.values()) if (t.projectId == pid) base.add(t);

        // Quick Sort
        List<Task> qs = new ArrayList<>(base);
        long t0 = System.nanoTime();
        long qCmp = QuickSort.sort(qs, cmp);
        double quickMs = (System.nanoTime() - t0) / 1_000_000.0;

        // Merge Sort
        List<Task> ms = new ArrayList<>(base);
        long t1 = System.nanoTime();
        long mCmp = MergeSort.sort(ms, cmp);
        double mergeMs = (System.nanoTime() - t1) / 1_000_000.0;

        StringBuilder sb = new StringBuilder();
        sb.append("{\"by\":").append(Json.esc(by))
          .append(",\"count\":").append(base.size())
          .append(",\"quick\":{\"ms\":").append(quickMs)
          .append(",\"comparisons\":").append(qCmp)
          .append(",\"tasks\":[").append(joinIds(qs)).append("]}")
          .append(",\"merge\":{\"ms\":").append(mergeMs)
          .append(",\"comparisons\":").append(mCmp)
          .append(",\"tasks\":[").append(joinIds(ms)).append("]}}");
        send(ex, 200, sb.toString());
    }

    static String joinIds(List<Task> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(list.get(i).toJson());
        }
        return sb.toString();
    }

    /** فرّق تسد: حساب إجمالي الجهد من الشجرة الهرمية للمهام */
    static void handleEffort(HttpExchange ex, Map<String, String> q) throws IOException {
        int pid = Integer.parseInt(q.getOrDefault("projectId", "1"));
        Map<Integer, List<Task>> children = store.childrenIndex();

        List<Task> roots = new ArrayList<>();
        for (Task t : store.tasks.values()) {
            if (t.projectId == pid && (t.parentId <= 0 || !store.tasks.containsKey(t.parentId))) roots.add(t);
        }
        roots.sort(Comparator.comparingInt(t -> t.id));

        double total = 0;
        int nodes = 0;
        int maxDepth = 0;
        StringBuilder rootsJson = new StringBuilder();
        List<String> treeLines = new ArrayList<>();

        for (int i = 0; i < roots.size(); i++) {
            Task r = roots.get(i);
            EffortCalculator.NodeResult res = EffortCalculator.calculate(r, children);
            total += res.subtreeHours;
            nodes += res.nodeCount;
            if (res.depth > maxDepth) maxDepth = res.depth;
            if (i > 0) rootsJson.append(",");
            rootsJson.append("{\"taskId\":").append(r.id)
                     .append(",\"title\":").append(Json.esc(r.title))
                     .append(",\"hours\":").append(res.subtreeHours)
                     .append(",\"nodeCount\":").append(res.nodeCount)
                     .append(",\"depth\":").append(res.depth).append("}");
            if (i > 0) treeLines.add("");
            treeLines.addAll(EffortCalculator.callTreeLines(r, children));
        }

        StringBuilder treeJson = new StringBuilder("[");
        for (int i = 0; i < treeLines.size(); i++) {
            if (i > 0) treeJson.append(",");
            treeJson.append(Json.esc(treeLines.get(i)));
        }
        treeJson.append("]");

        StringBuilder sb = new StringBuilder();
        sb.append("{\"projectId\":").append(pid)
          .append(",\"totalHours\":").append(total)
          .append(",\"nodeCount\":").append(nodes)
          .append(",\"maxDepth\":").append(maxDepth)
          .append(",\"roots\":[").append(rootsJson).append("]")
          .append(",\"callTree\":").append(treeJson)
          .append("}");
        send(ex, 200, sb.toString());
    }

    /** تقرير بسيط: نسبة الإنجاز الكلية + المهام المنجزة لكل عضو */
    static void handleReport(HttpExchange ex, Map<String, String> q) throws IOException {
        int pid = Integer.parseInt(q.getOrDefault("projectId", "1"));

        List<Task> projectTasks = new ArrayList<>();
        for (Task t : store.tasks.values()) if (t.projectId == pid) projectTasks.add(t);

        int total = projectTasks.size();
        int done = 0;
        for (Task t : projectTasks) if (t.status == Task.Status.DONE) done++;
        double percent = total == 0 ? 0 : Math.round(done * 10000.0 / total) / 100.0;

        StringBuilder membersJson = new StringBuilder();
        List<Member> mems = new ArrayList<>(store.members.values());
        mems.sort(Comparator.comparingInt(m -> m.id));
        for (int i = 0; i < mems.size(); i++) {
            Member m = mems.get(i);
            int mt = 0, md = 0;
            double hours = 0;
            for (Task t : projectTasks) {
                if (t.assigneeId == m.id) {
                    mt++;
                    if (t.status == Task.Status.DONE) { md++; }
                    hours += t.estimatedHours;
                }
            }
            double mp = mt == 0 ? 0 : Math.round(md * 10000.0 / mt) / 100.0;
            if (i > 0) membersJson.append(",");
            membersJson.append("{\"id\":").append(m.id)
                       .append(",\"name\":").append(Json.esc(m.name))
                       .append(",\"total\":").append(mt)
                       .append(",\"done\":").append(md)
                       .append(",\"hours\":").append(hours)
                       .append(",\"percent\":").append(mp).append("}");
        }

        String json = "{\"projectId\":" + pid
                + ",\"total\":" + total
                + ",\"done\":" + done
                + ",\"percent\":" + percent
                + ",\"members\":[" + membersJson + "]}";
        send(ex, 200, json);
    }

    // ============================ Static files ============================

    static void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        Path file = webRoot.resolve(path.substring(1)).normalize();
        if (!file.startsWith(webRoot) || !Files.exists(file) || Files.isDirectory(file)) {
            // دعم SPA: أعد الفهرس
            file = webRoot.resolve("index.html");
            if (!Files.exists(file)) { send(ex, 404, "404 — الملف غير موجود"); return; }
        }
        String mime = "text/html; charset=utf-8";
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".css")) mime = "text/css; charset=utf-8";
        else if (name.endsWith(".js")) mime = "application/javascript; charset=utf-8";
        else if (name.endsWith(".svg")) mime = "image/svg+xml";
        else if (name.endsWith(".png")) mime = "image/png";
        byte[] bytes = Files.readAllBytes(file);
        ex.getResponseHeaders().set("Content-Type", mime);
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    // ============================ helpers ============================

    static String jsonList(List<?> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(",");
            Object o = items.get(i);
            sb.append(o instanceof LogEntry ? ((LogEntry) o).toJson() : o.toString());
        }
        return sb.append("]").toString();
    }

    static String readBody(HttpExchange ex) throws IOException {
        InputStream is = ex.getRequestBody();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
        return bos.toString(StandardCharsets.UTF_8);
    }

    static Map<String, String> parseQuery(String query) {
        Map<String, String> m = new HashMap<>();
        if (query == null || query.isEmpty()) return m;
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                m.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                      URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
            }
        }
        return m;
    }

    static void send(HttpExchange ex, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }
}
