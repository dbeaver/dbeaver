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
package org.jkiss.dbeaver.antlr;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.Tree;
import org.antlr.v4.runtime.tree.Trees;
import org.jkiss.dbeaver.antlr.model.SyntaxModel;
import org.jkiss.dbeaver.antlr.sql.Sql92Lexer;
import org.jkiss.dbeaver.antlr.sql.Sql92Parser;
import org.jkiss.dbeaver.antlr.sql.model.SelectStatement;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.BitSet;


public class Test {

    public static void main(String[] args) throws IOException, XMLStreamException, FactoryConfigurationError, TransformerException {
        // prepareGrammarKeywords();
        
        var input = CharStreams.fromString("SELECT c.s.v.a,w.b AS x " +
            // "FROM xxx yy, (SELECT * FROM ttab) AS sub " +
            "FROM xxx yy, cc.ss.vv AS sub " +
            "cross JOIN aB c " +
            "NATURAL INNER JOIN cd.er ON cd.er.id = aBc.id " +
            "LEFT OUTER join qwe ON qwe.id = aBc.id " +
            "UNION JOIN jd " +
            "ORDER BY yyy,zzz");
        //var input = CharStreams.fromFileName("D:\\github.com\\dbeaver\\sql-server-sakila-insert-data.sql");
        var ll = new Sql92Lexer(input); //new CaseChangingCharStream(input, true));
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
            }
            @Override
            public void reportAttemptingFullContext(Parser arg0, DFA arg1, int arg2, int arg3, BitSet arg4, ATNConfigSet arg5) {
            }
            @Override
            public void reportAmbiguity(Parser arg0, DFA arg1, int arg2, int arg3, boolean arg4, BitSet arg5, ATNConfigSet arg6) {
            }
        });
        //var stmt = pp.sqlScript();
        var tree = pp.queryExpression();
        
        System.out.println();
//        var sb = new StringBuilder();
//        collect(tree, pp, sb, "");
//        System.out.println(sb.toString());
//         Files.writeString(Path.of("c:\\github.com\\dbeaver\\sql-server-sakila-insert-data.log"), sb.toString());
        
        Path dir = Path.of("C:\\Projects\\dbeaver\\dbeaver\\plugins\\org.jkiss.dbeaver.antlr.sql\\src\\org\\jkiss\\dbeaver\\antlr\\sql\\");
        var model = new SyntaxModel(pp);
        model.introduce(SelectStatement.class);
        Files.writeString(dir.resolve("parsed.xml"), model.toXml(tree));

        var stmt = model.map(tree, SelectStatement.class);
        Files.writeString(dir.resolve("model.json"), model.stringify(stmt.model));
    }
    
    private static void collect(Tree ctx, Parser pp, StringBuilder sb, String indent) {
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
            for (Tree t: Trees.getChildren(ctx)) {
                collect(t, pp, sb, indent + "    ");
            }
        }
    }
}
