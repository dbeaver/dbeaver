package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;

import java.util.*;

//TODO: should reconciler work all the time or be toggled from preferences? RN it works all the time
public class SQLReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {
    private static final Log log = Log.getLog(SQLReconcilingStrategy.class);

    private final ScriptContainer scriptContainer = new ScriptContainer();

    private IProgressMonitor monitor; //TODO use it

    private SQLEditorBase editor;

    private IDocument document;

    public synchronized void setEditor(SQLEditorBase editor) {
        this.editor = editor;
    }

    @Override
    public synchronized void setProgressMonitor(IProgressMonitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public synchronized void initialReconcile() {
        ProjectionAnnotationModel annotationModel = editor.getAnnotationModel();
        if (annotationModel == null) {
            return;
        }
        Collection<SQLScriptElement> queries = extractQueries(0, document.getLength()); //fixme maybe -1?
        Map<Annotation, Position> addedAnnotationsMap = new HashMap<>(queries.size(), 1);
        for (SQLScriptElement element : queries) {
            MutableSQLScriptPosition scriptPosition = new MutableSQLScriptPosition(element.getOffset(), element.getLength());
            scriptContainer.add(scriptPosition);
            if (isMultiline(element.getOffset(), element.getLength())) {
                ProjectionAnnotation annotation = new ProjectionAnnotation();
                scriptPosition.setAnnotation(annotation);
                scriptPosition.setMultiline(true);
                addedAnnotationsMap.put(annotation, scriptPosition);
            }
        }
        annotationModel.modifyAnnotations(null, addedAnnotationsMap,null);
    }

    @Override
    public synchronized void setDocument(IDocument document) {
        this.document = document;
    }

    @Override
    public synchronized void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
        ProjectionAnnotationModel annotationModel = editor.getAnnotationModel();
        if (annotationModel == null) {
            return;
        }
        int regionOffset = subRegion.getOffset();
        int regionLength = subRegion.getLength();
        switch (dirtyRegion.getType()) {
            case DirtyRegion.INSERT:
                incrementalReconcileUponInsert(regionOffset, regionLength, annotationModel);
                break;
            case DirtyRegion.REMOVE:
                incrementalReconcileUponRemove(regionOffset, regionLength, annotationModel);
                break;
            default:
                log.warn("Unexpected type " + dirtyRegion.getType() + "of DirtyRegion has arrived");
        }
    }

    @Override
    public synchronized void reconcile(IRegion partition) {
        ProjectionAnnotationModel annotationModel = editor.getAnnotationModel();
        if (annotationModel == null) {
            return;
        }
        incrementalReconcileUponInsert(partition.getOffset(), partition.getLength(), annotationModel);
    }

    private Collection<SQLScriptElement> extractQueries(int offset, int length) {
        Collection<SQLScriptElement> queries = editor.extractScriptQueries(offset, length, false, true, false);
        if (queries == null) {
            editor.reloadParserContext();
            queries = editor.extractScriptQueries(offset, length, false, true, false);
        }
        return queries;
    }

    private boolean isMultiline(int offset, int length) {
        try {
            return document.getLineOfOffset(offset) != document.getLineOfOffset(offset + length);
        } catch (BadLocationException e) {
            throw new DBSQLReconcilingException(e);
        }
    }

    private void incrementalReconcileUponInsert(int regionOffset, int regionLength, ProjectionAnnotationModel model) { //FIXME Rename
        MutableSQLScriptPosition statementToTheLeft = scriptContainer.closestStatementStartingToTheLeft(regionOffset);
        MutableSQLScriptPosition statementToTheRight = scriptContainer.closestStatementStartingToTheRight(regionOffset + regionLength); //fixme and +1?
        int leftBound = 0;
        if (statementToTheLeft != null) {
            leftBound = statementToTheLeft.getLength() + statementToTheLeft.getOffset(); //fixme and +1?
        }
        int rightBound = document.getLength(); //fixme and -1?
        if (statementToTheRight != null) {
            Collection<SQLScriptElement> collection = extractQueries(statementToTheRight.getOffset(), statementToTheRight.getLength()); //fixme add 1?
            if (collection.size() == 1) {
                rightBound = statementToTheRight.getOffset() - 1; //fixme or not to subtract one?
            }
        }
        Collection<SQLScriptElement> actualQueries = extractQueries(leftBound, rightBound);
        ScriptContainer previouslyFoundQueries = scriptContainer.getPositionsInBound(leftBound, rightBound);
        reconcile(model, actualQueries, previouslyFoundQueries);
    }

