@echo off

echo Unpack java archives

call :unpackJars plugins
call :unpackJars jre\lib .jar
call :unpackJars jre\lib\ext .jar

echo Unpack completed

goto :EOF

:unpackJars

for %%f in (%~1\*.pack) do (
    echo Unpack jar "%~1\%%~nf"
	unpack200 %~1\%%~nf.pack %~1\%%~nf%~2
	del %~1\%%~nf.pack
)

goto :EOF
