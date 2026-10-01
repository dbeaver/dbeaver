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
package org.jkiss.dbeaver.ui.ai.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.internal.AIUIMessages;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

/**
 * Base class for AI engine settings configurators.
 */
public abstract class AbstractAIEngineConfigurator<ENGINE extends AIEngineDescriptor, PROPERTIES extends AIEngineProperties>
    implements AIIObjectPropertyConfigurator<ENGINE, PROPERTIES> {

    @Nullable
    private Text timeoutText;
    @Nullable
    private Button logQueryCheck;

    private int timeout = AIEngineProperties.DEFAULT_TIMEOUT;
    private boolean logQuery;

    @NotNull
    protected Composite createAdvancedSettings(@NotNull Composite parent) {
        ExpandableComposite advancedSettings = UIUtils.createExpandableCompositeWithSeparator(
            parent,
            SWT.NONE,
            ExpandableComposite.TWISTIE | ExpandableComposite.COMPACT
        );
        advancedSettings.setText(AIUIMessages.ai_engine_configurator_advanced_settings);
        GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        gd.horizontalSpan = parent.getLayout() instanceof GridLayout gridLayout ? gridLayout.numColumns : 1;
        advancedSettings.setLayoutData(gd);

        Composite client = UIUtils.createComposite(advancedSettings, 2);

        timeoutText = UIUtils.createLabelText(
            client,
            AIUIMessages.ai_engine_configurator_timeout_label,
            String.valueOf(timeout),
            SWT.BORDER
        );
        timeoutText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        timeoutText.setToolTipText(AIUIMessages.ai_engine_configurator_timeout_tip);
        timeoutText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
        timeoutText.addModifyListener(e ->
            timeout = CommonUtils.toInt(timeoutText.getText(), AIEngineProperties.DEFAULT_TIMEOUT));

        logQueryCheck = UIUtils.createCheckbox(
            client,
            AIUIMessages.openai_configurator_log_query_label,
            AIUIMessages.openai_configurator_log_query_tip,
            false,
            2
        );
        logQueryCheck.addSelectionListener(
            SelectionListener.widgetSelectedAdapter(e -> logQuery = logQueryCheck.getSelection()));

        advancedSettings.setClient(client);
        advancedSettings.setExpanded(false);
        advancedSettings.addExpansionListener(new ExpansionAdapter() {
            @Override
            public void expansionStateChanged(@NotNull ExpansionEvent e) {
                parent.getShell().layout(true, true);
            }
        });
        return client;
    }

    protected void loadAdvancedSettings(@NotNull AIEngineProperties configuration) {
        timeout = configuration.getTimeout();
        logQuery = CommonUtils.toBoolean(configuration.isLoggingEnabled());
        applyAdvancedSettings();
    }

    protected void applyAdvancedSettings() {
        if (timeoutText != null && !timeoutText.isDisposed()) {
            timeoutText.setText(String.valueOf(timeout));
        }
        if (logQueryCheck != null && !logQueryCheck.isDisposed()) {
            logQueryCheck.setSelection(logQuery);
        }
    }

    protected void saveAdvancedSettings(@NotNull AIEngineProperties configuration) {
        configuration.setTimeout(timeout);
        configuration.setLoggingEnabled(logQuery);
    }
}
