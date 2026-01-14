set JAVA_HOME=D:\Java\jdk1.6.0_06
set PATH=%JAVA_HOME%/bin;%PATH%

javah -classpath ../../bin;../../../../plugins/org.jkiss.dbeaver.core/bin -o WMIServiceJNI.h org.jkiss.wmi.service.WMIService
javah -classpath ../../bin;../../../../plugins/org.jkiss.dbeaver.core/bin -o WMIObjectJNI.h org.jkiss.wmi.service.WMIObject