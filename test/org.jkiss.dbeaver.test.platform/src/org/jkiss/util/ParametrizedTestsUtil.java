/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.util;

import org.jkiss.code.NotNull;
import org.junit.jupiter.params.provider.Arguments;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ParametrizedTestsUtil {
    private ParametrizedTestsUtil() {
    }

    // combining logic: [a, b], [c, d, e] -> [ac, ad, ae, bc, bd, be]
    // [[1,2], [3,4]], [[a, b], [c, d]] -> [[1, a, b], [1, c, d], [2, a, b] ,... ,[4, c, d]]
    @NotNull
    public static Stream<? extends Arguments> cartesianArguments(
        @NotNull Iterable<? extends Arguments> first,
        @NotNull Iterable<? extends Arguments> second
    ) {
        List<Arguments> combined = new ArrayList<>();
        int totalArgsLength = -1;
        for (Arguments args1 : first) {
            for (Arguments args2 : second) {
                List<Object> args1List = args1.toList();
                List<Object> args2List = args2.toList();

                List<Object> combinedParameters = new ArrayList<>(args1List);
                combinedParameters.addAll(args2List);
                combined.add(Arguments.of(combinedParameters.toArray()));
                int currentArgsLength = args1List.size() + args2List.size();
                if (totalArgsLength == -1) {
                    totalArgsLength = currentArgsLength;
                } else if (totalArgsLength != currentArgsLength) {
                    throw new IllegalArgumentException(
                        "Combined arguments length must be equal. First processed length %d, found argument length %d args1: %s, args2: %s"
                            .formatted(totalArgsLength, currentArgsLength, args1List, args2List));
                }
            }
        }
        return combined.stream();
    }
}
