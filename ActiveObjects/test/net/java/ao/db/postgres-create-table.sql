CREATE TABLE person (
    id SERIAL NOT NULL,
    firstName VARCHAR(255) NOT NULL,
    lastName TEXT,
    age INTEGER,
    url VARCHAR(255) UNIQUE NOT NULL,
    favoriteClass VARCHAR(255),
    height DOUBLE PRECISION DEFAULT 62.3,
    companyID BIGINT,
    cool BOOLEAN DEFAULT TRUE,
    created TIMESTAMP DEFAULT now(),
    CONSTRAINT fk_person_companyid FOREIGN KEY (companyID) REFERENCES company(id),
    PRIMARY KEY(id)
)