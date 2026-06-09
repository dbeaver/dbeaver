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
import org.jkiss.dbeaver.ui.config.sample.SampleDatabaseUtil;
import org.jkiss.dbeaver.ui.forms.UIAlignX;
import org.jkiss.dbeaver.ui.forms.UIGrowX;
import org.jkiss.dbeaver.ui.forms.UIObservable;
import org.jkiss.dbeaver.ui.forms.UIPanelBuilder;

import java.util.function.Consumer;

public class EasyConfigSampleDatabasePage extends EasyConfigWizardPage {
    private final UIObservable<Boolean> createSampleDatabase = UIObservable.of(true);

    public EasyConfigSampleDatabasePage() {
        super(
            UIObservable.of("Sample Database"),
            UIObservable.of("You can set up a sample database to explore DBeaver features without connecting to your own databases.")
        );
    }

    @Override
    public void createControl(@NotNull Composite parent) {
        setControl(UIPanelBuilder.build(parent, buildPanel(createSampleDatabase)));
    }

    @Override
    public boolean isPageApplicable() {
        return !isSampleDatabaseExists();
    }

    @NotNull
    private static Consumer<UIPanelBuilder> buildPanel(@NotNull UIObservable<Boolean> createSampleDatabase) {
        return pb -> pb
            .margins(10, 10)
            .row(rb -> rb.label(lb -> lb
                .text("DBeaver comes with a handy sample database powered by SQLite that you can use "
                    + "to explore the features and capabilities of the application.")
                .wrap()
                .align(UIAlignX.FILL)
                .grow(UIGrowX.ALWAYS)))
            .row(rb -> rb.checkBox("Create sample database", createSampleDatabase));
    }

    private static boolean isSampleDatabaseExists() {
        var project = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        if (project == null) {
            return true;
        }
        return SampleDatabaseUtil.isSampleDatabaseExists(project.getDataSourceRegistry());
    }
}
