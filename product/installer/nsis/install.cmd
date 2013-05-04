@echo off

echo Unpack jars

call :unpackJars plugins
call :unpackJars jre\lib .jar
call :unpackJars jre\lib\ext .jar

goto :EOF

:unpackJars

echo Unpack jars in "%~1"
for %%f in (%~1\*.pack) do (
    echo Unpack jar "%~1\%%~nf"
	jre\bin\unpack200 %~1\%%~nf.pack %~1\%%~nf%~2
	del %~1\%%~nf.pack
)

goto :EOF
