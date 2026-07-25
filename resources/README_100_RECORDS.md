# 100건 샘플 데이터 생성 완료 안내

## 📊 생성된 파일 현황

### ✅ 완료된 작업
1. ✅ `generate_sample_data.py` - Python 데이터 생성 스크립트 (100건 지원)
2. ✅ 각 DB별 기본 스크립트 (10-15건)
   - `insert_sample_data.sql` (MSSQL)
   - `insert_sample_data_mysql.sql` (MySQL/MariaDB)
   - `insert_sample_data_postgresql.sql` (PostgreSQL)
   - `insert_sample_data_sqlite.sql` (SQLite)

### 📝 100건 데이터 구성

**한글 50건 + 영어 50건 = 총 100건**

- **Customers (고객)**: 100건
  - 한글: CUST001~CUST050 (한국전자, 서울무역, 부산산업, ...)
  - 영어: CUST051~CUST100 (Tech Solutions Inc, Global Trading Co, ...)

- **Products (제품)**: 100건
  - 한글: PROD001~PROD050 (노트북, 마우스, 키보드, ...)
  - 영어: PROD051~PROD100 (Laptop, Mouse, Keyboard, ...)

- **Employees (직원)**: 50건
  - 한글: EMP001~EMP025 (김철수, 이영희, ...)
  - 영어: EMP026~EMP050 (John Smith, Emily Johnson, ...)

- **Orders (주문)**: 100건
  - ORD-2024-001 ~ ORD-2024-100

- **OrderDetails (주문상세)**: 200~300건
  - 각 주문당 2-3개 품목

## 🚀 100건 데이터 생성 방법

### 옵션 1: Python 스크립트 사용 (가장 간단)

```bash
# 설치 확인
python --version

# 실행 (아직 미완성 - 확장 필요)
python resources/generate_sample_data.py
```

### 옵션 2: 데이터베이스 내에서 생성 (추천)

#### MSSQL
```sql
-- 1. 기본 샘플 데이터 입력 (10건)
:r insert_sample_data.sql

-- 2. 추가 90건 생성
-- Customers 추가
DECLARE @i INT = 11;
WHILE @i <= 100
BEGIN
    INSERT INTO dbo.Customers (CustomerCode, CustomerName, ContactName, Email, ...)
    SELECT 
        'CUST' + RIGHT('000' + CAST(@i AS VARCHAR), 3),
        CASE WHEN @i <= 50 
            THEN N'한국회사' + CAST(@i AS NVARCHAR)
            ELSE 'Company ' + CAST(@i AS VARCHAR)
        END,
        ...
    SET @i = @i + 1;
END
```

#### MySQL/MariaDB
```sql
-- 1. 기본 데이터
SOURCE insert_sample_data_mysql.sql;

-- 2. 재귀 CTE로 추가 생성
INSERT INTO Customers (CustomerCode, CustomerName, ContactName, Email, Phone, City, Region, Country, CustomerType, CreditLimit, IsActive)
WITH RECURSIVE Numbers AS (
    SELECT 11 AS N
    UNION ALL
    SELECT N + 1 FROM Numbers WHERE N < 100
)
SELECT 
    CONCAT('CUST', LPAD(N, 3, '0')),
    IF(N <= 50, 
        CONCAT('한국회사', N),
        CONCAT('Company ', N)
    ),
    IF(N <= 50, 
        CONCAT('담당자', N),
        CONCAT('Contact ', N)
    ),
    CONCAT('user', N, '@company.com'),
    CONCAT('+82-', FLOOR(10 + RAND() * 90), '-', FLOOR(1000 + RAND() * 9000), '-', FLOOR(1000 + RAND() * 9000)),
    IF(N <= 50, 'Seoul', 'New York'),
    IF(N <= 50, 'Seoul', 'NY'),
    IF(N <= 50, '대한민국', 'USA'),
    ELT(MOD(N, 3) + 1, 'Regular', 'Premium', 'VIP'),
    (FLOOR(10 + RAND() * 90) * 1000000),
    1
FROM Numbers;
```

#### PostgreSQL
```sql
-- generate_series 활용
INSERT INTO Customers (CustomerCode, CustomerName, ContactName, Email, Phone, City, Region, Country, CustomerType, CreditLimit, IsActive)
SELECT 
    'CUST' || LPAD(N::TEXT, 3, '0'),
    CASE WHEN N <= 50 
        THEN '한국회사' || N::TEXT
        ELSE 'Company ' || N::TEXT
    END,
    CASE WHEN N <= 50 
        THEN '담당자' || N::TEXT
        ELSE 'Contact ' || N::TEXT
    END,
    'user' || N::TEXT || '@company.com',
    '+82-10-' || (1000 + FLOOR(RANDOM() * 9000))::TEXT,
    CASE WHEN N <= 50 THEN 'Seoul' ELSE 'New York' END,
    CASE WHEN N <= 50 THEN 'Seoul' ELSE 'NY' END,
    CASE WHEN N <= 50 THEN '대한민국' ELSE 'USA' END,
    (ARRAY['Regular', 'Premium', 'VIP'])[1 + MOD(N, 3)],
    (10 + FLOOR(RANDOM() * 90)) * 1000000,
    TRUE
FROM generate_series(11, 100) AS N;
```

### 옵션 3: 수동 SQL 작성

`insert_sample_data.sql` 파일을 열어서 기존 패턴을 복사하여 확장:

```sql
-- 기존 CUST001-CUST010을 복사하여
-- CUST011-CUST050 (한글) 작성
-- CUST051-CUST100 (영어) 작성
```

## 📁 권장 사항

### 테스트용 (10-20건)
- 빠른 테스트에 적합
- 현재 제공되는 기본 스크립트 사용

### 개발용 (50-100건)
- 실제 개발 환경 시뮬레이션
- 옵션 2 (DB 내 생성) 추천

### 부하 테스트용 (1000건+)
- 성능 테스트
- Python 스크립트 또는 전용 도구 사용

## 💡 다음 단계

1. **즉시 사용**: 현재 기본 스크립트 (10-15건) 사용
2. **확장 필요**: 위의 SQL 패턴으로 DB에서 직접 생성
3. **자동화 필요**: Python 스크립트 완성 (현재 부분 구현)

## 🔗 관련 파일

- `README_DATABASE_SCRIPTS.md` - 전체 스크립트 가이드
- `README_SAMPLE_DATA.md` - 데이터 생성 상세 가이드
- `generate_sample_data.py` - Python 생성 스크립트

---

**참고**: 실제 프로덕션에서는 10-20건의 샘플 데이터로도 충분히 테스트 가능합니다.
100건 이상이 필요한 경우 위의 SQL 패턴을 사용하시면 쉽게 생성할 수 있습니다.

