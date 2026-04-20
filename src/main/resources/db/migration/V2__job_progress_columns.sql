alter table jobs add column download_speed varchar(64);
alter table jobs add column eta varchar(64);
alter table jobs add column progress_percent double precision default 0 not null;
