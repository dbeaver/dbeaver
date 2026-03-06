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
    private static final List<String> possibleBeginCases = List.of(
        "begin",
        "IF",
        "CASE",
        "LOOP"
    );

    private static final List<String> commentPatterns = List.of(
        "--.*\n"
    );

    private final OracleProcedurePackaged proc;
    private final String parentPackageBodyDefinition;

    private final Deque<Integer> beginStack = new ArrayDeque<>();

    // must be sorted in asc to exclude properly
    private final PriorityQueue<RegionRange> commentRanges = new PriorityQueue<>();

    private Matcher beginMatcher;
    private Matcher endMatcher;

    public ProcedureBodyExtractor(@NotNull OracleProcedurePackaged proc, @NotNull String parentPackageBodyDefinition) {
        this.proc = proc;
        this.parentPackageBodyDefinition = parentPackageBodyDefinition;
        defineCommentRegions();
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
        int functionEndIndex = tryFindLabeledProcEnd(startIndex);
        if (functionEndIndex >= 0) {
            return functionEndIndex;
        } else {
            beginMatcher = getBeginMatcher();
            endMatcher = getEndMatcher();
            return findFunctionEndIndex(startIndex);
        }
    }

    private void defineCommentRegions() {
        Matcher commentsMatcher = Pattern
            .compile(String.join("|", commentPatterns))
            .matcher(parentPackageBodyDefinition);
        while (commentsMatcher.find()) {
            RegionRange prevRange = commentRanges.peek();
            if (prevRange == null || commentsMatcher.start() >= prevRange.endExclusive()) {
                commentRanges.add(new RegionRange(commentsMatcher.start(), commentsMatcher.end() + 1));
            }
        }
    }

    private int tryFindLabeledProcEnd(int startIndex) {
        Matcher endFunctionWithName = Pattern
            .compile("end\\s*" + proc.getUniqueName() + "[\\s\\n]*;", Pattern.CASE_INSENSITIVE)
            .matcher(parentPackageBodyDefinition);
        return findFromIndex(endFunctionWithName, startIndex)
            ? endFunctionWithName.end()
            : -1;
    }

    private int findFunctionEndIndex(int startSearch) {
        boolean isNextEndFound = findFromIndex(endMatcher, startSearch);
        if (isNextEndFound) {
            fillBeginStack(startSearch, endMatcher.start());
            beginStack.poll();
            return beginStack.isEmpty()
                ? endMatcher.end()
                : findFunctionEndIndex(endMatcher.end());
        } else {
            return -1;
        }
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

    private boolean findFromIndex(@NotNull Matcher matcher, int startIndex) {
        var searchableRegions = defineSearchableRanges(startIndex, parentPackageBodyDefinition.length());
        for (RegionRange region : searchableRegions) {
            matcher.region(region.startInclusive(), region.endExclusive());
            if (matcher.find()) {
                return true;
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
            .compile("END\\s+.*;", Pattern.CASE_INSENSITIVE)
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


    private record RegionRange(int startInclusive, int endExclusive) implements Comparable<RegionRange> {

        public boolean isInsideRange(int index) {
            return index > startInclusive && index < endExclusive;
        }

        @Override
        public int compareTo(@NotNull RegionRange o) {
            int diff = this.startInclusive - o.startInclusive;
            return diff != 0 ? diff : this.endExclusive - o.endExclusive;
        }
    }

}
