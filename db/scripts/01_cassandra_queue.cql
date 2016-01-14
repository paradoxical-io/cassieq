CREATE TABLE queue (
  account_name text,
  queuename text,
  queue_size_counter_id text,
  bucket_size int,
  max_delivery_count int,
  status int,
  repair_worker_poll_freq_seconds int,
  repair_worker_tombstone_bucket_timeout_seconds int,
  delete_buckets_after_finalization boolean,
  version int,

  PRIMARY KEY (account_name, queuename)
);

CREATE TABLE queue_size (
    queue_size_counter_id text PRIMARY KEY,
    size counter
);

CREATE TABLE pointer (
  queueid text,
  pointer_type text,
  value bigint,

  PRIMARY KEY (queueid, pointer_type)
);

CREATE TABLE monoton (
  queueid text PRIMARY KEY,
  value bigint
);

CREATE TABLE deletion_job (
    account_name text,
    queuename text,
    version int,
    bucket_size int,
    PRIMARY KEY (account_name, queuename, version)
);

CREATE TABLE message (
  queueid text,
  bucket_num bigint,
  monoton bigint,
  message text,
  version int,
  acked boolean,
  next_visible_on timestamp,
  delivery_count int,
  tag text,
  created_date timestamp,
  updated_date timestamp,

  PRIMARY KEY ((queueid, bucket_num), monoton)
);

CREATE TABLE account (
    account_name text PRIMARY KEY,
    keys set<text>
);
