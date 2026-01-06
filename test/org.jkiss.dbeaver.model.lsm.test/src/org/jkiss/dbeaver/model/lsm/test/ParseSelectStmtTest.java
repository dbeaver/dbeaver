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
package org.jkiss.dbeaver.model.lsm.test;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardLexer;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
public class ParseSelectStmtTest extends DBeaverUnitTest {
    
    private static final String _selectStatementsSqlTextResourceName = "SelectStatements.sql.txt";
    
    private static List<String> readStatements(InputStream stream) {
        List<String> result = new LinkedList<>();
        
        try (Scanner scanner = new Scanner(stream)) {
            StringBuilder sb = new StringBuilder();
            
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String trimmed = line.trim();
                if (trimmed.length() > 0) {
                    if (!(trimmed.startsWith("#") || trimmed.startsWith("--"))) {
                        sb.append(line).append(" \n");
                    }
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
            var input = CharStreams.fromString(stmtText);
            var ll = new SQLStandardLexer(input);
            var tokens = new CommonTokenStream(ll);
            tokens.fill();
            
            var pp = new SQLStandardParser(tokens);
            pp.setBuildParseTree(true);
            
            var tree = pp.sqlQuery();
            var noErrors = pp.getNumberOfSyntaxErrors() == 0;
            if (!noErrors) {
                System.err.println();
                System.err.println(stmtText);
                System.err.println();
                
                tokens.getTokens().forEach(t -> System.err.println(t.toString() + " - " + ll.getVocabulary().getSymbolicName(t.getType())));
            }
            Assert.assertTrue(noErrors);
            
//            SyntaxModel model = new SyntaxModel(pp);
//            var ierrs = model.introduce(SelectStatement.class);
//            if (!ierrs.isEmpty()) {
//                ierrs.printToStderr();
//            }
//            Assert.assertTrue(ierrs.isEmpty());
//
//            var result = model.map(tree, SelectStatement.class);
//
//            if (!result.isNoErrors()) {
//                System.err.println();
//                System.err.println(model.stringify(result.getModel()));
//                System.err.println();
//                result.getErrors().printToStderr();
//                System.err.println();
//            }
//            Assert.assertTrue(result.isNoErrors());
        }
    }
}
