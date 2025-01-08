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
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.contentassist.*;
import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.jface.viewers.ISelection;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableParametrized;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionActivityTracker;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionAnalyzer;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionProposalBase;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionRequest;
import org.jkiss.dbeaver.model.sql.parser.SQLParserPartitions;
import org.jkiss.dbeaver.model.sql.parser.SQLWordPartDetector;
import org.jkiss.dbeaver.model.sql.registry.SQLCommandHandlerDescriptor;
import org.jkiss.dbeaver.model.sql.registry.SQLCommandsRegistry;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants.SQLAutocompletionMode;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQueryCompletionAnalyzer;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQueryCompletionProposal;
import org.jkiss.dbeaver.ui.editors.sql.templates.SQLContext;
import org.jkiss.dbeaver.ui.editors.sql.templates.SQLTemplateCompletionProposal;
import org.jkiss.dbeaver.ui.editors.sql.templates.SQLTemplatesRegistry;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * The SQL content assist processor. This content assist processor proposes text
 * completions and computes context information for a SQL content type.
 */
public class SQLCompletionProcessor implements IContentAssistProcessor
{
    private static final Log log = Log.getLog(SQLCompletionProcessor.class);

    private static final IContextInformationValidator VALIDATOR = new Validator();
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
    private SQLContentAssistant contentAssistant;

    public SQLCompletionProcessor(SQLEditorBase editor)
    {
        this.editor = editor;
    }

    public void initAssistant(SQLContentAssistant contentAssistant) {
        contentAssistant.addCompletionListener(new CompletionListener());
        this.contentAssistant = contentAssistant;
    }

    @Override
    public ICompletionProposal[] computeCompletionProposals(
        ITextViewer viewer,
        int documentOffset
    ) {
        IDocument document = editor.getDocument();
        if (document == null) {
            return new ICompletionProposal[0];
        }
        // This method blocks UI thread to prepare proposals, but in our implementation actual analysis performed in background jobs
        // asynchronously with the editor's state modifications due to waitJobCompletion(..) usage, which forcibly pumps UI event loop.
        // Accurate proposals preparation requires cursor position of where the user stops editing the text,
        // so we are synchronously creating a position object to track the location of interest, instead of the documentOffset
        // (which is where the proposals computation was initially triggered, not where it is really expected to get them).
        Position completionRequestPosition = new Position(documentOffset);
        try {
            IRegion line = document.getLineInformationOfOffset(documentOffset);
            if (documentOffset <= line.getLength() + line.getOffset() && line.getLength() > 0) { // we are in the nonempty line
                String typeAtLine = TextUtilities.getContentType(document, SQLParserPartitions.SQL_PARTITIONING, documentOffset - 1, true);
                // and previous position belongs to the single-line comment
                if (SQLParserPartitions.CONTENT_TYPE_SQL_COMMENT.equals(typeAtLine)) {
                    return new ICompletionProposal[0];
                }
            }
        } catch (BadLocationException e) {
            log.debug(e);
            return new ICompletionProposal[0];
        }

        final SQLCompletionRequest request = new SQLCompletionRequest(
            editor.getCompletionContext(),
            document,
            documentOffset,
            editor.extractQueryAtPos(documentOffset),
            simpleMode);
        SQLWordPartDetector wordDetector = request.getWordDetector();


        String contentType;
        try {
            // Check that word start position is in default partition (#5994)
            contentType = TextUtilities.getContentType(document, SQLParserPartitions.SQL_PARTITIONING, documentOffset, true);
        } catch (BadLocationException e) {
            log.debug(e);
            return new ICompletionProposal[0];
        }

        if (contentType == null) {
            return new ICompletionProposal[0];
        }

        request.setContentType(contentType);

        List<?> proposals;
        try {
            switch (contentType) {
                case IDocument.DEFAULT_CONTENT_TYPE:
                case SQLParserPartitions.CONTENT_TYPE_SQL_STRING:
                case SQLParserPartitions.CONTENT_TYPE_SQL_QUOTED:
                    if (lookupTemplates) {
                        return makeTemplateProposals(viewer, request);
                    }

                    try {
                        String commandPrefix = editor.getSyntaxManager().getControlCommandPrefix();
                        if (commandPrefix != null && wordDetector.getStartOffset() >= commandPrefix.length() &&
                            viewer.getDocument().get(wordDetector.getStartOffset() - commandPrefix.length(), commandPrefix.length()).equals(commandPrefix)
                        ) {
                            return makeCommandProposals(request, request.getWordPart());
                        }
                        document.addPosition(completionRequestPosition);
                    } catch (BadLocationException e) {
                        log.debug(e);
                    }

                    DBPDataSource dataSource = editor.getDataSource();

                    SQLAutocompletionMode mode = SQLAutocompletionMode.fromPreferences(this.editor.getActivePreferenceStore());

                    // UIUtils.waitJobCompletion(..) uses job.isFinished() which is not dropped on reschedule,
                    // so we should be able to recreate the whole job object including all its non-reusable dependencies.
                    List<Supplier<ProposalsComputationJobHolder>> completionJobSuppliers = new ArrayList<>();

                    if (request.getWordPart() != null && mode.useOldAnalyzer) {
                        if (dataSource != null) {
                            completionJobSuppliers.add(() -> {
                                // old analyzer is not reusable, but it doesn't matter because see the next comment below
                                SQLCompletionAnalyzer analyzer = new SQLCompletionAnalyzer(request);
                                return new ProposalsComputationJobHolder(new ProposalSearchJob(analyzer)) {
                                    @Override
                                    public List<?> getProposals() {
                                        return analyzer.getProposals();
                                    }

                                    @Override
                                    public Integer getProposalsOriginOffset() {
                                        // Actual origin for old analyzer's proposals is always request.getDocumentOffset(),
                                        // which may be out of sync with cursor position at the time of analysis being accomplished.
                                        // This is wrong, but old implementation ignores this fact,
                                        // so for now we explicitly pretend that it is always in sync with current cursor position.
                                        // Possible solution: support request object recreation when job restart needed.
                                        return null;
                                    }
                                };
                            });
                        }
                    }

                    if (mode.useNewAnalyzer) {
                        // new analyzer is reusable
                        SQLQueryCompletionAnalyzer newAnalyzer = new SQLQueryCompletionAnalyzer(this.editor, request, completionRequestPosition);
                        completionJobSuppliers.add(() -> new ProposalsComputationJobHolder(new NewProposalSearchJob(newAnalyzer)) {
                            @Override
                            public List<?> getProposals() {
                                return newAnalyzer.getResult();
                            }

                            @Override
                            public Integer getProposalsOriginOffset() {
                                return newAnalyzer.getActualContextOffset();
                            }
                        });
                    }

                    proposals = this.computeProposalsWithJobs(request, completionRequestPosition, completionJobSuppliers);
                    break;
                default:
                    proposals = Collections.emptyList();
                    break;
            }

            int actualCompletionOffset = completionRequestPosition.getOffset();
            List<ICompletionProposal> result = new ArrayList<>(proposals.size());
            if (actualCompletionOffset != request.getDocumentOffset()) {
                for (Object cp : proposals) {
                    if (cp instanceof ICompletionProposal proposal && (
                        (cp instanceof ICompletionProposalExtension2 exp && exp.validate(request.getDocument(), completionRequestPosition.getOffset(), null))
                        || !(cp instanceof ICompletionProposalExtension2)
                    )) {
                        result.add(proposal);
                    }
                }
            } else {
                for (Object cp : proposals) {
                    if (cp instanceof ICompletionProposal proposal) {
                        result.add(proposal);
                    }
                }
            }
            this.contentAssistant.setLastCompletionOffset(actualCompletionOffset);
            return ArrayUtils.toArray(ICompletionProposal.class, result);
        } finally {
            document.removePosition(completionRequestPosition);
        }
    }

