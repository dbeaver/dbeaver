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

import org.eclipse.ui.*;
import org.eclipse.ui.internal.Workbench;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.ai.AIChatConversation;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPRegistryListener;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.AbstractPartListener;
import org.jkiss.dbeaver.ui.UIExecutionQueue;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.AbstractPageListener;
import org.jkiss.dbeaver.ui.ai.chat.controls.AIChatControl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class AIChatViewHandler {
    private final AIChatControl chat;

    private final EventListener eventListener;
    private final PartListener partListener;

    public AIChatViewHandler(@NotNull AIChatView view) {
        this.chat = view.getChat();

        this.eventListener = new EventListener() {
            @Override
            public void handleDataSourceEvent(@NotNull DBPEvent event) {
                if (event.getAction() != DBPEvent.Action.OBJECT_UPDATE) {
                    updateActiveConversationQueueExec(view.getSite().getWorkbenchWindow());
                }
            }
        };
        this.partListener = new PartListener(view.getSite().getWorkbenchWindow()) {
            private IEditorPart lastPart;
            private void updateActivePart(@NotNull IWorkbenchPart part) {
                if (!(part instanceof IEditorPart editor) || part == lastPart) {
                    return;
                }
                lastPart = editor;
                updateActiveConversationQueueExec(part.getSite().getWorkbenchWindow());
            }

            @Override
            public void partActivated(@NotNull IWorkbenchPart part) {
                updateActivePart(part);
            }

            @Override
            public void partDeactivated(@NotNull IWorkbenchPart part) {
                // No need to refresh on deactivation. It leads to endless loop because
                // embedded editors get focus forcibly
                //updateActiveConversation(part.getSite().getWorkbenchWindow());
            }

            @Override
            public void partOpened(@NotNull IWorkbenchPart part) {
                updateActivePart(part);
            }

            @Override
            public void partClosed(@NotNull IWorkbenchPart part) {
                updateActivePart(part);
            }
        };

        updateActiveConversationQueueExec(Workbench.getInstance().getActiveWorkbenchWindow());
    }

    public void updateActiveConversationQueueExec(@Nullable IWorkbenchWindow window) {
        UIExecutionQueue.queueExec(() -> updateActiveConversation(window));
    }

    private void updateActiveConversation(@Nullable IWorkbenchWindow window) {
        if (window == null || window.getActivePage() == null) {
            return;
        }
        IEditorPart activeEditor = window.getActivePage().getActiveEditor();
        DBPDataSourceContainer container = null;
        if (activeEditor instanceof DBPDataSourceContainerProvider dscp) {
            container = dscp.getDataSourceContainer();
            if (container != null && container == chat.getDataSourceContainer()) {
                return;
            }
            if (chat.isDisposed()) {
                return;
            }

            AIChatConversation activeConversation = chat.getActiveConversation();
            DBPDataSourceContainer activeContainer = activeConversation.getDataSource();

            if (chat.isWaitingForResponse()) {
                container = activeContainer;
            }
        }
        if (activeEditor != null && container == null) {
            return;
        }
        final DBPDataSourceContainer finalContainer = container;
        UIUtils.syncExec(() -> {
            try {
                chat.setDataSource(finalContainer);
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError(
                    "AI Chat Error",
                    "Cannot set data source for AI chat",
                    e
                );
            }
        });
    }

    public void dispose() {
        eventListener.dispose();
        partListener.dispose();
    }

    private abstract static class EventListener implements DBPEventListener {
        private final Set<DBPDataSourceRegistry> registries = new HashSet<>();
        private final DBPRegistryListener registryListener;

        public EventListener() {
            this.registryListener = new DBPRegistryListener() {
                @Override
                public void handleRegistryLoad(@NotNull DBPDataSourceRegistry registry) {
                    registries.add(registry);
                    registry.addDataSourceListener(EventListener.this);
                }

                @Override
                public void handleRegistryUnload(@NotNull DBPDataSourceRegistry registry) {
                    registry.removeDataSourceListener(EventListener.this);
                    registries.remove(registry);
                }
            };
            DataSourceProviderRegistry.getInstance().addDataSourceRegistryListener(registryListener);

            // Register existing registries
            for (DBPDataSourceRegistry registry : DBUtils.getAllRegistries(false)) {
                registryListener.handleRegistryLoad(registry);
            }
        }

        public void dispose() {
            DataSourceProviderRegistry.getInstance().removeDataSourceRegistryListener(registryListener);
            for (DBPDataSourceRegistry registry : registries) {
                registry.removeDataSourceListener(this);
            }
            registries.clear();
        }
    }

    private static class PartListener extends AbstractPartListener {
        private final List<IWorkbenchPage> pages = new ArrayList<>();
        private final IWorkbenchWindow window;
        private final IPageListener pageListener;

        public PartListener(@NotNull IWorkbenchWindow window) {
            this.window = window;
            this.pageListener = new AbstractPageListener() {
                @Override
                public void pageOpened(@NotNull IWorkbenchPage page) {
                    page.addPartListener(PartListener.this);
                    pages.add(page);
                }

                @Override
                public void pageClosed(@NotNull IWorkbenchPage page) {
                    page.removePartListener(PartListener.this);
                    pages.remove(page);
                }
            };
            window.addPageListener(pageListener);

            // Register existing page
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                pageListener.pageOpened(page);
            }
        }

        public void dispose() {
            window.removePageListener(pageListener);
            for (IWorkbenchPage page : pages) {
                page.removePartListener(this);
            }
            pages.clear();
        }
    }
}
