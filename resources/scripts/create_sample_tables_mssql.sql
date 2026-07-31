-- ========================================
-- 샘플 테이블 생성 스크립트
-- Microsoft SQL Server 용 (SQL Server 2012+ 권장)
-- ========================================

SET NOCOUNT ON;

-- 기존 객체 삭제 (존재 시)
IF OBJECT_ID('dbo.vw_OrderSummary', 'V') IS NOT NULL DROP VIEW dbo.vw_OrderSummary;
IF OBJECT_ID('dbo.OrderDetails', 'U') IS NOT NULL DROP TABLE dbo.OrderDetails;
IF OBJECT_ID('dbo.Orders', 'U') IS NOT NULL DROP TABLE dbo.Orders;
IF OBJECT_ID('dbo.Products', 'U') IS NOT NULL DROP TABLE dbo.Products;
IF OBJECT_ID('dbo.Employees', 'U') IS NOT NULL DROP TABLE dbo.Employees;
IF OBJECT_ID('dbo.Customers', 'U') IS NOT NULL DROP TABLE dbo.Customers;
GO

-- ========================================
-- 1. Customers 테이블
-- ========================================
CREATE TABLE dbo.Customers (
  CustomerID INT IDENTITY(1,1) NOT NULL,
  CustomerCode VARCHAR(20) NOT NULL,
  CustomerName VARCHAR(100) NOT NULL,
  ContactName VARCHAR(50) NULL,
  Email VARCHAR(100) NULL,
  Phone VARCHAR(20) NULL,
  Address VARCHAR(200) NULL,
  City VARCHAR(50) NULL,
  Region VARCHAR(50) NULL,
  PostalCode VARCHAR(10) NULL,
  Country VARCHAR(50) NULL,
  CustomerType VARCHAR(20) CONSTRAINT DF_Customers_CustomerType DEFAULT ('Regular'),
  CreditLimit DECIMAL(15,2) CONSTRAINT DF_Customers_CreditLimit DEFAULT (0),
  IsActive BIT CONSTRAINT DF_Customers_IsActive DEFAULT (1),
  CreatedDate DATETIME2 CONSTRAINT DF_Customers_CreatedDate DEFAULT (SYSDATETIME()),
  LastUpdated DATETIME2 CONSTRAINT DF_Customers_LastUpdated DEFAULT (SYSDATETIME()),
  CONSTRAINT PK_Customers PRIMARY KEY (CustomerID),
  CONSTRAINT UK_Customers_Code UNIQUE (CustomerCode)
);
GO

CREATE INDEX IX_Customers_Name ON dbo.Customers (CustomerName);
CREATE INDEX IX_Customers_City ON dbo.Customers (City);
CREATE INDEX IX_Customers_Region ON dbo.Customers (Region);
CREATE INDEX IX_Customers_Type ON dbo.Customers (CustomerType);
GO

-- LastUpdated 자동 업데이트 트리거
IF OBJECT_ID('dbo.trg_customers_modtime', 'TR') IS NOT NULL DROP TRIGGER dbo.trg_customers_modtime;
GO
CREATE TRIGGER dbo.trg_customers_modtime
ON dbo.Customers
AFTER UPDATE
AS
BEGIN
  SET NOCOUNT ON;
  UPDATE c
    SET LastUpdated = SYSDATETIME()
  FROM dbo.Customers c
  INNER JOIN inserted i ON c.CustomerID = i.CustomerID;
END;
GO

