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
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;
import org.eclipse.ui.texteditor.spelling.SpellingService;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.parser.SQLRegionMarkerFolding;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorActivator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.stream.Collectors;

public class SQLReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {
    private static final Log log = Log.getLog(SQLReconcilingStrategy.class);

    private static final QualifiedName COLLAPSED_ANNOTATIONS =
        new QualifiedName(SQLEditorActivator.PLUGIN_ID, SQLReconcilingStrategy.class.getName() + ".collapsedFoldingAnnotations");

    private final NavigableSet<SQLScriptElementImpl> cache = new TreeSet<>();
    private final Map<String, SQLScriptElementImpl> regionCache = new LinkedHashMap<>();

    private volatile boolean projectionRefreshScheduled;
    private volatile boolean projectionRefreshPending;

    private final SQLEditorBase editor;

    private IDocument document;
    private IProgressMonitor monitor;

    // Spelling
    private SpellingService spellingService;
    private SQLSpellingContext spellingContext;
    private boolean initialized;

    public SQLReconcilingStrategy(SQLEditorBase editor) {
        this.editor = editor;
    }

    protected IAnnotationModel getAnnotationModel() {
        return editor.getAnnotationModel();
    }

    private boolean isSpellingEnabled() {
        return EditorsUI.getPreferenceStore().getBoolean("spellingEnabled");
    }

    @Override
    public void setDocument(IDocument document) {
        this.document = document;
        this.cache.clear();
        this.regionCache.clear();

        spellingService = EditorsUI.getSpellingService();
        if (spellingService.getActiveSpellingEngineDescriptor(editor.getViewerConfiguration().getPreferenceStore()) != null) {
            this.spellingContext = new SQLSpellingContext(editor);
            this.spellingContext.setContentType(SQLEditorUtils.getSQLContentType());
        }
    }

    @Override
    public void setProgressMonitor(IProgressMonitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
        if (DirtyRegion.INSERT.equals(dirtyRegion.getType())) {
            reconcile(subRegion.getOffset(), subRegion.getLength(), false);
        } else {
            reconcile(subRegion.getOffset(), 0, false);
        }
    }

    @Override
    public void reconcile(IRegion partition) {
        reconcile(0, document.getLength(), false);
    }

    @Override
    public void initialReconcile() {
        if (!initialized) {
            initialized = true;
            reconcile(0, document.getLength(), true);
        }
    }

    private Set<Integer> getSavedCollapsedAnnotationsOffsets() {
        IResource resource = getResource();
        if (resource == null) {
            return Collections.emptySet();
        }
        String data;
        try {
            data = resource.getPersistentProperty(COLLAPSED_ANNOTATIONS);
        } catch (CoreException e) {
            log.warn("Core Exception caught while reading saved collapsed folding positions: " + e.getMessage());
            return Collections.emptySet();
        }
        if (data == null) {
            return Collections.emptySet();
        }

        Set<Integer> collapsedPositionsOffsets = new HashSet<>();
        String[] offsets = data.split(";");
        for (String offset : offsets) {
            int offsetValue = CommonUtils.toInt(offset, -1);
            if (offsetValue == -1) {
                log.warn("Illegal offset parsed while reading saved collapsed annotation offsets. offset=" + offset);
                continue;
            }
            collapsedPositionsOffsets.add(offsetValue);
        }

        return collapsedPositionsOffsets;
    }

    //format: "offset_1;offset_2;...offset_n"
    public void saveState() {
        IResource resource = getResource();
        ProjectionAnnotationModel annotationModel = editor.getProjectionAnnotationModel();
        if (resource == null || annotationModel == null) {
            return;
        }
        StringJoiner stringJoiner = new StringJoiner(";");
        for (SQLScriptElementImpl position : cache) {
            ProjectionAnnotation annotation = position.getAnnotation();
            if (annotation != null && annotation.isCollapsed()) {
                stringJoiner.add(Integer.toString(position.getOffset()));
            }
        }
        for (SQLScriptElementImpl position : regionCache.values()) {
            ProjectionAnnotation annotation = position.getAnnotation();
            if (annotation != null && annotation.isCollapsed()) {
                stringJoiner.add(Integer.toString(position.getOffset()));
            }
        }
        String value;
        if (stringJoiner.length() == 0) {
            value = null;
        } else {
            value = stringJoiner.toString();
        }
        try {
            resource.setPersistentProperty(COLLAPSED_ANNOTATIONS, value);
        } catch (CoreException e) {
            log.warn("Core Exception caught while persisting saved collapsed folding positions", e);
        }
    }

