set xjc="C:\Program Files\Java\jdk1.8.0_181\bin\xjc"

rmdir ./classes/* /s /q

%xjc% -verbose -d ./classes/ -p org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017 -npa -no-header -encoding UTF8 -enableIntrospection -disableXmlSecurity -b ./bindings/sql2017.xjb ./schemas/sql2017/showplanxml.xsd 

%xjc% -verbose -d ./classes/ -p org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2016sp1 -npa -no-header -encoding UTF8 -enableIntrospection -disableXmlSecurity -b ./bindings/sql2016sp1.xjb ./schemas/sql2016sp1/showplanxml.xsd 

%xjc% -verbose -d ./classes/ -p org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2016 -npa -no-header -encoding UTF8 -enableIntrospection -disableXmlSecurity -b ./bindings/sql2016.xjb ./schemas/sql2016/showplanxml.xsd 

%xjc% -verbose -d ./classes/ -p org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2014sp2 -npa -no-header -encoding UTF8 -enableIntrospection -disableXmlSecurity -b ./bindings/sql2014sp2.xjb ./schemas/sql2014sp2/showplanxml.xsd 

%xjc% -verbose -d ./classes/ -p org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2014 -npa -no-header -encoding UTF8 -enableIntrospection -disableXmlSecurity -b ./bindings/sql2014.xjb ./schemas/sql2014/showplanxml.xsd 

%xjc% -verbose -d ./classes/ -p org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2012 -npa -no-header -encoding UTF8 -enableIntrospection -disableXmlSecurity -b ./bindings/sql2012.xjb ./schemas/sql2012/showplanxml.xsd

%xjc% -verbose -d ./classes/ -p org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2008 -npa -no-header -encoding UTF8 -enableIntrospection -disableXmlSecurity -b ./bindings/sql2008.xjb ./schemas/sql2008/showplanxml.xsd

%xjc% -verbose -d ./classes/ -p org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2005sp2 -npa -no-header -encoding UTF8 -enableIntrospection -disableXmlSecurity -b ./bindings/sql2005sp2.xjb ./schemas/sql2005sp2/showplanxml.xsd 

%xjc% -verbose -d ./classes/ -p org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2005 -npa -no-header -encoding UTF8 -enableIntrospection -disableXmlSecurity -b ./bindings/sql2005.xjb ./schemas/sql2005/showplanxml.xsd 