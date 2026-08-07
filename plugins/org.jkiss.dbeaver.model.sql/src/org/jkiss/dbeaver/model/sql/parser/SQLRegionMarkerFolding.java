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
package org.jkiss.dbeaver.model.sql.parser;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDialect;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Scans SQL comment region markers and provides indexes used by the SQL editor folding reconciler.
 */
public final class SQLRegionMarkerFolding {

    private static final Log log = Log.getLog(SQLRegionMarkerFolding.class);

    private SQLRegionMarkerFolding() {
    }

    public record RegionFold(@NotNull String regionKey, int offset, int length) {
    }

    private record RegionStartFrame(int offset, @NotNull String regionKey) {
    }

    private enum RegionMarkerType {
        START,
        END
    }

    private record RegionMarker(@NotNull RegionMarkerType type, @NotNull String name) {
    }

    private static final class RegionIndexNode {
        private final RegionFold region;
        private final RegionIndexNode parent;

        private RegionIndexNode(RegionFold region, RegionIndexNode parent) {
            this.region = region;
            this.parent = parent;
        }
    }

    /**
     * Index of nested region intervals. The latest region start is found in O(log n), then its
     * enclosing parents are checked until a containing region is found.
     */
    public static final class RegionIndex {
        private final NavigableMap<Integer, RegionIndexNode> regionsByOffset;

        private RegionIndex(NavigableMap<Integer, RegionIndexNode> regionsByOffset) {
            this.regionsByOffset = regionsByOffset;
        }

        public boolean isStrictlyEnclosed(int innerStart, int innerEnd) {
            var entry = regionsByOffset.floorEntry(innerStart);
            RegionIndexNode node = entry == null ? null : entry.getValue();
            while (node != null) {
                int outerStart = node.region.offset();
                int outerEnd = outerStart + node.region.length();
                if (SQLRegionMarkerFolding.isStrictlyEnclosed(innerStart, innerEnd, outerStart, outerEnd)) {
                    return true;
                }
                node = node.parent;
            }
            return false;
        }
    }

    @NotNull
    public static List<RegionFold> scanRegions(@NotNull IDocument document) {
        return scanRegions(document, BasicSQLDialect.INSTANCE);
    }

    @NotNull
    public static List<RegionFold> scanRegions(@NotNull IDocument document, @NotNull SQLDialect dialect) {
        List<RegionFold> regions = new ArrayList<>();
        if (document.getLength() == 0) {
            return regions;
        }
        IDocumentPartitioner partitioner = getSqlDocumentPartitioner(document);
        Deque<RegionStartFrame> regionStarts = new ArrayDeque<>();
        if (partitioner == null) {
            scanUnpartitionedDocument(document, dialect, regionStarts, regions);
        } else {
            scanCommentPartitions(document, dialect, partitioner, regionStarts, regions);
        }
        regions.sort((left, right) -> Integer.compare(left.offset(), right.offset()));
        return regions;
    }

    @NotNull
    public static List<RegionFold> scanFoldableRegions(@NotNull IDocument document) {
        return scanFoldableRegions(document, BasicSQLDialect.INSTANCE);
    }

    @NotNull
    public static List<RegionFold> scanFoldableRegions(@NotNull IDocument document, @NotNull SQLDialect dialect) {
        List<RegionFold> foldable = new ArrayList<>();
        for (RegionFold region : scanRegions(document, dialect)) {
            if (isValidDocumentRange(document, region.offset(), region.length())
                && getRegionNumberOfLines(document, region) > 1
            ) {
                foldable.add(region);
            }
        }
        return foldable;
    }

