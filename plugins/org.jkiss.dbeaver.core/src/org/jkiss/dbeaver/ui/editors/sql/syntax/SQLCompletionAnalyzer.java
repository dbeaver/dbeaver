/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.eclipse.swt.graphics.Image;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableParametrized;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.TextUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Completion utils
 */
class SQLCompletionAnalyzer implements DBRRunnableParametrized<DBRProgressMonitor> {
    private static final Log log = Log.getLog(SQLCompletionAnalyzer.class);

    private static final String MATCH_ANY_PATTERN = "%";

    static class CompletionRequest {
        final SQLEditorBase editor;
        final boolean simpleMode;
        int documentOffset;
        String activeQuery = null;

        SQLWordPartDetector wordDetector;
        String wordPart;
        SQLCompletionProcessor.QueryType queryType;

        final List<SQLCompletionProposal> proposals = new ArrayList<>();
        boolean searchFinished = false;

        CompletionRequest(SQLEditorBase editor, int documentOffset, boolean simpleMode) {
            this.editor = editor;
            this.documentOffset = documentOffset;
            this.simpleMode = simpleMode;
        }
    }

    private final CompletionRequest request;
    private DBRProgressMonitor monitor;

    SQLCompletionAnalyzer(CompletionRequest request) {
        this.request = request;
    }

