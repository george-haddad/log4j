create database logschema;
use logschema;

grant select,insert,delete on logschema.log4jTable to 'george'@'%' identified by 'george123';

create table if not exists logschema.log4jTable
(
	id bigint(20) not null auto_increment,
	logger varchar(255) collate utf8_unicode_ci not null,
	level varchar(15) collate utf8_unicode_ci not null,
	message text collate utf8_unicode_ci,
	stacktrace text collate utf8_unicode_ci,
	creationTime timestamp not null default now(),
	ip varchar(15) collate utf8_unicode_ci not null,
	primary key(id),
	key priority(level),
	key creationTime(creationTime)
)
engine=MyISAM default charset=utf8 collate=utf8_unicode_ci auto_increment=1;
