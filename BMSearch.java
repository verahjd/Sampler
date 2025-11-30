// BMSearch.java
import java.util.*;

public class BMSearch {
    // Search using Boyer-Moore bad-character heuristic only.
    // comparisons[0] increments per char comparison.
    // smallShiftInfo[0] will be set to the maximum consecutive small shifts encountered (shift<=1).
    public static List<Integer> search(String text, String pattern, long[] comparisons, int[] smallShiftInfo) {
        List<Integer> matches = new ArrayList<>();
        int n = text.length();
        int m = pattern.length();
        if (m == 0) {
            if (smallShiftInfo != null) smallShiftInfo[0] = 0;
            return matches;
        }

        int[] last = new int[256];
        Arrays.fill(last, -1);
        for (int i = 0; i < m; i++) last[pattern.charAt(i)] = i;

        int s = 0;
        int consecutiveSmallShifts = 0;
        int maxConsecutive = 0;

        while (s <= n - m) {
            int j = m - 1;
            // compare from right to left
            while (j >= 0) {
                comparisons[0]++;
                if (pattern.charAt(j) == text.charAt(s + j)) j--;
                else break;
            }
            if (j < 0) {
                matches.add(s);
                // full shift
                s += (m > 0) ? m : 1;
                consecutiveSmallShifts = 0;
            } else {
                comparisons[0]++; // count the mismatch check already done above
                char mism = text.charAt(s + j);
                int lo = (mism < 256) ? last[mism] : -1;
                int shift = Math.max(1, j - lo);
                if (shift <= 1) {
                    consecutiveSmallShifts++;
                    if (consecutiveSmallShifts > maxConsecutive) maxConsecutive = consecutiveSmallShifts;
                } else {
                    consecutiveSmallShifts = 0;
                }
                s += shift;
            }
        }

        if (smallShiftInfo != null) smallShiftInfo[0] = maxConsecutive;
        return matches;
    }
}
