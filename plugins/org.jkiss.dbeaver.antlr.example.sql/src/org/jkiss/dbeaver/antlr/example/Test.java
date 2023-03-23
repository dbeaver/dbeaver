package org.jkiss.dbeaver.antlr.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.Tree;
import org.antlr.v4.runtime.tree.Trees;
import org.jkiss.dbeaver.antlr.example.util.CaseChangingCharStream;
import org.jkiss.dbeaver.antlr.example.sql.Sql92;
import org.jkiss.dbeaver.antlr.example.sql.Sql92Lexer;

public class Test {

    public static void main(String[] args) throws IOException {
        // prepareGrammarKeywords();
        
        //var input = CharStreams.fromString("SELECT s.v.a*w.b AS c FROM xxx ORDER BY yyy,zzz");
        var input = CharStreams.fromFileName("D:\\github.com\\dbeaver\\sql-server-sakila-insert-data.sql");
        var ll = new Sql92Lexer(input); //new CaseChangingCharStream(input, true));
        var tokens = new CommonTokenStream(ll);
        //tokens.fill();
        //tokens.getTokens().forEach(t -> System.out.println(t.toString() + " - " + ll.getVocabulary().getSymbolicName(t.getType())));
        
        var pp = new Sql92(tokens);
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
        var stmt = pp.sqlScript();
        
        System.out.println();
        var sb = new StringBuilder();
        collect(stmt, pp, sb, "");
        //System.out.println(sb.toString());
        Files.writeString(Path.of("D:\\github.com\\dbeaver\\sql-server-sakila-insert-data.log"), sb.toString());
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
