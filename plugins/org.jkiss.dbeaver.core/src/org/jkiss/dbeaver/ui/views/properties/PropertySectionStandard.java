/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */
package org.jkiss.dbeaver.ui.views.properties;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.AbstractPropertySection;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

/**
 * PropertySectionStandard
 */
public class PropertySectionStandard extends AbstractPropertySection {

	protected PropertyPageStandard pageStandard;

    public PropertyPageStandard getPage()
    {
        return pageStandard;
    }

    public void createControls(Composite parent,
			final TabbedPropertySheetPage atabbedPropertySheetPage) {
		super.createControls(parent, atabbedPropertySheetPage);
		Composite composite = getWidgetFactory()
			.createFlatFormComposite(parent);
		pageStandard = new PropertyPageStandard();

		pageStandard.createControl(composite);
		FormData data = new FormData();
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(100, 0);
		data.top = new FormAttachment(0, 0);
		data.bottom = new FormAttachment(100, 0);
		pageStandard.getControl().setLayoutData(data);

		pageStandard.getControl().addControlListener(new ControlAdapter() {

			public void controlResized(ControlEvent e) {
				atabbedPropertySheetPage.resizeScrolledComposite();
			}
		});
	}

	public void setInput(IWorkbenchPart part, ISelection newSelection) {
        if (getSelection() == null || newSelection == null || !getSelection().equals(newSelection)) {
		    super.setInput(part, newSelection);
		    pageStandard.selectionChanged(part, newSelection);
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
}