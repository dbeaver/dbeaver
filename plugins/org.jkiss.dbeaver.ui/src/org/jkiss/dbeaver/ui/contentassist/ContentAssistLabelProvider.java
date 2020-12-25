package org.jkiss.dbeaver.ui.contentassist;

import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPImageProvider;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.utils.CommonUtils;

public class ContentAssistLabelProvider extends BaseLabelProvider implements ILabelProvider {

    @Override
    public Image getImage(Object element) {
        if (element instanceof DBPImageProvider) {
            DBPImage image = ((DBPImageProvider) element).getObjectImage();
            return image == null ? null : DBeaverIcons.getImage(image);
        }
        return null;
    }

    @Override
    public String getText(Object element) {
        return element instanceof IContentProposal ? ((IContentProposal) element).getLabel() : CommonUtils.toString(element);
    }
}
