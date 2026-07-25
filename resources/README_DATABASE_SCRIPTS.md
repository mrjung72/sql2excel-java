# Database Sample Scripts Guide

각 데이터베이스별 샘플 테이블 생성 및 데이터 입력 스크립트입니다.

## 📁 파일 구조

```
resources/
├── README_DATABASE_SCRIPTS.md      (이 파일)
│
├── MSSQL (SQL Server)
│   ├── create_sample_tables.sql          테이블 생성
│   ├── insert_sample_data.sql            샘플 데이터 입력
│   └── drop_sample_tables.sql            테이블 삭제
│
├── MySQL / MariaDB
│   ├── create_sample_tables_mysql.sql    테이블 생성
│   ├── insert_sample_data_mysql.sql      샘플 데이터 입력
│   └── drop_sample_tables_mysql.sql      테이블 삭제
│
├── PostgreSQL
│   ├── create_sample_tables_postgresql.sql    테이블 생성
│   ├── insert_sample_data_postgresql.sql      샘플 데이터 입력
│   └── drop_sample_tables_postgresql.sql      테이블 삭제
│
└── SQLite
    ├── create_sample_tables_sqlite.sql    테이블 생성
    ├── insert_sample_data_sqlite.sql      샘플 데이터 입력
    └── drop_sample_tables_sqlite.sql      테이블 삭제
```

## 🗄️ 테이블 구조

모든 데이터베이스에 동일한 5개 테이블이 생성됩니다:

1. **Customers** - 고객 정보 (10건)
2. **Products** - 제품 정보 (15건)
3. **Employees** - 직원 정보 (10건)
4. **Orders** - 주문 정보 (10건)
5. **OrderDetails** - 주문 상세 (22건)

## 📝 사용 방법

### 1. MSSQL (SQL Server)

#### SSMS (SQL Server Management Studio) 사용:
```sql
-- 1. 테이블 생성
:r create_sample_tables.sql

-- 2. 데이터 입력
:r insert_sample_data.sql

-- 3. 테이블 삭제 (필요시)
:r drop_sample_tables.sql
```

#### sqlcmd 명령줄 사용:
```bash
# 테이블 생성
sqlcmd -S localhost -U sa -P yourpassword -i create_sample_tables.sql

# 데이터 입력
sqlcmd -S localhost -U sa -P yourpassword -i insert_sample_data.sql

# 테이블 삭제
sqlcmd -S localhost -U sa -P yourpassword -i drop_sample_tables.sql
```

---

### 2. MySQL / MariaDB

#### MySQL Workbench 또는 명령줄 사용:
```bash
# 테이블 생성
mysql -u root -p < create_sample_tables_mysql.sql

# 데이터 입력
mysql -u root -p < insert_sample_data_mysql.sql

# 테이블 삭제
mysql -u root -p < drop_sample_tables_mysql.sql
```

#### MySQL 클라이언트 내부에서:
```sql
SOURCE create_sample_tables_mysql.sql;
SOURCE insert_sample_data_mysql.sql;
SOURCE drop_sample_tables_mysql.sql;
```

---

### 3. PostgreSQL

#### psql 사용:
```bash
# 데이터베이스 생성 (처음 한 번만)
createdb sampledb

# psql 접속
psql -U postgres -d sampledb

# psql 내부에서 실행
\i create_sample_tables_postgresql.sql
\i insert_sample_data_postgresql.sql
\i drop_sample_tables_postgresql.sql
```

#### 명령줄에서 직접 실행:
```bash
# 테이블 생성
psql -U postgres -d sampledb -f create_sample_tables_postgresql.sql

# 데이터 입력
psql -U postgres -d sampledb -f insert_sample_data_postgresql.sql

# 테이블 삭제
psql -U postgres -d sampledb -f drop_sample_tables_postgresql.sql
```

---

### 4. SQLite

#### SQLite 명령줄 사용:
```bash
# 데이터베이스 파일 생성 및 테이블 생성
sqlite3 sampledb.sqlite < create_sample_tables_sqlite.sql

# 데이터 입력
sqlite3 sampledb.sqlite < insert_sample_data_sqlite.sql

# 테이블 삭제
sqlite3 sampledb.sqlite < drop_sample_tables_sqlite.sql
```

