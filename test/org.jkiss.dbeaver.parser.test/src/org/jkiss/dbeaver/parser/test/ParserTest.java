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

import java.util.List;

import static org.jkiss.dbeaver.parser.grammar.ExpressionFactory.*;

import org.jkiss.dbeaver.parser.*;
import org.jkiss.dbeaver.parser.grammar.*;

@RunWith(MockitoJUnitRunner.class)
public class ParserTest {

    private static class GrammarCtx {
        public final GrammarInfo grammar;
        
        public GrammarCtx(boolean withSkipRule) {
            GrammarInfoBuilder gr = new GrammarInfoBuilder("test");
            if (withSkipRule) {
                gr.setUseSkipRule(false);
                gr.setRule(Rule.sp, regex("[\\s]*"));
                gr.setSkipRuleName(Rule.sp);
                gr.setUseSkipRule(true);
            }
            gr.setRule(Rule.expr, seq(call(Rule.opnd), any(call(Rule.op), call(Rule.opnd))));
            gr.setRule(Rule.op, alt("+", "-", "/", "*"));
            gr.setRule(Rule.opnd, alt(call(Rule.brace), call(Rule.numb)));
            gr.setRule(Rule.brace, seq("(", call(Rule.expr), ")"));
            gr.setRule(Rule.numb, regex("[0-9]+"));
            gr.setStartRuleName(Rule.expr);
            this.grammar = gr.buildGrammarInfo();
        }

        public ParseTreeNode node(String name, List<ParseTreeNode> children) {
            return new ParseTreeNode(grammar.findRule(name), -1, null, children);
        }

        public ParseTreeNode term(int position) {
            return new ParseTreeNode(null, position, null, List.of());
        }
    }
    
    private static class Rule {
        public static final String s = "s", sp = "sp", expr = "expr", op = "op", opnd = "opnd", brace = "brace", numb = "numb";
    }
    
    @Test
    public void parseExpressions() {
        GrammarCtx c = new GrammarCtx(false);
        
        Parser p = ParserFactory.getFactory(c.grammar).createParser();
        List<ParseTreeNode> tree = p.parse("(1+2+(3*4/5))+6+7").getTrees(false);

        Assert.assertEquals(1, tree.size());

        ParseTreeNode expectedTree = c.node(Rule.s, List.of(
            c.node(Rule.expr, List.of(
                c.node(Rule.opnd, List.of(
                    c.node(Rule.brace, List.of(
                        c.term(0),
                        c.node(Rule.expr, List.of(
                            c.node(Rule.opnd, List.of(
                                c.node(Rule.numb, List.of(
                                    c.term(1)
                                ))
                            )),
                            c.node(Rule.op, List.of(
                                c.term(2)
                            )),
                            c.node(Rule.opnd, List.of(
                                c.node(Rule.numb, List.of(
                                    c.term(3)
                                ))
                            )),
                            c.node(Rule.op, List.of(
                                c.term(4)
                            )),
                            c.node(Rule.opnd, List.of(
                                c.node(Rule.brace, List.of(
                                    c.term(5),
                                    c.node(Rule.expr, List.of(
                                        c.node(Rule.opnd, List.of(
                                            c.node(Rule.numb, List.of(
                                                c.term(6)
                                            ))
                                        )),
                                        c.node(Rule.op, List.of(
                                            c.term(7)
                                        )),
                                        c.node(Rule.opnd, List.of(
                                            c.node(Rule.numb, List.of(
                                                c.term(8)
                                            ))
                                        )),
                                        c.node(Rule.op, List.of(
                                            c.term(9)
                                        )),
                                        c.node(Rule.opnd, List.of(
                                            c.node(Rule.numb, List.of(
                                                c.term(10)
                                            ))
                                        ))
                                    )),
                                    c.term(11)
                                ))
                            ))
                        )),
                        c.term(12)
                    ))
                )),
                c.node(Rule.op, List.of(
                    c.term(13)
                )),
                c.node(Rule.opnd, List.of(
                    c.node(Rule.numb, List.of(
                        c.term(14)
                    ))
                )),
                c.node(Rule.op, List.of(
                    c.term(15)
                )),
                c.node(Rule.opnd, List.of(
                    c.node(Rule.numb, List.of(
                        c.term(16)
                    ))
                ))
            ))
        ));

        Assert.assertEquals(expectedTree.collectString(), tree.get(0).collectString());
    }

