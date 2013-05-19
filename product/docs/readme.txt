DBeaver @build.version@

@build.date@

README

Thank you for downloading DBeaver!

DBeaver is a multi-platform universal database manager and SQL Client. 
It supports MySQL, PostgreSQL, Oracle, DB2, MSSQL, Sybase, Mimer, HSQLDB, 
Derby, and any database that has a JDBC driver. 

It is a GUI program that allows to view/edit the structure of a database, 
execute SQL queries and scripts, browse and export table data, 
handle BLOB/CLOB values, modify database meta objects, etc. 

Installing DBeaver
========================
  Use automatic installer distribution (Windows and Linux versions)
  or just unpack archive and start "dbeaver" executable.
  
  Note: do not extract archive in the same folder where older version
  of DBeaver is installed.
  Remove previous version or extract archive in another folder.

Command line parameters
========================

  -data <path>
    Store all projects/configuration in folder <path>. By default DBeaver
	stores all its data in user's home ".dbeaver" folder.
	<path> could be an absolute or relative directory name.
	If you want to use DBeaver as redistributable program start it
	with arguments like "dbeaver -data workspace".

  -nl locale
    Use specified locale instead of default one.
    Example: -nl en (use English locale).

  -vm <java vm path>
    Use Java VM installed in <java vm path> folder instead of default
    location.

  -vmargs <jvm parameters>
    Allows to pass any number of additional parameters to JVM.
    Additional parameters may be used to customize environment or
    3-rd party jdbc drivers.

Licensing
==========================
  DBeaver is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

Home Page
==========
  http://dbeaver.jkiss.org

Authors
==========
  Architect, main programmer, team leader - Serge Rieder (serge@jkiss.org)
  Programmer, localizations manager, Russian localization - Eugene Fradkin (eugene.fradkin@gmail.com)
  Chinese localization - Brook.Tran (Brook.Tran.C@gmail.com)
  Italian localization - Roberto Rossi (impiastro@gmail.com)

Support
=======
  For technical support and assistance, you may find necessary information at the Support page
  (http://dbeaver.jkiss.org/forum) or contact us at support@jkiss.org.

Bug Reports
==============
  Send emails to bugs@jkiss.org

Contact us
==============
  support@jkiss.org     - Technical support
  serge@jkiss.org       - Technical support, feature suggestions and any other questions.