    @Nullable
    private IResource getResource() {
        return EditorUtils.getFileFromInput(editor.getEditorInput());
    }

    public void onDataSourceChange() {
        if (document == null) {
            return;
        }
        if (!initialized) {
            initialReconcile();
        } else {
            reconcile(0, document.getLength(), true);
        }
    }

    private void reconcile(int damagedRegionOffset, int damagedRegionLength, boolean restoreCollapsedAnnotations) {
        if (!editor.isFoldingEnabled()) {
            cache.clear(); // underlying annotation model being cleared, so reset the cache too
            regionCache.clear();
            return;
        }
        ProjectionAnnotationModel model = editor.getProjectionAnnotationModel();
        if (model == null) {
            return;
        }

        final int editOffset = damagedRegionOffset; // original dirty region; damagedRegionOffset is widened below for parser cache

        SQLScriptElementImpl leftBound = cache.lower(new SQLScriptElementImpl(damagedRegionOffset, damagedRegionLength));
        if (leftBound != null) {
            leftBound = cache.lower(leftBound);
        }
        SQLScriptElementImpl rightBound = cache.ceiling(new SQLScriptElementImpl(damagedRegionOffset + damagedRegionLength, 0));
        if (leftBound == null) {
            damagedRegionOffset = 0;
        } else {
            damagedRegionOffset = leftBound.getOffset() + leftBound.getLength();
        }
        if (rightBound == null) {
            damagedRegionLength = document.getLength();
        } else {
            damagedRegionLength = rightBound.getOffset() + rightBound.getLength() - damagedRegionOffset;
        }

        List<SQLScriptElement> parsedQueries = extractQueries(damagedRegionOffset, damagedRegionLength);
        if (parsedQueries == null) {
            return;
        }

        if (rightBound != null && !parsedQueries.isEmpty()) {
            SQLScriptElement rightmostParsedQuery = parsedQueries.get(parsedQueries.size() - 1);
            if (!rightBound.equals(getExpandedScriptElement(rightmostParsedQuery))) {
                parsedQueries = extractQueries(damagedRegionOffset, document.getLength());
                if (parsedQueries == null) {
                    return;
                }
                rightBound = null;
            }
        }

        Collection<SQLScriptElementImpl> cachedQueries;
        if (leftBound == null && rightBound == null) {
            cachedQueries = Collections.unmodifiableNavigableSet(cache);
        } else if (leftBound == null) {
            cachedQueries = Collections.unmodifiableNavigableSet(cache.headSet(rightBound, true));
        } else if (rightBound == null) {
            cachedQueries = Collections.unmodifiableNavigableSet(cache.tailSet(leftBound, false));
        } else {
            cachedQueries = Collections.unmodifiableNavigableSet(cache.subSet(leftBound, false, rightBound, true));
        }

        List<SQLScriptElementImpl> regionElements = extractRegionFoldingPositions();
        List<SQLRegionMarkerFolding.RegionFold> regionFolds = regionElements.stream()
            .map(r -> new SQLRegionMarkerFolding.RegionFold(r.getRegionKey(), r.getOffset(), r.getLength()))
            .toList();

        Set<SQLScriptElementImpl> parsedElements = new HashSet<>(parsedQueries.stream()
            .filter(this::deservesFolding)
            .map(this::getExpandedScriptElement)
            .filter(element -> !isStrictlyEnclosedInAnyRegion(element, regionFolds))
            .collect(Collectors.toSet()));
        Map<Annotation, SQLScriptElementImpl> additions = new HashMap<>();
        Set<Integer> savedCollapsedAnnotationsOffsets = restoreCollapsedAnnotations ? getSavedCollapsedAnnotationsOffsets() : Collections.emptySet();
        for (SQLScriptElementImpl element : parsedElements) {
            if (!cachedQueries.contains(element)) {
                ProjectionAnnotation annotation = new ProjectionAnnotation();
                element.setAnnotation(annotation);
                additions.put(annotation, element);
            }
        }
        Collection<SQLScriptElementImpl> deletedPositions = cachedQueries.stream()
            .filter(element -> !parsedElements.contains(element))
            .toList();
        Set<Integer> collapsedOffsetsToRestore = new HashSet<>(savedCollapsedAnnotationsOffsets);
        for (SQLScriptElementImpl deleted : deletedPositions) {
            ProjectionAnnotation annotation = deleted.getAnnotation();
            if (annotation != null && annotation.isCollapsed()) {
                collapsedOffsetsToRestore.add(deleted.getOffset());
            }
        }
        Annotation[] deletions = deletedPositions.stream()
            .map(SQLScriptElementImpl::getAnnotation)
            .toArray(Annotation[]::new);
        model.modifyAnnotations(deletions, additions, null);
        for (SQLScriptElementImpl element : additions.values()) {
            if (collapsedOffsetsToRestore.contains(element.getOffset())) {
                ProjectionAnnotation annotation = element.getAnnotation();
                if (annotation != null) {
                    model.collapse(annotation);
                }
            }
        }
        cache.removeAll(deletedPositions);
        cache.addAll(additions.values());

        syncRegionFolds(model, savedCollapsedAnnotationsOffsets, editOffset);

        if (isSpellingEnabled() && spellingContext != null) {
            IRegion[] regions = new IRegion[]{
                new Region(damagedRegionOffset, damagedRegionLength)
            };
            ISpellingProblemCollector spellingProblemCollector = new SpellingProblemCollector(
                getAnnotationModel(), damagedRegionOffset, damagedRegionLength);

            spellingService.check(document, regions, spellingContext, spellingProblemCollector, monitor);
        }
    }

