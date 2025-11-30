# Experiment Setup and Execution Guide

This README explains how to set up, compile, and run the experiment for comparing Static Hybrid vs Dynamic Hybrid string-matching algorithms on DNA patterns.

---

## 1. Project Structure

Place all Java source files and data files in the **same project folder**:

* `KMPSearch.java`
* `BMSearch.java`
* `HybridSearch.java`
* `ExperimentRunner.java`
* `sequence.fasta`
* `patterns.csv`
* `ground_truth.csv`

Your folder should look like this:

```
project-folder/
 ├── KMPSearch.java
 ├── BMSearch.java
 ├── HybridSearch.java
 ├── ExperimentRunner.java
 ├── sequence.fasta
 ├── patterns.csv
 ├── ground_truth.csv
```

---

## 2. Prerequisites

Make sure **Java Development Kit (JDK) 11 or later** is installed.

Check using:

```
java -version
javac -version
```

You should see version information. If not, install a JDK.

---

## 3. Compiling the Java Files

Open your terminal or IDE terminal and navigate to the project folder:

```
cd path/to/project-folder
```

Compile all Java files:

```
javac *.java
```

If there are no errors, compilation succeeded.

---

## 4. Running the Experiment

Run the main experiment driver:

```
java ExperimentRunner
```

The program will:

* Load DNA sequence(s) from `sequence.fasta`
* Load all patterns from `patterns.csv`
* Load match positions from `ground_truth.csv`
* Perform warm-up runs
* Execute measured runs using both Static and Dynamic Hybrid algorithms
* Measure execution time, comparisons, switch counts, and correctness

You’ll see console logs such as:

```
Processing pattern 1/40 (id=1 len=12)
Processing pattern 2/40 (id=2 len=8)
...
Done. Results written to results.csv and summary.csv
```

---

## 5. Output Files

After running, two CSV files will appear in the project folder:

### **1. results.csv** (raw data)

Contains every individual run:

```
pattern_id,algorithm,run,time_ns,comparisons,switch_count,correct,length,repetitive
```

### **2. summary.csv** (final averages)

One line per pattern:

```
pattern_id,length,repetitive,static_mean_time_ms,dynamic_mean_time_ms,
static_mean_comps,dynamic_mean_comps,mean_switches,static_correct_all,dynamic_correct_all
```

You will use **summary.csv** for Findings, Analysis, Graphs, and Discussion.

---

## 6. Troubleshooting

### "Could not find or load main class"

Make sure you're in the correct folder and compiled with:

```
javac *.java
```

### "File not found: sequence.fasta"

Ensure FASTA and CSV files are in the same folder as the Java files.

### IDE cannot run the file

Use terminal instead:

```
javac *.java
java ExperimentRunner
```



Your experiment is now fully set up and reproducible.
