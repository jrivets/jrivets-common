package org.jrivets.cluster;

public enum VersionState {
    /**
     * NEW - just written version, not confirmed from others
     */
    NEW,
    
    /**
     * QUORUM - confirmed version, and there is a quorum of other nodes 
     * which confirmed the version by the node requests
     */
    QUORUM,
    
    /**
     * The version is received from other node, confirmed by this one, 
     * but there is no quorum for the node, so quorum status is unknown
     */
    FOREIGN
}