    private void syncRegionFolds(
        @NotNull ProjectionAnnotationModel model,
        @NotNull Set<Integer> savedCollapsedAnnotationsOffsets,
        int editOffset
    ) {
        List<SQLScriptElementImpl> scannedRegions = extractFoldableRegionFoldingPositions();
        Map<String, SQLScriptElementImpl> scannedByKey = new LinkedHashMap<>();
        for (SQLScriptElementImpl region : scannedRegions) {
            if (region.getRegionKey() != null) {
                scannedByKey.put(region.getRegionKey(), region);
            }
        }

        Map<String, Boolean> collapsedByKey = new HashMap<>();
        for (SQLScriptElementImpl cachedRegion : regionCache.values()) {
            ProjectionAnnotation annotation = cachedRegion.getAnnotation();
            if (annotation != null && annotation.isCollapsed() && cachedRegion.getRegionKey() != null) {
                collapsedByKey.put(cachedRegion.getRegionKey(), true);
            }
        }
        for (Map.Entry<String, SQLScriptElementImpl> scannedEntry : scannedByKey.entrySet()) {
            if (savedCollapsedAnnotationsOffsets.contains(scannedEntry.getValue().getOffset())) {
                collapsedByKey.put(scannedEntry.getKey(), true);
            }
        }

        Map<Annotation, SQLScriptElementImpl> additions = new HashMap<>();
        List<Annotation> deletions = new ArrayList<>();
        Map<String, SQLScriptElementImpl> nextRegionCache = new LinkedHashMap<>();

        for (Map.Entry<String, SQLScriptElementImpl> cachedEntry : regionCache.entrySet()) {
            if (!scannedByKey.containsKey(cachedEntry.getKey())) {
                ProjectionAnnotation annotation = cachedEntry.getValue().getAnnotation();
                if (annotation != null) {
                    deletions.add(annotation);
                }
            }
        }

        for (Map.Entry<String, SQLScriptElementImpl> scannedEntry : scannedByKey.entrySet()) {
            String regionKey = scannedEntry.getKey();
            SQLScriptElementImpl scannedRegion = scannedEntry.getValue();
            SQLScriptElementImpl cachedRegion = regionCache.get(regionKey);

            if (!needsRegionAnnotationRecreate(cachedRegion, scannedRegion, model)) {
                nextRegionCache.put(regionKey, cachedRegion);
                continue;
            }

            if (cachedRegion != null) {
                ProjectionAnnotation oldAnnotation = cachedRegion.getAnnotation();
                if (oldAnnotation != null) {
                    if (oldAnnotation.isCollapsed()) {
                        collapsedByKey.put(regionKey, true);
                    }
                    deletions.add(oldAnnotation);
                }
            }

            ProjectionAnnotation annotation = new ProjectionAnnotation();
            scannedRegion.setAnnotation(annotation);
            additions.put(annotation, scannedRegion);
            nextRegionCache.put(regionKey, scannedRegion);
        }

        if (!deletions.isEmpty() || !additions.isEmpty()) {
            model.modifyAnnotations(deletions.toArray(Annotation[]::new), additions, null);
        }

        boolean collapseApplied = false;
        for (Map.Entry<String, SQLScriptElementImpl> entry : nextRegionCache.entrySet()) {
            SQLScriptElementImpl region = entry.getValue();
            ProjectionAnnotation annotation = region.getAnnotation();
            if (annotation == null
                || document == null
                || !SQLRegionMarkerFolding.isValidDocumentRange(document, region.getOffset(), region.getLength())
            ) {
                continue;
            }
            if (collapsedByKey.getOrDefault(entry.getKey(), false) && !annotation.isCollapsed()) {
                model.collapse(annotation);
                collapseApplied = true;
            }
        }

        boolean regionSetChanged = !regionCache.keySet().equals(scannedByKey.keySet());

        regionCache.clear();
        regionCache.putAll(nextRegionCache);

        if (SQLRegionMarkerFolding.needsFoldingGutterRefresh(
            !deletions.isEmpty() || !additions.isEmpty(),
            collapseApplied,
            regionSetChanged,
            editOffset,
            scannedByKey.values().stream()
                .map(r -> new SQLRegionMarkerFolding.RegionFold(r.getRegionKey(), r.getOffset(), r.getLength()))
                .toList()
        )) {
            scheduleProjectionPresentationRefresh();
        }
    }

