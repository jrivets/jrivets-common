package org.jrivets.journal;

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
    JournalInputStream getInputStream();

    /**
     * Returns output stream for the journal. The result for the method call is
     * unpredictable for closed journal.
     * 
     * @return journal {@code OutputStream} object
     */
    OutputStream getOutputStream();

    /**
     * Returns number of bytes available for read. This amount also includes
     * marked chunk, which already could be read, but which could be reset back
     * to be available for the read operation again. If no markers, the result
     * is same as what <code>getInputStream().abailable()</code> returns.
     * 
     * @return total number of bytes that can be accessible via
     *         <code>getInputStream()</code> stream.
     */
    long available();

    /**
     * Closes input and output data streams for the journal. Read/write attempts
     * to closed journal will cause unpredictable behavior.
     */
    void close();

}
