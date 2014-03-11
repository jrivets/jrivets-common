package org.jrivets.journal;

import java.io.InputStream;
import java.io.OutputStream;

final class FileSystemJournal implements Journal {
    
    private final AbstractChunkingPolicy policy;
    
    private final JournalInputStream in;
    
    private final JournalOutputStream out;
    
    FileSystemJournal(AbstractChunkingPolicy policy) {
        this.policy = policy;
        this.in = new JournalInputStream(policy);
        this.out = new JournalOutputStream(policy);
    }

    @Override
    public InputStream getInputStream() {
        return in;
    }

    @Override
    public OutputStream getOutputStream() {
        return out;
    }

    @Override
    public void close() {
        policy.close();
    }

    @Override
    public String toString() {
        return policy.toString();
    }
}
