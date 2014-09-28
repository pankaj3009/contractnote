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
            public static String delimiter=",";
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, COSVisitorException {
       
        PDDocument pd;
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
        File folder=new File(args[1]);  
        for(File fileEntry:folder.listFiles()){
            pd=PDDocument.load(fileEntry);
            PDFTextStripper stripper = new PDFTextStripper();
            wr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output)));
            String contract = stripper.getText(pd);
            String[] lines = contract.split(System.getProperty("line.separator"));
             switch (broker) {
            case "IBFNO":
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
                String reference = lines[19].split(" ")[3];
                String contractDate = lines[20].split(" ")[2];

                for (int i = 0; i < lines.length; i++) {
                    String[] item = lines[i].split(" ");
                    if (isNumeric(item[0])) {
                        Trade tr = new Trade();
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
                        trades.add(tr);
                    }
                }
                break;

            default:
                break;

        }
                if (pd != null) {
            pd.close();
        }
        // I use close() to flush the stream.
        wr.close();
        }
        



    }

             public static void writeToFile(String filename, Trade tr) throws IOException {
        
            File file = new File(filename+".csv");

            //if file doesnt exists, then create it
            boolean writeHeader=false;
            if (!file.exists()) {
                file.createNewFile();
                writeHeader=true;
                
            }
           FileWriter fileWritter = new FileWriter(file, true);
           
           BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
           if(writeHeader){
                                bufferWritter.write("contractNoteReference"+delimiter+"tradeDate"+delimiter+"tradeNumber"+delimiter+"orderTime"+delimiter+"executionTime"+delimiter
                   +"side"+delimiter+"symbol"+delimiter+"size"+delimiter+"price"+delimiter+"brokerage"+delimiter
                   +"serviceTax"+delimiter+"stt"+delimiter+"otherLevies"+delimiter+"netamount"+newline);

            }

            Double netamount=tr.side.equals("BUY")?-tr.size*tr.price-tr.brokerage-tr.serviceTax-tr.stt-tr.otherLevies:tr.size*tr.price-tr.brokerage-tr.serviceTax-tr.stt-tr.otherLevies;
            bufferWritter.write(tr.contractNoteReference+delimiter+tr.tradeDate+delimiter+tr.tradeNumber+delimiter+tr.orderTime+delimiter+tr.executionTime+delimiter
                   +tr.side+delimiter+tr.symbol+delimiter+tr.size+delimiter+tr.price+delimiter+tr.brokerage+delimiter
                   +tr.serviceTax+delimiter+tr.stt+delimiter+tr.otherLevies+delimiter+netamount+newline);
            

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
//return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
    }

    public static Integer getInteger(String str) {
        Scanner scanner = new Scanner(str);
        if (scanner.hasNextInt()) {
            return scanner.nextInt();
        } else {
            return 0;
        }
//return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
    }

    public static Double getDouble(String str) {
        Scanner scanner = new Scanner(str);
        if (scanner.hasNextDouble()) {
            return scanner.nextDouble();
        } else {
            return 0D;
        }
//return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
    }
}
