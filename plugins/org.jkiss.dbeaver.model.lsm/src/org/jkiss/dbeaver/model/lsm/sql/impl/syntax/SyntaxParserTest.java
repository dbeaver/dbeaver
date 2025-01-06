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
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.Tree;
import org.antlr.v4.runtime.tree.Trees;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.lsm.LSMAnalyzerParameters;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser.SqlQueriesContext;
import org.jkiss.dbeaver.model.stm.LSMInspections;
import org.jkiss.dbeaver.model.stm.STMErrorListener;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SyntaxParserTest {

    private static final Log log = Log.getLog(SyntaxParserTest.class);

    public static void main(String[] args) throws IOException, XMLStreamException, FactoryConfigurationError, TransformerException {
 
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

        inputText = "SELECT * FROM EMPLOYEES e WHERE e.EMPLOYEE_ID > :xl ";

        inputText = "select * from test. ";

        var input = CharStreams.fromString(inputText);
        var params = new LSMAnalyzerParameters(
            Map.of("\"", "\""),
            true,
            false,
            '?',
            List.of(Map.entry(1, Set.of(":"))),
            true
        );
        var ll = new SQLStandardLexer(input, params);
        var tokens = new CommonTokenStream(ll);
        tokens.fill();
        
        var pp = new SQLStandardParser(tokens, params);
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

        int pos = 36;
        System.out.println(LSMInspections.prepareTerms(tree));
//        System.out.println(LSMInspections.prepareAbstractSyntaxInspection(tree, pos).getReachabilityByName());
//        System.out.println(inputText.substring(0, pos) + "|" + inputText.substring(pos));
    }

    public static String collect(STMTreeNode ctx) {
        var input = CharStreams.fromString("");
        var params = new LSMAnalyzerParameters(
            Map.of("\"", "\""),
            true,
            false,
            '?',
            List.of(Map.entry(1, Set.of(":"))),
            true
        );
        var ll = new SQLStandardLexer(input, params);
        var tokens = new CommonTokenStream(ll);
        var pp = new SQLStandardParser(tokens, params);

        { // print simple parse tree view
            var sb = new StringBuilder();
            sb.append("\n");
            collect(ctx, pp, sb, "");
            return sb.toString();
        }
    }
    
    private static void collect(STMTreeNode ctx, Parser pp, StringBuilder sb, String indent) {
        sb.append(indent).append(Trees.getNodeText(ctx, pp));
        while (ctx.getChildCount() == 1 && !(ctx.getChild(0).getPayload() instanceof Token)) {
            ctx = ctx.getChildNode(0);
            sb.append(".").append(Trees.getNodeText(ctx, pp));
        }
        sb.append(" [").append(ctx.getRealInterval()).append("]\n");
        if (ctx.getChildCount() == 1 && ctx.getChild(0).getPayload() instanceof Token t) {
            sb.append(indent).append("    ").append(pp.getVocabulary().getDisplayName(t.getType())).append(" \"").append(Trees.getNodeText(ctx.getChild(0), pp)).append("\" [").append(ctx.getChildNode(0).getRealInterval()).append("]\n");
        } else {
            for (STMTreeNode t : ctx.getChildren()) {
                collect(t, pp, sb, indent + "    ");
            }
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
    
    static Collection<Transition> expandToTerms(ATNState state) {                        
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
                case Transition.EPSILON:
                case Transition.RULE: 
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
*/
}