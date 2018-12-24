/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.editors.binary.internal;

import org.eclipse.osgi.util.NLS;

public class BinaryEditorMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.editors.binary.BinaryEditorMessages"; //$NON-NLS-1$

	public static String dialog_find_replace_1_replacement;
	public static String dialog_find_replace_backward;
	public static String dialog_find_replace_cancel;
	public static String dialog_find_replace_close;
	public static String dialog_find_replace_direction;
	public static String dialog_find_replace_error_;
	public static String dialog_find_replace_find;
	public static String dialog_find_replace_find_literal;
	public static String dialog_find_replace_find_replace;
	public static String dialog_find_replace_copy;
	public static String dialog_find_replace_paste;
	public static String dialog_find_replace_goto_line;
	public static String dialog_find_replace_undo;
	public static String dialog_find_replace_redo;
	public static String dialog_find_replace_forward;
	public static String dialog_find_replace_found_literal;
	public static String dialog_find_replace_ignore_case;
	public static String dialog_find_replace_literal_not_found;
	public static String dialog_find_replace_new_find;
	public static String dialog_find_replace_replace;
	public static String dialog_find_replace_replace_all;
	public static String dialog_find_replace_replace_find;
	public static String dialog_find_replace_replace_with;
	public static String dialog_find_replace_replacements;
	public static String dialog_find_replace_searching;
	public static String dialog_find_replace_stop;
	public static String dialog_find_replace_text;
	public static String dialog_go_to_button_close;
	public static String dialog_go_to_button_go_to_location;
	public static String dialog_go_to_button_show_location;
	public static String dialog_go_to_label_enter_location_number;
	public static String dialog_go_to_label_not_number;
	public static String dialog_go_to_label_out_of_range;
	public static String dialog_go_to_title;

	public static String editor_binary_hex_default_font;
	public static String editor_binary_hex_font_style_bold;
	public static String editor_binary_hex_font_style_bold_italic;
	public static String editor_binary_hex_font_style_italic;
	public static String editor_binary_hex_font_style_regular;
	public static String editor_binary_hex_froup_font_selection;
	public static String editor_binary_hex_label_available_fix_width_fonts;
	public static String editor_binary_hex_label_name;
	public static String editor_binary_hex_label_style;
	public static String editor_binary_hex_label_size;
	public static String editor_binary_hex_sample_text;
	public static String editor_binary_hex_status_line_offset;
	public static String editor_binary_hex_status_line_selection;
	public static String editor_binary_hex_status_line_text_insert;
	public static String editor_binary_hex_status_line_text_ovewrite;
	public static String editor_binary_hex_status_line_value;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, BinaryEditorMessages.class);
	}

	private BinaryEditorMessages() {
	}
}
