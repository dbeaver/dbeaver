@echo off
SET ECLIPSE_HOME=D:/Java/eclipse-build

echo =====================================================================
echo Copy plugins
mkdir build
mkdir build\features
mkdir build\plugins
rem xcopy /E /Q ..\org.jkiss.dbeaver build\plugins

echo =====================================================================
echo Build product
java -jar %ECLIPSE_HOME%/plugins/org.eclipse.equinox.launcher_1.1.0.v20100507.jar -application org.eclipse.ant.core.antRunner -buildfile %ECLIPSE_HOME%/plugins/org.eclipse.pde.build_3.6.0.v20100603/scripts/productBuild/productBuild.xml -Dbuilder=D:/Devel/my/DBeaver/product

