set "scriptDir=%~dp0"
set "workspaceDir=%scriptDir%..\.."
REM mvnw.cmd runs the resolved Maven path unquoted; if %USERPROFILE% contains spaces, set MAVEN_USER_HOME to a path without spaces (or set it yourself).
IF NOT DEFINED MAVEN_USER_HOME SET "MAVEN_USER_HOME=%ProgramData%\DBeaver-m2"
IF NOT EXIST "%MAVEN_USER_HOME%" mkdir "%MAVEN_USER_HOME%"
IF NOT EXIST "%workspaceDir%\dbeaver-common" git clone https://github.com/dbeaver/dbeaver-common.git "%workspaceDir%\dbeaver-common"
IF NOT EXIST "%workspaceDir%\dbeaver-jdbc-libsql" git clone https://github.com/dbeaver/dbeaver-jdbc-libsql.git "%workspaceDir%\dbeaver-jdbc-libsql"
call "%workspaceDir%\dbeaver-common\mvnw.cmd" clean package -Pproduct-dbeaver-ce,product-dbeaver-eclipse-ce,appstore -T 1C -f "%workspaceDir%\dbeaver\product\aggregate"