-- ========================================
-- 2. Orders 테이블
-- ========================================
CREATE TABLE dbo.Orders (
  OrderID INT IDENTITY(1,1) NOT NULL,
  OrderNumber VARCHAR(30) NOT NULL,
  CustomerID INT NOT NULL,
  OrderDate DATETIME2 NOT NULL,
  RequiredDate DATETIME2 NULL,
  ShippedDate DATETIME2 NULL,
  OrderStatus VARCHAR(20) CONSTRAINT DF_Orders_OrderStatus DEFAULT ('Pending'),
  SubTotal DECIMAL(15,2) CONSTRAINT DF_Orders_SubTotal DEFAULT (0),
  TaxAmount DECIMAL(15,2) CONSTRAINT DF_Orders_TaxAmount DEFAULT (0),
  TotalAmount DECIMAL(15,2) CONSTRAINT DF_Orders_TotalAmount DEFAULT (0),
  PaymentMethod VARCHAR(30) NULL,
  PaymentStatus VARCHAR(20) CONSTRAINT DF_Orders_PaymentStatus DEFAULT ('Unpaid'),
  EmployeeID INT NULL,
  Notes VARCHAR(500) NULL,
  CreatedDate DATETIME2 CONSTRAINT DF_Orders_CreatedDate DEFAULT (SYSDATETIME()),
  LastUpdated DATETIME2 CONSTRAINT DF_Orders_LastUpdated DEFAULT (SYSDATETIME()),
  CONSTRAINT PK_Orders PRIMARY KEY (OrderID),
  CONSTRAINT UK_Orders_Number UNIQUE (OrderNumber),
  CONSTRAINT FK_Orders_Customers FOREIGN KEY (CustomerID) REFERENCES dbo.Customers(CustomerID)
);
GO

CREATE INDEX IX_Orders_CustomerID ON dbo.Orders (CustomerID);
CREATE INDEX IX_Orders_OrderDate ON dbo.Orders (OrderDate);
CREATE INDEX IX_Orders_Status ON dbo.Orders (OrderStatus);
CREATE INDEX IX_Orders_PaymentStatus ON dbo.Orders (PaymentStatus);
CREATE INDEX IX_Orders_ShippedDate ON dbo.Orders (ShippedDate);
GO

IF OBJECT_ID('dbo.trg_orders_modtime', 'TR') IS NOT NULL DROP TRIGGER dbo.trg_orders_modtime;
GO
CREATE TRIGGER dbo.trg_orders_modtime
ON dbo.Orders
AFTER UPDATE
AS
BEGIN
  SET NOCOUNT ON;
  UPDATE o
    SET LastUpdated = SYSDATETIME()
  FROM dbo.Orders o
  INNER JOIN inserted i ON o.OrderID = i.OrderID;
END;
GO

-- ========================================
-- 3. Products 테이블
-- ========================================
CREATE TABLE dbo.Products (
  ProductID INT IDENTITY(1,1) NOT NULL,
  ProductCode VARCHAR(20) NOT NULL,
  ProductName VARCHAR(100) NOT NULL,
  Category VARCHAR(50) NULL,
  UnitPrice DECIMAL(15,2) CONSTRAINT DF_Products_UnitPrice DEFAULT (0),
  UnitsInStock INT CONSTRAINT DF_Products_UnitsInStock DEFAULT (0),
  UnitsOnOrder INT CONSTRAINT DF_Products_UnitsOnOrder DEFAULT (0),
  ReorderLevel INT CONSTRAINT DF_Products_ReorderLevel DEFAULT (0),
  Discontinued BIT CONSTRAINT DF_Products_Discontinued DEFAULT (0),
  Description NVARCHAR(MAX) NULL,
  CreatedDate DATETIME2 CONSTRAINT DF_Products_CreatedDate DEFAULT (SYSDATETIME()),
  LastUpdated DATETIME2 CONSTRAINT DF_Products_LastUpdated DEFAULT (SYSDATETIME()),
  CONSTRAINT PK_Products PRIMARY KEY (ProductID),
  CONSTRAINT UK_Products_Code UNIQUE (ProductCode)
);
GO

CREATE INDEX IX_Products_Name ON dbo.Products (ProductName);
CREATE INDEX IX_Products_Category ON dbo.Products (Category);
CREATE INDEX IX_Products_Price ON dbo.Products (UnitPrice);
GO

IF OBJECT_ID('dbo.trg_products_modtime', 'TR') IS NOT NULL DROP TRIGGER dbo.trg_products_modtime;
GO
CREATE TRIGGER dbo.trg_products_modtime
ON dbo.Products
AFTER UPDATE
AS
BEGIN
  SET NOCOUNT ON;
  UPDATE p
    SET LastUpdated = SYSDATETIME()
  FROM dbo.Products p
  INNER JOIN inserted i ON p.ProductID = i.ProductID;
END;
GO

