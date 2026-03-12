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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ProcedureBodyExtractor {

    public static final String NO_DEFINITION_FOUND = "-- no definition found";

    private static final List<String> commentPatterns = List.of(
        "--.*\n",
        "/\\*[\\s\\S]*?\\*/"
    );

    private static final String END_PATTERN = "\\b(END\\s+.*|END\\s*);";

    private static final List<String> nestedProcedureStartCases = List.of(
        "PROCEDURE",
        "FUNCTION"
    );

    private static final List<String> possibleBeginCases = List.of(
        "BEGIN",
        "IF",
        "CASE",
        "LOOP"
    );

    private final OracleProcedurePackaged proc;
    private final String parentPackageBodyDefinition;

    // must be sorted in asc to exclude properly
    private final List<RegionRange> commentRanges = new ArrayList<>();

    private Matcher beginMatcher;
    private Matcher endMatcher;
    private Matcher nestedProcedureMatcher;

    public ProcedureBodyExtractor(@NotNull OracleProcedurePackaged proc, @NotNull String parentPackageBodyDefinition) {
        this.proc = proc;
        this.parentPackageBodyDefinition = parentPackageBodyDefinition;
        defineCommentRegions();
    }


    private void defineCommentRegions() {
        Matcher commentsMatcher = Pattern
            .compile(String.join("|", commentPatterns))
            .matcher(parentPackageBodyDefinition);
        while (commentsMatcher.find()) {
            RegionRange prevRange = commentRanges.isEmpty() ? null : commentRanges.getLast();
            if (prevRange != null && commentsMatcher.start() < prevRange.endExclusive()) {
                commentRanges.removeLast();
                commentRanges.add(new RegionRange(prevRange.startInclusive(), commentsMatcher.end()));
            } else {
                commentRanges.add(new RegionRange(commentsMatcher.start(), commentsMatcher.end()));
            }
        }
    }

    public String extractProcBody() {
        String procType = procType();
        if (procType != null) {
            Matcher procStart = Pattern
                .compile(procType + "\\s+" + Pattern.quote(proc.getName()), Pattern.CASE_INSENSITIVE)
                .matcher(parentPackageBodyDefinition);
            int numberOfOverloadsBefore = Objects.requireNonNullElse(proc.getOverloadNumber(), 1) - 1;
            if (findNthMatchInRange(procStart, 0, parentPackageBodyDefinition.length(), numberOfOverloadsBefore)) {
                int functionEndIndex = findProcEnd(procStart.end());
                return functionEndIndex >= 0
                    ? parentPackageBodyDefinition.substring(procStart.start(), functionEndIndex)
                    : parentPackageBodyDefinition.substring(procStart.start());
            }
        }
        return NO_DEFINITION_FOUND;
    }

    private int findProcEnd(int startIndex) {
        int functionEndIndex = tryFindLabeledProcEnd(startIndex);
        if (functionEndIndex >= 0) {
            return functionEndIndex;
        } else {
            beginMatcher = getBeginMatcher();
            endMatcher = getEndMatcher();
            nestedProcedureMatcher = getNestedProcedureMatcher();
            return new EndProcedureFinder(startIndex).findProcedureEndIndex();
        }
    }

    private int tryFindLabeledProcEnd(int startIndex) {
        Matcher endFunctionWithName = Pattern
            .compile("\\bEND\\s+" + proc.getName() + "\\s*;", Pattern.CASE_INSENSITIVE)
            .matcher(parentPackageBodyDefinition);
        return findFromIndex(endFunctionWithName, startIndex)
            ? endFunctionWithName.end()
            : -1;
    }

    private boolean findFromIndex(@NotNull Matcher matcher, int startIndex) {
        return findInRange(matcher, startIndex, parentPackageBodyDefinition.length());
    }

    private boolean findInRange(@NotNull Matcher matcher, int startIndex, int endIndexExclusive) {
        return findNthMatchInRange(matcher, startIndex, endIndexExclusive, 0);
    }

    private boolean findNthMatchInRange(@NotNull Matcher matcher, int startIndex, int endIndexExclusive, int occurrencesToSkip) {
        var searchableRegions = defineSearchableRanges(startIndex, endIndexExclusive);
        int matchesFound = 0;
        for (RegionRange region : searchableRegions) {
            matcher.region(region.startInclusive(), region.endExclusive());
            while (matcher.find()) {
                if (++matchesFound > occurrencesToSkip) {
                    return true;
                }
            }
        }
        return false;
    }

    @NotNull
    private List<RegionRange> defineSearchableRanges(int startIndex, int endIndexExclusive) {
        List<RegionRange> searchableRegions = new ArrayList<>();
        RegionRange rightPart = new RegionRange(startIndex, endIndexExclusive);
        for (RegionRange commentRange : commentRanges) {
            boolean isStartInside = rightPart.isInsideRange(commentRange.startInclusive());
            boolean isEndInside = rightPart.isInsideRange(commentRange.endExclusive());
            if (isStartInside) {
                searchableRegions.add(new RegionRange(rightPart.startInclusive(), commentRange.startInclusive()));
                if (!isEndInside) {
                    return searchableRegions;
                }
            }
            if (isEndInside) {
                rightPart = new RegionRange(commentRange.endExclusive(), rightPart.endExclusive());
            }
        }
        searchableRegions.add(rightPart);
        return searchableRegions;
    }

    private Matcher getBeginMatcher() {
        return Pattern
            .compile(
                possibleBeginCases.stream()
                    .map(token -> "(?<!END\\s+)" + token)
                    .collect(Collectors.joining("|")),
                Pattern.CASE_INSENSITIVE
            )
            .matcher(parentPackageBodyDefinition);
    }

    private Matcher getEndMatcher() {
        return Pattern
            .compile(END_PATTERN, Pattern.CASE_INSENSITIVE)
            .matcher(parentPackageBodyDefinition);
    }

    private Matcher getNestedProcedureMatcher() {
        return Pattern
            .compile(String.join("|", nestedProcedureStartCases))
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


    private record RegionRange(int startInclusive, int endExclusive) {
        public boolean isInsideRange(int index) {
            return index >= startInclusive && index < endExclusive;
        }
    }

    private class EndProcedureFinder {

        private final Deque<Integer> beginStack = new ArrayDeque<>();

        private final int procedureStartIndex;

        public EndProcedureFinder(int procedureStartIndex) {
            this.procedureStartIndex = procedureStartIndex;
        }

        public int findProcedureEndIndex() {
            return findProcedureEndIndex(procedureStartIndex);
        }

        private int findProcedureEndIndex(int startSearch) {
            boolean isNextEndFound = findFromIndex(endMatcher, startSearch);
            if (isNextEndFound) {
                boolean isNestedProcedureFound = findInRange(nestedProcedureMatcher, startSearch, endMatcher.start());
                if (isNestedProcedureFound) {
                    fillBeginStack(startSearch, nestedProcedureMatcher.start());
                    int nestedProcEnd = new EndProcedureFinder(nestedProcedureMatcher.end()).findProcedureEndIndex();
                    if (nestedProcEnd < 0) {
                        return nestedProcEnd;
                    }
                    return findProcedureEndIndex(nestedProcEnd);
                }
                fillBeginStack(startSearch, endMatcher.start());
                beginStack.poll();
                return beginStack.isEmpty()
                    ? endMatcher.end()
                    : findProcedureEndIndex(endMatcher.end());
            }
            return -1;
        }

        private void fillBeginStack(int fromIndex, int toIndex) {
            var searchableRegions = defineSearchableRanges(fromIndex, toIndex);
            for (RegionRange region : searchableRegions) {
                beginMatcher.region(region.startInclusive(), region.endExclusive());
                while (beginMatcher.find()) {
                    beginStack.push(beginMatcher.end());
                }
            }
        }
    }
}
