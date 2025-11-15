@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@echo off
setlocal

set ERROR_CODE=0

set MAVEN_HOME=
if not "%MAVEN_HOME%"=="" goto :findJavaFromJavaHome

set JAVACMD=
if exist "%JAVA_HOME%\bin\java.exe" set "JAVACMD=%JAVA_HOME%\bin\java.exe"
if exist "%JAVA_HOME%\bin\javaw.exe" set "JAVACMD=%JAVA_HOME%\bin\javaw.exe"

if not "%JAVACMD%"=="" goto :endInit

for %%i in (java.exe) do set "JAVACMD=%%~$PATH:i"

:endInit
if "%JAVACMD%"=="" (
  echo The JAVA_HOME environment variable is not defined correctly or java is not on PATH.
  echo Please set the JAVA_HOME variable in your environment to match the
  echo location of your Java installation.
  exit /B 1
)

set WRAPPER_JAR="%~dp0.mvn\wrapper\maven-wrapper.jar"
set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

if exist %WRAPPER_JAR% goto :runWrapper

set DOWNLOAD_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"

echo Couldn't find %WRAPPER_JAR%, downloading it ...

set WRAPPER_DIR="%~dp0.mvn\wrapper"
if not exist %WRAPPER_DIR% mkdir %WRAPPER_DIR%

powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest %DOWNLOAD_URL% -OutFile %WRAPPER_JAR%" || (
  echo Failed to download Maven Wrapper JAR from %DOWNLOAD_URL%
  exit /B 1
)

:runWrapper
"%JAVACMD%" %MAVEN_OPTS% -Dmaven.multiModuleProjectDirectory="%CD%" -classpath %WRAPPER_JAR% %WRAPPER_LAUNCHER% %MAVEN_CONFIG% %*
set ERROR_CODE=%ERRORLEVEL%

:end
endlocal & set EXIT_CODE=%ERROR_CODE%
exit /B %EXIT_CODE%
