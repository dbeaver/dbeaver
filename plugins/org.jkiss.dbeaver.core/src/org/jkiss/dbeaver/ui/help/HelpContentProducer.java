/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.help;

import org.eclipse.help.IHelpContentProducer;

import java.io.InputStream;
import java.util.Locale;

public class HelpContentProducer implements IHelpContentProducer {

	@Override
    public InputStream getInputStream(String pluginID, String href, Locale locale) {
        if (href.equals(IHelpContextIds.CTX_DRIVER_EDITOR)) {

        } else if (href.startsWith(IHelpContextIds.CTX_DRIVER_EDITOR)) {
            final String driverId = href.substring(IHelpContextIds.CTX_DRIVER_EDITOR.length() + 1);
            System.out.println(driverId);
        }
		return null;
	}

}
