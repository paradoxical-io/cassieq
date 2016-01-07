CREATE TABLE queue (
  queuename text PRIMARY KEY,
  bucket_size int,
  max_delivery_count int,
  status int,
  repair_worker_poll_freq_seconds int,
  repair_worker_tombstone_bucket_timeout_seconds int,
  delete_buckets_after_finalization boolean,
  version int
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
    queuename text,
    version int,
    bucket_size int,
    PRIMARY KEY (queuename, version)
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

CREATE TABLE leadership_election (
    group text PRIMARY KEY,
    leader_id text
);
