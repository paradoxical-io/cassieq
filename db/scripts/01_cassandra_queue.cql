CREATE TABLE queue (
  queuename text PRIMARY KEY,
  bucket_size int,
  max_delivery_count int,
  status int,
  version int
);

CREATE TABLE pointer (
  queuename text,
  pointer_type text,
  value bigint,

  PRIMARY KEY (queueid, pointer_type)
);

CREATE TABLE monoton (
  queuename text PRIMARY KEY,
  value bigint
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
