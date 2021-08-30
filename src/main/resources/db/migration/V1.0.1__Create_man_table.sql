create table man
(
    id         serial not null
        constraint man_pk primary key,
    partner_id integer
        constraint man_man_id_fk references man on delete set null,
    name       text   not null
);

create unique index man_id_uidx on man (id);

create unique index man_partner_id_uidx on man (partner_id);
