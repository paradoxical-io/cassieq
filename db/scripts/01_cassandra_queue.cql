CREATE TABLE queue (
  queueid text PRIMARY KEY,
  queuename text,
  bucket_size int,
  max_delivery_count int,
  status text,
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

CREATE TABLE queue_name_idx(
    queuename text PRIMARY KEY,
    version int,
    status text
);

CREATE TABLE message (
  queueid text,
  bucket_num bigint,
  monoton bigint,
  message text,
  version int,
  acked boolean,
  next_visible_on timestamp,
  created_date timestamp,
  delivery_count int,
  tag text,

  PRIMARY KEY ((queueid, bucket_num), monoton)
);
