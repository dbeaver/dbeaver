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
import org.eclipse.swt.widgets.Display;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.ui.TextUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.editors.sql.templates.SQLContext;
import org.jkiss.dbeaver.ui.editors.sql.templates.SQLTemplateCompletionProposal;
import org.jkiss.dbeaver.ui.editors.sql.templates.SQLTemplatesRegistry;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * The SQL content assist processor. This content assist processor proposes text
 * completions and computes context information for a SQL content type.
 */
public class SQLCompletionProcessor implements IContentAssistProcessor
{
    private static final Log log = Log.getLog(SQLCompletionProcessor.class);
    public static final String ALL_COLUMNS_PATTERN = "*";

    enum QueryType {
        TABLE,
        COLUMN
    }

    private static IContextInformationValidator VALIDATOR = new Validator();
    private static boolean lookupTemplates = false;
    private static boolean simpleMode = false;

    public static boolean isLookupTemplates() {
        return lookupTemplates;
    }

    public static void setLookupTemplates(boolean lookupTemplates) {
        SQLCompletionProcessor.lookupTemplates = lookupTemplates;
    }

    public static void setSimpleMode(boolean simpleMode) {
        SQLCompletionProcessor.simpleMode = simpleMode;
    }

    private final SQLEditorBase editor;

    public SQLCompletionProcessor(SQLEditorBase editor)
    {
        this.editor = editor;
    }

