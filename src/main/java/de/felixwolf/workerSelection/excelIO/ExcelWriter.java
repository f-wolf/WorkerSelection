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

/**
 * Class which is used to create the output file
 */

public class ExcelWriter {

	private Workbook wb = new XSSFWorkbook();
	private CreationHelper createHelper = wb.getCreationHelper();

	private static final Logger LOGGER = LoggerFactory.getLogger(ExcelWriter.class);

	/**
	 * Writes the schedule to the output file.
	 * @param outputs 			ArrayList of String arrays, each array is one line, each string of the arrays is one cell
	 * @throws IOException
	 */
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

	/**
	 * Creates a sheet for the worker information. Writes down how often they were active and the date of the last activity.
	 * @param allWorkers
	 */
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
			dateCell.setCellValue(lastActive);
			dateCell.setCellStyle(cellDateStyle);

			// write the counters
			for(int c = 0; c < oneCounter.length; c++){

				row.createCell(2 + c).setCellValue(allWorkers.get(i).getCounter()[c]);
			}
		}
	}

	/**
	 * saves the file
	 * @throws IOException
	 */
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
}
