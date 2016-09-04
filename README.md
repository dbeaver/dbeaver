[![Build Status](https://travis-ci.org/serge-rider/dbeaver.svg?branch=devel)](https://travis-ci.org/serge-rider/dbeaver)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/93fcfdba7805406298b2e60c9d56f50e)](https://www.codacy.com/app/serge/dbeaver?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=serge-rider/dbeaver&amp;utm_campaign=Badge_Grade)

<img src="https://github.com/serge-rider/dbeaver/wiki/images/dbeaver-icon-64x64.png" align="right"/>
# DBeaver


Free multi-platform database tool for developers, SQL programmers, database administrators and analysts. 
Supports any database which has JDBC driver (which basically means - ANY database). Can work with non-JDBC datasources (WMI, MongoDB, Cassandra, Redis).
* Has a lot of <a href="http://dbeaver.jkiss.org/docs/features/">features</a>. 
* Based on <a href="http://www.eclipse.org/">Eclipse</a> platform.
* Uses plugins architecture and provides additional functionality for the most popular databases (MySQL, PostgreSQL, Oracle, DB2 in version 3.7.x).

<a href="http://dbeaver.jkiss.org/product/dbeaver-ss-classic.png"><img src="http://dbeaver.jkiss.org/product/dbeaver-ss-classic.png" width="400"/></a>
<a href="http://dbeaver.jkiss.org/product/dbeaver-ss-dark.png"><img src="http://dbeaver.jkiss.org/product/dbeaver-ss-dark.png" width="400"/></a>

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
Binaries are in `product/standalone/target/products`

## Notes

- Please leave bug reports and feature requests in the <a href="https://github.com/serge-rider/dbeaver/issues">GitHub issue tracker</a>.
- DBeaver is a free non-profitable hobbie project. Please don't expect immediate reaction on issues.
- If you have any questions, suggestions, ideas, etc - <a href="mailto:serge@jkiss.org">write me</a>.
- Pull requests are welcome.
- Visit http://dbeaver.jkiss.org for more information.
- Thanks for using DBeaver! Star if you like it.
- 
