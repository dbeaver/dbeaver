/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.navigator.dialogs;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
import org.eclipse.ui.dialogs.SearchPattern;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableParametrized;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;

/**
 * GotoObjectDialog
 */
public class GotoObjectDialog extends FilteredItemsSelectionDialog {

    private static final String DIALOG_ID = "GotoObjectDialog";

    private static final boolean SHOW_OBJECT_TYPES = true;
    private static final int MAX_RESULT_COUNT = 1000;

    private final DBCExecutionContext context;
    private DBSObject container;
    private Map<String, Boolean> enabledTypes = new HashMap<>();
    private boolean hasMoreResults;

    public GotoObjectDialog(Shell shell, DBCExecutionContext context, DBSObject container) {
        super(shell, true);
        this.context = context;
        this.container = container;

        setTitle(UINavigatorMessages.dialog_project_goto_object_title + " '" + context.getDataSource().getContainer().getName() + "'");
        setListLabelProvider(new ObjectLabelProvider());
        setDetailsLabelProvider(new DetailsLabelProvider());
    }

    @Override
    protected Control createExtendedContentArea(Composite parent) {
        if (!SHOW_OBJECT_TYPES) {
            return null;
        }
        IDialogSettings driverSettings = DialogSettings.getOrCreateSection(
            getDialogSettings(), context.getDataSource().getContainer().getDriver().getId());

        DBSStructureAssistant structureAssistant = DBUtils.getAdapter(DBSStructureAssistant.class, context.getDataSource());
        if (structureAssistant == null) {
            return null;
        }

        List<DBSObjectType> typesToSearch = new ArrayList<>();
        for (DBSObjectType type : structureAssistant.getSearchObjectTypes()) {
            Class<? extends DBSObject> typeClass = type.getTypeClass();
            if (DBSEntityElement.class.isAssignableFrom(typeClass)) {
                // Skipp attributes (columns), methods, etc
                continue;
            }
            typesToSearch.add(type);
        }
        if (!CommonUtils.isEmpty(typesToSearch)) {
            Group cbGroup = new Group(parent, SWT.NONE);
            cbGroup.setText("Objects:");
            RowLayout rowLayout = new RowLayout(SWT.HORIZONTAL);
            rowLayout.wrap = true;
            cbGroup.setLayout(rowLayout);
            cbGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            for (DBSObjectType type : typesToSearch) {
                if (!isValidObjectType(type)) {
                    continue;
                }

                Button cb = new Button(cbGroup, SWT.CHECK);
                cb.setData(type);
                String typeName = CommonUtils.notEmpty(type.getTypeName());
                cb.setText(typeName);

                boolean enabled;
                if (driverSettings.get(typeName) != null) {
                    enabled = driverSettings.getBoolean(typeName);
                } else {
                    enabled = true;
                }
                cb.setSelection(enabled);
                enabledTypes.put(typeName, enabled);
                cb.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        enabledTypes.put(typeName, cb.getSelection());
                        driverSettings.put(typeName, cb.getSelection());
                        applyFilter();
                        //scheduleRefresh();
                    }
                });
            }

            return cbGroup;
        }

        return null;
    }

    @Override
    protected IDialogSettings getDialogSettings() {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected IStatus validateItem(Object item) {
        return Status.OK_STATUS;
    }

    @Override
    protected ItemsFilter createFilter() {
        return new ObjectFilter();
    }

    private boolean isValidObjectType(DBSObjectType objectType) {
        Class<? extends DBSObject> typeClass = objectType.getTypeClass();
        if (DBSEntityElement.class.isAssignableFrom(typeClass)) {
            return false;
        }
        return true;
    }

    @Override
    protected Comparator getItemsComparator() {
        return (o1, o2) -> {
            if (o1 instanceof DBPNamedObject && o2 instanceof DBPNamedObject) {
                return DBUtils.getObjectFullName((DBPNamedObject) o1, DBPEvaluationContext.UI).compareToIgnoreCase(
                    DBUtils.getObjectFullName((DBPNamedObject) o2, DBPEvaluationContext.UI));
            }
            return 0;
        };
    }

    @Override
    protected void fillContentProvider(AbstractContentProvider contentProvider, ItemsFilter itemsFilter, IProgressMonitor progressMonitor)
        throws CoreException {
        DBSStructureAssistant structureAssistant = DBUtils.getAdapter(DBSStructureAssistant.class, context.getDataSource());
        if (structureAssistant == null) {
            return;
        }
        String nameMask = ((ObjectFilter) itemsFilter).getNameMask();
        DBRProgressMonitor monitor = RuntimeUtils.makeMonitor(progressMonitor);
        try {
            monitor.beginTask("Search for '" + nameMask + "'", 100);
            List<DBSObjectType> typesToSearch = new ArrayList<>();
            for (DBSObjectType type : structureAssistant.getSearchObjectTypes()) {
                if (!isValidObjectType(type)) {
                    // Skip attributes (columns), methods, etc
                    continue;
                }
                if (!Boolean.TRUE.equals(enabledTypes.get(type.getTypeName()))) {
                    continue;
                }
                typesToSearch.add(type);
            }

            ObjectFinder objectFinder = new ObjectFinder(structureAssistant, monitor, context, typesToSearch, nameMask);
            DBExecUtils.tryExecuteRecover(monitor, context.getDataSource(), objectFinder);

            DBPDataSourceContainer dsContainer = context.getDataSource().getContainer();
            for (DBSObjectReference ref : objectFinder.getResult()) {
                DBSObjectFilter filter = dsContainer.getObjectFilter(ref.getObjectClass(), ref.getContainer(), true);
                if (filter == null || !filter.isEnabled() || filter.matches(ref.getName())) {
                    contentProvider.add(ref, itemsFilter);
                }
            }
        } catch (DBException e) {
            throw new CoreException(GeneralUtils.makeExceptionStatus(e));
        } finally {
            monitor.done();
        }
    }

    @Override
    public String getElementName(Object item) {
        if (item instanceof DBPNamedObject) {
            return DBUtils.getObjectFullName((DBPNamedObject) item, DBPEvaluationContext.UI);
        }
        return item.toString();
    }

    private static class ObjectLabelProvider extends LabelProvider implements DelegatingStyledCellLabelProvider.IStyledLabelProvider {
        @Override
        public StyledString getStyledText(Object element) {
            if (element instanceof DBPNamedObject) {
                DBPNamedObject namedObject = (DBPNamedObject) element;
                StyledString str = new StyledString(namedObject.getName());
                String fullName = DBUtils.getObjectFullName(namedObject, DBPEvaluationContext.UI);
                if (!CommonUtils.equalObjects(fullName, namedObject.getName())) {
                    str.append(" - ", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
                    str.append(fullName, StyledString.QUALIFIER_STYLER);
                }
                return str;
            }
            return new StyledString("?");
        }

        @Override
        public Image getImage(Object element) {
            if (element instanceof DBSObjectReference) {
                DBSObjectType objectType = ((DBSObjectReference) element).getObjectType();
                if (objectType != null) {
                    return DBeaverIcons.getImage(objectType.getImage());
                }
            }
            return null;
        }

        @Override
        public String getText(Object element) {
            if (element instanceof DBPNamedObject) {
                return ((DBPNamedObject) element).getName();
            }
            return null;
        }
    }

    private static class DetailsLabelProvider extends ObjectLabelProvider {
        @Override
        public String getText(Object element) {
            if (element instanceof DBPNamedObject) {
                return DBUtils.getObjectFullName((DBPNamedObject) element, DBPEvaluationContext.UI);
            }
            return super.getText(element);
        }
    }

    private class ObjectFilter extends ItemsFilter {

        private Pattern namePattern = null;
        private Map<String, Boolean> enabledTypesCopy;

        ObjectFilter() {
            this.enabledTypesCopy = new HashMap<>(enabledTypes);
        }

        @Override
        public int getMatchRule() {
            return SearchPattern.RULE_PATTERN_MATCH | SearchPattern.RULE_PREFIX_MATCH;
        }

        @Override
        public boolean matchItem(Object item) {
            if (item instanceof DBPNamedObject) {
                String objectName = ((DBPNamedObject) item).getName();
                //String pattern = getPattern().replaceAll("[\\*\\%\\?]", "");
                //return TextUtils.fuzzyScore(objectName, pattern) > 0;
                if (!getNamePattern().matcher(objectName).matches()) {
                    return false;
                }
                // Check for filters

                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean isConsistentItem(Object item) {
            return false;
        }

        public String getNameMask() {
            String nameMask = getPattern();
            nameMask = nameMask.replace("*", "%").replace("?", "_");
            int matchRule = getMatchRule();
            if ((matchRule & SearchPattern.RULE_PREFIX_MATCH) != 0 && !nameMask.endsWith("%")) {
                nameMask += "%";
            }
            return nameMask;
        }

        private Pattern getNamePattern() {
            if (namePattern == null) {
                namePattern = Pattern.compile(
                    SQLUtils.makeLikePattern(getNameMask()), Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
            }
            return namePattern;
        }

        @Override
        public boolean equalsFilter(ItemsFilter filter) {
            return
                filter instanceof ObjectFilter &&
                super.equalsFilter(filter) &&
                CommonUtils.equalObjects(enabledTypesCopy, ((ObjectFilter)filter).enabledTypesCopy);
        }

        @Override
        public boolean isSubFilter(ItemsFilter filter) {
            return !hasMoreResults && super.isSubFilter(filter) && CommonUtils.equalObjects(enabledTypesCopy, ((ObjectFilter)filter).enabledTypesCopy);
        }
    }

    private class ObjectFinder implements DBRRunnableParametrized<DBRProgressMonitor> {
        private final DBSStructureAssistant structureAssistant;
        private final DBRProgressMonitor monitor;
        private final DBCExecutionContext executionContext;
        private final List<DBSObjectType> typesToSearch;
        private final String nameMask;
        private List<DBSObjectReference> result;

        ObjectFinder(DBSStructureAssistant structureAssistant, DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBSObjectType> typesToSearch, String nameMask) {
            this.structureAssistant = structureAssistant;
            this.monitor = monitor;
            this.executionContext = executionContext;
            this.typesToSearch = typesToSearch;
            this.nameMask = nameMask;
        }

        public List<DBSObjectReference> getResult() {
            return result;
        }

        @Override
        public void run(DBRProgressMonitor param) throws InvocationTargetException, InterruptedException {
            try {
                result = structureAssistant.findObjectsByMask(
                    monitor,
                    executionContext,
                    container,
                    typesToSearch.toArray(new DBSObjectType[0]),
                    nameMask,
                    false,
                    true, MAX_RESULT_COUNT);
                hasMoreResults = result.size() >= MAX_RESULT_COUNT;
            } catch (Exception e) {
                throw new InvocationTargetException(e);
            }
        }
    }
}
