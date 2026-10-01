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
package org.jkiss.dbeaver.ui.ai.chat;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.*;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.AIContextSettings;
import org.jkiss.dbeaver.model.ai.registry.AISettingsEventListener;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.chat.controls.AIChatControl;
import org.jkiss.dbeaver.ui.ai.internal.AIUIFeatures;

public class AIChatView extends ViewPart {
    private static final Log log = Log.getLog(AIChatView.class);
    public static final String CONTEXT_ID = "com.dbeaver.ai.chat"; //$NON-NLS-1$

    private AIChatControl chat;
    private AIChatViewHandler handler;
    private AISettingsEventListener aiSettingsEventListener;

    @Override
    public void createPartControl(@NotNull Composite parent) {
        chat = new AIChatControl(parent, new AIChatControllerMain());
        handler = new AIChatViewHandler(this);

        IContextService service = getSite().getService(IContextService.class);
        if (service != null) {
            service.activateContext(CONTEXT_ID); //$NON-NLS-1$
        }

        AIContextSettings settings = chat.getCompletionSettings();

        DBPDataSourceContainer container = settings != null ? settings.getDataSourceContainer() : null;
        AIUIFeatures.AI_CHAT.use(AIUIFeatures.buildFeatureParameters(container));

        aiSettingsEventListener = registry -> {
            if (registry.getSettings().isAiDisabled()) {
                // What are we doing here?
                hide();
            }
        };
        AISettingsManager.getInstance().addChangedListener(aiSettingsEventListener);
    }

    @Override
    public void setFocus() {
        chat.setFocusOnPrompt();
    }

    @Override
    public void dispose() {
        super.dispose();

        if (handler != null) {
            handler.dispose();
        }
        if (aiSettingsEventListener != null) {
            AISettingsManager.getInstance().removeChangedListener(aiSettingsEventListener);
            aiSettingsEventListener = null;
        }

        // AIChatUtils.updateChatToggleContribution();
    }

    @NotNull
    public static AIChatView show() throws DBException {
        IWorkbenchWindow window = UIUtils.getActiveWorkbenchWindow();
        IWorkbenchPage page = window.getActivePage();
        IViewPart view = UIUtils.findView(window, IActionConstants.CHAT_VIEW_ID);

        if (view != null) {
            page.bringToTop(view);
            return (AIChatView) view;
        }

        try {
            return (AIChatView) page.showView(IActionConstants.CHAT_VIEW_ID);
        } catch (PartInitException e) {
            throw new DBException("Can't open AI chat view", e);
        }
    }

    public void hide() {
        IWorkbenchWindow window = UIUtils.getActiveWorkbenchWindow();
        IWorkbenchPage page = window.getActivePage();
        page.hideView(this);
    }

    @NotNull
    public AIChatControl getChat() {
        return chat;
    }

    private class AIChatControllerMain extends AIChatControllerBase {
        @Override
        public int getChatFeatures() {
            return FEATURE_CONTEXT_VIEW | FEATURE_PROMPT_VIEW | FEATURE_AUDIO_TRANSCRIPT;
        }

        @Nullable
        @Override
        public DBCExecutionContext getExecutionContext() {
            IWorkbenchPart activeEditor = UIUtils.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
            if (activeEditor instanceof DBPContextProvider contextProvider) {
                return contextProvider.getExecutionContext();
            }
            return null;
        }

        @NotNull
        @Override
        public IWorkbenchPartSite getSite() {
            return AIChatView.this.getSite();
        }

        @Nullable
        @Override
        public AIContextSettings getContextSettings() {
            return chat.getCompletionSettings();
        }
    }
}
