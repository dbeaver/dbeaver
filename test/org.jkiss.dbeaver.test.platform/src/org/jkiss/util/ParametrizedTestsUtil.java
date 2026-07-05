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

    @NotNull
    public static Stream<? extends Arguments> combineArguments(
        @NotNull Iterable<? extends Arguments> first,
        @NotNull Iterable<? extends Arguments> second
    ) {
        List<Arguments> combined = new ArrayList<>();
        for (Arguments args1 : first) {
            for (Arguments args2 : second) {
                List<Object> combinedParameters = new ArrayList<>(args1.toList());
                combinedParameters.addAll(args2.toList());
                combined.add(Arguments.of(combinedParameters.toArray()));
            }
        }
        return combined.stream();
    }
}
