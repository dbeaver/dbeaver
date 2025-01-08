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
package org.jkiss.dbeaver.model.sql.completion;

import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.eclipse.jface.text.BadLocationException;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDLabelValuePair;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableParametrized;
import org.jkiss.dbeaver.model.runtime.LocalCacheProgressMonitor;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.sql.analyzer.TableReferencesAnalyzer;
import org.jkiss.dbeaver.model.sql.analyzer.TableReferencesAnalyzerImpl;
import org.jkiss.dbeaver.model.sql.completion.hippie.HippieProposalProcessor;
import org.jkiss.dbeaver.model.sql.parser.SQLParserPartitions;
import org.jkiss.dbeaver.model.sql.parser.SQLWordPartDetector;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.*;
import org.jkiss.dbeaver.model.text.TextUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.Map.Entry;

/**
 * Completion analyzer
 */
public class SQLCompletionAnalyzer implements DBRRunnableParametrized<DBRProgressMonitor> {

    private static final Log log = Log.getLog(SQLCompletionAnalyzer.class);

    private static final String ALL_COLUMNS_PATTERN = "*";
    private static final String ENABLE_HIPPIE = "SQLEditor.ContentAssistant.activate.hippie";
    private static final String MATCH_ANY_PATTERN = "%";
    private static final String TABLE_TO_ATTRIBUTE_PATTERN = "%s%s%s";
    public static final int MAX_ATTRIBUTE_VALUE_PROPOSALS = 50;
    public static final int MAX_STRUCT_PROPOSALS = 100;
    private final SQLCompletionRequest request;
    private final TableReferencesAnalyzer tableRefsAnalyzer;
    private DBRProgressMonitor monitor;

    private final List<SQLCompletionProposalBase> proposals = new ArrayList<>();
    private boolean searchFinished = false;
    private boolean checkNavigatorNodes = true;

    public SQLCompletionAnalyzer(SQLCompletionRequest request) {
        this.request = request;

        final DBPPreferenceStore prefStore;
        final DBPDataSource dataSource = request.getContext().getDataSource();
        if (dataSource != null) {
            prefStore = request.getContext().getDataSource().getContainer().getPreferenceStore();
        } else {
            prefStore = DBWorkbench.getPlatform().getPreferenceStore();
        }

        tableRefsAnalyzer = new TableReferencesAnalyzerImpl(request);
    }

    @Override
    public void run(DBRProgressMonitor monitor) throws InvocationTargetException {
        try {
            runAnalyzer(monitor);
        } catch (DBException e) {
            throw new InvocationTargetException(e);
        }
    }

    public List<SQLCompletionProposalBase> getProposals() {
        return proposals;
    }

    public boolean isSearchFinished() {
        return searchFinished;
    }

    public void runAnalyzer(DBRProgressMonitor monitor) throws DBException {
        this.monitor = monitor;
        runAnalyzer();
    }

