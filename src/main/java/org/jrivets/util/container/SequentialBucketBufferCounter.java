package org.jrivets.util.container;

import org.jrivets.collection.RingBuffer;

/**
 * Buffer based objects counter implementation.
 * <p>
 * This class is good to implement window-based objects counting. An amount of
 * objects is always counted for a bucket (integer number) which cannot be less
 * than last mentioned one. The size of buffer is limited by number of the
 * buckets, so when objects are counted for a bucket with number N, all buckets
 * with number less than <code>N - size + 1</code> will be removed.
 * 
 * @author Dmitry Spasibenko
 *
 */
public class SequentialBucketBufferCounter {

    private final RingBuffer<VHolder> buffer;

    private long count;

    private static class VHolder {

        final long bucket;

        long count;

        VHolder(long bucket) {
            this.bucket = bucket;
        }
    }

    /**
     * Constructor
     * 
     * @param backetsCount
     *            - set maximum number of buckets allowed in the buffer
     */
    public SequentialBucketBufferCounter(int backetsCount) {
        buffer = new RingBuffer<VHolder>(backetsCount);
    }

    /**
     * Add an amount to the specified bucket. The method can cause significant
     * counter decrease so as due to its execution some older buckets can be
     * removed (swept).
     * 
     * @param bucket
     *            - bucket number, cannot be less than any mentioned earlier
     *            here
     * @param count
     *            - amount should be added to the bucket
     */
    public void add(long bucket, long count) {
        if (buffer.size() > 0 && buffer.last().bucket > bucket) {
            throw new IllegalArgumentException("bucket=" + bucket + " should be equal or bigger than existing one "
                    + buffer.last().bucket);
        }
        sweep(bucket);
        if (buffer.size() == 0 || buffer.last().bucket < bucket) {
            buffer.add(new VHolder(bucket));
        }
        buffer.last().count += count;
        this.count += count;
    }

    /**
     * @return cumulative counter for the buffer
     */
    public long getTotalCount() {
        return count;
    }

    private void sweep(long lastBacket) {
        long headAcceptable = lastBacket - buffer.capacity() + 1;
        while (buffer.size() > 0 && buffer.element().bucket < headAcceptable) {
            count -= buffer.remove().count;
        }
    }
}
