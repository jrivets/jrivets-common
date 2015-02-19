package org.jrivets.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    
    @SuppressWarnings("unchecked")
    public static <V> Map<String, V> toMap(Properties props) {
        HashMap<String, V> result = new HashMap<>();
        for (String name: props.stringPropertyNames()) {
            result.put(name, (V) props.getProperty(name));
        }
        return result;
    }
    
    public static List<String> scanPathForFile(String fileName) {
        String sysPath = System.getenv("PATH");
        if (sysPath == null) {
            return Collections.emptyList();
        }
        
        String[] filePaths = sysPath.split(File.pathSeparator);
        if (filePaths == null) {
            return Collections.emptyList();
        }

        ArrayList<String> result = new ArrayList<String>(5);
        for (String path: filePaths) {
            String fullName = path + File.separator + fileName;
            if (new File(fullName).exists()) {
                result.add(fullName);
            }
        }
        return result;
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
