/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.AbstractPropertySection;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditor;

/**
 * StandardPropertiesSection
 */
public class StandardPropertiesSection extends AbstractPropertySection {

	protected PropertyPageStandard pageStandard;
    private IDatabaseNodeEditor editor;

    public PropertyPageStandard getPage()
    {
        return pageStandard;
    }

    public void createControls(Composite parent,
			final TabbedPropertySheetPage tabbedPropertySheetPage) {
		super.createControls(parent, tabbedPropertySheetPage);

		pageStandard = new PropertyPageStandard();
		pageStandard.createControl(parent);

	}

	public void setInput(IWorkbenchPart part, ISelection newSelection) {
        if (!CommonUtils.equalObjects(getSelection(), newSelection)) {
		    super.setInput(part, newSelection);
		    pageStandard.selectionChanged(part, newSelection);
        }
        if (part instanceof IDatabaseNodeEditor) {
            this.editor = (IDatabaseNodeEditor)part;
        }
	}

	public void dispose() {
		super.dispose();
		if (pageStandard != null) {
			pageStandard.dispose();
			pageStandard = null;
		}
	}

	public void refresh() {
		//pageStandard.refresh();
	}

	public boolean shouldUseExtraSpace()
    {
		return true;
	}

    @Override
    public void aboutToBeShown()
    {
//        if (editor instanceof IProgressControlProvider) {
//            ((IProgressControlProvider)editor).getProgressControl().activate(true);
//        }
    }

    @Override
    public void aboutToBeHidden()
    {

    }
}