    @Override
    public ICompletionProposal[] computeCompletionProposals(
        ITextViewer viewer,
        int documentOffset)
    {
        final SQLCompletionAnalyzer.CompletionRequest request = new SQLCompletionAnalyzer.CompletionRequest(editor, documentOffset, simpleMode);
        SQLWordPartDetector wordDetector = request.wordDetector =
            new SQLWordPartDetector(viewer.getDocument(), editor.getSyntaxManager(), documentOffset);
        request.wordPart = wordDetector.getWordPart();

        if (lookupTemplates) {
            return makeTemplateProposals(viewer, request);
        }

        request.queryType = null;
        String searchPrefix = request.wordPart;
        {
            final String prevKeyWord = wordDetector.getPrevKeyWord();
            if (!CommonUtils.isEmpty(prevKeyWord)) {
                if (editor.getSyntaxManager().getDialect().isEntityQueryWord(prevKeyWord)) {
                    // TODO: its an ugly hack. Need a better way
                    if (SQLConstants.KEYWORD_INTO.equals(prevKeyWord) &&
                        !CommonUtils.isEmpty(wordDetector.getPrevWords()) &&
                        ("(".equals(wordDetector.getPrevDelimiter()) || ",".equals(wordDetector.getPrevDelimiter())))
                    {
                        request.queryType = QueryType.COLUMN;
                    } else {
                        request.queryType = QueryType.TABLE;
                    }
                } else if (editor.getSyntaxManager().getDialect().isAttributeQueryWord(prevKeyWord)) {
                    request.queryType = QueryType.COLUMN;
                    if (!request.simpleMode && CommonUtils.isEmpty(request.wordPart) && wordDetector.getPrevDelimiter().equals(ALL_COLUMNS_PATTERN)) {
                        wordDetector.moveToDelimiter();
                        searchPrefix = ALL_COLUMNS_PATTERN;
                    }
                }
            }
        }
        request.wordPart = searchPrefix;
        if (request.queryType != null && request.wordPart != null) {
            if (editor.getDataSource() != null) {
                ProposalSearchJob searchJob = new ProposalSearchJob(request);
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

        if (!CommonUtils.isEmpty(request.wordPart))  {
            // Keyword assist
            List<String> matchedKeywords = editor.getSyntaxManager().getDialect().getMatchedKeywords(request.wordPart);
            if (!request.simpleMode) {
                // Sort using fuzzy match
                Collections.sort(matchedKeywords, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return TextUtils.fuzzyScore(o1, request.wordPart) - TextUtils.fuzzyScore(o2, request.wordPart);
                    }
                });
            }
            for (String keyWord : matchedKeywords) {
                DBPKeywordType keywordType = editor.getSyntaxManager().getDialect().getKeywordType(keyWord);
                if (keywordType != null) {
                    request.proposals.add(
                        SQLCompletionAnalyzer.createCompletionProposal(
                            request,
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
        for (int i = 0; i < request.proposals.size(); i++) {
            SQLCompletionProposal proposal = request.proposals.get(i);
            for (int j = i + 1; j < request.proposals.size(); ) {
                SQLCompletionProposal proposal2 = request.proposals.get(j);
                if (proposal.getDisplayString().equals(proposal2.getDisplayString())) {
                    request.proposals.remove(j);
                } else {
                    j++;
                }
            }
        }
        DBSObject selectedObject = DBUtils.getActiveInstanceObject(editor.getDataSource());
        boolean hideDups = editor.getActivePreferenceStore().getBoolean(SQLPreferenceConstants.HIDE_DUPLICATE_PROPOSALS) && selectedObject != null;
        if (hideDups) {
            for (int i = 0; i < request.proposals.size(); i++) {
                SQLCompletionProposal proposal = request.proposals.get(i);
                for (int j = 0; j < request.proposals.size(); ) {
                    SQLCompletionProposal proposal2 = request.proposals.get(j);
                    if (i != j && proposal.hasStructObject() && proposal2.hasStructObject() &&
                        CommonUtils.equalObjects(proposal.getObject().getName(), proposal2.getObject().getName()) &&
                        proposal.getObjectContainer() == selectedObject) {
                        request.proposals.remove(j);
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
        return ArrayUtils.toArray(ICompletionProposal.class, request.proposals);
    }

    @NotNull
    private ICompletionProposal[] makeTemplateProposals(ITextViewer viewer, SQLCompletionAnalyzer.CompletionRequest request) {
        String wordPart = request.wordPart.toLowerCase();
        final List<SQLTemplateCompletionProposal> templateProposals = new ArrayList<>();
        // Templates
        for (Template template : editor.getTemplatesPage().getTemplateStore().getTemplates()) {
            if (template.getName().toLowerCase().startsWith(wordPart)) {
                templateProposals.add(new SQLTemplateCompletionProposal(
                    template,
                    new SQLContext(
                        SQLTemplatesRegistry.getInstance().getTemplateContextRegistry().getContextType(template.getContextTypeId()),
                        viewer.getDocument(),
                        new Position(request.wordDetector.getStartOffset(), request.wordDetector.getLength()),
                        editor),
                    new Region(request.documentOffset, 0),
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
        boolean useKeystrokes = editor.getActivePreferenceStore().getBoolean(SQLPreferenceConstants.ENABLE_KEYSTROKE_ACTIVATION);
        return useKeystrokes ?
            ".abcdefghijklmnopqrstuvwxyz_$".toCharArray() :
            new char[] {'.', };
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
        return VALIDATOR;
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
        private final SQLCompletionAnalyzer.CompletionRequest request;
        private transient boolean finished = false;

        public ProposalSearchJob(SQLCompletionAnalyzer.CompletionRequest request) {
            super("Search proposals...");
            setSystem(true);
            this.request = request;
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            try {
                monitor.beginTask("Seeking for completion proposals", 1);
                try {
                    monitor.subTask("Make structure proposals");
                    SQLCompletionAnalyzer analyzer = new SQLCompletionAnalyzer(monitor, request);
                    analyzer.runAnalyzer();
                } finally {
                    monitor.done();
                }
                applyFilters();
                return Status.OK_STATUS;
            } catch (Throwable e) {
                log.error(e);
                return Status.CANCEL_STATUS;
            } finally {
                finished = true;
            }
        }

        private void applyFilters() {
            DBPDataSource dataSource = editor.getDataSource();
            if (dataSource == null) {
                return;
            }
            DBPDataSourceContainer dsContainer = dataSource.getContainer();
            Map<DBSObject, Map<Class, List<SQLCompletionProposal>>> containerMap = new HashMap<>();
            for (SQLCompletionProposal proposal : request.proposals) {
                DBSObject container = proposal.getObjectContainer();
                DBPNamedObject object = proposal.getObject();
                if (object == null) {
                    continue;
                }
                Map<Class, List<SQLCompletionProposal>> typeMap = containerMap.get(container);
                if (typeMap == null) {
                    typeMap = new HashMap<>();
                    containerMap.put(container, typeMap);
                }
                Class objectType = object instanceof DBSObjectReference ? ((DBSObjectReference) object).getObjectClass() : object.getClass();
                List<SQLCompletionProposal> list = typeMap.get(objectType);
                if (list == null) {
                    list = new ArrayList<>();
                    typeMap.put(objectType, list);
                }
                list.add(proposal);
            }
            for (Map.Entry<DBSObject, Map<Class, List<SQLCompletionProposal>>> entry : containerMap.entrySet()) {
                for (Map.Entry<Class, List<SQLCompletionProposal>> typeEntry : entry.getValue().entrySet()) {
                    DBSObjectFilter filter = dsContainer.getObjectFilter(typeEntry.getKey(), entry.getKey(), true);
                    if (filter != null && filter.isEnabled()) {
                        for (SQLCompletionProposal proposal : typeEntry.getValue()) {
                            if (!filter.matches(proposal.getObject().getName())) {
                                request.proposals.remove(proposal);
                            }
                        }
                    }
                }
            }
        }
    }

}