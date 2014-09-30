/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package contractnote;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

/**
 *
 * @author admin
 */
public class Contractnote {

    public static String newline = System.getProperty("line.separator");
    public static String delimiter = ",";
    public static String fileName;
    public static PDDocument pd;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, COSVisitorException {


        BufferedWriter wr;
        String broker = args[0];
        File input = new File("ContractNote.FO.20140924.pdf");  // The PDF file from where you would like to extract
        File output = new File("SampleText.txt"); // The text file where you are going to store the extracted data
        //pd = PDDocument.load(input);
        //System.out.println(pd.getNumberOfPages());
        // System.out.println(pd.isEncrypted());
        //pd.save("CopyOfInvoice.pdf"); // Creates a copy called "CopyOfInvoice.pdf"
        //PDFTextStripper stripper = new PDFTextStripper();
        // stripper.setStartPage(1); //Start extracting from page 3
        //stripper.setEndPage(2); //Extract till page 5
        wr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output)));

        //String contract = stripper.getText(pd);
        //String[] lines = contract.split(System.getProperty("line.separator"));
        ArrayList<Trade> trades = new ArrayList<>();
        File folder = new File(args[1]);
        for (File fileEntry : folder.listFiles()) {
            try {
                fileName = fileEntry.getName();
                pd = PDDocument.load(fileEntry);
                PDFTextStripper stripper = new PDFTextStripper();
                wr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output)));
                String contract = stripper.getText(pd);
                String[] lines = contract.split(System.getProperty("line.separator"));
                switch (broker) {

                    case "IBFNO":
                        importIBFNO(lines, args[2]);
                        break;
                    case "ZERODHA":
                        break;
                    default:
                        break;
                }
                if (pd != null) {
                    pd.close();
                }
                // I use close() to flush the stream.
                wr.close();
            } catch (Exception e) {
                System.out.println("File import Failed: " + fileName);
            } finally {
                if (pd != null) {
                    pd.close();
                }
            }
        }
    }

    public static void importIBFNO(String[] lines, String outputFileName) throws IOException {
        if (fileName.contains("FO")) {
            //System.out.println(fileEntry.getName());
            int iClientName = 7;
            int iTradeNumber = 0;
            int iOrderTime = 1;
            int iExecutionTime = 3;
            int iSide = 4;
            int iSymbol = 5;
            int iSize = -1;
            int iPrice = -1;
            int iBrokerage = -1;
            int iServiceTax = -1;
            int iOtherLevies = -1;
            String reference = null;
            String contractDate = null;
            String clientName = null;
            for (int i = 0; i < lines.length; i++) {
                String[] temp = lines[i].split(" ");
                if (temp.length == 4 && temp[0].equals("Contract") && temp[1].equals("Note") && temp[2].equals("Number") && isNumeric(temp[3])) {
                    reference = lines[i].split(" ")[3];
                }
                if (temp.length == 3 && temp[0].trim().equals("Trade") && temp[1].trim().equals("Date")) {
                    contractDate = lines[i].split(" ")[2];
                }
                if (temp.length > 2 && temp[0].equals("Client") && temp[1].equals("Name")) {
                    clientName = lines[i].substring(11);
                }

            }

            for (int i = 0; i < lines.length; i++) {
                String[] item = lines[i].split(" ");
                boolean error = false;
                if (isNumeric(item[0])) {
                    if (getDouble(item[0]) > 10000000) {
                        if (item.length < 11) {
                            //broken format. print line and move on
                            System.out.println("Error importing fileName:" + fileName + ",Line:" + lines[i]);
                            error = true;
                        }
                        if (!error) {
                            Trade tr = new Trade();
                            tr.fileName = fileName;
                            tr.clientName = clientName;
                            tr.tradeDate = contractDate;
                            tr.contractNoteReference = reference;
                            tr.tradeNumber = item[iTradeNumber];
                            tr.orderTime = item[iOrderTime];
                            tr.executionTime = item[iExecutionTime];
                            tr.side = item[iSide];
                            //find next numeric id
                            int sizeIndex = iSymbol;
                            for (int j = sizeIndex; j < item.length; j++) {
                                if (isNumeric(item[j])) {
                                    sizeIndex = j;
                                    break;
                                }
                            }
                            //now setup indexes as we know the size of the symbol
                            for (int counter = iSymbol; counter < sizeIndex; counter++) {
                                tr.symbol = tr.symbol == null ? item[counter] : tr.symbol + item[counter];
                            }
                            tr.size = getInteger(item[sizeIndex]);
                            tr.price = getDouble(item[sizeIndex + 1]);
                            tr.brokerage = getDouble(item[sizeIndex + 3]);
                            tr.serviceTax = getDouble(item[sizeIndex + 4]);
                            tr.stt = getDouble(item[sizeIndex + 5]);
                            tr.otherLevies = getDouble(item[sizeIndex + 6]);
                            writeToFile(outputFileName, tr);
                        }
                    }
                }
            }
        }
    }

    public static void writeToFile(String filename, Trade tr) throws IOException {

        File file = new File(filename);

        //if file doesnt exists, then create it
        boolean writeHeader = false;
        if (!file.exists()) {
            file.createNewFile();
            writeHeader = true;

        }
        FileWriter fileWritter = new FileWriter(file, true);

        BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
        if (writeHeader) {
            bufferWritter.write("fileName" + delimiter + "clientName" + delimiter + "contractNoteReference" + delimiter + "tradeDate" + delimiter + "tradeNumber" + delimiter + "orderTime" + delimiter + "executionTime" + delimiter
                    + "side" + delimiter + "symbol" + delimiter + "size" + delimiter + "price" + delimiter + "brokerage" + delimiter
                    + "serviceTax" + delimiter + "stt" + delimiter + "otherLevies" + delimiter + "netamount" + newline);

        }

        Double netamount = tr.side.equals("BUY") ? -tr.size * tr.price - tr.brokerage - tr.serviceTax - tr.stt - tr.otherLevies : tr.size * tr.price - tr.brokerage - tr.serviceTax - tr.stt - tr.otherLevies;
        bufferWritter.write(tr.fileName + delimiter + tr.clientName + delimiter + tr.contractNoteReference + delimiter + tr.tradeDate + delimiter + tr.tradeNumber + delimiter + tr.orderTime + delimiter + tr.executionTime + delimiter
                + tr.side + delimiter + tr.symbol + delimiter + tr.size + delimiter + tr.price + delimiter + tr.brokerage + delimiter
                + tr.serviceTax + delimiter + tr.stt + delimiter + tr.otherLevies + delimiter + netamount + newline);


        bufferWritter.close();
    }

    public static boolean isNumeric(String str) {
        Scanner scanner = new Scanner(str);
        if (scanner.hasNextInt()) {
            return true;
        } else if (scanner.hasNextDouble()) {
            return true;
        } else {
            return false;
        }
    }

    public static Integer getInteger(String str) {
        Scanner scanner = new Scanner(str);
        if (scanner.hasNextInt()) {
            return scanner.nextInt();
        } else {
            return 0;
        }
    }

    public static Double getDouble(String str) {
        Scanner scanner = new Scanner(str);
        if (scanner.hasNextDouble()) {
            return scanner.nextDouble();
        } else {
            return 0D;
        }
    }
}
