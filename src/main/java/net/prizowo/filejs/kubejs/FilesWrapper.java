package net.prizowo.filejs.kubejs;

import net.prizowo.filejs.FilesJSPlugin;
import net.prizowo.filejs.Filesjs;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FilesWrapper {
    
    public String readFile(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
    
    public List<String> readLines(String path) throws IOException {
        return Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
    }
    
    public void writeFile(String path, String content) {
        try {
            Files.write(Paths.get(path), content.getBytes(StandardCharsets.UTF_8));
            FilesJSPlugin.FILE_CHANGED.post(new FileEventJS(path, content));
        } catch (IOException e) {
            Filesjs.LOGGER.error("Error writing file: " + path, e);
            throw new RuntimeException("Failed to write file: " + path, e);
        }
    }
    
    public void writeLines(String path, List<String> lines) throws IOException {
        Files.write(Paths.get(path), lines, StandardCharsets.UTF_8);
    }
    
    public void appendFile(String path, String content) throws IOException {
        Files.write(Paths.get(path), content.getBytes(StandardCharsets.UTF_8), 
            StandardOpenOption.APPEND, StandardOpenOption.CREATE);
    }
    
    public boolean exists(String path) {
        return Files.exists(Paths.get(path));
    }
    
    public void createDirectory(String path) throws IOException {
        Files.createDirectories(Paths.get(path));
    }
    
    public void delete(String path) throws IOException {
        Files.delete(Paths.get(path));
    }
    
    public List<String> listFiles(String path) throws IOException {
        return Files.list(Paths.get(path))
                .map(Path::toString)
                .collect(Collectors.toList());
    }
    
    public void copy(String source, String target) throws IOException {
        Files.copy(Paths.get(source), Paths.get(target), StandardCopyOption.REPLACE_EXISTING);
    }
    
    public void move(String source, String target) throws IOException {
        Files.move(Paths.get(source), Paths.get(target), StandardCopyOption.REPLACE_EXISTING);
    }
    
    private void validatePath(String path) {
    }
    

    public void appendLine(String path, String line) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(line);
        Files.write(Paths.get(path), lines, StandardCharsets.UTF_8, 
            StandardOpenOption.APPEND, StandardOpenOption.CREATE);
    }
    

    public void ensureDirectoryExists(String path) throws IOException {
        Path dirPath = Paths.get(path).getParent();
        if (dirPath != null && !Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
    }
} 