    private List<?> computeProposalsWithJobs(
        @NotNull SQLCompletionRequest request,
        @NotNull Position completionRequestPosition,
        @NotNull List<Supplier<ProposalsComputationJobHolder>> completionJobSuppliers
    ) {
        List<ProposalsComputationJobHolder> completionJobs = completionJobSuppliers.stream()
            .map(Supplier::get).collect(Collectors.toList()); // modifiable list!
        boolean hasRunningJobs = true;
        while (hasRunningJobs) {
            // wait for jobs to run asynchronously
            completionJobs.forEach(j -> UIUtils.waitJobCompletion(j.job));
            hasRunningJobs = false;
            // if any job was cancelled while running in background, break the whole proposals computation logic
            if (completionJobs.stream().anyMatch(j -> j.job.isCanceled())) {
                return Collections.emptyList();
            } else {
                // all the jobs succeed, validate if the result is applicable
                int currentOffset = completionRequestPosition.getOffset();
                for (int i = 0; i < completionJobs.size(); i++) {
                    Integer origin = completionJobs.get(i).getProposalsOriginOffset();
                    if (origin != null) {
                        ProposalsJobResultStatus jobResultStatus = evaluateJobResult(request, origin, currentOffset);
                        switch (jobResultStatus) {
                            case VALID: // do nothing, all proposals are ok
                            case PARTIALLY_VALID: // do nothing, proposals will be filtered with validation when supported
                                break;
                            case INVALID: // job restart required
                                completionJobs.set(i, completionJobSuppliers.get(i).get());
                                hasRunningJobs = true;
                                break;
                            case ABORT:
                            default:
                                return Collections.emptyList();
                        }
                    }
                }
            }
        }
        return completionJobs.stream().flatMap(j -> j.getProposals().stream()).toList();
    }

    private enum ProposalsJobResultStatus {
        VALID,
        PARTIALLY_VALID,
        INVALID,
        ABORT
    }

