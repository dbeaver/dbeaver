/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.sql.SQLStatementInfo;
import org.jkiss.dbeaver.ui.editors.sql.SQLConstants;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.properties.PropertyCollector;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The SQL content assist processor. This content assist processor proposes text
 * completions and computes context information for a SQL content type.
 */
public class SQLCompletionProcessor implements IContentAssistProcessor
{
    static final Log log = LogFactory.getLog(SQLCompletionProcessor.class);

    private static enum QueryType {
        TABLE,
        COLUMN
    }

    private SQLEditorBase editor;
    private IContextInformationValidator validator = new Validator();
    private int documentOffset;
    private String activeQuery = null;
    private SQLWordPartDetector wordDetector;

    public SQLCompletionProcessor(SQLEditorBase editor)
    {
        this.editor = editor;
    }

    /**
     * This method returns a list of completion proposals as ICompletionProposal
     * objects. The proposals are based on the word at the offset in the document
     * where the cursor is positioned. In this implementation, we find the word at
     * the document offset and compare it to our list of SQL reserved words. The
     * list is a subset, of those words that match what the user has entered. For
     * example, the text or proposes the SQL keywords OR and ORDER. The list is
     * returned as an array of completion proposals.
     *
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(ITextViewer, int)
     */
    @Override
    public ICompletionProposal[] computeCompletionProposals(
        ITextViewer viewer,
        int documentOffset)
    {
        this.documentOffset = documentOffset;
        this.activeQuery = null;

        wordDetector = new SQLWordPartDetector(viewer.getDocument(), editor.getSyntaxManager(), documentOffset);
        final List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
        final String wordPart = wordDetector.getWordPart();

        final boolean isStructureQuery =
            wordDetector.getPrevKeyWord() != null &&
            (CommonUtils.isEmpty(wordDetector.getPrevWords()) || (wordDetector.getPrevDelimiter() != null && wordDetector.getPrevDelimiter().indexOf(',') != -1));
        QueryType queryType = null;
        if (isStructureQuery) {
            if (editor.getSyntaxManager().getKeywordManager().isEntityQueryWord(wordDetector.getPrevKeyWord())) {
                queryType = QueryType.TABLE;

            } else if (editor.getSyntaxManager().getKeywordManager().isAttributeQueryWord(wordDetector.getPrevKeyWord()) && CommonUtils.isEmptyTrimmed(wordDetector.getPrevDelimiter())) {
                queryType = QueryType.COLUMN;
            }
        }
        if (queryType != null || wordDetector.containsSeparator(wordPart)) {
            // It's a table query
            // Use database information (only we are connected, of course)
            if (editor.getDataSource() != null) {
                try {
                    final QueryType qt = queryType;
                    DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                        @Override
                        public void run(DBRProgressMonitor monitor)
                            throws InvocationTargetException, InterruptedException
                        {
                            makeStructureProposals(monitor, proposals, wordPart, qt);
                        }
                    });
                } catch (InvocationTargetException e) {
                    log.warn("Error while seeking for structure proposals", e.getTargetException());
                } catch (InterruptedException e) {
                    // interrupted - do nothing
                }
            }
        } else if (wordPart.length() == 0) {
            // No assist
        } else {
            // Keyword assist
            List<String> matchedKeywords = editor.getSyntaxManager().getKeywordManager().getMatchedKeywords(wordPart);
            for (String keyWord : matchedKeywords) {
                DBPKeywordType keywordType = editor.getSyntaxManager().getKeywordManager().getKeywordType(keyWord);
                proposals.add(
                    createCompletionProposal(
                        keyWord,
                        keyWord,
                        keyWord + " (" + keywordType.name() + ")",
                        null,
                        false));
            }
        }

        // Remove duplications
        for (int i = 0; i < proposals.size(); i++) {
            ICompletionProposal proposal = proposals.get(i);
            for (int j = i + 1; j < proposals.size(); ) {
                ICompletionProposal proposal2 = proposals.get(j);
                if (proposal.getDisplayString().equals(proposal2.getDisplayString())) {
                    proposals.remove(j);
                } else {
                    j++;
                }
            }
        }
        return proposals.toArray(new ICompletionProposal[proposals.size()]);
    }

    private void makeStructureProposals(
        DBRProgressMonitor monitor,
        List<ICompletionProposal> proposals,
        String wordPart,
        QueryType queryType)
    {
        DBPDataSource dataSource = editor.getDataSource();
        if (queryType != null && dataSource != null) {
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
                    DBSObjectSelector objectSelector = DBUtils.getAdapter(DBSObjectSelector.class, dataSource);
                    if (objectSelector != null) {
                        DBSObject selectedObject = objectSelector.getSelectedObject();
                        if (selectedObject != null) {
                            makeProposalsFromChildren(monitor, selectedObject, null, proposals);
                        }
                    }
                    rootObject = (DBSObjectContainer) dataSource;
                }
                if (rootObject != null) {
                    makeProposalsFromChildren(monitor, rootObject, null, proposals);
                }
            } else {
                // Get root object or objects from active database (if any)
                makeStructureProposals(monitor, dataSource, proposals);
            }
        //} else if (queryType == QueryType.COLUMN) {

        } else {
            // Get list of sub-objects (filtered by wordPart)
            makeStructureProposals(monitor, dataSource, proposals);
        }
    }

    private void makeStructureProposals(
        DBRProgressMonitor monitor,
        DBPDataSource dataSource,
        List<ICompletionProposal> proposals)
    {
        final DBSObjectContainer rootContainer = DBUtils.getAdapter(DBSObjectContainer.class, dataSource);
        if (rootContainer == null) {
            return;
        }
        DBSObjectContainer selectedContainer = null;
        {
            DBSObjectSelector objectSelector = DBUtils.getAdapter(DBSObjectSelector.class, dataSource);
            if (objectSelector != null) {
                DBSObject selectedObject = objectSelector.getSelectedObject();
                if (selectedObject != null) {
                    selectedContainer = DBUtils.getAdapter(DBSObjectContainer.class, selectedObject);
                }
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
                // At last - try to find child tables by pattern
                DBSStructureAssistant structureAssistant = DBUtils.getAdapter(DBSStructureAssistant.class, childObject);
                if (structureAssistant != null) {
                    makeProposalsFromAssistant(monitor, structureAssistant, rootContainer, lastToken, proposals);
                }
            }
        }
    }

    private DBSObject getTableFromAlias(DBRProgressMonitor monitor, DBSObjectContainer sc, String token)
    {
        if (activeQuery == null) {
            activeQuery = editor.extractQueryAtPos(documentOffset).getQuery() + " ";
        }

        List<String> nameList = new ArrayList<String>();
        if (token == null) {
            token = "";
        }

        Matcher matcher;
        Pattern aliasPattern;
        DBPDataSourceInfo dataSourceInfo = editor.getDataSource().getInfo();
        String quote = dataSourceInfo.getIdentifierQuoteString();
        if (quote == null) {
            quote = SQLConstants.STR_QUOTE_DOUBLE;
        }
        quote = "\\" + quote;
        String catalogSeparator = dataSourceInfo.getCatalogSeparator();
        if (catalogSeparator == null) {
            catalogSeparator = SQLConstants.STRUCT_SEPARATOR;
        }
        String tableNamePattern = "((" + quote + "([.[^" + quote + "]]+)" + quote + ")|([\\w\\" + catalogSeparator + "]+))";
        String structNamePattern;
        if (CommonUtils.isEmpty(token)) {
            structNamePattern = "from\\s*" + tableNamePattern + "\\s*where";
        } else {
            structNamePattern = tableNamePattern +
                "(\\s*\\.\\s*" + tableNamePattern + ")?" +
                "\\s+((AS)\\s)?" + token + "[\\s,]+";
        }

        try {
            aliasPattern = Pattern.compile(structNamePattern, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            // Bad pattern - seems to be a bad token
            return null;
        }
        matcher = aliasPattern.matcher(activeQuery);
        if (!matcher.find()) {
            return null;
        }

        int groupCount = matcher.groupCount();
        if (groupCount < 4) {
            return null;
        }

        String startName = matcher.group(3);
        if (startName == null) {
            startName = matcher.group(4);
            if (startName == null) {
                return null;
            }
        }
        nameList.addAll(CommonUtils.splitString(startName, catalogSeparator.charAt(0)));
        if (groupCount >= 8) {
            String nextName = matcher.group(8);
            if (nextName == null && groupCount >= 9) {
                nextName = matcher.group(9);
            }
            if (nextName != null) {
                nameList.addAll(CommonUtils.splitString(nextName, catalogSeparator.charAt(0)));
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
            DBSObject childObject = DBUtils.findNestedObject(monitor, sc, nameList);
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
        String startPart,
        List<ICompletionProposal> proposals)
    {
        if (startPart != null) {
            startPart = wordDetector.removeQuotes(startPart).toUpperCase();
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
        DBSObjectContainer rootSC,
        String objectName,
        List<ICompletionProposal> proposals)
    {
        try {
            Collection<DBSObjectReference> tables = assistant.findObjectsByMask(
                monitor,
                rootSC,
                assistant.getAutoCompleteObjectTypes(),
                wordDetector.removeQuotes(objectName) + "%",
                wordDetector.isQuoted(objectName),
                100);
            for (DBSObjectReference table : tables) {
                proposals.add(makeProposalsFromObject(table, table.getObjectType().getImage()));
            }
        } catch (DBException e) {
            log.error(e);
        }
    }

    private ICompletionProposal makeProposalsFromObject(DBRProgressMonitor monitor, DBSObject object)
    {
        DBNNode node = DBeaverCore.getInstance().getNavigatorModel().getNodeByObject(monitor, object, true);
        return makeProposalsFromObject(object, node == null ? null : node.getNodeIconDefault());
    }

    private ICompletionProposal makeProposalsFromObject(DBPNamedObject object, Image objectIcon)
    {
        String objectFullName = DBUtils.getObjectFullName(object);

        StringBuilder info = new StringBuilder();
        PropertyCollector collector = new PropertyCollector(object, false);
        collector.collectProperties();
        for (IPropertyDescriptor descriptor : collector.getPropertyDescriptors()) {
            Object propValue = collector.getPropertyValue(descriptor.getId());
            if (propValue == null) {
                continue;
            }
            String propString = propValue.toString();
            info.append("<b>").append(descriptor.getDisplayName()).append(":  </b>");
            info.append(propString);
            info.append("<br>");
        }

        return createCompletionProposal(
            DBUtils.getQuotedIdentifier(editor.getDataSource(), object.getName()),
            objectFullName,
            info.toString(),
            objectIcon,
            true);
    }

    /*
    * Turns the vector into an Array of ICompletionProposal objects
    */
    protected ICompletionProposal createCompletionProposal(
        String replaceString,
        String displayString,
        String description,
        Image image,
        boolean isObject)
    {
        IPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();
        if (editor.getDataSource() != null) {
            store = editor.getDataSource().getContainer().getPreferenceStore();
        }
        if (isObject) {
            // Escape replace string if required
            replaceString = DBUtils.getQuotedIdentifier(editor.getDataSource(), replaceString);
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
                DBPIdentifierCase convertCase = quotedString ? editor.getDataSource().getInfo().storesQuotedCase() : editor.getDataSource().getInfo().storesUnquotedCase();
                replaceString = convertCase.transform(replaceString);
                break;
        }

        return new SQLCompletionProposal(
            editor.getSyntaxManager(),
            displayString,
            replaceString, // replacementString
            wordDetector, // wordDetector
            replaceString.length(), //cursorPosition the position of the cursor following the insert
                                // relative to replacementOffset
            image, //image to display
            new ContextInformation(image, displayString, displayString), //the context information associated with this proposal
            description);
    }

    /**
     * This method is incomplete in that it does not implement logic to produce
     * some context help relevant to SQL. It just hard codes two strings to
     * demonstrate the action
     *
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(ITextViewer,
     *      int)
     */
    @Override
    public IContextInformation[] computeContextInformation(
        ITextViewer viewer, int documentOffset)
    {
        SQLStatementInfo statementInfo = editor.extractQueryAtPos(documentOffset);
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

    @Override
    public char[] getContextInformationAutoActivationCharacters()
    {
        return null;
    }

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
}