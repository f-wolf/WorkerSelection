package de.felixwolf.workerSelection.excelIO;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import de.felixwolf.workerSelection.dataTypes.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExcelWriter {

	private Workbook wb = new XSSFWorkbook();
	private CreationHelper createHelper = wb.getCreationHelper();

	private static final Logger LOGGER = LoggerFactory.getLogger(ExcelWriter.class);



	public ExcelWriter() throws IOException {
		// make new Excelfile



		//wb = Workbook.createWorkbook(new File(filename));
	}


	
	public void writeResult(ArrayList<String[]> outputs) throws IOException {

		Sheet sheet_result = wb.createSheet("Result");

		for (int rowNum = 0; rowNum < outputs.size(); rowNum++) {
			String[] rowString = outputs.get(rowNum);

			Row row = sheet_result.createRow((short)rowNum);

			for (int col = 0; col < rowString.length; col++) {

				row.createCell(col).setCellValue(createHelper.createRichTextString(rowString[col]));
			}
		}
		
		//int columnCount = outputs.get(0).length;
		int [] widthInCharacters = {3, 11, 12, 6, 17, 17, 17};
		for(int i = 0; i < widthInCharacters.length; i++){
			sheet_result.setColumnWidth(i, widthInCharacters[i] * 256);
		}

	}


	public void writeWorkers(ArrayList<Worker> allWorkers)  {

		Sheet sheet_workers = wb.createSheet("Workers");

		CellStyle cellDateStyle = wb.createCellStyle();
		cellDateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd.mm.yyyy"));

		Row legendRow = sheet_workers.createRow(0);

		legendRow.createCell(0).setCellValue(createHelper.createRichTextString("Name"));
		legendRow.createCell(1).setCellValue(createHelper.createRichTextString("last active"));
		// make the legend for all counters
		int [] oneCounter = allWorkers.get(0).getCounter();
		for(int c = 0; c < oneCounter.length; c++){
			legendRow.createCell(2 + c).setCellValue(createHelper.createRichTextString("counter " + c));
		}

		// write all workers
		for(int i = 0; i < allWorkers.size(); i++){

			Row row = sheet_workers.createRow(i + 1);

			// write the worker's name
			row.createCell(0).setCellValue(createHelper.createRichTextString(allWorkers.get(i).getName()));

			// write last active date
			Date lastActive = allWorkers.get(i).getLastDate();
			Cell dateCell = row.createCell(1);
			dateCell.setCellValue(lastActive);// todo correctly formated?
			dateCell.setCellStyle(cellDateStyle);

			// write the counters
			for(int c = 0; c < oneCounter.length; c++){

				row.createCell(2 + c).setCellValue(allWorkers.get(i).getCounter()[c]);

			}
		}
	}

	public void finishFile() throws IOException{

		// creating a filename based on the current date and time
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		// get current date time with Date()
		Date date = new Date();
		String filename = dateFormat.format(date) + "_output.xlsx";

		FileOutputStream fileOut = new FileOutputStream(filename);
		wb.write(fileOut);
		fileOut.close();
	}


	
	
	

	/*
	 * public static void writeOutput(TheCalendar aCalendar) throws IOException,
	 * WriteException {
	 * 
	 * int currentRow = 1;
	 * 
	 * theCalendar = aCalendar.getTheCalendar(); sortedCalendar =
	 * aCalendar.getSortedCalendar(); Map<Integer, main.java.de.felixwolf.workerSelection.dataTypes.Worker> selectedWorkers = new
	 * HashMap<Integer, main.java.de.felixwolf.workerSelection.dataTypes.Worker>(); // reihenfolge // der // Tasks
	 * 
	 * // make new Excelfile
	 * 
	 * // Dateiname DateFormat dateFormat = new
	 * SimpleDateFormat("yyyyMMddHHmmss"); // get current date time with Date()
	 * Date date = new Date(); String filename = dateFormat.format(date) +
	 * "_output.xls";
	 * 
	 * WritableWorkbook wb = Workbook.createWorkbook(new File(filename));
	 * wb.createSheet("Result", 0); WritableSheet sheet0 =
	 * wb.getSheet(0);
	 * 
	 * // fill with Content
	 * 
	 * for (int a : sortedCalendar) { SEvent e1 = theCalendar.get(a);
	 * 
	 * Label labelDay = new Label(0, currentRow, e1.getTextDay()+ ", der " +
	 * e1.getTextDate() + " i: " + e1.getIntDate()); sheet0.addCell(labelDay);
	 * 
	 * // selectedWorkers = e1.getSelectedWorkers(); // sortedWorkers = new
	 * ArrayList<Integer>(selectedWorkers.keySet());
	 * 
	 * sortedWorkers = e1.getSelectedWorkers();
	 * 
	 * // System.out.println(
	 * "ExcelMaster:WriteExcel: soviele Arbeiter sind asugew�hlt: " // +
	 * sortedWorkers.size());
	 * 
	 * for (int b = 0; b < sortedWorkers.size(); b++) { main.java.de.felixwolf.workerSelection.dataTypes.Worker w1 =
	 * sortedWorkers.get(b); //
	 * System.out.println("ExcelMaster:WriteExcel: Name of main.java.de.felixwolf.workerSelection.dataTypes.Worker: " // +
	 * w1.getName() + " b: " + b + " Size: " + // selectedWorkers.size()); Label
	 * labelWorker = new Label(1 + b, currentRow, w1.getName());
	 * sheet0.addCell(labelWorker); }
	 * 
	 * currentRow++; }
	 * 
	 * wb.write(); wb.close();
	 * 
	 * }
	 */

}