    /**
     * Reconcile updates annotations off the UI thread; projection gutter icons may stay stale until
     * the viewer repaints (scrolling happens to trigger that). Force the same refresh explicitly.
     */
    private void scheduleProjectionPresentationRefresh() {
        if (projectionRefreshScheduled) {
            projectionRefreshPending = true;
            return;
        }
        projectionRefreshScheduled = true;
        UIUtils.asyncExec(this::runProjectionPresentationRefresh);
    }

    private void runProjectionPresentationRefresh() {
        try {
            if (document != null && editor.isFoldingEnabled()) {
                editor.refreshProjectionFoldingPresentation();
            }
        } catch (RuntimeException e) {
            log.warn("Failed to refresh projection folding presentation", e);
        }
        if (projectionRefreshPending) {
            projectionRefreshPending = false;
            UIUtils.asyncExec(this::runProjectionPresentationRefresh);
        } else {
            projectionRefreshScheduled = false;
        }
    }

    private boolean needsRegionAnnotationRecreate(
        @Nullable SQLScriptElementImpl cachedRegion,
        @NotNull SQLScriptElementImpl scannedRegion,
        @NotNull ProjectionAnnotationModel model
    ) {
        if (cachedRegion == null || cachedRegion.getAnnotation() == null) {
            return true;
        }
        int scannedOffset = scannedRegion.getOffset();
        int scannedLength = scannedRegion.getLength();
        if (cachedRegion.getOffset() != scannedOffset || cachedRegion.getLength() != scannedLength) {
            return true;
        }
        Position modelPosition = model.getPosition(cachedRegion.getAnnotation());
        return modelPosition == null
            || modelPosition.getOffset() != scannedOffset
            || modelPosition.getLength() != scannedLength;
    }

