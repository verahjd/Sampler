// ExperimentRunner.java
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class ExperimentRunner {
    // CONFIGURABLE
    static final String FASTA_FILE = "sequence.fasta";
    static final String PATTERNS_FILE = "patterns.csv";
    static final String GROUND_TRUTH_FILE = "ground_truth.csv";
    static final String RESULTS_CSV = "results.csv";
    static final String SUMMARY_CSV = "summary.csv";

    static final int WARMUPS = 3;        // quick for development; set to 10 for final
    static final int MEASURED_RUNS = 10; // quick; set to 30 for final

    public static void main(String[] args) throws Exception {
        System.out.println("Loading corpus...");
        List<Map.Entry<String,String>> corpus = readFasta(FASTA_FILE);
        System.out.println("Loaded " + corpus.size() + " sequences.");

        List<PatternRow> patterns = readPatterns(PATTERNS_FILE);
        Map<Integer, List<GroundRow>> ground = readGroundTruth(GROUND_TRUTH_FILE);

        // Prepare output writers
        BufferedWriter resOut = Files.newBufferedWriter(Paths.get(RESULTS_CSV));
        resOut.write("pattern_id,algorithm,run,time_ns,comparisons,switch_count,correct,length,repetitive\n");

        List<SummaryRow> summaryRows = new ArrayList<>();

        int patternIndex = 0;
        for (PatternRow p : patterns) {
            patternIndex++;
            System.out.printf("Processing pattern %d/%d (id=%d len=%d)\n", patternIndex, patterns.size(), p.pattern_id, p.pattern.length());
            boolean repetitive = isRepetitive(p.pattern);

            // Warmups
            for (int i = 0; i < WARMUPS; i++) {
                HybridSearch.staticHybridSearch(corpus, p.pattern);
                HybridSearch.dynamicHybridSearch(corpus, p.pattern);
            }

            // Measured runs
            List<Long> staticTimes = new ArrayList<>();
            List<Long> staticComps = new ArrayList<>();
            List<Long> dynamicTimes = new ArrayList<>();
            List<Long> dynamicComps = new ArrayList<>();
            List<Integer> dynamicSwitches = new ArrayList<>();
            boolean allStaticCorrect = true;
            boolean allDynamicCorrect = true;

            for (int run = 1; run <= MEASURED_RUNS; run++) {
                // Static
                long t0 = System.nanoTime();
                HybridSearch.SearchResult sres = HybridSearch.staticHybridSearch(corpus, p.pattern);
                long t1 = System.nanoTime();
                long tStatic = t1 - t0;
                staticTimes.add(tStatic);
                staticComps.add(sres.comparisons);
                boolean sCorrect = checkCorrectness(sres.matches, ground.get(p.pattern_id));
                allStaticCorrect = allStaticCorrect && sCorrect;
                resOut.write(String.format("%d,static,%d,%d,%d,%d,%b,%d\n",
                        p.pattern_id, run, tStatic, sres.comparisons, 0, sCorrect, p.pattern.length()));
                // Dynamic
                long d0 = System.nanoTime();
                HybridSearch.SearchResult dres = HybridSearch.dynamicHybridSearch(corpus, p.pattern);
                long d1 = System.nanoTime();
                long tDyn = d1 - d0;
                dynamicTimes.add(tDyn);
                dynamicComps.add(dres.comparisons);
                dynamicSwitches.add(dres.switchCount);
                boolean dCorrect = checkCorrectness(dres.matches, ground.get(p.pattern_id));
                allDynamicCorrect = allDynamicCorrect && dCorrect;
                resOut.write(String.format("%d,dynamic,%d,%d,%d,%d,%b,%d\n",
                        p.pattern_id, run, tDyn, dres.comparisons, dres.switchCount, dCorrect, p.pattern.length()));
            }
            resOut.flush();

            // prepare summary row
            double statTimeMs = staticTimes.stream().mapToLong(Long::longValue).average().orElse(0.0)/1e6;
            double dynTimeMs = dynamicTimes.stream().mapToLong(Long::longValue).average().orElse(0.0)/1e6;
            double statCompMean = staticComps.stream().mapToLong(Long::longValue).average().orElse(0.0);
            double dynCompMean = dynamicComps.stream().mapToLong(Long::longValue).average().orElse(0.0);
            double dynSwitchMean = dynamicSwitches.stream().mapToInt(Integer::intValue).average().orElse(0.0);

            summaryRows.add(new SummaryRow(p.pattern_id, p.pattern.length(), repetitive,
                    statTimeMs, dynTimeMs, statCompMean, dynCompMean, dynSwitchMean,
                    allStaticCorrect, allDynamicCorrect));
        }

        resOut.close();
        // write summary CSV
        BufferedWriter sumOut = Files.newBufferedWriter(Paths.get(SUMMARY_CSV));
        sumOut.write("pattern_id,length,repetitive,static_mean_time_ms,dynamic_mean_time_ms,static_mean_comps,dynamic_mean_comps,mean_switches,static_correct_all,dynamic_correct_all\n");
        for (SummaryRow s : summaryRows) {
            sumOut.write(String.format("%d,%d,%b,%.3f,%.3f,%.1f,%.1f,%.2f,%b,%b\n",
                    s.pattern_id, s.length, s.repetitive, s.static_mean_time_ms, s.dynamic_mean_time_ms,
                    s.static_mean_comps, s.dynamic_mean_comps, s.mean_switches, s.static_correct_all, s.dynamic_correct_all));
        }
        sumOut.close();

        System.out.println("Done. Results written to " + RESULTS_CSV + " and " + SUMMARY_CSV);
    }

    // ---------- helpers and simple data loaders ----------

    static List<Map.Entry<String,String>> readFasta(String path) throws IOException {
        List<Map.Entry<String,String>> list = new ArrayList<>();
        BufferedReader br = Files.newBufferedReader(Paths.get(path));
        String line;
        String header = null;
        StringBuilder sb = new StringBuilder();
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.charAt(0) == '>') {
                if (header != null) list.add(new AbstractMap.SimpleEntry<>(header, sb.toString().toUpperCase()));
                header = line.substring(1).trim();
                sb = new StringBuilder();
            } else {
                sb.append(line.trim());
            }
        }
        if (header != null) list.add(new AbstractMap.SimpleEntry<>(header, sb.toString().toUpperCase()));
        br.close();
        return list;
    }

    static List<PatternRow> readPatterns(String path) throws IOException {
        List<PatternRow> out = new ArrayList<>();
        BufferedReader br = Files.newBufferedReader(Paths.get(path));
        String header = br.readLine(); // skip header
        String line;
        while ((line = br.readLine()) != null) {
            String[] parts = splitCsvLine(line);
            if (parts.length < 5) continue;
            int pid = Integer.parseInt(parts[0]);
            String pattern = parts[4].trim().toUpperCase();
            out.add(new PatternRow(pid, pattern));
        }
        br.close();
        return out;
    }

    static Map<Integer, List<GroundRow>> readGroundTruth(String path) throws IOException {
        Map<Integer, List<GroundRow>> map = new HashMap<>();
        BufferedReader br = Files.newBufferedReader(Paths.get(path));
        String header = br.readLine(); // skip header
        String line;
        while ((line = br.readLine()) != null) {
            String[] parts = splitCsvLine(line);
            if (parts.length < 3) continue;
            int pid = Integer.parseInt(parts[0]);
            String src = parts[1];
            int idx = Integer.parseInt(parts[2]);
            map.computeIfAbsent(pid, k -> new ArrayList<>()).add(new GroundRow(pid, src, idx));
        }
        br.close();
        return map;
    }

    static boolean isRepetitive(String pattern) {
        int m = pattern.length();
        if (m == 0) return false;
        int max = 0;
        for (char c : new char[] {'A','C','G','T'}) {
            int cnt = 0;
            for (char ch : pattern.toCharArray()) if (ch == c) cnt++;
            if (cnt > max) max = cnt;
        }
        return ((double)max / m) >= 0.60;
    }

    // check correctness: ground contains rows or NONE/-1 entries
    static boolean checkCorrectness(List<HybridSearch.Match> found, List<GroundRow> groundList) {
        if (groundList == null) return false;
        // if ground shows NONE/-1, then found must be empty
        if (groundList.size() == 1 && groundList.get(0).source.equals("NONE") && groundList.get(0).index == -1) {
            return found.isEmpty();
        }
        // build set of ground matches
        Set<String> gt = new HashSet<>();
        for (GroundRow g : groundList) {
            if (g.index >= 0) gt.add(g.source + "@" + g.index);
        }
        // build set of found
        Set<String> fh = new HashSet<>();
        for (HybridSearch.Match m : found) fh.add(m.header + "@" + m.index);
        return fh.containsAll(gt); // require that all ground matches are found (found may have extras)
    }

    static String[] splitCsvLine(String line) {
        // simple csv split assuming no commas inside fields for our files
        return line.split(",", -1);
    }

    // small data classes
    static class PatternRow {
        int pattern_id;
        String pattern;
        PatternRow(int id, String pat) { pattern_id = id; pattern = pat; }
    }
    static class GroundRow {
        int pattern_id;
        String source;
        int index;
        GroundRow(int pid, String s, int i) { pattern_id = pid; source = s; index = i; }
    }
    static class SummaryRow {
        int pattern_id;
        int length;
        boolean repetitive;
        double static_mean_time_ms;
        double dynamic_mean_time_ms;
        double static_mean_comps;
        double dynamic_mean_comps;
        double mean_switches;
        boolean static_correct_all;
        boolean dynamic_correct_all;
        SummaryRow(int pid, int len, boolean rep,
                   double stt, double dyt, double stc, double dyc, double ms,
                   boolean sc, boolean dc) {
            pattern_id = pid; length = len; repetitive = rep;
            static_mean_time_ms = stt; dynamic_mean_time_ms = dyt;
            static_mean_comps = stc; dynamic_mean_comps = dyc; mean_switches = ms;
            static_correct_all = sc; dynamic_correct_all = dc;
        }
    }
}
