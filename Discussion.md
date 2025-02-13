# Discussion

## Solutions Considered

1. **Simple Single-threaded Approach**
   - Read file line by line using BufferedReader
   - Pros: Simple implementation, low memory overhead
   - Cons: Very slow for 1TB files, doesn't utilize modern hardware

2. **Memory-mapped Files with Multiple Threads**
   - Split file into chunks, process in parallel
   - Use MappedByteBuffer for efficient file access
   - Pros: Excellent performance, leverages OS memory management
   - Cons: More complex implementation

3. **Apache Spark Solution**
   - Use distributed computing framework
   - Pros: Built for big data processing
   - Cons: Overkill for single machine, requires Spark setup

## Final Solution Summary

Chose the memory-mapped files with multi-threading approach because:

1. **Performance**
   - Memory-mapped files provide near-native I/O performance
   - Multi-threading utilizes all available CPU cores
   - 1GB chunk size balances memory usage and thread overhead

2. **Resource Efficiency**
   - Uses memory mapping instead of loading entire file
   - Thread pool size adapts to available processors
   - Synchronized writing prevents memory leaks

3. **Maintainability**
   - Pure Java solution with no external dependencies
   - Clear separation of concerns (reading, processing, writing)
   - Standard library components for long-term maintainability

## Steps to Run

1. **Compile the Code**
   ```bash
   javac src/LargeFileReader.java
   ```

2. **Prepare Environment**
   - Ensure output directory exists
   - Verify sufficient disk space for output file
   - Input log file should be in working directory

3. **Run the Program**
   ```bash
   java -Xmx4g LargeFileReader 2024-03-20
   ```
   - Replace 2024-03-20 with target date
   - Adjust -Xmx based on available RAM

4. **Check Results**
   - Output will be in: output/output_YYYY-MM-DD.txt
   - Program will print completion message