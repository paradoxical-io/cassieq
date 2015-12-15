package io.paradoxical.cassieq.model;

/**
 * The order of this matters as we will not let
 * people go down the stack. You can only move forward
 *
 * active -> inactive -> deleting
 */
public enum QueueStatus {
    /**
     * New queues are always created in this state
     */
    Provisioning,

    Active,

    /**
     * This is the beginning of being marked for a deletion. You cannot create a new queue
     * while in this sate
     */
    PendingDelete,

    /**
     * ONce the deletion job tables are populated we can move to this state. You can now
     * create a queue with the same name if is in this state
     */
    Deleting,

    /**
     * Once all deletion jobs are complete (There may be many concurrently),
     * we set to inactive. This is the only time you can _actually_ remove the queue entry
     * from the table
     */
    Inactive
}
