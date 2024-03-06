/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.lsm.sql.impl.syntax;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.AtomTransition;
import org.antlr.v4.runtime.atn.ParserATNSimulator;
import org.antlr.v4.runtime.atn.RangeTransition;
import org.antlr.v4.runtime.atn.RuleStartState;
import org.antlr.v4.runtime.atn.RuleStopState;
import org.antlr.v4.runtime.atn.RuleTransition;
import org.antlr.v4.runtime.atn.SetTransition;
import org.antlr.v4.runtime.atn.Transition;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Tree;
import org.antlr.v4.runtime.tree.Trees;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser.SqlQueriesContext;
import org.jkiss.dbeaver.model.stm.STMErrorListener;
import org.jkiss.dbeaver.model.stm.STMTreeRuleNode;
import org.jkiss.dbeaver.model.stm.STMTreeTermNode;
import org.jkiss.dbeaver.utils.ListNode;
import org.jkiss.utils.Pair;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SyntaxParserTest {

    private static final Log log = Log.getLog(SyntaxParserTest.class);

    static void CompletionTest() {       
        
        var input = CharStreams.fromString("select * from tab where x < 2");
        var ll = new SQLStandardLexer(input, Map.of("'", "'"));
        var tokens = new CommonTokenStream(ll);
        tokens.fill();
        for (Token t: tokens.getTokens()) {
            System.out.println(t.getTokenIndex() + ": " + t.getText());
        }
        var recognizer = new SQLStandardParser(tokens);
        SqlQueriesContext tree = recognizer.sqlQueries();
        { // print simple parse tree view
            var sb = new StringBuilder();
            sb.append("\n");
            collect(tree, recognizer, sb, "");
            System.out.println(sb.toString());
        }
     
        
//        RuleNode rn;
//        var x = rn.getRuleContext().invokingState;
        
        var exceptRules = Set.of(SQLStandardParser.RULE_nonReserved, SQLStandardParser.RULE_anyUnexpected, SQLStandardParser.RULE_aggregateExprParam, SQLStandardParser.RULE_anyWord, SQLStandardParser.RULE_tableHintKeywords);
        
        var node = findNode(tree, t -> t instanceof STMTreeTermNode && t.getPayload() instanceof Token tk && tk == tokens.get(6));
        
//        ATNState s = recognizer.getATN().states.get(recognizer.getATN().ruleToStartState[SQLStandardParser.RULE_sqlQueries].stateNumber);
//        ATNState s = recognizer.getATN().states.get(654).getTransitions()[0].target;
        ATNState s = recognizer.getATN().states.get(((STMTreeTermNode)node).getAtnState()).getTransitions()[0].target;
//        ATNState s = recognizer.getATN().states.get(0).getTransitions()[0].target;
//        ATNState s = recognizer.getATN().states.get(tokens.getTokens().get(7)
        var stack = ListNode.<Integer>of(null);
        {
            var path = new LinkedList<RuleNode>();
            node = node.getParent();
            while (node != null && node instanceof RuleNode rn) {
                path.addFirst(rn);
                node = node.getParent();
            }
            for (RuleNode rn: path) {
                stack = ListNode.push(stack, rn.getRuleContext().getRuleIndex());
            }
        }
        
        // Collection<Transition> tt = expandToTerms(recognizer, s, Set.of("nonReserved", "anyUnexpected", "aggregateExprParam", "anyWord", "tableHintKeywords"));
        Collection<String> transitionTokens = collectFollowingTerms(recognizer, stack, s, exceptRules);
        System.out.println("expected one of " + transitionTokens.size() + " : " + String.join(", ", transitionTokens));
        
        
//        var ccc = new CodeCompletionCore(recognizer, null, null, exceptRules);
//        var cc = ccc.collectCandidates(7, null);
//        System.out.println(cc.tokens.size());
//        for (var kv: cc.tokens.entrySet()) {
//            if (kv.getKey() >= 0) {
//                System.out.println(recognizer.getTokenNames()[kv.getKey()]);
//            }
//        }
//        
    }
    
    public static void main(String[] args) throws IOException, XMLStreamException, FactoryConfigurationError, TransformerException {
        CompletionTest();
        if (true) {
            return;
        }
 
        String inputText = "SELECT ALL Product.*, \r\n"
            + "    Product.ProductID AS id,\r\n"
            + "    Product.Name AS ProductName,\r\n"
            + "    Product.ProductNumber,\r\n"
            + "    ProductCategory.Name AS ProductCategory,\r\n"
            + "    ProductSubCategory.Name AS ProductSubCategory,\r\n"
            + "    Product.ProductModelID\r\n"
            + "FROM Production.Product AS Prod(ProductID, Name, ProductNumber), T as A, \r\n"
            + "s -- ololo\r\n"
            + ".c -- ololo\r\n"
            + ".t -- ololo\r\n"
            + "as x\r\n"
            + "INNER JOIN Production.ProductSubCategory as A2\r\n"
            + "INNER JOIN (SELECT column_name FROM sch.table_name INNER JOIN T2 as A22 WHERE id > 3) as A3\r\n"
            + "ON ProductSubCategory.ProductSubcategoryID = Product.ProductSubcategoryID\r\n"
            + "UNION JOIN Cat.Production.ProductCategory\r\n"
            + "USING(ProductCategoryID)\r\n"
            + "GROUP BY ProductName\r\n"
            + "ORDER BY Product.ModifiedDate DESC";
//        inputText = "\n\rSELECT schedule[1:2][1:1] FROM sal_emp se where s;";
        
        inputText = "\n"
                + "SELECT BusinessEntityID, TerritoryID,   \n"
                + "    CONVERT(VARCHAR(20),SUM(SalesYTD) OVER ("
                + "          PARTITION BY TerritoryID   \n"
                + "          ORDER BY DATEPART(yy,ModifiedDate)   \n"
                + "          ROWS BETWEEN current_row AND 1 FOLLOWING \n"
                + "    ),1) AS CumulativeTotal  \n"
                + "FROM Sales.SalesPerson  \n"
                + "WHERE TerritoryID IS NULL OR TerritoryID < 5";

        inputText = "select "
                + " c.id"
                + " c.name,"
                + " c.title,"
                + " c.updated,"
                + " c.name "
                + ",(select json_aggr(distinct aafe order by 2 limit 50 separator 'f')\n"
                + "   from order_products_rewards\n"
                + "   where order_id = c.order_id\n"
                + "   group by order_id) fdi\n"
                + " from contracts c"
                + "where date(c.updated) = date(sysdate())\n"
                + "";
        
        inputText = "SELECT City, STRING_AGG(CONVERT(NVARCHAR(max), EmailAddress)s ';') FILTER (where a < b) AS Emails \n"
                + " FROM Person.BusinessEntityAddress AS BEA  \n"
                + " INNER JOIN Person.Address AS A ON BEA.AddressID = A.AddressID\n"
                + " INNER JOIN Person.EmailAddress AS EA ON BEA.BusinessEntityID = EA.BusinessEntityID \n"
                + " GROUP BY City";
        var input = CharStreams.fromString(inputText);
        var ll = new SQLStandardLexer(input, Map.of("'", "'"));
        var tokens = new CommonTokenStream(ll);
        tokens.fill();
        
        var pp = new SQLStandardParser(tokens);
        pp.addErrorListener(new STMErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, 
                Object offendingSymbol, 
                int line, 
                int charPositionInLine, 
                String msg, 
                RecognitionException re)  {
                String sourceName = recognizer.getInputStream().getSourceName();
                if (!sourceName.isEmpty()) {
                    sourceName = String.format("%s:%d:%d: ", sourceName, line, charPositionInLine);
                }

                System.out.println(sourceName + " line " + line + ":" + charPositionInLine + " " + msg);
            }

            @Override
            public void reportContextSensitivity(Parser arg0, DFA arg1, int arg2, int arg3, int arg4, ATNConfigSet arg5) {
                // just illustration of listeners possibility
//                System.out.println("reportContextSensitivity");
            }

            @Override
            public void reportAttemptingFullContext(Parser arg0, DFA arg1, int arg2, int arg3, BitSet arg4, ATNConfigSet arg5) {
                // just illustration of listeners possibility
//                System.out.println("reportAttemptingFullContext");
            }

            @Override
            public void reportAmbiguity(Parser arg0, DFA arg1, int arg2, int arg3, boolean arg4, BitSet arg5, ATNConfigSet arg6) {
                // just illustration of listeners possibility
//                System.out.println("reportAmbiguity");
            }
        });
        SqlQueriesContext tree = pp.sqlQueries();
        
        //System.out.println("err: " + tree.getRuleContexts(TableExpressionContext.class).get(0).err.getText());
        
        String str = tree.getTextContent();

        System.out.println(str);
        
        { // print simple parse tree view
            var sb = new StringBuilder();
            sb.append("\n");
            collect(tree, pp, sb, "");
            System.out.println(sb.toString());
        }
        
    }
    
    private static void collect(Tree ctx, Parser pp, StringBuilder sb, String indent) {        
        sb.append(indent).append(Trees.getNodeText(ctx, pp));
        while (ctx.getChildCount() == 1 && !(ctx instanceof STMTreeTermNode)) {
            sb.append(".").append(Trees.getNodeText(ctx, pp));
            ctx = ctx.getChild(0);
        }
        sb.append("\n");
        if (ctx.getChildCount() == 0 && ctx instanceof STMTreeTermNode) { //.getPayload() instanceof Token) {
            STMTreeTermNode tn = (STMTreeTermNode)ctx;
            sb.append(indent).append("    S").append(tn.getAtnState()).append(" \"").append(Trees.getNodeText(ctx, pp)).append("\"\n");
        } else {
            for (Tree t : Trees.getChildren(ctx)) {
                collect(t, pp, sb, indent + "    ");
            }
        }
    }
    
    public static Tree findNode(Tree ctx, Predicate<Tree> test) {
        if (test.test(ctx)) {
            return ctx;
        } else {
            for (Tree t : Trees.getChildren(ctx)) {
                Tree result = findNode(t, test);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }
    }
    
/*    
    static void setErrorHandler(Parser pp) {
        pp.setErrorHandler(new DefaultErrorStrategy( ) {
            @Override
            public void recover(Parser recognizer, RecognitionException e) {
                System.out.println("offending token: " + recognizer.getCurrentToken().getText());
                System.out.println("offending token: " + e.getOffendingToken().getText());
                System.out.println("expected one of " + e.getExpectedTokens().toString(recognizer.getVocabulary()));
                
                ATNState s = recognizer.getInterpreter().atn.states.get(recognizer.getState());
//                Collection<Transition> tt = expandToTerms(s);
//                IntervalSet tokens = getTransitionTokens(tt);
//                System.out.println("expected one of " + tokens.toString(recognizer.getVocabulary()));
//                
                var mySimulator = new ParserATNSimulator(recognizer.getATN(), recognizer.getInterpreter().decisionToDFA, recognizer.getInterpreter().getSharedContextCache()) {
                    public void doMyFancyWork(Token from, Token to) {
                        ATNConfigSet currState = this.computeStartState(s, recognizer.getContext(), true);
                        ATNConfigSet nextState = null;
                        for (int i = from.getTokenIndex(); i < to.getTokenIndex(); i++, currState = nextState) {
                            Token t = recognizer.getTokenStream().get(i);
                            System.out.println(i + ": " + currState);
                            try {
                                nextState = computeReachSet(currState, t.getType(), true);
                            } catch (Throwable ex) {
                                e.printStackTrace();
                                nextState = null;
                                break;
                            }
                        }
                        for (ATNState state: currState.getStates()) {
                            Collection<Transition> tt = expandToTerms(state);
                            IntervalSet tokens = getTransitionTokens(tt);
                            System.out.println("expected one of " + tokens.toString(recognizer.getVocabulary()));
                        }
                    }
                };
                mySimulator.doMyFancyWork(recognizer.getCurrentToken(), e.getOffendingToken());
                
                if (true) {
                    throw new RuntimeException();
                }
//                super.recover(recognizer, e); 
//                CommonTokenStream tokens = (CommonTokenStream) recognizer.getTokenStream(); 
//                // verify current token is not EOF 
//                if (tokens.LA(1) != recognizer.EOF) {
//                    tokens.consume(); 
//                }
            }
        });
    }
    */

    static IntervalSet getTransitionTokens(Collection<Transition> transitions) {
        IntervalSet tokens = new IntervalSet();
        for (Transition transition: transitions) {
            switch (transition.getSerializationType()) {
                case Transition.ATOM: 
                {
                    tokens.add(((AtomTransition)transition).label);
                    break;
                }
                case Transition.RANGE:
                {
                    RangeTransition t = (RangeTransition)transition;
                    Interval trange = Interval.of(t.from, t.to);
                    tokens.add(t.from, t.to);
                    break;
                }
                case Transition.SET:
                    tokens.addAll(((SetTransition)transition).set);
                    break;
                case Transition.NOT_SET: 
                case Transition.WILDCARD:
                    // matches "anything" so don't consider them
                    break;
                case Transition.EPSILON:
                case Transition.RULE: 
                    // is not responsible for matching, so ignore them
                case Transition.PREDICATE:
                case Transition.ACTION:
                case Transition.PRECEDENCE:
                    // doesn't describe matching in terms of tokens, so ignore them
                default:
                    throw new UnsupportedOperationException("Unrecognized ATN transition type.");
            }
        }
        return tokens;
    }
    
    static Collection<Transition> expandToTerms(Parser recognizer, ATNState state, Set<String> except) {                        
        HashSet<Transition> visited = new HashSet<>();
        HashSet<Transition> results = new HashSet<>();
        LinkedList<Transition> q = new LinkedList<>();
        for (Transition t: state.getTransitions()) {
            q.addLast(t);
        }
        while (q.size() > 0) {
            Transition transition = q.removeFirst();
            switch (transition.getSerializationType()) {
                case Transition.ATOM:
                case Transition.RANGE:
                case Transition.SET:
                case Transition.NOT_SET: 
                case Transition.WILDCARD: 
                    results.add(transition);
                    break;
                case Transition.RULE: {
                    var rt = (RuleTransition)transition;
                    String ruleName = recognizer.getRuleNames()[rt.ruleIndex];
                    if (except.contains(ruleName)) 
                        break;
                }
                case Transition.EPSILON:
                case Transition.PREDICATE:
                case Transition.ACTION:
                case Transition.PRECEDENCE:
                {
                    for (Transition t: transition.target.getTransitions()) {
                        if (visited.add(t)) {
                            q.addLast(t);
                        }
                    }
                    break;
                }
                default:
                    throw new UnsupportedOperationException("Unrecognized ATN transition type.");
            }
        }
        return results;
    }

    static final Map<Integer, Set<Integer>> subrulesByRule = new HashMap<>();
    
    private static Set<Integer> findSubrules(Parser pp, int ruleIndex) {
        return subrulesByRule.computeIfAbsent(ruleIndex, x -> {
            HashSet<Integer> result = new HashSet<>();
//            HashSet<ATNState> visited = new HashSet<>();
//            LinkedList<ATNState> q = new LinkedList<>();
//            q.addLast(pp.getATN().ruleToStartState[x]);
//            while (q.size() > 0) {
//                var s = q.removeFirst();
//                if (visited.add(s)) {
//                    if (s instanceof RuleStartState rs && rs.ruleIndex != ruleIndex) {
//                        result.add(rs.ruleIndex);
//                    } else if (s instanceof RuleStopState) {
//                        // do nothing
//                    } else {
//                        for (var t: s.getTransitions()) {
//                            q.addLast(t.target);
//                        }
//                    }
//                }
//            }
            for (var s: pp.getATN().states) {
                if (s.ruleIndex == ruleIndex) {
                    for (var t: s.getTransitions()) {
                        if (t.target instanceof RuleStartState rs && rs.ruleIndex != ruleIndex) {
                            result.add(rs.ruleIndex);
                        }
                    }
                }
            }
//            System.out.println("subrules of " + pp.getRuleNames()[x] + ": " + String.join(
//                    ", ", result.stream().map(r -> pp.getRuleNames()[r]).collect(Collectors.toList())
//            ));
            return result;
        });
    }
    
    private static Collection<String> collectFollowingTerms(Parser pp, ListNode<Integer> stateStack,  ATNState state, Set<Integer> exceptRules) {
        {
            LinkedList<String> path = new LinkedList<String>();
            stateStack.forEach(rn -> { if (rn != null) path.addFirst(pp.getRuleNames()[rn]); });
            System.out.println(String.join(".", path));
            System.out.println();
        }
        HashSet<Transition> visited = new HashSet<>();
        HashSet<String> results = new HashSet<>();
        LinkedList<Pair<Transition, ListNode<Integer>>> q = new LinkedList<>();
        for (Transition t: state.getTransitions()) {
            q.addLast(Pair.of(t, stateStack));
        }
        while (q.size() > 0) {
            Pair<Transition, ListNode<Integer>> pair = q.removeLast();
            Transition transition = pair.getFirst();
            ListNode<Integer> stack = pair.getSecond();

            transition:
            switch (transition.getSerializationType()) {
                case Transition.ATOM:
                case Transition.RANGE:
                case Transition.SET:
                case Transition.NOT_SET: 
                case Transition.WILDCARD:
                    {
                        LinkedList<String> path = new LinkedList<String>();
                        stack.forEach(rn -> { if (rn != null) path.addFirst(pp.getRuleNames()[rn]); });
                        System.out.println(String.join(".", path));
                    
                        IntervalSet transitionTokens = getTransitionTokens(List.of(transition));
                        for (Interval interval: transitionTokens.getIntervals()) {
                            int a = interval.a;
                            int b = interval.b;
                            for (int v = a; v <= b; v++) {
                                String word = pp.getVocabulary().getDisplayName(v);
                                results.add(word);
                                System.out.println("\t" + word);
                            }
                        }
                    }
                    break;
                case Transition.RULE:
                {
                    RuleTransition rt = (RuleTransition)transition;
                    if (exceptRules.contains(rt.ruleIndex)) {
                        break;
                    }
                    // fallthrough
                }
                case Transition.EPSILON:
                case Transition.PREDICATE:
                case Transition.ACTION:
                case Transition.PRECEDENCE:
                {
                    LinkedList<String> path = new LinkedList<String>();
                    stack.forEach(rn -> { if (rn != null) path.addFirst(pp.getRuleNames()[rn]); });
//                    if (targetName != null && targetName.equals("insertColumnsAndSource")) {
//                        System.out.println("WTF");
//                    }
                    
                    switch (transition.target.getStateType()) {
                    case ATNState.RULE_STOP: 
                        if (stack != null && stack.data != null && ((RuleStopState)transition.target).ruleIndex == stack.data.intValue()) {
                            {
                                String targetName = pp.getRuleNames()[((RuleStopState)transition.target).ruleIndex];
                                System.out.println("EXITING TO " + targetName + " AT " + String.join(".", path));
                            }
                            stack = stack.next; // pop 
                        } else {
//                            {
//                                String targetName = pp.getRuleNames()[((RuleStopState)transition.target).ruleIndex];
//                                System.out.println("NOT EXITING TO " + targetName + " AT " + String.join(".", path));
//                            }
                            break transition;
                        }
                        break;
                    case ATNState.RULE_START:
                        if (stack == null || stack.data == null || findSubrules(pp, stack.data).contains(((RuleStartState)transition.target).ruleIndex)) {
                            stack = ListNode.push(stack, ((RuleStartState)transition.target).ruleIndex);
                            {
                                String targetName = pp.getRuleNames()[((RuleStartState)transition.target).ruleIndex];
                                System.out.println("ENTERING " + targetName + " AT " + String.join(".", path));
                            }
                        } else {
//                            {
//                                String targetName = pp.getRuleNames()[((RuleStartState)transition.target).ruleIndex];
//                                System.out.println("NOT ENTERING " + targetName + " AT " + String.join(".", path));
//                            }
                            break transition;
                        }
                        break;
                    }
                    for (Transition t: transition.target.getTransitions()) {
                        if (visited.add(t)) {
                            q.addLast(Pair.of(t, stack));
                        }
                    }                    
                    break;
                }
                default:
                    throw new UnsupportedOperationException("Unrecognized ATN transition type.");
            }
        }
        return results;
    }
    
}