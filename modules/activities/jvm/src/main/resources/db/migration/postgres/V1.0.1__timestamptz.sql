alter table "activity" alter column "timestamp" type timestamptz;
alter table "activity" alter column "created_at" type timestamptz;
alter table "activity" alter column "import_time" type timestamptz;

alter table "activity_session" alter column "start_time" type timestamptz;
alter table "activity_session" alter column "end_time" type timestamptz;

alter table "activity_lap" alter column "start_time" type timestamptz;
alter table "activity_lap" alter column "end_time" type timestamptz;

alter table "activity_session_data" alter column "timestamp" type timestamptz;

alter table "strava_token" alter column "expires_at" type timestamptz;
alter table "strava_token" alter column "created_at" type timestamptz;
