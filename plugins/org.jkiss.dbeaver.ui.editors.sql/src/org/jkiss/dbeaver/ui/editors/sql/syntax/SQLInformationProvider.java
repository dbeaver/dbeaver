/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.IInformationProviderExtension;
import org.eclipse.jface.text.information.IInformationProviderExtension2;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.BorderData;
import org.eclipse.swt.layout.BorderLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionHelper;
import org.jkiss.dbeaver.model.sql.parser.SQLIdentifierDetector;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultPseudoColumn;
import org.jkiss.dbeaver.model.sql.semantics.model.ddl.SQLQueryObjectDataModel;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsTableDataModel;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.ui.AbstractPartListener;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.util.AnnotationsInformationView;
import org.jkiss.dbeaver.ui.editors.sql.util.ObjectInformationView;
import org.jkiss.dbeaver.ui.editors.sql.util.SQLAnnotationHover;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;
import java.util.Set;
import java.util.stream.IntStream;

public class SQLInformationProvider implements IInformationProvider, IInformationProviderExtension, IInformationProviderExtension2 {

    private static final Set<SQLQuerySymbolClass> ignoredSymbolClassDescription = Set.of(
        SQLQuerySymbolClass.ERROR,
        SQLQuerySymbolClass.UNKNOWN,
        SQLQuerySymbolClass.QUOTED
    );

    class EditorWatcher extends AbstractPartListener {

        @Override
        public void partClosed(IWorkbenchPart part) {
            if (part == editor) {
                editor.getSite().getWorkbenchWindow().getPartService().removePartListener(partListener);
                partListener = null;
            }
        }

        @Override
        public void partActivated(IWorkbenchPart part) {
            update();
        }

        @Override
        public void partBroughtToTop(IWorkbenchPart part) {
            update();
        }
    }

    protected SQLEditorBase editor;
    private final SQLContextInformer contextInformer;
    private IPartListener partListener;

    private String currentPerspective;
    @NotNull
    protected SQLAnnotationHover annotationHover;
    private IInformationControlCreator informationControlCreator;

    public SQLInformationProvider(@NotNull SQLEditorBase editor, @NotNull SQLContextInformer contextInformer) {
        this.editor = editor;
        this.contextInformer = contextInformer;
        this.annotationHover = new SQLAnnotationHover(editor);

        if (this.editor instanceof SQLEditor) {
            partListener = new EditorWatcher();
            IWorkbenchWindow window = this.editor.getSite().getWorkbenchWindow();
            window.getPartService().addPartListener(partListener);
            update();
        }
    }

    protected void update() {
        IWorkbenchWindow window = editor.getSite().getWorkbenchWindow();
        if (window == null) {
            return;
        }
        IWorkbenchPage page = window.getActivePage();
        if (page != null) {
            IPerspectiveDescriptor perspective = page.getPerspective();
            if (perspective != null) {
                String perspectiveId = perspective.getId();
                if (currentPerspective == null || !currentPerspective.equals(perspectiveId)) {
                    currentPerspective = perspectiveId;
                    this.annotationHover.setEditor(editor);
                }
            }
        }
    }

    @Override
    public IRegion getSubject(@NotNull ITextViewer textViewer, int offset) {
        final Point selectedRange = textViewer.getSelectedRange();

        IRegion hoverRegion = this.annotationHover.getHoverRegion(textViewer, offset);

        SQLDocumentSyntaxContext context = this.editor.getSyntaxContext();
        SQLQuerySymbolEntry symbolEntry = context == null ? null : context.findToken(offset);
        IRegion symbolRegion = symbolEntry == null
            ? null
            : new Region(context.getLastAccessedTokenOffset(), symbolEntry.getInterval().length());

        this.contextInformer.searchInformation(new Region(offset, 0));
        SQLIdentifierDetector.WordRegion wordRegion = contextInformer.getWordRegion();
        return new SubjectRegion(
            selectedRange.y > 1 ? new Region(selectedRange.x, selectedRange.y) : null,
            hoverRegion,
            symbolEntry, symbolRegion,
            wordRegion == null || wordRegion.isEmpty() ? null : wordRegion
        );
    }

    /*
     * @deprecated
     */
    @Override
    public String getInformation(ITextViewer textViewer, IRegion subject) {
        Object information = getInformation2(textViewer, subject);
        return information == null ? null : information.toString();
    }

