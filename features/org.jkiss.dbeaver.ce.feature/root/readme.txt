${dbeaver-product}

${dbeaver-version}

README

Thank you for downloading DBeaver!

Installing DBeaver
========================
  Use automatic installer distribution (Windows and Linux versions)
  or just unpack archive and start "dbeaver" executable.
  
  Note: do not extract archive in the same folder where older version
  of DBeaver is installed.
  Remove previous version or extract archive in another folder.

Command line parameters
========================

  -f <sql-file1 [sql-file2..]>
    Open specified SQL file(s) in SQL editor.
    This command can be used to associate SQL files with DBeaver in shell.

  -nosplash
    Do not show splash screen

  -data <path>
    Store all projects/configuration in folder <path>. By default DBeaver
	stores all its data in user's home "DBeaverData" folder.
	<path> can be an absolute or relative directory name.
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

  -clean
    Clears all Eclipse caches. Use it if DBeaver fails to start after version upgrade.

Licensing
==========================
  Apache License 2 (http://www.apache.org/licenses/LICENSE-2.0)

Web
==========
  Main web site: https://dbeaver.io
  Source code: https://github.com/dbeaver/dbeaver
  Issue tracker: https://github.com/dbeaver/dbeaver/issues

  Please use our issue tracker for technical support, feature suggestions and any other questions
