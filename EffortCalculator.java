package app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * تقنية "فرّق تسد" (Divide and Conquer) لحساب إجمالي الجهد المطلوب للمشروع.
 *
 * المشكلة: حساب إجمالي عدد الساعات المطلوبة للمشروع الكامل بناءً على الشجرة الهرمية
 * للمهام (مهمة رئيسية تحتوي مهاماً فرعية تحتوي بدورها مهاماً فرعية...).
 *
 * الحل: دالة Recursive — تستقبل المهمة الجذر، "تقسم" المشروع إلى أشجار فرعية
 * (الأبناء)، تحسب جهد كل شجرة فرعية باستدعاء ذاتي، ثم "تدمج" النتائج بجمعها:
 *
 *     effort(task) = task.hours + Σ effort(child)
 *
 * التعقيد: O(n) حيث n عدد المهام — لأن كل عقدة تُزار مرة واحدة فقط.
 * الفضاء: O(h) على Call Stack حيث h عمق الشجرة (أطول سلسلة استدعاءات ذاتية).
 */
public class EffortCalculator {

    /** نتيجة الحساب لشجرة مهمة واحدة (للعرض التفصيلي في الواجهة). */
    public static class NodeResult {
        public final Task task;
        public final double subtreeHours;
        public final int nodeCount;
        public final int depth;

        public NodeResult(Task task, double subtreeHours, int nodeCount, int depth) {
            this.task = task;
            this.subtreeHours = subtreeHours;
            this.nodeCount = nodeCount;
            this.depth = depth;
        }
    }

    /**
     * الدالة التكرارية (Recursive) "فرّق تسد":
     * فرّق: قسّم مهام الجذر إلى أشجار فرعية مستقلة.
     * احكم: احسب جهد كل شجرة فرعية باستدعاء ذاتي.
     * ادمج: اجمع جهود الأشجار الفرعية + جهد الجذر نفسه.
     */
    public static NodeResult calculate(Task root, Map<Integer, List<Task>> childrenOf) {
        return calc(root, childrenOf, 1);
    }

    private static NodeResult calc(Task node, Map<Integer, List<Task>> childrenOf, int depth) {
        double hours = node.estimatedHours;
        int count = 1;
        int maxChildDepth = depth;

        List<Task> children = childrenOf.get(node.id);
        if (children != null) {
            for (Task child : children) {          // فرّق
                NodeResult r = calc(child, childrenOf, depth + 1); // احكم (استدعاء ذاتي)
                hours += r.subtreeHours;           // ادمج
                count += r.nodeCount;
                if (r.depth > maxChildDepth) maxChildDepth = r.depth;
            }
        }
        return new NodeResult(node, hours, count, maxChildDepth);
    }

    /**
     * يمثل شجرة الاستدعاءات (Call Tree / Call Stack) كسلسلة نصية —
     * كل سطر عقدة، والإزاحة تمثل مستوى العمق في المكدس.
     */
    public static List<String> callTreeLines(Task root, Map<Integer, List<Task>> childrenOf) {
        List<String> lines = new ArrayList<>();
        buildLines(root, childrenOf, 0, lines);
        return lines;
    }

    private static void buildLines(Task node, Map<Integer, List<Task>> childrenOf, int depth, List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) sb.append("│  ");
        if (depth > 0) sb.append("├─ ");
        sb.append("calc(\"").append(node.title).append("\")");
        lines.add(sb.toString());

        List<Task> children = childrenOf.get(node.id);
        if (children != null) {
            for (Task child : children) buildLines(child, childrenOf, depth + 1, lines);
        }

        StringBuilder sb2 = new StringBuilder();
        for (int i = 0; i < depth; i++) sb2.append("│  ");
        if (depth > 0) sb2.append("├─ ");
        sb2.append("return ↩");
        lines.add(sb2.toString());
    }
}
