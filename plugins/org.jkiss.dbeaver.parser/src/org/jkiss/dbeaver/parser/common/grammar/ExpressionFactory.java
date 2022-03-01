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
package org.jkiss.dbeaver.parser.common.grammar;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ExpressionFactory {

    private static RuleExpression makeExpression(Object expr) {
        if (expr instanceof String) {
            return new CharactersExpression((String) expr);
        } else if (expr instanceof RuleExpression) {
            return (RuleExpression) expr;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static List<RuleExpression> makeCollection(Object... exprs) {
        return Arrays.stream(exprs).filter(e -> e != null).map(e -> makeExpression(e)).collect(Collectors.toList());
    }

    public static SequenceExpression seq(Object... exprs) {
        return new SequenceExpression(makeCollection(exprs));
    }

    public static RuleExpression call(String ruleName) {
        return new RuleCallExpression(ruleName);
    }

    public static RuleExpression optional(Object... exprs) {
        return new NumberExpression(seq(exprs), 0, 1);
    }

    public static RuleExpression alt(Object... exprs) {
        return new AlternativeExpression(makeCollection(exprs));
    }

    public static RuleExpression num(int min, int max, Object... exprs) {
        return new NumberExpression(seq(exprs), min, max);
    }

    public static RuleExpression any(Object... exprs) {
        return new NumberExpression(seq(exprs), 0, Integer.MAX_VALUE);
    }

    public static RuleExpression oneOrMore(Object... exprs) {
        return new NumberExpression(seq(exprs), 1, Integer.MAX_VALUE);
    }

    public static RegexExpression regex(String regex) {
        return new RegexExpression(regex);
    }
}
