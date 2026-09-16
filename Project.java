package app;

public class Project {
    public final int id;
    public String name;
    public String description;
    public String startDate;   // yyyy-MM-dd
    public String endDate;     // yyyy-MM-dd

    public Project(int id, String name, String description, String startDate, String endDate) {
        this.id = id;
        this.name = name;
        this.description = description == null ? "" : description;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String toJson() {
        return "{\"id\":" + id
                + ",\"name\":" + Json.esc(name)
                + ",\"description\":" + Json.esc(description)
                + ",\"startDate\":\"" + startDate + "\""
                + ",\"endDate\":\"" + endDate + "\"}";
    }

    @Override
    public String toString() {
        return toJson();
    }
}
