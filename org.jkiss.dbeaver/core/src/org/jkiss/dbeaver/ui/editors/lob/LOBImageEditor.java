package org.jkiss.dbeaver.ui.editors.lob;

import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ui.controls.imageview.ImageViewControl;

/**
 * LOB text editor
 */
public class LOBImageEditor extends EditorPart {

    private ImageViewControl imageViewer;

    public void doSave(IProgressMonitor monitor) {
    }

    public void doSaveAs() {
    }

    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
    }

    public boolean isDirty() {
        return false;
    }

    public boolean isSaveAsAllowed() {
        return false;
    }

    public void createPartControl(Composite parent) {
        imageViewer = new ImageViewControl(parent);
    }

    public void setFocus() {
        imageViewer.setFocus();
    }
}