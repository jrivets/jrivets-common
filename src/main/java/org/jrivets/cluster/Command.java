package org.jrivets.cluster;

public abstract class Command {
    
    private String key;

    public String getKey() {
        return key;
    }
    
    public abstract Value getValue();
    
    public abstract void sendTo(CmdController cmdController);
    
}
