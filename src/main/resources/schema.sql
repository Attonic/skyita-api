CREATE DATABASE IF NOT EXISTS skydb
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE skydb;

CREATE TABLE IF NOT EXISTS sky_snapshots
(
    sky_snapshot_id BINARY(16)   NOT NULL,
    snapshot_date   DATE         NOT NULL UNIQUE,
    generated_at    DATETIME(6)  NOT NULL,
    city            VARCHAR(255) NOT NULL,
    latitude        DOUBLE       NOT NULL,
    longitude       DOUBLE       NOT NULL,
    satellites_json TEXT,
    planets_json    TEXT,
    planet_count    INT,
    satellite_count INT,
    PRIMARY KEY (sky_snapshot_id)
);