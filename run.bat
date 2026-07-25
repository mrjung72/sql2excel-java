@echo off
for %%f in ("%~dp0\*.jar") do (
    java -jar "%%f" %*
    exit /b %errorlevel%
)
echo No jar file found.
exit /b 1
