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
package org.jkiss.dbeaver.parser.common.test;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.jkiss.dbeaver.parser.common.grammar.ExpressionFactory.*;

import java.util.List;

import org.jkiss.dbeaver.parser.common.*;
import org.jkiss.dbeaver.parser.common.grammar.*;

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
            gr.setRule(Rule.expr, seq(call(Rule.opnd), zeroOrMore(call(Rule.op), call(Rule.opnd))));
            gr.setRule(Rule.op, alt("+", "-", "/", "*"));
            gr.setRule(Rule.opnd, alt(call(Rule.brace), call(Rule.numb)));
            gr.setRule(Rule.brace, seq("(", call(Rule.expr), ")"));
            gr.setRule(Rule.numb, regex("[0-9]+"));
            gr.setStartRuleName(Rule.expr);
            this.grammar = gr.buildGrammarInfo();
        }

        public ParseTreeNode node(String name, List<ParseTreeNode> children) {
            return new ParseTreeNode(grammar.findRule(name), null, 0, -1, null, children);
        }

        public ParseTreeNode term(int position, int len) {
            return new ParseTreeNode(null, null, position, position + len, null, List.of());
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
                        c.term(0, 1),
                        c.node(Rule.expr, List.of(
                            c.node(Rule.opnd, List.of(
                                c.node(Rule.numb, List.of(
                                    c.term(1, 1)
                                ))
                            )),
                            c.node(Rule.op, List.of(
                                c.term(2, 1)
                            )),
                            c.node(Rule.opnd, List.of(
                                c.node(Rule.numb, List.of(
                                    c.term(3, 1)
                                ))
                            )),
                            c.node(Rule.op, List.of(
                                c.term(4, 1)
                            )),
                            c.node(Rule.opnd, List.of(
                                c.node(Rule.brace, List.of(
                                    c.term(5, 1),
                                    c.node(Rule.expr, List.of(
                                        c.node(Rule.opnd, List.of(
                                            c.node(Rule.numb, List.of(
                                                c.term(6, 1)
                                            ))
                                        )),
                                        c.node(Rule.op, List.of(
                                            c.term(7, 1)
                                        )),
                                        c.node(Rule.opnd, List.of(
                                            c.node(Rule.numb, List.of(
                                                c.term(8, 1)
                                            ))
                                        )),
                                        c.node(Rule.op, List.of(
                                            c.term(9, 1)
                                        )),
                                        c.node(Rule.opnd, List.of(
                                            c.node(Rule.numb, List.of(
                                                c.term(10, 1)
                                            ))
                                        ))
                                    )),
                                    c.term(11, 1)
                                ))
                            ))
                        )),
                        c.term(12, 1)
                    ))
                )),
                c.node(Rule.op, List.of(
                    c.term(13, 1)
                )),
                c.node(Rule.opnd, List.of(
                    c.node(Rule.numb, List.of(
                        c.term(14, 1)
                    ))
                )),
                c.node(Rule.op, List.of(
                    c.term(15, 1)
                )),
                c.node(Rule.opnd, List.of(
                    c.node(Rule.numb, List.of(
                        c.term(16, 1)
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
                        c.term(0, 1),
                        c.node(Rule.expr, List.of(
                            c.node(Rule.opnd, List.of(
                                c.node(Rule.numb, List.of(
                                    c.term(1, 1)
                                ))
                            )),
                            c.node(Rule.op, List.of(
                                c.term(3, 1)
                            )),
                            c.node(Rule.opnd, List.of(
                                c.node(Rule.numb, List.of(
                                    c.term(5, 1)
                                ))
                            )),
                            c.node(Rule.op, List.of(
                                c.term(7, 1)
                            )),
                            c.node(Rule.opnd, List.of(
                                c.node(Rule.brace, List.of(
                                    c.term(9, 1),
                                    c.node(Rule.expr, List.of(
                                        c.node(Rule.opnd, List.of(
                                            c.node(Rule.numb, List.of(
                                                c.term(10, 1)
                                            ))
                                        )),
                                        c.node(Rule.op, List.of(
                                            c.term(12, 1)
                                        )),
                                        c.node(Rule.opnd, List.of(
                                            c.node(Rule.numb, List.of(
                                                c.term(14, 1)
                                            ))
                                        )),
                                        c.node(Rule.op, List.of(
                                            c.term(20, 1)
                                        )),
                                        c.node(Rule.opnd, List.of(
                                            c.node(Rule.numb, List.of(
                                                c.term(22, 1)
                                            ))
                                        ))
                                    )),
                                    c.term(23, 1)
                                ))
                            ))
                        )),
                        c.term(24, 1)
                    ))
                )),
                c.node(Rule.op, List.of(
                    c.term(26, 1)
                )),
                c.node(Rule.opnd, List.of(
                    c.node(Rule.numb, List.of(
                        c.term(32, 1)
                    ))
                )),
                c.node(Rule.op, List.of(
                    c.term(34, 1)
                )),
                c.node(Rule.opnd, List.of(
                    c.node(Rule.numb, List.of(
                        c.term(36, 1)
                    ))
                ))
            ))
        ));


        Assert.assertEquals(expectedTree.collectString(), tree.get(0).collectString());
    }
    
    @Test
    public void parseWords() {
        GrammarInfoBuilder gb = new GrammarInfoBuilder("stmt");
        gb.setCaseSensitiveTerms(true);
        
        gb.setUseSkipRule(false);
        gb.setRule("sp", regex("[\\s]*"));
        gb.setSkipRuleName("sp");
        gb.setUseSkipRule(true);
        
        gb.setStartRuleName("stmt");
        gb.setRule("stmt", seq("select", call("name"), "from", call("name"), call("filter")));
        gb.setRule("expr", seq(call("name"), ">", call("value")));
        gb.setRule("name", regex("[^\\d\\W][\\w]*"));
        gb.setRule("value", regex("[\\d]+"));
        
        gb.setCaseSensitiveTerms(false);
        gb.setRule("filter", seq("where", call("expr")));
        gb.setCaseSensitiveTerms(true);
        
        Parser p = ParserFactory.getFactory(gb.buildGrammarInfo()).createParser();
        
        // String text = "select x from y WHERE z > 1";
        // System.out.println(p.parse(text).getTrees(false).get(0).collectString(text));
        
        Assert.assertTrue(p.parse("select x from y where z > 1").isSuccess());
        Assert.assertTrue(p.parse("select x from y where z>1").isSuccess());
        Assert.assertFalse(p.parse("selectx fromy wherez>1").isSuccess());
        Assert.assertFalse(p.parse("select xfrom ywherez>1").isSuccess());
        Assert.assertFalse(p.parse("selectx fromy wherez > 1").isSuccess());
    
        Assert.assertTrue(p.parse("select x from y WHERE z > 1").isSuccess());
        Assert.assertFalse(p.parse("select x FROM y where z > 1").isSuccess());
    }
    
    @Test
    public void parseArray() {
        final var gb = new GrammarInfoBuilder("Array");
        gb.setRule("sp", regex("[\\s]*"));
        gb.setSkipRuleName("sp");
        gb.setUseSkipRule(true);
        
        gb.setRule("number", regex("[-]?[0-9]+"));
        gb.setRule("string", regex("'[^'\\\\\\r\\n]*'"));
        
        gb.setRule("array_item", alt(call("number"), call("string"), call("array")));
        gb.setRule("array_item_list", seq(call("array_item"), zeroOrMore(",", call("array_item"))));
        gb.setRule("array", seq("{", optional(call("array_item_list")), "}"));
        
        gb.setStartRuleName("array");
        
        Parser p = ParserFactory.getFactory(gb.buildGrammarInfo()).createParser();
        
        Assert.assertFalse(p.parse("{{}").isSuccess());
        Assert.assertTrue(p.parse("{{}}").isSuccess());
        Assert.assertTrue(p.parse("{{0,2,3},{4,5,6},{7,8,9}}").isSuccess());
        Assert.assertFalse(p.parse("{{0,2,3},{4,5,6},{7,8,9}").isSuccess());

    }
    
    @Test
    public void parseEnum() {
        final var gb = new GrammarInfoBuilder("EnumType");

        gb.setRule("sp", regex("[\\s]*"));
        gb.setSkipRuleName("sp");
        gb.setUseSkipRule(true);

        gb.setRule("string", regex("'[^'\\\\\\r\\n]*'"));
        gb.setRule("number", regex("[-]?[0-9]+"));

        gb.setRule("enum_entry", seq(call("string"), "=", E.call("number")));
        gb.setRule("enum_entry_list", seq(call("enum_entry"), zeroOrMore(",", call("enum_entry"))));
        gb.setRule("enum", seq(regex("enum(8|16)"), "(", call("enum_entry_list"), ")"));

        gb.setStartRuleName("enum");

        Parser p = ParserFactory.getFactory(gb.buildGrammarInfo()).createParser();
        
        Assert.assertTrue(p.parse("Enum8('hello' = 1, 'world' = 2)").isSuccess());
        
    }
}
