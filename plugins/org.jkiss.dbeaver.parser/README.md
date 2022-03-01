# Parser

## Grammar
The grammar guiding the parser is from a subset of context-free grammars.
Each grammar described with a set of rules.
Each rule described with its own name and expression tree.
There is always a start rule which is the entry point of the parsing process. Start rule corresponds to the whole text being parsed.

Structure of the text decsribed in terms of terminals and non-terminals.
Each terminal represents an atomic sequence of characters.
Two kinds of terminal expressions supported: regex-based terminals and exact characted sequences.
Each non-terminal represents hierarchycal element of the structure of the text and described with the rule.
Rule expression combines subrules and termials into a subsequence of elements being children of the corresponding parse tree node.
Possible structure of this subsequence described by expression tree containing combination of subexpressions:
- ```call``` - subrule element
- ```regex``` - regex-based terminal element
- ```chars``` - exact characters terminal element
- ```seq``` - subsequence of elements in the specified order
- ```alt``` - any one of the specified elements
- ```num``` - some amount of entries of the specified element like
    - ```optional``` - 0 or 1 entries
    - ```any``` - 0 or more entries
    - ```oneOrMore``` - 1 or more entries
    - or any other amount of entries in the explicitly specified range

## Grammar processing
At first, grammar graph is built.
Grammar graph is a non-deterministic finite automaton consisting of grammar rules and special states encoding operations on rules like rule enter, exit and call.
After that grammar graph translated into finite automaton by terminals specifying parser flow.
All terminals for outgoing transitions of the FSM state are collected into one regular expression responsible for their on-demand recognition.

## Parsing
Parser moves forward through finite automaton being guided by terminals in the order of recognition and evaluating corresponding context operations until the end of text.
Context is a stack where information about rule call, enter and exit is stored. Parse tree is building after the end of parsing. If the grammar is ambiguous then the result will be more than one parse tree.

### Whitespaces
So-called skip-rule can be used to declare a name of the rule to apply before any terminal while the rule containing that terminal carries ```useSkipRule``` flag.
It gives a way to implicitly describe whitespaces or comments which are valid to be presented at any point of text across most of grammar rules.

### Usage
```java
GrammarInfoBuilder gb = new GrammarInfoBuilder("stmt"); // "stmt" - is a name of a start rule
gb.setUseSkipRule(false); // don't use skip rule before it's not set
gb.setRule("sp", regex("[\\s]*")); // rule with name "sp" defined as regular expression
gb.setSkipRuleName("sp"); // declare skip rule as "sp"
gb.setUseSkipRule(true); // turn on skip rule usage while parsing
        
//rule definitions
gb.setRule("stmt", seq("select", call("name"), "from", call("name"), "where", call("expr")));
gb.setRule("expr", seq(call("name"), ">", call("value")));
gb.setRule("name", regex("[^\\d\\W][\\w]*"));
gb.setRule("value", regex("[\\d]+"));

gb.setStartRuleName("stmt"); // set "stmt" as start rule
Parser p = ParserFactory.getFactory(gb.buildGrammarInfo()).createParser(); //get parser by the grammar from factory

// parse text and get parse tree for recognition
// for unambiguios grammar there is only one element in collection
List<ParseTreeNode> tree = p.parse("select x from y where z > 1")
        
// get parse result
p.parse("select x from y where z > 1").isSuccess()
</code>
```
Parse tree for provided example:
```shell
stmt
  ├─'select' (pos:0, len:6)    
  ├─name
  │ └─'x' (pos:7, len:1)
  ├─'from' (pos:9, len:4)
  ├─name
  │ └─'y' (pos:14, len:1)
  ├─'where' (pos:16, len:5)
  └─expr
    ├─name
    │ └─'z' (pos:22, len:1)
    ├─'>' (pos:24, len:1)
    └─value
      └─1 (pos:26, len:1)
```