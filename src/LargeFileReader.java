import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LargeFileReader {
    private static final Pattern DATE_PATTERN = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2})");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int CHUNK_SIZE = 1024 * 1024 * 1024; // 1GB chunks
    
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Please provide date in yyyy-MM-dd format");
            return;
        }
        
        LocalDate targetDate = LocalDate.parse(args[0], DATE_FORMATTER);
        String outputPath = "output/output_" + args[0] + ".txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            processLargeFile("large_log_file.log", targetDate, writer);
        }
    }
    
    private static void processLargeFile(String filename, LocalDate targetDate, BufferedWriter writer) throws Exception {
        try (RandomAccessFile file = new RandomAccessFile(filename, "r"); FileChannel channel = file.getChannel()) {
            long fileSize = channel.size();
            int threadCount = (int) Math.ceil((double) fileSize / CHUNK_SIZE);
            ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(threadCount, Runtime.getRuntime().availableProcessors())
            );
            
            CompletionService<Void> completionService = new ExecutorCompletionService<>(executor);
            
            for (long position = 0; position < fileSize; position += CHUNK_SIZE) {
                long chunkSize = Math.min(CHUNK_SIZE, fileSize - position);
                completionService.submit(new ChunkProcessor(channel, position, chunkSize, targetDate, writer));
            }
            
            for (int i = 0; i < threadCount; i++) {
                completionService.take().get();
            }
            
            executor.shutdown();
        }
    }
    
    private static class ChunkProcessor implements Callable<Void> {
        private final FileChannel channel;
        private final long position;
        private final long size;
        private final LocalDate targetDate;
        private final BufferedWriter writer;
        
        public ChunkProcessor(FileChannel channel, long position, long size, LocalDate targetDate, BufferedWriter writer) {
            this.channel = channel;
            this.position = position;
            this.size = size;
            this.targetDate = targetDate;
            this.writer = writer;
        }
        
        @Override
        public Void call() throws Exception {
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, position, size);
            StringBuilder line = new StringBuilder();
            
            while (buffer.hasRemaining()) {
                char c = (char) buffer.get();
                if (c == '\n') {
                    processLine(line.toString(), targetDate, writer);
                    line.setLength(0);
                } else {
                    line.append(c);
                }
            }
            
            if (line.length() > 0) {
                processLine(line.toString(), targetDate, writer);
            }
            
            return null;
        }
    }

    private static void processLine(String line, LocalDate targetDate, BufferedWriter writer) {
        Matcher matcher = DATE_PATTERN.matcher(line);
        if (matcher.find()) {
            LocalDate logDate = LocalDate.parse(matcher.group(1), DATE_FORMATTER);
            if (logDate.equals(targetDate)) {
                synchronized (writer) {
                    try {
                        writer.write(line);
                        writer.newLine();
                        writer.flush();
                    } catch (IOException e) {
                        System.err.println("Error writing to output file: " + e.getMessage());
                    }
                }
            }
        }
    }
}