/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.syntax;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.sql.SQLStatementInfo;
import org.jkiss.dbeaver.ui.editors.sql.SQLConstants;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.properties.PropertyCollector;

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

    private SQLEditor editor;
    private IContextInformationValidator validator = new Validator();
    private int documentOffset;
    private String activeQuery = null;
    private SQLWordPartDetector wordDetector;

    public SQLCompletionProcessor(SQLEditor editor)
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
            if (editor.getSyntaxManager().getKeywordManager().isTableQueryWord(wordDetector.getPrevKeyWord())) {
                queryType = QueryType.TABLE;

            } else if (editor.getSyntaxManager().getKeywordManager().isColumnQueryWord(wordDetector.getPrevKeyWord()) && CommonUtils.isEmptyTrimmed(wordDetector.getPrevDelimiter())) {
                queryType = QueryType.COLUMN;
            }
        }
        if (queryType != null || wordDetector.containsSeparator(wordPart)) {
            // It's a table query
            // Use database information (only we are connected, of course)
            if (editor.getDataSourceContainer() != null && editor.getDataSourceContainer().isConnected()) {
                try {
                    final QueryType qt = queryType;
                    DBeaverCore.getInstance().runInProgressService(new DBRRunnableWithProgress() {
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
        DBSDataSourceContainer dsContainer = editor.getDataSourceContainer();
        DBPDataSource dataSource = dsContainer.isConnected() ? dsContainer.getDataSource() : null;
        if (queryType != null) {
            // Try to determine which object is queried (if wordPart is not empty)
            // or get list of root database objects
            if (wordPart.length() == 0) {
                // Get root objects
                DBSObject rootObject = null;
                if (queryType == QueryType.COLUMN && dataSource instanceof DBSEntityContainer) {
                    // Try to detect current table
                    rootObject = getTableFromAlias(monitor, (DBSEntityContainer)dataSource, null);
                } else if (dataSource instanceof DBSEntityContainer) {
                    rootObject = (DBSEntityContainer) dataSource;
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
        if (!(dataSource instanceof DBSEntityContainer)) {
            return;
        }
        DBSEntityContainer sc = (DBSEntityContainer) dataSource;
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
                String objectName = JDBCObjectNameCaseTransformer.transformName(dataSource, token);
                childObject = sc.getChild(monitor, objectName);
                if (childObject == null) {
                    if (i == 0) {
                        // Assume it's a table alias ?
                        childObject = this.getTableFromAlias(monitor, sc, token);
                        if (childObject == null) {
                            DBSStructureAssistant structureAssistant = DBUtils.getAdapter(DBSStructureAssistant.class, sc);
                            if (structureAssistant != null) {
                                Collection<DBSObject> tables = structureAssistant.findObjectsByMask(
                                    monitor,
                                    null,
                                    structureAssistant.getAutoCompleteObjectTypes(),
                                    token,
                                    2);
                                if (!tables.isEmpty()) {
                                    childObject = tables.iterator().next();
                                }
                            }
                        }
                    } else {
                        // Path element not found. Damn - can't do anything.
                        return;
                    }
                }

                if (childObject instanceof DBSEntityContainer) {
                    sc = (DBSEntityContainer) childObject;
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
                    makeProposalsFromAssistant(monitor, structureAssistant, (DBSEntityContainer) dataSource, lastToken, proposals);
                }
            }
        }
    }

    private DBSObject getTableFromAlias(DBRProgressMonitor monitor, DBSEntityContainer sc, String token)
    {
        if (activeQuery == null) {
            activeQuery = editor.extractQueryAtPos(documentOffset).getQuery() + " ";
        }
        activeQuery = activeQuery.toUpperCase();
        if (token == null) {

        }

        // Hooray, we got the match.
        List<String> nameList = new ArrayList<String>();

        if (token == null) {
            token = "";
        }

        token = token.toUpperCase();
        //SQLQueryInfo queryInfo = new SQLQueryInfo(activeQuery);
        //List<SQLQueryInfo.TableRef> refList = queryInfo.getTableRefs();
        Matcher matcher;
        Pattern aliasPattern;
        String quote;
        quote = editor.getDataSource().getInfo().getIdentifierQuoteString();
        if (quote == null) {
            quote = SQLConstants.STR_QUOTE_DOUBLE;
        }
        quote = "\\" + quote;
        String tableNamePattern = "((" + quote + "([.[^" + quote + "]]+)" + quote + ")|([\\w]+))";
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
        nameList.add(startName);
        if (groupCount >= 8) {
            String nextName = matcher.group(8);
            if (nextName == null && groupCount >= 9) {
                nextName = matcher.group(9);
            }
            if (nextName != null) {
                nameList.add(nextName);
            }
        }

        if (nameList.isEmpty()) {
            return null;
        }
        try {
            DBSObject childObject = DBUtils.findNestedObject(monitor, sc, nameList);
            if (childObject == null && nameList.size() <= 1) {
                // No such object found - may be it's start of table name
                DBSStructureAssistant structureAssistant = DBUtils.getAdapter(DBSStructureAssistant.class, sc);
                if (structureAssistant != null) {
                    Collection<DBSObject> tables = structureAssistant.findObjectsByMask(
                        monitor,
                        sc,
                        structureAssistant.getAutoCompleteObjectTypes(),
                        nameList.get(0),
                        2);
                    if (!tables.isEmpty()) {
                        return tables.iterator().next();
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
            startPart = startPart.toUpperCase();
        }
        try {
            Collection<? extends DBSObject> children = null;
            if (parent instanceof DBSEntityContainer) {
                children = ((DBSEntityContainer)parent).getChildren(monitor);
            } else if (parent instanceof DBSTable) {
                children = ((DBSTable)parent).getColumns(monitor);
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
        DBSEntityContainer rootSC,
        String tableName,
        List<ICompletionProposal> proposals)
    {
        try {
            Collection<DBSObject> tables = assistant.findObjectsByMask(
                monitor,
                rootSC,
                assistant.getAutoCompleteObjectTypes(),
                tableName + "%",
                100);
            for (DBSObject table : tables) {
                proposals.add(makeProposalsFromObject(monitor, table));
            }
        } catch (DBException e) {
            log.error(e);
        }
    }

    private ICompletionProposal makeProposalsFromObject(DBRProgressMonitor monitor, DBSObject object)
    {
        String objectName = object.getName();
        //String displayString = objectName;
        //if (object instanceof DBSEntityQualified) {
        //    displayString = ((DBSEntityQualified)object).getFullQualifiedName();
        //}

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

        DBNNode node = DBeaverCore.getInstance().getNavigatorModel().getNodeByObject(monitor, object, true);
/*
        return new ContextInformation(
                node == null ? null : node.getNodeIconDefault(),
                childName,
                info.toString());
*/
        return createCompletionProposal(objectName, objectName, info.toString(), node == null ? null : node.getNodeIconDefault(), true);
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
        if (isObject) {
            // Escape replace string if required
            replaceString = DBUtils.getQuotedIdentifier(editor.getDataSource(), replaceString);
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

    public char[] getCompletionProposalAutoActivationCharacters()
    {
        return new char[] {'.'};
    }

    public char[] getContextInformationAutoActivationCharacters()
    {
        return null;
    }

    public String getErrorMessage()
    {
        return null;
    }

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

        public boolean isContextInformationValid(int offset)
        {
            return Math.abs(fInstallOffset - offset) < 5;
        }

        public void install(IContextInformation info,
            ITextViewer viewer, int offset)
        {
            fInstallOffset = offset;
        }

        public boolean updatePresentation(int documentPosition,
            TextPresentation presentation)
        {
            return false;
        }
    }
}