package io.paradoxical.cassieq.dataAccess;


public final class Tables {
    public static class Pointer {
        public static final String TABLE_NAME = "pointer";
        public static final String QUEUE_ID = "queueid";
        public static final String POINTER_TYPE = "pointer_type";
        public static final String VALUE = "value";
    }

    public static class Monoton {
        public static final String TABLE_NAME = "monoton";
        public static final String QUEUE_ID = "queueid";
        public static final String VALUE = "value";
    }

    public static class Account {
        public static final String TABLE_NAME = "account";

        public static final String ACCOUNT_NAME = "account_name";

        public static final String KEYS = "keys";
    }

    public static class Queue {
        public static final String TABLE_NAME = "queue";

        public static final String QUEUE_NAME = "queuename";
        public static final String ACCOUNT_NAME = "account_name";

        public static final String MAX_DELIVERY_COUNT = "max_delivery_count";
        public static final String BUCKET_SIZE = "bucket_size";
        public static final String STATUS = "status";
        public static final String VERSION = "version";
        public static final String REPAIR_WORKER_POLL_FREQ_SECONDS = "repair_worker_poll_freq_seconds";
        public static final String REPAIR_WORKER_TOMBSTONE_BUCKET_TIMEOUT_SECONDS = "repair_worker_tombstone_bucket_timeout_seconds";
        public static final String DELETE_BUCKETS_AFTER_FINALIZATION = "delete_buckets_after_finalization";
    }

    public static class QueueSize {
        public static final String TABLE_NAME = "queue_size";
        public static final String QUEUE_ID = "queueid";
        public static final String SIZE = "size";
    }

    public static class Message {
        public static final String TABLE_NAME = "message";
        public static final String QUEUE_ID = "queueid";
        public static final String BUCKET_NUM = "bucket_num";
        public static final String MONOTON = "monoton";
        public static final String MESSAGE = "message";
        public static final String VERSION = "version";
        public static final String DELIVERY_COUNT = "delivery_count";
        public static final String ACKED = "acked";
        public static final String NEXT_VISIBLE_ON = "next_visible_on";
        public static final String TAG = "tag";
        public static final String CREATED_DATE = "created_date";
        public static final String UPDATED_DATE = "updated_date";
    }

    public static class DeletionJob {
        public static final String TABLE_NAME = "deletion_job";
        public static final String QUEUE_NAME = "queuename";
        public static final String ACCOUNT_NAME = "account_name";
        public static final String VERSION = "version";
        public static final String BUCKET_SIZE = "bucket_size";
    }
}
