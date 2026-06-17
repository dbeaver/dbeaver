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
package org.jkiss.dbeaver.ui.config.easy.pages;

import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.config.easy.nls.EasyConfigMessages;
import org.jkiss.dbeaver.ui.config.easy.registry.EasyConfigAction;
import org.jkiss.dbeaver.ui.config.easy.registry.EasyConfigActionDescriptor;
import org.jkiss.dbeaver.ui.config.easy.registry.EasyConfigRegistry;
import org.jkiss.dbeaver.ui.forms.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EasyConfigFinalStepsPage extends EasyConfigWizardPage {
    private static final Log log = Log.getLog(EasyConfigFinalStepsPage.class);

    private final List<ActionState> states = new ArrayList<>();

    public EasyConfigFinalStepsPage() {
        super(EasyConfigMessages.final_steps_title, EasyConfigMessages.final_steps_description);
    }

    @Override
    public void createControl(@NotNull Composite parent) {
        setControl(UIPanelBuilder.build(parent, buildPanel()));
    }

    @Override
    public void applySettings() {
        for (ActionState state : states) {
            state.action().applyState(state.value().get());
        }
    }

    @NotNull
    private Consumer<UIPanelBuilder> buildPanel() {
        return pb -> pb
            .margins(10, 10)
            .accept(buildActionsPanel())
            .row(rb -> rb.label(lb -> lb
                .text(EasyConfigMessages.final_steps_header)
                .wrap()
                .align(UIAlignX.FILL)
                .grow(UIGrowX.ALWAYS)));
    }

    @NotNull
    private Consumer<UIPanelBuilder> buildActionsPanel() {
        return pb -> {
            for (EasyConfigActionDescriptor descriptor : EasyConfigRegistry.getInstance().getActions()) {
                EasyConfigAction.OfCheckbox action;
                try {
                    action = (EasyConfigAction.OfCheckbox) descriptor.createAction();
                } catch (DBException e) {
                    log.error("Error creating easy config action " + descriptor.getLabel(), e);
                    continue;
                }
                if (!action.isApplicable()) {
                    continue;
                }
                var value = UIObservable.of(action.loadState());
                pb.accept(buildAction(descriptor.getLabel(), descriptor.getDescription(), value));
                states.add(new ActionState(action, value));
            }
        };
    }

    @NotNull
    private Consumer<UIPanelBuilder> buildAction(
        @NotNull String label,
        @NotNull String description,
        @NotNull UIObservable<Boolean> value
    ) {
        return pb -> pb
            .row(rb -> rb.label(lb -> lb
                .text(description)
                .wrap()
                .align(UIAlignX.FILL)
                .grow(UIGrowX.ALWAYS)))
            .indent(pb1 -> pb1
                .row(rb -> rb.checkBox(label, value)))
            .row(UIRowBuilder::horizontalSpacer);
    }

    private record ActionState(@NotNull EasyConfigAction.OfCheckbox action, @NotNull UIObservable<Boolean> value) {
    }
}
