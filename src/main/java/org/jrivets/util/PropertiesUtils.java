package org.jrivets.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.jrivets.log.Logger;
import org.jrivets.log.LoggerFactory;

public final class PropertiesUtils extends StaticSingleton {
    
    private static final Logger logger = LoggerFactory.getLogger(PropertiesUtils.class);

    public static Properties loadFromResource(String resourceFileName) throws IOException {
        return loadFrom(resourceFileName, true);
    }
    
    public static Properties loadFromResourceQuietly(String resourceFileName) {
        try {
            return loadFromResource(resourceFileName);
        } catch(IOException ioe) {
            logger.error("Cannot load properties from file " + resourceFileName + " " + ioe.getMessage());
        }
        return null;
    }
    
    public static Properties loadFromFile(String fileName) throws IOException {
        return loadFrom(fileName, false);
    }
    
    public static Properties loadFromFileQuietly(String fileName) {
        try {
            return loadFromFile(fileName);
        } catch(IOException ioe) {
            logger.error("Cannot load properties from file " + fileName + " " + ioe.getMessage());
        }
        return null;
    }
    
    public static Properties addOrOverrideProps(Properties to, Properties from) {
        if (from != null) {
            for (Object key: from.keySet()) {
                to.put(key, from.get(key));
            }
        }
        return to;
    }
    
    private static Properties loadFrom(String fileName, boolean resource) throws IOException {
        Properties properties = new Properties();
        try (InputStream is = getStream(fileName, resource)) {
            if (is != null) { 
                properties.load(is);
            } else {
                logger.warn("The configuration file ", fileName, " can not be found");
                throw new FileNotFoundException("Cannot open file " + fileName + " to read properties");
            }
        } 
        return properties;
    }
    
    private static InputStream getStream(String fileName, boolean resource) throws FileNotFoundException {
        if (resource) {
            return Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
        }
        return new FileInputStream(fileName);
    }
}
