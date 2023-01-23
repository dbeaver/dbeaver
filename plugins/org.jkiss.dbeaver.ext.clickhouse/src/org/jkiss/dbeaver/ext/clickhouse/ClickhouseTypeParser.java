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
package org.jkiss.dbeaver.ext.clickhouse;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.parser.common.ParseResult;
import org.jkiss.dbeaver.parser.common.Parser;
import org.jkiss.dbeaver.parser.common.ParserFactory;
import org.jkiss.dbeaver.parser.common.grammar.ExpressionFactory.E;
import org.jkiss.dbeaver.parser.common.grammar.GrammarInfoBuilder;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class ClickhouseTypeParser {
    private static final Parser TYPE_PARSER;

    static {
        final var builder = new GrammarInfoBuilder("EnumType");

        builder.setRule("sp", E.regex("[\\s]*"));
        builder.setSkipRuleName("sp");
        builder.setUseSkipRule(true);

        // FIXME: string does not support escape sequences
        builder.setRule("string", E.regex("'[^'\\\\\\r\\n]*'"));
        builder.setRule("number", E.regex("[-]?[0-9]+"));

        builder.setRule("enum_entry", E.seq(E.call("string"), "=", E.call("number")));
        builder.setRule("enum_entry_list", E.seq(E.call("enum_entry"), E.zeroOrMore(",", E.call("enum_entry"))));
        builder.setRule("enum", E.seq(E.regex("enum(8|16)"), "(", E.call("enum_entry_list"), ")"));

        builder.setStartRuleName("enum");

        TYPE_PARSER = ParserFactory
            .getFactory(builder.buildGrammarInfo())
            .createParser();
    }

    private ClickhouseTypeParser() {
        // prevents instantiation
    }

    @NotNull
    public static Map<String, Integer> tryParseEnumEntries(@NotNull String type) {
        if (!isEnum(type)) {
            return Collections.emptyMap();
        }

        final ParseResult result = TYPE_PARSER.parse(type);

        if (!result.isSuccess()) {
            return Collections.emptyMap();
        }

        return result.getTrees(false).get(0).stream()
            .filter(node -> node.getRule() != null && node.getRule().getName().equals("enum_entry"))
            .map(node -> {
                final var keyNode = node.getChildren().get(0).getChildren().get(0);
                final var valNode = node.getChildren().get(2).getChildren().get(0);

                final var key = type.substring(keyNode.getPosition() + 1, keyNode.getEndPosition() - 1);
                final var val = type.substring(valNode.getPosition(), valNode.getEndPosition());

                return new Pair<>(key, CommonUtils.toInt(val));
            })
            .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
    }

    @NotNull
    public static String getTypeNameWithoutModifiers(@NotNull String fullTypeName) {
        final int div = fullTypeName.indexOf('(');
        if (div < 0) {
            return fullTypeName;
        } else {
            return fullTypeName.substring(0, div);
        }
    }

    private static boolean isEnum(@NotNull String type) {
        final String name = type.toLowerCase();
        return name.startsWith("enum8(") || name.startsWith("enum16(");
    }
}
