package org.jkiss.dbeaver.ui.properties;

import org.eclipse.jface.viewers.LabelProvider;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * Label provider for property sources
 */
public class DefaultPropertyLabelProvider extends LabelProvider
{
    public static final DefaultPropertyLabelProvider INSTANCE = new DefaultPropertyLabelProvider();
    @Override
    public String getText(Object element)
    {
        return UIUtils.makeStringForUI(element).toString();
    }
}