    private int getRegionNumberOfLines(@NotNull SQLScriptElementImpl region) {
        if (document == null || region.getRegionKey() == null) {
            return 1;
        }
        return SQLRegionMarkerFolding.getRegionNumberOfLines(
            document,
            new SQLRegionMarkerFolding.RegionFold(region.getRegionKey(), region.getOffset(), region.getLength())
        );
    }

    @Nullable
    private List<SQLScriptElement> extractQueries(int offset, int length) {
        return editor.extractScriptQueries(offset, length, false, true, false);
    }

    @NotNull
    private List<SQLScriptElementImpl> extractRegionFoldingPositions() {
        if (document == null) {
            return List.of();
        }
        return SQLRegionMarkerFolding.scanRegions(document).stream()
            .map(f -> new SQLScriptElementImpl(f.offset(), f.length(), f.regionKey()))
            .toList();
    }

    @NotNull
    private List<SQLScriptElementImpl> extractFoldableRegionFoldingPositions() {
        if (document == null) {
            return List.of();
        }
        return SQLRegionMarkerFolding.scanFoldableRegions(document).stream()
            .map(f -> new SQLScriptElementImpl(f.offset(), f.length(), f.regionKey()))
            .toList();
    }

    private boolean isStrictlyEnclosedInAnyRegion(
        @NotNull SQLScriptElementImpl element,
        @NotNull List<SQLRegionMarkerFolding.RegionFold> regions
    ) {
        int start = element.getOffset();
        int end = start + element.getLength();
        return SQLRegionMarkerFolding.isStrictlyEnclosedInAnyRegion(start, end, regions);
    }

    private boolean deservesFolding(SQLScriptElement element) {
        int numberOfLines = getNumberOfLines(element);
        if (numberOfLines == 1) {
            return false;
        }
        if (element.getOffset() + element.getLength() != document.getLength() && expandQueryLength(element) == element.getLength()) {
            return numberOfLines > 2;
        }
        return true;
    }

    private int getNumberOfLines(SQLScriptElement element) {
        try {
            int start = element.getOffset();
            int exclusiveEnd = start + element.getLength();
            if (exclusiveEnd <= start) {
                return 1;
            }
            int lastIncludedOffset = exclusiveEnd - 1;
            return document.getLineOfOffset(lastIncludedOffset) - document.getLineOfOffset(start) + 1;
        } catch (BadLocationException e) {
            throw new SQLReconcilingStrategyException(e);
        }
    }

    //expands query to the end of the line if there are only whitespaces after it. Returns desired length.
    private int expandQueryLength(SQLScriptElement element) { //todo simplify
        int position = element.getOffset() + element.getLength();
        while (position < document.getLength()) {
            char c = unsafeGetChar(position);
            if (c == '\n') {
                if (position + 1 < document.getLength()) {
                    position++;
                    break;
                }
            }
            if (Character.isWhitespace(c)) {
                position++;
            } else {
                return element.getLength();
            }
        }
        return position - element.getOffset();
    }

    @NotNull
    private SQLScriptElementImpl getExpandedScriptElement(@NotNull SQLScriptElement element) {
        return new SQLScriptElementImpl(element.getOffset(), expandQueryLength(element));
    }

    private char unsafeGetChar(int index) {
        try {
            return document.getChar(index);
        } catch (BadLocationException e) {
            throw new SQLReconcilingStrategyException(e);
        }
    }

    private static class SQLReconcilingStrategyException extends RuntimeException {
        private SQLReconcilingStrategyException(Throwable cause) {
            super(cause);
        }
    }

    private static class SQLScriptElementImpl extends Position implements SQLScriptElement, Comparable<SQLScriptElementImpl> {
        @Nullable
        private ProjectionAnnotation annotation;
        @Nullable
        private final String regionKey;

        SQLScriptElementImpl(int offset, int length) {
            this(offset, length, null);
        }