-- ========================================
-- 4. OrderDetails 테이블
-- ========================================
CREATE TABLE dbo.OrderDetails (
  OrderDetailID INT IDENTITY(1,1) NOT NULL,
  OrderID INT NOT NULL,
  ProductID INT NOT NULL,
  UnitPrice DECIMAL(15,2) NOT NULL,
  Quantity INT NOT NULL,
  Discount DECIMAL(5,2) CONSTRAINT DF_OrderDetails_Discount DEFAULT (0),
  LineTotal AS (UnitPrice * Quantity * (1 - (Discount/100.0))) PERSISTED,
  CreatedDate DATETIME2 CONSTRAINT DF_OrderDetails_CreatedDate DEFAULT (SYSDATETIME()),
  CONSTRAINT PK_OrderDetails PRIMARY KEY (OrderDetailID),
  CONSTRAINT FK_OrderDetails_Orders FOREIGN KEY (OrderID) REFERENCES dbo.Orders(OrderID) ON DELETE CASCADE,
  CONSTRAINT FK_OrderDetails_Products FOREIGN KEY (ProductID) REFERENCES dbo.Products(ProductID)
);
GO

CREATE INDEX IX_OrderDetails_OrderID ON dbo.OrderDetails (OrderID);
CREATE INDEX IX_OrderDetails_ProductID ON dbo.OrderDetails (ProductID);
GO

-- ========================================
-- 5. Employees 테이블
-- ========================================
CREATE TABLE dbo.Employees (
  EmployeeID INT IDENTITY(1,1) NOT NULL,
  EmployeeCode VARCHAR(20) NOT NULL,
  FirstName VARCHAR(50) NOT NULL,
  LastName VARCHAR(50) NOT NULL,
  Title VARCHAR(50) NULL,
  BirthDate DATE NULL,
  HireDate DATE NULL,
  Email VARCHAR(100) NULL,
  Phone VARCHAR(20) NULL,
  Department VARCHAR(50) NULL,
  Salary DECIMAL(15,2) NULL,
  ReportsTo INT NULL,
  IsActive BIT CONSTRAINT DF_Employees_IsActive DEFAULT (1),
  CreatedDate DATETIME2 CONSTRAINT DF_Employees_CreatedDate DEFAULT (SYSDATETIME()),
  LastUpdated DATETIME2 CONSTRAINT DF_Employees_LastUpdated DEFAULT (SYSDATETIME()),
  CONSTRAINT PK_Employees PRIMARY KEY (EmployeeID),
  CONSTRAINT UK_Employees_Code UNIQUE (EmployeeCode),
  CONSTRAINT FK_Employees_ReportsTo FOREIGN KEY (ReportsTo) REFERENCES dbo.Employees(EmployeeID)
);
GO

CREATE INDEX IX_Employees_Name ON dbo.Employees (LastName, FirstName);
CREATE INDEX IX_Employees_Department ON dbo.Employees (Department);
CREATE INDEX IX_Employees_ReportsTo ON dbo.Employees (ReportsTo);
GO

IF OBJECT_ID('dbo.trg_employees_modtime', 'TR') IS NOT NULL DROP TRIGGER dbo.trg_employees_modtime;
GO
CREATE TRIGGER dbo.trg_employees_modtime
ON dbo.Employees
AFTER UPDATE
AS
BEGIN
  SET NOCOUNT ON;
  UPDATE e
    SET LastUpdated = SYSDATETIME()
  FROM dbo.Employees e
  INNER JOIN inserted i ON e.EmployeeID = i.EmployeeID;
END;
GO

-- ========================================
-- 6. 주문 요약 뷰
-- ========================================
CREATE VIEW dbo.vw_OrderSummary AS
SELECT 
  o.OrderID,
  o.OrderNumber,
  o.OrderDate,
  c.CustomerCode,
  c.CustomerName,
  c.City AS CustomerCity,
  c.Region AS CustomerRegion,
  o.OrderStatus,
  o.PaymentStatus,
  o.TotalAmount,
  (SELECT COUNT(*) FROM dbo.OrderDetails od WHERE od.OrderID = o.OrderID) AS ItemCount,
  (SELECT ISNULL(SUM(od.Quantity),0) FROM dbo.OrderDetails od WHERE od.OrderID = o.OrderID) AS TotalQuantity,
  o.CreatedDate
FROM dbo.Orders o
JOIN dbo.Customers c ON o.CustomerID = c.CustomerID;
GO

PRINT 'MSSQL용 샘플 테이블 생성 완료';
