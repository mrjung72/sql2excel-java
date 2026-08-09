@echo off
for %%f in ("%~dp0\*.jar") do (
    java -Dlog4j2.configurationFile=config/log4j2.xml -jar "%%f" %*
    exit /b %errorlevel%
)
echo No jar file found.
exit /b 1
