package org.jrivets.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jrivets.log.Logger;
import org.jrivets.log.LoggerFactory;

public final class FileUtils {

    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    static boolean underTest;

    /**
     * Create unique temp directory
     * 
     * @return null if fails
     */
    public static File createTempDirQuietly() {
        try {
            return createTempDir();
        } catch (Exception ex) {
            // Oops, fail, just consume it...
            logger.error("Cannot create unique temp directory ", ex.getMessage());
            logger.debug(ex);
        }
        return null;
    }

    /**
     * Create unique temp directory
     * 
     * @return File objects which refers to the temp Dir, or throws an exception
     *         if fails
     */
    public static File createTempDir() throws IOException {
        long stopTimeMs = System.currentTimeMillis() + waitDelay();
        do {
            File tempDirFile = new File(System.getProperty("java.io.tmpdir"), getUniquePart());
            if (tempDirFile.mkdir()) {
                return tempDirFile;
            }
            Thread.yield();
        } while (System.currentTimeMillis() < stopTimeMs);
        throw new RuntimeException("Cannot create a temp directory for some reason.");
    }

    public static List<String> getListOfDirectories(String dir) {
        File file = new File(dir);
        return Arrays.asList(file.list((File current, String name) -> {
            return new File(current, name).isDirectory();
        }));
    }

    public static List<String> getListOfFiles(String dir, final String prefix) {
        File file = new File(dir);
        return Arrays.asList(file.list((File current, String name) -> {
            return (name == null || name.length() == 0 || name.startsWith(prefix))
                    && !new File(current, name).isDirectory();
        }));
    }

    public static void deleteDirectory(File directory) throws IOException {
        cleanDirectory(directory);
        Files.delete(Paths.get(directory.getAbsolutePath()));
    }

    public static void cleanDirectory(File directory) throws IOException {
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException(directory + " is not a directory");
        }

        File[] files = directory.listFiles();
        if (files == null) {
            throw new IOException("Failed to list contents of " + directory);
        }

        for (File file : files) {
            delete(file);
        }
    }

    public static void delete(File file) throws IOException {
        if (file.isDirectory()) {
            deleteDirectory(file);
            return;
        }
        Files.delete(Paths.get(file.getPath()));
    }

    private static String getUniquePart() {
        return underTest ? "test" : Long.toString(System.nanoTime());
    }

    private static long waitDelay() {
        return underTest ? 10L : TimeUnit.SECONDS.toMillis(10);
    }
}
