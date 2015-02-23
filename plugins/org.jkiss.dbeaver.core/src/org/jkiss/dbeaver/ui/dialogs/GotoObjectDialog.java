/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
import org.eclipse.ui.dialogs.SearchPattern;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * GotoObjectDialog
 */
public class GotoObjectDialog extends FilteredItemsSelectionDialog {

    private static final String DIALOG_ID = "GotoObjectDialog";

    private final DBPDataSource dataSource;
    private DBSObject container;

    public GotoObjectDialog(Shell shell, DBPDataSource dataSource, DBSObject container)
    {
        super(shell, true);
        this.dataSource = dataSource;
        this.container = container;

        setTitle("Goto Meta Object in '" + dataSource.getContainer().getName() + "'");
        setListLabelProvider(new ObjectLabelProvider());
        setDetailsLabelProvider(new DetailsLabelProvider());
    }

    @Override
    protected Control createExtendedContentArea(Composite parent)
    {
        return null;
    }

    @Override
    protected IDialogSettings getDialogSettings()
    {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected IStatus validateItem(Object item)
    {
        return Status.OK_STATUS;
    }

    @Override
    protected ItemsFilter createFilter()
    {
        return new ObjectFilter();
    }

    @Override
    protected Comparator getItemsComparator()
    {
        return new Comparator() {
            @Override
            public int compare(Object o1, Object o2)
            {
                if (o1 instanceof DBPNamedObject && o2 instanceof DBPNamedObject) {
                    return DBUtils.getObjectFullName((DBPNamedObject) o1).compareToIgnoreCase(
                        DBUtils.getObjectFullName((DBPNamedObject) o2));
                }
                return 0;
            }
        };
    }

    @Override
    protected void fillContentProvider(AbstractContentProvider contentProvider, ItemsFilter itemsFilter, IProgressMonitor progressMonitor)
        throws CoreException
    {
        DBSStructureAssistant structureAssistant = DBUtils.getAdapter(DBSStructureAssistant.class, dataSource);
        if (structureAssistant == null) {
            return;
        }
        String nameMask = ((ObjectFilter)itemsFilter).getNameMask();
        DefaultProgressMonitor monitor = new DefaultProgressMonitor(progressMonitor);
        try {
            monitor.beginTask("Search for '" + nameMask + "'", 100);
            List<DBSObjectType> typesToSearch = new ArrayList<DBSObjectType>();
            for (DBSObjectType type : structureAssistant.getSupportedObjectTypes()) {
                Class<? extends DBSObject> typeClass = type.getTypeClass();
                if (DBSEntityElement.class.isAssignableFrom(typeClass)) {
                    // Skipp attributes (columns), methods, etc
                    continue;
                }
                typesToSearch.add(type);
            }
            Collection<DBSObjectReference> result = structureAssistant.findObjectsByMask(
                monitor,
                container,
                typesToSearch.toArray(new DBSObjectType[typesToSearch.size()]),
                nameMask,
                false,
                1000);
            for (DBSObjectReference ref : result) {
                contentProvider.add(ref, itemsFilter);
            }
        } catch (DBException e) {
            throw new CoreException(RuntimeUtils.makeExceptionStatus(e));
        }
        finally {
            monitor.done();
        }
    }

    @Override
    public String getElementName(Object item)
    {
        if (item instanceof DBPNamedObject) {
            return DBUtils.getObjectFullName((DBPNamedObject) item);
        }
        return item.toString();
    }

    private static class ObjectLabelProvider extends LabelProvider implements DelegatingStyledCellLabelProvider.IStyledLabelProvider {
        @Override
        public StyledString getStyledText(Object element) {
            if (element instanceof DBPNamedObject) {
                DBPNamedObject namedObject = (DBPNamedObject) element;
                StyledString str = new StyledString(namedObject.getName());
                String fullName = DBUtils.getObjectFullName(namedObject);
                if (!CommonUtils.equalObjects(fullName, namedObject.getName())) {
                    str.append(" - ", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
                    str.append(fullName, StyledString.QUALIFIER_STYLER);
                }
                return str;
            }
            return new StyledString("?");
        }

        @Override
        public Image getImage(Object element)
        {
            if (element instanceof DBSObjectReference) {
                DBSObjectType objectType = ((DBSObjectReference) element).getObjectType();
                if (objectType != null) {
                    return objectType.getImage();
                }
            }
            return null;
        }

        @Override
        public String getText(Object element)
        {
            if (element instanceof DBPNamedObject) {
                return ((DBPNamedObject)element).getName();
            }
            return null;
        }
    }

    private static class DetailsLabelProvider extends ObjectLabelProvider {
        @Override
        public String getText(Object element)
        {
            if (element instanceof DBPNamedObject) {
                return DBUtils.getObjectFullName((DBPNamedObject) element);
            }
            return super.getText(element);
        }
    }

    private class ObjectFilter extends ItemsFilter {

        private Pattern namePattern = null;
        @Override
        public int getMatchRule()
        {
            return SearchPattern.RULE_PATTERN_MATCH | SearchPattern.RULE_PREFIX_MATCH;
        }

        @Override
        public boolean matchItem(Object item)
        {
            if (item instanceof DBPNamedObject) {
                String objectName = ((DBPNamedObject) item).getName();
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
        public boolean isConsistentItem(Object item)
        {
            return false;
        }

        public String getNameMask()
        {
            String nameMask = getPattern();
            nameMask = nameMask.replace("*", "%").replace("?", "_");
            int matchRule = getMatchRule();
            if ((matchRule & SearchPattern.RULE_PREFIX_MATCH) != 0 && !nameMask.endsWith("%")) {
                nameMask += "%";
            }
            return nameMask;
        }

        private Pattern getNamePattern()
        {
            if (namePattern == null) {
                namePattern = Pattern.compile(
                    SQLUtils.makeLikePattern(getNameMask()), Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
            }
            return namePattern;
        }

    }

}
