package org.jkiss.dbeaver.ui.editors.sql.syntax.extended;


import org.antlr.v4.runtime.misc.Interval;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.rules.IRule;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.lsm.LSMAnalyzer;
import org.jkiss.dbeaver.model.lsm.sql.dialect.LSMDialectRegistry;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.SQLSearchUtils;
import org.jkiss.dbeaver.model.sql.analyzer.TableReferencesRules;
import org.jkiss.dbeaver.model.sql.parser.SQLParserContext;
import org.jkiss.dbeaver.model.sql.parser.SQLScriptParser;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.stm.STMKnownRuleNames;
import org.jkiss.dbeaver.model.stm.STMSkippingErrorListener;
import org.jkiss.dbeaver.model.stm.STMSource;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.stm.STMTreeRuleNode;
import org.jkiss.dbeaver.model.stm.STMUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLRuleScanner;

import java.util.*;
import java.util.stream.Collectors;


public class SQLBackgroundParsingJob {

    private static final Timer schedulingTimer = new Timer("SQLBackgroundParsingJob.schedulingTimer.thread", true);
    private static final long schedulingTimeoutMilliseconds = 500;

    private final Object syncRoot = new Object();
    private final SQLEditorBase editor;
    private SQLDocumentSyntaxContext context = null;
    private IDocument document = null;
    private volatile TimerTask task = null; 
    private volatile boolean isRunning = false;
    
    private DocumentLifecycleListener documentListener = new DocumentLifecycleListener();
    
    public SQLBackgroundParsingJob(SQLEditorBase editor) {
        this.editor = editor;
    }

    public SQLDocumentSyntaxContext getCurrentContext() {
        return context;
    }
    
    public void setup() {
        synchronized (syncRoot) {
            if (this.editor.getTextViewer() != null) {
                this.editor.getTextViewer().addTextInputListener(documentListener);
                if (this.document == null) {
                    IDocument document = this.editor.getTextViewer().getDocument();
                    if (document != null) {
                        this.document = document;
                        this.document.addDocumentListener(documentListener);
                        this.schedule(null);
                    }
                }
            }
        }
    }
    
    public void dispose() {
        synchronized (syncRoot) {
            this.cancel();
            this.editor.getTextViewer().removeTextInputListener(documentListener);
            if (this.document != null) {
                this.document.removeDocumentListener(documentListener);
            }   
        }
    }
    
    private SQLDocumentSyntaxContext getContext() {
        if (this.context == null) {
            this.context = new SQLDocumentSyntaxContext(this.editor.getDocument());
        }
        return this.context;
    }

    public IRule[] prepareRules(SQLRuleScanner sqlRuleScanner) {
        this.getContext();
        return new IRule[] {
            new SQLPassiveSyntaxRule(this, sqlRuleScanner, SQLTokenType.T_TABLE),
            new SQLPassiveSyntaxRule(this, sqlRuleScanner, SQLTokenType.T_COLUMN),
            new SQLPassiveSyntaxRule(this, sqlRuleScanner, SQLTokenType.T_COLUMN_ALIAS),
            new SQLPassiveSyntaxRule(this, sqlRuleScanner, SQLTokenType.T_SEMANTIC_ERROR),
        };
    }

    private void schedule(DocumentEvent event) {
        synchronized (syncRoot) {
            if (editor.getRuleManager() == null) {
                return;
            }
            
            task = new TimerTask() {
                @Override
                public void run() {
                    try {
                        SQLBackgroundParsingJob.this.doWork();
                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }
                }
            };
            if (event != null) {
                // TODO drop only on lines-set change and apply in line offset on local insert or remove
                this.getContext().dropLineOfOffset(event.getOffset()); 
            }
            schedulingTimer.schedule(task, schedulingTimeoutMilliseconds * (this.isRunning ? 2 : 1));
        }
    }
    
    private void cancel() {
        synchronized (syncRoot) {
            if (task != null) {
                task.cancel();
                task = null;
            }
        }
    }

    private void setDocument(IDocument newDocument) {
        synchronized (syncRoot) {
            if (this.document != null) {
                this.cancel();
                this.document = null;
                this.context = null;
            }
            
            if (newDocument != null && SQLEditorUtils.isSQLSyntaxParserApplied(editor.getEditorInput())) {
                    this.context = new SQLDocumentSyntaxContext(newDocument);
                    this.document = newDocument;
                    this.schedule(null);
                    return;
            }
        }
    }
     
