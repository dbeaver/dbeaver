package org.jkiss.dbeaver.ext.dm.ui.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.dm.model.source.DmSourceObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

/**
 * Base task handler
 * 
 * @author caosw
 *
 */
public abstract class DmTaskHandler extends AbstractHandler implements IElementUpdater {

	private static final Log log = Log.getLog(DmTaskHandler.class);

	protected List<DmSourceObject> getDmSourceObjects(UIElement element) {
		List<DmSourceObject> objects = new ArrayList<DmSourceObject>();
		IWorkbenchPartSite partSite = UIUtils.getWorkbenchPartSite(element.getServiceLocator());
		if (partSite != null) {
			final ISelectionProvider selectionProvider = partSite.getSelectionProvider();
			if (selectionProvider != null) {
				ISelection selection = selectionProvider.getSelection();
				if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
					for (Iterator<?> iter = ((IStructuredSelection) selection).iterator(); iter.hasNext();) {
						final Object item = iter.next();
						final DmSourceObject sourceObject = RuntimeUtils.getObjectAdapter(item, DmSourceObject.class);
						if (sourceObject != null) {
							objects.add(sourceObject);
						}
					}
				}
			}
			if(objects.isEmpty()) {
				final IWorkbenchPart activePart = partSite.getPart();
				final DmSourceObject sourceObject = RuntimeUtils.getObjectAdapter(activePart, DmSourceObject.class);
				if(sourceObject!=null) {
					objects.add(sourceObject);
				}
			}
		}
		return objects;
	}
}
