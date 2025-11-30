// HybridSearch.java
import java.util.*;

public class HybridSearch {
    // Static hybrid rule: pattern length <= 30 -> KMP, else BM
    public static SearchResult staticHybridSearch(List<Map.Entry<String,String>> corpus, String pattern) {
        long[] comps = new long[] {0L};
        int[] smallShift = new int[] {0};
        List<Match> allMatches = new ArrayList<>();

        for (Map.Entry<String,String> e : corpus) {
            String header = e.getKey();
            String seq = e.getValue();
            List<Integer> ms;
            if (pattern.length() <= 30) {
                ms = KMPSearch.search(seq, pattern, comps);
            } else {
                ms = BMSearch.search(seq, pattern, comps, smallShift);
            }
            for (int pos : ms) allMatches.add(new Match(header, pos));
        }
        return new SearchResult(allMatches, comps[0], 0);
    }

    // Dynamic hybrid:
    //  - initial choice: if dominant base >= 0.6 -> start with KMP else BM
    //  - if BM run shows stagnation (smallShiftCount >= 3) we run KMP as fallback for that sequence
    //  - if KMP run shows extremely high comparisons for that sequence (> COMP_THRESHOLD), run BM as fallback
    //  - switchCount counts how many fallback switches we performed across the corpus
    public static SearchResult dynamicHybridSearch(List<Map.Entry<String,String>> corpus, String pattern) {
        long[] comps = new long[] {0L};
        int switchCount = 0;
        List<Match> allMatches = new ArrayList<>();
        int m = pattern.length();

        // compute dominant base fraction
        int maxCount = 0;
        for (char c : new char[] {'A','C','G','T'}) {
            int cnt = 0;
            for (char ch : pattern.toCharArray()) if (ch == c) cnt++;
            if (cnt > maxCount) maxCount = cnt;
        }
        double dominant = (m>0) ? ((double)maxCount / m) : 0.0;
        boolean startKMP = dominant >= 0.60;

        final long KMP_COMP_THRESHOLD = 200000; // very high; adjust if you want

        for (Map.Entry<String,String> e : corpus) {
            String header = e.getKey();
            String seq = e.getValue();

            if (startKMP) {
                long[] compsLocal = new long[] {0L};
                List<Integer> msK = KMPSearch.search(seq, pattern, compsLocal);
                comps[0] += compsLocal[0];
                for (int pos : msK) allMatches.add(new Match(header, pos));
                if (compsLocal[0] > KMP_COMP_THRESHOLD) {
                    // fallback to BM due to high comparisons
                    switchCount++;
                    long[] compsLocal2 = new long[] {0L};
                    int[] smallShift = new int[] {0};
                    List<Integer> msB = BMSearch.search(seq, pattern, compsLocal2, smallShift);
                    comps[0] += compsLocal2[0];
                    for (int pos : msB) {
                        if (!containsMatch(allMatches, header, pos)) allMatches.add(new Match(header, pos));
                    }
                }
            } else {
                // start with BM
                long[] compsLocal = new long[] {0L};
                int[] smallShift = new int[] {0};
                List<Integer> msB = BMSearch.search(seq, pattern, compsLocal, smallShift);
                comps[0] += compsLocal[0];
                for (int pos : msB) allMatches.add(new Match(header, pos));

                if (smallShift[0] >= 3) {
                    // stagnation detected, fallback to KMP from start (safe choice)
                    switchCount++;
                    long[] compsLocal2 = new long[] {0L};
                    List<Integer> msK = KMPSearch.search(seq, pattern, compsLocal2);
                    comps[0] += compsLocal2[0];
                    for (int pos : msK) {
                        if (!containsMatch(allMatches, header, pos)) allMatches.add(new Match(header, pos));
                    }
                }
            }
        }

        return new SearchResult(allMatches, comps[0], switchCount);
    }

    private static boolean containsMatch(List<Match> list, String header, int pos) {
        for (Match m : list) if (m.header.equals(header) && m.index == pos) return true;
        return false;
    }

    // small helper classes to return results
    public static class Match {
        public final String header;
        public final int index;
        public Match(String h, int i) { header = h; index = i; }
    }

    public static class SearchResult {
        public final List<Match> matches;
        public final long comparisons;
        public final int switchCount;
        public SearchResult(List<Match> matches, long comparisons, int switchCount) {
            this.matches = matches;
            this.comparisons = comparisons;
            this.switchCount = switchCount;
        }
    }
}
