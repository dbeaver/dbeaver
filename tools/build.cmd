cd ..
IF NOT EXIST ..\dbeaver-common git clone https://github.com/dbeaver/dbeaver-common.git ..\dbeaver-common
IF NOT EXIST ..\dbeaver-jdbc-libsql git clone https://github.com/dbeaver/dbeaver-jdbc-libsql.git ..\dbeaver-jdbc-libsql
cd product/aggregate
call mvn clean package -Pall-platforms -T 1C
cd ../..
pause