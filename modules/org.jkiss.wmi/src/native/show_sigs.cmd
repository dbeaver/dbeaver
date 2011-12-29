set JAVA_HOME=D:\Java\jdk1.6.0_06
set PATH=%JAVA_HOME%/bin;%PATH%

javap -classpath ../../bin;../../../../plugins/org.jkiss.dbeaver.core/bin -s org.jkiss.wmi.service.WMIService
