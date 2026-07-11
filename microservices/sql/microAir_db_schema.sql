CREATE DATABASE IF NOT EXISTS microAir_db;
USE microAir_db;

CREATE TABLE IF NOT EXISTS flight (
    flight_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    flight_number VARCHAR(20),
    origin VARCHAR(50),
    destination VARCHAR(50),
    departure_time DATETIME,
    arrival_time DATETIME,
    flight_status VARCHAR(20),
    economy_fare DECIMAL(10, 2),
    business_fare DECIMAL(10, 2),
    first_fare DECIMAL(10, 2)
);

CREATE TABLE IF NOT EXISTS booking (
    booking_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pnr VARCHAR(10),
    flight_id BIGINT,
    passenger_name VARCHAR(100),
    seat_number VARCHAR(10),
    fare_amount DECIMAL(10, 2),
    booking_status VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS check_in (
    check_in_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT,
    check_in_time DATETIME,
    baggage_count INT,
    baggage_weight DECIMAL(6, 2),
    check_in_status VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS crew_assignment (
    assignment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    crew_member_name VARCHAR(100),
    flight_id BIGINT,
    role VARCHAR(20),
    duty_hours DECIMAL(4, 1),
    assignment_status VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS frequent_flyer (
    member_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_name VARCHAR(100),
    miles_balance INT,
    membership_tier VARCHAR(20),
    enrollment_date DATE,
    member_status VARCHAR(20)
);

