# sql2excel-java

SQL 쿼리 결과를 엑셀 파일로 내보내는 Java 기반 CLI 도구입니다.
`sql2excel` Node.js 버전의 핵심 기능을 Java/Maven으로 재구현한 프로젝트입니다.

## 기능

- JSON/XML 형식의 쿼리 정의 파일 지원
- 다중 데이터베이스 연결 (MSSQL, MySQL/MariaDB, PostgreSQL, SQLite, Oracle, Tibero)
- JDBC 기반 연결 (ODBC 포함하여 `driverClass`/`driver` 설정으로 커스터마이징 가능)
- 변수 치환 및 `${DATE.KST:yyyyMMddHHmmss}` 형식의 날짜 변수
- `IN (...)` 리스트 변수 자동 변환
- DB별 `GETDATE()` → `NOW()`, `SYSTIMESTAMP`, `datetime('now')` 등 자동 변환
- LIMIT/TOP/FETCH FIRST 자동 추가
- Apache POI 기반 `.xlsx` 다중 시트 엑셀 출력
- XML 파일 포맷팅 (`format-xml`) 지원

## 요구사항

- JDK 11 이상
- Maven 3.8 이상
- 대상 데이터베이스 JDBC 드라이버 JAR (사용할 DB에 맞는 JAR를 `lib/` 디렉토리에 넣어야 합니다. SQLite/PostgreSQL/MariaDB 드라이버는 기본적으로 `lib/`에 포함되어 있습니다.)

## 빌드

```bash
mvn package
```

빌드 후 `target/sql2excel-java-1.0.0.jar` 파일이 생성됩니다.

## 실행

### 인터랙티브 메뉴

인수 없이 실행하면 `sql2excel-nodejs`와 동일한 메뉴 화면이 출력됩니다.

```bash
java -jar target/sql2excel-java-1.0.0.jar
```

```text
SQL2Excel v1.0.0

메뉴 선택

1. 쿼리 정의 파일 검증
2. 데이터베이스 연결 테스트

3. 엑셀 파일 생성 (XML)
4. 엑셀 파일 생성 (JSON)
5. 모든 스타일로 엑셀 파일 생성
6. 모든 스타일 샘플 엑셀 파일 생성
7. XML 파일 포맷팅
8. 도움말
0. 종료
```

### 명령어 직접 실행

```bash
# 실행 가능한 JAR로 직접 실행
java -jar target/sql2excel-java-1.0.0.jar export -q src/main/resources/queries/sample-queries.json

# Maven으로 실행
mvn compile exec:java -Dexec.mainClass="com.sql2excel.Sql2ExcelApplication" -Dexec.args="export -q src/main/resources/queries/sample-queries.json"

# DB 연결 테스트
java -jar target/sql2excel-java-1.0.0.jar list-dbs -c src/main/resources/config/dbinfo.json

# 쿼리 정의 파일 검증
java -jar target/sql2excel-java-1.0.0.jar validate -q src/main/resources/queries/sample-queries.json

# XML 파일 포맷팅
java -jar target/sql2excel-java-1.0.0.jar format-xml -i queries/sample.xml -o output/formatted-sample.xml
```

## 설정

### config/dbinfo.json

```json
{
  "sampleDB": {
    "type": "mssql",
    "user": "sample",
    "password": "sample1234!",
    "server": "localhost",
    "database": "SampleDB",
    "port": 1433,
    "options": {
      "encrypt": false,
      "trustServerCertificate": true,
      "connectionTimeout": 30000
    }
  }
}
```

- `type`: 데이터베이스 벤더별 대표 이름(`DatabaseType`)입니다. `mssql`, `mysql`, `mariadb`, `postgresql`, `sqlite`, `oracle`, `tibero`를 사용합니다.
- `driverClass` (필수): 사용할 JDBC 드라이버 클래스명. `dbinfo.json`에서 직접 정의합니다.
- `jar` (필수): JDBC 드라이버 JAR 파일 경로. 예: `lib/sqlite-jdbc-3.53.2.1.jar`. SQLite/PostgreSQL/MariaDB 드라이버는 기본적으로 `lib/`에 포함되어 있고, MSSQL/Oracle/Tibero 등 그 외 드라이버는 직접 `lib/`에 추가해야 합니다.
- `driver` (선택): Node.js 버전과의 호환성을 위해 유지. `.`이 포함된 클래스명이면 `driverClass`로 사용

### queries/sample-queries.json

```json
{
  "excel": {
    "db": "sampleDB",
    "output": "output/sample_${DATE.KST:yyyyMMddHHmmss}.xlsx",
    "maxRows": 1000
  },
  "vars": {
    "startDate": "2024-01-01",
    "endDate": "2024-06-30",
    "regionList": ["Seoul", "Busan"]
  },
  "sheets": [
    {
      "name": "Orders",
      "use": true,
      "db": "sampleDB",
      "query": "SELECT * FROM Orders WHERE OrderDate >= '${startDate}' AND OrderDate <= '${endDate}' AND Region IN (${regionList})"
    }
  ]
}
```

## 주의사항

- Tibero, MSSQL, Oracle 등 나머지 DB 드라이버는 Maven Central에서 자동으로 제공되지 않거나 라이선스 문제로 포함되어 있지 않으므로, 사용자가 직접 해당 JDBC JAR 파일을 `lib/` 디렉토리에 넣고 `dbinfo.json`의 `jar` 항목에 경로를 지정해야 합니다.
- SQLite 사용 시 `database` 필드에 파일 경로를 입력합니다. (`:memory:`로 인메모리 사용 가능)
