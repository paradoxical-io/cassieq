package io.paradoxical.cassieq.model;

/**
 * The order of this matters as we will not let
 * people go down the stack. You can only move forward
 *
 * active -> inactive -> deleting
 */
public enum QueueStatus {
    Active,
    Deleting,
    Inactive
}
