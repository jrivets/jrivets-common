package org.jrivets.cluster;

public final class WriteCommand extends Command{

    @Override
    public void sendTo(CmdController cmdController) {
        cmdController.onWrite(this);
    }

    @Override
    public Value getValue() {
        // TODO Auto-generated method stub
        return null;
    }
}
