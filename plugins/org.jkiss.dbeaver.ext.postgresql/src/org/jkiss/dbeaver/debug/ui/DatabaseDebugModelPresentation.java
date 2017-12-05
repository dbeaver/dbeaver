package org.jkiss.dbeaver.debug.ui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.ui.IDebugModelPresentationExtension;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class DatabaseDebugModelPresentation extends LabelProvider implements IDebugModelPresentationExtension {
    
    private final Map<String, Object> attributes = new HashMap<>();

    private final ILabelProvider labelProvider;

    public DatabaseDebugModelPresentation()
    {
        this(WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider());
    }

    public DatabaseDebugModelPresentation(ILabelProvider labelProvider)
    {
        this.labelProvider = labelProvider;
    }

    public Object getAttribute(String attribute)
    {
        return attributes.get(attribute);
    }

    @Override
    public void setAttribute(String attribute, Object value)
    {
        attributes.put(attribute, value);
    }

    @Override
    public Image getImage(Object element)
    {
        return labelProvider.getImage(element);
    }

    @Override
    public String getText(Object element)
    {
        return labelProvider.getText(element);
    }
    
    @Override
    public void dispose()
    {
        attributes.clear();
        super.dispose();
    }

    @Override
    public void computeDetail(IValue value, IValueDetailListener listener)
    {
    }

    @Override
    public IEditorInput getEditorInput(Object element)
    {
        return null;
    }

    @Override
    public String getEditorId(IEditorInput input, Object element)
    {
        return null;
    }

    @Override
    public boolean requiresUIThread(Object element)
    {
        return false;
    }

}
