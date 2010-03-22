package org.jkiss.dbeaver.ui.views.properties;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.IPropertySheetEntry;
import org.eclipse.ui.views.properties.PropertySheetEntry;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetSorter;
import org.jkiss.dbeaver.model.DBPObject;

import java.util.Hashtable;
import java.util.Map;

public class PropertiesPage extends PropertySheetPage
{
    private static Map<DBPObject, PropertiesPage> pagesMap = new Hashtable<DBPObject, PropertiesPage>();
    private DBPObject curObject;

    public PropertiesPage()
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
            curObject = null;
        }
        super.dispose();
    }

    public static PropertiesPage getPageByObject(DBPObject object)
    {
        return pagesMap.get(object);
    }

    public void setCurrentObject(IWorkbenchPart sourcePart, DBPObject object)
    {
        assert this.curObject == null;
        this.curObject = object;
        pagesMap.put(object, this);

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