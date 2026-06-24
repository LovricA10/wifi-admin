CREATE TABLE wifi_configuration (
    cpe_id          VARCHAR(64)  NOT NULL,
    wifi_band       VARCHAR(32)  NOT NULL,
    ssid            VARCHAR(32)  NOT NULL,
    encryption_type VARCHAR(32)  NOT NULL,
    password        VARCHAR(128),
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL,
    last_synced_at  TIMESTAMP,
    CONSTRAINT pk_wifi_configuration PRIMARY KEY (cpe_id)
);
