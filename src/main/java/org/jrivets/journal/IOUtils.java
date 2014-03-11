package org.jrivets.journal;

import java.io.Closeable;
import java.io.File;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Collection;

final class IOUtils {

    static final String temporaryDirectory = System.getProperty("java.io.tmpdir");
    
    static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (Throwable t) {
            // just consumes it
        }
    }
    
    static void releaseQuietly(FileLock lock) {
        try {
            lock.release();
        } catch (Throwable t) {
        }
    }
    
    static Collection<File> getFiles(String folderName, String prefix) {
        File folder = new File(folderName);
        File[] files = folder.listFiles();
        ArrayList<File> result = new ArrayList<File>(files.length);
        for (File file: files) {
            if (file.isFile() && file.getName().startsWith(prefix)) {
                result.add(file);
            }
        }
        return result;
    }
}
