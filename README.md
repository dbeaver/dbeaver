[![Twitter URL](https://img.shields.io/twitter/url/https/twitter.com/dbeaver_news.svg?style=social&label=Follow%20%40dbeaver_news)](https://twitter.com/dbeaver_news)
[![Build Status](https://travis-ci.org/dbeaver/dbeaver.svg?branch=devel)](https://travis-ci.org/dbeaver/dbeaver)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/70fbe55885864aa38d246b8180f5916a)](https://www.codacy.com/manual/serge/dbeaver?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=dbeaver/dbeaver&amp;utm_campaign=Badge_Grade)
[![Apache 2.0](https://img.shields.io/github/license/cronn-de/jira-sync.svg)](http://www.apache.org/licenses/LICENSE-2.0)
![Java CI](https://github.com/dbeaver/dbeaver/workflows/Java%20CI/badge.svg)
<!--[![paypal](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=KFGAGZ24YZE3C)-->


<img src="https://github.com/dbeaver/dbeaver/wiki/images/dbeaver-icon-64x64.png" align="right"/>

# DBeaver

Free multi-platform database tool for developers, SQL programmers, database administrators and analysts. 
Supports any database which has JDBC driver (which basically means - ANY database). EE version also supports non-JDBC datasources (MongoDB, Cassandra, Redis, DynamoDB, etc).

* Has a lot of <a href="https://github.com/dbeaver/dbeaver/wiki">features</a> including metadata editor, SQL editor, rich data editor, ERD, data export/import/migration, SQL execution plans, etc.
* Based on <a href="http://www.eclipse.org/">Eclipse</a> platform.
* Uses plugins architecture and provides additional functionality for the following databases: MySQL/MariaDB, PostgreSQL, Greenplum, Oracle, DB2 LUW, Exasol, SQL Server, Sybase/SAP ASE, SQLite, Firebird, H2, HSQLDB, Derby, Teradata, Vertica, Netezza, Informix, etc.

<a href="https://dbeaver.io/product/dbeaver-ss-mock.png"><img src="https://dbeaver.io/product/dbeaver-ss-mock.png" width="400"/></a>
<a href="https://dbeaver.io/product/dbeaver-ss-erd.png"><img src="https://dbeaver.io/product/dbeaver-ss-erd.png" width="400"/></a>
<a href="https://dbeaver.io/product/dbeaver-ss-classic-new.png"><img src="https://dbeaver.io/product/dbeaver-ss-classic-new.png" width="400"/></a>
<a href="https://dbeaver.io/product/dbeaver-ss-dark-new.png"><img src="https://dbeaver.io/product/dbeaver-ss-dark-new.png" width="400"/></a>

## Download

You can download prebuilt binaries from <a href="https://dbeaver.io/download" target="_blank">official web site</a> or directly from <a href="https://github.com/dbeaver/dbeaver/releases">GitHub releases</a>.

## Running

DBeaver requires Java (JRE) 1.8+ to run.

* <b>Windows</b> and <b>MacOS X</b> installers include JRE so just use them and don't think about internals.
* On <b>Linux</b> you may need to install Java manually (usually by running `sudo apt-get install openjdk-11-jdk` or something similar).
* If you don't use installer (on Windows or Mac OS X) you may need to download Java (JDK) from <a href="https://adoptopenjdk.net/" target="_blank">Adopt OpenJDK web site</a>.

## Documentation

* <a href="https://github.com/dbeaver/dbeaver/wiki">WIKI</a>
* <a href="https://github.com/dbeaver/dbeaver/issues">Issue tracker</a>

## Building

#### Prerequisites:

 1. Java (JDK) 8 or later (AdoptOpenJDK 11 is our default Java at the moment).
 2. Apache Maven 3+
 3. Internet access
 4. Git client

#### Build

```sh
git clone https://github.com/dbeaver/dbeaver.git dbeaver
cd dbeaver
mvn package
```
Binaries are in `product/standalone/target/products`

## Notes

- If you have any questions, suggestions, ideas, etc - please <a href="https://github.com/dbeaver/dbeaver/issues">create a ticket </a>.
- Pull requests are welcome.
- Visit https://dbeaver.io or https://dbeaver.com for more information.
- Follow us on Twitter: https://twitter.com/dbeaver_news/
- Thanks for using DBeaver! Star if you like it.

## Contribution: help the Beaver!

Hooray, we have reached 10k of stars on GitHub and continue to grow!
That's really cool, we are glad that you like DBeaver.

- We are actively looking for new source code contributors. We have added labels “Good first issue” and “Help wanted” to some tickets. If you want to be a part of our development team just be brave and take a ticket.
- You can buy <a href="https://dbeaver.com/download">DBeaver EE</a> version. It includes all features of CE + NoSQL databases support + EE extensions + official online support. Also, licensed users have priorities in bug fixes and new features development.

Thank you!  

- <a href="https://github.com/dbeaver/dbeaver/graphs/contributors">DBeaver Team</a> (contributors)

---------

<a href="https://github.com/dbeaver/cloudbeaver/"><img src="https://github.com/dbeaver/cloudbeaver/wiki/images/cloudbeaver-logo.png" width="250"/></a>

DBeaver is a desktop client.  
If you are loooking for a web-based database management tool - check our new product: <a href="https://github.com/dbeaver/cloudbeaver/">CloudBeaver</a>.  
It is based on DBeaver platform and thus supports any database and most of DBeaver features.
