package app;

/**
 * أدوات مساعدة بسيطة للتعامل مع JSON بدون مكتبات خارجية.
 * تدعم كتابة نصوص JSON وأخذ قيم مسطحة (flat) من جسم الطلب.
 */
public class Json {
    public static String esc(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    /** يبحث عن "key":"value" ويعيد value */
    public static String str(String json, String key, String def) {
        String k = "\"" + key + "\"";
        int i = json.indexOf(k);
        if (i < 0) return def;
        int colon = json.indexOf(':', i + k.length());
        if (colon < 0) return def;
        int q1 = json.indexOf('"', colon + 1);
        if (q1 < 0) return def;
        StringBuilder sb = new StringBuilder();
        for (int j = q1 + 1; j < json.length(); j++) {
            char c = json.charAt(j);
            if (c == '\\' && j + 1 < json.length()) {
                char n = json.charAt(++j);
                if (n == 'n') sb.append('\n');
                else if (n == 't') sb.append('\t');
                else if (n == 'r') sb.append('\r');
                else if (n == 'u') { sb.append((char) Integer.parseInt(json.substring(j + 1, j + 5), 16)); j += 4; }
                else sb.append(n);
            } else if (c == '"') break;
            else sb.append(c);
        }
        return sb.toString();
    }

    /** يبحث عن "key":number ويعيده */
    public static double num(String json, String key, double def) {
        String k = "\"" + key + "\"";
        int i = json.indexOf(k);
        if (i < 0) return def;
        int colon = json.indexOf(':', i + k.length());
        if (colon < 0) return def;
        int j = colon + 1;
        while (j < json.length() && (Character.isWhitespace(json.charAt(j)) || json.charAt(j) == '"')) j++;
        int start = j;
        while (j < json.length() && "-+.eE0123456789".indexOf(json.charAt(j)) >= 0) j++;
        try { return Double.parseDouble(json.substring(start, j)); }
        catch (Exception e) { return def; }
    }
}
