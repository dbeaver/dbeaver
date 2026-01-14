/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

package org.jfree.chart.swt;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.Composite;

/**
 * Printer job for SWT.
 */
public class ChartPrintJob {

    /**
     * Border on the paper not to print on (in inches)
     */
    private static final double BORDER = 0.8;

    private String jobName;

    /**
     * Creates a new job.
     * 
     * @param jobName  this will be the name of the print job or the default 
     *     name of the file (if printed to a file).
     */
    public ChartPrintJob(String jobName) {
        this.jobName = jobName;
    }

    /** 
     * Prints the specified element.
     * 
     * @param elementToPrint  the {@link Composite} to be printed.
     */
    public void print(Composite elementToPrint) {
        PrintDialog dialog = new PrintDialog(elementToPrint.getShell());
        PrinterData printerData = dialog.open();
        if (printerData == null) {
            return; // Anwender hat abgebrochen.
        }
        startPrintJob(elementToPrint, printerData);
    }

    protected void startPrintJob(Composite elementToPrint, 
            PrinterData printerData) {
        Printer printer = new Printer(printerData);
        try {
            printer.startJob(jobName);

            GC gc = new GC(printer);
            try {
                Rectangle printArea = getPrintableArea(printer, BORDER);
                printer.startPage();
                printComposite(elementToPrint, gc, printArea);
                printer.endPage();
            } finally {
                gc.dispose();
            }
            printer.endJob();

        } finally {
            printer.dispose();
        }
    }

    /**
     * @param printer
     * @param safetyBorder
     * @return the rectangle in pixels to print on (and which is also supported
     * by the printer hardware)
     */
    private Rectangle getPrintableArea(Printer printer, double safetyBorder) {
        int safetyBorderWidth = (int) (safetyBorder * printer.getDPI().x);
        int safetyBorderHeight = (int) (safetyBorder * printer.getDPI().y);

        Rectangle trim = printer.computeTrim(0, 0, 0, 0);
        int trimLeft = -trim.x;
        int trimTop = -trim.y;
        int trimRight = trim.x + trim.width;
        int trimBottom = trim.y + trim.height;

        int marginLeft = Math.max(trimLeft, safetyBorderWidth);
        int marginRight = Math.max(trimRight, safetyBorderWidth);
        int marginTop = Math.max(trimTop, safetyBorderHeight);
        int marginBottom = Math.max(trimBottom, safetyBorderHeight);

        int availWidth = printer.getClientArea().width - marginLeft
                - marginRight;
        int availHeight = printer.getClientArea().height - marginTop
                - marginBottom;
        return new Rectangle(marginLeft, marginTop, availWidth, availHeight);
    }

    private void printComposite(Composite elementToPrint, GC gc, 
            Rectangle printArea) {
        Image image = new Image(elementToPrint.getDisplay(),
                elementToPrint.getSize().x, elementToPrint.getSize().y);
        try {
            GC imageGC = new GC(image);
            try {
                elementToPrint.print(imageGC);
                Point fittedSize = calcFittedSize(printArea, 
                        elementToPrint.getSize());
                gc.drawImage(image, 0, 0, elementToPrint.getSize().x,
                        elementToPrint.getSize().y, printArea.x, printArea.y, 
                        fittedSize.x, fittedSize.y);

            } finally {
                imageGC.dispose();
            }
        } finally {
            image.dispose();
        }
    }

    /**
     * The object to print should be scaled up or down to fit horizontally and
     * vertically the available space.
     *     
     * @param printArea
     * @param originalSize
     * 
     * @return the fitted size of the object to print
     */
    Point calcFittedSize(Rectangle printArea, Point originalSize) {
        double scaleFactor = Math.min(
                (double) printArea.height / originalSize.y,
                (double) printArea.width / originalSize.x);
        int trgHeight = (int) Math.ceil(originalSize.y * scaleFactor);
        int trgWidth = (int) Math.ceil(originalSize.x * scaleFactor);
        return new Point(trgWidth, trgHeight);
    }

}