    /**
     * Checks only the lines and partitions affected by an edit. This lets the reconciler avoid a full region scan
     * for ordinary SQL text insertion while still detecting a newly-created marker.
     */
    public static boolean hasRegionMarkerInRange(
        @NotNull IDocument document,
        @NotNull SQLDialect dialect,
        int offset,
        int length
    ) {
        if (document.getLength() == 0) {
            return false;
        }
        try {
            int safeOffset = Math.max(0, Math.min(offset, document.getLength() - 1));
            int lastOffset = Math.max(safeOffset, Math.min(offset + Math.max(length, 1) - 1, document.getLength() - 1));
            int firstLine = document.getLineOfOffset(safeOffset);
            int lastLine = document.getLineOfOffset(lastOffset);
            int rangeOffset = document.getLineOffset(firstLine);
            int rangeEnd = document.getLineOffset(lastLine) + document.getLineInformation(lastLine).getLength();
            IDocumentPartitioner partitioner = getSqlDocumentPartitioner(document);
            if (partitioner == null) {
                for (int line = firstLine; line <= lastLine; line++) {
                    IRegion lineInfo = document.getLineInformation(line);
                    if (parseRegionMarker(document.get(lineInfo.getOffset(), lineInfo.getLength()), dialect) != null) {
                        return true;
                    }
                }
                return false;
            }
            for (ITypedRegion partition : partitioner.computePartitioning(rangeOffset, rangeEnd - rangeOffset)) {
                if (SQLParserPartitions.CONTENT_TYPE_SQL_COMMENT.equals(partition.getType())
                    && parsePartitionRegionMarker(document, dialect, partition) != null
                ) {
                    return true;
                }
            }
            return false;
        } catch (BadLocationException e) {
            log.warn("Error checking SQL region marker changes", e);
            return true;
        }
    }

    /**
     * Returns true when an edit touches a string or multi-line comment partition. Such an edit can change the
     * interpretation of markers outside the edited line, so the reconciler must use its safe full-scan path.
     */
    public static boolean hasPartitionSensitiveContentInRange(@NotNull IDocument document, int offset, int length) {
        if (document.getLength() == 0) {
            return false;
        }
        IDocumentPartitioner partitioner = getSqlDocumentPartitioner(document);
        if (partitioner == null) {
            return false;
        }
        try {
            int safeOffset = Math.max(0, Math.min(offset, document.getLength() - 1));
            int lastOffset = Math.max(safeOffset, Math.min(offset + Math.max(length, 1) - 1, document.getLength() - 1));
            int firstLine = document.getLineOfOffset(safeOffset);
            int lastLine = document.getLineOfOffset(lastOffset);
            int rangeOffset = document.getLineOffset(firstLine);
            int rangeEnd = document.getLineOffset(lastLine) + document.getLineInformation(lastLine).getLength();
            for (ITypedRegion partition : partitioner.computePartitioning(rangeOffset, rangeEnd - rangeOffset)) {
                if (SQLParserPartitions.CONTENT_TYPE_SQL_MULTILINE_COMMENT.equals(partition.getType())
                    || SQLParserPartitions.CONTENT_TYPE_SQL_STRING.equals(partition.getType())
                    || SQLParserPartitions.CONTENT_TYPE_SQL_QUOTED.equals(partition.getType())
                ) {
                    return true;
                }
            }
            return false;
        } catch (BadLocationException e) {
            log.warn("Error checking SQL partitions affected by edit", e);
            return true;
        }
    }

    public static boolean needsFoldingGutterRefresh(
        boolean annotationStructureChanged,
        boolean collapseApplied,
        boolean regionSetChanged,
        int editOffset,
        @NotNull Collection<RegionFold> regions
    ) {
        if (annotationStructureChanged || collapseApplied || regionSetChanged) {
            return true;
        }
        return documentEditShiftsRegionMarkers(editOffset, regions);
    }

    public static boolean documentEditShiftsRegionMarkers(int editOffset, @NotNull Collection<RegionFold> regions) {
        if (regions.isEmpty()) {
            return false;
        }
        for (RegionFold region : regions) {
            if (editOffset <= region.offset()) {
                return true;
            }
        }
        return false;
    }

