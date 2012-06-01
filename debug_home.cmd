set DBEAVER_HOME=D:/Devel/My/DBeaver
set ECLIPSE_HOME=d:\java\eclipse-3.7
SET ORACLE_HOME=D:\DB\Oracle\product\11.2.0\client_1
SET ORA_HOME=D:\DB\Oracle\product\11.2.0\client_1
SET PATH=%PATH%;%ORA_HOME%
start "DBeaver" javaw -Dfile.encoding=Cp1251 -classpath %ECLIPSE_HOME%\plugins\org.eclipse.equinox.launcher_1.2.0.v20110502.jar org.eclipse.equinox.launcher.Main -launcher %ECLIPSE_HOME%\eclipse.exe -name Eclipse -showsplash 600 -product org.jkiss.dbeaver.core.product -configuration "file:%DBEAVER_HOME%/workspace/.metadata/.plugins/org.eclipse.pde.core/DBeaver.product/" -dev "file:%DBEAVER_HOME%/workspace/.metadata/.plugins/org.eclipse.pde.core/DBeaver.product/dev.properties" -os win32 -ws win32 -arch x86 -nl ru_RU
