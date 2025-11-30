// Sampler.java (compile with: javac Sampler.java; run: java Sampler)
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Sampler {
    static final String FASTA = "sequence.fasta";
    static final String OUT_PATTERNS = "patterns.csv";
    static final String OUT_GROUND = "ground_truth.csv";
    static final int SEED = 12345;
    static final int SHORT = 15, MEDIUM = 15, LONG = 10;

    public static void main(String[] args) throws Exception {
        List<SeqEntry> entries = readFasta(FASTA);
        if(entries.isEmpty()) { System.err.println("No sequences found."); return; }
        Random r = new Random(SEED);
        List<Sample> samples = new ArrayList<>();
        sampleRange(entries, samples, r, SHORT, 5, 10);
        sampleRange(entries, samples, r, MEDIUM, 11, 50);
        sampleRange(entries, samples, r, LONG, 51, 200);
        writePatterns(samples);
        List<Ground> gt = computeGround(samples, entries);
        writeGround(gt);
        System.out.println("Wrote " + samples.size() + " samples and " + gt.size() + " ground rows.");
    }

    static class SeqEntry { String header, seq; SeqEntry(String h, String s){header=h;seq=s;} }
    static class Sample { String header; int start,len; String pat; Sample(String h,int s,int l,String p){header=h;start=s;len=l;pat=p;} }
    static class Ground { int pid; String header; int idx; Ground(int pid,String h,int idx){this.pid=pid;this.header=h;this.idx=idx;} }

    static List<SeqEntry> readFasta(String path) throws IOException {
        List<SeqEntry> list = new ArrayList<>();
        BufferedReader br = Files.newBufferedReader(Paths.get(path));
        String line, header=null;
        StringBuilder sb = new StringBuilder();
        while((line=br.readLine())!=null) {
            line=line.trim();
            if(line.isEmpty()) continue;
            if(line.charAt(0)=='>') {
                if(header!=null) { list.add(new SeqEntry(header, sb.toString())); }
                header = line.substring(1).trim();
                sb.setLength(0);
            } else {
                sb.append(line.toUpperCase());
            }
        }
        if(header!=null) list.add(new SeqEntry(header, sb.toString()));
        return list;
    }

    static void sampleRange(List<SeqEntry> entries, List<Sample> samples, Random r, int count, int minlen, int maxlen){
        int i=0;
        while(i<count) {
            SeqEntry e = entries.get(r.nextInt(entries.size()));
            if(e.seq.length() < minlen) continue;
            int maxStart = Math.max(0, e.seq.length() - minlen);
            int start = r.nextInt(maxStart+1);
            int L = minlen + r.nextInt(Math.max(1, Math.min(maxlen, e.seq.length()-start) - minlen + 1));
            String pat = e.seq.substring(start, start+L);
            if(!pat.matches("[ACGT]+")) continue;
            samples.add(new Sample(e.header, start, L, pat));
            i++;
        }
    }

    static void writePatterns(List<Sample> samples) throws Exception {
        BufferedWriter w = Files.newBufferedWriter(Paths.get(OUT_PATTERNS));
        w.write("pattern_id,source_file,start_index,length,pattern_string\n");
        int pid=1;
        for(Sample s: samples) {
            w.write(String.format("%d,%s,%d,%d,%s\n", pid++, s.header.replaceAll(",", "_"), s.start, s.len, s.pat));
        }
        w.close();
    }

    static List<Ground> computeGround(List<Sample> samples, List<SeqEntry> entries) {
        List<Ground> rows = new ArrayList<>();
        int pid=1;
        for(Sample s: samples) {
            boolean found = false;
            for(SeqEntry e: entries) {
                String seq = e.seq;
                int idx = seq.indexOf(s.pat);
                int start = 0;
                while(idx >= 0) {
                    rows.add(new Ground(pid, e.header.replaceAll(",", "_"), idx));
                    found = true;
                    start = idx + 1;
                    if(start >= seq.length()) break;
                    idx = seq.indexOf(s.pat, start);
                }
            }
            if(!found) rows.add(new Ground(pid, "NONE", -1));
            pid++;
        }
        return rows;
    }

    static void writeGround(List<Ground> rows) throws Exception {
        BufferedWriter w = Files.newBufferedWriter(Paths.get(OUT_GROUND));
        w.write("pattern_id,source_file,match_index\n");
        for(Ground g: rows) w.write(String.format("%d,%s,%d\n", g.pid, g.header, g.idx));
        w.close();
    }
}
