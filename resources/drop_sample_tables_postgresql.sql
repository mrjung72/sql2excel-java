-- ========================================
-- 샘플 테이블 삭제 스크립트
-- PostgreSQL용
-- ========================================

-- CASCADE 옵션으로 외래 키 제약 조건 무시하고 삭제
DROP TABLE IF EXISTS OrderDetails CASCADE;
DROP TABLE IF EXISTS Orders CASCADE;
DROP TABLE IF EXISTS Products CASCADE;
DROP TABLE IF EXISTS Employees CASCADE;
DROP TABLE IF EXISTS Customers CASCADE;

-- 트리거 함수 삭제
DROP FUNCTION IF EXISTS update_modified_column() CASCADE;

SELECT 'Sample tables dropped successfully for PostgreSQL!' AS Message;

