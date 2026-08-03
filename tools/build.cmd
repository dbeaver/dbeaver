set "scriptDir=%~dp0"
set "workspaceDir=%scriptDir%..\.."
IF NOT EXIST "%workspaceDir%\dbeaver-common" git clone https://github.com/dbeaver/dbeaver-common.git "%workspaceDir%\dbeaver-common"
call "%workspaceDir%\dbeaver-common\mvnw.cmd" clean package -Pproduct-dbeaver-ce,product-dbeaver-eclipse-ce,appstore -T 1C -f "%workspaceDir%\dbeaver\product\aggregate"
