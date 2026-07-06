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
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Scans {@code --region}/{@code --endregion} script markers and decides when folding gutter refresh is needed.
 */
public final class SQLRegionMarkerFolding {

    private static final Log log = Log.getLog(SQLRegionMarkerFolding.class);

    private static final Pattern REGION_START_PATTERN = Pattern.compile("^\\s*--\\s*region\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern REGION_END_PATTERN = Pattern.compile("^\\s*--\\s*endregion\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern REGION_MARKER_NAME_PATTERN = Pattern.compile("^\\s*--\\s*region\\s*(.*)", Pattern.CASE_INSENSITIVE);

    private SQLRegionMarkerFolding() {
    }

    public record RegionFold(@NotNull String regionKey, int offset, int length) {
    }

    private record RegionStartFrame(int offset, @NotNull String regionKey) {
    }

    @NotNull
    public static List<RegionFold> scanRegions(@NotNull IDocument document) {
        List<RegionFold> regions = new ArrayList<>();
        if (document.getLength() == 0) {
            return regions;
        }
        IDocumentPartitioner partitioner = getSqlDocumentPartitioner(document);
        Deque<RegionStartFrame> regionStarts = new ArrayDeque<>();
        int lineCount = document.getNumberOfLines();
        for (int line = 0; line < lineCount; line++) {
            collectRegionFoldingOnLine(document, line, lineCount, partitioner, regionStarts, regions);
        }
        return regions;
    }

    @NotNull
    public static List<RegionFold> scanFoldableRegions(@NotNull IDocument document) {
        List<RegionFold> foldable = new ArrayList<>();
        for (RegionFold region : scanRegions(document)) {
            if (isValidDocumentRange(document, region.offset(), region.length())
                && getRegionNumberOfLines(document, region) > 1
            ) {
                foldable.add(region);
            }
        }
        return foldable;
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

    public static boolean isStrictlyEnclosedInAnyRegion(
        int innerStart,
        int innerEnd,
        @NotNull Collection<RegionFold> regions
    ) {
        for (RegionFold region : regions) {
            int outerStart = region.offset();
            int outerEnd = outerStart + region.length();
            if (isStrictlyEnclosed(innerStart, innerEnd, outerStart, outerEnd)) {
                return true;
            }
        }
        return false;
    }

    private static void collectRegionFoldingOnLine(
        @NotNull IDocument document,
        int line,
        int lineCount,
        IDocumentPartitioner partitioner,
        @NotNull Deque<RegionStartFrame> regionStarts,
        @NotNull List<RegionFold> regions
    ) {
        try {
            IRegion lineInfo = document.getLineInformation(line);
            int lineOffset = lineInfo.getOffset();
            int lineLength = lineInfo.getLength();
            if (lineLength == 0) {
                return;
            }
            String lineText = document.get(lineOffset, lineLength);
            if (!isSqlRegionMarkerCommentLine(lineText, lineOffset, partitioner)) {
                return;
            }
            if (REGION_START_PATTERN.matcher(lineText).find()) {
                String markerName = extractRegionMarkerName(lineText);
                String regionKey = regionStarts.isEmpty()
                    ? markerName
                    : regionStarts.peek().regionKey() + "/" + markerName;
                regionStarts.push(new RegionStartFrame(lineOffset, regionKey));
            } else if (REGION_END_PATTERN.matcher(lineText).find() && !regionStarts.isEmpty()) {
                addClosedRegion(document, line, lineCount, regionStarts, regions);
            }
        } catch (BadLocationException e) {
            log.warn("Error scanning for region folding markers", e);
        }
    }

    private static boolean isSqlRegionMarkerCommentLine(
        @NotNull String lineText,
        int lineOffset,
        IDocumentPartitioner partitioner
    ) {
        if (partitioner == null) {
            return true;
        }
        int commentPos = lineText.indexOf("--");
        if (commentPos < 0) {
            return false;
        }
        String contentType = partitioner.getContentType(lineOffset + commentPos);
        return SQLParserPartitions.CONTENT_TYPE_SQL_COMMENT.equals(contentType);
    }

    private static void addClosedRegion(
        @NotNull IDocument document,
        int endMarkerLine,
        int lineCount,
        @NotNull Deque<RegionStartFrame> regionStarts,
        @NotNull List<RegionFold> regions
    ) throws BadLocationException {
        RegionStartFrame regionStart = regionStarts.pop();
        int endOffset = endMarkerLine + 1 < lineCount
            ? document.getLineOffset(endMarkerLine + 1)
            : document.getLength();
        int length = endOffset - regionStart.offset();
        if (length > 0) {
            regions.add(new RegionFold(regionStart.regionKey(), regionStart.offset(), length));
        }
    }

    @NotNull
    private static String extractRegionMarkerName(@NotNull String lineText) {
        var matcher = REGION_MARKER_NAME_PATTERN.matcher(lineText);
        if (!matcher.find()) {
            return "region";
        }
        String markerName = matcher.group(1).trim();
        return markerName.isEmpty() ? "region" : markerName.toUpperCase(Locale.ROOT);
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
