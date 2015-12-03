package io.paradoxical.cassieq.dataAccess.interfaces;

import io.paradoxical.cassieq.model.InvisibilityMessagePointer;
import io.paradoxical.cassieq.model.ReaderBucketPointer;
import io.paradoxical.cassieq.model.RepairBucketPointer;

public interface PointerRepository {
    /**
     * Conditional update the bucket if the message pointer still points to pointer
     *
     * otherwise return the value in the pointer. IF for wahtever reason they are still the same (weird)
     * try again
     *
     * @param ptr
     * @return
     */
    ReaderBucketPointer advanceMessageBucketPointer(ReaderBucketPointer original, ReaderBucketPointer ne);

    /**
     * Conditional update of either the minimum of the current in the db or the destination
     *
     * @param destination
     */
    InvisibilityMessagePointer tryMoveInvisiblityPointerTo(InvisibilityMessagePointer original, InvisibilityMessagePointer destination);

    RepairBucketPointer advanceRepairBucketPointer(RepairBucketPointer original, RepairBucketPointer next);

    InvisibilityMessagePointer getCurrentInvisPointer();

    ReaderBucketPointer getReaderCurrentBucket();

    RepairBucketPointer getRepairCurrentBucketPointer();
}
