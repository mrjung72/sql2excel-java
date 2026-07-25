-- ========================================
-- 샘플 테이블 생성 스크립트
-- PostgreSQL용
-- ========================================

-- 샘플 데이터베이스 생성 (존재하지 않는 경우)
-- Note: 이 부분은 psql에서 실행하거나, 이미 sampledb가 생성되어 있어야 합니다.
-- CREATE DATABASE sampledb ENCODING 'UTF8';

-- \c sampledb

-- ========================================
-- 1. Customers 테이블 생성
-- ========================================

DROP TABLE IF EXISTS Customers CASCADE;

CREATE TABLE Customers (
    CustomerID SERIAL NOT NULL,
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
    CustomerType VARCHAR(20) DEFAULT 'Regular',
    CreditLimit DECIMAL(15,2) DEFAULT 0,
    IsActive BOOLEAN DEFAULT TRUE,
    CreatedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    LastUpdated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT PK_Customers PRIMARY KEY (CustomerID),
    CONSTRAINT UK_Customers_Code UNIQUE (CustomerCode)
);

-- 인덱스 생성
CREATE INDEX IX_Customers_Name ON Customers (CustomerName);
CREATE INDEX IX_Customers_City ON Customers (City);
CREATE INDEX IX_Customers_Region ON Customers (Region);
CREATE INDEX IX_Customers_Type ON Customers (CustomerType);

-- LastUpdated 자동 업데이트를 위한 트리거 함수
CREATE OR REPLACE FUNCTION update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.LastUpdated = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_customers_modtime
    BEFORE UPDATE ON Customers
    FOR EACH ROW
    EXECUTE FUNCTION update_modified_column();

-- ========================================
-- 2. Orders 테이블 생성
-- ========================================

DROP TABLE IF EXISTS Orders CASCADE;

CREATE TABLE Orders (
    OrderID SERIAL NOT NULL,
    OrderNumber VARCHAR(30) NOT NULL,
    CustomerID INTEGER NOT NULL,
    OrderDate TIMESTAMP NOT NULL,
    RequiredDate TIMESTAMP NULL,
    ShippedDate TIMESTAMP NULL,
    OrderStatus VARCHAR(20) DEFAULT 'Pending',
    SubTotal DECIMAL(15,2) DEFAULT 0,
    TotalAmount DECIMAL(15,2) DEFAULT 0,
    DiscountAmount DECIMAL(15,2) DEFAULT 0,
    TaxAmount DECIMAL(15,2) DEFAULT 0,
    NetAmount DECIMAL(15,2) DEFAULT 0,
    ShippingAddress VARCHAR(200) NULL,
    PaymentMethod VARCHAR(30) NULL,
    PaymentStatus VARCHAR(20) DEFAULT 'Unpaid',
    EmployeeID INTEGER NULL,
    Notes TEXT NULL,
    CreatedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    LastUpdated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT PK_Orders PRIMARY KEY (OrderID),
    CONSTRAINT UK_Orders_Number UNIQUE (OrderNumber),
    CONSTRAINT FK_Orders_Customers 
        FOREIGN KEY (CustomerID) 
        REFERENCES Customers(CustomerID)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

CREATE INDEX IX_Orders_Customer ON Orders (CustomerID);
CREATE INDEX IX_Orders_Date ON Orders (OrderDate);
CREATE INDEX IX_Orders_Status ON Orders (OrderStatus);

CREATE TRIGGER update_orders_modtime
    BEFORE UPDATE ON Orders
    FOR EACH ROW
    EXECUTE FUNCTION update_modified_column();

-- ========================================
-- 3. Products 테이블 생성
-- ========================================

DROP TABLE IF EXISTS Products CASCADE;

CREATE TABLE Products (
    ProductID SERIAL NOT NULL,
    ProductCode VARCHAR(20) NOT NULL,
    ProductName VARCHAR(100) NOT NULL,
    Category VARCHAR(50) NULL,
    UnitPrice DECIMAL(15,2) DEFAULT 0,
    UnitsInStock INTEGER DEFAULT 0,
    UnitsOnOrder INTEGER DEFAULT 0,
    ReorderLevel INTEGER DEFAULT 0,
    Discontinued BOOLEAN DEFAULT FALSE,
    Description TEXT NULL,
    CreatedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    LastUpdated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT PK_Products PRIMARY KEY (ProductID),
    CONSTRAINT UK_Products_Code UNIQUE (ProductCode)
);

CREATE INDEX IX_Products_Name ON Products (ProductName);
CREATE INDEX IX_Products_Category ON Products (Category);
CREATE INDEX IX_Products_Price ON Products (UnitPrice);

CREATE TRIGGER update_products_modtime
    BEFORE UPDATE ON Products
    FOR EACH ROW
    EXECUTE FUNCTION update_modified_column();

-- ========================================
-- 4. OrderDetails 테이블 생성
-- ========================================

DROP TABLE IF EXISTS OrderDetails CASCADE;

CREATE TABLE OrderDetails (
    OrderDetailID SERIAL NOT NULL,
    OrderID INTEGER NOT NULL,
    ProductID INTEGER NOT NULL,
    UnitPrice DECIMAL(15,2) NOT NULL,
    Quantity INTEGER NOT NULL DEFAULT 1,
    Discount DECIMAL(5,2) DEFAULT 0,
    LineTotal DECIMAL(15,2) GENERATED ALWAYS AS (UnitPrice * Quantity * (1 - Discount / 100)) STORED,
    CreatedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT PK_OrderDetails PRIMARY KEY (OrderDetailID),
    CONSTRAINT FK_OrderDetails_Orders 
        FOREIGN KEY (OrderID) 
        REFERENCES Orders(OrderID)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT FK_OrderDetails_Products 
        FOREIGN KEY (ProductID) 
        REFERENCES Products(ProductID)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

CREATE INDEX IX_OrderDetails_Order ON OrderDetails (OrderID);
CREATE INDEX IX_OrderDetails_Product ON OrderDetails (ProductID);

-- ========================================
-- 5. Employees 테이블 생성
-- ========================================

DROP TABLE IF EXISTS Employees CASCADE;

CREATE TABLE Employees (
    EmployeeID SERIAL NOT NULL,
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
    ReportsTo INTEGER NULL,
    IsActive BOOLEAN DEFAULT TRUE,
    CreatedDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    LastUpdated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT PK_Employees PRIMARY KEY (EmployeeID),
    CONSTRAINT UK_Employees_Code UNIQUE (EmployeeCode),
    CONSTRAINT FK_Employees_ReportsTo 
        FOREIGN KEY (ReportsTo) 
        REFERENCES Employees(EmployeeID)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

CREATE INDEX IX_Employees_Name ON Employees (LastName, FirstName);
CREATE INDEX IX_Employees_Department ON Employees (Department);
CREATE INDEX IX_Employees_ReportsTo ON Employees (ReportsTo);

CREATE TRIGGER update_employees_modtime
    BEFORE UPDATE ON Employees
    FOR EACH ROW
    EXECUTE FUNCTION update_modified_column();

-- ========================================
-- 완료 메시지
-- ========================================

SELECT 'Sample tables created successfully for PostgreSQL!' AS Message;

