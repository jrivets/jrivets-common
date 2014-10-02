package org.jrivets.cluster;

final class ValueController implements CmdController {
    
    Storage storage;
  
    final TransactionsCollector<WriteTransactionDesc> wTransactions; 
    
    static class WriteTransactionDesc extends TransactionDesc {
        
        VersionsList vList;
        
        Version version;

        WriteTransactionDesc(String txId) {
            super(txId);
        }

        @Override
        void timeout() {
            
            
        }

        @Override
        void cancel() {
            // TODO Auto-generated method stub
            
        }
        
    }
    
    ValueController() {
        wTransactions = new TransactionsCollector<WriteTransactionDesc>(10000L);
    }
    
    @Override
    public void onWrite(WriteCommand wrtCmd) {
        VersionsList vList = storage.getVersions(wrtCmd.getKey());
        Version version = vList.newVersion(wrtCmd.getValue());
        String txId = wrtCmd.getKey() + version.version;
        
    }

}