    private static ProposalsJobResultStatus evaluateJobResult(SQLCompletionRequest request, int jobProposalsOrigin, int currentCursorPosition) {
        // Proposals were successfully prepared by the job,
        // but they still might be a bit out-of-sync with the cursor position.
        if (jobProposalsOrigin > currentCursorPosition) {
            // There were removals earlier than completion list appeared on the screen, so
            // prepared proposals are not relevant anymore.
            if (currentCursorPosition < request.getDocumentOffset()) {
                // The cursor is positioned before the initial request offset.
                // Eclipse drops completions list in this case anyway.
                return ProposalsJobResultStatus.ABORT;
            } else {
                // The cursor is positioned before the initial request offset,
                // but the job handled some insertions, which were then removed.
                // Let's restart the job.
                return ProposalsJobResultStatus.INVALID;
            }
        } else if (jobProposalsOrigin < currentCursorPosition) {
            // There were extra insertions at the moment when the job didn't handle them on the fly.
//            String unconsideredInput = null;
//            try {
//                unconsideredInput = request.getDocument().get(jobProposalsOrigin, currentCursorPosition - jobProposalsOrigin);
//            } catch (BadLocationException e) {
//                log.error("Failed to prepare completion proposals", e);
//                return ProposalsJobResultStatus.ABORT;
//            }

//            if (IDENTIFIER_PART_PATTERN.matcher(unconsideredInput).matches()) {
                // The inserted text fragment can be considered as part of a certain word or identifier,
                // so it's enough to do proposals validation to filter them out.
                return ProposalsJobResultStatus.PARTIALLY_VALID;
//            } else {
//                // The word structure was disrupted by non-identifier characters, so lets restart the job.
//                return ProposalsJobResultStatus.INVALID;
//            }
        } else {
            return ProposalsJobResultStatus.VALID;
        }
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
            if (template.getName().toLowerCase().startsWith(wordPart)
                && SQLEditorUtils.isTemplateContextFitsEditorContext(template.getContextTypeId(), editor)
            ) { 
                SQLContext templateContext = new SQLContext(
                    SQLTemplatesRegistry.getInstance().getTemplateContextRegistry().getContextType(template.getContextTypeId()),
                    viewer.getDocument(),
                    new Position(request.getWordDetector().getStartOffset(), request.getWordDetector().getLength()),
                    editor);
                ISelection selection = viewer.getSelectionProvider().getSelection();
                if (selection instanceof TextSelection) {
                    templateContext.setVariable(GlobalTemplateVariables.SELECTION, ((TextSelection) selection).getText());
                }
                templateProposals.add(new SQLTemplateCompletionProposal(
                    template,
                    templateContext,
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
        return useKeystrokes ? ".abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_$".toCharArray() : new char[] {'.', };
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

    private class CompletionListener implements ICompletionListener, ICompletionListenerExtension {

        @Override
        public void assistSessionStarted(ContentAssistEvent event) {
            SQLCompletionProcessor.setSimpleMode(event.isAutoActivated);
            contentAssistant.assistSessionStarted(event);
        }

        @Override
        public void assistSessionEnded(ContentAssistEvent event) {
            simpleMode = false;
            contentAssistant.setLastCompletionOffset(-1);
        }

        @Override
        public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
            SQLCompletionActivityTracker activityTracker =
                proposal instanceof SQLQueryCompletionProposal p ? p.getProposalContext().getActivityTracker() :
                proposal instanceof SQLCompletionProposalBase p ? p.getRequest().getActivityTracker() : null;

            if (activityTracker != null) {
                activityTracker.selectionChanged();
            }
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
        public void install(IContextInformation info, ITextViewer viewer, int offset)
        {
            fInstallOffset = offset;
        }

        @Override
        public boolean updatePresentation(int documentPosition, TextPresentation presentation)
        {
            return false;
        }
    }

    private class ProposalSearchJob extends AbstractJob {
        private final DBRRunnableParametrized<DBRProgressMonitor> analyzer;

        ProposalSearchJob(DBRRunnableParametrized<DBRProgressMonitor> analyzer) {
            super("Search proposals...");
            this.analyzer = analyzer;
            setSystem(true);
            setUser(false);
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

    private class NewProposalSearchJob extends AbstractJob {
        private final SQLQueryCompletionAnalyzer analyzer;

        public NewProposalSearchJob(SQLQueryCompletionAnalyzer analyzer) {
            super("Analyzing query for proposals...");
            this.analyzer = analyzer;
        }
        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            try {
                monitor.beginTask("Seeking for SQL completion proposals", 2);
                monitor.worked(1);
                try {
                    monitor.subTask("Find proposals");
                    if (editor.getDataSource() != null) {
                        DBExecUtils.tryExecuteRecover(monitor, editor.getDataSource(), this.analyzer);
                    } else {
                        this.analyzer.run(monitor);
                    }
                } finally {
                    monitor.done();
                }
                return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
            } catch (Throwable e) {
                log.error(e);
                return Status.CANCEL_STATUS;
            }
        }
    }

    private abstract static class ProposalsComputationJobHolder {
        public final AbstractJob job;

        public ProposalsComputationJobHolder(AbstractJob job) {
            this.job = job;
            this.job.schedule();
        }

        public abstract List<?> getProposals();

        public abstract Integer getProposalsOriginOffset();
    }
}
