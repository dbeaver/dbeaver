:: use the JDK9+ installation in %JAVA_HOME%
:: and generate the native header files in current directory
%JAVA_HOME%/bin/javac -classpath ../target/classes -h . -d ../target/classes ../src/org/jkiss/wmi/service/WMIService.java
%JAVA_HOME%/bin/javac -classpath ../target/classes -h . -d ../target/classes ../src/org/jkiss/wmi/service/WMIObject.java

:: rename the generated native header files
move /y org_jkiss_wmi_service_WMIService.h WMIServiceJNI.h
move /y org_jkiss_wmi_service_WMIObject.h WMIObjectJNI.h
