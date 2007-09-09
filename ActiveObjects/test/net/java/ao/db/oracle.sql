CREATE TABLE person (
    id NUMBER NOT NULL,
    firstName VARCHAR NOT NULL,
    lastName CLOB,
    age NUMBER(12),
    url VARCHAR UNIQUE,
    favoriteClass VARCHAR,
    height NUMBER(32,6) DEFAULT 62.3,
    companyID NUMBER,
    cool NUMBER DEFAULT 1,
    created TIMESTAMP DEFAULT SYSDATE,
    CONSTRAINT fk_person_companyid FOREIGN KEY (companyID) REFERENCES company(id),
    PRIMARY KEY(id)
)