    @Override
    public Object getInformation2(@NotNull ITextViewer textViewer, @NotNull IRegion subject) {
        if (subject instanceof SubjectRegion subjectRegion && subjectRegion.isEmpty()) {
            return null;
        }

        AnnotationsInformationView.AnnotationsHoverInfo annotationsInfo;
        String message;
        Object info;
        if (subject instanceof SubjectRegion subjectRegion) {
            annotationsInfo = subjectRegion.hoverRegion != null &&
                (subjectRegion.selectionRegion == null || equalRegions(subjectRegion.hoverRegion, subjectRegion.selectionRegion))
                ? this.annotationHover.getAnnotationsHoverInfo(textViewer, subjectRegion.hoverRegion, null, true)
                : null;

            if (subjectRegion.symbolEntry != null
                && (subjectRegion.selectionRegion == null || equalRegions(subjectRegion.symbolRegion, subjectRegion.selectionRegion))
            ) {
                SQLQuerySymbolEntry symbolEntry = subjectRegion.symbolEntry;
                while (symbolEntry.getDefinition() instanceof SQLQuerySymbolEntry def && symbolEntry.getDefinition() != def) {
                    symbolEntry = def;
                }

                DBSObject dbObject;
                switch (symbolEntry.getDefinition()) {
                    case SQLQueryObjectDataModel byObjDataRefDef -> {
                        dbObject = byObjDataRefDef.getObject();
                    }
                    case SQLQuerySymbolByDbObjectDefinition byObjDef -> {
                        dbObject = byObjDef.getDbObject();
                    }
                    case SQLQueryRowsTableDataModel byTableRefDef -> {
                        dbObject = byTableRefDef.getImmediateTargetObject();
                    }
                    case SQLQueryResultPseudoColumn pseudoColumn -> {
                        dbObject = null;
                        message = pseudoColumn.description + "\n"
                            + "Pseudo-column" + (
                                pseudoColumn.source == null ? "" : (" derived from the " + (
                                    pseudoColumn.realSource == null
                                        ? "query part: \n" + pseudoColumn.source.getSyntaxNode().getTextContent()
                                        : (pseudoColumn.realSource.getName()
                                            + SQLQuerySemanticUtils.getObjectTypeName(pseudoColumn.realSource))
                                ))
                            );
                    }
                    case SQLQuerySymbolEntry defSymbolEntry -> {
                        dbObject = null;
                    }
                    case null -> {
                        dbObject = null;
                    }
                    default -> throw new IllegalStateException("Not implemented");
                };

                if (dbObject != null) {
                    info = dbObject;
                    DBNModel navModel = dbObject.getDataSource().getContainer().getProject().getNavigatorModel();
                    if (navModel != null) {
                        navModel.getNodeByObject(new VoidProgressMonitor(), dbObject, true);
                    }
                    message = null;
                } else {
                    info = null;
                    message = SQLCompletionHelper.readAdditionalProposalInfo(
                        null,
                        editor.getCompletionContext(),
                        dbObject,
                        contextInformer.getKeywords(),
                        contextInformer.getKeywordType()
                    );
                }
                if (message == null  || message.equals(symbolEntry.getRawName())
                    || (contextInformer.getKeywords().length > 0 && message.equals(contextInformer.getKeywords()[0]))) {
                    message = ignoredSymbolClassDescription.contains(symbolEntry.getSymbolClass())
                        ? null
                        : symbolEntry.getSymbolClass().getDescription();
                }
            } else {
                info = this.prepareInformerAdditionalInfo();
                message = null;
            }
        } else {
            annotationsInfo = this.annotationHover.getAnnotationsHoverInfo(textViewer, subject, null, true);
            contextInformer.searchInformation(subject);
            info = this.prepareInformerAdditionalInfo();
            message = null;
        }
        if (info instanceof String text) {
            message = text;
            info = null;
        }
        if (annotationsInfo == null) {
            annotationsInfo = new AnnotationsInformationView.AnnotationsHoverInfo(Collections.emptyList(), subject, -1);
        }
        return new SubjectInformation(annotationsInfo, message, info);
    }

    @Nullable
    private Object prepareInformerAdditionalInfo() {
        DBSObject object = null;
        if (this.contextInformer.hasObjects() && this.contextInformer.getKeywordType() != DBPKeywordType.KEYWORD) {
            // Make object description
            DBRProgressMonitor monitor = new VoidProgressMonitor();
            final DBSObjectReference objectRef = contextInformer.getObjectReferences().getFirst();

            try {
                object = objectRef.resolveObject(monitor);
            } catch (DBException e) {
                // Can't resolve
                return e.getMessage();
            }
        } else if (ArrayUtils.isEmpty(contextInformer.getKeywords())) {
            return null;
        }
        if (object != null) {
            DBNModel navModel = object.getDataSource().getContainer().getProject().getNavigatorModel();
            if (navModel != null) {
                navModel.getNodeByObject(new VoidProgressMonitor(), object, true);
            }
            return object;
        }
        Object info = SQLCompletionHelper.readAdditionalProposalInfo(
            null,
            editor.getCompletionContext(),
            object,
            contextInformer.getKeywords(),
            contextInformer.getKeywordType()
        );
        if ((info == null || (contextInformer.getKeywords().length > 0 && info.equals(contextInformer.getKeywords()[0])))) {
            DBPKeywordType keywordType = contextInformer.getKeywordType();
            if (keywordType != null) {
                info = switch (keywordType) {
                    case KEYWORD -> "Keyword";
                    case FUNCTION -> "Function";
                    default -> null;
                };
            } else {
                info = null;
            }
        }
        return info;
    }

