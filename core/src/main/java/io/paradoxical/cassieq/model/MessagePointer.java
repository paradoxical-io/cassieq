package io.paradoxical.cassieq.model;

public interface MessagePointer extends Pointer {
    default BucketPointer toBucketPointer(BucketSize bucketSize){
        return GenericBucketPointer.valueOf(get() / bucketSize.get());
    }
}
