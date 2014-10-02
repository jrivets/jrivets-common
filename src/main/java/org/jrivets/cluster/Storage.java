package org.jrivets.cluster;

public interface Storage {

    VersionsList getVersions(String key);
    
}
