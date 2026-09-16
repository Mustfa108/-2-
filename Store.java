package app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** قاعدة بيانات في الذاكرة (In-Memory) مع بيانات تجريبية جاهزة. */
public class Store {
    public final Map<Integer, Project> projects = new ConcurrentHashMap<>();
    public final Map<Integer, Task> tasks = new ConcurrentHashMap<>();
    public final Map<Integer, Member> members = new ConcurrentHashMap<>();
    public final List<LogEntry> logs = java.util.Collections.synchronizedList(new ArrayList<>());

    private final AtomicInteger projectSeq = new AtomicInteger(0);
    private final AtomicInteger taskSeq = new AtomicInteger(0);
    private final AtomicInteger logSeq = new AtomicInteger(0);

    public int nextProjectId() { return projectSeq.incrementAndGet(); }
    public int nextTaskId() { return taskSeq.incrementAndGet(); }
    public int nextLogId() { return logSeq.incrementAndGet(); }

    public void addLog(int taskId, String title, Task.Status from, Task.Status to) {
        logs.add(new LogEntry(nextLogId(), taskId, title, from, to));
    }

    /** خريطة: معرف المهمة ← قائمة أبنائها المباشرين (لبناء الشجرة الهرمية) */
    public Map<Integer, List<Task>> childrenIndex() {
        Map<Integer, List<Task>> m = new ConcurrentHashMap<>();
        for (Task t : tasks.values()) {
            if (t.parentId > 0 && tasks.containsKey(t.parentId)) {
                m.computeIfAbsent(t.parentId, k -> new ArrayList<>()).add(t);
            }
        }
        return m;
    }

    /** بيانات تجريبية: مشروع + 5 أعضاء + مهام هرمية */
    public void seed() {
        members.put(1, new Member(1, "أحمد"));
        members.put(2, new Member(2, "سارة"));
        members.put(3, new Member(3, "محمد"));
        members.put(4, new Member(4, "ليلى"));
        members.put(5, new Member(5, "خالد"));

        projects.put(1, new Project(1, "نظام إدارة المهام والمشاريع",
                "المشروع التكاملي لمادة تحليل وتصميم الخوارزميات — بناء لوحة كانبان مع تطبيق خوارزميات الترتيب وفرّق تسد.",
                "2026-09-01", "2026-12-20"));

        int t1 = taskSeq.incrementAndGet();
        tasks.put(t1, new Task(t1, 1, -1, "تحليل المتطلبات", "جمع المتطلبات الوظيفية والخوارزمية للمشروع",
                Task.Priority.HIGH, "2026-09-25", Task.Status.DONE, 1, 12));

        int t2 = taskSeq.incrementAndGet();
        tasks.put(t2, new Task(t2, 1, -1, "تصميم قاعدة البيانات", "تصميم هيكل المهام الهرمية والعلاقات",
                Task.Priority.HIGH, "2026-10-05", Task.Status.IN_PROGRESS, 2, 16));
        int t2a = taskSeq.incrementAndGet();
        tasks.put(t2a, new Task(t2a, 1, t2, "جداول المشاريع والمهام", "تصميم الجداول الأساسية",
                Task.Priority.MEDIUM, "2026-09-28", Task.Status.DONE, 2, 6));
        int t2b = taskSeq.incrementAndGet();
        tasks.put(t2b, new Task(t2b, 1, t2, "علاقة الأب-ابن", "نمذجة التسلسل الهرمي للمهام",
                Task.Priority.MEDIUM, "2026-10-02", Task.Status.IN_PROGRESS, 3, 4));

        int t3 = taskSeq.incrementAndGet();
        tasks.put(t3, new Task(t3, 1, -1, "تنفيذ خوارزميات الترتيب", "Quick Sort وMerge Sort يدوياً مع قياس زمن التنفيذ",
                Task.Priority.HIGH, "2026-10-20", Task.Status.NEW, 4, 20));
        int t3a = taskSeq.incrementAndGet();
        tasks.put(t3a, new Task(t3a, 1, t3, "Quick Sort", "تنفيذ القسمة والترتيب التكراري",
                Task.Priority.HIGH, "2026-10-12", Task.Status.IN_PROGRESS, 4, 8));
        int t3b = taskSeq.incrementAndGet();
        tasks.put(t3b, new Task(t3b, 1, t3, "Merge Sort", "تنفيذ الدمج وقياس الأداء",
                Task.Priority.MEDIUM, "2026-10-16", Task.Status.NEW, 5, 8));

        int t4 = taskSeq.incrementAndGet();
        tasks.put(t4, new Task(t4, 1, -1, "فرّق تسد لحساب الجهد", "دالة تكرارية لحساب إجمالي ساعات المشروع من الشجرة",
                Task.Priority.MEDIUM, "2026-11-01", Task.Status.NEW, 5, 10));
        int t4a = taskSeq.incrementAndGet();
        tasks.put(t4a, new Task(t4a, 1, t4, "بناء فهرس الأبناء", "تحويل قائمة المهام إلى شجرة",
                Task.Priority.LOW, "2026-10-25", Task.Status.NEW, 5, 3));

        int t5 = taskSeq.incrementAndGet();
        tasks.put(t5, new Task(t5, 1, -1, "لوحة كانبان والسجل الزمني", "ثلاثة أعمدة مع تسجيل تغييرات الحالة",
                Task.Priority.LOW, "2026-11-15", Task.Status.NEW, 3, 14));

        addLog(t1, "تحليل المتطلبات", Task.Status.NEW, Task.Status.IN_PROGRESS);
        addLog(t1, "تحليل المتطلبات", Task.Status.IN_PROGRESS, Task.Status.DONE);
        addLog(t2a, "جداول المشاريع والمهام", Task.Status.NEW, Task.Status.DONE);
        addLog(t3a, "Quick Sort", Task.Status.NEW, Task.Status.IN_PROGRESS);
    }
}
