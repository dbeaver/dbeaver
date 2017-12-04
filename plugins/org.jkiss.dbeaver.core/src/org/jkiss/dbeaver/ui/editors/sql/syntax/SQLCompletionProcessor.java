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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.contentassist.*;
import org.eclipse.jface.text.templates.Template;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.registry.sql.SQLCommandHandlerDescriptor;
import org.jkiss.dbeaver.registry.sql.SQLCommandsRegistry;
import org.jkiss.dbeaver.ui.TextUtils;
import org.jkiss.dbeaver.ui.UIUtils;
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
    static final String ALL_COLUMNS_PATTERN = "*";

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

    static void setSimpleMode(boolean simpleMode) {
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

        try {
            String commandPrefix = editor.getSyntaxManager().getControlCommandPrefix();
            if (request.wordDetector.getStartOffset() >= commandPrefix.length() &&
                viewer.getDocument().get(request.wordDetector.getStartOffset() - commandPrefix.length(), commandPrefix.length()).equals(commandPrefix))
            {
                return makeCommandProposals(request, request.wordPart);
            }
        } catch (BadLocationException e) {
            log.debug(e);
        }

        String searchPrefix = request.wordPart;
        request.queryType = null;
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
        DBPDataSource dataSource = editor.getDataSource();
        if (request.wordPart != null) {
            if (dataSource != null) {
                ProposalSearchJob searchJob = new ProposalSearchJob(request);
                searchJob.schedule();
                // Wait until job finished
                UIUtils.waitJobCompletion(searchJob);
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
                            keywordType,
                            null,
                            false,
                            null)
                    );
                }
            }
        }
        filterProposals(request, dataSource);


        return ArrayUtils.toArray(ICompletionProposal.class, request.proposals);
    }

    private void filterProposals(SQLCompletionAnalyzer.CompletionRequest request, DBPDataSource dataSource) {

        // Remove duplications
        final Set<String> proposalMap = new HashSet<>(request.proposals.size());
        for (int i = 0; i < request.proposals.size(); ) {
            SQLCompletionProposal proposal = request.proposals.get(i);
            if (proposalMap.contains(proposal.getDisplayString())) {
                request.proposals.remove(i);
                continue;
            }
            proposalMap.add(proposal.getDisplayString());
            i++;
        }

        DBSObject selectedObject = dataSource == null ? null: DBUtils.getActiveInstanceObject(dataSource);
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

            }

        }

    }

    private ICompletionProposal[] makeCommandProposals(SQLCompletionAnalyzer.CompletionRequest request, String prefix) {
        final String controlCommandPrefix = editor.getSyntaxManager().getControlCommandPrefix();
        if (prefix.startsWith(controlCommandPrefix)) {
            prefix = prefix.substring(controlCommandPrefix.length());
        }
        final List<SQLCommandCompletionProposal> commandProposals = new ArrayList<>();
        for (SQLCommandHandlerDescriptor command : SQLCommandsRegistry.getInstance().getCommandHandlers()) {
            if (command.getId().startsWith(prefix)) {
                commandProposals.add(new SQLCommandCompletionProposal(request, command));
            }
        }
        return commandProposals.toArray(new ICompletionProposal[commandProposals.size()]);
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
        SQLScriptElement statementInfo = editor.extractQueryAtPos(documentOffset);
        if (statementInfo == null || CommonUtils.isEmpty(statementInfo.getText())) {
            return null;
        }

        IContextInformation[] result = new IContextInformation[1];
        result[0] = new ContextInformation(statementInfo.getText(), statementInfo.getText());
        return result;
    }

    @Override
    public char[] getCompletionProposalAutoActivationCharacters()
    {
        boolean useKeystrokes = editor.getActivePreferenceStore().getBoolean(SQLPreferenceConstants.ENABLE_KEYSTROKE_ACTIVATION);
        return useKeystrokes ?
            ".abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_$".toCharArray() :
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

        int fInstallOffset;

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

        ProposalSearchJob(SQLCompletionAnalyzer.CompletionRequest request) {
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