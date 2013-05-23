@echo off

for /r %%f in (%~1\*.pack) do (
    echo Unpack jar "%%~ff"
 	unpack200 -r "%%~ff" "%%~df%%~pf%%~nf%.jar"
)
