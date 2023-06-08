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

`SQLStandardDialect` is a singleton encapsulating `SQLStandardAnalyzer` with parser and lexer by the common grammar.
It's used to get a `SQLStandardAnalyzer` in implementation of `SQLDialect:getSyntaxAnalyzer` method.
```java
    default public LSMAnalyzer getSyntaxAnalyzer() {
        return SQLStandardDialect.getAnalyzer();
    }
```
If we need to use a common grammar, but with a specific parser configuration,
then this method should be overridden by the corresponding database dialect.
The following listing gives an example of configuring parser for specific dialect to use square brackets quotation for identifiers.
```java

private static final LSMAnalyzer analyzer = new SQLStandardDialect.SQLStandardAnalyzer() {
    @Override
    private SQLStandardParser prepareParser(LSMSource source, STMErrorListener errorListener) {
        SQLStandardParser parser = super.prepareParser(source, errorListener);
        parser.setIsSupportSquareBracketQuotation(true);
        return parser;
    }
};

@Override
public LSMAnalyzer getSyntaxAnalyzer() {
   return analyzer;
}
```