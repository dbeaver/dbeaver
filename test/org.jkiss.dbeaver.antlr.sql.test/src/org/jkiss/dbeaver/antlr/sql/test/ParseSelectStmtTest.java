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
package org.jkiss.dbeaver.antlr.sql.test;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.jkiss.dbeaver.antlr.model.SyntaxModel;
import org.jkiss.dbeaver.antlr.sql.Sql92Lexer;
import org.jkiss.dbeaver.antlr.sql.Sql92Parser;
import org.jkiss.dbeaver.antlr.sql.model.SelectStatement;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;


@RunWith(MockitoJUnitRunner.class)
public class ParseSelectStmtTest {
    
    private static final String _selectStatementsSqlTextResourceName = "SelectStatements.sql.txt";
    
    private static List<String> readStatements(InputStream stream) {
        List<String> result = new LinkedList<>();
        
        try (Scanner scanner = new Scanner(stream)) {
            StringBuilder sb = new StringBuilder();
            
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String trimmed = line.trim();
                if (trimmed.length() > 0 && !(trimmed.startsWith("#") || trimmed.startsWith("--"))) {
                    sb.append(line).append("\n");
                } else if (sb.toString().trim().length() > 0) {
                    result.add(sb.toString());
                    sb.setLength(0);
                }
            }
        }
        
        return result;
    }
    
    @Test
    public void testModel() throws IOException {
        var statementsToParse = readStatements(ParseSelectStmtTest.class.getResourceAsStream(_selectStatementsSqlTextResourceName));
        
        for (String stmtText : statementsToParse) {
            System.out.println();
            System.out.println(stmtText);
            System.out.println();
            
            var input = CharStreams.fromString(stmtText);
            var ll = new Sql92Lexer(input);
            var tokens = new CommonTokenStream(ll);
            tokens.fill();
            
            var pp = new Sql92Parser(tokens);
            pp.setBuildParseTree(true);
            
            var tree = pp.queryExpression();
            var noErrors = pp.getNumberOfSyntaxErrors() == 0;
            if (!noErrors) {
                tokens.getTokens().forEach(t -> System.out.println(t.toString() + " - " + ll.getVocabulary().getSymbolicName(t.getType())));
            }
            Assert.assertTrue(noErrors);
            
            SyntaxModel model = new SyntaxModel(pp);
            model.introduce(SelectStatement.class);
            var result = model.map(tree, SelectStatement.class);
            
            Assert.assertTrue(result.isOk());
            System.out.println(model.stringify(result.model));
            System.out.println();
        }
    }
}