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
package org.jkiss.dbeaver.ui.controls.resultset.spreadsheet;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.IParameter;
import org.eclipse.core.commands.IParameterValues;
import org.eclipse.core.commands.ParameterValuesException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.ui.services.IServiceLocator;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.handler.ResultSetHandlerMain;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SpreadsheetColumnFilterHandler extends AbstractHandler implements IElementUpdater {
    private static final Log log = Log.getLog(SpreadsheetColumnFilterHandler.class);

    public static final String PARAM_CRITERION = "criterion";

    @Nullable
    @Override
    public Object execute(@NotNull ExecutionEvent event) {
        SpreadsheetPresentation presentation = SpreadsheetCommandHandler.getActiveSpreadsheet(event);
        if (presentation != null) {
            presentation.getColumnFilter().toggle(getCriterion(event.getParameter(PARAM_CRITERION)));
            presentation.refreshData(true, false, true);
            refreshElements(presentation.getController());
        }
        return null;
    }

    @Override
    public void updateElement(@NotNull UIElement element, @NotNull Map parameters) {
        IWorkbenchPart part = element.getServiceLocator().getService(IWorkbenchPart.class);
        IResultSetController controller = ResultSetHandlerMain.getActiveResultSet(part);
        element.setChecked(controller != null
            && controller.getActivePresentation() instanceof SpreadsheetPresentation presentation
            && presentation.getColumnFilter().isEnabled(getCriterion((String) parameters.get(PARAM_CRITERION))));
    }

    static void refreshElements(@NotNull IResultSetController controller) {
        ICommandService service = controller.getSite().getService(ICommandService.class);
        if (service != null) {
            service.refreshElements(SpreadsheetCommandHandler.CMD_COLUMNS_HIDE_EMPTY, null);
        }
    }

    @NotNull
    private static ResultSetColumnFilter.Criterion getCriterion(@Nullable String value) {
        // Existing shortcut assignments have no parameter and retain the NULL-only behavior.
        return CommonUtils.valueOf(ResultSetColumnFilter.Criterion.class, value, ResultSetColumnFilter.Criterion.NULL_VALUES);
    }

    public static class CriterionParameterValues implements IParameterValues {
        @NotNull
        @Override
        public Map<String, String> getParameterValues() {
            Map<String, String> values = new LinkedHashMap<>();
            values.put(ResultSetMessages.controls_resultset_viewer_hide_null_columns, ResultSetColumnFilter.Criterion.NULL_VALUES.name());
            values.put(ResultSetMessages.controls_resultset_viewer_hide_empty_string_columns, ResultSetColumnFilter.Criterion.EMPTY_STRINGS.name());
            return values;
        }
    }

    public static class MenuContributor extends CompoundContributionItem implements IWorkbenchContribution {
        private IServiceLocator serviceLocator;

        @Override
        public void initialize(@NotNull IServiceLocator serviceLocator) {
            this.serviceLocator = serviceLocator;
        }

        @NotNull
        @Override
        protected IContributionItem[] getContributionItems() {
            ICommandService commandService = serviceLocator.getService(ICommandService.class);
            if (commandService == null) {
                return new IContributionItem[0];
            }
            try {
                IParameter parameter = commandService.getCommand(SpreadsheetCommandHandler.CMD_COLUMNS_HIDE_EMPTY)
                    .getParameter(PARAM_CRITERION);
                if (parameter != null) {
                    Map<String, String> values = parameter.getValues().getParameterValues();
                    List<IContributionItem> items = new ArrayList<>(values.size());
                    for (var value : values.entrySet()) {
                        items.add(ActionUtils.makeCommandContribution(
                            serviceLocator, SpreadsheetCommandHandler.CMD_COLUMNS_HIDE_EMPTY,
                            CommandContributionItem.STYLE_CHECK, value.getKey(),
                            null, null, false, Map.of(PARAM_CRITERION, value.getValue())));
                    }
                    return items.toArray(IContributionItem[]::new);
                }
            } catch (NotDefinedException | ParameterValuesException e) {
                log.error("Can't populate column filter menu", e);
            }
            return new IContributionItem[0];
        }
    }
}
