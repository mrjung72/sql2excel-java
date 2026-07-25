# 샘플 데이터 생성 가이드

## 📊 현재 샘플 데이터

기본 제공되는 샘플 데이터:
- **Customers**: 10건 (한글)
- **Products**: 15건 (한글)
- **Employees**: 10건 (한글)
- **Orders**: 10건
- **OrderDetails**: 22건

## 🚀 100건 샘플 데이터 생성 방법

### 방법 1: Python 스크립트 사용 (추천)

```bash
# Python 스크립트 실행
python resources/generate_sample_data.py > resources/insert_sample_data_100.sql

# 생성된 SQL 파일 실행
# MSSQL
sqlcmd -S localhost -U sa -P yourpassword -i resources/insert_sample_data_100.sql

# MySQL
mysql -u root -p < resources/insert_sample_data_100_mysql.sql

# PostgreSQL
psql -U postgres -d sampledb -f resources/insert_sample_data_100_postgresql.sql
```

### 방법 2: 직접 SQL 작성

샘플 데이터 패턴을 참고하여 직접 작성:

#### 한글 데이터 (50건)
```sql
-- 고객
('CUST001', '(주)한국전자', '김철수', ...),
('CUST002', '서울무역상사', '이영희', ...),
...
('CUST050', '인천스마트시티', '한예진', ...)
```

#### 영어 데이터 (50건)
```sql
-- 고객
('CUST051', 'Tech Solutions Inc', 'John Smith', ...),
('CUST052', 'Global Trading Co', 'Emily Johnson', ...),
...
('CUST100', 'Defense Systems', 'Nora Bell', ...)
```

### 방법 3: 데이터베이스 내 생성

#### MSSQL
```sql
-- Numbers 테이블 생성 (1-100)
WITH Numbers AS (
    SELECT TOP 100 ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS N
    FROM sys.objects, sys.columns
)
INSERT INTO Customers (CustomerCode, CustomerName, ContactName, Email, ...)
SELECT 
    'CUST' + RIGHT('000' + CAST(N AS VARCHAR), 3),
    CASE WHEN N <= 50 
        THEN '한국회사' + CAST(N AS VARCHAR)
        ELSE 'Company ' + CAST(N AS VARCHAR)
    END,
    ...
FROM Numbers
```

#### MySQL/MariaDB
```sql
-- 재귀 CTE 사용
WITH RECURSIVE Numbers AS (
    SELECT 1 AS N
    UNION ALL
    SELECT N + 1 FROM Numbers WHERE N < 100
)
INSERT INTO Customers (CustomerCode, CustomerName, ...)
SELECT 
    CONCAT('CUST', LPAD(N, 3, '0')),
    IF(N <= 50, 
        CONCAT('한국회사', N),
        CONCAT('Company ', N)
    ),
    ...
FROM Numbers;
```

#### PostgreSQL
```sql
-- generate_series 사용
INSERT INTO Customers (CustomerCode, CustomerName, ContactName, ...)
SELECT 
    'CUST' || LPAD(N::TEXT, 3, '0'),
    CASE WHEN N <= 50 
        THEN '한국회사' || N::TEXT
        ELSE 'Company ' || N::TEXT
    END,
    ...
FROM generate_series(1, 100) AS N;
```

## 📝 Python 스크립트 상세

`generate_sample_data.py` 스크립트는 다음 기능을 제공합니다:

### 기능
1. **100건 고객 데이터** (한글 50 + 영어 50)
2. **100건 제품 데이터** (한글 50 + 영어 50)
3. **50건 직원 데이터** (한글 25 + 영어 25)
4. **100건 주문 데이터** (날짜 분산)
5. **200-300건 주문 상세** (주문당 2-3개)

### 사용 예제
```bash
# 전체 데이터 생성
python resources/generate_sample_data.py --all --output mssql

# 고객만 생성
python resources/generate_sample_data.py --customers 100 --lang mixed

# 특정 DB용 생성
python resources/generate_sample_data.py --db mysql --count 100

# 한글만 또는 영어만
python resources/generate_sample_data.py --customers 100 --lang korean
python resources/generate_sample_data.py --customers 100 --lang english
```

## 🎯 데이터 생성 패턴

### 한글 데이터 (1-50)
- **회사명**: 한국전자, 서울무역, 부산산업, ...
- **담당자**: 김철수, 이영희, 박민수, ...
- **지역**: 서울, 부산, 대구, 인천, 광주, 대전, 울산, 제주
- **전화**: 02-XXXX-XXXX, 051-XXXX-XXXX

### 영어 데이터 (51-100)
- **Company**: Tech Solutions Inc, Global Trading Co, ...
- **Contact**: John Smith, Emily Johnson, ...
- **Location**: San Francisco, New York, London, ...
- **Phone**: +1-555-XXXX, +44-20-XXXX

## 💡 Tips

1. **대량 데이터 생성 시 성능**
   - BULK INSERT 사용
   - 인덱스 비활성화 후 데이터 입력
   - 트랜잭션 배치 처리

2. **현실적인 데이터**
   - 날짜 분산 (최근 6개월)
   - 지역별 분포 고려
   - 고객 등급 비율 (VIP 10%, Premium 30%, Regular 60%)

3. **참조 무결성**
   - 외래 키 관계 유지
   - 순서: Customers → Products → Orders → OrderDetails

## 📚 참고

- 기본 샘플 데이터: `insert_sample_data.sql` (각 DB별)
- 테이블 스키마: `create_sample_tables.sql` (각 DB별)
- 데이터 삭제: `drop_sample_tables.sql` (각 DB별)

---

**Note**: Python 스크립트는 Python 3.6 이상이 필요합니다.

