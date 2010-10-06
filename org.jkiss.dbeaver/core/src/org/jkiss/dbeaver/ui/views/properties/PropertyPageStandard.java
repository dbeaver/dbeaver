/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.IPropertySheetEntry;
import org.eclipse.ui.views.properties.PropertySheetEntry;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetSorter;
import org.jkiss.dbeaver.model.struct.DBSWrapper;

import java.util.Hashtable;
import java.util.Map;

public class PropertyPageStandard extends PropertySheetPage
{
    private static Map<Object, PropertyPageStandard> pagesMap = new Hashtable<Object, PropertyPageStandard>();
    private Object curObject;

    public PropertyPageStandard()
    {
        setSorter(
            new PropertySheetSorter() {
                public int compare(IPropertySheetEntry entryA, IPropertySheetEntry entryB)
                {
                    // No dumn sorting
                    return 0;
                }
            }
        );
    }

    @Override
    public void dispose()
    {
        if (curObject != null) {
            pagesMap.remove(curObject);
            if (curObject instanceof DBSWrapper) {
                pagesMap.put(((DBSWrapper)curObject).getObject(), this);
            }
            curObject = null;
        }
        super.dispose();
    }

    public static PropertyPageStandard getPageByObject(Object object)
    {
        return pagesMap.get(object);
    }

    public void setCurrentObject(IWorkbenchPart sourcePart, Object object)
    {
        assert this.curObject == null;
        this.curObject = object;
        pagesMap.put(object, this);
        if (object instanceof DBSWrapper) {
            pagesMap.put(((DBSWrapper)object).getObject(), this);
        }

        this.selectionChanged(sourcePart, new StructuredSelection(object));
    }

    public Object getCurrentObject()
    {
        if (this.curObject != null) {
            return this.curObject;
        }
        PropertySheetEntry curPropsObject = (PropertySheetEntry)getControl().getData();
        Object[] curObjects = curPropsObject.getValues();
        // Refresh only if current property sheet object is the same as for collector
        if (curObjects != null && curObjects.length > 0) {
            return curObjects[0];
        }
        return null;
    }
}