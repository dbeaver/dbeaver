package org.jkiss.dbeaver.postgresql.internal.debug.ui;

import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;

public class PgSqlDebugModelPresentation extends LabelProvider implements IDebugModelPresentation {

    @Override
    public IEditorInput getEditorInput(Object element)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getEditorId(IEditorInput input, Object element)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setAttribute(String attribute, Object value)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public Image getImage(Object element)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getText(Object element)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void computeDetail(IValue value, IValueDetailListener listener)
    {
        // TODO Auto-generated method stub

    }

}
