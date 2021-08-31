CREATE TABLE man
(
    id         BIGSERIAL PRIMARY KEY,
    partner_id BIGINT,
    name       TEXT NOT NULL,
    FOREIGN KEY (partner_id) REFERENCES man (id) ON DELETE SET NULL
);

ALTER TABLE man
    OWNER TO txdb;
