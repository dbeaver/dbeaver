/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.semantics;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.text.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.part.WorkbenchPart;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.semantics.*;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDummyDataSourceContext.DummyTableRowsSource;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.sql.semantics.model.ddl.*;
import org.jkiss.dbeaver.model.sql.semantics.model.dml.*;
import org.jkiss.dbeaver.model.sql.semantics.model.expressions.*;
import org.jkiss.dbeaver.model.sql.semantics.model.select.*;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.ui.AbstractUIJob;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLEditorHandlerToggleOutlineView;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class SQLEditorOutlinePage extends ContentOutlinePage implements IContentOutlinePage {
    
    private static final String LABEL_PROPERTY_KEY = "LABEL";

    private static final int SQL_QUERY_ORIGINAL_TEXT_PREVIEW_LENGTH = 100;
    @NotNull
    private final SQLEditorBase editor;
    @Nullable
    private TreeViewer treeViewer;
    private OutlineScriptNode scriptNode;
    @NotNull
    private final List<OutlineNode> rootNodes;
    @NotNull
    private SelectionSyncOperation currentSelectionSyncOp = SelectionSyncOperation.NONE;
    @NotNull
    private final SQLOutlineNodeBuilder currentNodeBuilder = new SQLOutlineNodeFullBuilder();

    @NotNull
    private final OutlineRefreshJob refreshJob = new OutlineRefreshJob();

    @NotNull
    private final CaretListener caretListener = event -> {
        if (currentSelectionSyncOp == SelectionSyncOperation.NONE) {
            LinkedList<OutlineNode> path = new LinkedList<>();
            int offset = event.caretOffset;
            OutlineNode node = scriptNode;
            OutlineNode nextNode = scriptNode;
            path.add(node);
            while (nextNode != null) {
                node = nextNode;
                nextNode = null;
                for (int i = 0; i < node.getChildrenCount(); i++) {
                    OutlineNode child = node.getChild(i);
                    IRegion range = child.getTextRange();
                    if (range != null && range.getOffset() <= offset) {
                        nextNode = child;
                        path.add(child);
                    } else {
                        break;
                    }
                }
            }

            if (node != null) {
                currentSelectionSyncOp = SelectionSyncOperation.FROM_EDITOR;
                treeViewer.getTree().setRedraw(false);
                treeViewer.reveal(new TreePath(path.toArray(OutlineNode[]::new)));
                treeViewer.setSelection(new StructuredSelection(node), true);
                treeViewer.getTree().setRedraw(true);
                currentSelectionSyncOp = SelectionSyncOperation.NONE;
            }
        }
    };

    @NotNull
    private final ITextInputListener textInputListener = new ITextInputListener() {

        @Override
        public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
            if (newInput != null) {
                treeViewer.setInput(editor.getEditorInput());
            }
        }

        @Override
        public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
        }
    };

    @NotNull
    private final IPropertyListener editorPropertyListener = (Object source, int propId) -> {
        if (propId == WorkbenchPart.PROP_TITLE) {
            treeViewer.update(this.scriptNode, new String[] { LABEL_PROPERTY_KEY });
        }
    };
    
    public SQLEditorOutlinePage(@NotNull SQLEditorBase editor) {
        this.editor = editor;
        this.rootNodes = List.of(this.scriptNode = new OutlineScriptNode());
    }

    /**
     * Refresh outline
     */
    public void refresh() {
        this.scriptNode.updateChildren();
        this.refreshJob.schedule(true);
    }
    
    @NotNull
    private SQLOutlineNodeBuilder getNodeBuilder() {
        return this.currentNodeBuilder;
    }

    @Override
    protected int getTreeStyle() {
        return super.getTreeStyle() | SWT.FULL_SELECTION | SWT.VIRTUAL;
    }

    @Override
    public void createControl(@NotNull Composite parent) {
        super.createControl(parent);
        this.treeViewer = super.getTreeViewer();
        this.treeViewer.setContentProvider(new ILazyTreeContentProvider() {
            @Override
            public void updateElement(@NotNull Object parent, int index) {
                if (parent == editor.getEditorInput()) {
                    this.updateChildNode(parent, index, rootNodes.get(index));
                } else if (parent instanceof OutlineNode node) {
                    this.updateChildNode(parent, index, node.getChild(index));
                } else {
                    throw new IllegalStateException(); // should never happen
                }
            }

            private void updateChildNode(@NotNull Object parent, int index, OutlineNode childNode) {
                treeViewer.replace(parent, index, childNode);
                treeViewer.setChildCount(childNode, childNode.getChildrenCount());
            }

            @Override
            public void updateChildCount(@NotNull Object element, int currentChildCount) {
                if (element == editor.getEditorInput()) {
                    treeViewer.setChildCount(element, rootNodes.size());
                } else if (element instanceof OutlineNode node) {
                    treeViewer.setChildCount(element, node.getChildrenCount());
                } else {
                    throw new IllegalStateException(); // should never happen
                }
            }

            @Nullable
            @Override
            public Object getParent(@NotNull Object element) {
                return element instanceof OutlineNode node ? node.getParent() : null;
            }
        });

        this.treeViewer.setUseHashlookup(true);
        this.treeViewer.setLabelProvider(new DecoratingStyledCellLabelProvider(new SQLOutlineLabelProvider(), null, null));
        this.treeViewer.setInput(editor.getEditorInput());
        this.treeViewer.setAutoExpandLevel(3);

        TextViewer textViewer = this.editor.getTextViewer();
        if (textViewer != null) {
            textViewer.getTextWidget().addCaretListener(this.caretListener);
            textViewer.addTextInputListener(this.textInputListener);
        }
        
        this.editor.addPropertyListener(editorPropertyListener);

        SQLEditorHandlerToggleOutlineView.refreshCommandState(editor.getSite());
        this.refreshJob.schedule(true);
    }

    @Override
    public void selectionChanged(@NotNull SelectionChangedEvent event) {
        if (currentSelectionSyncOp == SelectionSyncOperation.NONE) {
            if (event.getSelection() instanceof IStructuredSelection selection && selection.getFirstElement() instanceof OutlineNode node) {
                IRegion textRange = node.getTextRange();
                if (textRange != null) {
                    currentSelectionSyncOp = SelectionSyncOperation.TO_EDITOR;
                    this.editor.selectAndReveal(textRange.getOffset(), textRange.getLength());
                    currentSelectionSyncOp = SelectionSyncOperation.NONE;
                }
            }
        }

        super.selectionChanged(event);
    }

    @NotNull
    private String prepareQueryPreview(@NotNull SQLDocumentScriptItemSyntaxContext scriptElement) {
        return this.prepareQueryPreview(scriptElement.getOriginalText());
    }

    @NotNull
    private String prepareQueryPreview(@NotNull String queryOriginalText) {
        int queryOriginalLength = queryOriginalText.length();
        int queryPreviewLength = Math.min(queryOriginalLength, SQL_QUERY_ORIGINAL_TEXT_PREVIEW_LENGTH);
        StringBuilder result = new StringBuilder(queryPreviewLength);
        try (Scanner scanner = new Scanner(queryOriginalText)) {
            while (scanner.hasNextLine() && result.length() < queryPreviewLength) {
                String line = scanner.nextLine().trim();
                int fragmentLength = Math.min(line.length(), queryPreviewLength - result.length());
                result.append(line, 0, fragmentLength).append(" ");
            }
        }
        if (result.length() < queryOriginalLength) {
            result.append(" ...");
        }
        return result.toString();
    }

    @Override
    public void dispose() {
        this.editor.removePropertyListener(editorPropertyListener);
        
        TextViewer textViewer = this.editor.getTextViewer();
        if (textViewer != null) {
            textViewer.getTextWidget().removeCaretListener(this.caretListener);
            textViewer.removeTextInputListener(this.textInputListener);
        }
        
        this.scriptNode.dispose();
        super.dispose();
        
        SQLEditorHandlerToggleOutlineView.refreshCommandState(editor.getSite());
    }

    private enum SelectionSyncOperation {
        NONE, FROM_EDITOR, TO_EDITOR
    }

    private interface SQLOutlineNodeBuilder extends SQLQueryNodeModelVisitor<OutlineQueryNode, Object> {
    }

    private static class SQLOutlineLabelProvider implements ILabelProvider, IFontProvider, IStyledLabelProvider {
        @Override
        public void addListener(@Nullable ILabelProviderListener listener) {
            // no listeners
        }

        @Override
        public boolean isLabelProperty(@Nullable Object element, @Nullable String property) {
            return property != null && property.equals(LABEL_PROPERTY_KEY);
        }

        @Override
        public void removeListener(@Nullable ILabelProviderListener listener) {
            // no listeners
        }

        @Nullable
        @Override
        public Font getFont(@Nullable Object element) {
            // default font
            return null;
        }

        @Nullable
        @Override
        public Image getImage(@NotNull Object element) {
            return element instanceof OutlineNode node ? node.getImage() : DBeaverIcons.getImage(DBIcon.TYPE_UNKNOWN, false);
        }

        @NotNull
        @Override
        public String getText(@NotNull Object element) {
            return element instanceof OutlineNode node ? node.getText() : element.toString();
        }

        @NotNull
        @Override
        public StyledString getStyledText(@NotNull Object element) {
            StyledString result = new StyledString();
            if (element instanceof OutlineNode node) {
                String text = node.getText();
                result.append(text);
                
                String extra = node.getExtraText(); 
                if (extra != null) {
                    result.append(extra);
                    result.setStyle(text.length(), extra.length(), StyledString.DECORATIONS_STYLER);
                }
            } else {
                result.append(element.toString());
            }
            return result;
        }

        @Override
        public void dispose() {
        }
    }

    private abstract class OutlineNode {

        @Nullable
        private final OutlineNode parentNode;

        public OutlineNode(@Nullable OutlineNode parentNode) {
            this.parentNode = parentNode;
        }

        public abstract IRegion getTextRange();

        @Nullable
        public final OutlineNode getParent() {
            return this.parentNode;
        }

        public abstract String getText();

        @Nullable
        public String getExtraText() {
            return null;
        }

        @Nullable
        public Image getImage() {
            DBPImage icon = this.getIcon();
            return icon == null ? null : DBeaverIcons.getImage(icon, false);
        }

        protected abstract DBPImage getIcon();

        public abstract int getChildrenCount();

        public abstract OutlineNode getChild(int index);
    }

    private class OutlineInfoNode extends OutlineNode {
        @NotNull
        private final String title;
        @NotNull
        private final DBIcon icon;

        public OutlineInfoNode(@NotNull OutlineNode parentNode, @NotNull String title, @NotNull DBIcon icon) {
            super(parentNode);
            this.title = title;
            this.icon = icon;
        }

        @NotNull
        @Override
        public String getText() {
            return this.title;
        }

        @NotNull
        @Override
        public DBIcon getIcon() {
            return this.icon;
        }

        @Nullable
        @Override
        public OutlineNode getChild(int index) {
            return null;
        }

        @Override
        public int getChildrenCount() {
            return 0;
        }

        @Nullable
        @Override
        public IRegion getTextRange() {
            return null;
        }
    }

    private class OutlineScriptNode extends OutlineNode {
        @Nullable
        private SQLDocumentSyntaxContext documentContext;
        @Nullable
        private Image editorImage = null;
        @Nullable
        private Image outlineImage = null;

        @NotNull
        private final OutlineNode noElementsNode = new OutlineInfoNode(
            this,
            SQLEditorMessages.sql_editor_outline_no_elements_label,
            DBIcon.SMALL_INFO
        );
        @NotNull
        private final OutlineNode analysisDisabledNode = new OutlineInfoNode(
            this,
            SQLEditorMessages.sql_editor_outline_query_analysis_disabled_label,
            DBIcon.SMALL_INFO
        );

        @NotNull
        private Map<SQLDocumentScriptItemSyntaxContext, OutlineNode> elements = new HashMap<>();
        @NotNull
        private List<OutlineNode> children = Collections.emptyList();
        @NotNull
        private final SQLDocumentSyntaxContextListener syntaxContextListener = new SQLDocumentSyntaxContextListener() {
            @Override
            public void onScriptItemInvalidated(@Nullable SQLDocumentScriptItemSyntaxContext item) {
                refreshJob.schedule(true);
            }

            @Override
            public void onScriptItemIntroduced(@Nullable SQLDocumentScriptItemSyntaxContext item) {
                refreshJob.schedule(true);
            }

            @Override
            public void onAllScriptItemsInvalidated() {
                refreshJob.schedule(true);
            }
        };

        public OutlineScriptNode() {
            super(null);
            if (editor.isAdvancedHighlightingEnabled() && SQLEditorUtils.isSQLSyntaxParserEnabled(editor.getEditorInput())) {
                this.documentContext = editor.getSyntaxContext();
                this.documentContext.addListener(syntaxContextListener);
            }
        }

        @NotNull
        @Override
        public IRegion getTextRange() {
            return new Region(0, 0);
        }

        private void updateElements() {
            if (documentContext != null) {
                this.elements = documentContext.getScriptItems().stream()
                    .collect(Collectors.toMap(
                        e -> e.item,
                        e -> new OutlineScriptElementNode(this, e.offset, e.item),
                        (a, b) -> a, LinkedHashMap::new)
                    );
            }
        }

        private void updateChildren() {
            if (editor.isAdvancedHighlightingEnabled() && SQLEditorUtils.isSQLSyntaxParserEnabled(editor.getEditorInput())) {
                if (this.documentContext == null || editor.getSyntaxContext() != this.documentContext) {
                    if (this.documentContext != null) {
                        this.documentContext.removeListener(this.syntaxContextListener);
                    }
                    this.documentContext = editor.getSyntaxContext();
                    this.documentContext.addListener(this.syntaxContextListener);
                }
                if (this.elements.isEmpty()) {
                    this.children = List.of(this.noElementsNode);
                } else {
                    this.children = List.copyOf(this.elements.values());
                }
            } else {
                this.children = List.of(this.analysisDisabledNode);
            }
        }

        @Override
        public int getChildrenCount() {
            return this.children.size();
        }

        @NotNull
        @Override
        public OutlineNode getChild(int index) {
            return this.children.get(index);
        }

        @NotNull
        @Override
        public String getText() {
            return editor.getTitle();
        }

        @NotNull
        @Override
        public Image getImage() {
            // separate image lifetime, because we don't have a guarantee that outline will be disposed earlier than editor
            // the outline state changes to the response of the editor event asynchronously, so their lifetimes are a little bit different
            Image image = editor.getTitleImage();
            if (this.editorImage != image) {
                if (this.outlineImage != null) {
                    this.outlineImage.dispose();
                }
                this.outlineImage = new Image(image.getDevice(), image, SWT.IMAGE_COPY);
                this.editorImage = image;
            }
            return this.outlineImage;
        }

        @NotNull
        @Override
        protected DBIcon getIcon() {
            return DBIcon.TREE_SCRIPT;
        }

        public void dispose() {
            if (documentContext != null) {
                this.documentContext.removeListener(syntaxContextListener);
            }
            if (this.outlineImage != null && !this.outlineImage.isDisposed()) {
                this.outlineImage.dispose();
            }
        }
    }

    private class OutlineScriptElementNode extends OutlineQueryNode {
        private final int offset;
        @NotNull
        private final SQLDocumentScriptItemSyntaxContext scriptElement;

        public OutlineScriptElementNode(
            @NotNull OutlineScriptNode parent,
            int offset,
            @NotNull SQLDocumentScriptItemSyntaxContext scriptElement
        ) {
            super(
                parent,
                scriptElement.getQueryModel(),
                OutlineQueryNodeKind.DEFAULT,
                prepareQueryPreview(scriptElement),
                null,
                UIIcon.SQL_EXECUTE,
                scriptElement.getQueryModel()
            );
            this.offset = offset;
            this.scriptElement = scriptElement;
        }

        public int getOffset() {
            return this.offset;
        }

        @NotNull
        @Override
        public IRegion getTextRange() {
            return new Region(this.offset, 0);
        }
    }

    /**
     * Outline-specific nodes classification
     */
    private enum OutlineQueryNodeKind {
        DEFAULT,
        NATURAL_JOIN_SUBROOT,
        PROJECTION_SUBROOT,
        DELETE_SUBROOT,
        INSERT_SUBROOT,
        UPDATE_SUBROOT
    }

    private class OutlineQueryNode extends OutlineNode {
        @NotNull
        private final SQLQueryNodeModel model;
        @NotNull
        private final OutlineQueryNodeKind kind;
        private final String text;
        @Nullable
        private final String extraText;
        @NotNull
        private final DBPImage icon;
        @NotNull
        private final SQLQueryNodeModel[] childModels;
        @NotNull
        private final IRegion textRange;
        @Nullable
        private List<OutlineNode> children;

        public OutlineQueryNode(
            @NotNull OutlineNode parentNode,
            @NotNull SQLQueryNodeModel model,
            @NotNull OutlineQueryNodeKind kind,
            @NotNull String text,
            @Nullable String extraText,
            @NotNull DBPImage icon,
            @NotNull SQLQueryNodeModel... childModels
        ) {
            super(parentNode);
            this.model = model;
            this.kind = kind;
            this.text = text;
            this.extraText = extraText;
            this.icon = icon;
            this.childModels = childModels;

            int offset = parentNode instanceof OutlineScriptElementNode element ?
                element.getOffset() + this.model.getInterval().a :
                parentNode instanceof OutlineQueryNode queryNode ?
                    queryNode.getTextRange().getOffset() - queryNode.model.getInterval().a + this.model.getInterval().a :
                    parentNode.getTextRange().getOffset();

            this.textRange = new Region(offset, this.model.getInterval().length());
            this.children = null;
        }

        @NotNull
        @Override
        public IRegion getTextRange() {
            return this.textRange;
        }

        @NotNull
        private List<OutlineNode> obtainChildren() {
            if (this.children == null) {
                if (this.childModels.length == 0) {
                    this.children = Collections.emptyList();
                } else {
                    this.children = new ArrayList<>(childModels.length);
                    for (SQLQueryNodeModel childModel : childModels) {
                        if (childModel != null) {
                            childModel.apply(getNodeBuilder(), this);
                        }
                    }
                }
            }
            return this.children;
        }

        @NotNull
        @Override
        public String getText() {
            return this.text;
        }

        @Nullable
        @Override
        public String getExtraText() {
            return this.extraText;
        }
        
        @NotNull
        @Override
        protected DBPImage getIcon() {
            return this.icon;
        }

        @Override
        public int getChildrenCount() {
            return this.obtainChildren().size();
        }

        @NotNull
        @Override
        public OutlineNode getChild(int index) {
            return this.obtainChildren().get(index);
        }
    }

    @NotNull
    private OutlineScriptElementNode getScriptElementNode(@NotNull OutlineQueryNode node) {
        OutlineNode n = node;
        while (n != null) {
            if (n instanceof OutlineScriptElementNode e) {
                return e;
            } else {
                n = n.getParent();
            }
        }
        throw new IllegalStateException();
    }
    
    private class SQLOutlineNodeFullBuilder implements SQLOutlineNodeBuilder {
        @NotNull
        private static final Map<SQLQueryExprType, DBIcon> wellKnownTypeIcons = Map.of(
            SQLQueryExprType.UNKNOWN, DBIcon.TYPE_UNKNOWN,
            SQLQueryExprType.STRING, DBIcon.TYPE_STRING,
            SQLQueryExprType.BOOLEAN, DBIcon.TYPE_BOOLEAN,
            SQLQueryExprType.NUMERIC, DBIcon.TYPE_NUMBER,
            SQLQueryExprType.DATETIME, DBIcon.TYPE_DATETIME
        );

        @Nullable
        @Override
        public Object visitRowsCte(@NotNull SQLQueryRowsCteModel cte, @NotNull OutlineQueryNode node) {
            this.makeNode(node, cte, "CTE", DBIcon.TREE_FOLDER_LINK, cte.getAllQueries().toArray(SQLQueryRowsSourceModel[]::new));
            return null;
        }

        @Nullable
        @Override
        public Object visitRowsCteSubquery(@NotNull SQLQueryRowsCteSubqueryModel cteSubquery, @NotNull OutlineQueryNode node) {
            String text = cteSubquery.subqueryName == null ? SQLConstants.QUESTION : cteSubquery.subqueryName.getRawName();
            SQLQueryRowsSourceModel[] children = Stream.of(cteSubquery.source).filter(Objects::nonNull).toArray(SQLQueryRowsSourceModel[]::new);
            this.makeNode(node, cteSubquery, text, DBIcon.TREE_TABLE_LINK, children);
            return null;
        }

        @Nullable
        @Override
        public Object visitValueSubqueryExpr(@NotNull SQLQueryValueSubqueryExpression subqueryExpr, @NotNull OutlineQueryNode node) {
            this.makeNode(node, subqueryExpr, "Scalar subquery", UIIcon.FILTER_VALUE, subqueryExpr.getSource());
            return null;
        }

        @Nullable
        @Override
        public Object visitValueFlatExpr(@NotNull SQLQueryValueFlattenedExpression flattenedExpr, @NotNull OutlineQueryNode node) {
            switch (flattenedExpr.getOperands().size()) {
                case 0 -> {
                } /* do nothing */
                case 1 -> flattenedExpr.getOperands().get(0).apply(this, node);
                default ->
                    this.makeNode(
                        node,
                        flattenedExpr,
                        prepareQueryPreview(flattenedExpr.getExprContent()),
                        DBIcon.TREE_FUNCTION,
                        flattenedExpr.getOperands().toArray(SQLQueryNodeModel[]::new)
                    );
            }
            return null;
        }

        @Nullable
        @Override
        public Object visitValueVariableExpr(@NotNull SQLQueryValueVariableExpression varExpr, @NotNull OutlineQueryNode node) {
            DBPImage icon = switch (varExpr.getKind()) {
                case BATCH_VARIABLE -> UIIcon.SQL_VARIABLE2;
                case CLIENT_PARAMETER -> UIIcon.SQL_PARAMETER;
                case CLIENT_VARIABLE -> UIIcon.SQL_PARAMETER;
            };
            this.makeNode(node, varExpr, prepareQueryPreview(varExpr.getRawName()), icon);
            return null;
        }
        
        @Nullable
        @Override
        public Object visitValueColumnRefExpr(
            @NotNull SQLQueryValueColumnReferenceExpression columnRefExpr,
            @NotNull OutlineQueryNode node
        ) {
            SQLQueryQualifiedName tableName = columnRefExpr.getTableName();
            String tableRefString = tableName == null ? "" : (tableName.toIdentifierString() + ".");

            SQLQuerySymbol columnName = columnRefExpr.getColumnNameIfTrivialExpression();
            String text;
            String extraText;
            DBPImage icon;
            if (columnName != null) {
                SQLQuerySymbolDefinition def = columnName.getDefinition();
                while (def instanceof SQLQuerySymbolEntry s && s != s.getDefinition()) {
                    def = s.getDefinition();
                }

                text = tableRefString + columnName.getName();
                extraText = this.obtainExprTypeNameString(columnRefExpr);
                icon = this.obtainExprTypeIcon(columnRefExpr);
            } else {
                text = SQLConstants.QUESTION;
                extraText = null;
                icon = DBIcon.TYPE_UNKNOWN;
            }
            this.makeNode(node, columnRefExpr, text, extraText, icon);
            return null;
        }

        @Nullable
        @Override
        public Object visitValueTupleRefExpr(@NotNull SQLQueryValueTupleReferenceExpression tupleRefExpr, @NotNull OutlineQueryNode node) {
            SQLQueryQualifiedName tableName = tupleRefExpr.getTableName();
            String extraText = this.obtainExprTypeNameString(tupleRefExpr);
            DBPImage icon = this.obtainExprTypeIcon(tupleRefExpr);

            this.makeNode(node, tupleRefExpr, tableName.toIdentifierString(), extraText, icon);
            return null;
        }

        @Nullable
        @Override
        public Object visitValueMemberReferenceExpr(@NotNull SQLQueryValueMemberExpression memberRefExpr, @NotNull OutlineQueryNode node) {
            String text = prepareQueryPreview(memberRefExpr.getExprContent());
            String extraText = this.obtainExprTypeNameString(memberRefExpr);
            DBPImage icon = this.obtainExprTypeIcon(memberRefExpr);
            
            this.makeNode(node, memberRefExpr, text, extraText, icon);
            return null;
        }

        @Nullable
        @Override
        public Object visitValueIndexingExpr(@NotNull SQLQueryValueIndexingExpression indexingExpr, @NotNull OutlineQueryNode node) {
            String text = prepareQueryPreview(indexingExpr.getExprContent());
            String extraText = this.obtainExprTypeNameString(indexingExpr);
            DBPImage icon = this.obtainExprTypeIcon(indexingExpr);
            
            this.makeNode(node, indexingExpr, text, extraText, icon);
            return null;
        }

        @Nullable
        @Override
        public Object visitValueTypeCastExpr(@NotNull SQLQueryValueTypeCastExpression typeCastExpr, @NotNull OutlineQueryNode node) {
            typeCastExpr.getValueExpr().apply(this, node);
            return null;
        }

        @Nullable
        @Override
        public Object visitValueConstantExpr(@NotNull SQLQueryValueConstantExpression constExpr, @NotNull OutlineQueryNode node) {
            String extraText = this.obtainExprTypeNameString(constExpr);
            DBPImage icon = this.obtainExprTypeIcon(constExpr);
            
            this.makeNode(node, constExpr, constExpr.getValueString(), extraText, icon);
            return null;
        }

        @Nullable
        private String obtainExprTypeNameString(@NotNull SQLQueryValueExpression expr) {
            SQLQueryExprType type = expr.getValueType();
            String typeName = type == null || type == SQLQueryExprType.UNKNOWN ? null : type.getDisplayName();
            return typeName == null ? null : (" : " + typeName);
        }

        @NotNull
        private DBPImage obtainExprTypeIcon(@NotNull SQLQueryValueExpression expr) {
            SQLQueryExprType type = expr.getValueType();
            return type == null
                ? DBIcon.TYPE_UNKNOWN
                : type.getTypedDbObject() != null
                    ? DBValueFormatting.getTypeImage(type.getTypedDbObject())
                    : wellKnownTypeIcons.getOrDefault(type, DBIcon.TYPE_UNKNOWN);
        }
        
        @Nullable
        @Override
        public Object visitSelectionResult(@NotNull SQLQuerySelectionResultModel selectionResult, @NotNull OutlineQueryNode node) {
            selectionResult.getSublists().forEach(s -> s.apply(this, node));
            return null;
        }

        @Nullable
        @Override
        public Object visitSelectionModel(@NotNull SQLQueryModel selection, @NotNull OutlineQueryNode node) {
            if (selection.getQueryModel() != null) {
                selection.getQueryModel().apply(this, node);
            }
            return null;
        }

        @Nullable
        @Override
        public Object visitRowsTableData(@NotNull SQLQueryRowsTableDataModel tableData, @NotNull OutlineQueryNode node) {
            DBSEntity table = tableData.getTable();
            DBPImage icon = DBValueFormatting.getObjectImage(table);
            String text = table == null
                ? (tableData.getName() == null ? SQLConstants.QUESTION : tableData.getName().toIdentifierString())
                : DBUtils.getObjectFullName(table, DBPEvaluationContext.DML);
            this.makeNode(node, tableData, text, icon);
            return null;
        }

        @Nullable
        @Override
        public Object visitRowsTableValue(@NotNull SQLQueryRowsTableValueModel tableValue, OutlineQueryNode node) {
            this.makeNode(
                node,
                tableValue,
                SQLConstants.KEYWORD_VALUES,
                UIIcon.ROW_COPY,
                tableValue.getValues().toArray(SQLQueryNodeModel[]::new)
            );
            return null;
        }


        @Nullable
        @Override
        public Object visitRowsCrossJoin(@NotNull SQLQueryRowsCrossJoinModel crossJoin, @NotNull OutlineQueryNode node) {
            List<SQLQueryNodeModel> children = this.flattenRowSetsCombination(crossJoin, x -> true, (x, l) -> { /* do nothing*/ });
            this.makeNode(node, crossJoin, SQLConstants.KEYWORD_CROSS_JOIN, DBIcon.TREE_TABLE_LINK, children.toArray(SQLQueryNodeModel[]::new));
            return null;
        }

        @Nullable
        @Override
        public Object visitRowsCorrelatedSource(@NotNull SQLQueryRowsCorrelatedSourceModel correlated, @NotNull OutlineQueryNode node) {
            List<SQLQuerySymbolEntry> columnNames = correlated.getCorrelationColumNames();
            String suffix = columnNames.isEmpty() ? "" : columnNames.stream()
                .map(SQLQuerySymbolEntry::getRawName)
                .collect(Collectors.joining(", ", " (", ")"));
            String prefix = correlated.getSource() instanceof SQLQueryRowsTableDataModel t
                    ? (t.getName() == null ? SQLConstants.QUESTION : t.getName().toIdentifierString()) + " " + SQLConstants.KEYWORD_AS + " "
                    : "";
            this.makeNode(
                node,
                correlated,
                prefix + correlated.getAlias().getRawName() + suffix,
                DBIcon.TREE_TABLE_ALIAS,
                correlated.getSource()
            );
            return null;
        }

        @Nullable
        @Override
        public Object visitRowsNaturalJoin(@NotNull SQLQueryRowsNaturalJoinModel naturalJoin, @NotNull OutlineQueryNode node) {
            // TODO bring join kind here
            switch (node.kind) {
                case NATURAL_JOIN_SUBROOT -> {
                    if (naturalJoin.getCondition() != null) {
                        // TODO add expression text to the ON node and remove its immediate and only child with the same text
                        this.makeNode(node, naturalJoin.getCondition(), SQLConstants.KEYWORD_ON + " ", DBIcon.TREE_UNIQUE_KEY, naturalJoin.getCondition());
                    } else if (naturalJoin.getColumsToJoin() != null && naturalJoin.getColumsToJoin().size() > 0) {
                        String suffix = naturalJoin.getColumsToJoin().stream()
                            .map(SQLQuerySymbolEntry::getRawName)
                            .collect(Collectors.joining(", ", "(", ")"));
                        this.makeNode(node, naturalJoin, SQLConstants.KEYWORD_USING + " " + suffix, DBIcon.TREE_UNIQUE_KEY);
                    }
                }
                default -> {
                    List<SQLQueryNodeModel> children = this.flattenRowSetsCombination(naturalJoin, x -> true, (x, l) -> l.add(x));
                    this.makeNode(
                        node, naturalJoin, OutlineQueryNodeKind.NATURAL_JOIN_SUBROOT,
                            SQLConstants.KEYWORD_NATURAL_JOIN + " ", DBIcon.TREE_TABLE_LINK, children.toArray(SQLQueryNodeModel[]::new)
                    );
                }
            }
            return null;
        }

        @NotNull
        private <T extends SQLQueryRowsSetOperationModel> List<SQLQueryNodeModel> flattenRowSetsCombination(
            @NotNull T op,
            @NotNull Predicate<T> predicate,
            @NotNull BiConsumer<T, List<SQLQueryNodeModel>> action
        ) {
            List<SQLQueryNodeModel> result = new LinkedList<>();
            this.flattenRowSetsCombinationImpl(op.getClass(), op, predicate, result, action);
            return result;
        }

        @SuppressWarnings("unchecked")
        private <T extends SQLQueryRowsSetOperationModel> void flattenRowSetsCombinationImpl(
            @NotNull Class<? extends SQLQueryRowsSetOperationModel> type,
            @NotNull T op,
            @NotNull Predicate<T> predicate,
            @NotNull List<SQLQueryNodeModel> result,
            @NotNull BiConsumer<T, List<SQLQueryNodeModel>> action
        ) {
            SQLQueryRowsSourceModel left = op.getLeft();
            if (type.isInstance(left) && predicate.test((T) left)) {
                this.flattenRowSetsCombinationImpl(type, (T) left, predicate, result, action);
            } else {
                result.add(left);
            }
            result.add(op.getRight());
            action.accept(op, result);
        }

        @Override
        public Object visitRowsProjectionInto(@NotNull SQLQuerySelectIntoModel selectIntoStatement, @NotNull OutlineQueryNode node) {
            return this.visitRowsProjectionImpl(selectIntoStatement, node, selectIntoStatement.getTargets());
        }

        @Nullable
        @Override
        public Object visitRowsProjection(@NotNull SQLQueryRowsProjectionModel projection, @NotNull OutlineQueryNode node) {
            return this.visitRowsProjectionImpl(projection, node, null);
        }

        private Object visitRowsProjectionImpl(
            @NotNull SQLQueryRowsProjectionModel projection,
            @NotNull OutlineQueryNode node,
            @Nullable SQLQuerySelectIntoModel.SQLQuerySelectIntoTargetsList targetsList
        ) {
            if (node.kind == OutlineQueryNodeKind.PROJECTION_SUBROOT || node instanceof OutlineScriptElementNode) {
                String suffix = projection.getResultDataContext().getColumnsList().stream()
                    .map(c -> c.symbol.getName())
                    .collect(Collectors.joining(", ", "(", ")"));
                this.makeNode(
                    node, projection.getResult(),
                    SQLConstants.KEYWORD_SELECT + " " + suffix,
                    DBIcon.TREE_COLUMNS, projection.getResult()
                );
                if (targetsList != null) {
                    this.makeNode(
                        node,
                        targetsList,
                        SQLConstants.KEYWORD_INTO,
                        DBIcon.TREE_FOLDER_TABLE,
                        targetsList
                    );
                }
                this.makeNode(
                    node,
                    projection.getFromSource(),
                    SQLConstants.KEYWORD_FROM,
                    DBIcon.TREE_FOLDER_TABLE,
                    projection.getFromSource()
                );

                if (projection.getWhereClause() != null) {
                    this.makeNode(node, projection.getWhereClause(), SQLConstants.KEYWORD_WHERE, UIIcon.FILTER, projection.getWhereClause());
                }
                if (projection.getGroupByClause() != null) {
                    this.makeNode(
                        node,
                        projection.getGroupByClause(),
                        SQLConstants.KEYWORD_GROUP_BY,
                        UIIcon.GROUP_BY_ATTR,
                        projection.getGroupByClause()
                    );
                }
                if (projection.getHavingClause() != null) {
                    this.makeNode(
                        node,
                        projection.getHavingClause(),
                        SQLConstants.KEYWORD_HAVING,
                        UIIcon.FILTER,
                        projection.getHavingClause()
                    );
                }
                if (projection.getOrderByClause() != null) {
                    this.makeNode(
                        node,
                        projection.getOrderByClause(),
                        SQLConstants.KEYWORD_ORDER_BY,
                        UIIcon.SORT,
                        projection.getOrderByClause()
                    );
                }
            } else {
                String text = prepareQueryPreview(projection.getSyntaxNode().getTextContent());
                this.makeNode(node, projection, OutlineQueryNodeKind.PROJECTION_SUBROOT, text, DBIcon.TREE_TABLE_LINK, projection);
            }
            return null;
        }

        public Object visitRowsProjectionIntoTargetsList(
            @NotNull SQLQuerySelectIntoModel.SQLQuerySelectIntoTargetsList targetsList,
            @NotNull OutlineQueryNode arg
        ) {
            targetsList.getTargetNodes().forEach(t -> t.apply(this, arg));
            return null;
        }

        @Nullable
        @Override
        public Object visitRowsSetCorrespondingOp(
            @NotNull SQLQueryRowsSetCorrespondingOperationModel correspondingOp,
            @NotNull OutlineQueryNode arg
        ) {
            List<SQLQueryNodeModel> children = this.flattenRowSetsCombination(
                correspondingOp,
                x -> x.getKind().equals(correspondingOp.getKind()), (x, l) -> { /*do nothing*/ });
            this.makeNode(
                arg,
                correspondingOp,
                correspondingOp.getKind().toString(),
                DBIcon.TREE_TABLE_LINK,
                children.toArray(SQLQueryNodeModel[]::new)
            );
            return null;
        }

        @Nullable
        @Override
        public Object visitDummyTableRowsSource(@NotNull DummyTableRowsSource dummyTable, @NotNull OutlineQueryNode arg) {
            this.makeNode(arg, dummyTable, SQLConstants.QUESTION, DBIcon.TYPE_UNKNOWN);
            return null;
        }

        @Nullable
        @Override
        public Object visitSelectCompleteTupleSpec(@NotNull SQLQuerySelectionResultCompleteTupleSpec completeTupleSpec, @NotNull OutlineQueryNode arg) {
            this.makeNode(arg, completeTupleSpec, SQLConstants.ASTERISK, UIIcon.ASTERISK);
            return null;
        }

        @Nullable
        @Override
        public Object visitSelectTupleSpec(@NotNull SQLQuerySelectionResultTupleSpec tupleSpec, @NotNull OutlineQueryNode arg) {
            this.makeNode(
                arg,
                tupleSpec,
                tupleSpec.getTableName().toIdentifierString() + SQLConstants.DOT + SQLConstants.ASTERISK,
                UIIcon.ASTERISK
            );
            return null;
        }

        @Nullable
        @Override
        public Object visitSelectColumnSpec(@NotNull SQLQuerySelectionResultColumnSpec columnSpec, @NotNull OutlineQueryNode arg) {
            SQLQuerySymbolEntry alias = columnSpec.getAlias();
            SQLQueryValueExpression mayBeExpr = columnSpec.getValueExpression();
            SQLQuerySymbol mayBeColumnName = mayBeExpr == null ? null : mayBeExpr.getColumnNameIfTrivialExpression();
            
            String text;
            if (alias != null) {
                text = alias.getRawName();
            } else {
                if (mayBeColumnName != null) {
                    text = mayBeColumnName.getName();
                } else {
                    text = getScriptElementNode(arg).scriptElement.getOriginalText()
                        .substring(columnSpec.getInterval().a, columnSpec.getInterval().b + 1);
                }
            }
            String extraText = mayBeExpr == null ? null : this.obtainExprTypeNameString(mayBeExpr);

            SQLQueryNodeModel[] subnodes = Stream.of(mayBeExpr).filter(Objects::nonNull).toArray(SQLQueryNodeModel[]::new);
            this.makeNode(arg, columnSpec, text, extraText, DBIcon.TREE_COLUMN, subnodes);
            return null;
        }

        @Nullable
        @Override
        public Object visitTableStatementDelete(@NotNull SQLQueryDeleteModel deleteStatement, @NotNull OutlineQueryNode node) {
            if (node.kind == OutlineQueryNodeKind.DELETE_SUBROOT) {
                if (deleteStatement.getCondition() != null) {
                    this.makeNode(
                        node,
                        deleteStatement.getCondition(),
                        SQLConstants.KEYWORD_WHERE,
                        UIIcon.FILTER,
                        deleteStatement.getCondition()
                    );
                }
            } else {
                String tableName = deleteStatement.getTableModel() == null || deleteStatement.getTableModel().getName() == null
                    ? SQLConstants.QUESTION
                    : deleteStatement.getTableModel().getName().toIdentifierString();
                String nodeName = SQLConstants.KEYWORD_DELETE + " " + SQLConstants.KEYWORD_FROM + " " + tableName;
                // TODO add separate FROM node when Multi-Table Deletes would be supported
                this.makeNode(
                    node,
                    deleteStatement,
                    OutlineQueryNodeKind.DELETE_SUBROOT,
                    nodeName,
                    UIIcon.ROW_DELETE,
                    deleteStatement.getAliasedTableModel() != null
                        ? deleteStatement.getAliasedTableModel()
                        : deleteStatement.getTableModel(),
                    deleteStatement
                );
            }
            return null;
        }

        @Nullable
        @Override
        public Object visitTableStatementInsert(@NotNull SQLQueryInsertModel insertStatement, @NotNull OutlineQueryNode node) {
            if (node.kind == OutlineQueryNodeKind.INSERT_SUBROOT) {
                if (insertStatement.getValuesRows() != null) {
                    insertStatement.getValuesRows().apply(this, node);
                }
            } else {
                String tableName = insertStatement.getTableModel() == null || insertStatement.getTableModel().getName() == null
                    ? SQLConstants.QUESTION
                    : insertStatement.getTableModel().getName().toIdentifierString();
                List<SQLQuerySymbolEntry> columnNames = insertStatement.getColumnNames();
                String columns = columnNames == null
                    ? ""
                    : columnNames.stream().map(SQLQuerySymbolEntry::getName).collect(Collectors.joining(", ", "(", ")"));
                String nodeName = SQLConstants.KEYWORD_INSERT + " " + SQLConstants.KEYWORD_INTO + " " + tableName + columns;
                this.makeNode(
                    node,
                    insertStatement,
                    OutlineQueryNodeKind.INSERT_SUBROOT,
                    nodeName,
                    UIIcon.ROW_ADD,
                    insertStatement.getTableModel(),
                    insertStatement
                );
            }
            return null;
        }

        @Nullable
        @Override
        public Object visitTableStatementUpdate(@NotNull SQLQueryUpdateModel updateStatement, @NotNull OutlineQueryNode node) {
            switch (node.kind) {
                case UPDATE_SUBROOT: {
                    if (updateStatement.getSetClauseList() != null) {
                        this.makeNode(
                            node,
                            updateStatement,
                            SQLConstants.KEYWORD_SET,
                            DBIcon.TREE_FOLDER_LINK,
                            updateStatement.getSetClauseList().toArray(SQLQueryNodeModel[]::new)
                        );
                    }

                    if (updateStatement.getSourceRows() != null) {
                        this.makeNode(
                            node,
                            updateStatement.getSourceRows(),
                            SQLConstants.KEYWORD_FROM,
                            DBIcon.TREE_FOLDER_TABLE,
                            updateStatement.getSourceRows()
                        );
                    }
                    if (updateStatement.getWhereClause() != null) {
                        this.makeNode(
                            node,
                            updateStatement.getWhereClause(),
                            SQLConstants.KEYWORD_WHERE,
                            UIIcon.FILTER,
                            updateStatement.getWhereClause()
                        );
                    }
                    if (updateStatement.getOrderByClause() != null) {
                        this.makeNode(
                            node,
                            updateStatement.getOrderByClause(),
                            SQLConstants.KEYWORD_ORDER_BY,
                            UIIcon.SORT,
                            updateStatement.getOrderByClause()
                        );
                    }
                }
                break;
                default: {
                    List<String> targetNames = new LinkedList<>();
                    if (updateStatement.getSetClauseList() != null) {
                        for (SQLQueryUpdateSetClauseModel setClause : updateStatement.getSetClauseList()) {
                            setClause.targets.stream()
                                .map(SQLQueryValueExpression::getColumnNameIfTrivialExpression)
                                .map(s -> s == null ? "..." : s.getName())
                                .forEach(targetNames::add);
                        }
                    }
                    String columns = targetNames.stream().collect(Collectors.joining(", ", "(", ")"));
                    String targetTableName = updateStatement.getTargetRows() instanceof SQLQueryRowsTableDataModel table && table.getName() != null
                        ? table.getName().toIdentifierString()
                        : "...";
                    String nodeName = SQLConstants.KEYWORD_UPDATE + " " + targetTableName + " " + SQLConstants.KEYWORD_SET + " " + columns;
                    this.makeNode(
                        node,
                        updateStatement,
                        OutlineQueryNodeKind.UPDATE_SUBROOT,
                        nodeName,
                        UIIcon.ROW_EDIT,
                        updateStatement.getTargetRows(),
                        updateStatement
                    );
                }
                break;
            }
            return null;
        }

        @Nullable
        @Override
        public Object visitTableStatementUpdateSetClause(
            @NotNull SQLQueryUpdateSetClauseModel setClause,
            @NotNull OutlineQueryNode node
        ) {
            List<SQLQueryNodeModel> nodes = new ArrayList<>(setClause.targets.size() + setClause.sources.size());
            nodes.addAll(setClause.targets);
            nodes.addAll(setClause.sources);
            this.makeNode(node, setClause, setClause.contents, DBIcon.TYPE_ARRAY, nodes.toArray(SQLQueryNodeModel[]::new));
            return null;
        }

        @Nullable
        @Override
        public Object visitTableStatementDrop(@NotNull SQLQueryTableDropModel dropStatement, OutlineQueryNode node) {
            String tableNames =  dropStatement.getTables() == null || dropStatement.getTables().isEmpty() ? SQLConstants.QUESTION
                : dropStatement.getTables().stream()
                    .filter(Objects::nonNull)
                    .map(SQLQueryRowsTableDataModel::getName)
                    .filter(Objects::nonNull)
                    .map(SQLQueryQualifiedName::toIdentifierString)
                    .collect(Collectors.joining(", "));
            String nodeName =  "DROP " + (dropStatement.isView() ? "VIEW" : "TABLE")
                + (dropStatement.getIfExists() ? " IF EXISTS " : " ") + tableNames;
            this.makeNode(
                node,
                dropStatement,
                nodeName,
                UIIcon.REMOVE,
                dropStatement.getTables().toArray(SQLQueryRowsTableDataModel[]::new)
            );
            return null;
        }

        @Nullable
        @Override
        public Object visitObjectStatementDrop(@NotNull SQLQueryObjectDropModel dropStatement, OutlineQueryNode node) {
            String procName = dropStatement.getObject() == null ? SQLConstants.QUESTION : dropStatement.getObject().getName().toIdentifierString();
            String nodeName =  "DROP " + dropStatement.getObject().getObjectType().getTypeName().toUpperCase()
               + " " + (dropStatement.getIfExists() ? "IF EXISTS " : " ") + procName;
            this.makeNode(
                node,
                dropStatement,
                nodeName,
                UIIcon.REMOVE,
                dropStatement.getObject()
            );
            return null;
        }

        @Nullable
        @Override
        public Object visitObjectReference(@NotNull SQLQueryObjectDataModel objectReference, OutlineQueryNode node) {
            this.makeNode(
                node,
                objectReference,
                objectReference.getName().toIdentifierString(),
                objectReference.getObjectType().getImage()
            );
            return null;
        }

        @Nullable
        @Override
        public Object visitCreateTable(@NotNull SQLQueryTableCreateModel createTable, OutlineQueryNode node) {
            SQLQueryQualifiedName tableName = createTable.getTableName();
            String nodeName = "CREATE TABLE " + (tableName == null ? SQLConstants.QUESTION : tableName.toIdentifierString());
            this.makeNode(
                node, createTable, nodeName, UIIcon.ACTION_OBJECT_ADD,
                Stream.concat(createTable.getColumns().stream(), createTable.getConstraints().stream()).toArray(SQLQueryNodeModel[]::new)
            );
            return null;
        }

        @Nullable
        @Override
        public Object visitColumnConstraintSpec(@NotNull SQLQueryColumnConstraintSpec columnConstraintSpec, OutlineQueryNode node) {
            String nodeText = prepareQueryPreview(columnConstraintSpec.getSyntaxNode().getTextContent());
            this.makeNode(node, columnConstraintSpec, nodeText, DBIcon.TREE_CONSTRAINT);
            return null;
        }

        @Nullable
        @Override
        public Object visitColumnSpec(@NotNull SQLQueryColumnSpec columnSpec, OutlineQueryNode node) {
            String nodeText = columnSpec.getColumnName() == null ? SQLConstants.QUESTION : columnSpec.getColumnName().getName();
            this.makeNode(
                node, columnSpec, nodeText, " " + CommonUtils.notNull(columnSpec.getTypeName(), ""), DBIcon.TREE_COLUMN,
                Stream.concat(
                    Stream.of(columnSpec.getDefaultValueExpression()),
                    columnSpec.getConstraints().stream()
                ).toArray(SQLQueryNodeModel[]::new)
            );
            return null;
        }

        @Nullable
        @Override
        public Object visitTableConstraintSpec(@NotNull SQLQueryTableConstraintSpec tableConstraintSpec, OutlineQueryNode node) {
            String nodeText = prepareQueryPreview(tableConstraintSpec.getSyntaxNode().getTextContent());
            this.makeNode(node, tableConstraintSpec, nodeText, DBIcon.TREE_CONSTRAINT);
            return null;
        }

        @Nullable
        @Override
        public Object visitAlterTable(@NotNull SQLQueryTableAlterModel alterTable, OutlineQueryNode node) {
            String nodeText = prepareQueryPreview(alterTable.getSyntaxNode().getTextContent());
            this.makeNode(
                node, alterTable, nodeText, DBIcon.TREE_FOLDER_CONSTRAINT,
                Stream.concat(
                    Stream.of(alterTable.getTargetTable()),
                    alterTable.getAlterActions().stream()
                ).toArray(SQLQueryNodeModel[]::new)
            );
            return null;
        }

        @Nullable
        @Override
        public Object visitAlterTableAction(@NotNull SQLQueryTableAlterActionSpec actionSpec, OutlineQueryNode node) {
            String nodeText = prepareQueryPreview(actionSpec.getSyntaxNode().getTextContent());
            this.makeNode(
                node, actionSpec, nodeText, DBIcon.TREE_CONSTRAINT,
                actionSpec.getColumnSpec(), actionSpec.getTableConstraintSpec()
            );
            return null;
        }

        private void makeNode(
            @NotNull OutlineQueryNode parent,
            @NotNull SQLQueryNodeModel model,
            @NotNull String text,
            @NotNull DBPImage icon,
            @NotNull SQLQueryNodeModel... childModels
        ) {
            makeNode(parent, model, text, null, icon, childModels);
        }
        
        private void makeNode(
            @NotNull OutlineQueryNode parent,
            @NotNull SQLQueryNodeModel model,
            @NotNull OutlineQueryNodeKind kind,
            @NotNull String text,
            @NotNull DBPImage icon,
            @NotNull SQLQueryNodeModel... childModels
        ) {
            makeNode(parent, model, kind, text, null, icon, childModels);
        }
            
        private void makeNode(
            @NotNull OutlineQueryNode parent,
            @NotNull SQLQueryNodeModel model,
            @NotNull String text,
            @Nullable String extraText,
            @NotNull DBPImage icon,
            @NotNull SQLQueryNodeModel... childModels
        ) {
            makeNode(parent, model, OutlineQueryNodeKind.DEFAULT, text, extraText, icon, childModels);
        }
        
        private void makeNode(
            @NotNull OutlineQueryNode parent,
            @NotNull SQLQueryNodeModel model,
            @NotNull OutlineQueryNodeKind kind,
            @NotNull String text,
            @Nullable String extraText,
            @NotNull DBPImage icon,
            @NotNull SQLQueryNodeModel... childModels
        ) {
            parent.children.add(new OutlineQueryNode(parent, model, kind, text, extraText, icon, childModels));
        }
    }
    
    private class OutlineRefreshJob {
        @NotNull
        private final AtomicBoolean updateElements = new AtomicBoolean(false);

        @NotNull
        private final AbstractUIJob job = new AbstractUIJob("SQL editor outline refresh") {
            @NotNull
            @Override
            protected IStatus runInUIThread(@NotNull DBRProgressMonitor monitor) {
                boolean doUpdateElements = updateElements.getAndSet(false);
                if (treeViewer != null && !treeViewer.getTree().isDisposed()) {
                    if (doUpdateElements) {
                        scriptNode.updateElements();
                    }
                    scriptNode.updateChildren();
                    treeViewer.refresh();
                }
                return Status.OK_STATUS;
            }
        };
        
        public void schedule(boolean updateElements) {
            this.updateElements.set(updateElements);
            switch (this.job.getState()) {
                case Job.WAITING, Job.SLEEPING -> this.job.cancel();
            }
            this.job.schedule(500);
        }
    }
}