    @Test
    public void parseExpressionsWithWhitespaces() {
        GrammarCtx c = new GrammarCtx(true);
        
        Parser p = ParserFactory.getFactory(c.grammar).createParser();
        List<ParseTreeNode> tree = p.parse("(1 + 2 + (3 * 4     / 5)) +     6 + 7").getTrees(false);

        Assert.assertEquals(1, tree.size());

        ParseTreeNode expectedTree = c.node(Rule.s, List.of(
            c.node(Rule.expr, List.of(
                c.node(Rule.opnd, List.of(
                    c.node(Rule.brace, List.of(
                        c.term(0),
                        c.node(Rule.expr, List.of(
                            c.node(Rule.opnd, List.of(
                                c.node(Rule.numb, List.of(
                                    c.term(1)
                                ))
                            )),
                            c.node(Rule.op, List.of(
                                c.term(3)
                            )),
                            c.node(Rule.opnd, List.of(
                                c.node(Rule.numb, List.of(
                                    c.term(5)
                                ))
                            )),
                            c.node(Rule.op, List.of(
                                c.term(7)
                            )),
                            c.node(Rule.opnd, List.of(
                                c.node(Rule.brace, List.of(
                                    c.term(9),
                                    c.node(Rule.expr, List.of(
                                        c.node(Rule.opnd, List.of(
                                            c.node(Rule.numb, List.of(
                                                c.term(10)
                                            ))
                                        )),
                                        c.node(Rule.op, List.of(
                                            c.term(12)
                                        )),
                                        c.node(Rule.opnd, List.of(
                                            c.node(Rule.numb, List.of(
                                                c.term(14)
                                            ))
                                        )),
                                        c.node(Rule.op, List.of(
                                            c.term(20)
                                        )),
                                        c.node(Rule.opnd, List.of(
                                            c.node(Rule.numb, List.of(
                                                c.term(22)
                                            ))
                                        ))
                                    )),
                                    c.term(23)
                                ))
                            ))
                        )),
                        c.term(24)
                    ))
                )),
                c.node(Rule.op, List.of(
                    c.term(26)
                )),
                c.node(Rule.opnd, List.of(
                    c.node(Rule.numb, List.of(
                        c.term(32)
                    ))
                )),
                c.node(Rule.op, List.of(
                    c.term(34)
                )),
                c.node(Rule.opnd, List.of(
                    c.node(Rule.numb, List.of(
                        c.term(36)
                    ))
                ))
            ))
        ));


        Assert.assertEquals(expectedTree.collectString(), tree.get(0).collectString());
    }
    
    @Test
    public void parseWords() {
        GrammarInfoBuilder gb = new GrammarInfoBuilder("stmt");
        gb.setUseSkipRule(false);
        gb.setRule("sp", regex("[\\s]*"));
        gb.setSkipRuleName("sp");
        gb.setUseSkipRule(true);
        gb.setRule("stmt", seq("select", call("name"), "from", call("name"), "where", call("expr")));
        gb.setRule("expr", seq(call("name"), ">", call("value")));
        gb.setRule("name", regex("[^\\d\\W][\\w]*"));
        gb.setRule("value", regex("[\\d]+"));
        gb.setStartRuleName("stmt");
        Parser p = ParserFactory.getFactory(gb.buildGrammarInfo()).createParser();
        
        Assert.assertTrue(p.parse("select x from y where z > 1").isSuccess());
        Assert.assertTrue(p.parse("select x from y where z>1").isSuccess());
        Assert.assertFalse(p.parse("selectx fromy wherez>1").isSuccess());
        Assert.assertFalse(p.parse("select xfrom ywherez>1").isSuccess());
        Assert.assertFalse(p.parse("selectx fromy wherez > 1").isSuccess());
    }
}
