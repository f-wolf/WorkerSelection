package de.felixwolf.workerSelection.dataTypes;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.TimeZone;

/**
 * The settings class is used to organize all settings which can be found on the settings sheet of the input file
 */

public class Settings {

    private static final Logger LOGGER = LoggerFactory.getLogger(Settings.class);

    private static Sheet sheet_settings;
    private static Workbook workbook;

    private static TimeZone timeZone = TimeZone.getTimeZone("Europe/Berlin");
    private static int coolDownTime = 8;

    /**
     * initializes all settings of the settings sheet
     * @param path
     */
    public static void init(String path){

        initSettingsSheet(path);
        initSettings();

        try {
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        LOGGER.info("The settings are read in.");
    }

    /**
     * Initializes all settings it can find in the first n rows
     */
    private static void initSettings(){

        int nrows = 100; // opt Todo: read this number from the settings sheet

        for(int rownNum = 1; rownNum <= nrows; rownNum++){

            Row row = sheet_settings.getRow(rownNum);
            if(row == null){
                continue;
            }

            Cell settingKeyCell = row.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if(isEmptyKeyCell(settingKeyCell)){
                // this is an empty cell
                continue;
            }

            String key = settingKeyCell.getStringCellValue();
            key = key.trim().toLowerCase();
            Cell valueCell = row.getCell(1, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

            readSetting(key, valueCell);
        }
    }

    /**
     * Method to call the right method to read the given valueCell
     * @param key
     * @param valueCell
     */
    private static void readSetting(String key, Cell valueCell){

        switch (key){
            case "cooldowntime": readCoolDownTime(valueCell); break;
            case "timezone": readTimeZone(valueCell); break;

            default: LOGGER.warn("Unknown setting key detected: " + key + ". It is not read in.");
        }
    }

    /**
     * initializes the time zone
     */
    private static void readTimeZone(Cell valueCell){
        String timeZoneInput;
        try{
            timeZoneInput = valueCell.getStringCellValue();

            // check whether the input is a valid timezone
            String[] allTimeZonesArray = TimeZone.getAvailableIDs();
            if(Arrays.asList(allTimeZonesArray).contains(timeZoneInput)){
                timeZone = TimeZone.getTimeZone(timeZoneInput);
            }
            else {
                LOGGER.warn("The input is not a valid timezone. Default 'Europe/Berlin' will be used");
            }

        } catch (Exception e){
            LOGGER.error("The timzone could not be read. Default 'Europe/Berlin' will be used");
        }
    }

    /**
     * Reads the cool down time of the settings sheet. The cool down time is the minimum time length in days of inactivity
     * after the last assignment.
     * @return
     */
    private static void readCoolDownTime(Cell valueCell){
        try {
            coolDownTime = (int) valueCell.getNumericCellValue();
        } catch (Exception e){
            LOGGER.warn("The cool down time could not be read from the settings. Using default of 8");
        }
    }

    /**
     * Initializes the settings sheet from the input file
     * @param path
     */
    private static void initSettingsSheet(String path){
        try {

            if(isXLSX(path)){
                workbook = new XSSFWorkbook(new File(path));
            }
            else {
                InputStream inp = new FileInputStream(path);
                workbook = WorkbookFactory.create(inp);
            }

        } catch (Exception e) {
            LOGGER.error("Error reading the input file. Please make sure it is a valid excel file.");
            e.printStackTrace();
        }
        sheet_settings = workbook.getSheet("Settings");
    }

    /**
     * Check whether a key cell is completely empty (null) or consisting of only spaces
     * @param cell
     * @return
     */
    private static boolean isEmptyKeyCell(Cell cell){

        if(cell == null){
            return true;
        }

        String cellContent = " ";
        try {
            cellContent = cell.getStringCellValue();
        } catch (Exception e){
            return true;
        }

        if(cellContent.matches("\\s+")){
            return true;
        }

        return false;
    }

    /**
     * Copy from excelreader
     * @param path
     * @return
     */
    private static boolean isXLSX(String path){

        String [] splitPath = path.split("\\.");

        String fileExtension = splitPath[splitPath.length-1];
        fileExtension = fileExtension.toLowerCase();

        switch (fileExtension){
            case "xlsx": return true;
            case "xls": return false;
            default: LOGGER.error("Wrong input file type: " + fileExtension);
                LOGGER.error("Please check the input file. The program will terminate now");
                System.exit(65);
        }
        return false;
    }

    public static int getCoolDownTime() {
        return coolDownTime;
    }

    public static TimeZone getTimeZone() {
        return timeZone;
    }
}
