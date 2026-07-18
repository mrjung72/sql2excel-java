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

## 요구사항

- JDK 11 이상
- Maven 3.8 이상
- 대상 데이터베이스 JDBC 드라이버 (`pom.xml`에 주요 드라이버는 등록되어 있음)

## 빌드

```bash
mvn package
```

빌드 후 `target/sql2excel-java-1.0.0.jar` 파일이 생성됩니다.

## 실행

```bash
# 실행 가능한 JAR로 직접 실행
java -jar target/sql2excel-java-1.0.0.jar export -q src/main/resources/queries/sample-queries.json

# Maven으로 실행
mvn compile exec:java -Dexec.mainClass="com.sql2excel.Sql2ExcelApplication" -Dexec.args="export -q src/main/resources/queries/sample-queries.json"

# DB 연결 테스트
java -jar target/sql2excel-java-1.0.0.jar list-dbs -c src/main/resources/config/dbinfo.json

# 쿼리 정의 파일 검증
java -jar target/sql2excel-java-1.0.0.jar validate -q src/main/resources/queries/sample-queries.json
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

- `type`: `mssql`, `mysql`, `mariadb`, `postgresql`, `sqlite`, `oracle`, `tibero`
- `driverClass` (선택): 사용할 JDBC 드라이버 클래스명. 생략하면 `type`에 따른 기본 클래스 사용
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

- Tibero JDBC 드라이버는 Maven Central에 없으므로, Tibero 설치 경로의 `tibero6-jdbc.jar`를 로컬 Maven 저장소에 설치하거나 `pom.xml`의 `tibero-jdbc` dependency를 사용합니다.
- SQLite 사용 시 `database` 필드에 파일 경로를 입력합니다. (`:memory:`로 인메모리 사용 가능)
