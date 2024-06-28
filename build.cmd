set MAVEN_OPTS=-Xmx2048m
IF NOT EXIST ..\dbeaver-common git clone https://github.com/dbeaver/dbeaver-common.git ..\dbeaver-common
cd product/aggregate
call mvn clean package -Pall-platforms -T 1C
cd ../..
pause