CREATE TABLE person (
    id NUMBER NOT NULL,
    firstName VARCHAR(255) NOT NULL,
    lastName CLOB,
    age NUMBER(12),
    url VARCHAR(255) UNIQUE NOT NULL,
    favoriteClass VARCHAR(255),
    height NUMBER(32,6) DEFAULT 62.3,
    companyID NUMBER,
    cool NUMBER DEFAULT 1,
    created TIMESTAMP DEFAULT SYSDATE,
    CONSTRAINT fk_person_companyid FOREIGN KEY (companyID) REFERENCES company(id),
    PRIMARY KEY(id)
)