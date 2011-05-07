/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.tabbed.AbstractPropertySection;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

/**
 * StandardPropertiesSection
 */
public class StandardPropertiesSection extends AbstractPropertySection implements ILazyPropertyLoadListener {

	protected PropertyTreeViewer propertyTree;
    private IPropertySource curPropertySource;

    public void createControls(Composite parent, final TabbedPropertySheetPage tabbedPropertySheetPage)
    {
		super.createControls(parent, tabbedPropertySheetPage);

		propertyTree = new PropertyTreeViewer(parent, SWT.NONE);
        PropertiesContributor.getInstance().addLazyListener(this);

	}

	public void setInput(IWorkbenchPart part, ISelection newSelection) {
        if (!CommonUtils.equalObjects(getSelection(), newSelection)) {
		    super.setInput(part, newSelection);
            if (!newSelection.isEmpty() && newSelection instanceof IStructuredSelection) {
                Object element = ((IStructuredSelection) newSelection).getFirstElement();
                if (element instanceof IPropertySource) {
                    curPropertySource = (IPropertySource)element;
                    propertyTree.loadProperties((IPropertySource)element);
                }
            }
		    //pageStandard.selectionChanged(part, newSelection);
        }
	}

	public void dispose() {
        PropertiesContributor.getInstance().removeLazyListener(this);
		super.dispose();
	}

	public void refresh() {
		//propertyTree.refresh();
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

    public void handlePropertyLoad(Object object, Object propertyId, Object propertyValue, boolean completed)
    {
        if (curPropertySource.getEditableValue() == object && !propertyTree.getControl().isDisposed()) {
            propertyTree.refresh();
        }
    }

}