    private void runAnalyzer() throws DBException {
        String searchPrefix = request.getWordPart();
        request.setQueryType(null);
        SQLWordPartDetector wordDetector = request.getWordDetector();
        SQLSyntaxManager syntaxManager = request.getContext().getSyntaxManager();
        String prevKeyWord = wordDetector.getPrevKeyWord();
        boolean isPrevWordEmpty = CommonUtils.isEmpty(wordDetector.getPrevWords());
        boolean isInLiteral = SQLParserPartitions.CONTENT_TYPE_SQL_STRING.equals(request.getContentType());
        String prevDelimiter = wordDetector.getPrevDelimiter();
        // Here we handle the case when user started typing the new query on the next line without query delimiter for the previous one.
        // If setting `Blank line is statement delimiter` set, then active query is only newly typed characters
        // and prev word can't exist in this new query - offset of prev word doesn't fit active query offset, so we set it accordingly.
        if (request.getActiveQuery() == null || wordDetector.getPrevKeyWordOffset() < request.getActiveQuery().getOffset()) {
            prevKeyWord = null;
            isPrevWordEmpty = true;
        }
        {
            if (!CommonUtils.isEmpty(prevKeyWord)) {
                if (syntaxManager.getDialect().isEntityQueryWord(prevKeyWord)) {
                    // TODO: its an ugly hack. Need a better way
                    if (SQLConstants.KEYWORD_DELETE.equals(prevKeyWord) ||
                        SQLConstants.KEYWORD_INSERT.equals(prevKeyWord)
                    ) {
                        request.setQueryType(null);
                    } else if (SQLConstants.KEYWORD_INTO.equals(prevKeyWord) &&
                        !isPrevWordEmpty && ("(".equals(prevDelimiter) || ",".equals(prevDelimiter)))
                    {
                        request.setQueryType(SQLCompletionRequest.QueryType.COLUMN);
                    } else if (SQLConstants.KEYWORD_INTO.equals(prevKeyWord) && !isPrevWordEmpty && ("(*".equals(prevDelimiter) ||
                            "{*".equals(prevDelimiter) || "[*".equals(prevDelimiter))) {
                        wordDetector.shiftOffset(-SQLCompletionAnalyzer.ALL_COLUMNS_PATTERN.length());
                        searchPrefix = SQLCompletionAnalyzer.ALL_COLUMNS_PATTERN;
                        request.setQueryType(SQLCompletionRequest.QueryType.COLUMN);
                    } else if (SQLConstants.KEYWORD_JOIN.equals(prevKeyWord) && isPrevWordEmpty) {
                        request.setQueryType(SQLCompletionRequest.QueryType.JOIN);
                    } else {
                        if (!isPrevWordEmpty && CommonUtils.isEmpty(prevDelimiter)) {
                            // Seems to be table alias
                            //request.setQueryType(SQLCompletionRequest.QueryType.COLUMN);
                        } else if (SQLConstants.KEYWORD_INTO.equals(prevKeyWord) && isInLiteral) {
                            // Here we should not show any proposals
                            // INSERT INTO tableName VALUES ('|');
                            return;
                        } else {
                            request.setQueryType(SQLCompletionRequest.QueryType.TABLE);
                        }
                    }
                } else if (syntaxManager.getDialect().isAttributeQueryWord(prevKeyWord)) {
                    request.setQueryType(SQLCompletionRequest.QueryType.COLUMN);
                    char curChar = ' ';
                    try {
                        curChar = request.getDocument().getChar(wordDetector.getCursorOffset() - 1);
                    } catch (BadLocationException e) {
                        log.debug(e);
                    }
                    if (!request.isSimpleMode() &&
                        CommonUtils.isEmpty(request.getWordPart()) &&
                        prevDelimiter.indexOf(curChar) != -1 &&
                        prevDelimiter.equals(SQLCompletionAnalyzer.ALL_COLUMNS_PATTERN) &&
                        !CommonUtils.isEmpty(wordDetector.getNextWord()))
                    {
                        wordDetector.shiftOffset(-SQLCompletionAnalyzer.ALL_COLUMNS_PATTERN.length());
                        searchPrefix = SQLCompletionAnalyzer.ALL_COLUMNS_PATTERN;
                    }
                } else if (SQLUtils.isExecQuery(syntaxManager.getDialect(), prevKeyWord)) {
                    request.setQueryType(SQLCompletionRequest.QueryType.EXEC);
                }
            }
        }
        request.setWordPart(searchPrefix);

        DBPDataSource dataSource = request.getContext().getDataSource();
        if (dataSource == null) {
            return;
        }
        String wordPart = request.getWordPart();
        boolean emptyWord = wordPart.isEmpty();
        boolean isNumber = !CommonUtils.isEmpty(wordPart) && CommonUtils.isNumber(wordPart);
        boolean isInQuotedIdentifier = SQLParserPartitions.CONTENT_TYPE_SQL_QUOTED.equals(request.getContentType());

        SQLCompletionRequest.QueryType queryType = request.getQueryType();
        Map<String, Object> parameters = new LinkedHashMap<>();
        List<String> prevWords = wordDetector.getPrevWords();
        String previousWord = "";
        if (!CommonUtils.isEmpty(prevWords)) {
            previousWord = prevWords.get(0).toUpperCase(Locale.ENGLISH);
        }
        boolean procExec;
        if (!CommonUtils.isEmpty(prevWords) &&
            (SQLConstants.KEYWORD_PROCEDURE.equals(previousWord) || SQLConstants.KEYWORD_FUNCTION.equals(previousWord))) {
            parameters.put(SQLCompletionProposalBase.PARAM_EXEC, false);
            procExec = false;
        } else {
            parameters.put(SQLCompletionProposalBase.PARAM_EXEC, true);
            procExec = true;
        }
        if (queryType != null) {
            // Try to determine which object is queried (if wordPart is not empty)
            // or get list of root database objects
            if (emptyWord || isInLiteral || isNumber || isInQuotedIdentifier) {
                // Get root objects
                List<DBSObject> rootObjects = null;
                if (queryType == SQLCompletionRequest.QueryType.COLUMN && dataSource instanceof DBSObjectContainer) {
                    // Try to detect current table
                    rootObjects = getTableListFromAlias((DBSObjectContainer) dataSource, null);
                    if (prevKeyWord != null) {
                        switch (prevKeyWord) {
                            case SQLConstants.KEYWORD_ON:
                                // Join?
                                for (DBSObject obj : rootObjects) {
                                    makeJoinColumnProposals((DBSObjectContainer) dataSource, (DBSEntity) obj);
                                }
                                // Fall-thru
                            case SQLConstants.KEYWORD_SET:
                            case SQLConstants.KEYWORD_WHERE:
                            case SQLConstants.KEYWORD_AND:
                            case SQLConstants.KEYWORD_OR:
                                if (!request.isSimpleMode()) {
                                    boolean isLike = SQLConstants.KEYWORD_LIKE.equals(previousWord)
                                        || SQLConstants.KEYWORD_ILIKE.equals(previousWord);
                                    boolean waitsForValue = isInLiteral || (!CommonUtils.isEmpty(prevWords) &&
                                        isLike
                                        || (!CommonUtils.isEmpty(prevDelimiter) &&
                                            !prevDelimiter.endsWith(")")));
                                    if (waitsForValue && request.getContext().isShowValues()) {
                                        for (DBSObject obj : rootObjects) {
                                            makeProposalsFromAttributeValues(
                                                dataSource,
                                                wordDetector,
                                                isInLiteral || isNumber,
                                                (DBSEntity) obj);
                                        }
                                    }
                                }
                                break;
                            default:
                                break;
                        }
                    }
                } else if (dataSource instanceof DBSObjectContainer) {
                    // Try to get from active object
                    DBCExecutionContext context = request.getContext().getExecutionContext();
                    if (context != null) {
                        DBCExecutionContextDefaults<?, ?> contextDefaults = context.getContextDefaults();
                        if (contextDefaults != null) {
                            DBSSchema defaultSchema = contextDefaults.getDefaultSchema();
                            if (defaultSchema != null) {
                                makeProposalsFromChildren(defaultSchema, null, false, parameters);
                            }
                            DBSCatalog defaultCatalog = contextDefaults.getDefaultCatalog();
                            if (defaultCatalog != null) {
                                makeProposalsFromChildren(defaultCatalog, null, false, parameters);
                            }
                        }
                    }
                    // add top level objects to proposals
                    makeDataSourceProposals(parameters);
                }
                if (!isInLiteral) {
                    if (rootObjects != null) {
                        for (DBSObject obj : rootObjects) {
                            makeProposalsFromChildren(obj, null, false, parameters);
                        }
                    } else if (getActiveInstanceObject() == null) {
                        // get completion from data source
                        makeProposalsFromChildren(dataSource, null, false, parameters);
                    }
                    if (queryType == SQLCompletionRequest.QueryType.JOIN && !proposals.isEmpty()
                        && dataSource instanceof DBSObjectContainer) {
                        // Filter out non-joinable tables
                        DBSObject leftTable = getTableFromAlias((DBSObjectContainer) dataSource, null);
                        if (leftTable instanceof DBSEntity) {
                            filterNonJoinableProposals((DBSEntity) leftTable);
                        }
                    }
                }
            } else if (!isInLiteral) {
                DBSObject rootObject = null;
                if (queryType == SQLCompletionRequest.QueryType.COLUMN && dataSource instanceof DBSObjectContainer) {
                    // Part of column name
                    // Try to get from active object
                    DBSObjectContainer sc = (DBSObjectContainer) dataSource;
                    if (request.getContext().getExecutionContext() != null) {
                        DBSObject selectedObject = getActiveInstanceObject();
                        if (selectedObject instanceof DBSObjectContainer) {
                            sc = (DBSObjectContainer) selectedObject;
                        }
                    }
                    SQLDialect sqlDialect = request.getContext().getDataSource().getSQLDialect();
                    String tableAlias = null;
                    if (ALL_COLUMNS_PATTERN.equals(wordPart)) {
                        if (!isPrevWordEmpty) {
                            if (!prevKeyWord.equalsIgnoreCase("INTO")) {
                                String prevWord = wordDetector.getPrevWords().get(0);
                                if (prevWord.contains(sqlDialect.getCatalogSeparator())) {
                                    int divPos = prevWord.lastIndexOf(sqlDialect.getCatalogSeparator());
                                    tableAlias = prevWord.substring(0, divPos);
                                }
                            }
                        }
                    }
                    if (tableAlias == null) {
                        int divPos = wordPart.lastIndexOf(syntaxManager.getStructSeparator());
                        tableAlias = divPos == -1 ? null : wordPart.substring(0, divPos);
                    }
                    if (tableAlias == null && !CommonUtils.isEmpty(wordPart)) {
                        // May be an incomplete table alias. Try to find such table
                        rootObject = getTableFromAlias(sc, wordPart);
                        if (rootObject != null) {
                            // Found alias - no proposals
                            searchFinished = true;
                            return;
                        }
                    }
                    rootObject = getTableFromAlias(sc, tableAlias);
                    if (rootObject == null && tableAlias != null) {
                        // Maybe alias ss a table name
                        String[] allNames = SQLUtils.splitFullIdentifier(
                            tableAlias,
                            sqlDialect.getCatalogSeparator(),
                            sqlDialect.getIdentifierQuoteStrings(),
                            false);
                        rootObject = SQLSearchUtils.findObjectByFQN(monitor, sc, request, Arrays.asList(allNames));
                    }
                }
                if (rootObject != null) {
                    makeProposalsFromChildren(rootObject, wordPart, false, parameters);
                } else {
                    // Get root object or objects from active database (if any)
                    if (queryType != SQLCompletionRequest.QueryType.COLUMN && queryType != SQLCompletionRequest.QueryType.EXEC) {
                        makeDataSourceProposals(parameters);
                    }
                }
            }

            if (!request.isSimpleMode() &&
                !isInLiteral &&
                (queryType ==  SQLCompletionRequest.QueryType.EXEC ||
                (queryType == SQLCompletionRequest.QueryType.COLUMN && request.getContext().isSearchProcedures())) &&
                dataSource instanceof DBSObjectContainer)
            {
                makeProceduresProposals(dataSource, wordPart, procExec);
            }
        } else {
            if (!isInLiteral && !request.isSimpleMode() && !CommonUtils.isEmpty(prevWords)) {
                if (SQLConstants.KEYWORD_PROCEDURE.equals(previousWord) || SQLConstants.KEYWORD_FUNCTION.equals(previousWord)) {
                    makeProceduresProposals(dataSource, wordPart, procExec);
                }
            }
        }

        if (!emptyWord && !isInLiteral && !isInQuotedIdentifier) {
            makeProposalsFromQueryParts();
        }

        // Final filtering
        if (!searchFinished && !isInLiteral && !isInQuotedIdentifier) {
            List<String> matchedKeywords = Collections.emptyList();
            Set<String> allowedKeywords = null;

            SQLDialect sqlDialect = request.getContext().getDataSource().getSQLDialect();
            if (CommonUtils.isEmpty(prevKeyWord)) {
                allowedKeywords = new HashSet<>();
                Collections.addAll(allowedKeywords, sqlDialect.getQueryKeywords());
                Collections.addAll(allowedKeywords, sqlDialect.getDMLKeywords());
                Collections.addAll(allowedKeywords, sqlDialect.getDDLKeywords());
                Collections.addAll(allowedKeywords, sqlDialect.getExecuteKeywords());
            } else if (ArrayUtils.contains(sqlDialect.getQueryKeywords(), prevKeyWord.toUpperCase(Locale.ENGLISH))) {
                // SELECT ..
                // Limit with FROM if we already have some expression
                String delimiter = wordDetector.getPrevDelimiter();
                if (delimiter.equals(ALL_COLUMNS_PATTERN) ||
                    (!isPrevWordEmpty && (CommonUtils.isEmpty(delimiter) || delimiter.endsWith(")"))))
                {
                    // last expression ends with space or with ")"
                    allowedKeywords = new HashSet<>();
                    if (proposals.isEmpty() && CommonUtils.isEmpty(wordDetector.getPrevWords())) {
                        if (!SQLConstants.KEYWORD_FROM.equalsIgnoreCase(wordDetector.getNextWord())) {
                            // No proposals for *. Probably it is a query start
                            allowedKeywords.add(SQLConstants.KEYWORD_FROM);
                            if (CommonUtils.isEmpty(request.getWordPart()) || request.getWordPart().equals(ALL_COLUMNS_PATTERN)) {
                                matchedKeywords = Arrays.asList(SQLConstants.KEYWORD_FROM);
                            }
                        }
                    } else if (delimiter.equals(ALL_COLUMNS_PATTERN)) {
                        // Shift offset because we need space after *
                        wordDetector.shiftOffset(1);
                    }
                }
            } else if (sqlDialect.isEntityQueryWord(prevKeyWord)) {
                allowedKeywords = new HashSet<>();
                if (SQLConstants.KEYWORD_DELETE.equals(prevKeyWord)) {
                    allowedKeywords.add(SQLConstants.KEYWORD_FROM);
                } else if (SQLConstants.KEYWORD_INSERT.equals(prevKeyWord)) {
                    allowedKeywords.add(SQLConstants.KEYWORD_INTO);
                } else if (SQLConstants.KEYWORD_UPDATE.equals(prevKeyWord)) {
                    allowedKeywords.add(SQLConstants.KEYWORD_SET);
                } else {
                    if (!SQLConstants.KEYWORD_WHERE.equalsIgnoreCase(wordDetector.getNextWord()) &&
                        !SQLConstants.KEYWORD_INTO.equals(prevKeyWord)
                    ) {
                        allowedKeywords.add(SQLConstants.KEYWORD_WHERE);
                    }
                }
                if (CommonUtils.isEmpty(request.getWordPart())) {
                    matchedKeywords = new ArrayList<>(allowedKeywords);
                }
            }

            if (matchedKeywords.isEmpty() && !CommonUtils.isEmpty(request.getWordPart())) {
                // Keyword assist
                matchedKeywords = syntaxManager.getDialect().getMatchedKeywords(request.getWordPart());
                if (!request.isSimpleMode()) {
                    // Sort using fuzzy match
                    matchedKeywords.sort(Comparator.comparingInt(o -> TextUtils.fuzzyScore(o, request.getWordPart())));
                }
            }
            for (String keyWord : matchedKeywords) {
                DBPKeywordType keywordType = syntaxManager.getDialect().getKeywordType(keyWord);
                if (keywordType != null) {
                    if (keywordType == DBPKeywordType.TYPE) {
                        continue;
                    }
                    if (request.getQueryType() == SQLCompletionRequest.QueryType.COLUMN && !(keywordType == DBPKeywordType.FUNCTION || keywordType == DBPKeywordType.KEYWORD || keywordType == DBPKeywordType.OTHER)) {
                        continue;
                    }
                    if (allowedKeywords != null && !allowedKeywords.contains(keyWord)) {
                        continue;
                    }
                    proposals.add(
                        SQLCompletionAnalyzer.createCompletionProposal(
                            request,
                            keyWord,
                            keyWord,
                            false,
                            keywordType,
                            null,
                            false,
                            null,
                            Collections.emptyMap())
                    );
                }
            }
            if (dataSource.getContainer().getPreferenceStore().getBoolean(ENABLE_HIPPIE)) {
                makeProposalFromHippie(wordDetector);
            }
        }
        filterProposals(dataSource);
    }

