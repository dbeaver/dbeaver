/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSView;
import org.jkiss.dbeaver.ui.AbstractUIJob;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.semantics.*;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLDocumentSyntaxContext.SQLDocumentSyntaxContextListener;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDummyDataSourceContext.DummyTableRowsSource;
import org.jkiss.dbeaver.ui.editors.sql.semantics.model.*;
import org.jkiss.dbeaver.ui.editors.sql.semantics.model.SQLQuerySelectionResultModel.*;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class SQLEditorOutlinePage extends ContentOutlinePage implements IContentOutlinePage {
	
	private enum SelectionSyncOperation {
		NONE,
		FROM_EDITOR,
		TO_EDITOR
	}
		
	private final SQLEditorBase editor;
	
	private TreeViewer treeViewer;
	
	private OutlineScriptNode scriptNode;
	private List<OutlineNode> rootNodes;
	
	private SelectionSyncOperation currentSelectionSyncOp = SelectionSyncOperation.NONE;
	
	public SQLEditorOutlinePage(SQLEditorBase editor) {
		this.editor = editor;
		this.rootNodes = List.of(this.scriptNode = new OutlineScriptNode());
	}
	
	private SQLOutlineNodeBuilder currentNodeBuilder = new SQLOutlineNodeFullBuilder();
	
	private SQLOutlineNodeBuilder getNodeBuilder() {
		return this.currentNodeBuilder;
	}
	
	@Override
	protected int getTreeStyle() {
		return super.getTreeStyle() | SWT.FULL_SELECTION | SWT.VIRTUAL;
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		
		this.treeViewer = super.getTreeViewer();
		// this.treeViewer.setContentProvider(new SQLOutlineContentProvider());
		
		this.treeViewer.setContentProvider(new ILazyTreeContentProvider() {
			@Override
			public void updateElement(Object parent, int index) {
				if (parent == editor.getEditorInput()) {
					this.updateChildNode(parent, index, rootNodes.get(index));
				} else if (parent instanceof OutlineNode node) {
					this.updateChildNode(parent, index, node.getChild(index));										
				} else {
					throw new UnsupportedOperationException();
				}
			}
			
			private void updateChildNode(Object parent, int index, OutlineNode childNode) {
				treeViewer.replace(parent, index, childNode);
				treeViewer.setChildCount(childNode, childNode.getChildrenCount());
			}
	
			@Override
			public void updateChildCount(Object element, int currentChildCount) {				
				if (element == editor.getEditorInput()) {
					treeViewer.setChildCount(element, rootNodes.size());
				} else if (element instanceof OutlineNode node) {
					treeViewer.setChildCount(element, node.getChildrenCount());
				} else {
					throw new UnsupportedOperationException();
				}
			}
	
			@Override
			public Object getParent(Object element) {
				return element instanceof OutlineNode node ? node.getParent() : null;
			}
		});

		this.treeViewer.setLabelProvider(new SQLOutlineLabelProvider());
		this.treeViewer.setInput(editor.getEditorInput());
		this.treeViewer.setAutoExpandLevel(3);
		
		this.editor.getTextViewer().getTextWidget().addCaretListener(new CaretListener() {
			
			@Override
			public void caretMoved(CaretEvent event) {
				if (currentSelectionSyncOp == SelectionSyncOperation.NONE) {
					int offset = event.caretOffset;
					OutlineNode node = scriptNode, nextNode = scriptNode;
					while (nextNode != null) {
						node = nextNode;
						nextNode = null;
						for (int i = 0; i < node.getChildrenCount(); i++) {
							OutlineNode child = node.getChild(i);
							IRegion range = child.getTextRange();
							if (range.getOffset() <= offset) { // && range.getOffset() + range.getLength() > offset) {
								nextNode = child;
							} else {
								break;
							}
						}
					}
					
					if (node != null) {
						currentSelectionSyncOp = SelectionSyncOperation.FROM_EDITOR;
						treeViewer.setSelection(new StructuredSelection(node), true);
						currentSelectionSyncOp = SelectionSyncOperation.NONE;
					}
				}
			}
		});
	}
	
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
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
	
	@Override
	public void dispose() {
		this.scriptNode.dispose();
		super.dispose();
	}
	
	private abstract class OutlineNode {
		public static final OutlineNode[] EMPTY_NODES_ARRAY = new OutlineNode[0];
		
		private OutlineNode parentNode;
		
		public OutlineNode(OutlineNode parentNode) {
			this.parentNode = parentNode;
		}

		public abstract IRegion getTextRange();

		public final OutlineNode getParent() {
			return this.parentNode;
		}

		public abstract String getText();

		public Image getImage() {
			DBIcon icon = this.getIcon();
			return icon == null ? null : DBeaverIcons.getImage(icon, false);
		}
		
		protected abstract DBIcon getIcon();

		public abstract int getChildrenCount();
		
		public abstract OutlineNode getChild(int index);
	}
	
	private class OutlineInfoNode extends OutlineNode {
		private final String title;
		private final DBIcon icon;
		
		public OutlineInfoNode(OutlineNode parentNode, String title, DBIcon icon) {
			super(parentNode);
			this.title = title;
			this.icon = icon;
		}

		@Override
		public String getText() {
			return this.title;
		}

		@Override
		public DBIcon getIcon() {
			return this.icon;
		}

		@Override
		public OutlineNode getChild(int index) {
			return null;
		}
		
		@Override
		public int getChildrenCount() {
			return 0;
		}
		
		@Override
		public IRegion getTextRange() {
			return null;
		}
	}
	
	private class OutlineScriptNode extends OutlineNode {
		final SQLDocumentSyntaxContext documentContext;

		OutlineNode noElementsNode = new OutlineInfoNode(this, "No elements detected", DBIcon.SMALL_INFO);
		
		Map<SQLDocumentScriptItemSyntaxContext, OutlineNode> elements = new HashMap<>();
		List<OutlineNode> children = Collections.emptyList();
		
		final SQLDocumentSyntaxContextListener syntaxContextListener = new SQLDocumentSyntaxContextListener() {
			@Override
			public void onScriptItemInvalidated(SQLDocumentScriptItemSyntaxContext item) {
				UIUtils.syncExec(() -> {
					//elements.remove(item);
					updateElements();
					updateChildren();
					scheduleRefresh();
				});
			}
			@Override
			public void onScriptItemIntroduced(SQLDocumentScriptItemSyntaxContext item) {
				UIUtils.syncExec(() -> {
					updateElements();
					updateChildren();
					scheduleRefresh();
				});
			}
			@Override
			public void onAllScriptItemsInvalidated() {
				UIUtils.syncExec(() -> {
					//elements.clear();
					updateElements();
					updateChildren();
					scheduleRefresh();
				});
			}
		};
		
		AbstractUIJob refreshJob = new AbstractUIJob("SQL editor outline refresh") {
			@Override
			protected IStatus runInUIThread(DBRProgressMonitor monitor) {
				treeViewer.refresh();
	            return Status.OK_STATUS;
			}
		};
		
		public OutlineScriptNode() {
			super(null);
			this.documentContext = editor.getSyntaxContext();

			this.updateElements();
			this.updateChildren();
			
			this.documentContext.addListener(syntaxContextListener);
		}

		@Override
		public IRegion getTextRange() {
			return new Region(0, 0);
		}
		
		private void scheduleRefresh() {
			switch (this.refreshJob.getState()) {
			case Job.WAITING:
			case Job.SLEEPING:
				this.refreshJob.cancel();
			}
			this.refreshJob.schedule(500);
		}
		
		private void updateElements() {
			this.elements = documentContext.getScriptItems().stream()
            	.collect(Collectors.toMap(e -> e.item, e -> new OutlineScriptElementNode(this, e.offset, e.item), (a, b) -> a, () -> new LinkedHashMap<>()));
		}
		
		private void updateChildren() {
			if (this.elements.isEmpty()) {
				this.children = List.of(noElementsNode);
			} else {
				this.children = List.copyOf(this.elements.values());
			}
		}
		
		@Override
		public int getChildrenCount() {
			return this.children.size();
		}
		
		@Override
		public OutlineNode getChild(int index) {
			return this.children.get(index);
		}

		@Override
		public String getText() {
			return editor.getTitle();
		}
		
		@Override
		public Image getImage() {
			Image image = editor.getTitleImage();
			return new Image(image.getDevice(), image, SWT.IMAGE_COPY);
		}
		
		@Override
		protected DBIcon getIcon() {
			return DBIcon.TREE_SCRIPT;
		}

		public void dispose() {
			this.documentContext.removeListener(syntaxContextListener);			
		}
	}

	private class OutlineScriptElementNode extends OutlineQueryNode {
		private final int offset;
		private final SQLDocumentScriptItemSyntaxContext scriptElement;
		
		public OutlineScriptElementNode(OutlineScriptNode parent, int offset, SQLDocumentScriptItemSyntaxContext scriptElement) {
			super(parent, scriptElement.getQueryModel(), "SQL Query", UIIcon.SQL_EXECUTE, scriptElement.getQueryModel());
			this.offset = offset;
			this.scriptElement = scriptElement;
		}
		
		public int getOffset() {
			return this.offset;
		}
		
		@Override
		public IRegion getTextRange() {
			return new Region(this.offset, 0);
		}
	}
	
	private class OutlineQueryNode extends OutlineNode {
		private final SQLQueryNodeModel model;
		private final String text;
		private final DBIcon icon;
		private final SQLQueryNodeModel[] childModels;
		private final IRegion textRange;
		private List<OutlineNode> children;
		
		public OutlineQueryNode(OutlineNode parentNode, SQLQueryNodeModel model, String text, DBIcon icon, SQLQueryNodeModel ... childModels) {
			super(parentNode);
			this.model = model;
			this.text = text;
			this.icon = icon;
			this.childModels = childModels;

			int offset = parentNode instanceof OutlineScriptElementNode element ? element.getOffset() + this.model.getInterval().a
					: parentNode instanceof OutlineQueryNode queryNode ? queryNode.getTextRange().getOffset() - queryNode.model.getInterval().a + this.model.getInterval().a
					: parentNode.getTextRange().getOffset();
			
			this.textRange = new Region(offset, this.model.getInterval().length());
			
			this.children = null;
		}
		
		@Override
		public IRegion getTextRange() {
			return this.textRange;
		}
		
		private List<OutlineNode> obtainChildren() { 
			if (this.children == null) {
				if (this.childModels.length == 0) {
					this.children = Collections.emptyList();
				} else {
					this.children = new ArrayList<>(childModels.length);
					for (SQLQueryNodeModel childModel: childModels) {
						if (childModel != null) {
							childModel.apply(getNodeBuilder(), this);
						}
					}
				}
			}
			return this.children;
		}

		@Override
		public String getText() {
			return this.text;
		}

		@Override
		protected DBIcon getIcon() {
			return this.icon;
		}

		@Override
		public int getChildrenCount() {
			return this.obtainChildren().size();
		}
		
		@Override
		public OutlineNode getChild(int index) {
			return this.obtainChildren().get(index);
		}
	}
	
	private static interface SQLOutlineNodeBuilder extends SQLQueryNodeModelVisitor<OutlineQueryNode, Object> {
	}
	
	private class SQLOutlineNodeFullBuilder implements SQLOutlineNodeBuilder {

		@Override
		public Object visitValueSubqueryExpr(SQLQueryValueSubqueryExpression subqueryExpr, OutlineQueryNode arg) {
			this.makeNode(arg, subqueryExpr, "Scalar subquery", UIIcon.FILTER_VALUE, subqueryExpr.getSource());
			return null;
		}

		@Override
		public Object visitValueFlatExpr(SQLQueryValueFlattenedExpression flattenedExpr, OutlineQueryNode arg) {
			flattenedExpr.getOperands().forEach(e -> e.apply(this, arg));
			return null;
		}

		@Override
		public Object visitValueColumnRefExpr(SQLQueryValueColumnReferenceExpression columnRefExpr, OutlineQueryNode arg) {
			this.makeNode(arg, columnRefExpr, columnRefExpr.getColumnNameIfTrivialExpression().getName(), DBIcon.TREE_COLUMN);
			return null;
		}

		@Override
		public Object visitSelectionResult(SQLQuerySelectionResultModel selectionResult, OutlineQueryNode arg) {
			selectionResult.getSublists().forEach(s -> s.apply(this, arg));
			return null;
		}

		@Override
		public Object visitSelectionModel(SQLQuerySelectionModel selection, OutlineQueryNode arg) {
			selection.getResultSource().apply(this, arg);
			return null;
		}

		@Override
		public Object visitRowsTableData(SQLQueryRowsTableDataModel tableData, OutlineQueryNode arg) {
			DBSEntity t = tableData.getTable();
			boolean isTable = t instanceof DBSTable;
			boolean isView = t instanceof DBSView;
			DBIcon icon = isTable ? DBIcon.TREE_TABLE : isView ? DBIcon.TREE_VIEW : DBIcon.TYPE_UNKNOWN;	
			this.makeNode(arg, tableData, tableData.getName().toIdentifierString(), icon);
			return null;
		}

		@Override
		public Object visitRowsTableValue(SQLQueryRowsTableValueModel tableValue, OutlineQueryNode arg) {
			this.makeNode(arg, tableValue, "Table value", DBIcon.TYPE_UNKNOWN); // TODO
			return null;
		}

		@Override
		public Object visitRowsSelectionFilter(SQLQueryRowsSelectionFilterModel selectionFilter, OutlineQueryNode arg) {
			selectionFilter.getFromSource().apply(this, arg);

			if (selectionFilter.getWhereClause() != null) {
				this.makeNode(arg, selectionFilter.getWhereClause(), "WHERE", UIIcon.FILTER, selectionFilter.getWhereClause());
			}
			if (selectionFilter.getHavingClause() != null) {
				this.makeNode(arg, selectionFilter.getHavingClause(), "HAVING", UIIcon.FILTER, selectionFilter.getHavingClause());
			}
			if (selectionFilter.getGroupByClause() != null) {
				this.makeNode(arg, selectionFilter.getGroupByClause(), "GROUP BY", UIIcon.GROUP_BY_ATTR, selectionFilter.getGroupByClause());
			}
			if (selectionFilter.getOrderByClause() != null) {
				this.makeNode(arg, selectionFilter.getOrderByClause(), "ORDER BY", UIIcon.SORT, selectionFilter.getOrderByClause());
			}
			return null;
		}

		@Override
		public Object visitRowsCrossJoin(SQLQueryRowsCrossJoinModel crossJoin, OutlineQueryNode arg) {
			//this.makeNode(arg, crossJoin, "CROSS JOIN", DBIcon.TREE_TABLE_LINK, crossJoin.getLeft(), crossJoin.getRight());
			
			List<SQLQueryNodeModel> children = this.flattenRowSetsCombination(crossJoin, x -> true, (x, l) -> { });
			this.makeNode(arg, crossJoin, "CROSS JOIN", DBIcon.TREE_TABLE_LINK, children.toArray(SQLQueryNodeModel[]::new));
			return null;
		}

		@Override
		public Object visitRowsCorrelatedSource(SQLQueryRowsCorrelatedSourceModel correlated, OutlineQueryNode arg) {
			List<SQLQuerySymbolEntry> columnNames = correlated.getCorrelationColumNames();
			String suffix = columnNames.isEmpty() ? "" : columnNames.stream().map(s -> s.getRawName()).collect(Collectors.joining(", ", " (", ")"));
			this.makeNode(arg, correlated, correlated.getAlias().getRawName() + suffix, DBIcon.TREE_TABLE_ALIAS, correlated.getSource());
			return null;
		}

		@Override
		public Object visitRowsNaturalJoin(SQLQueryRowsNaturalJoinModel naturalJoin, OutlineQueryNode arg) {
			// TODO bring join kind here
			if (arg.model instanceof SQLQueryRowsNaturalJoinModel) {
				if (naturalJoin.getCondition() != null) {
					this.makeNode(arg, naturalJoin.getCondition(), "ON ", DBIcon.TREE_FOREIGN_KEY, naturalJoin.getCondition());
				} else {
					String suffix = naturalJoin.getColumsToJoin().stream().map(s -> s.getRawName()).collect(Collectors.joining(", ", "(", ")"));
					this.makeNode(arg, naturalJoin, "USING " + suffix, DBIcon.TREE_FOREIGN_KEY);
				}
			} else {
				List<SQLQueryNodeModel> children = this.flattenRowSetsCombination(naturalJoin, x -> true, (x, l) -> l.add(x));
				this.makeNode(arg, naturalJoin, "NATURAL JOIN ", DBIcon.TREE_TABLE_LINK, children.toArray(SQLQueryNodeModel[]::new));
			}
			return null;
		}
		
		private <T extends SQLQueryRowsSetOperationModel> List<SQLQueryNodeModel> flattenRowSetsCombination(T op, Predicate<T> predicate, BiConsumer<T, List<SQLQueryNodeModel>> action) {
			List<SQLQueryNodeModel> result = new LinkedList<>();
			this.flattenRowSetsCombinationImpl(op.getClass(), op, predicate, result, action);
			return result;
		}
		
		@SuppressWarnings("unchecked")
		private <T extends SQLQueryRowsSetOperationModel> void flattenRowSetsCombinationImpl(Class<? extends SQLQueryRowsSetOperationModel> type, T op, Predicate<T> predicate, List<SQLQueryNodeModel> result, BiConsumer<T, List<SQLQueryNodeModel>> action) {
			SQLQueryRowsSourceModel left = op.getLeft();
			if (type.isInstance(left) && predicate.test((T)left)) {
				this.flattenRowSetsCombinationImpl(type, (T)left, predicate, result, action);
			} else {
				result.add(left);
			}
			result.add(op.getRight());
			action.accept(op, result);
		}

		@Override
		public Object visitRowsProjection(SQLQueryRowsProjectionModel projection, OutlineQueryNode arg) {
			String suffix = projection.getDataContext().getColumnsList().stream().map(c -> c.getName()).collect(Collectors.joining(", ", "(", ")"));
			this.makeNode(arg, projection.getResult(), "SELECT " + suffix, DBIcon.TREE_COLUMNS, projection.getResult());
			this.makeNode(arg, projection.getFromSource(), "FROM", DBIcon.TREE_FOLDER_TABLE, projection.getFromSource());
			return null;
		}

		@Override
		public Object visitRowsSetCorrespondingOp(SQLQueryRowsSetCorrespondingOperationModel correspondingOp, OutlineQueryNode arg) {
//			this.makeNode(
//				arg, correspondingOp, correspondingOp.getKind().toString(), DBIcon.TREE_TABLE_LINK,
//				correspondingOp.getLeft(),
//				correspondingOp.getRight()
//			);

			List<SQLQueryNodeModel> children = this.flattenRowSetsCombination(correspondingOp, x -> x.getKind().equals(correspondingOp.getKind()), (x, l) -> { });
			this.makeNode(arg, correspondingOp, correspondingOp.getKind().toString(), DBIcon.TREE_TABLE_LINK, children.toArray(SQLQueryNodeModel[]::new));
			return null;
		}

		@Override
		public Object visitDummyTableRowsSource(DummyTableRowsSource dummyTable, OutlineQueryNode arg) {
			this.makeNode(arg, dummyTable, "?", DBIcon.TYPE_UNKNOWN);
			return null;
		}

		@Override
		public Object visitSelectCompleteTupleSpec(CompleteTupleSpec completeTupleSpec, OutlineQueryNode arg) {
			this.makeNode(arg, completeTupleSpec, "*", UIIcon.ASTERISK);
			return null;
		}

		@Override
		public Object visitSelectTupleSpec(TupleSpec tupleSpec, OutlineQueryNode arg) {
			this.makeNode(arg, tupleSpec, tupleSpec.getTableName().toIdentifierString(), UIIcon.ASTERISK);
			return null;
		}

		@Override
		public Object visitSelectColumnSpec(ColumnSpec columnSpec, OutlineQueryNode arg) {
			SQLQuerySymbolEntry alias = columnSpec.getAlias();
			SQLQuerySymbol mayBeColumnName = columnSpec.getValueExpression().getColumnNameIfTrivialExpression();
			this.makeNode(arg, columnSpec, alias == null ? (mayBeColumnName == null ? "?" : mayBeColumnName.getName()) : alias.getRawName(), DBIcon.TREE_COLUMN, columnSpec.getValueExpression());
			return null;
		}

		private void makeNode(OutlineQueryNode parent, SQLQueryNodeModel model, String text, DBIcon icon, SQLQueryNodeModel ... childModels) {
			parent.children.add(new OutlineQueryNode(parent, model, text, icon, childModels));
		}
	}
	
	private static class SQLOutlineLabelProvider implements ILabelProvider, IFontProvider {
		
		@Override
		public void addListener(ILabelProviderListener listener) {
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
		}

		@Override
		public Font getFont(Object element) {
			return null;
		}

		@Override
		public Image getImage(Object element) {
			return element instanceof OutlineNode node ? node.getImage() :  DBeaverIcons.getImage(DBIcon.TYPE_UNKNOWN, false);
		}

		@Override
		public String getText(Object element) {
			return element instanceof OutlineNode node ? node.getText() : element.toString(); 
		}
		
		@Override
		public void dispose() {
		}
	}	
}
