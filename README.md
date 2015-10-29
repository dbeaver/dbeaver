# DBeaver
Free multi-platform database tool for developers, SQL programmers, database administrators and analysts. 
Supports any database which has JDBC driver (which basically means - ANY database). Can work with non-JDBC datasources (WMI, MongoDB, Cassandra).
* Has a lot of <a href="http://dbeaver.jkiss.org/docs/features/">features</a>. 
* Based on <a href="http://www.eclipse.org/">Eclipse</a> platform.
* Uses plugins architecture and provides additional functionality for the most popular databases (MySQL, Oracle, DB2 in version 3.5.x).

## Download

You can download prebuilt binaries from http://dbeaver.jkiss.org/download/

## Building

#### Prerequisites:
 1. Java (JDK) 1.7+
 2. Apache Maven 3+
 3. Internet access

#### Build
```sh
git clone https://github.com/serge-rider/dbeaver.git dbeaver
cd dbeaver
mvn install
```
Binaries are in product/standalone/target/products

## Notes

- Pull requests are welcome.
- DBeaver is a free non-profitable hobbie project. Please don't expect immediate reaction on issues.
- If you have any questions, suggestions, ideas, etc - <a href="mailto:serge@jkiss.org">write me</a>.
- Visit http://dbeaver.jkiss.org for more information.
- Thanks for using DBeaver ;)