    public static int getRegionNumberOfLines(@NotNull IDocument document, @NotNull RegionFold region) {
        if (!isValidDocumentRange(document, region.offset(), region.length())) {
            return 1;
        }
        try {
            int startLine = document.getLineOfOffset(region.offset());
            int endLine = document.getLineOfOffset(region.offset() + region.length() - 1);
            return endLine - startLine + 1;
        } catch (BadLocationException e) {
            return 1;
        }
    }

    public static boolean isValidDocumentRange(@NotNull IDocument document, int offset, int length) {
        if (offset < 0 || length <= 0) {
            return false;
        }
        int end = offset + length;
        if (end > document.getLength()) {
            return false;
        }
        try {
            document.getLineOfOffset(offset);
            if (end == document.getLength()) {
                if (document.getLength() == 0) {
                    return false;
                }
                document.getLineOfOffset(document.getLength() - 1);
            } else {
                document.getLineOfOffset(end);
            }
            return true;
        } catch (BadLocationException e) {
            return false;
        }
    }

    @NotNull
    public static RegionIndex createRegionIndex(@NotNull Collection<RegionFold> regions) {
        List<RegionFold> sortedRegions = regions.stream()
            .sorted((left, right) -> Integer.compare(left.offset(), right.offset()))
            .toList();
        NavigableMap<Integer, RegionIndexNode> regionsByOffset = new TreeMap<>();
        Deque<RegionIndexNode> openRegions = new ArrayDeque<>();
        for (RegionFold region : sortedRegions) {
            while (!openRegions.isEmpty() && region.offset() >= endOffset(openRegions.peek().region)) {
                openRegions.pop();
            }
            RegionIndexNode node = new RegionIndexNode(region, openRegions.peek());
            regionsByOffset.put(region.offset(), node);
            openRegions.push(node);
        }
        return new RegionIndex(regionsByOffset);
    }

    private static void scanCommentPartitions(
        @NotNull IDocument document,
        @NotNull SQLDialect dialect,
        IDocumentPartitioner partitioner,
        @NotNull Deque<RegionStartFrame> regionStarts,
        @NotNull List<RegionFold> regions
    ) {
        try {
            for (ITypedRegion partition : partitioner.computePartitioning(0, document.getLength())) {
                if (!SQLParserPartitions.CONTENT_TYPE_SQL_COMMENT.equals(partition.getType())) {
                    continue;
                }
                RegionMarker marker = parsePartitionRegionMarker(document, dialect, partition);
                if (marker != null) {
                    collectRegionMarker(document, marker, partition.getOffset(), regionStarts, regions);
                }
            }
        } catch (BadLocationException e) {
            log.warn("Error scanning for region folding markers", e);
        }
    }

    private static void scanUnpartitionedDocument(
        @NotNull IDocument document,
        @NotNull SQLDialect dialect,
        @NotNull Deque<RegionStartFrame> regionStarts,
        @NotNull List<RegionFold> regions
    ) {
        int lineCount = document.getNumberOfLines();
        for (int line = 0; line < lineCount; line++) {
            try {
                IRegion lineInfo = document.getLineInformation(line);
                RegionMarker marker = parseRegionMarker(document.get(lineInfo.getOffset(), lineInfo.getLength()), dialect);
                if (marker != null) {
                    collectRegionMarker(document, marker, lineInfo.getOffset(), regionStarts, regions);
                }
            } catch (BadLocationException e) {
                log.warn("Error scanning for region folding markers", e);
            }
        }
    }

    private static void collectRegionMarker(
        @NotNull IDocument document,
        @NotNull RegionMarker marker,
        int markerOffset,
        @NotNull Deque<RegionStartFrame> regionStarts,
        @NotNull List<RegionFold> regions
    ) throws BadLocationException {
        if (marker.type() == RegionMarkerType.START) {
            String regionKey = regionStarts.isEmpty()
                ? marker.name()
                : regionStarts.peek().regionKey() + "/" + marker.name();
            regionStarts.push(new RegionStartFrame(markerOffset, regionKey));
        } else if (!regionStarts.isEmpty()) {
            int endMarkerLine = document.getLineOfOffset(markerOffset);
            addClosedRegion(document, endMarkerLine, regionStarts, regions);
        }
    }