    private void makeProposalFromHippie(@NotNull SQLWordPartDetector wordPartDetector) {
        HippieProposalProcessor hippieProposalProcessor = new HippieProposalProcessor(wordPartDetector);
        String[] displayNames = hippieProposalProcessor.computeCompletionStrings(request.getDocument(), request.getDocumentOffset() - 1);
        for (String word : displayNames) {
            if (!hasProposal(proposals, word) && !word.contains(".")) {
                proposals.add(request.getContext().createProposal(
                    request,
                    word,
                    word, // replacementString
                    word.length(), //cursorPosition the position of the cursor following the insert
                    null, //image to display
                    //new ContextInformation(null, displayString, displayString), //the context information associated with this proposal
                    DBPKeywordType.LITERAL,
                    null,
                    null,
                    Collections.emptyMap()));
            }
        }
    }
    @Nullable
    private DBSObject getActiveInstanceObject() {
        DBCExecutionContext context = request.getContext().getExecutionContext();
        if (context == null) {
            return null;
        }
        return DBUtils.getActiveInstanceObject(context);
    }

    private void makeProceduresProposals(@NotNull DBPDataSource dataSource, @NotNull String wordPart, boolean exec) throws DBException {
        // Add procedures/functions for column proposals
        DBSStructureAssistant<?> structureAssistant = DBUtils.getAdapter(DBSStructureAssistant.class, dataSource);
        DBSObjectContainer sc = (DBSObjectContainer) dataSource;
        DBSObject selectedObject = getActiveInstanceObject();
        if (selectedObject instanceof DBSObjectContainer) {
            SQLWordPartDetector wordDetector = request.getWordDetector();
            if (request.getContext().isSearchGlobally() && !wordDetector.containsSeparator(wordPart)) {
                // Like "SELECT proc_name|" (and proc_name is from another container)
                // Do not send information about the scheme to the assistant
            } else if (wordPart.length() > 1 && wordDetector.containsSeparator(wordPart) && !wordPart.contains(selectedObject.getName())) {
                // Like "SELECT schema_name.proc_name|" or just "SELECT schema_name.|" called from another container SQL editor
                // It seems the user indicates the full path to the procedure/function from another scheme.
                // Let's try to find a procedure container
                String[] objectsNames = wordDetector.splitIdentifier(wordPart);
                if (!ArrayUtils.isEmpty(objectsNames)) {
                    boolean endsOnStructureSeparator = wordPart.charAt(wordPart.length() - 1) == wordDetector.getStructSeparator();
                    int arrayIndex = 0;
                    if (endsOnStructureSeparator) {
                        // If word part ends on structure separator, then container name should be the last in the array
                        arrayIndex = objectsNames.length - 1;
                    } else if (objectsNames.length > 1) {
                        // In this case, the procedure name should be the last in the array and container name - second last
                        arrayIndex = objectsNames.length - 2;
                    }
                    String containerName = wordDetector.removeQuotes(objectsNames[arrayIndex]);
                    if (selectedObject instanceof DBSProcedureContainer) {
                        // selectedObject is a container, but not the one we are looking for. We will find our container through it
                        DBSObjectContainer selectedObjectParentObject = DBUtils.getParentOfType(DBSObjectContainer.class, selectedObject);
                        if (selectedObjectParentObject != null) {
                            DBSObject ourContainer = selectedObjectParentObject.getChild(monitor, containerName);
                            if (ourContainer instanceof DBSProcedureContainer && ourContainer instanceof DBSObjectContainer) {
                                sc = (DBSObjectContainer) ourContainer;
                            }
                        }
                    }
                }
            } else {
                sc = (DBSObjectContainer) selectedObject;
            }
        }
        if (structureAssistant != null) {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put(SQLCompletionProposalBase.PARAM_EXEC, exec);
            makeProposalsFromAssistant(
                structureAssistant,
                sc,
                new DBSObjectType[] { RelationalObjectType.TYPE_PROCEDURE },
                wordPart,
                params);
        }
    }

