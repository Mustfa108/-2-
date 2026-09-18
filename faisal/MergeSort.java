package app;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * خوارزمية الترتيب بالدمج (Merge Sort) — تنفيذ يدوي كامل بدون sort() الجاهزة.
 *
 * الفكرة: "فرّق تسد" خالصة — نقسم القائمة نصفين، نرتب كل نصف تكرارياً،
 * ثم ندمج النصفين المرتبين في قائمة واحدة مرتبة.
 *
 * التعقيد:
 *  - أفضل وأوسط وأسوأ الحالات: O(n log n) — القسمة دائماً متوازنة (نصفين متساويين)
 *    فيكون العمق log n، والدمج في كل مستوى يكلف n، لذا الأداء ثابت ومضمون.
 */
public class MergeSort {

    public static <T> long sort(List<T> list, Comparator<T> cmp) {
        comparisons = 0;
        if (list.size() > 1) {
            List<T> sorted = mergeSort(new ArrayList<>(list), cmp);
            // انسخ النتيجة إلى نفس القائمة
            for (int i = 0; i < sorted.size(); i++) list.set(i, sorted.get(i));
        }
        return comparisons;
    }

    private static long comparisons;

    private static <T> List<T> mergeSort(List<T> list, Comparator<T> cmp) {
        int n = list.size();
        if (n <= 1) return list; // حالة الأساس: عنصر واحد مرتب أصلاً

        int mid = n / 2;
        List<T> left = mergeSort(new ArrayList<>(list.subList(0, mid)), cmp);
        List<T> right = mergeSort(new ArrayList<>(list.subList(mid, n)), cmp);
        return merge(left, right, cmp); // احكم: ادمج الجزأين المرتبين
    }

    private static <T> List<T> merge(List<T> left, List<T> right, Comparator<T> cmp) {
        List<T> result = new ArrayList<>(left.size() + right.size());
        int i = 0, j = 0;
        while (i < left.size() && j < right.size()) {
            comparisons++;
            if (cmp.compare(left.get(i), right.get(j)) <= 0) {
                result.add(left.get(i++));
            } else {
                result.add(right.get(j++));
            }
        }
        while (i < left.size()) result.add(left.get(i++));
        while (j < right.size()) result.add(right.get(j++));
        return result;
    }
}
