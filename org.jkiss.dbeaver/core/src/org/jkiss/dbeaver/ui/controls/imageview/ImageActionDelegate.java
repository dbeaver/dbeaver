package org.jkiss.dbeaver.ui.controls.imageview;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.ui.IActionDelegate;

/**
 * Action delegate for all toolbar push-buttons.
 * <p/>
 *
 * @author Chengdong Li: cli4@uky.edu
 */
public class ImageActionDelegate implements IActionDelegate {
    /**
     * pointer to image view
     */
    public ImageViewControl imageViewControl = null;
    /**
     * Action id of this delegate
     */
    public String id;

    public ImageActionDelegate(ImageViewControl viewControl) {
        this.imageViewControl = viewControl;
    }

    /* (non-Javadoc)
      * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
      */
    public void run(IAction action) {
        String id = action.getId();
        if (imageViewControl.getSourceImage() == null) return;
        if (id.equals("toolbar.zoomin")) {
            imageViewControl.zoomIn();
            return;
        } else if (id.equals("toolbar.zoomout")) {
            imageViewControl.zoomOut();
            return;
        } else if (id.equals("toolbar.fit")) {
            imageViewControl.fitCanvas();
            return;
        } else if (id.equals("toolbar.rotate")) {
            /* rotate image anti-clockwise */
            ImageData src = imageViewControl.getImageData();
            if (src == null) return;
            PaletteData srcPal = src.palette;
            PaletteData destPal;
            ImageData dest;
            /* construct a new ImageData */
            if (srcPal.isDirect) {
                destPal = new PaletteData(srcPal.redMask, srcPal.greenMask, srcPal.blueMask);
            } else {
                destPal = new PaletteData(srcPal.getRGBs());
            }
            dest = new ImageData(src.height, src.width, src.depth, destPal);
            /* rotate by rearranging the pixels */
            for (int i = 0; i < src.width; i++) {
                for (int j = 0; j < src.height; j++) {
                    int pixel = src.getPixel(i, j);
                    dest.setPixel(j, src.width - 1 - i, pixel);
                }
            }
            imageViewControl.setImageData(dest);
            return;
        } else if (id.equals("toolbar.original")) {
            imageViewControl.showOriginal();
            return;
        }
    }

    /* (non-Javadoc)
      * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
      */
    public void selectionChanged(IAction action, ISelection selection) {}

}
