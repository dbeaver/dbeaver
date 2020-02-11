/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableParametrized;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.sql.parser.SQLWordPartDetector;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.text.TextUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Completion analyzer
 */
public class SQLCompletionAnalyzer implements DBRRunnableParametrized<DBRProgressMonitor> {
    private static final Log log = Log.getLog(SQLCompletionAnalyzer.class);

    private static final String ALL_COLUMNS_PATTERN = "*";
    private static final String MATCH_ANY_PATTERN = "%";

    private final SQLCompletionRequest request;
    private DBRProgressMonitor monitor;

    private final List<SQLCompletionProposalBase> proposals = new ArrayList<>();
    private boolean searchFinished = false;

    public SQLCompletionAnalyzer(SQLCompletionRequest request) {
        this.request = request;
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
        String prevKeyWord = request.getWordDetector().getPrevKeyWord();
        {
            if (!CommonUtils.isEmpty(prevKeyWord)) {
                if (syntaxManager.getDialect().isEntityQueryWord(prevKeyWord)) {
                    // TODO: its an ugly hack. Need a better way
                    if (SQLConstants.KEYWORD_DELETE.equals(prevKeyWord)) {
                        request.setQueryType(null);
                    } else if (SQLConstants.KEYWORD_INTO.equals(prevKeyWord) &&
                        !CommonUtils.isEmpty(request.getWordDetector().getPrevWords()) &&
                        ("(".equals(request.getWordDetector().getPrevDelimiter()) || ",".equals(wordDetector.getPrevDelimiter())))
                    {
                        request.setQueryType(SQLCompletionRequest.QueryType.COLUMN);
                    } else if (SQLConstants.KEYWORD_JOIN.equals(prevKeyWord)) {
                        request.setQueryType(SQLCompletionRequest.QueryType.JOIN);
                    } else {
                        request.setQueryType(SQLCompletionRequest.QueryType.TABLE);
                    }
                } else if (syntaxManager.getDialect().isAttributeQueryWord(prevKeyWord)) {
                    request.setQueryType(SQLCompletionRequest.QueryType.COLUMN);
                    if (!request.isSimpleMode() && CommonUtils.isEmpty(request.getWordPart()) && wordDetector.getPrevDelimiter().equals(SQLCompletionAnalyzer.ALL_COLUMNS_PATTERN)) {
                        wordDetector.moveToDelimiter();
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
        boolean emptyWord = wordPart.length() == 0;

        SQLCompletionRequest.QueryType queryType = request.getQueryType();
        if (queryType != null) {
            // Try to determine which object is queried (if wordPart is not empty)
            // or get list of root database objects
            if (emptyWord) {
                // Get root objects
                DBPObject rootObject = null;
                if (queryType == SQLCompletionRequest.QueryType.COLUMN && dataSource instanceof DBSObjectContainer) {
                    // Try to detect current table
                    rootObject = getTableFromAlias((DBSObjectContainer)dataSource, null);
                    if (rootObject instanceof DBSEntity && SQLConstants.KEYWORD_ON.equals(wordDetector.getPrevKeyWord())) {
                        // Join?
                        if (makeJoinColumnProposals((DBSObjectContainer)dataSource, (DBSEntity)rootObject)) {
                            return;
                        }
                    }
                } else if (dataSource instanceof DBSObjectContainer) {
                    // Try to get from active object
                    DBSObject selectedObject = DBUtils.getActiveInstanceObject(request.getContext().getExecutionContext());
                    if (selectedObject != null) {
                        makeProposalsFromChildren(selectedObject, null, false);
                        rootObject = DBUtils.getPublicObject(selectedObject.getParentObject());
                    } else {
                        rootObject = dataSource;
                    }
                }
                if (rootObject != null) {
                    makeProposalsFromChildren(rootObject, null, false);
                }
                if (queryType == SQLCompletionRequest.QueryType.JOIN && !proposals.isEmpty() && dataSource instanceof DBSObjectContainer) {
                    // Filter out non-joinable tables
                    DBSObject leftTable = getTableFromAlias((DBSObjectContainer) dataSource, null);
                    if (leftTable instanceof DBSEntity) {
                        filterNonJoinableProposals((DBSEntity)leftTable);
                    }
                }
            } else {
                DBSObject rootObject = null;
                if (queryType == SQLCompletionRequest.QueryType.COLUMN && dataSource instanceof DBSObjectContainer) {
                    // Part of column name
                    // Try to get from active object
                    DBSObjectContainer sc = (DBSObjectContainer) dataSource;
                    DBSObject selectedObject = DBUtils.getActiveInstanceObject(request.getContext().getExecutionContext());
                    if (selectedObject instanceof DBSObjectContainer) {
                        sc = (DBSObjectContainer)selectedObject;
                    }
                    int divPos = wordPart.lastIndexOf(syntaxManager.getStructSeparator());
                    String tableAlias = divPos == -1 ? null : wordPart.substring(0, divPos);
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
                        SQLDialect sqlDialect = request.getContext().getDataSource().getSQLDialect();
                        String[] allNames = SQLUtils.splitFullIdentifier(
                            tableAlias,
                            sqlDialect.getCatalogSeparator(),
                            sqlDialect.getIdentifierQuoteStrings(),
                            false);
                        rootObject = SQLSearchUtils.findObjectByFQN(monitor, sc, request.getContext().getExecutionContext(), Arrays.asList(allNames), !request.isSimpleMode(), request.getWordDetector());
                    }
                }
                if (rootObject != null) {
                    makeProposalsFromChildren(rootObject, wordPart, false);
                } else {
                    // Get root object or objects from active database (if any)
                    if (queryType != SQLCompletionRequest.QueryType.COLUMN && queryType != SQLCompletionRequest.QueryType.EXEC) {
                        makeDataSourceProposals();
                    }
                }
            }

            if (!request.isSimpleMode() &&
                (queryType ==  SQLCompletionRequest.QueryType.EXEC ||
                    (queryType == SQLCompletionRequest.QueryType.COLUMN && request.getContext().isSearchProcedures())) &&
                dataSource instanceof DBSObjectContainer)
            {
                // Add procedures/functions for column proposals
                DBSStructureAssistant structureAssistant = DBUtils.getAdapter(DBSStructureAssistant.class, dataSource);
                DBSObjectContainer sc = (DBSObjectContainer) dataSource;
                DBSObject selectedObject = DBUtils.getActiveInstanceObject(request.getContext().getExecutionContext());
                if (selectedObject instanceof DBSObjectContainer) {
                    sc = (DBSObjectContainer)selectedObject;
                }
                if (structureAssistant != null) {
                    makeProposalsFromAssistant(structureAssistant,
                        sc,
                        new DBSObjectType[] {RelationalObjectType.TYPE_PROCEDURE },
                        wordPart);
                }
            }
        } else {
            // Get list of sub-objects (filtered by wordPart)
            //makeDataSourceProposals();
        }

        if (!emptyWord) {
            makeProposalsFromQueryParts();
        }

        // Final filtering
        if (!searchFinished) {
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
                String delimiter = request.getWordDetector().getPrevDelimiter();
                if (!CommonUtils.isEmpty(wordDetector.getPrevWords()) && (CommonUtils.isEmpty(delimiter) || delimiter.endsWith(")"))) {
                    // last expression ends with space or with ")"
                    allowedKeywords = new HashSet<>();
                    allowedKeywords.add(SQLConstants.KEYWORD_FROM);
                    if (CommonUtils.isEmpty(request.getWordPart())) {
                        matchedKeywords = Arrays.asList(SQLConstants.KEYWORD_FROM);
                    }
                }
            } else if (sqlDialect.isEntityQueryWord(prevKeyWord)) {
                allowedKeywords = new HashSet<>();
                if (SQLConstants.KEYWORD_DELETE.equals(prevKeyWord)) {
                    allowedKeywords.add(SQLConstants.KEYWORD_FROM);
                } else {
                    allowedKeywords.add(SQLConstants.KEYWORD_WHERE);
                }
            }

            if (!CommonUtils.isEmpty(request.getWordPart())) {
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
                    if (request.getQueryType() == SQLCompletionRequest.QueryType.COLUMN && !(keywordType == DBPKeywordType.FUNCTION || keywordType == DBPKeywordType.KEYWORD)) {
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
                            keywordType,
                            null,
                            false,
                            null)
                    );
                }
            }
        }
        filterProposals(dataSource);
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
        DBSObject selectedObject = defaultInstance == null ? null : DBUtils.getActiveInstanceObject(request.getContext().getExecutionContext());
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
            Map<DBSObject, Map<Class, List<SQLCompletionProposalBase>>> containerMap = new HashMap<>();
            for (SQLCompletionProposalBase proposal : proposals) {
                DBSObject container = proposal.getObjectContainer();
                DBPNamedObject object = proposal.getObject();
                if (object == null) {
                    continue;
                }
                Map<Class, List<SQLCompletionProposalBase>> typeMap = containerMap.computeIfAbsent(container, k -> new HashMap<>());
                Class objectType = object instanceof DBSObjectReference ? ((DBSObjectReference) object).getObjectClass() : object.getClass();
                List<SQLCompletionProposalBase> list = typeMap.computeIfAbsent(objectType, k -> new ArrayList<>());
                list.add(proposal);
            }
            for (Map.Entry<DBSObject, Map<Class, List<SQLCompletionProposalBase>>> entry : containerMap.entrySet()) {
                for (Map.Entry<Class, List<SQLCompletionProposalBase>> typeEntry : entry.getValue().entrySet()) {
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
        String wordPart = request.getWordPart();
        // Find all aliases matching current word
        if (!CommonUtils.isEmpty(request.getActiveQuery().getText()) && !CommonUtils.isEmpty(wordPart)) {
            if (wordPart.indexOf(request.getContext().getSyntaxManager().getStructSeparator()) != -1 || wordPart.equals(ALL_COLUMNS_PATTERN)) {
                return;
            }
            SQLDialect sqlDialect = request.getContext().getDataSource().getSQLDialect();
            String tableNamePattern = getTableNamePattern(sqlDialect);
            String tableAliasPattern = getTableAliasPattern("(" + wordPart + "[a-z]*)", tableNamePattern);
            Pattern rp = Pattern.compile(tableAliasPattern);
            // Append trailing space to let alias regex match correctly
            Matcher matcher = rp.matcher(request.getActiveQuery().getText() + " ");
            while (matcher.find()) {
                String tableName = matcher.group(1);
                String tableAlias = matcher.group(2);
                if (tableAlias.equals(wordPart)) {
                    continue;
                }

                if (!hasProposal(proposals, tableName)) {
                    proposals.add(
                        0,
                        SQLCompletionAnalyzer.createCompletionProposal(
                            request,
                            tableName,
                            tableName,
                            DBPKeywordType.OTHER,
                            null,
                            false,
                            null)
                    );
                }
                if (!CommonUtils.isEmpty(tableAlias) && !hasProposal(proposals, tableAlias)) {
                    proposals.add(
                        0,
                        SQLCompletionAnalyzer.createCompletionProposal(
                            request,
                            tableAlias,
                            tableAlias,
                            DBPKeywordType.OTHER,
                            null,
                            false,
                            null)
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
            DBSObject rightTable = SQLSearchUtils.findObjectByFQN(monitor, sc, request.getContext().getExecutionContext(), Arrays.asList(allNames), !request.isSimpleMode(), request.getWordDetector());
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

    private void makeDataSourceProposals() throws DBException {
        DBPDataSource dataSource = request.getContext().getDataSource();
        final DBSObjectContainer rootContainer = DBUtils.getAdapter(DBSObjectContainer.class, dataSource);
        if (rootContainer == null) {
            return;
        }

        DBSObjectContainer sc = rootContainer;
        DBSObject childObject = sc;
        String[] tokens = request.getWordDetector().splitWordPart();

        // Detect selected object (container).
        // There could be multiple selected objects on different hierarchy levels (e.g. PG)
        DBSObjectContainer selectedContainers[];
        {
            DBSObject[] selectedObjects = DBUtils.getSelectedObjects(monitor, request.getContext().getExecutionContext());
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
            childObject = objectName == null ? null : sc.getChild(monitor, objectName);
            if (childObject == null && i == 0 && objectName != null) {
                for (int k = 0; k < selectedContainers.length; k++) {
                    if (selectedContainers[k] != null) {
                        // Probably it is from selected object, let's try it
                        childObject = selectedContainers[k].getChild(monitor, objectName);
                        if (childObject != null) {
                            sc = selectedContainers[k];
                            break;
                        }
                    }
                }
            }
            if (childObject == null) {
                if (i == 0) {
                    // Assume it's a table alias ?
                    childObject = getTableFromAlias(sc, token);
                    if (childObject == null && !request.isSimpleMode()) {
                        // Search using structure assistant
                        DBSStructureAssistant structureAssistant = DBUtils.getAdapter(DBSStructureAssistant.class, sc);
                        if (structureAssistant != null) {
                            Collection<DBSObjectReference> references = structureAssistant.findObjectsByMask(
                                monitor,
                                request.getContext().getExecutionContext(),
                                null,
                                structureAssistant.getAutoCompleteObjectTypes(),
                                request.getWordDetector().removeQuotes(token),
                                request.getWordDetector().isQuoted(token),
                                false, 2);
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
            makeProposalsFromChildren(childObject, null, false);
        } else {
            // Get matched children
            makeProposalsFromChildren(childObject, lastToken, false);
            if (tokens.length == 1) {
                // Get children from selected object
            }
            if (tokens.length == 1) {
                // Try in active object
                for (int k = 0; k < selectedContainers.length; k++) {
                    if (selectedContainers[k] != null && selectedContainers[k] != childObject) {
                        makeProposalsFromChildren(selectedContainers[k], lastToken, true);
                    }
                }

                if (proposals.isEmpty() && !request.isSimpleMode()) {
                    // At last - try to find child tables by pattern
                    DBSStructureAssistant structureAssistant = null;
                    for (DBSObject object = childObject; object != null; object =  object.getParentObject()) {
                        structureAssistant = DBUtils.getAdapter(DBSStructureAssistant.class, object);
                        if (structureAssistant != null) {
                            break;
                        }
                    }
                    if (structureAssistant != null) {
                        makeProposalsFromAssistant(structureAssistant, sc, null, lastToken);
                    }
                }
            }
        }
    }

    @Nullable
    private DBSObject getTableFromAlias(DBSObjectContainer sc, @Nullable String token)
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
        SQLScriptElement activeQuery = request.getActiveQuery();
        if (activeQuery == null) {
            return null;
        }

        final List<String> nameList = new ArrayList<>();
        SQLDialect sqlDialect = dataSource.getSQLDialect();
        {
            // Regex matching MUST be very fast.
            // Otherwise UI will freeze during SQL typing.
            // So let's make regex as simple as possible.
            // TODO: will be replaced by SQL preparse + structure analysis

//            String quote = quoteString == null ? SQLConstants.STR_QUOTE_DOUBLE :
//                SQLConstants.STR_QUOTE_DOUBLE.equals(quoteString) || SQLConstants.STR_QUOTE_APOS.equals(quoteString) ?
//                    quoteString :
//                    Pattern.quote(quoteString);
            String catalogSeparator = sqlDialect.getCatalogSeparator();
            while (token.endsWith(catalogSeparator)) {
                token = token.substring(0, token.length() -1);
            }

            // Use silly pattern with all possible characters
            // Valid regex for quote identifiers and FQ names is monstrous and very slow
            String tableNamePattern = getTableNamePattern(sqlDialect);
            String structNamePattern;
            if (CommonUtils.isEmpty(token)) {
                String kwList = "from|update|join|into";
                if (request.getQueryType() != SQLCompletionRequest.QueryType.COLUMN) {
                    kwList = kwList + "|,";
                }
                structNamePattern = "(?:" + kwList + ")\\s+" + tableNamePattern;
            } else {
                structNamePattern = getTableAliasPattern(token, tableNamePattern);
            }

            Pattern aliasPattern;
            try {
                aliasPattern = Pattern.compile(structNamePattern, Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException e) {
                // Bad pattern - seems to be a bad token
                return null;
            }
            // Append trailing space to let alias regex match correctly
            String testQuery = SQLUtils.stripComments(request.getContext().getSyntaxManager().getDialect(), activeQuery.getText()) + " ";
            Matcher matcher = aliasPattern.matcher(testQuery);
            while (matcher.find()) {
                if (!nameList.isEmpty() && matcher.start() > request.getDocumentOffset() - activeQuery.getOffset()) {
                    // Do not search after cursor
                    break;
                }
                nameList.clear();
                int groupCount = matcher.groupCount();
                for (int i = 1; i <= groupCount; i++) {
                    String group = matcher.group(i);
                    if (!CommonUtils.isEmpty(group)) {
                        String[][] quoteStrings = sqlDialect.getIdentifierQuoteStrings();

                        String[] allNames = SQLUtils.splitFullIdentifier(group, catalogSeparator, quoteStrings, false);
                        Collections.addAll(nameList, allNames);
                    }
                }
            }
        }

        return SQLSearchUtils.findObjectByFQN(monitor, sc, request.getContext().getExecutionContext(), nameList, !request.isSimpleMode(), request.getWordDetector());
    }

    private String getTableAliasPattern(String alias, String tableNamePattern) {
        return tableNamePattern + "\\s+(?:as\\s)?" + alias + "[\\s,]+";
    }

    private static String getTableNamePattern(SQLDialect sqlDialect) {
        String[][] quoteStrings = sqlDialect.getIdentifierQuoteStrings();
        StringBuilder quotes = new StringBuilder();
        if (quoteStrings != null) {
            for (String[] quotePair : quoteStrings) {
                if (quotes.indexOf(quotePair[0]) == -1) quotes.append('\\').append(quotePair[0]);
                if (quotes.indexOf(quotePair[1]) == -1) quotes.append('\\').append(quotePair[1]);
            }
        }
        // Use silly pattern with all possible characters
        // Valid regex for quote identifiers and FQ names is monstrous and very slow
        return "([\\p{L}0-9_$§#@\\.\\-" + quotes.toString() + "]+)";
    }

    private void makeProposalsFromChildren(DBPObject parent, @Nullable String startPart, boolean addFirst) throws DBException {
        if (request.getQueryType() == SQLCompletionRequest.QueryType.EXEC) {
            return;
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
        if (parent instanceof DBSObjectContainer) {
            children = ((DBSObjectContainer)parent).getChildren(monitor);
        } else if (parent instanceof DBSEntity) {
            children = ((DBSEntity)parent).getAttributes(monitor);
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
                    if (prevWord.length() > 0 && prevWord.charAt(prevWord.length() - 1) == request.getContext().getSyntaxManager().getStructSeparator()) {
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
                    makeProposalsFromChildren(child, startPart, addFirst);
                    continue;
                }
                if (allObjects) {
                    if (combinedMatch.length() > 0) {
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
            if (combinedMatch.length() > 0) {
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
                        matchedObjects.sort(DBUtils.nameComparatorIgnoreCase());
                    }
                } else {
                    matchedObjects.sort((o1, o2) -> {
                        int score1 = scoredMatches.get(o1.getName());
                        int score2 = scoredMatches.get(o2.getName());
                        if (score1 == score2) {
                            if (o1 instanceof DBSAttributeBase) {
                                return ((DBSAttributeBase) o1).getOrdinalPosition() - ((DBSAttributeBase) o2).getOrdinalPosition();
                            }
                            return o1.getName().compareToIgnoreCase(o2.getName());
                        }
                        return score2 - score1;
                    });
                }
                List<SQLCompletionProposalBase> childProposals = new ArrayList<>(matchedObjects.size());
                for (DBSObject child : matchedObjects) {
                    SQLCompletionProposalBase proposal = makeProposalsFromObject(child, !(parent instanceof DBPDataSource));
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

    private boolean objectNameMatches(@Nullable String startPart, DBSObject child, boolean matchContains) {
        String nameCI = child.getName().toUpperCase(Locale.ENGLISH);
        return !CommonUtils.isEmpty(startPart) && (matchContains ? nameCI.contains(startPart) : nameCI.startsWith(startPart));
    }

    private void makeProposalsFromAssistant(
        DBSStructureAssistant assistant,
        @Nullable DBSObjectContainer rootSC,
        DBSObjectType[] objectTypes,
        String objectName) throws DBException
    {
        Collection<DBSObjectReference> references = assistant.findObjectsByMask(
            monitor,
            request.getContext().getExecutionContext(),
            rootSC,
            objectTypes == null ? assistant.getAutoCompleteObjectTypes() : objectTypes,
            makeObjectNameMask(request.getWordDetector().removeQuotes(objectName)),
            request.getWordDetector().isQuoted(objectName),
            request.getContext().isSearchGlobally(), 100);
        for (DBSObjectReference reference : references) {
            proposals.add(makeProposalsFromObject(
                reference,
                !(rootSC instanceof DBPDataSource),
                reference.getObjectType().getImage()));
        }
    }

    private String makeObjectNameMask(String objectName) {
        if (request.getContext().isSearchInsideNames()) {
            return MATCH_ANY_PATTERN + objectName + MATCH_ANY_PATTERN;
        } else {
            return objectName + MATCH_ANY_PATTERN;
        }
    }

    private SQLCompletionProposalBase makeProposalsFromObject(DBSObject object, boolean useShortName)
    {
        DBNNode node = DBNUtils.getNodeByObject(monitor, object, false);

        DBPImage objectIcon = node == null ? null : node.getNodeIconDefault();
        if (objectIcon == null) {
            objectIcon = DBValueFormatting.getObjectImage(object);
        }
        return makeProposalsFromObject(object, useShortName, objectIcon);
    }

    private SQLCompletionProposalBase makeProposalsFromObject(
        DBPNamedObject object,
        boolean useShortName,
        @Nullable DBPImage objectIcon)
    {
        String alias = null;
        if (SQLConstants.KEYWORD_FROM.equals(request.getWordDetector().getPrevKeyWord())) {
            if (object instanceof DBSEntity && ((DBSEntity) object).getDataSource().getContainer().getPreferenceStore().getBoolean(SQLModelPreferences.SQL_PROPOSAL_INSERT_TABLE_ALIAS)) {
                SQLDialect dialect = SQLUtils.getDialectFromObject(object);
                if (dialect.supportsAliasInSelect()) {
                    String firstKeyword = SQLUtils.getFirstKeyword(dialect, request.getActiveQuery().getText());
                    if (dialect.supportsAliasInUpdate() || !ArrayUtils.contains(dialect.getDMLKeywords(), firstKeyword.toUpperCase(Locale.ENGLISH))) {
                        String queryText = request.getActiveQuery().getText();
                        Set<String> aliases = new LinkedHashSet<>();
                        if (request.getActiveQuery() instanceof SQLQuery) {
                            Statement sqlStatement = ((SQLQuery) request.getActiveQuery()).getStatement();
                            if (sqlStatement != null) {
                                TablesNamesFinder namesFinder = new TablesNamesFinder() {
                                    public void visit(Table table) {
                                        if (table != null && table.getAlias() != null && table.getAlias().getName() != null) {
                                            aliases.add(table.getAlias().getName().toLowerCase(Locale.ENGLISH));
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
                            return Pattern.compile("\\s+" + s + "[^\\w]+").matcher(queryText).find();
                        });
                    }
                }
            }
        }
        String objectName = useShortName ?
            object.getName() :
            DBUtils.getObjectFullName(object, DBPEvaluationContext.DML);

        boolean isSingleObject = true;
        String replaceString = null;
        DBPDataSource dataSource = request.getContext().getDataSource();
        if (dataSource != null) {
            // If we replace short name with referenced object
            // and current active schema (catalog) is not this object's container then
            // replace with full qualified name
            if (!request.getContext().isUseShortNames() && object instanceof DBSObjectReference) {
                if (request.getWordDetector().getFullWord().indexOf(request.getContext().getSyntaxManager().getStructSeparator()) == -1) {
                    DBSObjectReference structObject = (DBSObjectReference) object;
                    if (structObject.getContainer() != null) {
                        DBSObject selectedObject = DBUtils.getActiveInstanceObject(request.getContext().getExecutionContext());
                        if (selectedObject != structObject.getContainer()) {
                            replaceString = structObject.getFullyQualifiedName(DBPEvaluationContext.DML);
                            isSingleObject = false;
                        }
                    }
                }
            }
            if (replaceString == null) {
                if (request.getContext().isUseFQNames() && object instanceof DBPQualifiedObject) {
                    replaceString = ((DBPQualifiedObject)object).getFullyQualifiedName(DBPEvaluationContext.DML);
                } else {
                    replaceString = DBUtils.getQuotedIdentifier(dataSource, object.getName());
                }
            }
        } else {
            replaceString = DBUtils.getObjectShortName(object);
        }
        if (!CommonUtils.isEmpty(alias)) {
            replaceString += " " + /*convertKeywordCase(request, "as", false) + " " + */alias;
        }
        return createCompletionProposal(
            request,
            replaceString,
            objectName,
            DBPKeywordType.OTHER,
            objectIcon,
            isSingleObject,
            object);
    }

    /*
        * Turns the vector into an Array of ICompletionProposal objects
        */
    static SQLCompletionProposalBase createCompletionProposal(
        SQLCompletionRequest request,
        String replaceString,
        String displayString,
        DBPKeywordType proposalType,
        @Nullable DBPImage image,
        boolean isObject,
        @Nullable DBPNamedObject object)
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
        if (!quotedString) {
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
            object);
    }

    private static String convertKeywordCase(SQLCompletionRequest request, String replaceString, boolean isObject) {
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
            null);
    }

}
