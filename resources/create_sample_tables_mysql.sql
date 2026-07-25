-- ========================================
-- 샘플 테이블 생성 스크립트
-- MySQL / MariaDB용
-- ========================================

-- 샘플 데이터베이스 생성 (존재하지 않는 경우)
CREATE DATABASE IF NOT EXISTS sampledb
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE sampledb;

-- ========================================
-- 1. Customers 테이블 생성
-- ========================================

DROP TABLE IF EXISTS Customers;

CREATE TABLE Customers (
    CustomerID INT AUTO_INCREMENT NOT NULL,
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
    CreatedDate DATETIME DEFAULT CURRENT_TIMESTAMP,
    LastUpdated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    PRIMARY KEY (CustomerID),
    UNIQUE KEY UK_Customers_Code (CustomerCode),
    INDEX IX_Customers_Name (CustomerName),
    INDEX IX_Customers_City (City),
    INDEX IX_Customers_Region (Region),
    INDEX IX_Customers_Type (CustomerType)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- 2. Orders 테이블 생성
-- ========================================

DROP TABLE IF EXISTS Orders;

CREATE TABLE Orders (
    OrderID INT AUTO_INCREMENT NOT NULL,
    OrderNumber VARCHAR(30) NOT NULL,
    CustomerID INT NOT NULL,
    OrderDate DATETIME NOT NULL,
    RequiredDate DATETIME NULL,
    ShippedDate DATETIME NULL,
    OrderStatus VARCHAR(20) DEFAULT 'Pending',
    SubTotal DECIMAL(15,2) DEFAULT 0,
    TotalAmount DECIMAL(15,2) DEFAULT 0,
    DiscountAmount DECIMAL(15,2) DEFAULT 0,
    TaxAmount DECIMAL(15,2) DEFAULT 0,
    NetAmount DECIMAL(15,2) DEFAULT 0,
    ShippingAddress VARCHAR(200) NULL,
    PaymentMethod VARCHAR(30) NULL,
    PaymentStatus VARCHAR(20) DEFAULT 'Unpaid',
    EmployeeID INT NULL,
    Notes TEXT NULL,
    CreatedDate DATETIME DEFAULT CURRENT_TIMESTAMP,
    LastUpdated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    PRIMARY KEY (OrderID),
    UNIQUE KEY UK_Orders_Number (OrderNumber),
    INDEX IX_Orders_Customer (CustomerID),
    INDEX IX_Orders_Date (OrderDate),
    INDEX IX_Orders_Status (OrderStatus),
    
    CONSTRAINT FK_Orders_Customers 
        FOREIGN KEY (CustomerID) 
        REFERENCES Customers(CustomerID)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- 3. Products 테이블 생성
-- ========================================

DROP TABLE IF EXISTS Products;

CREATE TABLE Products (
    ProductID INT AUTO_INCREMENT NOT NULL,
    ProductCode VARCHAR(20) NOT NULL,
    ProductName VARCHAR(100) NOT NULL,
    Category VARCHAR(50) NULL,
    UnitPrice DECIMAL(15,2) DEFAULT 0,
    UnitsInStock INT DEFAULT 0,
    UnitsOnOrder INT DEFAULT 0,
    ReorderLevel INT DEFAULT 0,
    Discontinued BOOLEAN DEFAULT FALSE,
    Description TEXT NULL,
    CreatedDate DATETIME DEFAULT CURRENT_TIMESTAMP,
    LastUpdated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    PRIMARY KEY (ProductID),
    UNIQUE KEY UK_Products_Code (ProductCode),
    INDEX IX_Products_Name (ProductName),
    INDEX IX_Products_Category (Category),
    INDEX IX_Products_Price (UnitPrice)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- 4. OrderDetails 테이블 생성
-- ========================================

DROP TABLE IF EXISTS OrderDetails;

CREATE TABLE OrderDetails (
    OrderDetailID INT AUTO_INCREMENT NOT NULL,
    OrderID INT NOT NULL,
    ProductID INT NOT NULL,
    UnitPrice DECIMAL(15,2) NOT NULL,
    Quantity INT NOT NULL DEFAULT 1,
    Discount DECIMAL(5,2) DEFAULT 0,
    LineTotal DECIMAL(15,2) GENERATED ALWAYS AS (UnitPrice * Quantity * (1 - Discount / 100)) STORED,
    CreatedDate DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (OrderDetailID),
    INDEX IX_OrderDetails_Order (OrderID),
    INDEX IX_OrderDetails_Product (ProductID),
    
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- 5. Employees 테이블 생성
-- ========================================

DROP TABLE IF EXISTS Employees;

CREATE TABLE Employees (
    EmployeeID INT AUTO_INCREMENT NOT NULL,
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
    IsActive BOOLEAN DEFAULT TRUE,
    CreatedDate DATETIME DEFAULT CURRENT_TIMESTAMP,
    LastUpdated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    PRIMARY KEY (EmployeeID),
    UNIQUE KEY UK_Employees_Code (EmployeeCode),
    INDEX IX_Employees_Name (LastName, FirstName),
    INDEX IX_Employees_Department (Department),
    INDEX IX_Employees_ReportsTo (ReportsTo),
    
    CONSTRAINT FK_Employees_ReportsTo 
        FOREIGN KEY (ReportsTo) 
        REFERENCES Employees(EmployeeID)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- 완료 메시지
-- ========================================

SELECT 'Sample tables created successfully for MySQL/MariaDB!' AS Message;