    private void makeProposalsFromAttributeValues(DBPDataSource dataSource, SQLWordPartDetector wordDetector, boolean isInLiteral, DBSEntity entity) throws DBException {
        List<String> prevWords = wordDetector.getPrevWords();
        if (!prevWords.isEmpty()) {
            // Column name?
            String columnName = prevWords.get(prevWords.size() - 1);
            if (!DBUtils.isQuotedIdentifier(dataSource, columnName)) {
                int divPos = columnName.indexOf(request.getContext().getSyntaxManager().getStructSeparator());
                if (divPos != -1) {
                    columnName = columnName.substring(divPos + 1);
                }
            }
            columnName = DBUtils.getUnQuotedIdentifier(dataSource, columnName);
            DBSEntityAttribute attribute = entity.getAttribute(monitor, columnName);

            if (attribute != null) {
                try (DBCSession session = request.getContext().getExecutionContext().openSession(monitor, DBCExecutionPurpose.META, "Read attribute values")) {

                    List<DBDLabelValuePair> valueEnumeration = null;

                    // For dictionary reference read dictionary values
                    // Otherwise try to read plain attribute values
                    DBSEntityReferrer enumConstraint = DBStructUtils.getEnumerableConstraint(monitor, attribute);
                    if (enumConstraint instanceof DBSEntityAssociation) {
                        DBSEntity dictEntity = DBStructUtils.getAssociatedEntity(monitor, enumConstraint);
                        if (dictEntity != null) {
                            DBSEntityAttribute refAttribute = DBUtils.getReferenceAttribute(monitor, (DBSEntityAssociation) enumConstraint, attribute, false);
                            if (refAttribute != null) {
                                valueEnumeration = ((DBSDictionary) dictEntity).getDictionaryEnumeration(
                                    monitor,
                                    refAttribute,
                                    null,
                                    null,
                                    Collections.emptyList(),
                                    true,
                                    true,
                                    false,
                                    0,
                                    MAX_ATTRIBUTE_VALUE_PROPOSALS
                                );
                            }
                        }
                    }

                    if (CommonUtils.isEmpty(valueEnumeration) && attribute instanceof DBSAttributeEnumerable) {
                        valueEnumeration = ((DBSAttributeEnumerable) attribute).getValueEnumeration(
                            session,
                            isInLiteral ? wordDetector.getFullWord() : null,
                            MAX_ATTRIBUTE_VALUE_PROPOSALS,
                            false,
                            false,
                            false);
                    }

                    if (!CommonUtils.isEmpty(valueEnumeration)) {
                        valueEnumeration.sort((o1, o2) -> DBUtils.compareDataValues(o1.getValue(), o2.getValue()));
                        DBDValueHandler valueHandler = DBUtils.findValueHandler(session, attribute);
                        DBPImage attrImage = null;
                        for (DBDLabelValuePair valuePair : valueEnumeration) {
                            String displayString = SQLUtils.convertValueToSQL(session.getDataSource(), attribute, valueHandler, valuePair.getValue(), DBDDisplayFormat.UI, false);
                            if (!CommonUtils.isEmpty(valuePair.getLabel()) && !CommonUtils.equalObjects(valuePair.getLabel(), valuePair.getValue())) {
                                displayString += " - " + valuePair.getLabel() + "";
                            }
                            String sqlValue = isInLiteral ?
                                valueHandler.getValueDisplayString(attribute, valuePair.getValue(), DBDDisplayFormat.NATIVE) :
                                SQLUtils.convertValueToSQL(dataSource.getDataSource(), attribute, valueHandler, valuePair.getValue(), DBDDisplayFormat.NATIVE, false);
                            proposals.add(request.getContext().createProposal(
                                request,
                                displayString,
                                sqlValue,
                                sqlValue.length(),
                                attrImage,
                                DBPKeywordType.LITERAL,
                                null,
                                null,
                                Collections.emptyMap()));
                        }
                    }
                }
            }
        }
    }

    private void filterProposals(DBPDataSource dataSource) {

        // Remove duplications
        final Set<String> proposalMap = new HashSet<>(proposals.size());
        for (int i = 0; i < proposals.size(); ) {
            SQLCompletionProposalBase proposal = proposals.get(i);
            if (proposalMap.contains(proposal.getDisplayString())) {
                proposals.remove(i);
                continue;
            }
            proposalMap.add(proposal.getDisplayString());
            i++;
        }

        DBSInstance defaultInstance = dataSource == null ? null : dataSource.getDefaultInstance();
        DBCExecutionContext executionContext = request.getContext().getExecutionContext();
        DBSObject selectedObject = defaultInstance == null || executionContext == null ? null : DBUtils.getActiveInstanceObject(executionContext);
        boolean hideDups = request.getContext().isHideDuplicates() && selectedObject != null;
        if (hideDups) {
            for (int i = 0; i < proposals.size(); i++) {
                SQLCompletionProposalBase proposal = proposals.get(i);
                for (int j = 0; j < proposals.size(); ) {
                    SQLCompletionProposalBase proposal2 = proposals.get(j);
                    if (i != j && proposal.hasStructObject() && proposal2.hasStructObject() &&
                        CommonUtils.equalObjects(proposal.getObject().getName(), proposal2.getObject().getName()) &&
                        proposal.getObjectContainer() == selectedObject) {
                        proposals.remove(j);
                    } else {
                        j++;
                    }
                }
            }
        }

        if (hideDups) {
            // Remove duplicates from non-active schema

            if (selectedObject instanceof DBSObjectContainer) {

            }

        }

        // Apply navigator object filters
        if (dataSource != null) {
            DBPDataSourceContainer dsContainer = dataSource.getContainer();
            Map<DBSObject, Map<Class<?>, List<SQLCompletionProposalBase>>> containerMap = new HashMap<>();
            for (SQLCompletionProposalBase proposal : proposals) {
                DBSObject container = proposal.getObjectContainer();
                DBPNamedObject object = proposal.getObject();
                if (object == null) {
                    continue;
                }
                Map<Class<?>, List<SQLCompletionProposalBase>> typeMap = containerMap.computeIfAbsent(container, k -> new HashMap<>());
                Class<?> objectType = object instanceof DBSObjectReference ? ((DBSObjectReference) object).getObjectClass() : object.getClass();
                List<SQLCompletionProposalBase> list = typeMap.computeIfAbsent(objectType, k -> new ArrayList<>());
                list.add(proposal);
            }
            for (Map.Entry<DBSObject, Map<Class<?>, List<SQLCompletionProposalBase>>> entry : containerMap.entrySet()) {
                for (Map.Entry<Class<?>, List<SQLCompletionProposalBase>> typeEntry : entry.getValue().entrySet()) {
                    DBSObjectFilter filter = dsContainer.getObjectFilter(typeEntry.getKey(), entry.getKey(), true);
                    if (filter != null && filter.isEnabled()) {
                        for (SQLCompletionProposalBase proposal : typeEntry.getValue()) {
                            if (!filter.matches(proposal.getObject().getName())) {
                                proposals.remove(proposal);
                            }
                        }
                    }
                }
            }
        }
    }

