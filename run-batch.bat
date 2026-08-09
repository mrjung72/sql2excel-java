@echo off
setlocal enabledelayedexpansion

set "BASE_DIR=%~dp0"
cd /d "%BASE_DIR%"

:: Find JAR in script directory or target
set "JAR="
for %%f in ("%BASE_DIR%*.jar") do (
    if not defined JAR set "JAR=%%f"
)
if not defined JAR (
    if exist "%BASE_DIR%target\sql2excel-java-*.jar" (
        for %%f in ("%BASE_DIR%target\sql2excel-java-*.jar") do (
            if not defined JAR set "JAR=%%f"
        )
    )
)
if not defined JAR (
    echo No jar file found. >&2
    exit /b 1
)

:: Parse arguments: -c ^|--category ^<category^> or list of query files
set "CATEGORY="
set "WORK_QUERY_FILES="

if "%~1"=="" (
    echo Usage: %~nx0 [-c ^|--category ^<category^>] [query1 query2 ...] >&2
    exit /b 1
)

if /i "%~1"=="-c" (
    set "CATEGORY=%~2"
    if "!CATEGORY!"=="" (
        echo Category not specified. >&2
        exit /b 1
    )
    if not exist "queries\!CATEGORY!" (
        echo Category not found: !CATEGORY! >&2
        exit /b 1
    )
    for /f "delims=" %%f in ('dir /b /o:n "queries\!CATEGORY!\*.xml"') do (
        if defined WORK_QUERY_FILES (
            set "WORK_QUERY_FILES=!WORK_QUERY_FILES! !CATEGORY!\%%f"
        ) else (
            set "WORK_QUERY_FILES=!CATEGORY!\%%f"
        )
    )
    if "!WORK_QUERY_FILES!"=="" (
        echo No query files in category !CATEGORY!. >&2
        exit /b 1
    )
) else if /i "%~1"=="--category" (
    set "CATEGORY=%~2"
    if "!CATEGORY!"=="" (
        echo Category not specified. >&2
        exit /b 1
    )
    if not exist "queries\!CATEGORY!" (
        echo Category not found: !CATEGORY! >&2
        exit /b 1
    )
    for /f "delims=" %%f in ('dir /b /o:n "queries\!CATEGORY!\*.xml"') do (
        if defined WORK_QUERY_FILES (
            set "WORK_QUERY_FILES=!WORK_QUERY_FILES! !CATEGORY!\%%f"
        ) else (
            set "WORK_QUERY_FILES=!CATEGORY!\%%f"
        )
    )
    if "!WORK_QUERY_FILES!"=="" (
        echo No query files in category !CATEGORY!. >&2
        exit /b 1
    )
) else (
    set "WORK_QUERY_FILES=%*"
)

:: Get current timestamp as YYYYMMDDHHMMSS
set "DT="
set "TMP_TS=%BASE_DIR%.dt.tmp"
powershell -Command "Get-Date -Format 'yyyyMMddHHmmss'" > "%TMP_TS%" 2>nul
set /p DT=<"%TMP_TS%"
del "%TMP_TS%" 2>nul

if "%DT%"=="" (
    echo Failed to get current timestamp. >&2
    exit /b 1
)

set "CURR_YYYYMMDD=%DT:~0,8%"
set "LOG_DIR=log\%CURR_YYYYMMDD%"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"
if not exist "output" mkdir "output"

:: Helper timestamp for messages: YYYY-MM-DD HH:MM:SS
set "TS=%DT:~0,4%-%DT:~4,2%-%DT:~6,2% %DT:~8,2%:%DT:~10,2%:%DT:~12,2%"

set "OVERALL_EXIT=0"
set "CNT=0"

for %%q in (%WORK_QUERY_FILES%) do (
    call :run_query "%%q"
)

echo [%TS%] Finish work (overall exit %OVERALL_EXIT%) ...
exit /b %OVERALL_EXIT%

:query_failed
echo [%TS%] Failed (exit %errorlevel%): %QNAME% >&2
set "OVERALL_EXIT=%errorlevel%"
exit /b %errorlevel%

:run_query
set "QNAME=%~1"
set "QUERY_PATH="
if exist "queries\%QNAME%" (
    set "QUERY_PATH=queries\%QNAME%"
) else if exist "%QNAME%" (
    set "QUERY_PATH=%QNAME%"
)

if "!QUERY_PATH!"=="" (
    echo [%TS%] Query file not found: %QNAME% >&2
    set "OVERALL_EXIT=1"
    exit /b 1
)

set "WQ_FILE_NAME=%~n1"
set /a CNT+=1
set "LOG_FILE=%LOG_DIR%\%WQ_FILE_NAME%-%DT%-%CNT%.log"

echo [%TS%] Running %QNAME% ...
java -Dlog4j2.configurationFile=config/log4j2.xml -jar "%JAR%" export -x "!QUERY_PATH!" > "!LOG_FILE!" 2>&1
if !errorlevel! neq 0 goto :query_failed
echo [%TS%] Success: %QNAME%
exit /b 0
