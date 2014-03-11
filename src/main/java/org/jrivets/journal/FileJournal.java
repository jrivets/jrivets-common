package org.jrivets.journal;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * 
 *  @author Dmitry Spasibenko 
 *
 */
public interface FileJournal {

    InputStream getInputStream();
    
    OutputStream getOutputStream();
    
}
