/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 15, 2004
 */
package org.jkiss.dbeaver.ext.erd.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.palette.CombinedTemplateCreationEntry;
import org.eclipse.gef.palette.ConnectionCreationToolEntry;
import org.eclipse.gef.palette.MarqueeToolEntry;
import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteEntry;
import org.eclipse.gef.palette.PaletteGroup;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.palette.PaletteSeparator;
import org.eclipse.gef.palette.SelectionToolEntry;
import org.eclipse.gef.palette.ToolEntry;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.jkiss.dbeaver.ext.erd.Activator;
import org.jkiss.dbeaver.ext.erd.dnd.DataElementFactory;
import org.jkiss.dbeaver.ext.erd.model.Column;
import org.jkiss.dbeaver.ext.erd.model.Table;

/**
 * Encapsulates functionality to create the PaletteViewer
 * @author Phil Zoio
 */
public class PaletteViewerCreator
{

	/** the palette root */
	private PaletteRoot paletteRoot;


	/**
	 * Returns the <code>PaletteRoot</code> this editor's palette uses.
	 * 
	 * @return the <code>PaletteRoot</code>
	 */
	public PaletteRoot createPaletteRoot()
	{
		// create root
		paletteRoot = new PaletteRoot();

		// a group of default control tools
		PaletteGroup controls = new PaletteGroup("Controls");
		paletteRoot.add(controls);

		// the selection tool
		ToolEntry tool = new SelectionToolEntry();
		controls.add(tool);

		// use selection tool as default entry
		paletteRoot.setDefaultEntry(tool);

		// the marquee selection tool
		controls.add(new MarqueeToolEntry());

		// a separator
		PaletteSeparator separator = new PaletteSeparator(Activator.PLUGIN_ID + ".palette.seperator");
		separator.setUserModificationPermission(PaletteEntry.PERMISSION_NO_MODIFICATION);
		controls.add(separator);

		controls.add(new ConnectionCreationToolEntry("Connections", "Create Connections", null, AbstractUIPlugin
				.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons/relationship.gif"),
				AbstractUIPlugin
						.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons/relationship.gif")));

		PaletteDrawer drawer = new PaletteDrawer("New Component", AbstractUIPlugin.imageDescriptorFromPlugin(
				Activator.PLUGIN_ID, "icons/connection.gif"));

		List<CombinedTemplateCreationEntry> entries = new ArrayList<CombinedTemplateCreationEntry>();

		CombinedTemplateCreationEntry tableEntry = new CombinedTemplateCreationEntry("New Table", "Create a new table",
				Table.class, new DataElementFactory(Table.class), AbstractUIPlugin.imageDescriptorFromPlugin(
						Activator.PLUGIN_ID, "icons/table.gif"), AbstractUIPlugin
						.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons/table.gif"));

		CombinedTemplateCreationEntry columnEntry = new CombinedTemplateCreationEntry("New Column", "Add a new column",
				Column.class, new DataElementFactory(Column.class), AbstractUIPlugin.imageDescriptorFromPlugin(
						Activator.PLUGIN_ID, "icons/column.gif"), AbstractUIPlugin
						.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons/column.gif"));

		entries.add(tableEntry);
		entries.add(columnEntry);

		drawer.addAll(entries);

		paletteRoot.add(drawer);

		// todo add your palette drawers and entries here
		return paletteRoot;

	}

	/**
	 * @return Returns the paletteRoot.
	 */
	public PaletteRoot getPaletteRoot()
	{
		return paletteRoot;
	}
}