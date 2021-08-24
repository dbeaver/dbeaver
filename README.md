[![Twitter URL](https://img.shields.io/twitter/url/https/twitter.com/dbeaver_news.svg?style=social&label=Follow%20%40dbeaver_news)](https://twitter.com/dbeaver_news)
[![Build Status](https://api.travis-ci.com/dbeaver/dbeaver.svg?branch=devel)](https://app.travis-ci.com/github/dbeaver/dbeaver)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/fa0bb9cf5a904c7d87424f8f6351ba92)](https://www.codacy.com/gh/dbeaver/dbeaver/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=dbeaver/dbeaver&amp;utm_campaign=Badge_Grade)
[![Apache 2.0](https://img.shields.io/github/license/cronn-de/jira-sync.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Java CI](https://github.com/dbeaver/dbeaver/workflows/Java%20CI/badge.svg)](https://github.com/dbeaver/dbeaver/actions?query=workflow%3A%22Java+CI%22)
<!--[![paypal](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=KFGAGZ24YZE3C)-->


<img src="https://github.com/dbeaver/dbeaver/wiki/images/dbeaver-icon-64x64.png" align="right"/>

# DBeaver

Free multi-platform database tool for developers, SQL programmers, database administrators and analysts.  
Supports any database which has JDBC driver (which basically means - ANY database). 
<a href="https://dbeaver.com/download/">Commercial versions</a> also support non-JDBC datasources such as 
MongoDB, Cassandra, Couchbase, Redis, BigTable, DynamoDB, etc.
You can find the list of all databases supported in commercial versions 
<a href="https://dbeaver.com/databases/">here</a>.

* Has a lot of <a href="https://github.com/dbeaver/dbeaver/wiki">features</a> including metadata editor, SQL editor, rich data editor, ERD, data export/import/migration, SQL execution plans, etc.
* Based on <a href="https://wiki.eclipse.org/Rich_Client_Platform">Eclipse</a> platform.
* Uses plugins architecture and provides additional functionality for the following databases: MySQL/MariaDB, PostgreSQL, Greenplum, Oracle, DB2 LUW, Exasol, SQL Server, Sybase/SAP ASE, SQLite, Firebird, H2, HSQLDB, Derby, Teradata, Vertica, Netezza, Informix, etc.

<a href="https://dbeaver.io/product/dbeaver-ss-mock.png"><img src="https://dbeaver.io/product/dbeaver-ss-mock.png" width="400"/></a>
<a href="https://dbeaver.io/product/dbeaver-ss-erd.png"><img src="https://dbeaver.io/product/dbeaver-ss-erd.png" width="400"/></a>
<a href="https://dbeaver.io/product/dbeaver-ss-classic-new.png"><img src="https://dbeaver.io/product/dbeaver-ss-classic-new.png" width="400"/></a>
<a href="https://dbeaver.io/product/dbeaver-ss-dark-new.png"><img src="https://dbeaver.io/product/dbeaver-ss-dark-new.png" width="400"/></a>

## Download

You can download prebuilt binaries from <a href="https://dbeaver.io/download" target="_blank">official website</a> or directly from <a href="https://github.com/dbeaver/dbeaver/releases">GitHub releases</a>.  
You can also download <a href="https://dbeaver.io/files/ea" target="_blank">Early Access</a> version. We publish it 1-2 times per week.  

## Running

Just run an installer (or unzip an archive) and run `dbeaver`.  

Note: DBeaver needs Java to run. <a href="https://adoptopenjdk.net/" target="_blank">Open JDK 11</a> is included in all DBeaver distributions. (since version 7.3.1).  
You can change default JDK version by replacing directory `jre` in dbeaver installation folder.

## Documentation

* <a href="https://github.com/dbeaver/dbeaver/wiki">WIKI</a>
* <a href="https://github.com/dbeaver/dbeaver/issues">Issue tracker</a>

## Building

#### Prerequisites:

 1. Java (JDK) 11 or later (<a href="https://adoptopenjdk.net/" target="_blank">AdoptOpenJDK 11</a> is our default Java at the moment).
 2. <a href="https://maven.apache.org/" target="_blank">Apache Maven 3.6+</a>
 3. Internet access
 4. Git client

#### Build

```sh
git clone https://github.com/dbeaver/dbeaver.git dbeaver
cd dbeaver
mvn package
```
Binaries are in `product/community/target/products`

## Notes

- For bug reports and feature requests - please <a href="https://github.com/dbeaver/dbeaver/issues">create a ticket</a>.
- If you have any questions, ideas, etc - please <a href="https://github.com/dbeaver/dbeaver/discussions">start a discussion</a>.
- Pull requests are welcome.
- Visit https://dbeaver.io or https://dbeaver.com for more information.
- Follow us on Twitter: https://twitter.com/dbeaver_news/
- Thanks for using DBeaver! Star if you like it.

## Contribution: help the Beaver!

Hooray, we have reached 17k of stars on GitHub and continue to grow!  
That's really cool, we are glad that you like DBeaver.

- We are actively looking for new source code contributors. We have added labels “Good first issue” and “Help wanted” to some tickets. If you want to be a part of our development team just be brave and take a ticket.
- You can buy <a href="https://dbeaver.com/buy/">one of our commercial versions</a>. They include NoSQL databases support, additional extensions, and official online support. Also, licensed users have priorities in bug fixes and new features development.

Thank you!  

- <a href="https://github.com/dbeaver/dbeaver/graphs/contributors">DBeaver Team</a> (contributors)

---------

<a href="https://github.com/dbeaver/cloudbeaver/"><img src="https://github.com/dbeaver/cloudbeaver/wiki/images/cloudbeaver-logo.png" width="250"/></a>

DBeaver is a desktop client.  
If you are looking for a web-based database management tool - check our new product: <a href="https://cloudbeaver.io/">CloudBeaver</a>.  
It is based on DBeaver platform and thus supports any database and most of DBeaver features.
