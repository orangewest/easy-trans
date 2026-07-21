CREATE TABLE IF NOT EXISTS teacher (
    id          BIGINT PRIMARY KEY,
    name        VARCHAR(50),
    sex         INT,
    creator     BIGINT,
    create_date TIMESTAMP
);
