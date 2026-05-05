/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.preferences;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.text.templates.TemplatePersistenceData;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.texteditor.templates.TemplatePreferencePage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.editors.sql.templates.SQLTemplateStore;
import org.jkiss.dbeaver.ui.editors.sql.templates.SQLTemplatesRegistry;
import org.jkiss.dbeaver.ui.preferences.PreferenceStoreDelegate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Templates preference page
 */
public class PrefPageSQLTemplates extends TemplatePreferencePage implements IWorkbenchPropertyPage {

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.sql.templates";

    private static final int BTN_IDX_EXPORT = 0;
    private static final int BTN_IDX_IMPORT = 1;
    private static final int BTN_IDX_REVERT = 2;
    private static final int BTN_IDX_RESTORE = 3;
    private static final int BTN_IDX_REMOVE = 4;
    private static final int BTN_IDX_EDIT = 5;
    private static final int BTN_IDX_NEW = 6;
    
    private final List<Button> buttons = new ArrayList<>();
    private Table table;
    
    private final SQLTemplateStore templateStore;
    
    public PrefPageSQLTemplates() {
        setPreferenceStore(new PreferenceStoreDelegate(DBWorkbench.getPlatform().getPreferenceStore()));
        setTemplateStore(templateStore = SQLTemplatesRegistry.getInstance().getTemplateStore());
        setContextTypeRegistry(SQLTemplatesRegistry.getInstance().getTemplateContextRegistry());
    }

    @Override
    protected Control createContents(Composite ancestor) {
        Control root = super.createContents(ancestor);
        
        buttons.clear();
        Stack<Control> stack = new Stack<>();
        stack.add(root);
        while (stack.size() > 0) {
            Control ctl = stack.pop();
            if (ctl instanceof Button) {
                Button btn = (Button) ctl;
                if ((btn.getStyle() & SWT.PUSH) != 0) {
                    buttons.add(btn);
                    btn.setEnabled(false);
                }
            } else if (ctl instanceof Table) {
                table = (Table) ctl;
            } else if (ctl instanceof Composite) {
                for (Control cctl : ((Composite) ctl).getChildren()) {
                    stack.push(cctl);
                }
            }
        }
        
        return root;
    }
    
    @Override
    protected void updateButtons() {
        if (buttons.size() > 0) {
            boolean editAllowed = DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER);
            buttons.get(BTN_IDX_IMPORT).setEnabled(editAllowed);
            buttons.get(BTN_IDX_NEW).setEnabled(editAllowed);
            
            Set<String> customTemplateNames = templateStore.getCustomTemplateNames();

            TableItem[] selection = table.getSelection();
            int selectionCount = selection.length;
            List<TemplatePersistenceData> items = Stream.of(selection)
                .map(t -> (TemplatePersistenceData) t.getData()).collect(Collectors.toList());
            boolean canRevert = items.stream().anyMatch(TemplatePersistenceData::isModified);

            int itemCount = table.getItemCount();
            boolean canRestore = templateStore.getTemplateData(true).length != templateStore.getTemplateData(false).length;
            buttons.get(BTN_IDX_EDIT).setEnabled(selectionCount == 1
                && (editAllowed || !customTemplateNames.contains(items.get(0).getTemplate().getName())));
            buttons.get(BTN_IDX_EXPORT).setEnabled(selectionCount > 0);
            buttons.get(BTN_IDX_REMOVE).setEnabled(selectionCount > 0 && selectionCount <= itemCount
                && (editAllowed || items.stream().noneMatch(t -> customTemplateNames.contains(t.getTemplate().getName()))));
            buttons.get(BTN_IDX_RESTORE).setEnabled(canRestore);
            buttons.get(BTN_IDX_REVERT).setEnabled(canRevert);
        } else {
            super.updateButtons();
        }
    }

    @Nullable
    @Override
    protected Template editTemplate(@NotNull Template template, boolean edit, boolean isNameModifiable) {
        boolean editAllowed = DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER);
        boolean isCustomTemplate = templateStore.getCustomTemplateNames().contains(template.getName());
        if (editAllowed || !isCustomTemplate) {
            return super.editTemplate(template, edit, isNameModifiable);
        } else {
            return null;
        }
    }

    protected String getFormatterPreferenceKey() {
        return SQLTemplateStore.PREF_STORE_KEY;
    }

    @Override
    public IAdaptable getElement() {
        return null;
    }

    @Override
    public void setElement(IAdaptable element) {
    }
}
