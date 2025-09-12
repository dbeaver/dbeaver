[![Twitter URL](https://img.shields.io/twitter/url/https/twitter.com/dbeaver_news.svg?style=social&label=Follow%20%40dbeaver_news)](https://twitter.com/dbeaver_news)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/fa0bb9cf5a904c7d87424f8f6351ba92)](https://app.codacy.com/gh/dbeaver/dbeaver/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![Apache 2.0](https://img.shields.io/github/license/cronn-de/jira-sync.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Tickets in review](https://img.shields.io/github/issues/dbeaver/dbeaver/wait%20for%20review)](https://github.com/dbeaver/dbeaver/issues?q=is%3Aissue+is%3Aopen+label%3A"wait%20for%20review")
<img src="https://github.com/dbeaver/dbeaver/wiki/images/dbeaver-icon-64x64.png" align="right"/>

# DBeaver

Free multi-platform database tool for developers, SQL programmers, database administrators and analysts.  

* Has a lot of <a href="https://github.com/dbeaver/dbeaver/wiki">features</a> including schema editor, SQL editor, data editor, AI integration, ER diagrams, data export/import/migration, SQL execution plans, database administration tools, database dashboards, Spatial data viewer, proxy and SSH tunnelling, custom database drivers editor, etc.
* Out of the box supports more than <a href="#supported-databases">100 database drivers</a>.
* Supports any database which has JDBC or ODBC driver (basically - almost all existing databases).
* Supports smart AI completion and code generation with OpenAI or Copilot

<a href="https://dbeaver.io/product/dbeaver-sql-editor.png"><img src="https://dbeaver.io/product/dbeaver-sql-editor.png" width="400"/></a>
<a href="https://dbeaver.io/product/dbeaver-gis-viewer.png"><img src="https://dbeaver.io/product/dbeaver-gis-viewer.png" width="400"/></a>
<a href="https://dbeaver.io/product/dbeaver-data-editor.png"><img src="https://dbeaver.io/product/dbeaver-data-editor.png" width="400"/></a>
<a href="https://dbeaver.io/product/dbeaver-erd.png"><img src="https://dbeaver.io/product/dbeaver-erd.png" width="400"/></a>

## Download

You can download prebuilt binaries from <a href="https://dbeaver.io/download" target="_blank">official website</a> or directly from <a href="https://github.com/dbeaver/dbeaver/releases">GitHub releases</a>.  
You can also download <a href="https://dbeaver.io/files/ea" target="_blank">Early Access</a> version. We publish daily.  

## Running

Just run an installer (or unzip an archive) and run `dbeaver`.  

Note: DBeaver needs Java to run. <a href="https://adoptium.net/temurin/releases/?package=jre" target="_blank">OpenJDK 21</a> is included in all DBeaver distributions.
You can change default JDK version by replacing directory `jre` in dbeaver installation folder.

## Documentation

* [Full product documentation](https://dbeaver.com/docs/dbeaver/)
* [WIKI](https://github.com/dbeaver/dbeaver/wiki)
* [Issue tracker](https://github.com/dbeaver/dbeaver/issues)
* [Building from sources](https://github.com/dbeaver/dbeaver/wiki/Build-from-sources)

## Architecture

- DBeaver is written mostly on Java. However, it also uses a set of native OS-specific components for desktop UI, high performance database drivers and networking.
- Basic frameworks:
  - [OSGI](https://en.wikipedia.org/wiki/OSGi) platform for plugins and dependency management. Community version consists of 130+ plugins.
  - [Eclipse RCP](https://github.com/eclipse-platform/eclipse.platform.ui/blob/master/docs/Rich_Client_Platform.md) platform for rich user interface build.
  - [JDBC](https://en.wikipedia.org/wiki/Java_Database_Connectivity) for basic database connectivity API.
  - [JSQLParser](https://github.com/JSQLParser/JSqlParser) and [Antlr4](https://github.com/antlr/antlr4) for SQL grammar and semantic parser.
- For networking and additional functionality we use wide range of open source libraries such as [SSHJ](https://github.com/hierynomus/sshj), [Apache POI](https://github.com/apache/poi), [JFreeChart](https://github.com/jfree/jfreechart), [JTS](https://github.com/locationtech/jts), [Apache JEXL](https://github.com/apache/commons-jexl) etc.
- We separate model plugins from desktop UI plugins. This allows us to use the same set of "back-end" plugins in both DBeaver and [CloudBeaver](https://github.com/dbeaver/cloudbeaver).
- Dependencies: being an OSGI application we use P2 repositories for third party dependencies. For additional Maven dependencies we use our own [DBeaver P2 repo](https://github.com/dbeaver/dbeaver-deps-ce).

## Supported databases

### Community version

Out of the box DBeaver supports following database drivers: 
MySQL, MariaDB, Oracle, DB2, PostgreSQL, SQL Server, Sybase, Apache Hive, Drill, Presto, Trino, Phoenix, Exasol, Informix, Teradata, Vertica, Netezza, Firebird, Derby, H2, H2GIS, WMI, Snowflake, Greenplum, Redshift, Athena, SAP HANA, MaxDB, NuoDB, MS Access, SQLite, CSV, DBF, Firebird, TimescaleDB, Yellowbrick, CockroachDB, OrientDB, MonetDB, Google BigQuery, Google Spanner, Apache Hive/Impala/Spark, Apache Ignite, MapD, Azure SQL, CrateDB, Elasticsearch, Ocient, Ingres, OmniSci, Yugabyte, IRIS, Data Virtuality, Denodo, Virtuoso, Machbase, DuckDB, Babelfish, OceanBase, Salesforce, EnterpriseDB, Apache Druid, Apache Kylin, Databricks, OpenSearch, TiDB, TDEngine, Materialize, JDBCX, Dameng, Altibase, StarRocks, CUBRID, GaussDB, DolphinDB, LibSQL, GBase 8s, Databend, Cloudberry, Teiid, Kingbase.

### PRO versions

<a href="https://dbeaver.com/download/">Commercial versions</a> extends functionality of many popular drivers and also support non-JDBC datasources such as:
ODBC, MongoDB, Cassandra, Couchbase, CouchDB, Redis, InfluxDB, Firestore, BigTable, DynamoDB, Kafka KSQL, Neo4j, AWS Neptune, AWS Timestream, Azure CosmosDB, Yugabyte, Salesforce, etc.  
Also, we support flat files as databases: CSV, XLSX, Json, XML, Parquet.  
You can find the list of all databases supported in commercial versions <a href="https://dbeaver.com/databases/">here</a>.

## Feedback

- For bug reports and feature requests - please <a href="https://github.com/dbeaver/dbeaver/issues">create a ticket</a>.
- To promote <a href="https://github.com/dbeaver/dbeaver/issues?q=is%3Aissue+is%3Aopen+sort%3Areactions-%2B1-desc+label%3A%22wait+for+votes%22">a ticket</a> to a higher priority - please vote for it with 👍 under the ticket description.
- If you have any questions, ideas, etc - please <a href="https://github.com/dbeaver/dbeaver/discussions">start a discussion</a>.
- Pull requests are welcome. See our <a href="https://github.com/dbeaver/dbeaver/wiki/Contribute-your-code">guide for contributors</a>.
- Visit https://dbeaver.com for more information.
- Follow us on [X](https://x.com/dbeaver_news/) and watch educational video on [YouTube](https://www.youtube.com/@DBeaver_video)
- Thanks for using DBeaver! Star if you like it.

## Contribution: help the Beaver!

Hooray, we have reached 40k+ stars on GitHub and continue to grow!  
That's really cool, and we are glad that you like DBeaver.

- We are actively looking for new source code contributors. We have added labels “Good first issue” and “Help wanted” to some tickets. If you want to be a part of our development team, just be brave and take a ticket. <a href="https://dbeaver.com/help-dbeaver/">We are happy to reward</a> our most active contributors every major sprint.
- You can buy <a href="https://dbeaver.com/buy/">one of our commercial versions</a>. They include NoSQL databases support, additional extensions, and official online support. Also, licensed users have priorities in bug fixes and the development of new features.

Thank you!  

- <a href="https://github.com/dbeaver/dbeaver/graphs/contributors">DBeaver Team</a> (contributors)

---------

<a href="https://github.com/dbeaver/cloudbeaver/"><img src="https://github.com/dbeaver/cloudbeaver/wiki/images/cloudbeaver-logo.png" width="250"/></a>

<a href="https://github.com/dbeaver/cloudbeaver">CloudBeaver</a> is a web-based database management tool built on the DBeaver platform. It brings the capabilities of DBeaver to the browser, enabling database management from any device with an internet connection and eliminating the need for local installation. Supporting any database, CloudBeaver incorporates most of DBeaver's features and includes advanced access management for secure collaboration.
Designed with a user-friendly interface, CloudBeaver simplifies complex database operations and is suitable for both individual developers and organizations. Its scalable architecture accommodates various needs, making it a convenient solution for managing databases anytime and anywhere through web-based accessibility.
