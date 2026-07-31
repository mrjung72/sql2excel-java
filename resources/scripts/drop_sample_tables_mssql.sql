-- ========================================
-- 샘플 테이블/뷰 삭제 스크립트 (MSSQL)
-- 의존성 순서에 맞춰 안전하게 드롭
-- ========================================

SET NOCOUNT ON;

-- 1) 뷰 드롭
IF OBJECT_ID('dbo.vw_OrderSummary', 'V') IS NOT NULL DROP VIEW dbo.vw_OrderSummary;
GO

-- 2) 트리거 드롭 (존재 시)
IF OBJECT_ID('dbo.trg_customers_modtime', 'TR') IS NOT NULL DROP TRIGGER dbo.trg_customers_modtime;
IF OBJECT_ID('dbo.trg_orders_modtime', 'TR') IS NOT NULL DROP TRIGGER dbo.trg_orders_modtime;
IF OBJECT_ID('dbo.trg_products_modtime', 'TR') IS NOT NULL DROP TRIGGER dbo.trg_products_modtime;
IF OBJECT_ID('dbo.trg_employees_modtime', 'TR') IS NOT NULL DROP TRIGGER dbo.trg_employees_modtime;
GO

-- 3) 외래키 제약 해제 (존재 시)
IF OBJECT_ID('dbo.FK_OrderDetails_Orders', 'F') IS NOT NULL ALTER TABLE dbo.OrderDetails DROP CONSTRAINT FK_OrderDetails_Orders;
IF OBJECT_ID('dbo.FK_OrderDetails_Products', 'F') IS NOT NULL ALTER TABLE dbo.OrderDetails DROP CONSTRAINT FK_OrderDetails_Products;
IF OBJECT_ID('dbo.FK_Employees_ReportsTo', 'F') IS NOT NULL ALTER TABLE dbo.Employees DROP CONSTRAINT FK_Employees_ReportsTo;
IF OBJECT_ID('dbo.FK_Orders_Customers', 'F') IS NOT NULL ALTER TABLE dbo.Orders DROP CONSTRAINT FK_Orders_Customers;
GO

-- 4) 테이블 드롭 (자식 → 부모 순서)
IF OBJECT_ID('dbo.OrderDetails', 'U') IS NOT NULL DROP TABLE dbo.OrderDetails;
IF OBJECT_ID('dbo.Orders', 'U') IS NOT NULL DROP TABLE dbo.Orders;
IF OBJECT_ID('dbo.Products', 'U') IS NOT NULL DROP TABLE dbo.Products;
IF OBJECT_ID('dbo.Employees', 'U') IS NOT NULL DROP TABLE dbo.Employees;
IF OBJECT_ID('dbo.Customers', 'U') IS NOT NULL DROP TABLE dbo.Customers;
GO

PRINT 'MSSQL 샘플 객체 삭제 완료';