    private void incrementalReconcileUponRemove(int regionOffset, int regionLength, ProjectionAnnotationModel model) {
        incrementalReconcileUponInsert(regionOffset, 0, model); //FIXME
    }

    private void reconcile(ProjectionAnnotationModel model, Collection<SQLScriptElement> actualQueries, ScriptContainer previouslyFoundQueries) {
        Map<Annotation, Position> addedAnnotations = new HashMap<>();
        Collection<Annotation> removedAnnotations = new ArrayList<>();
        Collection<MutableSQLScriptPosition> visited = new HashSet<>();
        for (SQLScriptElement element: actualQueries) {
            boolean isMultiline = isMultiline(element.getOffset(), element.getLength());
            MutableSQLScriptPosition position = previouslyFoundQueries.get(element.getOffset(), element.getLength());
            if (position == null) {
                position = new MutableSQLScriptPosition(element.getOffset(), element.getLength());
                if (isMultiline) {
                    ProjectionAnnotation annotation = new ProjectionAnnotation();
                    position.setMultiline(true);
                    position.setAnnotation(annotation);
                    addedAnnotations.put(annotation, position);
                }
                scriptContainer.add(position);
                continue;
            }
            visited.add(position);
            if (isMultiline != position.isMultiline()) {
                position.setMultiline(isMultiline);
                if (isMultiline) {
                    ProjectionAnnotation annotation = new ProjectionAnnotation();
                    position.setAnnotation(annotation);
                    addedAnnotations.put(annotation, position);
                } else {
                    removedAnnotations.add(position.getAnnotation());
                    position.setAnnotation(null);
                }
            }
        }
        for (MutableSQLScriptPosition position: previouslyFoundQueries) {
            if (!visited.contains(position)) {
                scriptContainer.remove(position);
                removedAnnotations.add(position.getAnnotation());
            }
        }
        model.modifyAnnotations(removedAnnotations.toArray(new Annotation[0]), addedAnnotations, null);
    }

    private static class DBSQLReconcilingException extends RuntimeException {
        DBSQLReconcilingException(BadLocationException e) {
            super(e);
        }
    }

    private static class ScriptContainer implements Iterable<MutableSQLScriptPosition> {
        //NB: comparator is not consistent with equals!
        private static final Comparator<MutableSQLScriptPosition> COMPARATOR = Comparator.comparingInt(Position::getOffset);

        private final NavigableSet<MutableSQLScriptPosition> container;

        ScriptContainer() {
            container = new TreeSet<>(COMPARATOR);
        }

        private ScriptContainer(NavigableSet<MutableSQLScriptPosition> container) {
            this.container = container;
        }

        @Nullable
        MutableSQLScriptPosition get(int offset, int length) {
            Iterator<MutableSQLScriptPosition> iterator = container.tailSet(new MutableSQLScriptPosition(offset, length)).iterator();
            if (!iterator.hasNext()) {
                return null;
            }
            MutableSQLScriptPosition position = iterator.next();
            if (position.getOffset() == offset && position.getLength() == length) {
                return position;
            }
            return null;
        }

        void add(MutableSQLScriptPosition position) {
            container.add(position);
        }

        void remove(MutableSQLScriptPosition position) {
            container.remove(position);
        }

        @Nullable
        //fixme rename
        //fixme delete goes to the right and finds the first position with pos.offset >= offset
        MutableSQLScriptPosition closestStatementStartingToTheLeft(int offset) {
            return container.lower(new MutableSQLScriptPosition(offset + 1, 0));
        }

        @Nullable
        //fixme rename
        //fixme delete goes to the left and finds the first position with pos.offset <= offset
        MutableSQLScriptPosition closestStatementStartingToTheRight(int offset) {
            return container.higher(new MutableSQLScriptPosition(offset - 1, 0));
        }

        //returns unmodifiable view
        //inclusive
        ScriptContainer getPositionsInBound(int leftBound, int rightBound) {
            MutableSQLScriptPosition start = container.higher(new MutableSQLScriptPosition(leftBound - 1, 0));
            MutableSQLScriptPosition end = container.lower(new MutableSQLScriptPosition(rightBound, 0));
            if (start != null && end != null) {
                return new ScriptContainer(container.subSet(start, true, end, true));
            }
            if (start == null && end != null) {
                return new ScriptContainer(container.tailSet(end, true));
            }
            if (start != null) {
                return new ScriptContainer(container.headSet(start, true));
            }
            return new ScriptContainer();
        }

        @NotNull
        @Override
        public Iterator<MutableSQLScriptPosition> iterator() {
            return container.iterator();
        }
    }
}