    private void makeProposalsFromQueryParts() {
        if (request.getQueryType() == null && request.getWordDetector().getPrevKeyWord().equalsIgnoreCase(SQLConstants.KEYWORD_FROM)) {
            // Seems to be table alias
            return;
        }
        String wordPart = request.getWordPart();
        // Find all aliases matching current word
        SQLScriptElement activeQuery = request.getActiveQuery();
        if (activeQuery != null && !CommonUtils.isEmpty(activeQuery.getText()) && !CommonUtils.isEmpty(wordPart)) {
            if (wordPart.indexOf(request.getContext().getSyntaxManager().getStructSeparator()) != -1 || wordPart.equals(ALL_COLUMNS_PATTERN)) {
                return;
            }
            final Map<String, String> names = tableRefsAnalyzer.getFilteredTableReferences(wordPart, true);
            for (Entry<String, String> name : names.entrySet()) {
                final String tableName = name.getKey();
                final String tableAlias = name.getValue();
                if (!CommonUtils.isEmpty(tableName) && !hasProposal(proposals, tableName)) {
                    proposals.add(
                        0,
                        SQLCompletionAnalyzer.createCompletionProposal(
                            request,
                            tableName,
                            tableName,
                            false,
                            DBPKeywordType.OTHER,
                            null,
                            false,
                            null,
                            Map.of(SQLCompletionProposalBase.PARAM_NO_SPACE, true))
                    );
                }
                if (!CommonUtils.isEmpty(tableAlias) && !hasProposal(proposals, tableAlias)) {
                    proposals.add(
                        0,
                        SQLCompletionAnalyzer.createCompletionProposal(
                            request,
                            tableAlias,
                            tableAlias,
                            false,
                            DBPKeywordType.OTHER,
                            null,
                            false,
                            null,
                            Map.of(SQLCompletionProposalBase.PARAM_NO_SPACE, true))
                    );
                }
            }
        }
    }

    private static boolean hasProposal(List<SQLCompletionProposalBase> proposals, String displayName) {
        for (SQLCompletionProposalBase proposal : proposals) {
            if (displayName.equals(proposal.getDisplayString())) {
                return true;
            }
        }
        return false;
    }

    private boolean makeJoinColumnProposals(DBSObjectContainer sc, DBSEntity leftTable) {
        SQLWordPartDetector joinTableDetector = new SQLWordPartDetector(
            request.getDocument(),
            request.getContext().getSyntaxManager(),
            request.getWordDetector().getStartOffset(),
            2);
        List<String> prevWords = joinTableDetector.getPrevWords();

        if (!CommonUtils.isEmpty(prevWords)) {
            DBPDataSource dataSource = request.getContext().getDataSource();
            SQLDialect sqlDialect = dataSource.getSQLDialect();
            String rightTableName = prevWords.get(0);
            String[] allNames = SQLUtils.splitFullIdentifier(
                rightTableName,
                sqlDialect.getCatalogSeparator(),
                sqlDialect.getIdentifierQuoteStrings(),
                false);
            DBSObject rightTable = SQLSearchUtils.findObjectByFQN(monitor, sc, request, Arrays.asList(allNames));
            if (rightTable instanceof DBSEntity) {
                try {
                    String joinCriteria = SQLUtils.generateTableJoin(monitor, leftTable, DBUtils.getQuotedIdentifier(leftTable), (DBSEntity) rightTable, DBUtils.getQuotedIdentifier(rightTable));
                    proposals.add(createCompletionProposal(request, joinCriteria, joinCriteria, DBPKeywordType.OTHER, "Join condition"));
                    return true;
                } catch (DBException e) {
                    log.error("Error generating join condition", e);
                }
            }
        }
        return false;
    }

    private void filterNonJoinableProposals(DBSEntity leftTable) {
        // Remove all table proposals which don't have FKs between them and leftTable
        List<SQLCompletionProposalBase> joinableProposals = new ArrayList<>();
        for (SQLCompletionProposalBase proposal : proposals) {
            if (proposal.getObject() instanceof DBSEntity) {
                DBSEntity rightTable = (DBSEntity) proposal.getObject();
                if (tableHaveJoins(rightTable, leftTable) || tableHaveJoins(leftTable, rightTable)) {
                    proposal.setReplacementAfter(" ON");
                    joinableProposals.add(proposal);
                }
            }
        }
        if (!joinableProposals.isEmpty()) {
            proposals.clear();
            proposals.addAll(joinableProposals);
        }
    }

    private boolean tableHaveJoins(DBSEntity table1, DBSEntity table2) {
        try {
            Collection<? extends DBSEntityAssociation> associations = table1.getAssociations(monitor);
            if (!CommonUtils.isEmpty(associations)) {
                for (DBSEntityAssociation fk : associations) {
                    if (fk.getAssociatedEntity() == table2) {
                        return true;
                    }
                }
            }
            return false;
        } catch (DBException e) {
            log.error(e);
            return false;
        }
    }

    private void makeDataSourceProposals(@NotNull Map<String, Object> parameters) throws DBException {
        DBPDataSource dataSource = request.getContext().getDataSource();
        final DBSObjectContainer rootContainer = DBUtils.getAdapter(DBSObjectContainer.class, dataSource);
        if (rootContainer == null) {
            return;
        }
        DBCExecutionContext executionContext = request.getContext().getExecutionContext();
        if (executionContext == null) {
            return;
        }

        DBSObjectContainer sc = rootContainer;
        DBSObject childObject = sc;
        String[] tokens = Arrays.stream(request.getWordDetector().splitWordPart()).filter(CommonUtils::isNotEmpty).toArray(String[]::new);

        // Detect selected object (container).
        // There could be multiple selected objects on different hierarchy levels (e.g. PG)
        DBSObjectContainer[] selectedContainers;
        {
            DBSObject[] selectedObjects = DBUtils.getSelectedObjects(executionContext);
            selectedContainers = new DBSObjectContainer[selectedObjects.length];
            for (int i = 0; i < selectedObjects.length; i++) {
                selectedContainers[i] = DBUtils.getAdapter(DBSObjectContainer.class, selectedObjects[i]);
            }
        }

        String lastToken = null;
        for (int i = 0; i < tokens.length; i++) {
            final String token = tokens[i];
            if (i == tokens.length - 1 && !request.getWordDetector().getWordPart().endsWith(".")) {
                lastToken = token;
                break;
            }
            if (sc == null) {
                break;
            }
            // Get next structure container
            final String objectName =
                request.getWordDetector().isQuoted(token) ? request.getWordDetector().removeQuotes(token) :
                DBObjectNameCaseTransformer.transformName(dataSource, token);
            sc.cacheStructure(monitor, DBSObjectContainer.STRUCT_ENTITIES);
            childObject = objectName == null ? null : sc.getChild(monitor, objectName);
            if (!DBStructUtils.isConnectedContainer(childObject)) {
                childObject = null;
            }
            if (childObject == null && i == 0 && objectName != null) {
                for (DBSObjectContainer selectedContainer : selectedContainers) {
                    if (selectedContainer != null) {
                        // Probably it is from selected object, let's try it
                        selectedContainer.cacheStructure(monitor, DBSObjectContainer.STRUCT_ENTITIES);
                        childObject = selectedContainer.getChild(monitor, objectName);
                        if (childObject != null) {
                            sc = selectedContainer;
                            break;
                        }
                    }
                }
            }
            if (childObject == null) {
                if (i == 0) {
                    // Assume it's a table alias ?
                    childObject  = getTableFromAlias(sc, token);
                    if (childObject == null  && !request.isSimpleMode()) {
                        // Search using structure assistant
                        DBSStructureAssistant structureAssistant = DBUtils.getAdapter(DBSStructureAssistant.class, sc);
                        if (structureAssistant != null) {
                            DBSStructureAssistant.ObjectsSearchParams params = new DBSStructureAssistant.ObjectsSearchParams(
                                    structureAssistant.getAutoCompleteObjectTypes(),
                                    request.getWordDetector().removeQuotes(token)
                            );
                            params.setCaseSensitive(request.getWordDetector().isQuoted(token));
                            params.setMaxResults(2);
                            Collection<DBSObjectReference> references = structureAssistant.findObjectsByMask(monitor, executionContext, params);
                            if (!references.isEmpty()) {
                                childObject = references.iterator().next().resolveObject(monitor);
                            }
                        }
                    }
                } else {
                    // Path element not found. Damn - can't do anything.
                    return;
                }
            }

            if (childObject instanceof DBSObjectContainer) {
                sc = (DBSObjectContainer) childObject;
            } else {
                sc = null;
            }
        }
        if (childObject == null) {
            return;
        }
        if (lastToken == null) {
            // Get all children objects as proposals
            makeProposalsFromChildren(childObject, null, false, parameters);
        } else {
            // Get matched children
            makeProposalsFromChildren(childObject, lastToken, false, parameters);
            if (tokens.length == 1) {
                // Get children from selected object
            }
            if (tokens.length == 1) {
                // Try in active object
                for (DBSObjectContainer selectedContainer : selectedContainers) {
                    if (selectedContainer != null && selectedContainer != childObject) {
                        makeProposalsFromChildren(selectedContainer, lastToken, true, parameters);
                    }
                }

                if (proposals.isEmpty() && !request.isSimpleMode()) {
                    // At last - try to find child tables by pattern
                    DBSStructureAssistant<?> structureAssistant = null;
                    for (DBSObject object = childObject; object != null; object =  object.getParentObject()) {
                        structureAssistant = DBUtils.getAdapter(DBSStructureAssistant.class, object);
                        if (structureAssistant != null) {
                            break;
                        }
                    }
                    if (structureAssistant != null) {
                        makeProposalsFromAssistant(structureAssistant, sc, null, lastToken, parameters);
                    }
                }
            }
        }
    }

