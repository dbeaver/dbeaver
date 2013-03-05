package org.jkiss.dbeaver.tools.transfer;

import org.eclipse.swt.graphics.Image;

/**
 * Transformer
 */
public interface IDataTransferProcessor {

    String getId();

    String getName();

    String getDescription();

    Image getIcon();
}