        SQLScriptElementImpl(int offset, int length, @Nullable String regionKey) {
            super(offset, length);
            this.regionKey = regionKey;
        }

        @Nullable
        public String getRegionKey() {
            return regionKey;
        }

        @Nullable
        public ProjectionAnnotation getAnnotation() {
            return annotation;
        }

        public void setAnnotation(@Nullable ProjectionAnnotation annotation) {
            this.annotation = annotation;
        }

        @Override
        public int compareTo(@NotNull SQLScriptElementImpl o) {
            int diff = getOffset() - o.getOffset();
            if (diff != 0) {
                return diff;
            }
            return getLength() - o.getLength();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Position p) {
                return equals(p.getOffset(), p.getLength());
            }
            if (o instanceof SQLScriptElement e) {
                return equals(e.getOffset(), e.getLength());
            }
            return false;
        }

        private boolean equals(int offset, int length) {
            return getOffset() == offset && getLength() == length;
        }

        @Override
        public int hashCode() {
            return Objects.hash(getOffset(), getLength());
        }

        @NotNull
        @Override
        public String getOriginalText() {
            return "";
        }

        @NotNull
        @Override
        public String getText() {
            return "";
        }

        @Override
        public Object getData() {
            return "";
        }

        @Override
        public void setData(Object data) {
            //do nothing
        }

        @Override
        public void reset() {
            //do nothing
        }

        @Nullable
        @Override
        public DBPDataSource getDataSource() {
            return null;
        }
    }

    /**
     * Spelling
     */
    private static class SpellingProblemCollector implements ISpellingProblemCollector {

        @Nullable
        private final IAnnotationModel annotationModel;
        private Map<Annotation, Position> addedAnnotations;
        private final int regionOffset;
        private final int regionLength;
        private final Object lockObject;

        public SpellingProblemCollector(
            @Nullable IAnnotationModel annotationModel,
            int regionOffset,
            int regionLength
        ) {
            this.annotationModel = annotationModel;
            if (this.annotationModel instanceof ISynchronizable) {
                Object amLock = ((ISynchronizable) this.annotationModel).getLockObject();
                lockObject = Objects.requireNonNullElse(amLock, this.annotationModel);
            } else {
                lockObject = Objects.requireNonNullElse(this.annotationModel, this);
            }
            this.regionOffset = regionOffset;
            this.regionLength = regionLength;
        }

        @Override
        public void accept(SpellingProblem problem) {
            addedAnnotations.put(
                new SpellingAnnotation(problem),
                new Position(problem.getOffset(), problem.getLength()));
        }

        @Override
        public void beginCollecting() {
            addedAnnotations = new HashMap<>();
        }

        @Override
        public void endCollecting() {
            if (annotationModel == null) {
                return;
            }
            List<Annotation> toRemove = new ArrayList<>();

            synchronized (lockObject) {
                if (annotationModel == null) {
                    addedAnnotations = null;
                    return;
                }
                Iterator<Annotation> iter = annotationModel.getAnnotationIterator();
                while (iter.hasNext()) {
                    Annotation annotation = iter.next();
                    if (annotation instanceof SpellingAnnotation) {
                        SpellingProblem spellingProblem = ((SpellingAnnotation) annotation).getSpellingProblem();
                        int problemOffset = spellingProblem.getOffset();
                        if (problemOffset >= regionOffset && problemOffset < regionOffset + regionLength) {
                            toRemove.add(annotation);
                        }
                    }
                }
                Annotation[] annotationsToRemove = toRemove.toArray(new Annotation[0]);

                if (annotationModel instanceof IAnnotationModelExtension) {
                    ((IAnnotationModelExtension) annotationModel).replaceAnnotations(annotationsToRemove, addedAnnotations);
                } else {
                    for (Annotation element : annotationsToRemove) {
                        annotationModel.removeAnnotation(element);
                    }
                    for (Map.Entry<Annotation, Position> entry : addedAnnotations.entrySet()) {
                        annotationModel.addAnnotation(entry.getKey(), entry.getValue());
                    }
                }
            }

            addedAnnotations = null;
        }
    }

}
