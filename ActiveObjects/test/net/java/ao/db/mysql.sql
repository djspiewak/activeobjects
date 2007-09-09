CREATE TABLE person (
    id INTEGER AUTO_INCREMENT NOT NULL,
    firstName VARCHAR NOT NULL,
    lastName TEXT,
    age INTEGER(12),
    url VARCHAR UNIQUE,
    favoriteClass VARCHAR,
    height DOUBLE(32,6) DEFAULT 62.3,
    companyID INTEGER,
    cool BOOLEAN DEFAULT 1,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_person_companyid FOREIGN KEY (companyID) REFERENCES company(id),
    PRIMARY KEY(id)
) ENGINE=InnoDB