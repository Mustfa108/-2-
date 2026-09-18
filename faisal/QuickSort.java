package app;

import java.util.Comparator;
import java.util.List;

/**
 * خوارزمية الترتيب السريع (Quick Sort) — تنفيذ يدوي كامل بدون sort() الجاهزة.
 *
 * الفكرة: اختيار عنصر محوري (Pivot) وتقسيم القائمة بحيث تصغر العناصر عنه يميناً
 * وتكبر عنه يساراً، ثم الترتيب التكراري (Recursive) لكل جزء على حدة.
 *
 * التعقيد:
 *  - أفضل وأوسط الحالات: O(n log n) — لأن القسمة المتوازنة تنتج شجرة استدعاءات بعمق log n
 *    وفي كل مستوى نعالج n عنصراً إجمالاً.
 *  - أسوأ حالة: O(n²) — عندما يكون المحور دائماً أصغر/أكبر عنصر (مثل قائمة مرتبة مسبقاً
 *    مع اختيار آخر عنصر كمحور)، فتصبح القسمة غير متوازنة والعمق n.
 */
public class QuickSort {

    /** ترتيب القائمة تصاعدياً حسب المقارنة المعطاة، ويعيد عدد المقارنات المنفذة. */
    public static <T> long sort(List<T> list, Comparator<T> cmp) {
        comparisons = 0;
        quickSort(list, 0, list.size() - 1, cmp);
        return comparisons;
    }

    private static long comparisons;

    private static <T> void quickSort(List<T> list, int low, int high, Comparator<T> cmp) {
        if (low < high) {
            int p = partition(list, low, high, cmp);
            quickSort(list, low, p - 1, cmp);   // فرّق: الجزء الأيسر
            quickSort(list, p + 1, high, cmp);  // فرّق: الجزء الأيمن
        }
    }

    /** قسمة Lomuto: آخر عنصر كمحور */
    private static <T> int partition(List<T> list, int low, int high, Comparator<T> cmp) {
        T pivot = list.get(high);
        int i = low - 1;
        for (int j = low; j < high; j++) {
            comparisons++;
            if (cmp.compare(list.get(j), pivot) <= 0) {
                i++;
                swap(list, i, j);
            }
        }
        swap(list, i + 1, high);
        return i + 1;
    }

    private static <T> void swap(List<T> list, int i, int j) {
        T tmp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, tmp);
    }
}
