package io.paradoxical.cassieq.model;

public interface MessagePointer extends Pointer {
    BucketPointer toBucketPointer(BucketSize bucketSize);
}
