package org.jkiss.dbeaver.data.htmlcopy.transfer.stream;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.HTMLTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferConsumer;
import org.jkiss.dbeaver.ui.UIUtils;

public class StreamTransfertConsumerHtmlCopy extends StreamTransferConsumer {
	
    public void finishTransfer(DBRProgressMonitor monitor) {


            if (outputBuffer != null) {
                UIUtils.syncExec(() -> {
                	HTMLTransfer textTransfer = HTMLTransfer.getInstance();
                    new Clipboard(UIUtils.getDisplay()).setContents(
                        new Object[]{outputBuffer.toString()},
                        new Transfer[]{textTransfer});
                });
                outputBuffer = null;
            }
       
    }

}
