package io.paradoxical.cassieq.model;

public interface BucketPointer extends Pointer {
    default BucketPointer next(){
        return GenericBucketPointer.valueOf(get() + 1);
    }

    default MonotonicIndex startOf(BucketSize bucketsize){
        return MonotonicIndex.valueOf(get() * bucketsize.get());
    }
}
