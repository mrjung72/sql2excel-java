#!/bin/sh
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$BASE_DIR" || exit 1

JAR=$(ls -1 "$BASE_DIR"/*.jar 2>/dev/null | head -1)
if [ -z "$JAR" ]; then
    JAR=$(ls -1 "$BASE_DIR/target"/sql2excel-java-*.jar 2>/dev/null | head -1)
fi

if [ -z "$JAR" ]; then
    echo "No jar file found." >&2
    exit 1
fi

if command -v cygpath >/dev/null 2>&1; then
    JAR=$(cygpath -w "$JAR")
fi

# Default work queries (relative to queries/)
DEFAULT_WORK="
dynamic-sheet-sample.xml
mariadb-test.xml
postgresql-test.xml
queries-with-dynamic-variables.xml
"

# Use command-line arguments if provided, otherwise the default list
if [ $# -gt 0 ]; then
    WORK_QUERY_FILES="$@"
else
    WORK_QUERY_FILES=$DEFAULT_WORK
fi

CURR_YYYYMMDD=$(date +%Y%m%d)
LOG_DIR="log/${CURR_YYYYMMDD}"
mkdir -p "$LOG_DIR" output

echo "[$(date +%Y-%m-%d\ %H:%M:%S)] Start work ..."
OVERALL_EXIT=0

for work_query_file in $WORK_QUERY_FILES; do
    # Resolve the query file: first try queries/, then as-is
    if [ -f "queries/${work_query_file}" ]; then
        QUERY_PATH="queries/${work_query_file}"
    elif [ -f "${work_query_file}" ]; then
        QUERY_PATH="${work_query_file}"
    else
        echo "[$(date +%Y-%m-%d\ %H:%M:%S)] Query file not found: ${work_query_file}" >&2
        OVERALL_EXIT=1
        continue
    fi

    if command -v cygpath >/dev/null 2>&1; then
        QUERY_PATH=$(cygpath -w "$QUERY_PATH")
    fi

    # Derive log file name from query file name (without extension)
    wq_file_name=${work_query_file##*/}
    wq_file_name=${wq_file_name%.*}
    RUN_TIME=$(date +%Y%m%d%H%M%S)
    LOG_FILE="${LOG_DIR}/${wq_file_name}-${RUN_TIME}.log"

    echo "[$(date +%Y-%m-%d\ %H:%M:%S)] Running ${work_query_file} ..."
    java -Dlog4j2.configurationFile=config/log4j2.xml -jar "$JAR" export -x "$QUERY_PATH" > "$LOG_FILE" 2>&1
    EXIT_CODE=$?

    if [ $EXIT_CODE -eq 0 ]; then
        echo "[$(date +%Y-%m-%d\ %H:%M:%S)] Success: ${work_query_file}"
    else
        echo "[$(date +%Y-%m-%d\ %H:%M:%S)] Failed (exit $EXIT_CODE): ${work_query_file}" >&2
        OVERALL_EXIT=$EXIT_CODE
    fi
done

echo "[$(date +%Y-%m-%d\ %H:%M:%S)] Finish work (overall exit ${OVERALL_EXIT}) ..."
exit $OVERALL_EXIT
