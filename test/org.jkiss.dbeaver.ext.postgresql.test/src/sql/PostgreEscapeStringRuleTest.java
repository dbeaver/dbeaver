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
package org.jkiss.dbeaver.ext.postgresql.sql;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.text.parser.*;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;




@RunWith(MockitoJUnitRunner.class)
public class PostgreEscapeStringRuleTest {

    private static final Logger log = LoggerFactory.getLogger(SyntaxParserTest.class);

    @Mock
    TPCharacterScanner scannerMock;
    @Test
    public void testEvaluate() {
         PostgreEscapeStringRule obj = new PostgreEscapeStringRule();

         boolean resume = false;

         // Mock the TPCharacterScanner
         try {


             when(scannerMock.getColumn()).thenReturn(0);
             when(scannerMock.read()).thenReturn(TPCharacterScanner.EOF);

             // Call the method under test
             TPToken result = obj.evaluate(scannerMock, resume);

             // Assert that the result matches the expected token
             assertEquals(SQLTokenType.T_STRING, result);

             // Verify that the expected methods were called on the scanner
             verify(scannerMock).getColumn();
             verify(scannerMock, times(4)).read();
             verify(scannerMock, times(2)).unread();
         } catch (Exception e) {
             log.error("Error"+ e);

         }
    }

}

