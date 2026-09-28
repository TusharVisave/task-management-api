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

@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch script
@REM ----------------------------------------------------------------------------

@IF "%DEBUG%" == "" @ECHO OFF
@SETLOCAL

SET ERROR_CODE=0

@REM Set local scope for the variables with windows NT shell
IF "%OS%"=="Windows_NT" @SETLOCAL

@REM Find the project root directory
SET "EXEC_DIR=%CD%"
SET "WDIR=%~dp0"
DIR "%WDIR%" >NUL 2>&1
IF %ERRORLEVEL% NEQ 0 (
  SET "WDIR=%CD%"
)

cd /D "%WDIR%"
mvn %*
SET ERROR_CODE=%ERRORLEVEL%
cd /D "%EXEC_DIR%"

EXIT /B %ERROR_CODE%
