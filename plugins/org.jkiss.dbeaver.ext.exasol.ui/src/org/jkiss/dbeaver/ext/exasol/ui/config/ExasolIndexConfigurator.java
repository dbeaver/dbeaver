package org.jkiss.dbeaver.ext.exasol.ui.config;

import java.util.Arrays;

import org.jkiss.dbeaver.ext.exasol.model.ExasolTableColumn;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableIndex;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableIndexColumn;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditIndexPage;


public class ExasolIndexConfigurator implements DBEObjectConfigurator<ExasolTableIndex> {


	@Override
	public ExasolTableIndex configureObject(DBRProgressMonitor monitor, Object container, ExasolTableIndex index) {
		return UITask.run(() -> {
				EditIndexPage editPage = new EditIndexPage(
						"create index",
						index,
						Arrays.asList(new DBSIndexType("LOCAL","LOCAL"), new DBSIndexType("GLOBAL","GLOBAL"))
					);
				if (!editPage.edit()) {
					return null;
				}
				
				index.setIndexType(editPage.getIndexType());
				index.setName(index.getIndexType().getName() + " INDEX " + index.getSimpleColumnString());
				int colIndex = 1;
				for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
					index.addColumn(
								new ExasolTableIndexColumn(index, (ExasolTableColumn) tableColumn, colIndex++)
							);
				}
				return index;
			});
		}


}
