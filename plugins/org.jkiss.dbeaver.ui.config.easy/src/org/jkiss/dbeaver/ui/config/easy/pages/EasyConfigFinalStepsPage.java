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
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.config.easy.nls.EasyConfigMessages;
import org.jkiss.dbeaver.ui.config.sample.SampleDatabaseUtil;
import org.jkiss.dbeaver.ui.forms.*;

import java.util.function.Consumer;

public class EasyConfigFinalStepsPage extends EasyConfigWizardPage {
    private final UIObservable<Boolean> createSampleDatabase = UIObservable.of(true);
    private final UIObservable<Boolean> showTips = UIObservable.of(true);

    public EasyConfigFinalStepsPage() {
        super(EasyConfigMessages.final_steps_title, EasyConfigMessages.final_steps_description);
    }

    @Override
    public void createControl(@NotNull Composite parent) {
        setControl(UIPanelBuilder.build(parent, buildPanel()));
    }

    @NotNull
    private Consumer<UIPanelBuilder> buildPanel() {
        return pb -> pb
            .margins(10, 10)
            .accept(buildSampleDatabasePanel(createSampleDatabase))
            .accept(buildTipsPanel(showTips))
            .row(rb -> rb.label(lb -> lb
                .text("You're all set! Click Finish to start using DBeaver.")
                .wrap()
                .align(UIAlignX.FILL)
                .grow(UIGrowX.ALWAYS)));
    }

    @NotNull
    private static Consumer<UIPanelBuilder> buildTipsPanel(@NotNull UIObservable<Boolean> showTips) {
        if (!canShowTipsOption()) {
            return UIRowBuilder.identityConsumer();
        }
        return pb -> pb
            .row(rb -> rb.label(lb -> lb
                .text("You can enable tips that will appear daily to help you get "
                    + "familiar with the application and learn some useful features.")
                .wrap()
                .align(UIAlignX.FILL)
                .grow(UIGrowX.ALWAYS)))
            .indent(pb1 -> pb1
                .row(rb -> rb.checkBox("Turn on \"Tip of the day\"", showTips)));
    }

    @NotNull
    private static Consumer<UIPanelBuilder> buildSampleDatabasePanel(@NotNull UIObservable<Boolean> createSampleDatabase) {
        if (!canShowSampleDatabaseOption()) {
            return UIRowBuilder.identityConsumer();
        }
        return pb -> pb
            .margins(10, 10)
            .row(rb -> rb.label(lb -> lb
                .text("DBeaver comes with a handy sample database powered by SQLite that you can use "
                    + "to explore the features and capabilities of the application.")
                .wrap()
                .align(UIAlignX.FILL)
                .grow(UIGrowX.ALWAYS)))
            .indent(pb1 -> pb1
                .row(rb -> rb.checkBox("Create sample database", createSampleDatabase)))
            .row(UIRowBuilder::horizontalSpacer);
    }

    private static boolean canShowTipsOption() {
        // FIXME we don't have access to tip of the day stuff because it's located in the standalone app
        //   bundle for some reason. We also can't just put this page there, as this page also
        //   features the sample database option, which can appear in the non-standalone app as well.
        return false;
    }

    private static boolean canShowSampleDatabaseOption() {
        var project = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        if (project == null) {
            return false;
        }
        // Don't show the option to create a sample database if it already exists in the workspace
        return !SampleDatabaseUtil.isSampleDatabaseExists(project.getDataSourceRegistry());
    }
}