    @Nullable
    private static RegionMarker parsePartitionRegionMarker(
        @NotNull IDocument document,
        @NotNull SQLDialect dialect,
        @NotNull ITypedRegion partition
    ) throws BadLocationException {
        IRegion lineInfo = document.getLineInformation(document.getLineOfOffset(partition.getOffset()));
        if (!document.get(lineInfo.getOffset(), partition.getOffset() - lineInfo.getOffset()).isBlank()) {
            return null;
        }
        return parseRegionMarker(document.get(partition.getOffset(), partition.getLength()), dialect);
    }

    private static RegionMarker parseRegionMarker(@NotNull String lineText, @NotNull SQLDialect dialect) {
        String markerText = lineText.stripLeading();
        String commentPrefix = findCommentPrefix(markerText, dialect.getSingleLineComments());
        if (commentPrefix == null) {
            return null;
        }
        String markerBody = markerText.substring(commentPrefix.length()).stripLeading();
        String startName = markerBodyAfterKeyword(markerBody, "region");
        if (startName != null) {
            String name = startName.trim();
            return new RegionMarker(RegionMarkerType.START, name.isEmpty() ? "REGION" : name.toUpperCase(Locale.ROOT));
        }
        if (markerBodyAfterKeyword(markerBody, "endregion") != null) {
            return new RegionMarker(RegionMarkerType.END, "");
        }
        return null;
    }

    private static String findCommentPrefix(@NotNull String markerText, String[] commentPrefixes) {
        if (commentPrefixes == null) {
            return null;
        }
        String matchingPrefix = null;
        for (String prefix : commentPrefixes) {
            if (prefix != null && !prefix.isEmpty() && markerText.startsWith(prefix)
                && (matchingPrefix == null || prefix.length() > matchingPrefix.length())
            ) {
                matchingPrefix = prefix;
            }
        }
        return matchingPrefix;
    }

    private static String markerBodyAfterKeyword(@NotNull String markerBody, @NotNull String keyword) {
        if (!markerBody.regionMatches(true, 0, keyword, 0, keyword.length())) {
            return null;
        }
        if (markerBody.length() == keyword.length()) {
            return "";
        }
        char next = markerBody.charAt(keyword.length());
        if (Character.isLetterOrDigit(next) || next == '_') {
            return null;
        }
        return markerBody.substring(keyword.length());
    }

    private static void addClosedRegion(
        @NotNull IDocument document,
        int endMarkerLine,
        @NotNull Deque<RegionStartFrame> regionStarts,
        @NotNull List<RegionFold> regions
    ) throws BadLocationException {
        int lineCount = document.getNumberOfLines();
        RegionStartFrame regionStart = regionStarts.pop();
        int endOffset = endMarkerLine + 1 < lineCount
            ? document.getLineOffset(endMarkerLine + 1)
            : document.getLength();
        int length = endOffset - regionStart.offset();
        if (length > 0) {
            regions.add(new RegionFold(regionStart.regionKey(), regionStart.offset(), length));
        }
    }

    private static int endOffset(@NotNull RegionFold region) {
        return region.offset() + region.length();
    }

    private static IDocumentPartitioner getSqlDocumentPartitioner(@NotNull IDocument document) {
        if (document instanceof IDocumentExtension3 ext) {
            return ext.getDocumentPartitioner(SQLParserPartitions.SQL_PARTITIONING);
        }
        return null;
    }

    private static boolean isStrictlyEnclosed(int innerStart, int innerEnd, int outerStart, int outerEnd) {
        return innerStart >= outerStart && innerEnd <= outerEnd
            && (innerStart > outerStart || innerEnd < outerEnd);
    }
}
