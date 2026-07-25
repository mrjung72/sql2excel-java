-- ========================================
-- 샘플 테이블 생성 스크립트
-- SQLite용
-- ========================================

-- SQLite는 파일 기반이므로 데이터베이스 생성 불필요
-- 파일명: sampledb.sqlite

-- ========================================
-- 1. Customers 테이블 생성
-- ========================================

DROP TABLE IF EXISTS Customers;

CREATE TABLE Customers (
    CustomerID INTEGER PRIMARY KEY AUTOINCREMENT,
    CustomerCode TEXT NOT NULL UNIQUE,
    CustomerName TEXT NOT NULL,
    ContactName TEXT,
    Email TEXT,
    Phone TEXT,
    Address TEXT,
    City TEXT,
    Region TEXT,
    PostalCode TEXT,
    Country TEXT,
    CustomerType TEXT DEFAULT 'Regular',
    CreditLimit REAL DEFAULT 0,
    IsActive INTEGER DEFAULT 1,  -- SQLite uses INTEGER for BOOLEAN (0=false, 1=true)
    CreatedDate TEXT DEFAULT (datetime('now', 'localtime')),
    LastUpdated TEXT DEFAULT (datetime('now', 'localtime'))
);

-- 인덱스 생성
CREATE INDEX IX_Customers_Name ON Customers (CustomerName);
CREATE INDEX IX_Customers_City ON Customers (City);
CREATE INDEX IX_Customers_Region ON Customers (Region);
CREATE INDEX IX_Customers_Type ON Customers (CustomerType);

-- LastUpdated 자동 업데이트를 위한 트리거
CREATE TRIGGER update_customers_timestamp 
AFTER UPDATE ON Customers
FOR EACH ROW
BEGIN
    UPDATE Customers 
    SET LastUpdated = datetime('now', 'localtime')
    WHERE CustomerID = NEW.CustomerID;
END;

-- ========================================
-- 2. Orders 테이블 생성
-- ========================================

DROP TABLE IF EXISTS Orders;

CREATE TABLE Orders (
    OrderID INTEGER PRIMARY KEY AUTOINCREMENT,
    OrderNumber TEXT NOT NULL UNIQUE,
    CustomerID INTEGER NOT NULL,
    OrderDate TEXT NOT NULL,
    RequiredDate TEXT,
    ShippedDate TEXT,
    OrderStatus TEXT DEFAULT 'Pending',
    TotalAmount REAL DEFAULT 0,
    DiscountAmount REAL DEFAULT 0,
    TaxAmount REAL DEFAULT 0,
    NetAmount REAL DEFAULT 0,
    ShippingAddress TEXT,
    Notes TEXT,
    CreatedDate TEXT DEFAULT (datetime('now', 'localtime')),
    LastUpdated TEXT DEFAULT (datetime('now', 'localtime')),
    
    FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

CREATE INDEX IX_Orders_Customer ON Orders (CustomerID);
CREATE INDEX IX_Orders_Date ON Orders (OrderDate);
CREATE INDEX IX_Orders_Status ON Orders (OrderStatus);

CREATE TRIGGER update_orders_timestamp 
AFTER UPDATE ON Orders
FOR EACH ROW
BEGIN
    UPDATE Orders 
    SET LastUpdated = datetime('now', 'localtime')
    WHERE OrderID = NEW.OrderID;
END;

-- ========================================
-- 3. Products 테이블 생성
-- ========================================

DROP TABLE IF EXISTS Products;

CREATE TABLE Products (
    ProductID INTEGER PRIMARY KEY AUTOINCREMENT,
    ProductCode TEXT NOT NULL UNIQUE,
    ProductName TEXT NOT NULL,
    Category TEXT,
    UnitPrice REAL DEFAULT 0,
    UnitsInStock INTEGER DEFAULT 0,
    UnitsOnOrder INTEGER DEFAULT 0,
    ReorderLevel INTEGER DEFAULT 0,
    Discontinued INTEGER DEFAULT 0,
    Description TEXT,
    CreatedDate TEXT DEFAULT (datetime('now', 'localtime')),
    LastUpdated TEXT DEFAULT (datetime('now', 'localtime'))
);

CREATE INDEX IX_Products_Name ON Products (ProductName);
CREATE INDEX IX_Products_Category ON Products (Category);
CREATE INDEX IX_Products_Price ON Products (UnitPrice);

CREATE TRIGGER update_products_timestamp 
AFTER UPDATE ON Products
FOR EACH ROW
BEGIN
    UPDATE Products 
    SET LastUpdated = datetime('now', 'localtime')
    WHERE ProductID = NEW.ProductID;
END;

-- ========================================
-- 4. OrderDetails 테이블 생성
-- ========================================

DROP TABLE IF EXISTS OrderDetails;

CREATE TABLE OrderDetails (
    OrderDetailID INTEGER PRIMARY KEY AUTOINCREMENT,
    OrderID INTEGER NOT NULL,
    ProductID INTEGER NOT NULL,
    UnitPrice REAL NOT NULL,
    Quantity INTEGER NOT NULL DEFAULT 1,
    Discount REAL DEFAULT 0,
    LineTotal REAL GENERATED ALWAYS AS (UnitPrice * Quantity * (1 - Discount / 100)) STORED,
    CreatedDate TEXT DEFAULT (datetime('now', 'localtime')),
    
    FOREIGN KEY (OrderID) REFERENCES Orders(OrderID)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    FOREIGN KEY (ProductID) REFERENCES Products(ProductID)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

CREATE INDEX IX_OrderDetails_Order ON OrderDetails (OrderID);
CREATE INDEX IX_OrderDetails_Product ON OrderDetails (ProductID);

-- ========================================
-- 5. Employees 테이블 생성
-- ========================================

DROP TABLE IF EXISTS Employees;

CREATE TABLE Employees (
    EmployeeID INTEGER PRIMARY KEY AUTOINCREMENT,
    EmployeeCode TEXT NOT NULL UNIQUE,
    FirstName TEXT NOT NULL,
    LastName TEXT NOT NULL,
    Title TEXT,
    BirthDate TEXT,  -- SQLite stores dates as TEXT in ISO format
    HireDate TEXT,
    Email TEXT,
    Phone TEXT,
    Department TEXT,
    Salary REAL,
    ReportsTo INTEGER,
    IsActive INTEGER DEFAULT 1,
    CreatedDate TEXT DEFAULT (datetime('now', 'localtime')),
    LastUpdated TEXT DEFAULT (datetime('now', 'localtime')),
    
    FOREIGN KEY (ReportsTo) REFERENCES Employees(EmployeeID)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

CREATE INDEX IX_Employees_Name ON Employees (LastName, FirstName);
CREATE INDEX IX_Employees_Department ON Employees (Department);
CREATE INDEX IX_Employees_ReportsTo ON Employees (ReportsTo);

CREATE TRIGGER update_employees_timestamp 
AFTER UPDATE ON Employees
FOR EACH ROW
BEGIN
    UPDATE Employees 
    SET LastUpdated = datetime('now', 'localtime')
    WHERE EmployeeID = NEW.EmployeeID;
END;

-- ========================================
-- 완료 메시지
-- ========================================

SELECT 'Sample tables created successfully for SQLite!' AS Message;

