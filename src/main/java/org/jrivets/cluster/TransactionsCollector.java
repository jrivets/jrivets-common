package org.jrivets.cluster;

import org.jrivets.util.container.AbstractKeyValueHolder;

abstract class TransactionDesc {
    
    final String txId;
    
    TransactionDesc(String txId) {
        this.txId = txId;
    }
    
    abstract void timeout();
    
    abstract void cancel();
    
    @Override
    public String toString() {
        return "{txId=" + txId + "}";
    }
}

final class TransactionsCollector<T extends TransactionDesc> extends AbstractKeyValueHolder<String, T> {

    private T desc;
    
    TransactionsCollector(long txTimeoutMs) {
        super(txTimeoutMs, Integer.MAX_VALUE);
    }

    @Override
    protected T getNewValue(String key) {
        return desc;
    }

    synchronized void addTxDesc(T desc) {
        TransactionDesc d = getValue(desc.txId);
        if (d != null) {
            throw new IllegalStateException("Transaction Id=" + desc.txId + " already present in the collector " + d);
        }
        
        this.desc = desc;
        try {
            // place it to the collector
            getValue(desc.txId);
        } finally {
            this.desc = null;
        }
    }
    
    @Override
    protected void onRemove(Holder holder) {
        holder.getValue().timeout();
    }
    
    @Override
    protected void onClear() {
        for (Holder holder: holders.values()) {
            holder.getValue().cancel();
        }
    }
}
