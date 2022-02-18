/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.parser.test;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.jkiss.dbeaver.parser.grammar.ExpressionFactory.*;
import org.jkiss.dbeaver.parser.*;
import org.jkiss.dbeaver.parser.grammar.*;

@RunWith(MockitoJUnitRunner.class)
public class ParserTest {

    @Test
    public void parseExpressions() {
        GrammarInfo gr = GrammarInfo.ofRules("test",
            rule("expr", call("opnd"), any(call("op"), call("opnd"))),
            rule("op", alt("+", "-", "/", "*")),
            rule("opnd", alt(call("brace"), call("numb"))),
            rule("brace", "(", call("expr"), ")"),
            rule("numb", regex("[0-9]+"))
        );
        
        Parser p = ParserFabric.getFabric(gr).createParser();
        ParseTreeNode tree = p.parse("(1+2+(3*4/5))+6+7");
        
        String expectedTreeDesc = "s@0\n"
                + "  expr@0\n"
                + "    opnd@0\n"
                + "      brace@0\n"
                + "        $@0\n"
                + "        expr@1\n"
                + "          opnd@1\n"
                + "            numb@1\n"
                + "              $@1\n"
                + "          op@2\n"
                + "            $@2\n"
                + "          opnd@3\n"
                + "            numb@3\n"
                + "              $@3\n"
                + "          op@4\n"
                + "            $@4\n"
                + "          opnd@5\n"
                + "            brace@5\n"
                + "              $@5\n"
                + "              expr@6\n"
                + "                opnd@6\n"
                + "                  numb@6\n"
                + "                    $@6\n"
                + "                op@7\n"
                + "                  $@7\n"
                + "                opnd@8\n"
                + "                  numb@8\n"
                + "                    $@8\n"
                + "                op@9\n"
                + "                  $@9\n"
                + "                opnd@10\n"
                + "                  numb@10\n"
                + "                    $@10\n"
                + "              $@11\n"
                + "        $@12\n"
                + "    op@13\n"
                + "      $@13\n"
                + "    opnd@14\n"
                + "      numb@14\n"
                + "        $@14\n"
                + "    op@15\n"
                + "      $@15\n"
                + "    opnd@16\n"
                + "      numb@16\n"
                + "        $@16\n";
        
        Assert.assertEquals(expectedTreeDesc, tree.collectString());
    }

}
