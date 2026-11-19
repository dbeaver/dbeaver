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
package org.jkiss.dbeaver.ui.app.standalone.update;

import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionInfo;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.ExtensionContributionFactory;
import org.eclipse.ui.menus.IContributionRoot;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.ui.services.IServiceLocator;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.BaseEditorColors;
import org.jkiss.dbeaver.ui.BaseThemeSettings;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.function.Consumer;

/**
 * Colored toolbar presentation, retaining the standard command's dropdown and lifecycle.
 */
public class VersionUpdateContribution extends ExtensionContributionFactory {
    private static final Log log = Log.getLog(VersionUpdateContribution.class);
    private static final String UPDATE_MENU_ID = "org.jkiss.dbeaver.ui.versionUpdate.menu";

    @Override
    public void createContributionItems(@NotNull IServiceLocator serviceLocator, @NotNull IContributionRoot additions) {
        var parameters = new CommandContributionItemParameter(
            serviceLocator, UPDATE_MENU_ID, VersionUpdateHandler.COMMAND_UPDATE,
            CommandContributionItem.STYLE_PULLDOWN
        );
        parameters.mode = CommandContributionItem.MODE_FORCE_TEXT;
        var command = new CommandContributionItem(parameters);
        additions.addContributionItem(new ContributionItem(UPDATE_MENU_ID) {
            @Override
            public void fill(@NotNull ToolBar toolbar, int index) {
                command.fill(toolbar, index);
                configureToolbar(serviceLocator, toolbar, index, command);
            }

            @Override
            public void dispose() {
                command.dispose();
                super.dispose();
            }
        }, new Expression() {
            @Override
            @NotNull
            public EvaluationResult evaluate(@NotNull IEvaluationContext context) {
                return EvaluationResult.valueOf(VersionUpdateHandler.isUpdateAvailable());
            }

            @Override
            public void collectExpressionInfo(@NotNull ExpressionInfo info) {
                info.addAccessedPropertyName(VersionUpdateHandler.PROPERTY_AVAILABLE);
            }
        });
    }

    private void configureToolbar(
        @NotNull IServiceLocator serviceLocator,
        @NotNull ToolBar toolbar,
        int index,
        @NotNull CommandContributionItem command
    ) {
        var item = toolbar.getItem(index < 0 ? toolbar.getItemCount() - 1 : index);
        Consumer<String> themeListener = id -> UIUtils.asyncExec(() -> updateColors(toolbar));
        BaseThemeSettings.instance.addPropertyListener(
            BaseEditorColors.COLOR_BACKGROUND_BLUE, themeListener, null
        );
        BaseThemeSettings.instance.addPropertyListener(
            BaseEditorColors.COLOR_BACKGROUND_GREEN, themeListener, null
        );
        item.addDisposeListener(event -> {
            BaseThemeSettings.instance.removePropertyListener(BaseEditorColors.COLOR_BACKGROUND_BLUE, themeListener);
            BaseThemeSettings.instance.removePropertyListener(BaseEditorColors.COLOR_BACKGROUND_GREEN, themeListener);
        });
        // Apply after the workbench has styled the native toolbar during creation.
        UIUtils.asyncExec(() -> updateColors(toolbar));

        // A second element participates in command refreshes solely to update native colors.
        ICommandService service = serviceLocator.getService(ICommandService.class);
        UIElement element = new UIElement(serviceLocator) {
            @Override
            public void setText(@Nullable String text) {
            }

            @Override
            public void setTooltip(@Nullable String text) {
            }

            @Override
            public void setIcon(@Nullable ImageDescriptor descriptor) {
                updateColors(toolbar);
            }

            @Override
            public void setChecked(boolean checked) {
            }

            @Override
            public void setDisabledIcon(@Nullable ImageDescriptor descriptor) {
            }

            @Override
            public void setHoverIcon(@Nullable ImageDescriptor descriptor) {
            }
        };
        try {
            var reference = service.registerElementForCommand(command.getCommand(), element);
            item.addDisposeListener(event -> service.unregisterElement(reference));
        } catch (NotDefinedException e) {
            log.error("Update command is not defined", e);
        }
    }

    private static void updateColors(@NotNull ToolBar toolbar) {
        if (toolbar.isDisposed()) {
            return;
        }
        var background = VersionUpdatePresentation.getUpdateBackground(VersionUpdateHandler.isMajorUpdate());
        var foreground = toolbar.getParent().getForeground();
        toolbar.setBackground(background);
        toolbar.setForeground(foreground);
        for (var item : toolbar.getItems()) {
            item.setBackground(background);
            item.setForeground(foreground);
        }
    }
}