    @Override
    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        try {
            this.monitor = monitor;
            runAnalyzer();
        } catch (DBException e) {
            throw new InvocationTargetException(e);
        }
    }


    void runAnalyzer() throws DBException {
        DBPDataSource dataSource = request.editor.getDataSource();
        if (dataSource == null) {
            return;
        }
        boolean emptyWord = request.wordPart.length() == 0;
        if (request.queryType != null) {
            // Try to determine which object is queried (if wordPart is not empty)
            // or get list of root database objects
            if (emptyWord) {
                // Get root objects
                DBPObject rootObject = null;
                if (request.queryType == SQLCompletionProcessor.QueryType.COLUMN && dataSource instanceof DBSObjectContainer) {
                    // Try to detect current table
                    rootObject = getTableFromAlias((DBSObjectContainer)dataSource, null);
                    if (rootObject instanceof DBSEntity && SQLConstants.KEYWORD_ON.equals(request.wordDetector.getPrevKeyWord())) {
                        // Join?
                        if (makeJoinColumnProposals((DBSObjectContainer)dataSource, (DBSEntity)rootObject)) {
                            return;
                        }
                    }
                } else if (dataSource instanceof DBSObjectContainer) {
                    // Try to get from active object
                    DBSObject selectedObject = DBUtils.getActiveInstanceObject(dataSource.getDefaultInstance());
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
                if (request.queryType == SQLCompletionProcessor.QueryType.JOIN && !request.proposals.isEmpty() && dataSource instanceof DBSObjectContainer) {
                    // Filter out non-joinable tables
                    DBSObject leftTable = getTableFromAlias((DBSObjectContainer) dataSource, null);
                    if (leftTable instanceof DBSEntity) {
                        filterNonJoinableProposals((DBSEntity)leftTable);
                    }
                }
            } else {
                DBSObject rootObject = null;
                if (request.queryType == SQLCompletionProcessor.QueryType.COLUMN && dataSource instanceof DBSObjectContainer) {
                    // Part of column name
                    // Try to get from active object
                    DBSObjectContainer sc = (DBSObjectContainer) dataSource;
                    DBSObject selectedObject = DBUtils.getActiveInstanceObject(dataSource.getDefaultInstance());
                    if (selectedObject instanceof DBSObjectContainer) {
                        sc = (DBSObjectContainer)selectedObject;
                    }
                    int divPos = request.wordPart.indexOf(request.editor.getSyntaxManager().getStructSeparator());
                    String tableAlias = divPos == -1 ? null : request.wordPart.substring(0, divPos);
                    if (tableAlias == null && !CommonUtils.isEmpty(request.wordPart)) {
                        // May be an incomplete table alias. Try to find such table
                        rootObject = getTableFromAlias(sc, request.wordPart);
                        if (rootObject != null) {
                            // Found alias - no proposals
                            request.searchFinished = true;
                            return;
                        }
                    }
                    rootObject = getTableFromAlias(sc, tableAlias);
                }
                if (rootObject != null) {
                    makeProposalsFromChildren(rootObject, request.wordPart, false);
                } else {
                    // Get root object or objects from active database (if any)
                    makeDataSourceProposals();
                }
            }

            if (!request.simpleMode &&
                (request.queryType ==  SQLCompletionProcessor.QueryType.EXEC ||
                    (request.queryType == SQLCompletionProcessor.QueryType.COLUMN && dataSource.getContainer().getPreferenceStore().getBoolean(SQLPreferenceConstants.SHOW_COLUMN_PROCEDURES))) &&
                dataSource instanceof DBSObjectContainer)
            {
                // Add procedures/functions for column proposals
                DBSStructureAssistant structureAssistant = DBUtils.getAdapter(DBSStructureAssistant.class, dataSource);
                DBSObjectContainer sc = (DBSObjectContainer) dataSource;
                DBSObject selectedObject = DBUtils.getActiveInstanceObject(dataSource.getDefaultInstance());
                if (selectedObject instanceof DBSObjectContainer) {
                    sc = (DBSObjectContainer)selectedObject;
                }
                if (structureAssistant != null) {
                    makeProposalsFromAssistant(dataSource, structureAssistant,
                        sc,
                        new DBSObjectType[] {RelationalObjectType.TYPE_PROCEDURE },
                        request.wordPart);
                }
            }
        } else {
            // Get list of sub-objects (filtered by wordPart)
            //makeDataSourceProposals();
        }
    }

    private boolean makeJoinColumnProposals(DBSObjectContainer sc, DBSEntity leftTable) {
        SQLWordPartDetector joinTableDetector = new SQLWordPartDetector(
            request.editor.getDocument(),
            request.editor.getSyntaxManager(),
            request.wordDetector.getStartOffset(),
            2);
        List<String> prevWords = joinTableDetector.getPrevWords();

        if (!CommonUtils.isEmpty(prevWords)) {
            DBPDataSource dataSource = request.editor.getDataSource();
            SQLDialect sqlDialect = SQLUtils.getDialectFromDataSource(dataSource);
            String rightTableName = prevWords.get(0);
            String[] allNames = SQLUtils.splitFullIdentifier(
                rightTableName,
                sqlDialect.getCatalogSeparator(),
                sqlDialect.getIdentifierQuoteStrings(),
                false);
            DBSObject rightTable = SQLSearchUtils.findObjectByFQN(monitor, sc, dataSource, Arrays.asList(allNames), !request.simpleMode, request.wordDetector);
            if (rightTable instanceof DBSEntity) {
                try {
                    String joinCriteria = SQLUtils.generateTableJoin(monitor, leftTable, DBUtils.getQuotedIdentifier(leftTable), (DBSEntity) rightTable, DBUtils.getQuotedIdentifier(rightTable));
                    request.proposals.add(createCompletionProposal(request, joinCriteria, joinCriteria, DBPKeywordType.OTHER, "Join condition"));
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
        List<SQLCompletionProposal> proposals = request.proposals;
        List<SQLCompletionProposal> joinableProposals = new ArrayList<>();
        for (SQLCompletionProposal proposal : proposals) {
            if (proposal.getObject() instanceof DBSEntity) {
                DBSEntity rightTable = (DBSEntity) proposal.getObject();
                if (tableHaveJoins(rightTable, leftTable) || tableHaveJoins(leftTable, rightTable)) {
                    proposal.setReplacementAfter(" ON");
                    joinableProposals.add(proposal);
                }
            }
        }
        if (!joinableProposals.isEmpty()) {
            request.proposals.clear();
            request.proposals.addAll(joinableProposals);
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
        DBPDataSource dataSource = request.editor.getDataSource();
        final DBSObjectContainer rootContainer = DBUtils.getAdapter(DBSObjectContainer.class, dataSource);
        if (rootContainer == null) {
            return;
        }

        DBSObjectContainer sc = rootContainer;
        DBSObject childObject = sc;
        String[] tokens = request.wordDetector.splitWordPart();

        // Detect selected object (container).
        // There could be multiple selected objects on different hierarchy levels (e.g. PG)
        DBSObjectContainer selectedContainers[];
        {
            DBSObject[] selectedObjects = DBUtils.getSelectedObjects(dataSource);
            selectedContainers = new DBSObjectContainer[selectedObjects.length];
            for (int i = 0; i < selectedObjects.length; i++) {
                selectedContainers[i] = DBUtils.getAdapter(DBSObjectContainer.class, selectedObjects[i]);
            }
        }

        String lastToken = null;
        for (int i = 0; i < tokens.length; i++) {
            final String token = tokens[i];
            if (i == tokens.length - 1 && !request.wordDetector.getWordPart().endsWith(".")) {
                lastToken = token;
                break;
            }
            if (sc == null) {
                break;
            }
            // Get next structure container
            final String objectName =
                request.wordDetector.isQuoted(token) ? request.wordDetector.removeQuotes(token) :
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
                    if (childObject == null && !request.simpleMode) {
                        // Search using structure assistant
                        DBSStructureAssistant structureAssistant = DBUtils.getAdapter(DBSStructureAssistant.class, sc);
                        if (structureAssistant != null) {
                            Collection<DBSObjectReference> references = structureAssistant.findObjectsByMask(
                                monitor,
                                null,
                                structureAssistant.getAutoCompleteObjectTypes(),
                                request.wordDetector.removeQuotes(token),
                                request.wordDetector.isQuoted(token),
                                false,
                                2);
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

                if (request.proposals.isEmpty() && !request.simpleMode) {
                    // At last - try to find child tables by pattern
                    DBSStructureAssistant structureAssistant = null;
                    for (DBSObject object = childObject; object != null; object =  object.getParentObject()) {
                        structureAssistant = DBUtils.getAdapter(DBSStructureAssistant.class, object);
                        if (structureAssistant != null) {
                            break;
                        }
                    }
                    if (structureAssistant != null) {
                        makeProposalsFromAssistant(dataSource, structureAssistant, sc, null, lastToken);
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
        }

        final DBPDataSource dataSource = request.editor.getDataSource();
        if (!(dataSource instanceof SQLDataSource)) {
            return null;
        }
        if (request.activeQuery == null) {
            final SQLScriptElement queryAtPos = request.editor.extractQueryAtPos(request.documentOffset);
            if (queryAtPos != null) {
                request.activeQuery = queryAtPos.getText() + " ";
            }
        }
        if (request.activeQuery == null) {
            return null;
        }

        final List<String> nameList = new ArrayList<>();
        SQLDialect sqlDialect = SQLUtils.getDialectFromDataSource(dataSource);
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
            String tableNamePattern = "([\\p{L}0-9_$§#@\\.\\-" + quotes.toString() + "]+)";
            String structNamePattern;
            if (CommonUtils.isEmpty(token)) {
                structNamePattern = "(?:from|update|join|into)\\s*" + tableNamePattern;
            } else {
                structNamePattern = tableNamePattern + "\\s+(?:as\\s)?" + token + "[\\s,]+";
            }

            Pattern aliasPattern;
            try {
                aliasPattern = Pattern.compile(structNamePattern, Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException e) {
                // Bad pattern - seems to be a bad token
                return null;
            }
            String testQuery = SQLUtils.stripComments(request.editor.getSyntaxManager().getDialect(), request.activeQuery);
            Matcher matcher = aliasPattern.matcher(testQuery);
            if (matcher.find()) {
                int groupCount = matcher.groupCount();
                for (int i = 1; i <= groupCount; i++) {
                    String group = matcher.group(i);
                    if (!CommonUtils.isEmpty(group)) {
                        String[] allNames = SQLUtils.splitFullIdentifier(group, catalogSeparator, quoteStrings, false);
                        Collections.addAll(nameList, allNames);
                    }
                }
            }
        }

        return SQLSearchUtils.findObjectByFQN(monitor, sc, dataSource, nameList, true, request.wordDetector);
    }

    private void makeProposalsFromChildren(DBPObject parent, @Nullable String startPart, boolean addFirst) throws DBException {
        if (startPart != null) {
            startPart = request.wordDetector.removeQuotes(startPart).toUpperCase(Locale.ENGLISH);
            int divPos = startPart.lastIndexOf(request.editor.getSyntaxManager().getStructSeparator());
            if (divPos != -1) {
                startPart = startPart.substring(divPos + 1);
            }
        }

        DBPDataSource dataSource = request.editor.getDataSource();
        boolean matchContains = dataSource != null && dataSource.getContainer().getPreferenceStore().getBoolean(SQLPreferenceConstants.PROPOSALS_MATCH_CONTAINS);
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
            boolean simpleMode = request.simpleMode;
            boolean allObjects = !simpleMode && SQLCompletionProcessor.ALL_COLUMNS_PATTERN.equals(startPart);
            String objPrefix = null;
            if (allObjects) {
                if (!CommonUtils.isEmpty(request.wordDetector.getPrevWords())) {
                    String prevWord = request.wordDetector.getPrevWords().get(0);
                    if (prevWord.length() > 0 && prevWord.charAt(prevWord.length() - 1) == request.editor.getSyntaxManager().getStructSeparator()) {
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
                if (allObjects) {
                    if (combinedMatch.length() > 0) {
                        combinedMatch.append(", ");
                        if (objPrefix != null) combinedMatch.append(objPrefix);
                    }
                    combinedMatch.append(DBUtils.getQuotedIdentifier(child));
                } else {
                    int score = CommonUtils.isEmpty(startPart) ? 1 : TextUtils.fuzzyScore(child.getName(), startPart);
                    if (score > 0) {
                        matchedObjects.add(child);
                        scoredMatches.put(child.getName(), score);
                    }
                }
            }
            if (combinedMatch.length() > 0) {
                String replaceString = combinedMatch.toString();

                request.proposals.add(createCompletionProposal(
                    request,
                    replaceString,
                    replaceString,
                    DBPKeywordType.OTHER,
                    "All objects"));
            } else if (!matchedObjects.isEmpty()) {
                if (startPart == null) {
                    matchedObjects.sort(DBUtils.nameComparatorIgnoreCase());
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
                List<SQLCompletionProposal> childProposals = new ArrayList<>(matchedObjects.size());
                for (DBSObject child : matchedObjects) {
                    SQLCompletionProposal proposal = makeProposalsFromObject(child, !(parent instanceof DBPDataSource));
                    if (!scoredMatches.isEmpty()) {
                        int proposalScore = scoredMatches.get(child.getName());
                        proposal.setProposalScore(proposalScore);
                    }

                    childProposals.add(proposal);
                }
                if (addFirst) {
                    // Add proposals in the beginning (because the most strict identifiers have to be first)
                    request.proposals.addAll(0, childProposals);
                } else {
                    request.proposals.addAll(childProposals);
                }
            }
        }
    }

    private boolean objectNameMatches(@Nullable String startPart, DBSObject child, boolean matchContains) {
        String nameCI = child.getName().toUpperCase(Locale.ENGLISH);
        return matchContains ? nameCI.contains(startPart) : nameCI.startsWith(startPart);
    }

    private void makeProposalsFromAssistant(
            DBPDataSource dataSource,
            DBSStructureAssistant assistant,
            @Nullable DBSObjectContainer rootSC,
            DBSObjectType[] objectTypes,
            String objectName) throws DBException
    {
        Collection<DBSObjectReference> references = assistant.findObjectsByMask(
            monitor,
            rootSC,
            objectTypes == null ? assistant.getAutoCompleteObjectTypes() : objectTypes,
            makeObjectNameMask(dataSource, request.wordDetector.removeQuotes(objectName)),
            request.wordDetector.isQuoted(objectName),
            dataSource.getContainer().getPreferenceStore().getBoolean(SQLPreferenceConstants.USE_GLOBAL_ASSISTANT),
            100);
        for (DBSObjectReference reference : references) {
            request.proposals.add(makeProposalsFromObject(
                reference,
                !(rootSC instanceof DBPDataSource),
                reference.getObjectType().getImage()));
        }
    }

    private String makeObjectNameMask(DBPDataSource dataSource, String objectName) {
        if (dataSource.getContainer().getPreferenceStore().getBoolean(SQLPreferenceConstants.PROPOSALS_MATCH_CONTAINS)) {
            return MATCH_ANY_PATTERN + objectName + MATCH_ANY_PATTERN;
        } else {
            return objectName + MATCH_ANY_PATTERN;
        }
    }

    private SQLCompletionProposal makeProposalsFromObject(DBSObject object, boolean useShortName)
    {
        DBNNode node = NavigatorUtils.getNodeByObject(monitor, object, false);
        return makeProposalsFromObject(object, useShortName, node == null ? null : node.getNodeIconDefault());
    }

    private SQLCompletionProposal makeProposalsFromObject(
        DBPNamedObject object,
        boolean useShortName,
        @Nullable DBPImage objectIcon)
    {
        String objectName = useShortName ?
            object.getName() :
            DBUtils.getObjectFullName(object, DBPEvaluationContext.DML);

        boolean isSingleObject = true;
        String replaceString = null;
        DBPDataSource dataSource = request.editor.getDataSource();
        if (dataSource != null) {
            DBPPreferenceStore prefs = request.editor.getActivePreferenceStore();

            // If we replace short name with referenced object
            // and current active schema (catalog) is not this object's container then
            // replace with full qualified name
            if (!prefs.getBoolean(SQLPreferenceConstants.PROPOSAL_SHORT_NAME) && object instanceof DBSObjectReference) {
                if (request.wordDetector.getFullWord().indexOf(request.editor.getSyntaxManager().getStructSeparator()) == -1) {
                    DBSObjectReference structObject = (DBSObjectReference) object;
                    if (structObject.getContainer() != null) {
                        DBSObject selectedObject = DBUtils.getActiveInstanceObject(dataSource.getDefaultInstance());
                        if (selectedObject != structObject.getContainer()) {
                            replaceString = DBUtils.getFullQualifiedName(
                                dataSource,
                                structObject.getContainer() instanceof DBPDataSource ? null : structObject.getContainer(),
                                object);
                            isSingleObject = false;
                        }
                    }
                }
            }
            if (replaceString == null) {
                if (prefs.getBoolean(SQLPreferenceConstants.PROPOSAL_ALWAYS_FQ) && object instanceof DBPQualifiedObject) {
                    replaceString = ((DBPQualifiedObject)object).getFullyQualifiedName(DBPEvaluationContext.DML);
                } else {
                    replaceString = DBUtils.getQuotedIdentifier(dataSource, object.getName());
                }
            }
        } else {
            replaceString = DBUtils.getObjectShortName(object);
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
    static SQLCompletionProposal createCompletionProposal(
        CompletionRequest request,
        String replaceString,
        String displayString,
        DBPKeywordType proposalType,
        @Nullable DBPImage image,
        boolean isObject,
        @Nullable DBPNamedObject object)
    {
        SQLEditorBase editor = request.editor;
        DBPPreferenceStore store = editor.getActivePreferenceStore();
        DBPDataSource dataSource = editor.getDataSource();
        if (dataSource != null) {
            if (isObject) {
                // Escape replace string if required
                // FIXME: do not escape! it may (will) escape identifiers twice
                //replaceString = DBUtils.getQuotedIdentifier(dataSource, replaceString);
            }
        }

        // If we have quoted string then ignore pref settings
        boolean quotedString = request.wordDetector.isQuoted(replaceString);
        if (!quotedString) {
            final int proposalCase = store.getInt(SQLPreferenceConstants.PROPOSAL_INSERT_CASE);
            switch (proposalCase) {
                case SQLPreferenceConstants.PROPOSAL_CASE_UPPER:
                    replaceString = replaceString.toUpperCase();
                    break;
                case SQLPreferenceConstants.PROPOSAL_CASE_LOWER:
                    replaceString = replaceString.toLowerCase();
                    break;
                default:
                    // Do not convert case if we got it directly from object
                    if (!isObject) {
                        SQLDialect dialect = editor.getSyntaxManager().getDialect();
                        DBPKeywordType keywordType = dialect.getKeywordType(replaceString);
                        if (keywordType == DBPKeywordType.KEYWORD) {
                            replaceString = editor.getSyntaxManager().getKeywordCase().transform(replaceString);
                        } else {
                            replaceString = dialect.storesUnquotedCase().transform(replaceString);
                        }
                    }
                    break;
            }
        }

        Image img = image == null ? null : DBeaverIcons.getImage(image);
        return new SQLCompletionProposal(
            request,
            displayString,
            replaceString, // replacementString
            replaceString.length(), //cursorPosition the position of the cursor following the insert
                                // relative to replacementOffset
            img, //image to display
            null,//new ContextInformation(img, displayString, displayString), //the context information associated with this proposal
            proposalType,
            null,
            object);
    }

    protected static SQLCompletionProposal createCompletionProposal(
        CompletionRequest request,
        String replaceString,
        String displayString,
        DBPKeywordType proposalType,
        String description)
    {
        return new SQLCompletionProposal(
            request,
            displayString,
            replaceString, // replacementString
            replaceString.length(), //cursorPosition the position of the cursor following the insert
            null, //image to display
            null,//new ContextInformation(null, displayString, displayString), //the context information associated with this proposal
            proposalType,
            description,
            null);
    }

}