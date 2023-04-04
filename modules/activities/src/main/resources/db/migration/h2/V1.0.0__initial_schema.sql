-- TAG
create table "tag"(
  "id" bigserial not null primary key,
  "name" varchar(254) not null -- unique
);
alter table "tag" add constraint "tag_name_uniq" unique("name");
create index if not exists "tag_name_idx" on "tag"("name");

-- LOCATION
create table "activity_location"(
  "id" bigserial not null primary key,
  "location" varchar(2000) not null -- unique
);
alter table "activity_location" add constraint "activity_location_location_uniq" unique("location");
create index if not exists "activity_location_location_idx" on "activity_location"("location");

-- ACTIVITY
create table "activity"(
  "id" bigserial not null primary key,
  "location_id" bigint not null, --fk
  "path" varchar(2000) not null,
  "file_id" varchar(254) not null, --unique
  "device" varchar(254) not null,
  "serial_number" bigint,
  "created_at" timestamp,
  "name" varchar(2000) not null,
  "timestamp" timestamp not null,
  "total_time" bigint not null,
  "notes" text
);

alter table "activity" add constraint "activity_location_fkey"
foreign key ("location_id") references "activity_location"("id") on delete cascade;

alter table "activity" add constraint "activity_file_id_uniq" unique("file_id");

-- ACTIVITY-SESSION
create table "activity_session"(
  "id" bigserial not null primary key,
  "activity_id" bigint not null, --fk
  "sport" int not null,
  "sub_sport" int not null,
  "start_time" timestamp not null,
  "end_time" timestamp not null,
  "moving_time" bigint not null,
  "elapsed_time" bigint not null,
  "distance" decimal not null,
  "start_pos_lat" bigint,
  "start_pos_long" bigint,
  "calories" decimal not null,
  "total_ascend" decimal,
  "total_descend" decimal,
  "min_temp" decimal,
  "max_temp" decimal,
  "avg_temp" decimal,
  "min_hr" int,
  "max_hr" int,
  "avg_hr" int,
  "max_speed" decimal,
  "avg_speed" decimal,
  "max_power" decimal,
  "avg_power" decimal
);

alter table "activity_session" add constraint "activity_session_activity_fkey"
foreign key ("activity_id") references "activity"("id") on delete cascade;

create index if not exists "activity_start_time_idx" on "activity_session"("start_time");
create index if not exists "activity_sport_idx" on "activity_session"("sport");
create index if not exists "activity_subsport_idx" on "activity_session"("sub_sport");

-- ACTIVITY-SESSION-DATA
create table "activity_session_data"(
  "id" bigserial not null primary key,
  "activity_session_id" bigint not null,
  "timestamp" timestamp not null,
  "position_lat" bigint,
  "position_long" bigint,
  "altitude" decimal,
  "heartrate" int,
  "cadence" int,
  "distance" decimal,
  "speed" decimal,
  "power" decimal,
  "grade" decimal,
  "temperature" decimal,
  "calories" decimal
);
alter table "activity_session_data" add constraint "activity_session_data_activity_id_fk"
foreign key ("activity_session_id") references "activity_session"("id") on delete cascade;

--alter table "activity_session_data" add constraint "activity_session_data_time_uniq"
--unique("activity_session_id", "timestamp");

 create index if not exists "activity_session_data_timestamp_idx"
 on "activity_session_data"("timestamp");

-- TAG-ACTIVITY
create table "activity_tag"(
  "id" bigserial not null primary key,
  "activity_id" bigint not null,
  "tag_id" bigint not null
);
alter table "activity_tag" add constraint "activity_tag_activity_id_fk"
foreign key ("activity_id") references "activity"("id") on delete cascade;

alter table "activity_tag" add constraint "activity_tag_tag_id_fk"
foreign key ("tag_id") references "tag"("id") on delete cascade;

alter table "activity_tag" add constraint "activity_tag_uniq" unique("activity_id", "tag_id");