    @NotNull
    @Override
    public IInformationControlCreator getInformationPresenterControlCreator() {
        if (informationControlCreator == null) {
            informationControlCreator = SQLSymbolInformationControl::new;
        }
        return informationControlCreator;
    }

    private record SubjectInformation(
        @NotNull AnnotationsInformationView.AnnotationsHoverInfo annotationsInfo,
        @Nullable String message,
        @Nullable Object info
    ) {
    }

    private record SubjectRegion(
        @Nullable IRegion selectionRegion,
        @Nullable IRegion hoverRegion,
        @Nullable SQLQuerySymbolEntry symbolEntry,
        @Nullable IRegion symbolRegion,
        @Nullable SQLIdentifierDetector.WordRegion wordRegion
    ) implements IRegion {

        public boolean isEmpty() {
            return this.selectionRegion == null
                && this.hoverRegion == null
                && this.symbolEntry == null
                && this.symbolRegion == null
                && this.wordRegion == null;
        }

        @Override
        public int getLength() {
            return IntStream.of(
                this.selectionRegion == null ? Integer.MIN_VALUE : (this.selectionRegion.getOffset() + this.selectionRegion.getLength()),
                this.hoverRegion == null ? Integer.MIN_VALUE : (this.hoverRegion.getOffset() + this.hoverRegion.getLength()),
                this.symbolRegion == null ? Integer.MIN_VALUE : (this.symbolRegion.getOffset() + this.symbolRegion.getLength()),
                this.wordRegion == null ? Integer.MIN_VALUE : (this.wordRegion.identEnd)
            ).max().orElse(Integer.MIN_VALUE) - this.getOffset();
        }

        @Override
        public int getOffset() {
            return IntStream.of(
                this.selectionRegion == null ? Integer.MAX_VALUE : this.selectionRegion.getOffset(),
                this.hoverRegion == null ? Integer.MAX_VALUE : this.hoverRegion.getOffset(),
                this.symbolRegion == null ? Integer.MAX_VALUE : this.symbolRegion.getOffset(),
                this.wordRegion == null ? Integer.MAX_VALUE : this.wordRegion.identStart
            ).min().orElse(Integer.MAX_VALUE);
        }
    }

    private static boolean equalRegions(@NotNull IRegion a, @NotNull IRegion b) {
        return a.getOffset() == b.getOffset() && a.getLength() == b.getLength();
    }

    private class SQLSymbolInformationControl extends DefaultInformationControl implements IInformationControlExtension2 {

        private Composite contentParent;

        private AnnotationsInformationView annotationsInfoView;
        private ObjectInformationView objectInformationView;

        private Boolean hasContents = null;

        public SQLSymbolInformationControl(@NotNull Shell shell) {
            super(shell, true);
        }

        @Override
        protected void createContent(@NotNull Composite parent) {
            this.contentParent = parent;
            super.createContent(parent);

            BorderLayout layout = new BorderLayout();
//            layout.type = SWT.VERTICAL;
            parent.setLayout(layout);
            for (Control c : parent.getChildren()) {
                c.setLayoutData(new BorderData(SWT.CENTER));
            }
            parent.getShell().setMinimumSize(this.computeSizeConstraints(60, 6));

            this.annotationsInfoView = new AnnotationsInformationView(this, editor);
            this.annotationsInfoView.setForceAnnotationIcon(true);
            this.annotationsInfoView.createControl(parent).setLayoutData(new BorderData(SWT.TOP));
        }

        @Override
        public boolean hasContents() {
            return this.hasContents != null ?  this.hasContents : super.hasContents();
        }

        @Override
        public void setInput(@NotNull Object input) {
            if (input instanceof SubjectInformation info) {

                this.annotationsInfoView.setLinksInformation(info.annotationsInfo);
                this.hasContents = !info.annotationsInfo.annotationsGroups().isEmpty();

                if (info.info instanceof DBPNamedObject object) {
                    this.objectInformationView = new ObjectInformationView();
                    this.objectInformationView.createContent(this.contentParent).setLayoutData(new BorderData(SWT.CENTER));
                    this.objectInformationView.setInput(object);
                    this.hasContents = true;
                    this.setInformation(null);
                }

                this.setInformation(info.message);
                this.hasContents |= CommonUtils.isNotEmpty(info.message);
            } else {
                super.setInformation(CommonUtils.toString(input));
            }
        }

        @Override
        public void setVisible(boolean visible) {
            if (visible) {
                this.annotationsInfoView.show();
            }
            super.setVisible(visible);
        }
    }
}