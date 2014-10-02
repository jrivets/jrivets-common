package org.jrivets.cluster;

final class Version {
    
    long version;
    
    VersionState state;
   
    Version(Long version, VersionState state) {
        this.version = version;
        this.state = state;
    }
}
