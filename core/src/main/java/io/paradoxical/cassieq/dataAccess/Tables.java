package io.paradoxical.cassieq.dataAccess;

public final class Tables {
    public static class Pointer {
        public static final String TABLE_NAME = "pointer";
        public static final String QUEUENAME = "queuename";
        public static final String POINTER_TYPE = "pointer_type";
        public static final String VALUE = "value";
    }

    public static class Monoton {
        public static final String TABLE_NAME = "monoton";
        public static final String QUEUENAME = "queuename";
        public static final String VALUE = "value";
    }

    public static class Queue {
        public static final String TABLE_NAME = "queue";
        public static final String QUEUENAME = "queuename";
        public static final String MAX_DEQUEUE_COUNT = "max_dequeue_count";
        public static final String BUCKET_SIZE = "bucket_size";
    }

    public static class Message {

        public static final String TABLE_NAME = "message";
        public static final String QUEUENAME = "queuename";
        public static final String BUCKET_NUM = "bucket_num";
        public static final String MONOTON = "monoton";
        public static final String MESSAGE = "message";
        public static final String VERSION = "version";
        public static final String DELIVERY_COUNT = "delivery_count";
        public static final String ACKED = "acked";
        public static final String NEXT_VISIBLE_ON = "next_visible_on";
        public static final String CREATED_DATE = "created_date";
        public static final String TAG = "tag";
    }
}
