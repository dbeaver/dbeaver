/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.eclipse.jface.text.templates.TemplateProposal;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionAnalyzer;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionProposalBase;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionRequest;
import org.jkiss.dbeaver.model.sql.parser.SQLParserPartitions;
import org.jkiss.dbeaver.model.sql.parser.SQLWordPartDetector;
import org.jkiss.dbeaver.model.sql.registry.SQLCommandHandlerDescriptor;
import org.jkiss.dbeaver.model.sql.registry.SQLCommandsRegistry;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.editors.sql.templates.SQLContext;
import org.jkiss.dbeaver.ui.editors.sql.templates.SQLTemplateCompletionProposal;
import org.jkiss.dbeaver.ui.editors.sql.templates.SQLTemplatesRegistry;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The SQL content assist processor. This content assist processor proposes text
 * completions and computes context information for a SQL content type.
 */
public class SQLCompletionProcessor implements IContentAssistProcessor
{
    private static final Log log = Log.getLog(SQLCompletionProcessor.class);

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

    public void initAssistant(SQLContentAssistant contentAssistant) {
        contentAssistant.addCompletionListener(new CompletionListener());
    }

    @Override
    public ICompletionProposal[] computeCompletionProposals(
        ITextViewer viewer,
        int documentOffset)
    {
        IDocument document = editor.getDocument();
        if (document == null) {
            return new ICompletionProposal[0];
        }
        final SQLCompletionRequest request = new SQLCompletionRequest(
            editor.getCompletionContext(),
            document,
            documentOffset,
            editor.extractQueryAtPos(documentOffset),
            simpleMode);
        SQLWordPartDetector wordDetector = request.getWordDetector();


        try {
            // Check that word start position is in default partition (#5994)
            String contentType = TextUtilities.getContentType(document, SQLParserPartitions.SQL_PARTITIONING, wordDetector.getStartOffset(), true);
            if (contentType == null || (!IDocument.DEFAULT_CONTENT_TYPE.equals(contentType) && !SQLParserPartitions.CONTENT_TYPE_SQL_QUOTED.equals(contentType))) {
                return new ICompletionProposal[0];
            }
        } catch (BadLocationException e) {
            log.debug(e);
            return new ICompletionProposal[0];
        }

        if (lookupTemplates) {
            return makeTemplateProposals(viewer, request);
        }

        try {
            String commandPrefix = editor.getSyntaxManager().getControlCommandPrefix();
            if (wordDetector.getStartOffset() >= commandPrefix.length() &&
                viewer.getDocument().get(wordDetector.getStartOffset() - commandPrefix.length(), commandPrefix.length()).equals(commandPrefix))
            {
                return makeCommandProposals(request, request.getWordPart());
            }
        } catch (BadLocationException e) {
            log.debug(e);
        }


        SQLCompletionAnalyzer analyzer = new SQLCompletionAnalyzer(request);
        DBPDataSource dataSource = editor.getDataSource();
        if (request.getWordPart() != null) {
            if (dataSource != null) {
                ProposalSearchJob searchJob = new ProposalSearchJob(analyzer);
                searchJob.schedule();
                // Wait until job finished
                UIUtils.waitJobCompletion(searchJob);
            }
        }

        List<SQLCompletionProposalBase> proposals = analyzer.getProposals();
        List<ICompletionProposal> result = new ArrayList<>();
        for (SQLCompletionProposalBase cp : proposals) {
            if (cp instanceof ICompletionProposal) {
                result.add((ICompletionProposal) cp);
            }
        }

        return ArrayUtils.toArray(ICompletionProposal.class, result);
    }

    private ICompletionProposal[] makeCommandProposals(SQLCompletionRequest request, String prefix) {
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
        return commandProposals.toArray(new ICompletionProposal[0]);
    }

    @NotNull
    private ICompletionProposal[] makeTemplateProposals(ITextViewer viewer, SQLCompletionRequest request) {
        String wordPart = request.getWordPart().toLowerCase();
        final List<SQLTemplateCompletionProposal> templateProposals = new ArrayList<>();
        // Templates
        for (Template template : editor.getTemplatesPage().getTemplateStore().getTemplates()) {
            if (template.getName().toLowerCase().startsWith(wordPart)) {
                templateProposals.add(new SQLTemplateCompletionProposal(
                    template,
                    new SQLContext(
                        SQLTemplatesRegistry.getInstance().getTemplateContextRegistry().getContextType(template.getContextTypeId()),
                        viewer.getDocument(),
                        new Position(request.getWordDetector().getStartOffset(), request.getWordDetector().getLength()),
                        editor),
                    new Region(request.getDocumentOffset(), 0),
                    null));
            }
        }
        templateProposals.sort(Comparator.comparing(TemplateProposal::getDisplayString));
        return templateProposals.toArray(new ICompletionProposal[0]);
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

    private static class CompletionListener implements ICompletionListener, ICompletionListenerExtension {

        @Override
        public void assistSessionStarted(ContentAssistEvent event) {

        }

        @Override
        public void assistSessionEnded(ContentAssistEvent event) {
            simpleMode = false;
        }

        @Override
        public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {

        }

        @Override
        public void assistSessionRestarted(ContentAssistEvent event) {
            simpleMode = true;
        }
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
        private final SQLCompletionAnalyzer analyzer;

        ProposalSearchJob(SQLCompletionAnalyzer analyzer) {
            super("Search proposals...");
            setSystem(false);
            this.analyzer = analyzer;
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            try {
                monitor.beginTask("Seeking for SQL completion proposals", 1);
                try {
                    monitor.subTask("Find proposals");
                    DBExecUtils.tryExecuteRecover(monitor, editor.getDataSource(), analyzer);
                } finally {
                    monitor.done();
                }
                return Status.OK_STATUS;
            } catch (Throwable e) {
                log.error(e);
                return Status.CANCEL_STATUS;
            }
        }

    }

}
