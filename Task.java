package app;

/** مهمة داخل المشروع (تدعم المهام الفرعية عبر parentId لتحقيق التسلسل الهرمي) */
public class Task {
    public final int id;
    public int projectId;
    public int parentId;          // -1 = مهمة رئيسية (عقدة جذر)
    public String title;
    public String description;
    public Priority priority;     // عالية / متوسطة / منخفضة
    public String dueDate;        // تاريخ التسليم yyyy-MM-dd
    public Status status;
    public int assigneeId;        // معرف عضو الفريق
    public double estimatedHours; // الجهد المتوقع بالساعات (يستخدمه Divide & Conquer)
    public long createdAt;

    public enum Priority { HIGH, MEDIUM, LOW }
    public enum Status { NEW, IN_PROGRESS, DONE }

    public Task(int id, int projectId, int parentId, String title, String description,
                Priority priority, String dueDate, Status status,
                int assigneeId, double estimatedHours) {
        this.id = id;
        this.projectId = projectId;
        this.parentId = parentId;
        this.title = title;
        this.description = description == null ? "" : description;
        this.priority = priority;
        this.dueDate = dueDate;
        this.status = status;
        this.assigneeId = assigneeId;
        this.estimatedHours = estimatedHours;
        this.createdAt = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return toJson();
    }

    public String toJson() {
        return "{\"id\":" + id
                + ",\"projectId\":" + projectId
                + ",\"parentId\":" + parentId
                + ",\"title\":" + Json.esc(title)
                + ",\"description\":" + Json.esc(description)
                + ",\"priority\":\"" + priority + "\""
                + ",\"dueDate\":\"" + dueDate + "\""
                + ",\"status\":\"" + status + "\""
                + ",\"assigneeId\":" + assigneeId
                + ",\"estimatedHours\":" + estimatedHours
                + ",\"createdAt\":" + createdAt
                + "}";
    }

    public static Task fromJson(String body, int id) {
        int projectId = (int) Json.num(body, "projectId", 1);
        int parentId = (int) Json.num(body, "parentId", -1);
        String title = Json.str(body, "title", "بدون عنوان");
        String desc = Json.str(body, "description", "");
        Priority pr;
        try { pr = Priority.valueOf(Json.str(body, "priority", "MEDIUM")); } catch (Exception e) { pr = Priority.MEDIUM; }
        String due = Json.str(body, "dueDate", "");
        Status st;
        try { st = Status.valueOf(Json.str(body, "status", "NEW")); } catch (Exception e) { st = Status.NEW; }
        int assignee = (int) Json.num(body, "assigneeId", -1);
        double hours = Json.num(body, "estimatedHours", 0);
        return new Task(id, projectId, parentId, title, desc, pr, due, st, assignee, hours);
    }
}
