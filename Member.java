package app;

public class Member {
    public final int id;
    public final String name;

    public Member(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public String toJson() {
        return "{\"id\":" + id + ",\"name\":" + Json.esc(name) + "}";
    }

    @Override
    public String toString() {
        return toJson();
    }
}
