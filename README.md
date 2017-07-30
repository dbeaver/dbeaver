[![Build Status](https://travis-ci.org/serge-rider/dbeaver.svg?branch=devel)](https://travis-ci.org/serge-rider/dbeaver)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/93fcfdba7805406298b2e60c9d56f50e)](https://www.codacy.com/app/serge/dbeaver?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=serge-rider/dbeaver&amp;utm_campaign=Badge_Grade)

<img src="https://github.com/serge-rider/dbeaver/wiki/images/dbeaver-icon-64x64.png" align="right"/>

# DBeaver

Free multi-platform database tool for developers, SQL programmers, database administrators and analysts. 
Supports any database which has JDBC driver (which basically means - ANY database). EE version also supports non-JDBC datasources (WMI, MongoDB, Cassandra, Redis).
* Has a lot of <a href="http://dbeaver.jkiss.org/docs/features/">features</a> including metadata editor, SQL editor, rich data editor, ERD, data export/import/migration, SQL execution plans, etc. 
* Based on <a href="http://www.eclipse.org/">Eclipse</a> platform.
* Uses plugins architecture and provides additional functionality for the following databases: MySQL/MariaDB, PostgreSQL, Oracle, DB2 LUW, Exasol, SQL Server, SQLite, Firebird, H2, HSQLDB, Derby, Teradata, Vertica, Netezza, Informix.

<a href="http://dbeaver.jkiss.org/product/dbeaver-ss-classic.png"><img src="http://dbeaver.jkiss.org/product/dbeaver-ss-classic.png" width="400"/></a>
<a href="http://dbeaver.jkiss.org/product/dbeaver-ss-dark.png"><img src="http://dbeaver.jkiss.org/product/dbeaver-ss-dark.png" width="400"/></a>

## Download

You can download prebuilt binaries from http://dbeaver.jkiss.org/download/  
History and release notes: https://github.com/serge-rider/dbeaver/releases

## Running

DBeaver requires Java (JRE) 1.8+ to run.
* <b>Windows</b> installer includes JRE so just use it and don't think about internals.
* On <b>Linux</b> you may need to install Java manually (usually by running `sudo apt-get install default-jre` or something similar).
* On <b>MacOS X</b> you may need to download Java (JDK) from <a href="http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html">Oracle web site</a>. Or use <a href="http://stackoverflow.com/questions/24342886/how-to-install-java-8-on-mac">these instructions</a>.

## Documentation

WIKI: https://github.com/serge-rider/dbeaver/wiki  
Issue tracker: https://github.com/serge-rider/dbeaver/issues

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

- DBeaver is a free non-profitable hobby project. Please don't expect immediate reaction on issues.
- If you have any questions, suggestions, ideas, etc - <a href="mailto:serge@jkiss.org">write me</a>.
- Pull requests are welcome.
- Visit http://dbeaver.jkiss.org for more information.
- Thanks for using DBeaver! Star if you like it.

## Donation

Initially DBeaver was just a fun project which didn't suppose any donation model. And I was totally happy with that for years.  
But once DBeaver became a popular tool and we receive hundreds of feature requests - I cannot provide quality support anymore  because it consumes too much time. I have my daytime job, family and lots of other things to do.  
Anyhow - instead of donation button we created so-called <a href="https://github.com/serge-rider/dbeaver/wiki/Enterprise-Edition">Enterprise Edition</a> version. It is a commercial version of DBeaver which includes some additional enterprise features and official customer support.  
So, if you want to support this project - just <a href="https://dbeaver.com/purchase">buy a license</a> (there are discounts for community - check our WIKI). And you will get official support as a sign of our gratitude. Thank you!  
If you have any questions - please write me on serge@jkiss.org.  
Thank you!  