    @Nullable
    private List<DBSObject> getTableListFromAlias(DBSObjectContainer sc, @Nullable String token)
    {
        if (token == null) {
            token = "";
        } else if (token.equals(ALL_COLUMNS_PATTERN)) {
            return null;
        }

        final DBPDataSource dataSource = request.getContext().getDataSource();
        if (dataSource == null) {
            return null;
        }

        final SQLDialect sqlDialect = dataSource.getSQLDialect();
        final String catalogSeparator = sqlDialect.getCatalogSeparator();

        while (token.endsWith(catalogSeparator)) {
            token = token.substring(0, token.length() - 1);
        }

        final Map<String, String> names = tableRefsAnalyzer.getFilteredTableReferences(token, false);
        List<DBSObject> objects = new ArrayList<>();
        for (Entry<String, String> name : names.entrySet()) {
            if (name != null && CommonUtils.isNotEmpty(name.getKey())) {
                final String[][] quoteStrings = sqlDialect.getIdentifierQuoteStrings();
                final String[] allNames = SQLUtils.splitFullIdentifier(name.getKey(), catalogSeparator, quoteStrings, false);
                DBSObject obj = SQLSearchUtils.findObjectByFQN(monitor, sc, request, Arrays.asList(allNames));
                if (obj != null) {
                    objects.add(obj);
                }
            }
        }
        return objects;
    }
    
    @Nullable
    private DBSObject getTableFromAlias(DBSObjectContainer sc, @Nullable String token) {
        if (token == null) {
            token = "";
        } else if (token.equals(ALL_COLUMNS_PATTERN)) {
            return null;
        }

        final DBPDataSource dataSource = request.getContext().getDataSource();
        if (dataSource == null) {
            return null;
        }

        final SQLDialect sqlDialect = dataSource.getSQLDialect();
        final String catalogSeparator = sqlDialect.getCatalogSeparator();

        while (token.endsWith(catalogSeparator)) {
            token = token.substring(0, token.length() - 1);
        }

        final Map<String, String> names = tableRefsAnalyzer.getFilteredTableReferences(token, false);
        for (Entry<String, String> name : names.entrySet()) {
            if (name != null && CommonUtils.isNotEmpty(name.getKey())) {
                final String[][] quoteStrings = sqlDialect.getIdentifierQuoteStrings();
                final String[] allNames = SQLUtils.splitFullIdentifier(name.getKey(), catalogSeparator, quoteStrings, false);
                return SQLSearchUtils.findObjectByFQN(monitor, sc, request, Arrays.asList(allNames));
            }
        }
        return null;
    }

    public void setCheckNavigatorNodes(boolean check) {
        this.checkNavigatorNodes = check;
    }

    private void makeProposalsFromChildren(DBPObject parent, @Nullable String startPart, boolean addFirst, Map<String, Object> params) throws DBException {
        if (request.getQueryType() == SQLCompletionRequest.QueryType.EXEC) {
            return;
        }
        DBRProgressMonitor mdMonitor = request.getContext().getDataSource().getContainer().isExtraMetadataReadEnabled() ?
            monitor : new LocalCacheProgressMonitor(monitor);

        if (parent instanceof DBSAlias alias && !mdMonitor.isForceCacheUsage()) {
            DBSObject realParent = alias.getTargetObject(mdMonitor);
            if (realParent == null) {
                log.debug("Can't get synonym target object");
            } else {
                parent = realParent;
            }
        }
        SQLWordPartDetector wordDetector = request.getWordDetector();
        if (startPart != null) {
            startPart = wordDetector.removeQuotes(startPart).toUpperCase(Locale.ENGLISH);
            int divPos = startPart.lastIndexOf(request.getContext().getSyntaxManager().getStructSeparator());
            if (divPos != -1) {
                startPart = startPart.substring(divPos + 1);
            }
        }

        DBPDataSource dataSource = request.getContext().getDataSource();
        Collection<? extends DBSObject> children = null;
        if (parent instanceof DBSObjectContainer objectContainer) {
            if (DBStructUtils.isConnectedContainer(parent)) {
                children = objectContainer.getChildren(mdMonitor);
            }
        } else if (parent instanceof DBSEntity entity) {
            children = entity.getAttributes(mdMonitor);
        }
        if (children != null && !children.isEmpty()) {
            //boolean isJoin = SQLConstants.KEYWORD_JOIN.equals(request.wordDetector.getPrevKeyWord());
            List<DBSObject> matchedObjects = new ArrayList<>();
            final Map<String, Integer> scoredMatches = new HashMap<>();
            boolean simpleMode = request.isSimpleMode();
            boolean allObjects = !simpleMode && ALL_COLUMNS_PATTERN.equals(startPart);
            String objPrefix = null;
            if (allObjects) {
                if (!CommonUtils.isEmpty(wordDetector.getPrevWords())) {
                    String prevWord = wordDetector.getPrevWords().get(0);
                    if (!prevWord.isEmpty() && prevWord.charAt(prevWord.length() - 1) == request.getContext().getSyntaxManager().getStructSeparator()) {
                        objPrefix = prevWord;
                    }
                }
            }
            StringBuilder combinedMatch = new StringBuilder();
            for (DBSObject child : children) {
                if (DBUtils.isHiddenObject(child)) {
                    // Skip hidden
                    continue;
                }
                if (DBUtils.isVirtualObject(child)) {
                    makeProposalsFromChildren(child, startPart, addFirst, Collections.emptyMap());
                    continue;
                }
                if (allObjects) {
                    if (!combinedMatch.isEmpty()) {
                        combinedMatch.append(", ");
                        if (objPrefix != null) combinedMatch.append(objPrefix);
                    }
                    combinedMatch.append(DBUtils.getQuotedIdentifier(child));
                } else {
                    if (dataSource != null && !request.getContext().isSearchInsideNames()) {
                        // startsWith
                        if (CommonUtils.isEmpty(startPart) || CommonUtils.startsWithIgnoreCase(child.getName(), startPart)) {
                            matchedObjects.add(child);
                        }
                    } else {
                        // Use fuzzy search for contains
                        int score = CommonUtils.isEmpty(startPart) ? 1 : TextUtils.fuzzyScore(child.getName(), startPart);
                        if (score > 0) {
                            matchedObjects.add(child);
                            scoredMatches.put(child.getName(), score);
                        }
                    }
                }
            }
            if (!combinedMatch.isEmpty()) {
                String replaceString = combinedMatch.toString();

                proposals.add(createCompletionProposal(
                    request,
                    replaceString,
                    replaceString,
                    DBPKeywordType.OTHER,
                    "All objects"));
            } else if (!matchedObjects.isEmpty()) {
                if (startPart == null || scoredMatches.isEmpty()) {
                    if (dataSource != null && request.getContext().isSortAlphabetically()) {
                        matchedObjects.sort((o1, o2) -> {
                            if (o1 instanceof DBSAttributeBase && o2 instanceof DBSAttributeBase) {
                                return DBUtils.orderComparator().compare((DBSAttributeBase) o1, (DBSAttributeBase) o2);
                            }
                            return DBUtils.nameComparatorIgnoreCase().compare(o1, o2);
                        });
                    }
                } else {
                    matchedObjects.sort((o1, o2) -> {
                        int score1 = scoredMatches.get(o1.getName());
                        int score2 = scoredMatches.get(o2.getName());
                        if (score1 == score2) {
                            if (o1 instanceof DBSAttributeBase && o2 instanceof DBSAttributeBase) {
                                return DBUtils.orderComparator().compare((DBSAttributeBase) o1, (DBSAttributeBase) o2);
                            }
                            return DBUtils.nameComparatorIgnoreCase().compare(o1, o2);
                        }
                        return score2 - score1;
                    });
                }
                List<SQLCompletionProposalBase> childProposals = new ArrayList<>(matchedObjects.size());
                for (DBSObject child : matchedObjects) {
                    SQLCompletionProposalBase proposal = makeProposalsFromObject(child, !(parent instanceof DBPDataSource), params);
                    if (proposal == null) {
                        continue;
                    }
                    if (!scoredMatches.isEmpty()) {
                        int proposalScore = scoredMatches.get(child.getName());
                        proposal.setProposalScore(proposalScore);
                    }

                    childProposals.add(proposal);
                }
                if (addFirst) {
                    // Add proposals in the beginning (because the most strict identifiers have to be first)
                    proposals.addAll(0, childProposals);
                } else {
                    proposals.addAll(childProposals);
                }
            }
        }
    }

