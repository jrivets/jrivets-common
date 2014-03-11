package org.jrivets.journal;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * The Journal interface provides byte streams access to a data store.
 * {@code InputStream} provides mark/reset read functionality for the storage.
 * 
 * @author Dmitry Spasibenko
 * 
 */
public interface Journal {

    /**
     * Returns input stream for the journal. The result for the method call is
     * unpredictable for closed journal. The {@code InputStream} object returned
     * by the method should support mark/reset functionality.
     * 
     * @return journal {@code InputStream} object
     */
    InputStream getInputStream();

    /**
     * Returns output stream for the journal. The result for the method call is
     * unpredictable for closed journal.
     * 
     * @return journal {@code OutputStream} object
     */
    OutputStream getOutputStream();

    /**
     * Closes input and output data streams for the journal. Read/write attempts
     * to closed journal will cause unpredictable behavior.
     */
    void close();

}
