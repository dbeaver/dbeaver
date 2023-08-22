package org.jkiss.dbeaver.ext.dm.ui.config;

import java.util.Collections;
import java.util.Map;

import org.jkiss.dbeaver.ext.dm.model.DmTableColumn;
import org.jkiss.dbeaver.ext.dm.model.DmTableIndex;
import org.jkiss.dbeaver.ext.dm.model.DmTableIndexColumn;
import org.jkiss.dbeaver.ext.dm.ui.internal.DmUIMessages;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditIndexPage;
import org.jkiss.utils.CommonUtils;

public class DmIndexConfigurator implements DBEObjectConfigurator<DmTableIndex> {

	@Override
	public DmTableIndex configureObject(DBRProgressMonitor monitor, Object container, DmTableIndex index,Map<String, Object> options) {
        return UITask.run(() -> {
            EditIndexPage editPage = new EditIndexPage(
                DmUIMessages.edit_dm_index_manager_dialog_title,
                index,
                Collections.singletonList(DBSIndexType.OTHER));
            if (!editPage.edit()) {
                return null;
            }

            StringBuilder idxName = new StringBuilder(64);
            idxName.append(CommonUtils.escapeIdentifier(index.getTable().getName())).append("_") //$NON-NLS-1$
                .append(CommonUtils.escapeIdentifier(editPage.getSelectedAttributes().iterator().next().getName()))
                .append("_IDX"); //$NON-NLS-1$
            index.setName(DBObjectNameCaseTransformer.transformName(index.getDataSource(), idxName.toString()));
            index.setUnique(editPage.isUnique());
            index.setIndexType(editPage.getIndexType());
            int colIndex = 1;
            for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
                index.addColumn(
                    new DmTableIndexColumn(
                        index,
                        (DmTableColumn) tableColumn,
                        colIndex++,
                        !Boolean.TRUE.equals(editPage.getAttributeProperty(tableColumn, EditIndexPage.PROP_DESC)),
                        null));
            }
            return index;
        });
	}

}