    private void makeProposalsFromAssistant(
        @NotNull DBSStructureAssistant assistant,
        @Nullable DBSObjectContainer rootSC,
        DBSObjectType[] objectTypes,
        String objectName,
        @NotNull Map<String, Object> params) throws DBException
    {
        DBSStructureAssistant.ObjectsSearchParams assistantParams = new DBSStructureAssistant.ObjectsSearchParams(
                objectTypes == null ? assistant.getAutoCompleteObjectTypes() : objectTypes,
                makeObjectNameMask(objectName, rootSC)
        );
        assistantParams.setParentObject(rootSC);
        assistantParams.setCaseSensitive(request.getWordDetector().isQuoted(objectName));
        assistantParams.setGlobalSearch(request.getContext().isSearchGlobally());
        assistantParams.setMaxResults(MAX_STRUCT_PROPOSALS);
        Collection<DBSObjectReference> references = assistant.findObjectsByMask(monitor, request.getContext().getExecutionContext(), assistantParams);
        for (DBSObjectReference reference : references) {
            proposals.add(
                makeProposalsFromObject(
                    reference,
                    !(rootSC instanceof DBPDataSource),
                    reference.getObjectType().getImage(),
                    params));
        }
    }

    private String makeObjectNameMask(String objectName, @Nullable DBSObjectContainer rootSC) {
        SQLWordPartDetector wordDetector = request.getWordDetector();
        if (wordDetector.containsSeparator(objectName)) {
            String[] strings = wordDetector.splitIdentifier(objectName);
            if (rootSC != null) {
                boolean endsOnStructureSeparator = objectName.charAt(objectName.length() - 1) == wordDetector.getStructSeparator();
                if (endsOnStructureSeparator) {
                    // Any object name in this case
                    objectName = "";
                } else {
                    // We assume at this stage that the user writes the full path to the object, once in the objectName there are separators.
                    // To search through an structure assistant, we need only the last part of the objectName string after the last separator
                    objectName = wordDetector.removeQuotes(strings[strings.length - 1]);
                }
            }
        } else {
            objectName = wordDetector.removeQuotes(objectName);
        }
        if (request.getContext().isSearchInsideNames()) {
            if (CommonUtils.isEmpty(objectName)) {
                return MATCH_ANY_PATTERN;
            }
            return MATCH_ANY_PATTERN + objectName + MATCH_ANY_PATTERN;
        } else {
            return objectName + MATCH_ANY_PATTERN;
        }
    }

    private SQLCompletionProposalBase makeProposalsFromObject(DBSObject object, boolean useShortName, Map<String, Object> params) {
        DBNNode node;
        if (request.getContext().getDataSource().getContainer().isExtraMetadataReadEnabled()) {
            node = DBNUtils.getNodeByObject(monitor, object, false);
        } else {
            node = DBNUtils.getNodeByObject(object);
        }
        if (checkNavigatorNodes && node == null && (object instanceof DBSEntity || object instanceof DBSObjectContainer)) {
            return null;
        }

        DBPImage objectIcon = node == null ? null : node.getNodeIconDefault();
        if (objectIcon == null) {
            objectIcon = DBValueFormatting.getObjectImage(object);
        }
        return makeProposalsFromObject(object, useShortName, objectIcon, params);
    }

