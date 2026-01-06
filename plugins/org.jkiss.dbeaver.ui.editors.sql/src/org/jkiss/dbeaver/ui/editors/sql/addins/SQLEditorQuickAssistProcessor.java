/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.addins;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.ui.internal.texteditor.spelling.NoCompletionsProposal;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class SQLEditorQuickAssistProcessor implements IQuickAssistProcessor {
    private static final Log log = Log.getLog(SQLEditorQuickAssistProcessor.class);

    @NotNull
    private static final ICompletionProposal[] noSuggestionsProposal =  new ICompletionProposal[] {
        new NoCompletionsProposal()
    };

    private record QuickFixHandler(
        @Nullable
        SQLEditorQuickFixProcessorDescriptor descriptor,
        @NotNull
        IQuickAssistProcessor processor
    ) {
    }

    @NotNull
    private final List<QuickFixHandler> processors = new ArrayList<>();

    @NotNull
    private final SQLEditorBase editor;

    public SQLEditorQuickAssistProcessor(@NotNull SQLEditorBase editor) {
        this.editor = editor;
    }

    public void appendProcessor(@NotNull IQuickAssistProcessor quickAssistProcessor) {
        processors.add(new QuickFixHandler(null, quickAssistProcessor));
    }

    public void appendProcessors(@NotNull Collection<SQLEditorQuickFixProcessorDescriptor> quickFixProcessorDescriptors) {
        for (SQLEditorQuickFixProcessorDescriptor d : quickFixProcessorDescriptors) {
            try {
                processors.add(new QuickFixHandler(d, d.createInstance()));
            } catch (DBException e) {
                log.error("Can't load quick fix handler '" + d.getId() + "'", e);
            }
        }
    }

    @Override
    @Nullable
    public String getErrorMessage() {
        List<String> errorMessages = processors.stream().map(p -> p.processor.getErrorMessage()).toList();
        if (errorMessages.stream().anyMatch(CommonUtils::isEmpty)) {
            return null;
        } else {
            return errorMessages.stream().filter(CommonUtils::isNotEmpty).distinct().collect(Collectors.joining("\n"));
        }
    }

    @Override
    public boolean canFix(@NotNull Annotation annotation) {
        return processors.stream().anyMatch(
            p -> (p.descriptor == null || p.descriptor.handlesAnnotation(annotation)) && p.processor.canFix(annotation)
        );
    }

    @Override
    public boolean canAssist(@NotNull IQuickAssistInvocationContext invocationContext) {
        return processors.stream().anyMatch(
            p -> (p.descriptor == null || p.descriptor.isEnabled(editor.getEditorSite())) && p.processor.canAssist(invocationContext)
        );
    }

    @Override
    @NotNull
    public ICompletionProposal[] computeQuickAssistProposals(@NotNull IQuickAssistInvocationContext invocationContext) {
        ICompletionProposal[] proposals = processors.stream()
            .flatMap(p -> Arrays.stream(p.processor.computeQuickAssistProposals(invocationContext)))
            .filter(p -> !(p instanceof NoCompletionsProposal))
            .toArray(ICompletionProposal[]::new);
        return ArrayUtils.isEmpty(proposals) ? noSuggestionsProposal : proposals;
    }
}
