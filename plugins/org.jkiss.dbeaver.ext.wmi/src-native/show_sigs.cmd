:: use the JDK9+ installation in %JAVA_HOME%
:: and show the method signatures in the WMIService class file
%JAVA_HOME%/bin/javap -classpath ../target/classes -s org.jkiss.wmi.service.WMIService
