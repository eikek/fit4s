-- TAG
create table "tag"(
  "id" bigserial not null primary key,
  "name" varchar(254) not null -- unique
);
alter table "tag" add constraint "tag_name_uniq" unique("name");
create index if not exists "tag_name_idx" on "tag"("name");

insert into "tag" ("id", "name") values (-999, 'System/Deleted');

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
  "notes" text,
  "import_time" timestamp not null
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
  "distance" double precision not null,
  "start_pos_lat" bigint,
  "start_pos_long" bigint,
  "calories" double precision not null,
  "total_ascend" double precision,
  "total_descend" double precision,
  "min_temp" double precision,
  "max_temp" double precision,
  "avg_temp" double precision,
  "min_hr" int,
  "max_hr" int,
  "avg_hr" int,
  "max_speed" double precision,
  "avg_speed" double precision,
  "max_power" int,
  "avg_power" int,
  "norm_power" int,
  "max_cadence" int,
  "avg_cadence" int,
  "tss" double precision,
  "num_pool_len" int,
  "iff" double precision,
  "swim_stroke" int,
  "avg_stroke_distance" double precision,
  "avg_stroke_count" double precision,
  "pool_length" double precision,
  "avg_grade" double precision
);

alter table "activity_session" add constraint "activity_session_activity_fkey"
foreign key ("activity_id") references "activity"("id") on delete cascade;

create index if not exists "activity_start_time_idx" on "activity_session"("start_time");
create index if not exists "activity_sport_idx" on "activity_session"("sport");
create index if not exists "activity_subsport_idx" on "activity_session"("sub_sport");

-- ACTIVITY-LAP
create table "activity_lap"(
  "id" bigserial not null primary key,
  "activity_session_id" bigint not null, --fk
  "sport" int not null,
  "sub_sport" int not null,
  "trigger" int,
  "start_time" timestamp not null,
  "end_time" timestamp not null,
  "start_pos_lat" bigint,
  "start_pos_long" bigint,
  "end_pos_lat" bigint,
  "end_pos_long" bigint,
  "moving_time" bigint not null,
  "elapsed_time" bigint not null,
  "calories" double precision not null,
  "distance" double precision not null,
  "min_temp" double precision,
  "max_temp" double precision,
  "avg_temp" double precision,
  "max_speed" double precision,
  "avg_speed" double precision,
  "min_hr" int,
  "max_hr" int,
  "avg_hr" int,
  "max_power" int,
  "avg_power" int,
  "norm_power" int,
  "max_cadence" int,
  "avg_cadence" int,
  "total_ascend" double precision,
  "total_descend" double precision,
  "num_pool_len" int,
  "swim_stroke" int,
  "avg_stroke_distance" double precision,
  "stroke_count" int,
  "avg_grade" double precision
);

alter table "activity_lap" add constraint "activity_lap_activity_session_fkey"
foreign key ("activity_session_id") references "activity_session"("id") on delete cascade;

create index if not exists "activity_lap_start_time_idx" on "activity_lap"("start_time");
create index if not exists "activity_lap_end_time_idx" on "activity_lap"("end_time");


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

-- ACTIVITY_STRAVA

create table "activity_strava"(
  "id" bigserial not null primary key,
  "activity_id" bigint not null,
  "strava_id" bigint not null
);

alter table "activity_strava" add constraint "activity_strava_activity_fkey"
foreign key ("activity_id") references "activity"("id") on delete cascade;

create index if not exists "activity_strava_strava_id_idx" on "activity_strava"("strava_id");

alter table "activity_strava"
 add constraint "activity_strava_strava_id_uniq" unique("strava_id");
alter table "activity_strava"
 add constraint "activity_strava_strava_id_activity_id_uniq" unique("activity_id", "strava_id");


-- GEOPLACE
create table "geo_place"(
  "id" bigserial not null primary key,
  "osm_place_id" bigint not null,
  "osm_id" bigint not null,
  "position_lat" bigint not null,
  "position_lng" bigint not null,
  "road" varchar(255),
  "location" varchar(255),
  "country" varchar(255),
  "country_code" varchar(50) not null,
  "post_code" varchar(50),
  "bbox_lat1" bigint not null,
  "bbox_lat2" bigint not null,
  "bbox_lng1" bigint not null,
  "bbox_lng2" bigint not null
);
alter table "geo_place"
add constraint "geo_place_osm_place_id_uniq"
unique ("osm_place_id");

alter table "geo_place"
add constraint "geo_place_position_uniq"
unique ("position_lat", "position_lng");

-- activity place
create table "activity_geo_place" (
  "id" bigserial not null primary key,
  "geo_place_id" bigint not null,
  "activity_session_id" bigint not null,
  "position_name" varchar(255) not null
);

alter table "activity_geo_place"
add constraint "activity_geo_place_geo_place_id_fk"
foreign key ("geo_place_id") references "geo_place"("id") on delete cascade;

alter table "activity_geo_place"
add constraint "activity_geo_place_activity_session_id_fk"
foreign key ("activity_session_id") references "activity_session"("id") on delete cascade;

alter table "activity_geo_place"
add constraint "activity_geo_place_place_activity_session_uniq"
unique ("geo_place_id", "activity_session_id", "position_name");

---CREATE FUNCTION HAVSC
CREATE OR REPLACE FUNCTION public.sctorad(
  sc double precision
)
  RETURNS double precision
  LANGUAGE plpgsql
AS
$$
declare
  result double precision;
begin
  select radians( sc * (180.0 / power(2, 31)) )
  into result;
  return result;
end;
$$;

CREATE OR REPLACE FUNCTION public.havsc(
  alat double precision,
  alng double precision,
  blat double precision,
  blng double precision
)
  RETURNS double precision
  LANGUAGE plpgsql
AS
$$
declare
  atemp double precision;
  dist double precision;
begin
  SELECT power(sin((sctorad(blat) - sctorad(alat)) / 2), 2) +
    cos(sctorad(alat)) * cos(sctorad(blat)) *
    power(sin((sctorad(blng) - sctorad(alng)) / 2), 2)
  INTO atemp;

  select 6371 * 2 * atan2(sqrt(atemp), sqrt(1 - atemp))
  into dist;

  return dist;
end;
$$;


-- strava token
create table "strava_token"(
  "id" bigserial not null primary key,
  "token_type" varchar(255) not null,
  "access_token" text not null,
  "refresh_token" text not null,
  "expires_at" timestamp not null,
  "expires_in" bigint not null,
  "scope" text not null,
  "created_at" timestamp not null
);

create index if not exists "strava_token_expires_at_idx" on "strava_token"("expires_at");
create index if not exists "strava_token_created_at_idx" on "strava_token"("created_at");
