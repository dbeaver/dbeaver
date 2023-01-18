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
package org.jkiss.dbeaver.parser.common.grammar;

import org.jkiss.code.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ExpressionFactory {
    private ExpressionFactory() {
        // prevents instantiation
    }

    @NotNull
    public static SequenceExpression seq(@NotNull Object... exprs) {
        return new SequenceExpression(makeCollection(exprs));
    }

    @NotNull
    public static RuleExpression call(@NotNull String ruleName) {
        return new RuleCallExpression(ruleName);
    }

    @NotNull
    public static RuleExpression alt(@NotNull Object... exprs) {
        return new AlternativeExpression(makeCollection(exprs));
    }

    @NotNull
    public static RuleExpression num(int min, int max, @NotNull Object... exprs) {
        return new NumberExpression(seq(exprs), min, max);
    }

    @NotNull
    public static RuleExpression optional(@NotNull Object... exprs) {
        return num(0, 1, exprs);
    }

    @NotNull
    public static RuleExpression zeroOrMore(@NotNull Object... exprs) {
        return num(0, Integer.MAX_VALUE, exprs);
    }

    @NotNull
    public static RuleExpression oneOrMore(@NotNull Object... exprs) {
        return num(1, Integer.MAX_VALUE, exprs);
    }

    @NotNull
    public static RegexExpression regex(@NotNull String regex) {
        return new RegexExpression(regex);
    }

    private static RuleExpression makeExpression(@NotNull Object expr) {
        if (expr instanceof String) {
            return new CharactersExpression((String) expr);
        } else if (expr instanceof RuleExpression) {
            return (RuleExpression) expr;
        } else {
            throw new IllegalArgumentException("Unsupported expression: " + expr);
        }
    }

    @NotNull
    private static List<RuleExpression> makeCollection(@NotNull Object... exprs) {
        return Arrays.stream(exprs).map(ExpressionFactory::makeExpression).collect(Collectors.toList());
    }

    /**
     * A helper class to avoid declaring static imports from the factory
     */
    public static class E extends ExpressionFactory {
        private E() {
            // prevents instantiation
        }
    }
}
