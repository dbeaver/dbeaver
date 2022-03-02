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
All terminals for outgoing transitions of the FSM state are collected into one regular expression pattern responsible for their on-demand recognition.

## Parsing
Parser moves forward through finite automaton being guided by terminals in the order of recognition and evaluating corresponding context operations until the end of text.
Context is a stack where information about rule call, enter and exit is stored. Parse tree is building after the end of parsing. If the grammar is ambiguous then the result will be more than one parse tree.

### Whitespaces and case-sensivity
So-called skip-rule can be used to declare a name of the rule to apply before any terminal while the rule containing that terminal carries ```useSkipRule``` flag.
It gives a way to implicitly describe whitespaces or comments which are valid to be presented at any point of text across most of grammar rules (see example below).

Word boundaries are automatically respected for terminals of exact-chars kind if terminal starts or ends with alphanumerical character.
It makes much easier to describe something like keywords which are not allowed to stick to each other but allowed to stick to operator characters:
```where X``` - valid
```whereX``` - not valid
```X<3 and``` - valid
```X<3and``` - not valid

Each rule also has ```caseSensitiveTerms``` flag which tells if its terminals should be recognized with respect to case-sensivity of the pattern or not.

### Usage
Example source:
```java
GrammarInfoBuilder gb = new GrammarInfoBuilder("stmt"); // "stmt" - is a name of a start rule
gb.setCaseSensitiveTerms(true);

gb.setUseSkipRule(false); // don't use skip rule before it's not set
gb.setRule("sp", regex("[\\s]*")); // rule with name "sp" defined as regular expression
gb.setSkipRuleName("sp"); // declare skip rule as "sp"
gb.setUseSkipRule(true); // turn on skip rule usage while parsing
        
//rule definitions
gb.setStartRuleName("stmt"); // set "stmt" as start rule
gb.setRule("stmt", seq("select", call("name"), "from", call("name"), call("filter")));
gb.setRule("expr", seq(call("name"), ">", call("value")));
gb.setRule("name", regex("[^\\d\\W][\\w]*"));
gb.setRule("value", regex("[\\d]+"));

gb.setCaseSensitiveTerms(false);
gb.setRule("filter", seq("where", call("expr")));
gb.setCaseSensitiveTerms(true);

Parser p = ParserFactory.getFactory(gb.buildGrammarInfo()).createParser(); //get parser for the grammar

// parse text
String text = "select x from y WHERE z > 1";
ParseResult result = p.parse(text);
// successful parse always produces only one parse tree for unambiguous grammar

// print parse tree
System.out.println(result.getTrees(false).get(0).collectString(text));
```
Example output:
```shell
<NULL> (0-27)
  stmt (0-27)
    'select' (0-6)
    name (6-8)
      'x' (7-8)
    'from' (9-13)
    name (13-15)
      'y' (14-15)
    filter (15-27)
      'WHERE' (16-21)
      expr (21-27)
        name (21-23)
          'z' (22-23)
        '>' (24-25)
        value (25-27)
          '1' (26-27)
```