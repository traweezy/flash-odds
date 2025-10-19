@ECHO OFF
SETLOCAL

SET BASEDIR=%~dp0
SET WRAPPER_JAR=%BASEDIR%.mvn\wrapper\maven-wrapper.jar
SET WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

IF NOT EXIST "%WRAPPER_JAR%" (
  ECHO Maven wrapper JAR missing: %WRAPPER_JAR%
  EXIT /B 1
)

SET JAVA_CMD=%JAVA_HOME%\bin\java.exe
IF NOT EXIST "%JAVA_CMD%" (
  FOR %%i IN (java.exe) DO SET JAVA_CMD=%%~$PATH:i
)

IF "%JAVA_CMD%"=="" (
  ECHO Java executable not found. Set JAVA_HOME or ensure java is on PATH.
  EXIT /B 1
)

"%JAVA_CMD%" -Dmaven.multiModuleProjectDirectory="%BASEDIR%" -classpath "%WRAPPER_JAR%" %WRAPPER_LAUNCHER% %*
