/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.*;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.editors.sql.templates.SQLContext;
import org.jkiss.dbeaver.ui.editors.sql.templates.SQLTemplateCompletionProposal;
import org.jkiss.dbeaver.ui.editors.sql.templates.SQLTemplatesRegistry;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The SQL content assist processor. This content assist processor proposes text
 * completions and computes context information for a SQL content type.
 */
public class SQLCompletionProcessor implements IContentAssistProcessor
{
    private static final Log log = Log.getLog(SQLCompletionProcessor.class);

    private enum QueryType {
        TABLE,
        COLUMN
    }

    private SQLEditorBase editor;
    private IContextInformationValidator validator = new Validator();
    private int documentOffset;
    private String activeQuery = null;
    private SQLWordPartDetector wordDetector;
    private static boolean lookupTemplates = false;

    public SQLCompletionProcessor(SQLEditorBase editor)
    {
        this.editor = editor;
    }

    public static boolean isLookupTemplates() {
        return lookupTemplates;
    }

    public static void setLookupTemplates(boolean lookupTemplates) {
        SQLCompletionProcessor.lookupTemplates = lookupTemplates;
    }

    @Override
    public ICompletionProposal[] computeCompletionProposals(
        ITextViewer viewer,
        int documentOffset)
    {
        this.documentOffset = documentOffset;
        this.activeQuery = null;

        this.wordDetector = new SQLWordPartDetector(viewer.getDocument(), editor.getSyntaxManager(), documentOffset);
        final String wordPart = wordDetector.getWordPart();

        if (lookupTemplates) {
            return makeTemplateProposals(viewer, documentOffset, wordPart);
        }

        final List<SQLCompletionProposal> proposals = new ArrayList<>();
        QueryType queryType = null;
        {
            final String prevKeyWord = wordDetector.getPrevKeyWord();
            if (!CommonUtils.isEmpty(prevKeyWord)) {
                if (editor.getSyntaxManager().getDialect().isEntityQueryWord(prevKeyWord)) {
                    queryType = QueryType.TABLE;
                } else if (editor.getSyntaxManager().getDialect().isAttributeQueryWord(prevKeyWord)) {
                    queryType = QueryType.COLUMN;
                }
            }
        }
        if (queryType != null && wordPart != null) {
            if (editor.getDataSource() != null) {
                ProposalSearchJob searchJob = new ProposalSearchJob(proposals, wordPart, queryType);
                searchJob.schedule();

                // Wait until job finished
                Display display = Display.getCurrent();
                while (!searchJob.finished) {
                    if (!display.readAndDispatch()) {
                        display.sleep();
                    }
                }
                display.update();
            }
        }

        if (proposals.isEmpty() || !CommonUtils.isEmpty(wordPart))  {
            // Keyword assist
            List<String> matchedKeywords = editor.getSyntaxManager().getDialect().getMatchedKeywords(wordPart);
            for (String keyWord : matchedKeywords) {
                DBPKeywordType keywordType = editor.getSyntaxManager().getDialect().getKeywordType(keyWord);
                if (keywordType != null) {
                    proposals.add(
                        createCompletionProposal(
                            keyWord,
                            keyWord,
                            keyWord + " (" + keywordType.name() + ")",
                            null,
                            false,
                            null)
                    );
                }
            }
        }

        // Remove duplications
        for (int i = 0; i < proposals.size(); i++) {
            SQLCompletionProposal proposal = proposals.get(i);
            for (int j = i + 1; j < proposals.size(); ) {
                SQLCompletionProposal proposal2 = proposals.get(j);
                if (proposal.getDisplayString().equals(proposal2.getDisplayString())) {
                    proposals.remove(j);
                } else {
                    j++;
                }
            }
        }
        DBSObject selectedObject = getDefaultObject(editor.getDataSource());
        boolean hideDups = getPreferences().getBoolean(SQLPreferenceConstants.HIDE_DUPLICATE_PROPOSALS) && selectedObject != null;
        if (hideDups) {
            for (int i = 0; i < proposals.size(); i++) {
                SQLCompletionProposal proposal = proposals.get(i);
                for (int j = 0; j < proposals.size(); ) {
                    SQLCompletionProposal proposal2 = proposals.get(j);
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
                //List<ICompletionProposal>
            }

        }
        return proposals.toArray(new ICompletionProposal[proposals.size()]);
    }

    @NotNull
    private ICompletionProposal[] makeTemplateProposals(ITextViewer viewer, int documentOffset, String wordPart) {
        wordPart = wordPart.toLowerCase();
        final List<SQLTemplateCompletionProposal> templateProposals = new ArrayList<>();
        // Templates
        for (Template template : editor.getTemplatesPage().getTemplateStore().getTemplates()) {
            if (template.getName().toLowerCase().startsWith(wordPart)) {
                templateProposals.add(new SQLTemplateCompletionProposal(
                    template,
                    new SQLContext(
                        SQLTemplatesRegistry.getInstance().getTemplateContextRegistry().getContextType(template.getContextTypeId()),
                        viewer.getDocument(),
                        new Position(wordDetector.getStartOffset(), wordDetector.getLength()),
                        editor),
                    new Region(documentOffset, 0),
                    null));
            }
        }
        Collections.sort(templateProposals, new Comparator<SQLTemplateCompletionProposal>() {
            @Override
            public int compare(SQLTemplateCompletionProposal o1, SQLTemplateCompletionProposal o2) {
                return o1.getDisplayString().compareTo(o2.getDisplayString());
            }
        });
        return templateProposals.toArray(new ICompletionProposal[templateProposals.size()]);
    }

    private void makeStructureProposals(
        final DBRProgressMonitor monitor,
        final List<SQLCompletionProposal> proposals,
        final String wordPart,
        final QueryType queryType)
    {
        DBPDataSource dataSource = editor.getDataSource();
        if (dataSource == null) {
            return;
        }
        if (queryType != null) {
            // Try to determine which object is queried (if wordPart is not empty)
            // or get list of root database objects
            if (wordPart.length() == 0) {
                // Get root objects
                DBSObject rootObject = null;
                if (queryType == QueryType.COLUMN && dataSource instanceof DBSObjectContainer) {
                    // Try to detect current table
                    rootObject = getTableFromAlias(monitor, (DBSObjectContainer)dataSource, null);
                } else if (dataSource instanceof DBSObjectContainer) {
                    // Try to get from active object
                    DBSObject selectedObject = getDefaultObject(dataSource);
                    if (selectedObject != null) {
                        makeProposalsFromChildren(monitor, selectedObject, null, proposals);
                    }
                    rootObject = dataSource;
                }
                if (rootObject != null) {
                    makeProposalsFromChildren(monitor, rootObject, null, proposals);
                }
            } else {
                DBSObject rootObject = null;
                if (queryType == QueryType.COLUMN && dataSource instanceof DBSObjectContainer) {
                    // Part of column name
                    // Try to get from active object
                    DBSObjectContainer sc = (DBSObjectContainer) dataSource;
                    DBSObject selectedObject = getDefaultObject(dataSource);
                    if (selectedObject instanceof DBSObjectContainer) {
                        sc = (DBSObjectContainer)selectedObject;
                    }
                    int divPos = wordPart.indexOf(editor.getSyntaxManager().getStructSeparator());
                    String tableAlias = divPos == -1 ? null : wordPart.substring(0, divPos);
                    rootObject = getTableFromAlias(monitor, sc, tableAlias);
                }
                if (rootObject != null) {
                    makeProposalsFromChildren(monitor, rootObject, wordPart, proposals);
                } else {
                    // Get root object or objects from active database (if any)
                    makeStructureProposals(monitor, dataSource, proposals);
                }
            }
        } else {
            // Get list of sub-objects (filtered by wordPart)
            makeStructureProposals(monitor, dataSource, proposals);
        }
    }

    private void makeStructureProposals(
        DBRProgressMonitor monitor,
        DBPDataSource dataSource,
        List<SQLCompletionProposal> proposals)
    {
        final DBSObjectContainer rootContainer = DBUtils.getAdapter(DBSObjectContainer.class, dataSource);
        if (rootContainer == null) {
            return;
        }
        DBSObjectContainer selectedContainer = null;
        {
            DBSObject selectedObject = getDefaultObject(dataSource);
            if (selectedObject != null) {
                selectedContainer = DBUtils.getAdapter(DBSObjectContainer.class, selectedObject);
            }
        }

        DBSObjectContainer sc = rootContainer;
        DBSObject childObject = sc;
        List<String> tokens = wordDetector.splitWordPart();

        String lastToken = null;
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (i == tokens.size() - 1 && !wordDetector.getWordPart().endsWith(".")) {
                lastToken = token;
                break;
            }
            if (sc == null) {
                break;
            }
            // Get next structure container
            try {
                String objectName = DBObjectNameCaseTransformer.transformName(dataSource, token);
                childObject = sc.getChild(monitor, objectName);
                if (childObject == null && i == 0 && selectedContainer != null) {
                    // Probably it is from selected object, let's try it
                    childObject = selectedContainer.getChild(monitor, objectName);
                    if (childObject != null) {
                        sc = selectedContainer;
                    }
                }
                if (childObject == null) {
                    if (i == 0) {
                        // Assume it's a table alias ?
                        childObject = this.getTableFromAlias(monitor, sc, token);
                        if (childObject == null) {
                            DBSStructureAssistant structureAssistant = DBUtils.getAdapter(DBSStructureAssistant.class, sc);
                            if (structureAssistant != null) {
                                Collection<DBSObjectReference> references = structureAssistant.findObjectsByMask(
                                    monitor,
                                    null,
                                    structureAssistant.getAutoCompleteObjectTypes(),
                                    wordDetector.removeQuotes(token),
                                    wordDetector.isQuoted(token),
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
            } catch (DBException e) {
                log.error(e);
                return;
            }
        }
        if (childObject == null) {
            return;
        }
        if (lastToken == null) {
            // Get all children objects as proposals
            makeProposalsFromChildren(monitor, childObject, null, proposals);
        } else {
            // Get matched children
            makeProposalsFromChildren(monitor, childObject, lastToken, proposals);
            if (proposals.isEmpty() || tokens.size() == 1) {
                if (selectedContainer != null && selectedContainer != childObject) {
                    // Try in active object
                    makeProposalsFromChildren(monitor, selectedContainer, lastToken, proposals);
                }
                if (proposals.isEmpty()) {
                    // At last - try to find child tables by pattern
                    DBSStructureAssistant structureAssistant = null;
                    for (DBSObject object = childObject; object != null; object =  object.getParentObject()) {
                        structureAssistant = DBUtils.getAdapter(DBSStructureAssistant.class, object);
                        if (structureAssistant != null) {
                            break;
                        }
                    }
                    if (structureAssistant != null) {
                        makeProposalsFromAssistant(monitor, structureAssistant, sc, lastToken, proposals);
                    }
                }
            }
        }
    }

    @Nullable
    private DBSObject getTableFromAlias(DBRProgressMonitor monitor, DBSObjectContainer sc, @Nullable String token)
    {
        final DBPDataSource dataSource = editor.getDataSource();
        if (!(dataSource instanceof SQLDataSource)) {
            return null;
        }
        if (activeQuery == null) {
            final SQLQuery queryAtPos = editor.extractQueryAtPos(documentOffset);
            if (queryAtPos != null) {
                activeQuery = queryAtPos.getQuery() + " ";
            }
        }

        final List<String> nameList = new ArrayList<>();
        if (token == null) {
            token = "";
        }

        {
            Matcher matcher;
            Pattern aliasPattern;
            SQLDialect sqlDialect = ((SQLDataSource) dataSource).getSQLDialect();
            String quoteString = sqlDialect.getIdentifierQuoteString();
            String quote = quoteString == null ? SQLConstants.STR_QUOTE_DOUBLE :
                SQLConstants.STR_QUOTE_DOUBLE.equals(quoteString) ?
                    quoteString :
                    Pattern.quote(quoteString);
            String catalogSeparator = sqlDialect.getCatalogSeparator();
            while (token.endsWith(catalogSeparator)) token = token.substring(0, token.length() -1);

            String tableNamePattern = "((?:" + quote + "(?:[.[^" + quote + "]]+)" + quote + ")|(?:[\\w" + Pattern.quote(catalogSeparator) + "]+))";
            String structNamePattern;
            if (CommonUtils.isEmpty(token)) {
                structNamePattern = "(?:from|update|join|into)\\s*" + tableNamePattern;
            } else {
                structNamePattern = tableNamePattern +
                    "(?:\\s*\\.\\s*" + tableNamePattern + ")?" +
                    "\\s+(?:(?:AS)\\s)?" + token + "[\\s,]+";
            }

            try {
                aliasPattern = Pattern.compile(structNamePattern, Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException e) {
                // Bad pattern - seems to be a bad token
                return null;
            }
            String testQuery = SQLUtils.stripComments(editor.getSyntaxManager().getDialect(), activeQuery);
            matcher = aliasPattern.matcher(testQuery);
            if (!matcher.find()) {
                return null;
            }

            int groupCount = matcher.groupCount();
            for (int i = 1; i <= groupCount; i++) {
                String group = matcher.group(i);
                if (!CommonUtils.isEmpty(group)) {
                    String[] allNames = group.split(Pattern.quote(catalogSeparator));
                    for (String name : allNames) {
                        if (quoteString != null && name.startsWith(quoteString) && name.endsWith(quoteString)) {
                            name = name.substring(1, name.length() - 1);
                        }
                        nameList.add(name);
                    }
                }
            }
        }

        if (nameList.isEmpty()) {
            return null;
        }

        for (int i = 0; i < nameList.size(); i++) {
            nameList.set(i,
                    DBObjectNameCaseTransformer.transformName(sc.getDataSource(), nameList.get(i)));
        }

        try {
            DBSObject childObject = null;
            while (childObject == null) {
                childObject = DBUtils.findNestedObject(monitor, sc, nameList);
                if (childObject == null) {
                    DBSObjectContainer parentSc = DBUtils.getParentAdapter(DBSObjectContainer.class, sc);
                    if (parentSc == null) {
                        break;
                    }
                    sc = parentSc;
                }
            }
            if (childObject == null && nameList.size() <= 1) {
                // No such object found - may be it's start of table name
                DBSStructureAssistant structureAssistant = DBUtils.getAdapter(DBSStructureAssistant.class, sc);
                if (structureAssistant != null) {
                    String objectNameMask = nameList.get(0);
                    Collection<DBSObjectReference> tables = structureAssistant.findObjectsByMask(
                        monitor,
                        sc,
                        structureAssistant.getAutoCompleteObjectTypes(),
                        wordDetector.removeQuotes(objectNameMask),
                        wordDetector.isQuoted(objectNameMask),
                        2);
                    if (!tables.isEmpty()) {
                        return tables.iterator().next().resolveObject(monitor);
                    }
                }
                return null;
            } else {
                return childObject;
            }
        } catch (DBException e) {
            log.error(e);
            return null;
        }
    }

    private void makeProposalsFromChildren(
        DBRProgressMonitor monitor,
        DBSObject parent,
        @Nullable String startPart,
        List<SQLCompletionProposal> proposals)
    {
        if (startPart != null) {
            startPart = wordDetector.removeQuotes(startPart).toUpperCase();
            int divPos = startPart.lastIndexOf(editor.getSyntaxManager().getStructSeparator());
            if (divPos != -1) {
                startPart = startPart.substring(divPos + 1);
            }
        }
        try {
            Collection<? extends DBSObject> children = null;
            if (parent instanceof DBSObjectContainer) {
                children = ((DBSObjectContainer)parent).getChildren(monitor);
            } else if (parent instanceof DBSEntity) {
                children = ((DBSEntity)parent).getAttributes(monitor);
            }
            if (children != null && !children.isEmpty()) {
                for (DBSObject child : children) {
                    if (startPart != null && !child.getName().toUpperCase().startsWith(startPart)) {
                        continue;
                    }
                    if (child instanceof DBPHiddenObject && ((DBPHiddenObject) child).isHidden()) {
                        // Skip hidden
                        continue;
                    }
                    proposals.add(makeProposalsFromObject(monitor, child));
                }
            }
        } catch (DBException e) {
            log.error(e);
        }
    }

    private void makeProposalsFromAssistant(
        DBRProgressMonitor monitor,
        DBSStructureAssistant assistant,
        @Nullable DBSObjectContainer rootSC,
        String objectName,
        List<SQLCompletionProposal> proposals)
    {
        try {
            Collection<DBSObjectReference> references = assistant.findObjectsByMask(
                monitor,
                rootSC,
                assistant.getAutoCompleteObjectTypes(),
                wordDetector.removeQuotes(objectName) + "%",
                wordDetector.isQuoted(objectName),
                100);
            for (DBSObjectReference reference : references) {
                proposals.add(makeProposalsFromObject(monitor, reference, reference.getObjectType().getImage()));
            }
        } catch (DBException e) {
            log.error(e);
        }
    }

    private SQLCompletionProposal makeProposalsFromObject(DBRProgressMonitor monitor, DBSObject object)
    {
        DBNNode node = DBeaverCore.getInstance().getNavigatorModel().getNodeByObject(monitor, object, false);
        return makeProposalsFromObject(monitor, object, node == null ? null : node.getNodeIconDefault());
    }

    private SQLCompletionProposal makeProposalsFromObject(DBRProgressMonitor monitor, DBPNamedObject object, @Nullable DBPImage objectIcon)
    {
        String objectName = DBUtils.getObjectFullName(object);

        boolean isSingleObject = true;
        String replaceString = null;
        DBPDataSource dataSource = editor.getDataSource();
        if (dataSource != null) {
            // If we replace short name with referenced object
            // and current active schema (catalog) is not this object's container then
            // replace with full qualified name
            if (!getPreferences().getBoolean(SQLPreferenceConstants.PROPOSAL_SHORT_NAME) && object instanceof DBSObjectReference) {
                if (wordDetector.getFullWord().indexOf(editor.getSyntaxManager().getStructSeparator()) == -1) {
                    DBSObjectReference structObject = (DBSObjectReference) object;
                    if (structObject.getContainer() != null) {
                        DBSObject selectedObject = getDefaultObject(dataSource);
                        if (selectedObject != structObject.getContainer()) {
                            replaceString = DBUtils.getFullQualifiedName(
                                dataSource,
                                structObject.getContainer(),
                                object);
                            isSingleObject = false;
                        }
                    }
                }
            }
            if (replaceString == null) {
                replaceString = DBUtils.getQuotedIdentifier(dataSource, object.getName());
            }
        } else {
            replaceString = DBUtils.getObjectShortName(object);
        }
        return createCompletionProposal(
            replaceString,
            objectName,
            null,
            objectIcon,
            isSingleObject,
            object);
    }

    public static String makeObjectDescription(DBRProgressMonitor monitor, DBPNamedObject object) {
        StringBuilder info = new StringBuilder();
        PropertyCollector collector = new PropertyCollector(object, false);
        collector.collectProperties();
        for (DBPPropertyDescriptor descriptor : collector.getPropertyDescriptors2()) {
            if (descriptor.isRemote()) {
                // Skip lazy properties
                continue;
            }
            Object propValue = collector.getPropertyValue(monitor, descriptor.getId());
            if (propValue == null) {
                continue;
            }
            String propString;
            if (propValue instanceof DBPNamedObject) {
                propString = ((DBPNamedObject) propValue).getName();
            } else {
                propString = DBUtils.getDefaultValueDisplayString(propValue, DBDDisplayFormat.UI);
            }
            info.append("<b>").append(descriptor.getDisplayName()).append(":  </b>");
            info.append(propString);
            info.append("<br>");
        }
        return info.toString();
    }

    private DBPPreferenceStore getPreferences() {
        DBPPreferenceStore store = null;
        DBPDataSource dataSource = editor.getDataSource();
        if (dataSource != null) {
            store = dataSource.getContainer().getPreferenceStore();
        }
        if (store == null) {
            store = DBeaverCore.getGlobalPreferenceStore();
        }
        return store;
    }

    /*
    * Turns the vector into an Array of ICompletionProposal objects
    */
    protected SQLCompletionProposal createCompletionProposal(
        String replaceString,
        String displayString,
        String description,
        @Nullable DBPImage image,
        boolean isObject,
        @Nullable DBPNamedObject object)
    {
        DBPPreferenceStore store = getPreferences();
        DBPDataSource dataSource = editor.getDataSource();
        if (dataSource != null) {
            if (isObject) {
                // Escape replace string if required
                replaceString = DBUtils.getQuotedIdentifier(dataSource, replaceString);
            }
        }

        // If we have quoted string then ignore pref settings
        boolean quotedString = wordDetector.isQuoted(replaceString);
        final int proposalCase = quotedString ?
            SQLPreferenceConstants.PROPOSAL_CASE_DEFAULT :
            store.getInt(SQLPreferenceConstants.PROPOSAL_INSERT_CASE);
        switch (proposalCase) {
            case SQLPreferenceConstants.PROPOSAL_CASE_UPPER:
                replaceString = replaceString.toUpperCase();
                break;
            case SQLPreferenceConstants.PROPOSAL_CASE_LOWER:
                replaceString = replaceString.toLowerCase();
                break;
            default:
                DBPIdentifierCase convertCase = quotedString && dataSource instanceof SQLDataSource ?
                    ((SQLDataSource) dataSource).getSQLDialect().storesQuotedCase() : DBPIdentifierCase.MIXED;
                replaceString = convertCase.transform(replaceString);
                break;
        }

        Image img = image == null ? null : DBeaverIcons.getImage(image);
        return new SQLCompletionProposal(
            editor.getSyntaxManager(),
            displayString,
            replaceString, // replacementString
            wordDetector, // wordDetector
            replaceString.length(), //cursorPosition the position of the cursor following the insert
                                // relative to replacementOffset
            img, //image to display
            new ContextInformation(img, displayString, displayString), //the context information associated with this proposal
            description,
            object);
    }

    /**
     * This method is incomplete in that it does not implement logic to produce
     * some context help relevant to SQL. It just hard codes two strings to
     * demonstrate the action
     *
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(ITextViewer,
     *      int)
     */
    @Nullable
    @Override
    public IContextInformation[] computeContextInformation(
        ITextViewer viewer, int documentOffset)
    {
        SQLQuery statementInfo = editor.extractQueryAtPos(documentOffset);
        if (statementInfo == null || CommonUtils.isEmpty(statementInfo.getQuery())) {
            return null;
        }

        IContextInformation[] result = new IContextInformation[1];
        result[0] = new ContextInformation(statementInfo.getQuery(), statementInfo.getQuery());
        return result;
    }

    @Override
    public char[] getCompletionProposalAutoActivationCharacters()
    {
        return new char[] {'.'};
    }

    @Nullable
    @Override
    public char[] getContextInformationAutoActivationCharacters()
    {
        return null;
    }

    @Nullable
    @Override
    public String getErrorMessage()
    {
        return null;
    }

    @Override
    public IContextInformationValidator getContextInformationValidator()
    {
        return validator;
    }

    @Nullable
    private static DBSObject getDefaultObject(DBSInstance dataSource)
    {
        DBSObjectSelector objectSelector = DBUtils.getAdapter(DBSObjectSelector.class, dataSource);
        if (objectSelector != null) {
            return objectSelector.getSelectedObject();
        }
        return null;
    }

    /**
     * Simple content assist tip closer. The tip is valid in a range of 5
     * characters around its popup location.
     */
    protected static class Validator implements IContextInformationValidator, IContextInformationPresenter
    {

        protected int fInstallOffset;

        @Override
        public boolean isContextInformationValid(int offset)
        {
            return Math.abs(fInstallOffset - offset) < 5;
        }

        @Override
        public void install(IContextInformation info,
            ITextViewer viewer, int offset)
        {
            fInstallOffset = offset;
        }

        @Override
        public boolean updatePresentation(int documentPosition,
            TextPresentation presentation)
        {
            return false;
        }
    }

    private class ProposalSearchJob extends AbstractJob {
        private List<SQLCompletionProposal> proposals;
        private String wordPart;
        private QueryType qt;
        private transient boolean finished = false;

        public ProposalSearchJob(List<SQLCompletionProposal> proposals, String wordPart, QueryType qt) {
            super("Search proposals...");
            setUser(true);
            this.proposals = proposals;
            this.wordPart = wordPart;
            this.qt = qt;
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            try {
                monitor.beginTask("Seeking for completion proposals", 1);
                try {
                    monitor.subTask("Make structure proposals");
                    makeStructureProposals(monitor, proposals, wordPart, qt);
                } finally {
                    monitor.done();
                }

                return Status.OK_STATUS;
            } catch (Throwable e) {
                log.error(e);
                return Status.CANCEL_STATUS;
            } finally {
                finished = true;
            }
        }
    }

}