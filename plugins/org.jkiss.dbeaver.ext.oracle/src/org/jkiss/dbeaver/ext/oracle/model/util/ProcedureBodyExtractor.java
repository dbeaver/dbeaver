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
package org.jkiss.dbeaver.ext.oracle.model.util;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.oracle.model.OracleProcedurePackaged;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcedureBodyExtractor {

    public static final String NO_DEFINITION_FOUND = "-- no definition found";
    private static final List<String> possibleBeginCases = List.of(
        "begin",
        "IF",
        "CASE",
        "LOOP",
        "FOR",
        "WHILE"
    );

    private final OracleProcedurePackaged proc;
    private final String parentPackageBodyDefinition;

    private final Deque<Integer> beginStack = new ArrayDeque<>();
    private final Deque<Integer> endStack = new ArrayDeque<>();

    private Matcher beginMatcher;
    private Matcher endMatcher;


    public ProcedureBodyExtractor(@NotNull OracleProcedurePackaged proc, @NotNull String parentPackageBodyDefinition) {
        this.proc = proc;
        this.parentPackageBodyDefinition = parentPackageBodyDefinition;
    }


    public String extractProcBody() {
        String procType = procType();
        if (procType != null) {
            Matcher procStart = Pattern
                .compile(procType + "\\s+" + proc.getUniqueName(), Pattern.CASE_INSENSITIVE)
                .matcher(parentPackageBodyDefinition);
            if (procStart.find()) {
                int functionEndIndex = findProcEnd(procStart.end());
                return functionEndIndex >= 0
                    ? parentPackageBodyDefinition.substring(procStart.start(), functionEndIndex)
                    : parentPackageBodyDefinition.substring(procStart.start());
            }
        }
        return NO_DEFINITION_FOUND;
    }

    private int findProcEnd(int startIndex) {
        int functionEndIndex = tryFindProcEnd(startIndex);
        if (functionEndIndex >= 0) {
            return functionEndIndex;
        } else {
            beginStack.addFirst(startIndex);
            beginMatcher = getBeginMatcher();
            endMatcher = getEndMatcher();
            fillStacks();
            Integer endIndex = endStack.peek();
            return endIndex != null ? endIndex : -1;
        }
    }

    private int tryFindProcEnd(int startIndex) {
        Matcher endFunctionWithName = Pattern
            .compile("end\\s*" + proc.getUniqueName() + "[\\s\\n]*;", Pattern.CASE_INSENSITIVE)
            .matcher(parentPackageBodyDefinition);
        return endFunctionWithName.find(startIndex)
            ? endFunctionWithName.end()
            : -1;
    }

    private void fillStacks() {
        Integer lastBegin = beginStack.peek();
        if (lastBegin != null) {
            Integer lastEnd = Objects.requireNonNullElse(endStack.peek(), lastBegin);
            boolean isNextEndFound = findFromIndex(endMatcher, lastEnd);
            if (isNextEndFound && endMatcher.start() > lastBegin) {
                endStack.addFirst(endMatcher.end());
                boolean isNextBeginFound = findFromIndex(beginMatcher, lastBegin);
                if (isNextBeginFound) {
                    beginStack.addFirst(beginMatcher.end());
                    fillStacks();
                }
            }
        }
    }

    private boolean findFromIndex(@NotNull Matcher matcher, int index) {
        matcher.region(index, parentPackageBodyDefinition.length());
        return matcher.find();
    }

    private Matcher getBeginMatcher() {
        return Pattern
            .compile(String.join("|", possibleBeginCases), Pattern.CASE_INSENSITIVE)
            .matcher(parentPackageBodyDefinition);
    }

    private Matcher getEndMatcher() {
        return Pattern
            .compile("end[\\s\\n]*;", Pattern.CASE_INSENSITIVE)
            .matcher(parentPackageBodyDefinition);
    }

    @Nullable
    private String procType() {
        return switch (proc.getProcedureType()) {
            case PROCEDURE -> "procedure";
            case FUNCTION -> "function";
            default -> null;
        };
    }

}
