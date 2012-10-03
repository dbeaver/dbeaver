@rem java -jar D:\Java\eclipse-3.5\plugins\org.eclipse.equinox.launcher_1.0.100.v20080509-1800.jar
@rem java -Dfile.encoding=Cp1251 -classpath D:\Java\eclipse-3.5\plugins\org.eclipse.equinox.launcher_1.0.201.R35x_v20090715.jar org.eclipse.equinox.launcher.Main -launcher D:\Java\eclipse-3.4\eclipse.exe -name Eclipse -showsplash 600 -product org.jkiss.dbeaver.core.product -data "C:\Documents and Settings\jurgen\workspace/../runtime-DBeaver.product" -configuration "file:C:/Documents and Settings/jurgen/workspace/.metadata/.plugins/org.eclipse.pde.core/DBeaver.product/" -dev "file:C:/Documents and Settings/jurgen/workspace/.metadata/.plugins/org.eclipse.pde.core/DBeaver.product/dev.properties" -os win32 -ws win32 -arch x86 -nl ru_RU

set JAVA_HOME=D:\Java\jdk1.7.0
rem set JAVA_HOME=D:\Java\jdk1.5.0_05\
set PATH=%JAVA_HOME%/bin;%PATH%
java -version

SET ORACLE_HOME=D:\DB\Oracle\product\11.2.0\client_1
SET ORA_HOME=D:\DB\Oracle\product\11.2.0\client_1
SET PATH=%PATH%;%ORA_HOME%

start "DBeaver" javaw -Dosgi.requiredJavaVersion=1.5 -Xms40m -Xmx512m -Dfile.encoding=Cp1251 -classpath D:\Java\eclipse-3.6\plugins\org.eclipse.equinox.launcher_1.1.0.v20100507.jar org.eclipse.equinox.launcher.Main -launcher D:\Java\eclipse-3.6\eclipse.exe -name Eclipse -showsplash 600 -product org.jkiss.dbeaver.core.product -configuration file:D:/Devel/my/DBeaver/workspace/.metadata/.plugins/org.eclipse.pde.core/DBeaver.product/ -dev file:D:/Devel/my/DBeaver/workspace/.metadata/.plugins/org.eclipse.pde.core/DBeaver.product/dev.properties -os win32 -ws win32 -arch x86 -nl ru_RU -consoleLog