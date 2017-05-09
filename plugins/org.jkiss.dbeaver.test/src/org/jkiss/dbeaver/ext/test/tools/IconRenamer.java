package org.jkiss.dbeaver.ext.test.tools;

import java.io.File;
import java.io.FilenameFilter;

public class IconRenamer {

	public static void main(String[] args) {
		//fixIconSet1();
		fixIconSet2();
	}

	private static void fixIconSet1() {
		File[] icons = new File("C:\\devel\\my\\ext\\new-icons\\Icons_set1\\").listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".png");
			}
		});
		if (icons != null) {
			for (File icon : icons) {
				String fileName = icon.getName();
				fileName = fileName.substring(6);
				int divPos = fileName.lastIndexOf('_');
				fileName = fileName.substring(0, divPos) + fileName.substring(divPos + 6);
				fileName = fileName.toLowerCase();
				System.out.println(fileName);

				icon.renameTo(new File(icon.getParent(), fileName));
			}
		}
	}

	private static void fixIconSet2() {
		File[] icons = new File("C:\\devel\\my\\ext\\new-icons\\Icons_set2\\").listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".png");
			}
		});
		if (icons != null) {
			for (File icon : icons) {
				String fileName = icon.getName().toLowerCase();
				int divPos = fileName.lastIndexOf('@');
				if (divPos != -1) {
					fileName = fileName.substring(0, divPos) + "@2x.png";
				}
				System.out.println(fileName);

				icon.renameTo(new File(icon.getParent(), fileName));
			}
		}
	}

}
