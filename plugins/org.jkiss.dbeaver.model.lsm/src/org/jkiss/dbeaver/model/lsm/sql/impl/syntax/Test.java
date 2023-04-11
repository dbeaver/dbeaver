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
import java.util.BitSet;


public class Test {

    public static void main(String[] args) throws IOException, XMLStreamException, FactoryConfigurationError, TransformerException {
        // prepareGrammarKeywords();
        
    	
        var input = CharStreams.fromString("SELECT ALL\r\n"
            + "    Product.ProductID,\r\n"
            + "    Product.Name AS ProductName,\r\n"
            + "    Product.ProductNumber,\r\n"
            + "    ProductCategory.Name AS ProductCategory,\r\n"
            + "    ProductSubCategory.Name AS ProductSubCategory,\r\n"
            + "    Product.ProductModelID\r\n"
            + "FROM Production.Product\r\n"
            + "INNER JOIN Production.ProductSubCategory\r\n"
            + "ON ProductSubCategory.ProductSubcategoryID = Product.ProductSubcategoryID\r\n"
            + "UNION JOIN Production.ProductCategory\r\n"
            + "ON ProductCategory.ProductCategoryID = ProductSubCategory.ProductCategoryID\r\n"
            + "ORDER BY Product.ModifiedDate DESC"
        );
        //var input = CharStreams.fromFileName("D:\\github.com\\dbeaver\\sql-server-sakila-insert-data.sql");
        var ll = new Sql92Lexer(input);
        var tokens = new CommonTokenStream(ll);
        tokens.fill();
        tokens.getTokens().forEach(t -> System.out.println(t.toString() + " - " + ll.getVocabulary().getSymbolicName(t.getType())));
        
        var pp = new Sql92Parser(tokens);
        pp.setBuildParseTree(true);
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
            }

            @Override
            public void reportAttemptingFullContext(Parser arg0, DFA arg1, int arg2, int arg3, BitSet arg4, ATNConfigSet arg5) {
                // just illustration of listeners possibility
            }

            @Override
            public void reportAmbiguity(Parser arg0, DFA arg1, int arg2, int arg3, boolean arg4, BitSet arg5, ATNConfigSet arg6) {
                // just illustration of listeners possibility
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
        model.introduce(SelectStatement.class);
        
        
        var result = model.map(tree, SelectStatement.class);
        
        System.out.println();
        { // print human-readable representation of the complete form of the parse tree and model 
            //Path dir = Path.of("C:\\Projects\\dbeaver\\dbeaver\\plugins\\org.jkiss.dbeaver.model.sql.lsm.syntax.grammar\\src\\org\\jkiss\\dbeaver\\antlr\\sql\\");
            //Files.writeString(dir.resolve("parsed.xml"), model.toXml(tree));
            //Files.writeString(dir.resolve("model.json"), model.stringify(result.getModel()));
        }
    }
    
    private static void collect(Tree ctx, Parser pp, StringBuilder sb, String indent) {
        // String xtra = ctx instanceof CustomXPathModelNodeBase ? ((CustomXPathModelNodeBase)ctx).getIndex() + "" : "" ;
        
        sb.append(indent).append(Trees.getNodeText(ctx, pp));
        Object p = null;
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
}