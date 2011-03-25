/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.internal.views.properties.tabbed.view.TabbedPropertyComposite;
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

        TabbedPropertyComposite tpc = (TabbedPropertyComposite) atabbedPropertySheetPage.getControl();
//        tpc.getScrolledComposite().setExpandVertical(false);
//        tpc.getScrolledComposite().setExpandHorizontal(false);
//        tpc.getScrolledComposite().setAlwaysShowScrollBars(false);

		pageStandard = new PropertyPageStandard();
		pageStandard.createControl(parent);

/*
        pageStandard.getControl().addControlListener(new ControlAdapter() {

            public void controlResized(ControlEvent e) {
                atabbedPropertySheetPage.resizeScrolledComposite();
            }
        });
*/
	}

	public void setInput(IWorkbenchPart part, ISelection newSelection) {
        if (!CommonUtils.equalObjects(getSelection(), newSelection)) {
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