-- -----------------------------------------------------------------------------
-- V1: initial schema
-- Column names follow the JPA mapping of the entities in the domain module
-- (field names, no explicit @Column(name)). Ids are UUIDs stored as text.
-- -----------------------------------------------------------------------------

CREATE TABLE admin (
    id         VARCHAR(36)  NOT NULL,
    username   VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    createdAt  DATETIME(6)  NOT NULL,
    updatedAt  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_admin_username UNIQUE (username),
    CONSTRAINT uk_admin_email UNIQUE (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE customer (
    id                       VARCHAR(36)    NOT NULL,
    firstName                VARCHAR(255)   NOT NULL,
    lastName                 VARCHAR(255)   NOT NULL,
    email                    VARCHAR(255)   NOT NULL,
    password                 VARCHAR(255)   NOT NULL,
    phoneNumber              VARCHAR(255)   NOT NULL,
    drivingLicenseNumber     VARCHAR(255)   NOT NULL,
    drivingLicenseExpiryDate DATE           NOT NULL,
    dateOfBirth              DATE           NOT NULL,
    verifiedIdentity         TINYINT(1)     NOT NULL DEFAULT 0,
    billingAddress           VARCHAR(255),
    balance                  DECIMAL(12, 2) NOT NULL DEFAULT 0,
    creationDate             DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_customer_email UNIQUE (email),
    CONSTRAINT uk_customer_phone UNIQUE (phoneNumber),
    CONSTRAINT uk_customer_licence UNIQUE (drivingLicenseNumber),
    CONSTRAINT ck_customer_balance CHECK (balance >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE car_type (
    id           VARCHAR(36)   NOT NULL,
    category     VARCHAR(255)  NOT NULL,
    brand        VARCHAR(255)  NOT NULL,
    model        VARCHAR(255)  NOT NULL,
    engine       VARCHAR(255),
    power        INT           NOT NULL DEFAULT 0,
    maxSpeed     INT           NOT NULL DEFAULT 0,
    acceleration DOUBLE        NOT NULL DEFAULT 0,
    weight       DOUBLE        NOT NULL DEFAULT 0,
    driveType    VARCHAR(255),
    transmission VARCHAR(255),
    seats        INT           NOT NULL DEFAULT 0,
    description  VARCHAR(1000),
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE car_type_features (
    car_type_id VARCHAR(36)  NOT NULL,
    feature     VARCHAR(255),
    CONSTRAINT fk_feature_car_type FOREIGN KEY (car_type_id) REFERENCES car_type (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE car (
    id                  VARCHAR(36)    NOT NULL,
    licensePlate        VARCHAR(255)   NOT NULL,
    car_type_id         VARCHAR(36)    NOT NULL,
    dailyRentalPrice    DECIMAL(10, 2) NOT NULL,
    depositAmount       DECIMAL(10, 2) NOT NULL,
    status              VARCHAR(255)   NOT NULL,
    imageUrl            VARCHAR(1000),
    registrationDate    DATE,
    lastMaintenanceDate DATE,
    vin                 VARCHAR(255),
    color               VARCHAR(255),
    insuranceExpiryDate DATE,
    version             BIGINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_car_plate UNIQUE (licensePlate),
    CONSTRAINT fk_car_car_type FOREIGN KEY (car_type_id) REFERENCES car_type (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE booking (
    bookingId       VARCHAR(36)    NOT NULL,
    car_id          VARCHAR(36)    NOT NULL,
    customer_id     VARCHAR(36)    NOT NULL,
    startDate       DATE           NOT NULL,
    endDate         DATE           NOT NULL,
    totalCost       DECIMAL(12, 2) NOT NULL,
    depositAmount   DECIMAL(12, 2) NOT NULL,
    bookingStatus   VARCHAR(255)   NOT NULL,
    paymentStatus   VARCHAR(255)   NOT NULL,
    createdAt       DATETIME(6)    NOT NULL,
    paidAt          DATETIME(6),
    closedAt        DATETIME(6),
    rejectionReason VARCHAR(500),
    version         BIGINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (bookingId),
    CONSTRAINT fk_booking_car FOREIGN KEY (car_id) REFERENCES car (id),
    CONSTRAINT fk_booking_customer FOREIGN KEY (customer_id) REFERENCES customer (id),
    CONSTRAINT ck_booking_dates CHECK (endDate > startDate)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- The availability search and the double-booking check look up bookings of one car by date
CREATE INDEX ix_booking_car_dates ON booking (car_id, startDate, endDate);
CREATE INDEX ix_booking_status ON booking (bookingStatus);

CREATE TABLE password_reset_code (
    id        VARCHAR(36)  NOT NULL,
    role      VARCHAR(255) NOT NULL,
    email     VARCHAR(255) NOT NULL,
    codeHash  VARCHAR(255) NOT NULL,
    expiresAt DATETIME(6)  NOT NULL,
    attempts  INT          NOT NULL DEFAULT 0,
    used      TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
