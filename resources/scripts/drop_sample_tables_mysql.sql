-- ========================================
-- 샘플 테이블 삭제 스크립트
-- MySQL / MariaDB용
-- ========================================

USE sampledb;

-- 외래 키 제약 조건 때문에 역순으로 삭제
DROP TABLE IF EXISTS OrderDetails;
DROP TABLE IF EXISTS Orders;
DROP TABLE IF EXISTS Products;
DROP TABLE IF EXISTS Employees;
DROP TABLE IF EXISTS Customers;

SELECT 'Sample tables dropped successfully for MySQL/MariaDB!' AS Message;

