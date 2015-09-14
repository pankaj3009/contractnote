/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package contractnote;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private static final Logger logger = Logger.getLogger(Contractnote.class.getName());
private static final String DIGIT_PATTERN = "\\d+";
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, COSVisitorException {
        usage();
        BufferedWriter wr;
        String broker = args[0];
        //File input = new File("HODP0008_12112012_2.pdf");  // The PDF file from where you would like to extract
        //File output = new File("SampleText.txt"); // The text file where you are going to store the extracted data
        //pd = PDDocument.load(input);
        //System.out.println(pd.getNumberOfPages());
        // System.out.println(pd.isEncrypted());
        //pd.save("CopyOfInvoice.pdf"); // Creates a copy called "CopyOfInvoice.pdf"
        //PDFTextStripper stripper = new PDFTextStripper();
        // stripper.setStartPage(1); //Start extracting from page 3
        //stripper.setEndPage(2); //Extract till page 5
//        wr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output)));

        //String contract = stripper.getText(pd);
        //String[] lines = contract.split(System.getProperty("line.separator"));
        ArrayList<Trade> trades = new ArrayList<>();
        File folder = new File(args[1]);
        FileInputStream configFile;
        if (new File("logging.properties").exists()) {
            configFile = new FileInputStream("logging.properties");
            LogManager.getLogManager().readConfiguration(configFile);
        }
        for (File fileEntry : folder.listFiles()) {
            try {

                fileName = fileEntry.getName();
                if(fileName.contains("pdf") ){
                System.out.println(fileEntry.getName());
                pd = PDDocument.load(fileEntry);
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(0);
                if(broker.equals("IBFNO")){
                stripper.setEndPage(pd.getNumberOfPages());                    
                }else if (broker.equals("ZERODHA")){
                stripper.setEndPage(pd.getNumberOfPages() - 1);
                }
//                wr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output)));
                String contract="";
                try{
                 contract = stripper.getText(pd);
                }catch (Exception e){
                stripper.setEndPage(pd.getNumberOfPages());
                 contract = stripper.getText(pd);              }
                
                String[] lines = contract.split(System.getProperty("line.separator"));
                switch (broker) {

                    case "IBFNO":
                        importIBFNO(lines, args[2]);
                        break;
                    case "ZERODHA":
                        importZerodha(lines, args[2], fileName);
                        break;
                    default:
                        break;
                }
                if (pd != null) {
                    pd.close();
                }
                // I use close() to flush the stream.
//                wr.close();
                }
                } catch (Exception e) {
                    logger.log(Level.SEVERE,null,e);
                logger.log(Level.SEVERE, "Error reading pdf: {0}", new Object[]{fileEntry});
            } finally {
                if (pd != null) {
                    pd.close();
                }
            }
        }
    }

    public static void usage() {
        System.out.println("Requires three inputs");
        System.out.println("Input 1: IBFNO or ZERODHA, case sensitive!!");
        System.out.println("Input 2: Directory containing contract notes");
        System.out.println("Input 3: output file name");
    }

    public static void importIBFNO(String[] lines, String outputFileName) throws IOException {
        if (fileName.contains("FO")) {
            //System.out.println(fileEntry.getName());
            int iClientName = 7;
            int iTradeNumber = 2;
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
                    contractDate = lines[i].split(" ")[2].replace("-", "");
                }
                if (temp.length > 2 && temp[0].equals("Client") && temp[1].equals("Name")) {
                    clientName = lines[i].substring(11);
                }

            }

            for (int i = 0; i < lines.length; i++) {
                String[] item = lines[i].split(" ");
                boolean error = false;
                //if (isNumeric(item[0])) {
                if (getDouble(item[0]) > 10000000 || item[0].trim().equals("N/A")) {
                    if (item.length < 11) {
                        //broken format. print line and move on
                        String line = lines[i] + " " + lines[i + 1] + " " + lines[i + 2];
                        item = line.split(" ");
                        if (item.length < 11) {
                            error = true;
                            logger.log(Level.SEVERE, "Error importing fileName:{0},Line:{1}", new Object[]{fileName, lines[i]});

                        }
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
                        tr.side = item[iSide].equals("BUY") ? "Buy" : "Sell";
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
                        tr.size = Math.abs(getInteger(item[sizeIndex]));
                        tr.price = getDouble(item[sizeIndex + 1]);
                        tr.brokerage = Math.abs(getDouble(item[sizeIndex + 3]));
                        tr.serviceTax = Math.abs(getDouble(item[sizeIndex + 4]));
                        tr.stt = Math.abs(getDouble(item[sizeIndex + 5]));
                        tr.otherLevies = Math.abs(getDouble(item[sizeIndex + 6]));
                        writeToFile(outputFileName, tr);
                    }
                }
                //}
            }
        }
    }

    public static void importZerodha(String[] lines, String outputFileName, String inputFileName) throws IOException {
        try {
            int iClientName = 7;
            int iTradeNumber = 9;
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
            reference = "NA";
            contractDate = fileName.split("_")[0];
            contractDate=lines[36];
            clientName = "Pankaj Kumar Sharma";
            //get total traded value
            Double tradedValue = 0D;
            Double otherLevies = 0D;
            Double brokerage = 0D;
            Double servicetax=0D;
            HashMap<String, Integer> totalOrders = new HashMap<>();
            int startIndex = 0;
            int endIndex = 0;
                int j=0;
            for (String s : lines) {
                if (s.contains("Stamp")) {
                    if(s.matches("(.)*(\\d)(.)*")){
                    otherLevies = otherLevies + getDouble(getValue(s));
                    }else{
                        logger.log(Level.SEVERE,"No Stamp Duty Found. Possible closeout for file {0}",new Object[]{inputFileName});
                    }
                }
                if (s.contains("Sebi Charges")) {
                    String[] substr = s.split(" ");
                    int len=substr.length;
                    if(substr.length>=4){
                    otherLevies = otherLevies + getDouble(getValue(substr[len-1].replaceAll(",", "")));
                    if(len-3==2){
                    otherLevies = otherLevies + getDouble(getValue(substr[len-3].replaceAll(",", "")));
                    }
                    }else{
                        s=lines[j+5];
                        substr = s.split(" ");
                    if(substr.length>=4){
                    otherLevies = otherLevies + getDouble(getValue(substr[1].replaceAll(",", "")));
                    otherLevies = otherLevies + getDouble(getValue(substr[3].replaceAll(",", "")));
                        
                    }else{
                        logger.log(Level.SEVERE,"Sebi Charges not updated correctly for file {0}",new Object[]{inputFileName});
                    }
                    }
                }

                if (s.contains("Total Brokerage") && !s.contains("Gross") ) {
                    String[] substr = s.split(" ");
                    if(substr.length>=4){
                    brokerage = getDouble(getValue(substr[3].replaceAll(",", "")));
                    }else{
                       s=lines[j+2];
                       substr=s.split(" ");
                       if(substr.length>=6){
                       brokerage = getDouble(getValue(substr[5].replaceAll(",", "")));
                       }else{
                           logger.log(Level.SEVERE,"No Brokerage Found. Possible Closeout for file {0}",new Object[]{inputFileName});
                       }
                       
                    }
                    }
                if (s.contains("Service Tax")&& !s.contains("Service Tax No")) {
                    String[] substr = s.split(" ");
                    if(substr.length==4){
                     servicetax= getDouble(getValue(substr[3].replaceAll(",", "")));
                    }
                }

                String[] item = s.split(" ");
                boolean error = false;
                if (isNumeric(item[0])) {
                    if (getDouble(item[0]) > 200000 ||((item.length>2) && item[1].trim().contains("00:00:00"))) {
                        if (item.length < 11) {
                            //broken format. print line and move on
                            logger.log(Level.SEVERE, "Error importing fileName:{0},Line:{1}", new Object[]{fileName, s});
                            error = true;
                        }
                        if (!error) {
                            int suborders = totalOrders.get(item[0]) != null ? totalOrders.get(item[0]) + 1 : 1;
                            totalOrders.put(item[0], suborders);
                            for (int counter = 0; counter < item.length; counter++) {
                                String str = item[counter];
                                String[] substr = str.split("(?<=\\D)(?=\\d\\.\\d\\d)|(?<=\\d\\.\\d\\d)(?=\\D)");
                                if (substr.length == 2) {
                                    if (isNumeric(substr[0]) && substr[1].matches("[a-zA-Z]+")) {
                                        tradedValue = tradedValue + Math.abs(getDouble(substr[0]));

                                    }
                                }
                            }
                        }
                    }
                }
                j++;
            }
            int orders = totalOrders.size();
            for (int i = 0; i < lines.length; i++) {
                Double itemValue = 0D;
                String[] item = lines[i].split(" ");
                boolean error = false;
                if (isNumeric(item[0])) {
                    if (getDouble(item[0]) > 200000||item[1].contains("00:00:00")) {
                        if (item.length < 11) {
                            //broken format. print line and move on
                            logger.log(Level.SEVERE, "Error importing fileName:{0},Line:{1}", new Object[]{fileName, lines[i]});
                            error = true;
                        }
                        if (!error) {
                            Trade tr = new Trade();
                            tr.fileName = fileName;
                            tr.clientName = clientName;
                            tr.tradeDate = contractDate.substring(6, 10) + contractDate.substring(3, 5) + contractDate.substring(0, 2);
                            tr.contractNoteReference = lines[35].trim();
                            tr.orderTime = item[iOrderTime];
                            int count = 0;
                            for (String s : item) {

                                if (s.contains(":")) {
                                    count = count + 1;
                                }
                                if (count == 1) {
                                    tr.executionTime = s;
                                    break;
                                }
                            }
                            //get side
                            String contractType="irregular";
                            int countertype=0;
                            for (String s : item) {          
                                String[] substr = s.split("(?<=\\D)(?=\\d\\.\\d\\d)|(?<=\\d\\.\\d\\d)(?=\\D)");
                                if(substr.length==2 && isInteger(substr[0]) && !isInteger(substr[1])){
                                    contractType="regular";
                                    break;
                                }
                                countertype++;
                                
                            }
                            
                            switch(contractType){
                            case "regular":
                            for (String s : item) {
                                String[] substr = s.split("(?<=\\D)(?=\\d\\.\\d\\d)|(?<=\\d\\.\\d\\d)(?=\\D)");
                            
                                if (substr.length == 2) {
                                    if (isNumeric(substr[0]) && !substr[0].contains("-") && substr[1].matches("[a-zA-Z]+")) {
                                        tr.side = "Sell";
                                        tr.symbol = substr[1];
                                        itemValue = Math.abs(getDouble(substr[0]));
                                        break;
                                    } else if (isNumeric(substr[0]) && substr[0].contains("-") && substr[1].matches("[a-zA-Z]+")) {
                                        tr.side = "Buy";
                                        tr.symbol = substr[1];
                                        itemValue = Math.abs(getDouble(substr[0]));
                                        break;
                                    }
                                }
                            }
                            break;
                                    
                                case "irregular":
                             int rowcounter=0;
                             for (String s : item) {
                             String[] substr = s.split("(?<=\\D)(?=\\d\\.\\d\\d)|(?<=\\d\\.\\d\\d)(?=\\D)");
                            
                                if (rowcounter>=2 && isInteger(substr[0])) {
                                    if (isNumeric(substr[0]) && !substr[0].contains("-")) {
                                        tr.side = "Sell";
                                        tr.symbol = item[6];
                                        itemValue = Math.abs(getDouble(substr[0]));
                                        tradedValue = tradedValue + Math.abs(getDouble(substr[0]));
                                        break;
                                    } else if (isNumeric(substr[0]) && substr[0].contains("-") ) {
                                        tr.side = "Buy";
                                        tr.symbol = item[6];
                                        itemValue = Math.abs(getDouble(substr[0]));
                                        tradedValue = tradedValue + Math.abs(getDouble(substr[0]));
                                        break;
                                    }
                                }
                                rowcounter++;
                            }
                             break;
                                default:
                                    break;
                                    
                            }

                            //concatenate symbol name
                            for (int counter = 0; counter < item.length; counter++) {
                                if (item[counter].startsWith("(")) {
                                    startIndex = counter;
                                }
                                if (item[counter].endsWith(")")) {
                                    endIndex = counter;
                                }
                            }
                            boolean option = false;
                            if (startIndex == endIndex && (item[startIndex].contains("PE")) || item[startIndex].contains("CE")) {
                                option = true;
                                for (int c = startIndex - 3; c <= endIndex; c++) {
                                    tr.symbol = tr.symbol + item[c].replace(",", "");
                                }
                            } else {

                                for (int c = startIndex - 1; c <= endIndex; c++) {
                                    tr.symbol = tr.symbol + item[c];
                                }
                            }
                            switch(contractType){
                                case "regular":
                            tr.tradeNumber = item[endIndex + 1];
                            switch (item.length) {
                                case 20:
                                    if (getDouble(item[0]) > 1000000) {
                                        tr.size = getInteger(item[endIndex + 4]);
                                        tr.price = getDouble(item[endIndex + 6]);
                                    } else {
                                        //handling closeout
                                        tr.size = getInteger(item[endIndex + 6]);
                                        tr.price = getDouble(item[endIndex + 8]);
                                    }
                                    break;

                                case 22:
                                    tr.size = getInteger(item[endIndex + 6]);
                                    tr.price = getDouble(item[endIndex + 8]);
                                default:
                                    if (tr.side.equals("Buy")) {
                                        tr.size = getInteger(item[endIndex + 4]);
                                        tr.price = getDouble(item[endIndex + 6]);
                                    } else if (tr.side.equals("Sell")) {
                                        tr.size = getInteger(item[endIndex + 4]);
                                        tr.price = getDouble(item[endIndex + 6]);
                                    }
                                    break;
                            }
                            break;
                            case "irregular":
                            tr.tradeNumber = item[4];
                            if(getInteger(item[3])>0){
                                tr.size = getInteger(item[3]);
                                tr.price = getDouble(item[endIndex+2]);                                
                            }else{
                                tr.size = getInteger(item[endIndex+2]);
                                tr.price=getDouble(item[endIndex+4]);                                
                            }
                            
                            break;
                                default:
                                    break;
                            }

                            tr.brokerage = brokerage / (totalOrders.size() * totalOrders.get(item[0]));
                            tr.serviceTax=servicetax* itemValue / tradedValue;
                            if (tr.side.equals("Buy") && !option) {
                                //tr.serviceTax = getDouble(item[startIndex - 3]);
                                tr.stt = 0D;
                            } else if (tr.side.equals("Sell") && !option) {
                                if (getDouble(item[0]) > 1000000) {
                                  //  tr.serviceTax = getDouble(item[startIndex - 6]);
                                    //tr.serviceTax = tr.serviceTax + 0.03 * tr.serviceTax;//edu tax and surcharge
                                    //tr.serviceTax = tr.serviceTax + tr.brokerage * 0.1236;
                                    tr.stt = getDouble(item[startIndex - 4]);
                                } else {
                                    //handling closeout
                                    //tr.serviceTax = getDouble(item[startIndex - 4]);
                                    //tr.serviceTax = tr.serviceTax + 0.03 * tr.serviceTax;//edu tax and surcharge
                                    //tr.serviceTax = tr.serviceTax + tr.brokerage * 0.1236;
                                    tr.stt = 0D;//No STT was charged by Zerodha in the contract note!!
                                }
                            } else if (tr.side.equals("Buy") && option) {
                                //tr.serviceTax = getDouble(item[startIndex - 5]);//st is available at line level on transaction costs
                                //tr.serviceTax = tr.serviceTax + 0.03 * tr.serviceTax;//edu tax and surcharge
                                //tr.serviceTax = tr.serviceTax + tr.brokerage * 0.1236;
                                tr.stt = 0D;
                            } else if (tr.side.equals("Sell") && option) {
                                //tr.serviceTax = getDouble(item[startIndex - 8]);
                                //tr.serviceTax = tr.serviceTax + 0.03 * tr.serviceTax;//edu tax and surcharge
                                //tr.serviceTax = tr.serviceTax + tr.brokerage * 0.1236;
                                tr.stt = getDouble(item[startIndex - 6]);
                            }
                            tr.otherLevies = otherLevies * itemValue / tradedValue;
                            writeToFile(outputFileName, tr);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE,null,e);
            logger.log(Level.SEVERE, "Error:{0},Symbol:{1}", new Object[]{e,inputFileName});
        }
    }

        public static boolean isInteger(String str) {
        if (str == null||str.isEmpty()) {
            return false;
        }
        str = str.trim();
        int length = str.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if ((c <= '/' || c >= ':') && c!='.') {
                return false;
            }
        }
        return true;
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

        Double netamount = tr.side.equals("Buy") ? -tr.size * tr.price - tr.brokerage - tr.serviceTax - tr.stt - tr.otherLevies : tr.size * tr.price - tr.brokerage - tr.serviceTax - tr.stt - tr.otherLevies;
        bufferWritter.write(tr.fileName + delimiter + tr.clientName + delimiter + tr.contractNoteReference + delimiter + tr.tradeDate + delimiter + tr.tradeNumber + delimiter + tr.orderTime + delimiter + tr.executionTime + delimiter
                + tr.side + delimiter + tr.symbol + delimiter + tr.size + delimiter + tr.price + delimiter + tr.brokerage + delimiter
                + tr.serviceTax + delimiter + tr.stt + delimiter + tr.otherLevies + delimiter + netamount + newline);


        bufferWritter.close();
    }

    private static String getValue(String string) {
        Pattern pattern = Pattern.compile("(\\d+\\.\\d+)|(\\d+)");
        Matcher matcher = pattern.matcher(string);
        matcher.find();
        return matcher.group();
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
        if (!isNumeric(str)) {
            return 0D;
        } else {
            Scanner scanner = new Scanner(str);
            if (scanner.hasNextDouble()) {
                return scanner.nextDouble();
            } else {
                return 0D;
            }
        }
    }
}
