/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.lsm.mapping.SyntaxModel;
import org.jkiss.dbeaver.model.lsm.sql.impl.SelectStatement;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.BitSet;


public class SyntaxParserTest {


    public static void main(String[] args) throws IOException, XMLStreamException, FactoryConfigurationError, TransformerException {
 
        String inputText = "SELECT ALL Product.*, \r\n"
            + "    Product.ProductID AS id,\r\n"
            + "    Product.Name AS ProductName,\r\n"
            + "    Product.ProductNumber,\r\n"
            + "    ProductCategory.Name AS ProductCategory,\r\n"
            + "    ProductSubCategory.Name AS ProductSubCategory,\r\n"
            + "    Product.ProductModelID\r\n"
            + "FROM Production.Product AS Prod(ProductID, Name, ProductNumber) \r\n"
            + "INNER JOIN Production.ProductSubCategory\r\n"
            + "ON ProductSubCategory.ProductSubcategoryID = Product.ProductSubcategoryID\r\n"
            + "UNION JOIN Cat.Production.ProductCategory\r\n"
            + "USING(ProductCategoryID)\r\n"
            + "GROUP BY ProductName\r\n"
            + "ORDER BY Product.ModifiedDate DESC";
        var input = CharStreams.fromString(inputText);
        //var input = CharStreams.fromFileName("D:\\projects\\TestSqlXmlWtf\\TestSqlXmlWtf\\sqlxml-s.xml");
        //var input = CharStreams.fromFileName("D:\\github.com\\dbeaver\\sql-server-sakila-insert-data.sql");
        // input = CharStreams.fromString("SELECT column_name FROM sch.table_name");
        var ll = new Sql92Lexer(input);
        var tokens = new CommonTokenStream(ll);
        tokens.fill();
        // tokens.getTokens().forEach(t -> System.out.println(t.toString() + " - " + ll.getVocabulary().getSymbolicName(t.getType())));
        System.out.println(tokens.getTokens().size());
        
        var pp = new Sql92Parser(tokens);
        pp.addErrorListener(new ANTLRErrorListener() {
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

                System.err.println(sourceName + " line " + line + ":" + charPositionInLine + " " + msg);
                //errorMsg = errorMsg + "\n" + sourceName + "line " + line + ":" + charPositionInLine + " " + msg;
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
        
        var tree = pp.sqlQuery();
        
        { // print simple parse tree view
            System.out.println();
            var sb = new StringBuilder();
            collect(tree, pp, sb, "");
            System.out.println(sb.toString());
        }

        var model = new SyntaxModel(pp);
        var introErrs = model.introduce(SelectStatement.class);
        if (!introErrs.isEmpty()) { 
            introErrs.printToStderr();
        }
        
        var result = model.map(tree, SelectStatement.class);
        if (!result.isNoErrors()) {
            result.getErrors().printToStderr();
        }
        
        System.out.println();
        try { // print human-readable representation of the complete form of the parse tree and model 
            Path dir = Path.of("d:\\Temp");
            Files.writeString(dir.resolve("parsed.xml"), model.toXml(tree));
            Files.writeString(dir.resolve("model.json"), model.stringify(result.getModel()));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        for (int i = 5; i < inputText.length(); i += 5) {
            var resultModel = result.getModel();
            if (resultModel != null) {
                var lr = resultModel.findBoundSyntaxAt(i);
                if (lr != null) {
                    var lri = lr.getInterval();
                    System.out.println(i + " found " + lr + " \t - \"" + inputText.substring(lri.a, lri.b + 1).replace("\r", "\\r").replace("\n", "\\n") + "\" \t - " + lr.getAstNodeFullName());
                } else {
                    System.out.println(i + " not found in 0.." + inputText.length());
                }
            } else {
                System.out.println("\n\rNo model");
            }
        }
        
        
//        var resultModel = result.getModel();
//        var lr = resultModel.findBoundSyntaxAt(11);
//        if (lr != null) {
//            var lri = lr.getInterval();
//            System.out.println(" found " + lr + " \t - \"" + inputText.substring(lri.a, lri.b + 1).replace("\r", "\\r").replace("\n", "\\n") + "\" \t - " + lr.getAstNodeFullName());
//        } else {
//            System.out.println(" not found in 0.." + inputText.length());
//        }
    }
    
    private static void collect(Tree ctx, Parser pp, StringBuilder sb, String indent) {
        // String xtra = ctx instanceof CustomXPathModelNodeBase ? ((CustomXPathModelNodeBase)ctx).getIndex() + "" : "" ;
        
        sb.append(indent).append(Trees.getNodeText(ctx, pp));
        while (ctx.getChildCount() == 1 && !(ctx.getChild(0).getPayload() instanceof Token)) {
            ctx = ctx.getChild(0);
            sb.append(".").append(Trees.getNodeText(ctx, pp));
        }
        sb.append("\n");
        if (ctx.getChildCount() == 1 && ctx.getChild(0).getPayload() instanceof Token) {
            sb.append(indent).append("    \"").append(Trees.getNodeText(ctx.getChild(0), pp)).append("\"\n");
        } else {
            for (Tree t : Trees.getChildren(ctx)) {
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