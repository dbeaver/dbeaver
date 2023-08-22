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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.clickhouse.ClickhouseDataTypesParser.EnumEntryContext;
import org.jkiss.dbeaver.ext.clickhouse.ClickhouseDataTypesParser.EnumTypeContext;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class ClickhouseTypeParser {

    private ClickhouseTypeParser() {
        // prevents instantiation
    }

    @NotNull
    public static Map<String, Integer> tryParseEnumEntries(@NotNull String type) {
        if (!isEnum(type)) {
            return Collections.emptyMap();
        }
        var input = CharStreams.fromString(type);
        var ll = new ClickhouseDataTypesLexer(input);
        var tokens = new CommonTokenStream(ll);
        var pp = new ClickhouseDataTypesParser(tokens);
        var tree = pp.enumType();

        if (tree instanceof EnumTypeContext && tree.enumEntryList() != null && tree.enumEntryList().enumEntry() != null) {
            return tree.enumEntryList().enumEntry().stream()
                .filter(node -> node instanceof EnumEntryContext && node.String() != null && node.Number() != null)
                .map(node -> {
                    final var stringValue = node.String().getText();
                    final var key = stringValue.substring(1, stringValue.length() - 1);
                    final var val = CommonUtils.toInt(node.Number().getText());
                    return new Pair<>(key, val);
                })
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
        }

        return Collections.emptyMap();
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
