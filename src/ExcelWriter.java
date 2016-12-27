import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jxl.CellView;
import jxl.DateCell;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableCell;
import jxl.write.WritableCellFormat;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

public class ExcelWriter {

	WritableWorkbook result;
	

	public ExcelWriter() throws IOException {
		// make new Excelfile
		// creating a filename based on the current date and time
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		// get current date time with Date()
		Date date = new Date();
		String filename = dateFormat.format(date) + "_output.xls";

		result = Workbook.createWorkbook(new File(filename));
		
	}

	
	public void writeResult(ArrayList<String[]> outputs) throws IOException, RowsExceededException, WriteException {
		
		result.createSheet("Result", 0);
		WritableSheet sheet0  = result.getSheet(0);
		for (int row = 0; row < outputs.size(); row++) {
			String[] rowString = outputs.get(row);
			for (int col = 0; col < rowString.length; col++) {
				Label label = new Label(col, row, rowString[col]);
				sheet0.addCell(label);
			}

		}
		
		// change width of columns
		
		int [] widthInCharacters = {3, 11, 12, 6, 17, 17, 17};
		
		for (int i = 0; i < 7; i++) {
			CellView cv = sheet0.getColumnView(i);
			cv.setSize(widthInCharacters[i] * 256); 
			//cv.setAutosize(true);
			sheet0.setColumnView(i, cv);
		}
		



	}
	
	public void writeWorkers(ArrayList<Worker> allWorkers) throws RowsExceededException, WriteException {
		result.createSheet("Workers", 1);
		WritableSheet sheet1  = result.getSheet(1);
		
		Label label = new Label(0, 0, "Name");
		sheet1.addCell(label);
		
		Label label2 = new Label(1, 0, "last active");
		sheet1.addCell(label2);
		
		// make the legend for all counters
		int [] oneCounter = allWorkers.get(0).getCounter();
		for(int c = 0; c < oneCounter.length; c++){
			label = new Label(2 + c, 0,  "counter " + c);
			sheet1.addCell(label);
		}
		
		// write all workers
		for(int i = 0; i < allWorkers.size(); i++){
			Label name = new Label(0, i + 1, allWorkers.get(i).getName());
			sheet1.addCell(name);
			
			// write last active date
			Date lastActive = allWorkers.get(i).getLastDate();
			WritableCellFormat format = new jxl.write.WritableCellFormat(new jxl.write.DateFormat("m/d/yyyy h:mm"));
			WritableCell cell = new jxl.write.DateTime(1, i+1, lastActive, format);
			sheet1.addCell(cell);
			
			// write the counters
			for(int c = 0; c < oneCounter.length; c++){
				sheet1.addCell(new jxl.write.Number(c + 2, i + 1, allWorkers.get(i).getCounter()[c]));
			}
			
			// old solution
			//Label count = new Label(i + 1, 1, String.valueOf(allWorkers.get(i).getCounter()[0]));
			//sheet1.addCell(count);
		}
		
	}
	
	
	public void finishFile() throws IOException, WriteException{
		result.write();
		result.close();
	}


	
	
	

	/*
	 * public static void writeOutput(TheCalendar aCalendar) throws IOException,
	 * WriteException {
	 * 
	 * int currentRow = 1;
	 * 
	 * theCalendar = aCalendar.getTheCalendar(); sortedCalendar =
	 * aCalendar.getSortedCalendar(); Map<Integer, Worker> selectedWorkers = new
	 * HashMap<Integer, Worker>(); // reihenfolge // der // Tasks
	 * 
	 * // make new Excelfile
	 * 
	 * // Dateiname DateFormat dateFormat = new
	 * SimpleDateFormat("yyyyMMddHHmmss"); // get current date time with Date()
	 * Date date = new Date(); String filename = dateFormat.format(date) +
	 * "_output.xls";
	 * 
	 * WritableWorkbook result = Workbook.createWorkbook(new File(filename));
	 * result.createSheet("Result", 0); WritableSheet sheet0 =
	 * result.getSheet(0);
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
	 * for (int b = 0; b < sortedWorkers.size(); b++) { Worker w1 =
	 * sortedWorkers.get(b); //
	 * System.out.println("ExcelMaster:WriteExcel: Name of Worker: " // +
	 * w1.getName() + " b: " + b + " Size: " + // selectedWorkers.size()); Label
	 * labelWorker = new Label(1 + b, currentRow, w1.getName());
	 * sheet0.addCell(labelWorker); }
	 * 
	 * currentRow++; }
	 * 
	 * result.write(); result.close();
	 * 
	 * }
	 */

}
