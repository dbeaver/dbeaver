The classes represented on the diagram are the infrastructure for ANTLR parser usage.


![](/LSMDiagram.png)



Working with ANTLR generated parser starts with extending `SQLStandardParser` with `STMParserOverrides`.

We also override AST Nodes to extend their functionality. We have three types of nodes:
- terminal node (`STMTreeTermNode`)
- non-terminal node (`STMTreeRuleNode`)
- error node (`STMTreeTermErrorNode`)

All of them have the common parent `STMTreeNode` which extends Tree class of ANTLR library.
So, when we parse some text by the grammar, we get an object of type `STMTreeNode`.

The key interface is `LSMAnalyzer`. It encapsulates parser and lexer creation and parsing process.
To parse sql query you need to call `LSMAnalyzer:parseSQLQueryTree` method which has two arguments.
The first argument is `STMSource`. It's responsible for providing the source text to the parser.
The second argument is of type `STMErrorListener` and used to define the behaviour on errors while parsing.
Use `STMLoggingErrorListener` to log errors and `STMSkippingErrorListener` to just skip them and do nothing.

The concrete implementation of `LSMAnalyzer` used according to `SQLDialect` implementation with `LSMDialectRegistry`. `LSMDialectRegistry` contains a map with analyzer to dialect correspondence, which comes from extension point declared in `plugin.xml`.
`SQLStandardAnalyzer` is a common analyzer with parser and lexer by the common grammar for most SQL databases without any specific settings. It's declared via extension point in `org.jkiss.dbeaver.model.sql.plugin.xml`.

```xml
    <extension point="org.jkiss.dbeaver.lsm.dialectSyntax">
        <lsmDialect analyzerClass="org.jkiss.dbeaver.model.lsm.sql.dialect.SQLStandardAnalyzer">
            <appliesTo dialectClass="org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect"/>
        </lsmDialect>
    </extension>
```
If we need to use a common grammar, but with a specific parser configuration, or want to use analyzer with database-specific parser and lexer,
then we need to describe it accordingly using extension point.
The following listing gives an example of configuring parser for specific dialect to use square brackets quotation for identifiers.
```java
public class SQLiteSQLAnalyzer extends SQLStandardAnalyzer {
    @NotNull
    @Override
    protected SQLStandardParser prepareParser(@NotNull STMSource source, @Nullable STMErrorListener errorListener) {
        SQLStandardParser parser = super.prepareParser(source, errorListener);
        parser.setIsSupportSquareBracketQuotation(true);
        return parser;
    }
}
```
Usage of corresponding extension point for this case is the following:
```xml
    <extension point="org.jkiss.dbeaver.lsm.dialectSyntax">
        <lsmDialect analyzerClass="org.jkiss.dbeaver.ext.sqlite.model.SQLiteSQLAnalyzer">
            <appliesTo dialectClass="org.jkiss.dbeaver.ext.sqlite.model.SQLiteSQLDialect"/>
        </lsmDialect>
    </extension>
```