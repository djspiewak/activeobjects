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

CREATE SEQUENCE person_id_seq INCREMENT BY 1 START WITH 1 NOMAXVALUE MINVALUE 1

CREATE TRIGGER person_id_autoinc
BEFORE INSERT
    ON person   FOR EACH ROW
BEGIN
    SELECT person_id_seq.NEXTVAL INTO :NEW.id FROM DUAL; 
END;
