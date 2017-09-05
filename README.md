[![Build Status](https://travis-ci.org/serge-rider/dbeaver.svg?branch=devel)](https://travis-ci.org/serge-rider/dbeaver)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/93fcfdba7805406298b2e60c9d56f50e)](https://www.codacy.com/app/serge/dbeaver?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=serge-rider/dbeaver&amp;utm_campaign=Badge_Grade)

<img src="https://github.com/serge-rider/dbeaver/wiki/images/dbeaver-icon-64x64.png" align="right"/>

# DBeaver

Free multi-platform database tool for developers, SQL programmers, database administrators and analysts. 
Supports any database which has JDBC driver (which basically means - ANY database). EE version also supports non-JDBC datasources (WMI, MongoDB, Cassandra, Redis).
* Has a lot of <a href="https://github.com/serge-rider/dbeaver/wiki">features</a> including metadata editor, SQL editor, rich data editor, ERD, data export/import/migration, SQL execution plans, etc. 
* Based on <a href="http://www.eclipse.org/">Eclipse</a> platform.
* Uses plugins architecture and provides additional functionality for the following databases: MySQL/MariaDB, PostgreSQL, Oracle, DB2 LUW, Exasol, SQL Server, SQLite, Firebird, H2, HSQLDB, Derby, Teradata, Vertica, Netezza, Informix.

<a href="https://dbeaver.jkiss.org/product/dbeaver-ss-classic.png"><img src="https://dbeaver.jkiss.org/product/dbeaver-ss-classic.png" width="400"/></a>
<a href="https://dbeaver.jkiss.org/product/dbeaver-ss-dark.png"><img src="https://dbeaver.jkiss.org/product/dbeaver-ss-dark.png" width="400"/></a>

## Download

You can download prebuilt binaries from <a href="https://dbeaver.jkiss.org/download">official web site</a> or directly from <a href="https://github.com/serge-rider/dbeaver/releases">GitHub releases</a>.

## Running

DBeaver requires Java (JRE) 1.8+ to run.
* <b>Windows</b> installer includes JRE so just use it and don't think about internals.
* On <b>Linux</b> you may need to install Java manually (usually by running `sudo apt-get install default-jre` or something similar).
* On <b>MacOS X</b> you may need to download Java (JDK) from <a href="http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html">Oracle web site</a>. Or use <a href="http://stackoverflow.com/questions/24342886/how-to-install-java-8-on-mac">these instructions</a>.

## Documentation

<a href="https://github.com/serge-rider/dbeaver/wiki">WIKI</a>  
<a href="https://github.com/serge-rider/dbeaver/issues">Issue tracker</a>

## Building

#### Prerequisites:
 1. Java (JDK) 1.8+
 2. Apache Maven 3+
 3. Internet access

#### Build
```sh
git clone https://github.com/serge-rider/dbeaver.git dbeaver
cd dbeaver
mvn package
```
Binaries are in `product/standalone/target/products`

## Notes

- If you have any questions, suggestions, ideas, etc - <a href="mailto:serge@jkiss.org">write me</a>.
- Pull requests are welcome.
- Visit https://dbeaver.jkiss.org or https://dbeaver.com for more information.
- Thanks for using DBeaver! Star if you like it.

## Help the Beaver!

Hooray, we have reached 3k of stars on GitGub and more than 500k of users!
That's really cool, we are glad that you like DBeaver.
We receiving lots of tickets with bug reports and feature requests every day - and we going to resolve most of them.
In order to be able to continue develop this project and do everything faster and better we decided to create <a href="https://dbeaver.com/download">DBeaver Enterprise Edition</a>.

EE version includes all features of CE + NoSQL databases support + EE extensions + official online support. Also licensed users have priorities in bug fixes and new features development.

If you need extra features or you want to support DBeaver - purchase EE version.  
Those who read up to this point - you can use community discounts (check our <a href="https://github.com/serge-rider/dbeaver/wiki/Enterprise-Edition">WIKI</a>).

Save wild animals :) Thank you!  