    private final static Set<String> tableIdentifierNodeNames = Set.of(STMKnownRuleNames.tableName, STMKnownRuleNames.correlationName);
    private final static Set<String> columnReferenceNodeNames = Set.of(STMKnownRuleNames.columnReference);
    private final static Set<String> columnIdentifierNodeNames = Set.of(STMKnownRuleNames.columnName, STMKnownRuleNames.tableName, STMKnownRuleNames.correlationName);
    
    private void doWork() throws BadLocationException {
        synchronized (syncRoot) {
            this.task = null;
            this.isRunning = true;
        }
        
        SQLDocumentSyntaxContext context = new SQLDocumentSyntaxContext(document);
        
        SQLParserContext parserContext = new SQLParserContext(editor.getDataSource(), editor.getSyntaxManager(), editor.getRuleManager(), document);
        List<SQLScriptElement> elements = SQLScriptParser.extractScriptQueries(parserContext, 0, document.getLength(), true, false, false);
        
        SQLDialect dialect = editor.getSQLDialect();
        for(SQLScriptElement element: elements) {
            STMSource querySource = STMSource.fromString(element.getOriginalText());
            LSMAnalyzer analyzer = LSMDialectRegistry.getInstance().getAnalyzerForDialect(dialect);
            STMTreeRuleNode query = analyzer.parseSqlQueryTree(querySource, new STMSkippingErrorListener());
            
            Map<String, DBSTable> referencedTablesByAlias = new HashMap<>();
            List<DBSTable> allReferencedTables = new LinkedList<>();
            List<STMTreeNode> tableReferences = STMUtils.expandSubtree(
                    query,
                    TableReferencesRules.expandRulesToTableRef,
                    TableReferencesRules.extractRulesToTableRef);
            for (STMTreeNode tableRef : tableReferences) {
                String tableAlias = null;
                DBSTable referencedTable = null;
                for (STMTreeNode identifier : STMUtils.expandSubtree(tableRef, null, tableIdentifierNodeNames)) { // TODO consider another color for correlation
                    Interval interval = identifier.getRealInterval();
                    
                    if (identifier.getNodeName().equals(STMKnownRuleNames.tableName) && identifier instanceof STMTreeRuleNode ruleNode) {
                        if (editor.getDataSource() instanceof DBSObjectContainer container) { // just an example of real validation
                            DBSObject obj = SQLSearchUtils.findObjectByFQN(new VoidProgressMonitor(), container, editor.getExecutionContext(), STMUtils.expandTermStrings(identifier), false, null);
                            if (obj == null) {
                                context.registerToken(interval.a + element.getOffset(), interval.b + element.getOffset() + 1, SQLTokenType.T_SEMANTIC_ERROR);
                                continue;
                            } else if (obj instanceof DBSTable table) {
                                referencedTable = table;
                                allReferencedTables.add(table);
                            }
                        }   
                    } else if (identifier.getNodeName().equals(STMKnownRuleNames.correlationName)) {
                        tableAlias = STMUtils.expandTermStrings(identifier).stream().collect(Collectors.joining());
                    }
                    context.registerToken(interval.a + element.getOffset(), interval.b + element.getOffset() + 1, SQLTokenType.T_TABLE);
                }
                
                if (tableAlias != null && referencedTable != null) {
                    referencedTablesByAlias.put(tableAlias, referencedTable);
                }
            }

            List<STMTreeNode> asClauses = STMUtils.expandSubtree(
                    query,
                    null, // TODO clarify? depends on subquery processing strategy
                    Set.of(STMKnownRuleNames.asClause));
            Set<String> knownColumnAliases = new HashSet<>(asClauses.size());
            for (STMTreeNode asClause : asClauses) {
                for (STMTreeNode identifier : STMUtils.expandSubtree(asClause, null, columnIdentifierNodeNames)) {
                    Interval interval = identifier.getRealInterval();
                    context.registerToken(interval.a + element.getOffset(), interval.b + element.getOffset() + 1, SQLTokenType.T_COLUMN_ALIAS);
                    knownColumnAliases.add(identifier.getTextContent()); // TODO use actualIdentifier
                }
            }
            
            List<STMTreeNode> columnReferences = STMUtils.expandSubtree(
                    query,
                    null, // TODO clarify? depends on subquery processing strategy
                    columnReferenceNodeNames);
            for (STMTreeNode columnRef : columnReferences) {
                DBSTable table = null;
                String columnName = null;
                boolean isInCondition = false;
                SQLTokenType columnTokenType = SQLTokenType.T_COLUMN;
                Interval columnInterval = null;
                for (STMTreeNode parent = columnRef.getStmParent(); parent != null; parent = parent.getStmParent()) {
                    if (parent.getNodeName().equals(STMKnownRuleNames.tableExpression)) {
                        isInCondition = true;
                    }
                }
                for (STMTreeNode identifier : STMUtils.expandSubtree(columnRef, null, columnIdentifierNodeNames)) {                    
                    Interval interval = identifier.getRealInterval();
                    SQLTokenType tokenType = identifier.getNodeName().equals(STMKnownRuleNames.columnName) ? SQLTokenType.T_COLUMN : SQLTokenType.T_TABLE;
                    if (tokenType.equals(SQLTokenType.T_COLUMN)) {
                        if (isInCondition && knownColumnAliases.contains(identifier.getTextContent())) {
                            columnTokenType = SQLTokenType.T_COLUMN_ALIAS;
                        }
                        columnName = STMUtils.expandTermStrings(identifier).stream().collect(Collectors.joining());
                        columnInterval = interval;
                    } else if (tokenType.equals(SQLTokenType.T_TABLE)) {
                        List<String> nameParts = STMUtils.expandTermStrings(identifier);
                        String name = STMUtils.expandTermStrings(identifier).stream().collect(Collectors.joining());
                        table = referencedTablesByAlias.get(name);
                        if (table == null && editor.getDataSource() instanceof DBSObjectContainer container) { // just an example of real validation
                            DBSObject obj = SQLSearchUtils.findObjectByFQN(new VoidProgressMonitor(), container, editor.getExecutionContext(), nameParts, false, null);
                            if (obj instanceof DBSTable) {
                                table = (DBSTable)obj;
                            }
                        }
                        context.registerToken(interval.a + element.getOffset(), interval.b + element.getOffset() + 1, table != null ? SQLTokenType.T_TABLE : SQLTokenType.T_SEMANTIC_ERROR);
                        if (table == null) {
                            continue;
                        }
                    }
                }
                if (columnName != null) {
                    if (isInCondition) {
                        // TODO
                    } else {
                        if (table != null) { // explicitly referenced table.column
                            try {
                                DBSEntityAttribute tableAttr = null;
                                for (DBSEntityAttribute attr: table.getAttributes(new VoidProgressMonitor())) {
                                    if (!DBUtils.isHiddenObject(attr) && attr.getName().equalsIgnoreCase(columnName)) {
                                        tableAttr = attr;
                                    }
                                }
                                if (tableAttr == null) {
                                    columnTokenType = SQLTokenType.T_SEMANTIC_ERROR;
                                }
                            } catch (DBException e) {
                                e.printStackTrace();
                            }
                        } else {
                            for (DBSTable someTable: allReferencedTables) {
                                // TODO
                            }
                        }
                    }
                }
                context.registerToken(columnInterval.a + element.getOffset(), columnInterval.b + element.getOffset() + 1, columnTokenType);
            }
        }
//    
//    
//        if (model instanceof SelectStatement select) {
//            Map<String, String> tableRefs = new HashMap<>();
//            SelectionSource.Visitor v = new SelectionSource.Visitor() {
//                @Override
//                public void visitTable(Table table) {
//                    dialect.name
//                }
//                @Override
//                public void visitNaturalJoin(NaturalJoin naturalJoin) {
//                    // TODO Auto-generated method stub
//                    
//                }
//                @Override
//                public void visitCrossJoin(CrossJoin crossJoin) {
//                    // TODO Auto-generated method stub
//                    
//                }
//            };
//        }
        
        UIUtils.asyncExec(() -> {
            editor.getTextViewer().invalidateTextPresentation(0, document.getLength());
            //ISourceViewer viewer = editor.getViewer();
            //viewer.invalidateTextPresentation();
            // document.getDocumentPartitioner().documentChanged(new DocumentEvent(document, 0, document.getLength(), document.get()));
        });
        
        synchronized (syncRoot) {
            this.context = context;
            this.isRunning = false;
        }
    }
    
    private class DocumentLifecycleListener implements IDocumentListener, ITextInputListener {

        public void documentAboutToBeChanged(DocumentEvent event) {
            SQLBackgroundParsingJob.this.cancel();
        }

        public void documentChanged(DocumentEvent event) {
            SQLBackgroundParsingJob.this.schedule(event);
        }

        public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
            if (oldInput != null) {
                SQLBackgroundParsingJob.this.cancel();
                oldInput.removeDocumentListener(this);
            }
        }

        public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
            if (newInput != null) {
                newInput.addDocumentListener(this);
                SQLBackgroundParsingJob.this.setDocument(newInput);
            }
        }
    }
}

