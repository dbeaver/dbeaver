/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.core.expressions.Expression;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.services.IEvaluationReference;
import org.eclipse.ui.services.IEvaluationService;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.controls.VerticalButton;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLEditorHandlerSwitchPresentation;
import org.jkiss.dbeaver.ui.editors.sql.registry.SQLPresentationDescriptor;
import org.jkiss.dbeaver.ui.editors.sql.registry.SQLPresentationPanelDescriptor;
import org.jkiss.dbeaver.ui.editors.sql.registry.SQLPresentationRegistry;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

class ExtraPresentationManager {
    @NotNull
    private final SQLEditor owner;
    final Map<SQLPresentationDescriptor, SQLEditorPresentation> presentations = new LinkedHashMap<>();
    final Map<SQLPresentationPanelDescriptor, SQLEditorPresentationPanel> panels = new HashMap<>();
    final Map<SQLPresentationDescriptor, Integer> presentationStackIndices = new HashMap<>();

    SQLPresentationDescriptor activePresentationDescriptor;
    SQLEditorPresentation activePresentation;
    SQLEditorPresentationPanel activePresentationPanel;

    public ExtraPresentationManager(@NotNull SQLEditor owner) {
        this.owner = owner;
        for (SQLPresentationDescriptor presentation : SQLPresentationRegistry.getInstance().getPresentations()) {
            presentations.put(presentation, null);

            for (SQLPresentationPanelDescriptor panel : presentation.getPanels()) {
                panels.put(panel, null);
            }
        }
    }

    public boolean setActivePresentation(@Nullable SQLPresentationDescriptor descriptor) throws DBException {
        if (this.owner.getPresentationStack() == null || activePresentationDescriptor == descriptor) {
            // Same presentation, no op
            return true;
        }

        if (activePresentation != null && !activePresentation.canHidePresentation(this.owner)) {
            // Presentation decided not to close
            return false;
        }

        if (descriptor == null) {
            // Just hide presentation
            activePresentationDescriptor = null;
            activePresentation = null;
            activePresentationPanel = null;

            SQLEditorPropertyTester.firePropertyChange(SQLEditorPropertyTester.PROP_CAN_EXECUTE);
            return true;
        }

        SQLEditorPresentation presentation = presentations.get(descriptor);

        if (presentation == null) {
            presentation = descriptor.createPresentation();

            if (presentation.canShowPresentation(this.owner, true)) {
                // Must be done before doing something to presentationStack
                presentationStackIndices.put(descriptor, this.owner.getPresentationStack().getChildren().length);

                final Composite placeholder = new Composite(this.owner.getPresentationStack(), SWT.NONE);
                placeholder.setLayout(new FillLayout());

                if (activePresentation != null) {
                    activePresentation.hidePresentation(this.owner);
                }

                activePresentationDescriptor = descriptor;
                activePresentation = presentation;
                activePresentation.createPresentation(placeholder, this.owner);
                activePresentation.showPresentation(this.owner, true);
                presentations.put(descriptor, activePresentation);

                SQLEditorPropertyTester.firePropertyChange(SQLEditorPropertyTester.PROP_CAN_EXECUTE);

                return true;
            }
        } else {
            if (presentation.canShowPresentation(this.owner, false)) {
                if (activePresentation != null) {
                    activePresentation.hidePresentation(this.owner);
                }

                activePresentationDescriptor = descriptor;
                activePresentation = presentation;
                activePresentation.showPresentation(this.owner, false);

                SQLEditorPropertyTester.firePropertyChange(SQLEditorPropertyTester.PROP_CAN_EXECUTE);

                return true;
            }
        }

        return false;
    }

    @Nullable
    Control getActivePresentationControl() {
        if (this.owner.getPresentationStack() == null || activePresentationDescriptor == null) {
            return null;
        }
        final int index = presentationStackIndices.get(activePresentationDescriptor);
        return this.owner.getPresentationStack().getChildren()[index];
    }

    @NotNull
    VerticalButton createPresentationButton(@NotNull SQLPresentationDescriptor presentation) {
        final VerticalButton button = new VerticalButton(owner.getPresentationSwitchFolder(), SWT.RIGHT | SWT.CHECK);
        button.setData(presentation);
        button.setText(presentation.getLabel());
        button.setImage(DBeaverIcons.getImage(presentation.getIcon()));

        final String toolTip = ActionUtils.findCommandDescription(
            SQLEditorHandlerSwitchPresentation.CMD_SWITCH_PRESENTATION_ID, this.owner.getSite(), true,
            SQLEditorHandlerSwitchPresentation.PARAM_PRESENTATION_ID, presentation.getId()
        );

        if (CommonUtils.isEmpty(toolTip)) {
            button.setToolTipText(presentation.getDescription());
        } else {
            button.setToolTipText(presentation.getDescription() + " (" + toolTip + ")");
        }

        final IEvaluationService evaluationService = this.owner.getSite().getService(IEvaluationService.class);
        final Expression enabledWhen = presentation.getEnabledWhen();

        if (evaluationService != null && enabledWhen != null) {
            final IEvaluationReference reference = evaluationService.addEvaluationListener(
                enabledWhen,
                event -> handlePresentationEnablement(button, presentation, CommonUtils.toBoolean(event.getNewValue())),
                "enabled"
            );

            button.addDisposeListener(e -> evaluationService.removeEvaluationListener(reference));
        }

        return button;
    }

    private void handlePresentationEnablement(
        @NotNull VerticalButton button,
        @NotNull SQLPresentationDescriptor presentation,
        boolean enabled
    ) {
        if (this.owner.isDisposed()) {
            return;
        }

        if (!enabled && activePresentationDescriptor == presentation) {
            this.owner.showExtraPresentation((SQLPresentationDescriptor) null);
        }

        button.setVisible(enabled);
        button.getParent().layout(true, true);
    }

    public void dispose() {
        activePresentationDescriptor = null;
        activePresentation = null;
        activePresentationPanel = null;

        for (SQLEditorPresentation presentation : presentations.values()) {
            if (presentation != null) {
                presentation.dispose();
            }
        }

        presentations.clear();
        panels.clear();
    }
}
