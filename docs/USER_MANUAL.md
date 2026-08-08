# sql2excel-java 사용자 매뉴얼

SQL 쿼리 결과를 Excel(.xlsx), CSV, JSON, XML, SQL(.sql) 등으로 내보내는 Java 기반 CLI 도구입니다.

---

## 목차

1. [개요](#1-개요)
2. [실행 환경](#2-실행-환경)
3. [디렉토리 구조](#3-디렉토리-구조)
4. [데이터베이스 설정](#4-데이터베이스-설정)
5. [쿼리 정의 파일](#5-쿼리-정의-파일)
6. [변수 시스템](#6-변수-시스템)
7. [동적 시트(Dynamic Sheets)](#7-동적-시트dynamic-sheets)
8. [Excel 스타일 설정](#8-excel-스타일-설정)
9. [출력 형식](#9-출력-형식)
10. [명령어 참조](#10-명령어-참조)
11. [인터랙티브 메뉴](#11-인터랙티브-메뉴)
12. [문제 해결](#12-문제-해결)
13. [예시 모음](#13-예시-모음)

---

## 1. 개요

`sql2excel-java`는 SQL 쿼리 결과를 다중 시트 엑셀 파일로 변환하는 도구입니다. 다음 기능을 제공합니다.

- JSON/XML 형식의 쿼리 정의 파일
- 다중 데이터베이스 연결 (MSSQL, MySQL/MariaDB, PostgreSQL, SQLite, Oracle, Tibero)
- 변수 치환 및 `${DATE.KST:yyyyMMdd}` 형식의 날짜 변수
- 목록 변수를 `IN (...)` 절로 자동 변환
- DB별 `GETDATE()` 대체
- LIMIT/TOP/FETCH FIRST 자동 추가
- `queryDefs`를 통한 공통 쿼리 정의
- `dynamicVars`를 통한 동적 변수
- `dynamic-sheets`를 통한 행 단위 동적 시트 생성
- 시트별 스타일 지정 및 `config/excel-style.json` 스타일 템플릿
- `.xlsx`, `.csv/.txt`, `.json`, `.xml`, `.sql` 출력 지원
- `.xlsx` 출력 시 **목차(TOC) 시트 자동 생성**
- 인터랙티브 메뉴 / 명령줄(CLI) 실행

---

## 2. 실행 환경

### 2.1 요구사항

- JDK 21 이상
- 대상 DB용 JDBC 드라이버 JAR
  - 기본 포함: PostgreSQL, MariaDB, SQLite용 드라이버 (`lib/`)
  - 직접 추가: MSSQL, Oracle, Tibero, MySQL 등 사용 시 `lib/` 디렉토리에 JAR 추가

### 2.2 실행

릴리즈 디렉토리(`target/sql2excel-java-1.2.7/`)에서 다음 중 하나로 실행합니다.

```bash
# Windows
run.bat

# Linux/macOS
./run.sh

# 또는 직접 JAR 실행
java -jar sql2excel-java-1.2.7.jar [명령어] [옵션]
```

---

## 3. 디렉토리 구조

릴리즈 버전 디렉토리 기준(`target/sql2excel-java-1.2.7/`)입니다.

```
sql2excel-java-1.2.7/
├── config/
│   ├── dbinfo.json          # 데이터베이스 연결 설정
│   └── excel-style.json     # Excel 스타일 템플릿
├── datas/                   # SQLite 파일 등 데이터 보관
├── lib/                     # JDBC 드라이버 JAR
│   ├── mariadb-java-client-3.5.9.jar
│   ├── postgresql-42.7.13.jar
│   └── sqlite-jdbc-3.53.2.1.jar
├── queries/                 # 쿼리 정의 파일 (XML/JSON)
├── run.bat                  # Windows 실행 배치
├── run.sh                   # Linux/macOS 실행 스크립트
├── sql2excel-java-1.2.7.jar # 실행 JAR
└── output/                  # (실행 후 생성) 내보낸 파일 저장 위치
```

---

## 4. 데이터베이스 설정 (`config/dbinfo.json`)

각 데이터베이스는 별칭(alias)으로 등록합니다.

### 4.1 공통 필드

| 필드 | 필수 | 설명 |
|------|------|------|
| `type` | 예 | DBMS 종류: `mssql`, `mysql`, `mariadb`, `postgresql`, `sqlite`, `oracle`, `tibero` |
| `driverClass` | 예 | JDBC 드라이버 클래스명. 예: `org.postgresql.Driver` |
| `jar` | 예 | JDBC 드라이버 JAR 경로. 예: `lib/postgresql-42.7.13.jar` |
| `user` | 조건 | 사용자명. (SQLite는 불필요) |
| `password` | 조건 | 비밀번호. (SQLite는 불필요) |
| `server` | 조건 | 호스트. SQLite는 `database` 필드 사용. |
| `port` | 조건 | 포트. 기본값: `mssql 1433`, `mysql/mariadb 3306`, `postgresql 5432`, `oracle 1521`, `tibero 8629` |
| `database` | 조건 | 데이터베이스명. SQLite에서는 파일 경로. |
| `serviceName` | 조건 | Oracle 서비스명 |
| `sid` | 조건 | Oracle SID |
| `connectString` | 아니오 | 전체 JDBC URL 직접 지정. `jdbc:`로 시작하면 그대로 사용. |
| `options` | 아니오 | JDBC 연결 옵션. `connectionTimeout`(ms), `encrypt`, `trustServerCertificate` 등 |

### 4.2 예시

```json
{
  "postgresDB": {
    "type": "postgresql",
    "driverClass": "org.postgresql.Driver",
    "jar": "lib/postgresql-42.7.13.jar",
    "user": "postgres",
    "password": "1111",
    "server": "localhost",
    "database": "postgres",
    "port": 5432,
    "options": {
      "connectionTimeout": 3000
    }
  },
  "mariaDB": {
    "type": "mariadb",
    "driverClass": "org.mariadb.jdbc.Driver",
    "jar": "lib/mariadb-java-client-3.5.9.jar",
    "user": "sahara",
    "password": "1111",
    "server": "localhost",
    "database": "sampledb",
    "port": 3306,
    "options": {
      "connectionTimeout": 3000
    }
  },
  "sqliteDB": {
    "type": "sqlite",
    "driverClass": "org.sqlite.JDBC",
    "jar": "lib/sqlite-jdbc-3.53.2.1.jar",
    "database": "./datas/mydb.sqlite"
  }
}
```

---

## 5. 쿼리 정의 파일

쿼리 정의 파일은 `queries/`에 둡니다. JSON 또는 XML 형식을 지원합니다.  
확장자에 따라 자동으로 파싱됩니다.

### 5.1 공통 구조

| 영역 | 설명 |
|------|------|
| `excel` | 출력 설정, 기본 DB, 기본 행 수, 기본 스타일, 날짜 형식 |
| `vars` | 쿼리에서 사용할 고정 변수 |
| `queryDefs` | 여러 시트에서 참조할 공통 쿼리 정의 |
| `dynamicVars` | SQL 실행 결과로 얻는 동적 변수 |
| `sheets` | 엑셀 시트 단위 쿼리 정의 |
| `dynamic-sheets` | (XML) 동적 시트 템플릿. `dynamicVars` 결과를 반복해 여러 시트 생성 |

### 5.2 `excel` 영역

| 속성 | 필수 | 설명 |
|------|------|------|
| `db` | 조건 | 기본 DB 별칭. 시트별 `db`가 없을 때 사용. |
| `output` | 예 | 출력 파일 경로. 상대/절대 경로 모두 가능. 예: `output/result_${DATE:yyyyMMddHHmmss}.xlsx`, `D:/workspace/output/result_${DATE:yyyyMMddHHmmss}.xlsx` |
| `maxRows` | 아니오 | 전역 최대 조회 행 수. 시트별 `maxRows`가 우선. `0` 또는 비우면 무제한. |
| `style` | 아니오 | 기본 엑셀 스타일 이름 (`excel-style.json`의 key). 시트별 `style`이 우선. |
| `date-column-format` | 아니오 | 날짜 컬럼 기본 표시 형식. 예: `yyyy/MM/dd hh:mm:ss` |

출력 파일 확장자에 따라 다음 형식으로 내보냅니다.

| 확장자 | 출력 형식 |
|--------|-----------|
| `.xlsx` | Excel (통합 워크북) |
| `.csv`, `.txt` | CSV. 시트가 2개 이상이면 `출력_시트명.csv`/`txt`로 분리 |
| `.sql` | SQL. 시트당 `.sql` 파일 (첫 번째 컬럼을 SQL 문으로 출력) |
| `.json` | JSON (통합 파일, 시트명이 key) |
| `.xml` | XML 2개 파일 (속성 기반 + 요소 기반) |

### 5.3 `vars` 영역

`vars`는 쿼리 문자열 `${name}` 형태로 치환됩니다.

- 문자열, 숫자, 날짜, 리스트 사용 가능
- 리스트는 SQL 안에서 `IN (...)` 형태로 자동 변환
- JSON에서는 배열, XML에서는 `["Seoul", "Busan"]` 문법 사용

**JSON 예시**

```json
{
  "vars": {
    "startDate": "2024-01-01",
    "endDate": "2024-06-30",
    "regionList": ["Seoul", "Busan"]
  }
}
```

**XML 예시**

```xml
<vars>
  <var name="startDate">2024-01-01</var>
  <var name="endDate">2024-06-30</var>
  <var name="regionList">["Seoul", "Busan"]</var>
</vars>
```

### 5.4 `queryDefs`

공통 쿼리를 정의하고 `sheet`의 `queryRef`로 참조합니다.

**XML 예시**

```xml
<queryDefs>
  <queryDef id="customer_base">
    <![CDATA[
      SELECT CustomerID, CustomerName
      FROM Customers
      WHERE Region IN (${regionList})
    ]]>
  </queryDef>
</queryDefs>

<sheet name="Customers" queryRef="customer_base" db="postgresDB"/>
```

**JSON 예시**

```json
{
  "queryDefs": {
    "customer_base": "SELECT CustomerID, CustomerName FROM Customers WHERE Region IN (${regionList})"
  },
  "sheets": [
    { "name": "Customers", "queryRef": "customer_base", "db": "postgresDB" }
  ]
}
```

### 5.5 `dynamicVars`

SQL을 미리 실행해 그 결과를 후속 쿼리에서 변수로 사용합니다.

| 속성 | 설명 |
|------|------|
| `name` | 변수명. 결과는 `${name.컬럼명}` 형태로 사용. |
| `type` | `column_identified`(기본) 또는 `key_value_pairs` |
| `db` | 실행 DB. 미지정 시 `excel.db` 사용. |
| `query` | 동적 변수를 조회할 SQL |

**`column_identified`**

각 컬럼을 이름으로 하는 리스트 변수를 만듭니다.

```xml
<dynamicVar name="customerData" db="postgresDB">
  SELECT DISTINCT CustomerID, Region FROM Customers
</dynamicVar>
```

사용 예: `${customerData.CustomerID}`, `${customerData.Region}`

**`key_value_pairs`**

첫 번째 컬럼을 key, 두 번째 컬럼을 value로 사용합니다.

```xml
<dynamicVar name="orderDetails" type="key_value_pairs" db="mariaDB">
  SELECT OrderID, OrderDetailID FROM OrderDetails
</dynamicVar>
```

사용 예: `${orderDetails.1001}` → `2001`

### 5.6 `sheets` 영역

| 속성 | 설명 |
|------|------|
| `name` | 시트명. `${변수}` 치환 가능. 최대 31자. |
| `use` | `true`(기본) 또는 `false`. `false`면 해당 시트를 건너뜀. |
| `db` | DB 별칭. 미지정 시 `excel.db` 사용. |
| `query` | 실행할 SQL. (JSON 필수, XML에는 CDATA/`<query>`/`queryRef` 중 하나) |
| `queryRef` | `queryDefs`의 id 참조. |
| `params` | 시트별 추가 변수. `vars`와 병합됩니다. |
| `maxRows` | 시트별 최대 행 수. `excel.maxRows`보다 우선. |
| `exceptColumns` | 출력에서 제외할 컬럼. 쉼표 구분. |
| `hiddenColumns` | 시트에서 숨길 컬럼. 쉼표 구분. |
| `style` | 시트별 스타일 이름. 미지정 시 `excel.style` 사용. |
| `date-column-format` | 시트별 날짜 표시 형식. |
| `aggregateColumn` | (향후 확보) 집계 기준 컬럼 |

**`hiddenColumns` 사용법 (XML)**

```xml
<sheet name="Orders" db="postgresDB">
  <query hide_columns="InternalID,TempFlag">
    SELECT * FROM Orders
  </query>
</sheet>
```

**JSON `hiddenColumns`**

```json
{
  "sheets": [
    {
      "name": "Orders",
      "hiddenColumns": "InternalID,TempFlag",
      "query": "SELECT * FROM Orders"
    }
  ]
}
```

### 5.7 `params` — 시트별 추가 변수

시트 안에서만 사용하는 변수를 추가할 수 있습니다.

**XML 예시**

```xml
<sheet name="RegionOrders" db="postgresDB">
  <params>
    <param name="region">Seoul</param>
  </params>
  <query>
    SELECT * FROM Orders WHERE Region = '${region}'
  </query>
</sheet>
```

**JSON 예시**

```json
{
  "sheets": [
    {
      "name": "RegionOrders",
      "db": "postgresDB",
      "params": { "region": "Seoul" },
      "query": "SELECT * FROM Orders WHERE Region = '${region}'"
    }
  ]
}
```

---

## 6. 변수 시스템

### 6.1 쿼리 문자열 치환

`${변수명}` 형태를 찾아 값으로 치환합니다. 대소문자를 구분하지 않습니다.

### 6.2 특수 변수

| 변수 | 설명 | 기본 형식 |
|------|------|-----------|
| `${DATE[:pattern]}` | 현재 UTC 기준 날짜/시간 | `yyyyMMddHHmmss` |
| `${DATE.KST[:pattern]}` | KST(한국) 기준 | `yyyyMMddHHmmss` |
| `${DATE.JST[:pattern]}` | JST(일본) 기준 | `yyyyMMddHHmmss` |
| `${CURRENT_TIMESTAMP[:pattern]}` | 현재 날짜/시간 | `yyyy-MM-dd HH:mm:ss` |
| `${CURRENT_DATE[:pattern]}` | 현재 날짜 | `yyyy-MM-dd` |
| `${CURRENT_TIME[:pattern]}` | 현재 시간 | `HH:mm:ss` |
| `${UNIX_TIMESTAMP}` | 유닉스 타임스탬프(초) | 숫자 |
| `${TODAY}` | 오늘 날짜 | `yyyy-MM-dd` |
| `${GETDATE}` | 현재 날짜/시간 문자열 | `yyyy-MM-dd HH:mm:ss` |

지원하는 타임존: UTC, GMT, KST, JST, CST, SGT, PHT, AEST, ICT, CET, EET, IST, GST, EST, CST_US, MST, PST, AST, AKST, HST, BRT, ART.

### 6.3 목록 변수

변수가 리스트인 경우:

- **SQL 컨텍스트**(`query` 안): `${regionList}` → `Seoul, Busan`
- **비 SQL 컨텍스트**(`output` 경로 등): `Seoul,Busan`

```sql
WHERE Region IN (${regionList})
-- 치환 후: WHERE Region IN ('Seoul', 'Busan')
```

### 6.4 CLI 변수 오버라이드

명령줄에서 `-v` 옵션으로 `vars`를 덮어쓸 수 있습니다.

```bash
java -jar sql2excel-java-1.2.7.jar export -x queries/sample.xml \
  -v startDate=2024-03-01 \
  -v regionList="[Seoul,Daegu]"
```

---

## 7. 동적 시트(Dynamic Sheets)

`dynamic-sheets`는 `dynamicVars`로 가져온 리스트 데이터를 반복(iteration)해 여러 시트를 자동으로 생성합니다.

### 7.1 동작 원리

1. `dynamicVar`로 데이터를 조회하면 `${customerData.CustomerID}`처럼 컬럼별 리스트가 생성됩니다.
2. `dynamic-sheets`의 `for` 속성에 동적 변수명을 지정합니다.
3. 조회된 행 수만큼 시트가 복제되며, 각 행의 컬럼 값이 `params`로 주입됩니다.

### 7.2 `dynamic-sheet` 속성

| 속성 | 설명 |
|------|------|
| `name` | 생성될 시트명 템플릿. `${변수}` 치환 가능. |
| `for` | (필수) 반복할 동적 변수명. `dynamicVar`의 `name`과 일치해야 함. |
| `query` / `queryRef` | 실행할 쿼리. |
| `params` | 기본 `params`. 반복 시 동적 값과 병합됩니다. |
| `db`, `style`, `maxRows`, `date-column-format` | 일반 `sheet`와 동일. |

### 7.3 XML 예시

```xml
<dynamicVar name="customerData" db="postgresDB">
  SELECT DISTINCT CustomerID, CustomerName
  FROM Customers
  WHERE IsActive = TRUE
</dynamicVar>

<dynamic-sheets>
  <dynamic-sheet name="Customer_${CustomerID}" for="customerData" db="postgresDB">
    <query>
      SELECT * FROM Orders WHERE CustomerID = '${CustomerID}'
    </query>
  </dynamic-sheet>
</dynamic-sheets>
```

위 예시는 `customerData`의 행 수만큼 `Customer_1`, `Customer_2`, ... 시트를 생성하고,  
각 시트마다 `CustomerID`와 `CustomerName` 값이 쿼리에 주입됩니다.

### 7.4 JSON 예시

```json
{
  "dynamicVars": [
    {
      "name": "customerData",
      "db": "postgresDB",
      "query": "SELECT DISTINCT CustomerID, CustomerName FROM Customers WHERE IsActive = TRUE"
    }
  ],
  "dynamicSheets": [
    {
      "name": "Customer_${CustomerID}",
      "iterVar": "customerData",
      "db": "postgresDB",
      "query": "SELECT * FROM Orders WHERE CustomerID = '${CustomerID}'"
    }
  ]
}
```

XML에서는 `for` 속성, JSON에서는 `iterVar` 필드를 사용합니다.

---

## 8. Excel 스타일 설정

`config/excel-style.json`에서 스타일 템플릿을 정의합니다.

### 8.1 기본 구조

```json
{
  "default": {
    "header": { ... },
    "body": {
      "default": { ... },
      "number": { ... },
      "date": { ... }
    }
  },
  "modern": {
    "header": { ... },
    "body": { ... }
  }
}
```

- 최상위 key는 스타일 이름입니다.
- `default` 스타일은 **기본 베이스**입니다. 다른 스타일은 `default`와 deep merge되어 속성을 상속합니다.
- `body`는 `default`, `number`, `date` 세 가지 서브 스타일로 구성됩니다.
- `body`를 평탄하게(flat) 작성하면 전체 셀을 `default`로 처리합니다.

### 8.2 `header` / `body.{default|number|date}` 공통 속성

```json
{
  "font": { "name": "Arial", "size": 11, "bold": true, "color": "#000000" },
  "fill": { "color": "43DE63" },
  "alignment": { "horizontal": "center", "vertical": "middle" },
  "border": { "all": { "style": "thin", "color": "27F584" } },
  "numberFormat": { "decimal": 2, "thousands": true }
}
```

#### 8.2.1 `font`

| 속성 | 설명 |
|------|------|
| `name` | 폰트명. 예: `Arial`, `맑은 고딕` |
| `size` | 폰트 크기(pt) |
| `bold` | `true` 또는 `false` |
| `color` | 6자리 RGB hex. 앞에 `#` 유무 상관없음. |

#### 8.2.2 `fill`

| 속성 | 설명 |
|------|------|
| `color` | 6자리 RGB hex. |

#### 8.2.3 `alignment`

| 속성 | 설명 |
|------|------|
| `horizontal` | `left`, `center`, `right` |
| `vertical` | `top`, `middle`, `bottom` |

값을 지정하지 않으면 다음 기본값이 적용됩니다.

| 서브 스타일 | 기본 horizontal | 기본 vertical |
|-------------|-----------------|---------------|
| `body.default` | `left` | `middle` |
| `body.number` | `right` | `middle` |
| `body.date` | `center` | `middle` |
| `header` | `center` | `middle` |

#### 8.2.4 `border`

```json
"border": {
  "all": { "style": "thin", "color": "000000" }
}
```

| `style` | 설명 |
|---------|------|
| `thin`, `medium`, `thick`, `dashed`, `dotted`, `double`, `hair` | 선 스타일 |

#### 8.2.5 `numberFormat` (number 서브 스타일)

| 속성 | 설명 |
|------|------|
| `decimal` | 소수점 자릿수. `0`이면 정수. |
| `thousands` | `true`면 천 단위 쉼표. `false`면 생략. |

예시:

```json
{
  "numberFormat": { "decimal": 2, "thousands": true }
}
```

→ `#,##0.00`

### 8.3 스타일 상속과 오버라이드

- `default` 스타일에 정의된 `header`/`body`가 베이스가 됩니다.
- `modern`, `dark` 등 다른 스타일은 필요한 속성만 다시 정의하면 나머지는 `default`에서 상속합니다.
- `body.number`/`date`가 비어있거나 일부만 정의되어 있으면 `default.body.number`/`date`와 병합됩니다.

```json
{
  "default": { ... },
  "business": {
    "header": {
      "font": { "name": "Arial", "size": 12, "bold": true, "color": "FFFFFF" },
      "fill": { "color": "203864" }
    },
    "body": {
      "number": {
        "numberFormat": { "decimal": 0, "thousands": false }
      }
    }
  }
}
```

### 8.4 날짜 셀 서식

`body.date`는 정렬/배경색/폰트/테두리를 설정합니다.  
**날짜 숫자 형식**은 `excel.date-column-format` 또는 `sheet.date-column-format`로 지정합니다.

```json
{
  "excel": {
    "style": "business",
    "date-column-format": "yyyy/MM/dd hh:mm:ss"
  }
}
```

`date-column-format`이 없으면 날짜가 Excel 내부 시리얼 숫자 형태로 보일 수 있습니다.

### 8.5 시트별 스타일 오버라이드

`excel`에 `style`을 지정하면 전체 시트에 적용되고, `sheet`에 `style`을 지정하면 해당 시트만 오버라이드합니다.

```xml
<excel style="modern">
  <sheet name="Summary" style="business">
</excel>
```

### 8.6 사용자 정의 스타일 추가

`excel-style.json`에 새 key를 추가하면 즉시 사용할 수 있습니다. `default`를 베이스로 삼아 필요한 속성만 오버라이드하세요.

```json
{
  "myStyle": {
    "header": {
      "fill": { "color": "FF5733" },
      "font": { "color": "FFFFFF", "bold": true }
    },
    "body": {
      "number": {
        "numberFormat": { "decimal": 1, "thousands": true }
      }
    }
  }
}
```

사용:

```xml
<excel style="myStyle" .../>
```

## 9. 출력 형식

`excel.output` 확장자에 따라 출력 형식이 결정됩니다.

| 형식 | 설명 |
|------|------|
| `.xlsx` | **목차(TOC) 시트 자동 생성**, 스타일, 서식, 다중 시트, 하이퍼링크 지원 |
| `.csv/.txt` | 스타일 없이 데이터만 쉼표로 구분 |
| `.sql` | 시트당 `.sql` 파일. 각 행의 첫 번째 컬럼 값을 SQL 문으로 출력하며, 끝에 `;`를 자동 추가합니다. |
| `.json` | JSON 객체 배열 형태 |
| `.xml` | XML 2종류를 생성합니다. `<sheets>` 기반의 **속성(attribute) 형식**과 **요소(element) 형식** 파일. 스타일 정보는 없습니다. |

**`.sql` 출력 사용법**

`.sql` 출력은 주로 쿼리 결과의 첫 번째 컬럼에 실행할 SQL 문을 담아 배치(batch) SQL 스크립트를 만드는 용도입니다.

```xml
<excel output="output/migration.sql" db="postgresDB">
  ...
</excel>

<sheet name="DDL" db="postgresDB">
  SELECT 'ALTER TABLE ' || table_name || ' ADD COLUMN updated_at timestamp' AS ddl
  FROM information_schema.tables
  WHERE table_schema = 'public'
</sheet>
```

- 시트가 1개면 `migration.sql`에 모두 기록됩니다.
- 시트가 2개 이상이면 `migration_시트명.sql` 형태로 파일이 분리됩니다.
- 숨김 컬럼(`hiddenColumns`)은 제외되며, 첫 번째 **보이는** 컬럼만 SQL 문으로 취급됩니다.

**`.xml` 출력 상세**

`.xml` 확장자를 지정하면 다음 2개의 XML 파일이 생성됩니다.

| 파일 | 형식 | 설명 |
|------|------|------|
| `output.xml` | **속성(attribute) 기반** | `<col name="컬럼명" type="데이터타입">값</col>` |
| `output_element.xml` | **요소(element) 기반** | `<컬럼명>값</컬럼명>` |

예시:

```xml
<!-- output.xml -->
<sheets>
  <sheet name="Orders">
    <row>
      <col name="OrderID" type="number">1001</col>
      <col name="OrderDate" type="date">2024-01-15 10:20:30</col>
    </row>
  </sheet>
</sheets>

<!-- output_element.xml -->
<sheets>
  <sheet name="Orders">
    <row>
      <OrderID>1001</OrderID>
      <OrderDate>2024-01-15 10:20:30</OrderDate>
    </row>
  </sheet>
</sheets>
```

- `type` 속성은 `string`, `number`, `boolean`, `date`, `null` 중 하나입니다.
- 요소 기반 XML에서 컬럼명은 유효한 XML 요소명으로 변환됩니다. (`xml`로 시작하면 `_` 접두사, 특수문자는 `_`로 치환, 중복 뒤에 `_2` 등 추가)
- 숨김 컬럼은 두 파일 모두에서 제외됩니다.

### 9.1 텍스트 기반 출력의 시트별 파일 분리

`.csv`, `.txt`, `.sql` 확장자로 출력할 때 시트가 **2개 이상**이면 출력 파일을 시트별로 분리합니다.

| 확장자 | 1개 시트 | 2개 이상 시트 |
|--------|----------|---------------|
| `.csv` | `output.csv` | `output_Order.csv`, `output_Summary.csv`, ... |
| `.txt` | `output.txt` | `output_Order.txt`, `output_Summary.txt`, ... |
| `.sql` | `output.sql` | `output_Order.sql`, `output_Summary.sql`, ... |

- 파일명은 `출력파일_시트명.확장자` 형태입니다.
- 시트명에 경로/특수문자가 들어가면 `_`로 치환됩니다.
- `.json`과 `.xml`은 출력 파일 1개(또는 `.xml`은 2개)에 모든 시트를 담습니다.

### 9.2 목차(TOC) 시트 자동 생성

`.xlsx` 출력 시 가장 앞에 **목차** 시트가 자동으로 추가됩니다.  
CSV/TXT/SQL/JSON/XML 등 텍스트 기반 출력에는 목차가 생성되지 않습니다.

| 컬럼 | 내용 |
|------|------|
| `시트명` | 생성된 시트 이름 |
| `조회건수` | 해당 시트의 조회 결과 행 수 |
| `사용된 SQL문` | 실행된 SQL 문 |

목차 시트는 `excel`에 지정된 `style`의 `header`/`body` 스타일을 적용받으며,  
`사용된 SQL문` 컬럼은 줄바꿈(`wrap`) 처리됩니다.

---

## 10. 명령어 참조

### 10.1 `export` — 쿼리 실행 및 내보내기

```bash
java -jar sql2excel-java-1.2.7.jar export -x queries/sample.xml
java -jar sql2excel-java-1.2.7.jar export -q queries/sample.json -v key=value
```

| 옵션 | 설명 |
|------|------|
| `-c, --config` | DB 설정 파일. 기본값: `config/dbinfo.json` |
| `-q, --query` | JSON 쿼리 파일 경로 |
| `-x, --xml` | XML 쿼리 파일 경로 |
| `-v, --var` | CLI 변수 (반복 가능) |

`-q`와 `-x` 중 하나는 반드시 지정해야 합니다.

### 10.2 `export-styles` — 모든 스타일별로 각각 내보내기

`excel-style.json`에 정의된 각 스타일로 개별 파일을 생성합니다.

```bash
java -jar sql2excel-java-1.2.7.jar export-styles -x queries/sample.xml
```

출력 파일명은 `<원본명>-<스타일명>.xlsx` 형태로 생성됩니다.  
예: `output/result-modern.xlsx`, `output/result-business.xlsx`

### 10.3 `export-style-samples` — 스타일 샘플 통합 파일

각 스타일이 적용된 샘플 데이터를 한 워크북에 모아 만듭니다.

```bash
java -jar sql2excel-java-1.2.7.jar export-style-samples
```

| 옵션 | 설명 |
|------|------|
| `-s, --styles` | 스타일 파일. 기본값: `config/excel-style.json` |
| `-o, --output` | 출력 파일. 기본값: `output/style-samples.xlsx` |

### 10.4 `list-dbs` — DB 연결 테스트

```bash
java -jar sql2excel-java-1.2.7.jar list-dbs -c config/dbinfo.json
```

### 10.5 `validate` — 쿼리 정의 파일 검증

쿼리 정의 파일(XML/JSON)과 DB 설정 파일을 읽어 구문 오류나 필수 항목 누락을 미리 확인합니다.  
실제 DB 연결이나 SQL 실행은 하지 않으므로, 실행 전 파일만으로 빠르게 검사할 수 있습니다.

```bash
java -jar sql2excel-java-1.2.7.jar validate -x queries/sample.xml
java -jar sql2excel-java-1.2.7.jar validate -q queries/sample.json
java -jar sql2excel-java-1.2.7.jar validate -x queries/sample.xml -c config/dbinfo.json
```

| 옵션 | 설명 |
|------|------|
| `-q, --query` | JSON 쿼리 파일 경로 |
| `-x, --xml` | XML 쿼리 파일 경로 |
| `-c, --config` | DB 설정 파일. 기본값: `config/dbinfo.json` |

`-q`와 `-x` 중 하나는 반드시 지정해야 합니다.

**검증 항목**

1. **쿼리 파일 파싱**
   - XML/JSON 형식이 올바른지
   - XML의 CDATA, JSON의 인코딩 문제 등
   - 파싱 실패 시 즉시 오류 메시지 출력

2. **DB 설정 로드**
   - `dbinfo.json`에서 별칭(alias)별 DB 설정을 정상적으로 읽는지
   - 지정된 `--config` 파일이 존재하고 읽을 수 있는지

3. **시트 존재 여부**
   - `<sheets>`/`<dynamic-sheets>`(또는 JSON의 `sheets`/`dynamicSheets`)에 하나 이상의 시트가 정의되어 있는지
   - `use="false"` 또는 `use: false`인 시트는 검증 대상에서 제외

4. **시트명 검사 (`use=true`인 시트만)**
   - 시트명이 비어 있으면 **오류**
   - 시트명이 31자를 초과하면 **경고** (Excel 시트명 제한)
   - 시트명에 특수문자가 있으면 Excel 저장 시 치환될 수 있음

5. **쿼리 내용 검사 (`use=true`인 시트만)**
   - 시트에 직접 쿼리가 있거나, `queryRef`로 참조한 `queryDefs`에 쿼리가 있는지
   - 쿼리가 비어 있으면 **오류**

6. **DB 연결 설정 연결 검사 (`use=true`인 시트만)**
   - 시트의 `db` → 없으면 `excel`의 `db` → 없으면 오류
   - 위 별칭이 `dbinfo.json`에 등록되어 있는지
   - 등록되지 않은 DB를 참조하면 **오류**

**결과 코드**

| 반환 값 | 의미 |
|--------|------|
| `0` | 검증 통과 (경고는 있을 수 있음) |
| `1` | 검증 실패 또는 예외 발생 |

**예시 출력**

```text
Validation passed.
```

```text
Error: sheet 'Summary' has no query.
Error: database 'oracleDB' for sheet 'Orders' not found.
Validation failed.
```

```text
Warning: sheet name too long (max 31): MonthlySalesReportByRegion
Validation passed.
```

**주의**

- `validate`는 파일 구조와 설정 연결만 검사합니다.
- SQL 문법 오류, 실제 테이블/컬럼 존재 여부, DB 네트워크 연결은 검사하지 않습니다.
- `queryRef`를 사용하는 경우 `queryDefs`에 정의된 쿼리가 올바르게 연결되는지까지 확인합니다.

---

## 11. 인터랙티브 메뉴

인수 없이 실행하면 메뉴 화면이 열립니다.

```bash
java -jar sql2excel-java-1.2.7.jar
```

```text
1. 쿼리 정의 파일 검증
2. 데이터베이스 연결 테스트
3. 엑셀 파일 생성 (XML)
4. 엑셀 파일 생성 (JSON)
5. 모든 스타일로 엑셀 파일 생성
6. 모든 스타일 샘플 엑셀 파일 생성
7. 도움말
0. 종료
```

번호를 선택하면 `queries/` 폴더의 파일 목록을 보여주고 실행합니다.

---

## 12. DBMS별 자동 처리

### 12.1 `GETDATE()` 변환

쿼리 문자열에 `GETDATE()` (함수 호출 형태)가 포함되어 있으면, 대상 DBMS에 맞는 함수로 자동 변환됩니다.

| DBMS | `GETDATE()` 변환 결과 |
|------|----------------------|
| PostgreSQL | `NOW()` |
| MySQL, MariaDB | `NOW()` |
| Oracle, Tibero | `SYSTIMESTAMP` |
| SQLite | `datetime('now')` |
| MSSQL | `GETDATE()` |

`${GETDATE}` 변수는 현재 날짜/시간 **문자열**을 치환합니다. DB 함수를 사용하려면 `GETDATE()`를 직접 쿼리에 넣으세요.

### 12.2 `maxRows` 자동 적용

`maxRows`가 설정되어 있고 쿼리에 `LIMIT`/`TOP`/`FETCH FIRST`/`ROWNUM`이 없으면 DBMS별로 자동 추가됩니다.

- MSSQL: `SELECT TOP (N) ...`
- Oracle/Tibero: `FETCH FIRST N ROWS ONLY`
- MySQL/MariaDB/PostgreSQL/SQLite: `LIMIT N`

---

## 13. 문제 해결

### 13.1 “Error: no database specified for sheet ...”

시트에 `db`가 없고 `excel.db`도 없습니다. 둘 중 하나를 추가하세요.

### 13.2 “database not found: ...”

`dbinfo.json`에 해당 별칭이 없거나, `dbinfo.json` 경로가 잘못되었습니다.

### 13.3 스타일이 적용되지 않는 것처럼 보임

- `excel-style.json` 문법이 올바른지 확인 (`config/excel-style.json` 위치)
- 스타일 이름이 `excel-style.json`에 존재하는지 확인 (소문자/대문자 무관)
- `date` 셀이 Excel 시리얼 숫자로 보이면 `date-column-format`을 지정하세요.
- 셀 값이 DB에서 **문자열**로 반환되면 number/date 스타일이 아닌 default로 처리됩니다.

### 13.4 JDBC 드라이버 오류

```
java.lang.ClassNotFoundException: org.postgresql.Driver
```

`lib/`에 해당 JAR이 있는지, `dbinfo.json`의 `jar` 경로가 맞는지 확인하세요.

### 13.5 변수 치환 안 됨

변수명이 `vars` 또는 CLI `-v`에 정확히 등록되었는지, `${변수명}` 형식이 맞는지 확인하세요.

### 13.6 `GETDATE()` 변환 안 됨

- `GETDATE()` 함수 호출 형태(괄호 포함)를 사용하면 DBMS별 함수로 자동 변환됩니다.
- `${GETDATE}`는 현재 날짜/시간 **문자열**을 치환합니다.

### 13.7 `dynamic-sheet`가 생성되지 않음

- `dynamicVar`가 먼저 정의되어 있어야 합니다.
- `dynamic-sheet`의 `for`(XML) / `iterVar`(JSON) 값이 `dynamicVar`의 `name`과 일치해야 합니다.
- 반복할 데이터가 비어있지 않은지 확인하세요.

---

## 14. 예시 모음

### 14.1 기본 명령

```bash
# DB 연결 테스트
java -jar sql2excel-java-1.2.7.jar list-dbs

# 쿼리 파일 검증
java -jar sql2excel-java-1.2.7.jar validate -x queries/sample.xml

# 기본 엑셀 생성
java -jar sql2excel-java-1.2.7.jar export -x queries/sample.xml

# 변수 오버라이드
java -jar sql2excel-java-1.2.7.jar export -x queries/sample.xml \
  -v startDate=2024-01-01 \
  -v endDate=2024-06-30

# 모든 스타일로 생성
java -jar sql2excel-java-1.2.7.jar export-styles -x queries/sample.xml

# 스타일 샘플 통합 파일
java -jar sql2excel-java-1.2.7.jar export-style-samples
```

### 14.2 종합 XML 예시

```xml
<queries>
  <excel db="postgresDB"
         output="output/orders_${DATE:yyyyMMddHHmmss}.xlsx"
         maxRows="1000"
         style="modern"
         date-column-format="yyyy/MM/dd hh:mm:ss">
  </excel>

  <vars>
    <var name="startDate">2024-01-01</var>
    <var name="endDate">2024-06-30</var>
    <var name="regionList">["Seoul", "Busan"]</var>
  </vars>

  <dynamicVar name="customerData" db="postgresDB">
    SELECT DISTINCT CustomerID, CustomerName
    FROM Customers
    WHERE IsActive = TRUE
  </dynamicVar>

  <queryDefs>
    <queryDef id="orders_base">
      <![CDATA[
        SELECT *
        FROM Orders
        WHERE OrderDate >= '${startDate}'::date
          AND OrderDate <= '${endDate}'::date
          AND Region IN (${regionList})
      ]]>
    </queryDef>
  </queryDefs>

  <sheets>
    <sheet name="All Orders" queryRef="orders_base" db="postgresDB">
    </sheet>

    <sheet name="Summary" db="postgresDB" style="business" maxRows="100">
      <![CDATA[
        SELECT Region, COUNT(*) AS OrderCount, SUM(TotalAmount) AS TotalAmount
        FROM Orders
        WHERE OrderDate >= '${startDate}'::date
          AND OrderDate <= '${endDate}'::date
        GROUP BY Region
      ]]>
    </sheet>
  </sheets>

  <dynamic-sheets>
    <dynamic-sheet name="Customer_${CustomerID}" for="customerData" db="postgresDB" queryRef="orders_base">
    </dynamic-sheet>
  </dynamic-sheets>
</queries>
```

### 14.3 종합 JSON 예시

```json
{
  "excel": {
    "db": "postgresDB",
    "output": "output/orders_${DATE:yyyyMMddHHmmss}.xlsx",
    "maxRows": 1000,
    "style": "modern",
    "date-column-format": "yyyy/MM/dd hh:mm:ss"
  },
  "vars": {
    "startDate": "2024-01-01",
    "endDate": "2024-06-30",
    "regionList": ["Seoul", "Busan"]
  },
  "queryDefs": {
    "orders_base": "SELECT * FROM Orders WHERE OrderDate >= '${startDate}' AND OrderDate <= '${endDate}' AND Region IN (${regionList})"
  },
  "sheets": [
    {
      "name": "All Orders",
      "queryRef": "orders_base",
      "db": "postgresDB"
    },
    {
      "name": "Summary",
      "db": "postgresDB",
      "style": "business",
      "maxRows": 100,
      "query": "SELECT Region, COUNT(*) AS OrderCount, SUM(TotalAmount) AS TotalAmount FROM Orders GROUP BY Region"
    }
  ]
}
```
