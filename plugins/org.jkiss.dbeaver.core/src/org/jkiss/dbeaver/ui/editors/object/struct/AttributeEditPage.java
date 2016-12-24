/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.editors.object.struct;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.runtime.properties.ObjectPropertyDescriptor;
import org.jkiss.dbeaver.runtime.properties.PropertySourceAbstract;
import org.jkiss.dbeaver.runtime.properties.PropertySourceEditable;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;

public class AttributeEditPage extends BaseObjectEditPage {

    private DBSEntityAttribute attribute;
    private final DBECommandContext commandContext;

    public AttributeEditPage(@Nullable DBECommandContext commandContext, @NotNull DBSEntityAttribute attribute)
    {
        super("Edit attribute " + DBUtils.getObjectFullName(attribute, DBPEvaluationContext.UI));
        this.commandContext = commandContext;
        this.attribute = attribute;
    }

    @Override
    protected Control createPageContents(Composite parent) {
        Composite propsGroup = new Composite(parent, SWT.NONE);
        propsGroup.setLayout(new GridLayout(2, false));
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        propsGroup.setLayoutData(gd);

        final Text nameText = UIUtils.createLabelText(propsGroup, "Name", attribute.getName()); //$NON-NLS-2$
        nameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e)
            {
                if (attribute instanceof DBPNamedObject2) {
                    ((DBPNamedObject2) attribute).setName(nameText.getText());
                }
            }
        });

        UIUtils.createControlLabel(propsGroup, "Properties").setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        PropertyTreeViewer propertyViewer = new PropertyTreeViewer(propsGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 400;
        propertyViewer.getControl().setLayoutData(gd);
        propertyViewer.addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                return true;
            }
        });

        PropertySourceAbstract pc = new PropertySourceEditable(commandContext, attribute, attribute);
        pc.collectProperties();
        for (DBPPropertyDescriptor prop : pc.getProperties()) {
            if (prop instanceof ObjectPropertyDescriptor) {
                if (((ObjectPropertyDescriptor) prop).isEditPossible() && !((ObjectPropertyDescriptor) prop).isNameProperty()) {
                    continue;
                }
            }
            pc.removeProperty(prop);
        }
        propertyViewer.loadProperties(pc);

        return propsGroup;
    }

    @Override
    protected void performFinish() throws DBException {
        //commandContext.saveChanges(VoidProgressMonitor.INSTANCE);
    }
}