    private SQLCompletionProposalBase makeProposalsFromObject(
        DBPNamedObject object,
        boolean useShortName,
        @Nullable DBPImage objectIcon,
        @NotNull Map<String, Object> params) {
        String alias = null;
        String objectName = null;
        String replaceString = null;
        boolean isSingleObject = true;
        boolean isFQName = false;
        SQLTableAliasInsertMode aliasMode = SQLTableAliasInsertMode.NONE;
        String prevWord = request.getWordDetector().getPrevKeyWord();
        if (SQLConstants.KEYWORD_FROM.equals(prevWord) ||
            SQLConstants.KEYWORD_INTO.equals(prevWord) ||
            SQLConstants.KEYWORD_JOIN.equals(prevWord)) {
            if (object instanceof DBSEntity) {
                aliasMode = SQLTableAliasInsertMode
                    .fromPreferences(((DBSEntity) object).getDataSource().getContainer().getPreferenceStore());
            }
            if (aliasMode != SQLTableAliasInsertMode.NONE) {
                SQLDialect dialect = SQLUtils.getDialectFromObject(object);
                if (dialect.supportsAliasInSelect() && request.getActiveQuery() != null) {
                    String firstKeyword = SQLUtils.getFirstKeyword(dialect, request.getActiveQuery().getText());
                    if (dialect.supportsAliasInUpdate()
                        || !ArrayUtils.contains(dialect.getDMLKeywords(), firstKeyword.toUpperCase(Locale.ENGLISH))) {

                        Set<String> aliases = new LinkedHashSet<>();
                        if (request.getActiveQuery() instanceof SQLQuery) {
                            Statement sqlStatement = ((SQLQuery) request.getActiveQuery()).getStatement();
                            if (sqlStatement != null) {
                                TablesNamesFinder namesFinder = new TablesNamesFinder() {
                                    @Override
                                    public void visit(@Nullable Table table) {
                                        if (table != null && table.getAlias() != null && table.getAlias().getName() != null) {
                                            aliases.add(table.getAlias().getName().toLowerCase(Locale.ENGLISH));
                                        }
                                    }

                                    @Override
                                    public void visit(@Nullable CreateView createView) {
                                        if (createView != null && createView.getView().getAlias() != null
                                            && createView.getView().getName() != null) {
                                            aliases.add(createView.getView().getAlias().getName().toLowerCase(Locale.ENGLISH));
                                        }
                                    }
                                };
                                sqlStatement.accept(namesFinder);
                            }
                        }
                        // It is table name completion after FROM. Auto-generate table alias
                        SQLDialect sqlDialect = SQLUtils.getDialectFromObject(object);
                        alias = SQLUtils.generateEntityAlias((DBSEntity) object, s -> {
                            if (aliases.contains(s) || sqlDialect.getKeywordType(s) != null) {
                                return true;
                            }
                            return !tableRefsAnalyzer.getFilteredTableReferences(s, false).isEmpty();
                        });
                        if (alias.equalsIgnoreCase(object.getName())) {
                            // Don't use alias, when it's identical to entity name
                            alias = "";
                        }
                    }
                }
            }
        }
        if (SQLConstants.KEYWORD_WHERE.equals(prevWord) ||
            SQLConstants.KEYWORD_AND.equals(prevWord)) {
            String tableName;
            DBSEntity parentObject = null;
            if (object instanceof DBSTableColumn tableColumn) {
                aliasMode = SQLTableAliasInsertMode
                    .fromPreferences(tableColumn.getDataSource().getContainer().getPreferenceStore());
                parentObject = tableColumn.getParentObject();
            } else if (object instanceof DBSEntityAttribute tableColumn) {
                aliasMode = SQLTableAliasInsertMode
                    .fromPreferences(tableColumn.getDataSource().getContainer().getPreferenceStore());
                parentObject = tableColumn.getParentObject();
            }
            SQLDialect sqlDialect = SQLUtils.getDialectFromObject(object);
            if (parentObject != null) {
                tableName = DBUtils.getQuotedIdentifier(parentObject);
            } else {
                tableName = object.getName();
            }
            if (aliasMode != SQLTableAliasInsertMode.NONE) {
                Map<String, String> table2Alices = tableRefsAnalyzer.getTableAliasesFromQuery();
                alias = table2Alices.get(tableName);
                String wordPart = request.getWordDetector().getWordPart();
                objectName = DBUtils.getQuotedIdentifier(object);
                if (wordPart.isEmpty()) {
                    objectName = String.format(
                        TABLE_TO_ATTRIBUTE_PATTERN,
                        Objects.requireNonNullElse(alias, tableName),
                        sqlDialect.getStructSeparator(),
                        objectName);
                }
                replaceString = objectName;
            }
        }
        if (objectName == null || objectName.isEmpty()) {
            objectName = useShortName ? object.getName() : DBUtils.getObjectFullName(object, DBPEvaluationContext.DML);
        }
        if (replaceString == null || replaceString.isEmpty()) {
            DBPDataSource dataSource = request.getContext().getDataSource();
            // If we replace short name with referenced object
            // and current active schema (catalog) is not this object's container then
            // replace with full qualified name
            if (dataSource != null
                && !request.getContext().isUseShortNames()
                && object instanceof DBSObjectReference
                && request.getWordDetector().getFullWord()
                    .indexOf(request.getContext().getSyntaxManager().getStructSeparator()) == -1) {
                DBSObjectReference structObject = (DBSObjectReference) object;
                DBSObject objectContainer = structObject.getContainer();
                if (objectContainer != null) {
                    DBSObject selectedObject = getActiveInstanceObject();
                    if (selectedObject != null && selectedObject != objectContainer) {
                        if (DBSProcedure.class.isAssignableFrom(structObject.getObjectClass())) {
                            // We do not need full routine name with parameters here
                            replaceString = DBUtils.getFullQualifiedName(dataSource, objectContainer, structObject);
                        } else {
                            replaceString = structObject.getFullyQualifiedName(DBPEvaluationContext.DML);
                            isFQName = true;
                        }
                        isSingleObject = false;
                    }
                }
            }
            if (replaceString == null) {
                if (request.getContext().isUseFQNames() && object instanceof DBPQualifiedObject qo) {
                    replaceString = qo.getFullyQualifiedName(DBPEvaluationContext.DML);
                    isFQName = true;
                } else {
                    replaceString = DBUtils.getQuotedIdentifier(dataSource, object.getName());
                }
            }
        }

        if (!SQLConstants.KEYWORD_WHERE.equals(prevWord)
            && !SQLConstants.KEYWORD_AND.equals(prevWord)
            && !CommonUtils.isEmpty(alias)) {
            if (aliasMode == SQLTableAliasInsertMode.EXTENDED) {
                replaceString += " " + convertKeywordCase(request, "as", false);
            }
            replaceString += " " + alias;
        }
        return createCompletionProposal(
            request,
            replaceString,
            objectName,
            isFQName,
            DBPKeywordType.OTHER,
            objectIcon,
            isSingleObject,
            object,
            params);
    }
 
    /*
        * Turns the vector into an Array of ICompletionProposal objects
        */
    static SQLCompletionProposalBase createCompletionProposal(
        SQLCompletionRequest request,
        String replaceString,
        String displayString,
        boolean isFQName,
        DBPKeywordType proposalType,
        @Nullable DBPImage image,
        boolean isObject,
        @Nullable DBPNamedObject object,
        @NotNull Map<String, Object> params)
    {
        //SQLEditorBase editor = request.editor;
        //DBPPreferenceStore store = editor.getActivePreferenceStore();
        DBPDataSource dataSource = request.getContext().getDataSource();
        if (dataSource != null) {
            if (isObject) {
                // Escape replace string if required
                // FIXME: do not escape! it may (will) escape identifiers twice
                //replaceString = DBUtils.getQuotedIdentifier(dataSource, replaceString);
            }
        }

        // If we have quoted string then ignore pref settings
        boolean quotedString = request.getWordDetector().isQuoted(replaceString);
        if (!quotedString && !isFQName) {
            replaceString = convertKeywordCase(request, replaceString, isObject);
        }
        int cursorPos;
        if (proposalType == DBPKeywordType.FUNCTION) {
            replaceString += "()";
            cursorPos = replaceString.length() - 2;
        } else {
            cursorPos = replaceString.length();
        }

        return request.getContext().createProposal(
            request,
            displayString,
            replaceString, // replacementString
            cursorPos, //cursorPosition the position of the cursor following the insert relative to replacementOffset
            image, //image to display
            //new ContextInformation(img, displayString, displayString), //the context information associated with this proposal
            proposalType,
            null,
            object,
            params);
    }

    public static String convertKeywordCase(SQLCompletionRequest request, String replaceString, boolean isObject) {
        final int proposalCase = request.getContext().getInsertCase();
        switch (proposalCase) {
            case SQLCompletionContext.PROPOSAL_CASE_UPPER:
                replaceString = replaceString.toUpperCase();
                break;
            case SQLCompletionContext.PROPOSAL_CASE_LOWER:
                replaceString = replaceString.toLowerCase();
                break;
            default:
                // Do not convert case if we got it directly from object
                if (!isObject) {
                    SQLDialect dialect = request.getContext().getSyntaxManager().getDialect();
                    DBPKeywordType keywordType = dialect.getKeywordType(replaceString);
                    if (keywordType == DBPKeywordType.KEYWORD) {
                        replaceString = request.getContext().getSyntaxManager().getKeywordCase().transform(replaceString);
                    } else {
                        replaceString = dialect.storesUnquotedCase().transform(replaceString);
                    }
                }
                break;
        }
        return replaceString;
    }

    protected static SQLCompletionProposalBase createCompletionProposal(
        SQLCompletionRequest request,
        String replaceString,
        String displayString,
        DBPKeywordType proposalType,
        String description)
    {
        return request.getContext().createProposal(
            request,
            displayString,
            replaceString, // replacementString
            replaceString.length(), //cursorPosition the position of the cursor following the insert
            null, //image to display
            //new ContextInformation(null, displayString, displayString), //the context information associated with this proposal
            proposalType,
            description,
            null,
            Collections.emptyMap());
    }

}
