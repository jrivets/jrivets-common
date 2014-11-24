package org.jrivets.journal;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.jrivets.util.Strings;

/**
 * Journal builder provides factory methods to construct different
 * {@code Journal} implementations.
 * 
 * @author Dmitry Spasibenko
 * 
 */
public final class JournalBuilder {

    long maxCapacity = Long.MAX_VALUE;

    long maxChunkSize = Long.MAX_VALUE;

    String folderName;

    String prefixName;

    private boolean cleanAfterOpen;

    private boolean singleWrite;

    /**
     * Allows to set maximum journal (size) capacity. Default is
     * {@code Long.MAX_VALUE} which is maximum supported journal size.
     * 
     * @param maxCapacity
     * @return the builder object
     */
    public JournalBuilder withMaxCapacity(long maxCapacity) {
        if (maxCapacity <= 0) {
            throw new IllegalArgumentException("maxCapacity=" + maxCapacity + " should be positive value.");
        }
        this.maxCapacity = maxCapacity;
        return this;
    }

    /**
     * Defines maximum file (chunk) size for file system data journals. Default
     * is {@code Long.MAX_VALUE} which is maximum supported file-chunk size.
     * 
     * @param maxChunkSize
     * @return the builder object
     */
    public JournalBuilder withMaxChunkSize(long maxChunkSize) {
        if (maxChunkSize <= 0) {
            throw new IllegalArgumentException("maxChunkSize=" + maxChunkSize + " should be positive value.");
        }
        this.maxChunkSize = maxChunkSize;
        return this;
    }

    /**
     * Defines folder name where chunks are going to be stored for file system
     * data journals.
     * 
     * @param folderName
     * @return the builder object
     */
    public JournalBuilder withFolderName(String folderName) {
        if (!Strings.isNullOrEmpty(folderName)) {
            this.folderName = folderName;
        }
        return this;
    }

    /**
     * Defines prefix for each file-chunk name for file system data journals.
     * All files for the journal will have name started from the prefix.
     * 
     * @param prefixName
     * @return the builder object
     */
    public JournalBuilder withPrefixName(String prefixName) {
        if (Strings.isNullOrEmpty(prefixName)) {
            throw new IllegalArgumentException("prefixName should not be empty");
        }
        this.prefixName = prefixName;
        return this;
    }

    /**
     * Cleans up the journal immediately after creation. All data stored in
     * files will be cleaned up.
     * 
     * @return the builder object
     */
    public JournalBuilder cleanAfterOpen() {
        this.cleanAfterOpen = true;
        return this;
    }

    /**
     * The <code>singleWrite</code> flag controls how write operation will be
     * performed. If the flag value is <code>true</code> then the written buffer
     * value cannot be split between different chunks and all buffer will be
     * written into one chunk, even if the chunk's capacity will be exceeded.
     * 
     * @param singleWrite
     * @return
     */
    public JournalBuilder withSingleWrite(boolean singleWrite) {
        this.singleWrite = singleWrite;
        return this;
    }

    /**
     * Constructs new {@link Journal} instance with the builder configuration
     * settings.
     * <p>
     * The journal will persist all its data (including metadata) in one
     * file-system folder. The method will throw {@link FileNotFoundException}
     * if the provided folder does not exist. All journal file names will start
     * with the file name prefix. Metadata is stored in the file with the prefix
     * name even. Data file names consist of 2 parts: the file prefix and the
     * chunk number which lies in the [0..Integer.MAX_VALUE] range, so number of
     * chunks cannot exceed {@link Integer.MAX_VALUE}.
     * <p>
     * The journal can pick-up data written by another journal instance before,
     * so every time when new the journal instance is created it will check the
     * folder, read the journal metadata (if it exists) and check the metadata
     * and chunks consistency. If the consistency check fails, the method will
     * throw {@link IllegalStateException} exception.
     * <p>
     * The new instance journal configuration can be different with existing
     * data: the actual journal size can be bigger than configured - if amount
     * of data in file-chunks exceed the configured journal capacity. This case
     * you cannot write to the journal until its actual size shrinks less than
     * configured capacity.
     * <p>
     * File chunks can be bigger/smaller than configured if they were created by
     * previous instance of the journal with different capacity/chunk-size
     * settings. The journal instance will use previously created file-chunks as
     * is, but new ones will be created according to actual chunk size settings
     * for the instance.
     * <p>
     * New journal instance will clean all previously persisted data when it is
     * constructed if the builder is configured with {@link cleanAfterOpen()}
     * call.
     * <p>
     * Only one journal instance can access same journal data at a time, so an
     * attempt to create second instance of the journal with same journal
     * setting will fail (the {@link IllegalStateException} exception will be
     * thrown) until the first journal instance is closed.
     * <p>
     * The journal instance is thread-safe for read-write access, but it is not
     * for read-read and write-write operations. It means that input stream and
     * output stream can be accessed from different threads without additional
     * synchronizations, but if the journal is supposed to be used in multi-read
     * or multi-write (many threads perform access to same (input or output)
     * stream simultaneously) mode, the invocation code should guard access to
     * the same stream and support one thread for access one stream at a time.
     * In other words input and output streams are not thread-safe.
     * 
     * @return
     * @throws IOException
     */
    public Journal buildExpandable() throws IOException {
        if (maxChunkSize > maxCapacity) {
            throw new IllegalArgumentException("maxChunksSize=" + maxChunkSize
                    + " should not be greater than maxCapacity=" + maxCapacity);
        }
        return new FileSystemJournal(new ChunkingPolicy(maxCapacity, maxChunkSize, folderName, prefixName,
                cleanAfterOpen, singleWrite));
    }

}
