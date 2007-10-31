CREATE TABLE person (
    id INTEGER IDENTITY(1,1) NOT NULL,
    firstName VARCHAR(255) NOT NULL,
    lastName NTEXT,
    age INTEGER(12),
    url VARCHAR(255) UNIQUE NOT NULL,
    favoriteClass VARCHAR(255),
    height DOUBLE(32,6) DEFAULT 62.3,
    companyID BIGINT,
    cool INTEGER DEFAULT 1,
    created DATETIME DEFAULT GetDate(),
    CONSTRAINT fk_person_companyid FOREIGN KEY (companyID) REFERENCES company(id),
    PRIMARY KEY(id)
)