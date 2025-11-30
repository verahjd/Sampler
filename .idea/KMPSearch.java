// KMPSearch.java
import java.util.*;

public class KMPSearch {
    // Search pattern in text. comparisons[0] is incremented for each character comparison.
    public static List<Integer> search(String text, String pattern, long[] comparisons) {
        List<Integer> matches = new ArrayList<>();
        int n = text.length();
        int m = pattern.length();
        if (m == 0) return matches;

        // build prefix function (pi)
        int[] pi = new int[m];
        for (int i = 1, k = 0; i < m; i++) {
            while (k > 0 && pattern.charAt(k) != pattern.charAt(i)) {
                k = pi[k-1];
            }
            if (pattern.charAt(k) == pattern.charAt(i)) k++;
            pi[i] = k;
        }

        // search
        for (int i = 0, q = 0; i < n; i++) {
            // compare text[i] with pattern[q] (count each comparison)
            comparisons[0]++;
            while (q > 0 && pattern.charAt(q) != text.charAt(i)) {
                q = pi[q-1];
                // Note: the while loop's mismatch comparison was already counted above.
            }
            if (pattern.charAt(q) == text.charAt(i)) q++;
            if (q == m) {
                matches.add(i - m + 1);
                q = pi[q-1];
            }
        }
        return matches;
    }
}
