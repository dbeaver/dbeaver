[![Twitter URL](https://img.shields.io/twitter/url/https/twitter.com/dbeaver_news.svg?style=social&label=Follow%20%40dbeaver_news)](https://twitter.com/dbeaver_news)
[![Build Status](https://travis-ci.org/dbeaver/dbeaver.svg?branch=devel)](https://travis-ci.org/dbeaver/dbeaver)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/93fcfdba7805406298b2e60c9d56f50e)](https://www.codacy.com/app/serge/dbeaver?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=dbeaver/dbeaver&amp;utm_campaign=Badge_Grade)
[![Apache 2.0](https://img.shields.io/github/license/cronn-de/jira-sync.svg)](http://www.apache.org/licenses/LICENSE-2.0)
<!--[![paypal](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=KFGAGZ24YZE3C)-->

<img src="https://github.com/dbeaver/dbeaver/wiki/images/dbeaver-icon-64x64.png" align="right"/>

# DBeaver

Free multi-platform database tool for developers, SQL programmers, database administrators and analysts. 
Supports any database which has JDBC driver (which basically means - ANY database). EE version also supports non-JDBC datasources (WMI, MongoDB, Cassandra, Redis).

* Has a lot of <a href="https://github.com/dbeaver/dbeaver/wiki">features</a> including metadata editor, SQL editor, rich data editor, ERD, data export/import/migration, SQL execution plans, etc.
* Based on <a href="http://www.eclipse.org/">Eclipse</a> platform.
* Uses plugins architecture and provides additional functionality for the following databases: MySQL/MariaDB, PostgreSQL, Oracle, DB2 LUW, Exasol, SQL Server, Sybase/SAP ASE, SQLite, Firebird, H2, HSQLDB, Derby, Teradata, Vertica, Netezza, Informix, etc.

<a href="https://dbeaver.io/product/dbeaver-ss-mock.png"><img src="https://dbeaver.io/product/dbeaver-ss-mock.png" width="400"/></a>
<a href="https://dbeaver.io/product/dbeaver-ss-erd.png"><img src="https://dbeaver.io/product/dbeaver-ss-erd.png" width="400"/></a>
<a href="https://dbeaver.io/product/dbeaver-ss-classic.png"><img src="https://dbeaver.io/product/dbeaver-ss-classic.png" width="400"/></a>
<a href="https://dbeaver.io/product/dbeaver-ss-dark.png"><img src="https://dbeaver.io/product/dbeaver-ss-dark.png" width="400"/></a>

## Download

You can download prebuilt binaries from <a href="https://dbeaver.io/download">official web site</a> or directly from <a href="https://github.com/dbeaver/dbeaver/releases">GitHub releases</a>.

## Running

DBeaver requires Java (JRE) 1.8+ to run.

* <b>Windows</b> and <b>MacOS X</b> installers include JRE so just use them and don't think about internals.
* On <b>Linux</b> you may need to install Java manually (usually by running `sudo apt-get install default-jre` or something similar).
* If you don't use installer (on Windows or Mac OS X) you may need to download Java (JDK) from <a href="http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html">Oracle web site</a>.

## Documentation

<a href="https://github.com/dbeaver/dbeaver/wiki">WIKI</a>
<a href="https://github.com/dbeaver/dbeaver/issues">Issue tracker</a>

## Building

#### Prerequisites:

 1. Java (JDK) 8 or Java 9.
 2. Apache Maven 3+
 3. Internet access

#### Build

```sh
git clone https://github.com/dbeaver/dbeaver.git dbeaver
cd dbeaver
mvn package
```
Binaries are in `product/standalone/target/products`

## Notes

- If you have any questions, suggestions, ideas, etc - <a href="mailto:dbeaver@jkiss.org">write us</a>.
- Pull requests are welcome.
- Visit https://dbeaver.io or https://dbeaver.com for more information.
- Follow us on Twitter: https://twitter.com/dbeaver_news/
- Thanks for using DBeaver! Star if you like it.

## Contribution: help the Beaver!

Hooray, we have reached 5k of stars on GitHub and continue to grow!
That's really cool, we are glad that you like DBeaver.

- We are actively looking for new source code contributors. We have added labels “Good first issue” and “Help wanted” to some tickets. If you want to be a part of our development team just be brave and take a ticket.
- We decided to try Bounty: https://www.bountysource.com/issues/62789348-tip-of-the-day-popup. We are not sure that it works. But if yes we will be glad to place more tickets to this platform.
- You can buy <a href="https://dbeaver.com/download">DBeaver EE</a> version. It includes all features of CE + NoSQL databases support + EE extensions + official online support. Also, licensed users have priorities in bug fixes and new features development.
Those who read up to this point - you can use community discounts (check our <a href="https://github.com/dbeaver/dbeaver/wiki/Enterprise-Edition">WIKI</a>).

Save wild animals :) Thank you!  

DBeaver Team
