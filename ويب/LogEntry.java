package app;

/** سجل زمني لتغييرات حالة المهام (Activity Log) */
public class LogEntry {
    public final int id;
    public final int taskId;
    public final String taskTitle;
    public final Task.Status fromStatus;
    public final Task.Status toStatus;
    public final long timestamp;

    public LogEntry(int id, int taskId, String taskTitle, Task.Status from, Task.Status to) {
        this.id = id;
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        this.fromStatus = from;
        this.toStatus = to;
        this.timestamp = System.currentTimeMillis();
    }

    private static String statusAr(Task.Status s) {
        switch (s) {
            case NEW: return "جديد";
            case IN_PROGRESS: return "قيد العمل";
            default: return "منتهي";
        }
    }

    public String toJson() {
        return "{\"id\":" + id
                + ",\"taskId\":" + taskId
                + ",\"taskTitle\":" + Json.esc(taskTitle)
                + ",\"from\":\"" + fromStatus + "\""
                + ",\"to\":\"" + toStatus + "\""
                + ",\"fromAr\":" + Json.esc(statusAr(fromStatus))
                + ",\"toAr\":" + Json.esc(statusAr(toStatus))
                + ",\"timestamp\":" + timestamp + "}";
    }
}