#### SQLite 클라이언트 내부에서:
```bash
# SQLite 실행
sqlite3 sampledb.sqlite

# 내부에서 실행
.read create_sample_tables_sqlite.sql
.read insert_sample_data_sqlite.sql
.read drop_sample_tables_sqlite.sql
```

---

## 🔗 외래 키 관계

```
Customers (1) -----> (N) Orders
                          |
                          | (1)
                          |
                          v
                        (N) OrderDetails (N) <----- (1) Products
                        
Employees (1) -----> (N) Employees (ReportsTo - 자기참조)
```

## 📊 샘플 데이터 통계

| 테이블 | 레코드 수 | 설명 |
|--------|----------|------|
| Customers | 10 | 다양한 지역의 고객 |
| Products | 15 | 전자제품, 사무기기, 가구 등 |
| Employees | 10 | 조직 계층 구조 포함 |
| Orders | 10 | 2024년 1-2월 주문 |
| OrderDetails | 22 | 주문별 상세 내역 |

## 🎯 SQL2Excel 테스트 쿼리 예제

### 고객별 주문 통계
```sql
-- MySQL/MariaDB/PostgreSQL
SELECT 
    c.CustomerName,
    c.City,
    COUNT(o.OrderID) as OrderCount,
    SUM(o.NetAmount) as TotalAmount
FROM Customers c
LEFT JOIN Orders o ON c.CustomerID = o.CustomerID
GROUP BY c.CustomerID, c.CustomerName, c.City
ORDER BY TotalAmount DESC;

-- MSSQL
SELECT TOP 10
    c.CustomerName,
    c.City,
    COUNT(o.OrderID) as OrderCount,
    SUM(o.NetAmount) as TotalAmount
FROM Customers c
LEFT JOIN Orders o ON c.CustomerID = o.CustomerID
GROUP BY c.CustomerID, c.CustomerName, c.City
ORDER BY TotalAmount DESC;
```

### 제품별 판매 통계
```sql
SELECT 
    p.ProductName,
    p.Category,
    SUM(od.Quantity) as TotalSold,
    SUM(od.LineTotal) as TotalRevenue
FROM Products p
LEFT JOIN OrderDetails od ON p.ProductID = od.ProductID
GROUP BY p.ProductID, p.ProductName, p.Category
ORDER BY TotalRevenue DESC
LIMIT 10;  -- MySQL/MariaDB/PostgreSQL/SQLite
```

### 부서별 직원 통계
```sql
SELECT 
    Department,
    COUNT(*) as EmployeeCount,
    AVG(Salary) as AvgSalary,
    MIN(Salary) as MinSalary,
    MAX(Salary) as MaxSalary
FROM Employees
WHERE IsActive = TRUE  -- PostgreSQL/MySQL/MariaDB
-- WHERE IsActive = 1  -- MSSQL/SQLite
GROUP BY Department
ORDER BY EmployeeCount DESC;
```

## 💡 Tips

1. **초기화 순서**: DROP → CREATE → INSERT
2. **외래 키**: 삭제 시 OrderDetails → Orders → Customers 순서 유지
3. **인코딩**: UTF-8 사용 권장
4. **날짜 형식**: 
   - MSSQL: `datetime2(7)`
   - MySQL/MariaDB: `DATETIME`
   - PostgreSQL: `TIMESTAMP`
   - SQLite: `TEXT` (ISO format)

## 🚀 빠른 시작

```bash
# 1. MySQL 예제
mysql -u root -p < create_sample_tables_mysql.sql
mysql -u root -p < insert_sample_data_mysql.sql

# 2. SQL2Excel로 테스트
node src/excel-cli.js export --xml ./queries/mysql-test-queries.xml

# 3. 정리
mysql -u root -p < drop_sample_tables_mysql.sql
```

---

**Note**: 각 데이터베이스의 설정 파일(`config/dbinfo.json`)도 함께 확인하세요!

