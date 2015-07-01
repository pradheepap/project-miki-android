package wwckl.projectmiki.models;

import android.util.Log;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by cheeyim on 2/2/2015.
 */
public class Bill {
    final int fMaxGSTpercent = 21;
    final int fMinGSTpercent = 5;
    final int fMaxSVCpercent = 21;
    final int fMinSVCpercent = 5;

    private BigDecimal mTotal = new BigDecimal(0.00);
    private BigDecimal mSubTotal = new BigDecimal(0.00);
    private BigDecimal mGST = new BigDecimal(0.00);
    private int mGSTpercent = 0;
    private BigDecimal mSVC = new BigDecimal(0.00);
    private int mSVCpercent = 0;
    private BigDecimal mAdjust = new BigDecimal(0.00);
    private int mNoOfPplSharing = 0;
    private int mBillSplitNum = 0;
    private List<Item> mListOfAllItems = new ArrayList<>();
    private List<BillSplit> mListOfGuests = new ArrayList<>();
    private Boolean mUseSubtotals = false; // whether to include subtotals in calculations.

    // hash words 'library'
    private static final Set<String> fTOTAL_WORDS = new LinkedHashSet<>();
    private static final Set<String> fSUBTOTAL_WORDS = new LinkedHashSet<>();
    private static final Set<String> fGST_WORDS = new LinkedHashSet<>();
    private static final Set<String> fSVC_WORDS = new LinkedHashSet<>();
    private static final Set<String> fCASH_WORDS = new LinkedHashSet<>();
    private static final Set<String> fWHITELIST_WORDS = new LinkedHashSet<>();
    private static final Set<String> fADJUST_WORDS = new LinkedHashSet<>();
    // initialise word tokens
    static {
        fTOTAL_WORDS.add("TOTAL");
        fTOTAL_WORDS.add("TOT");
        fTOTAL_WORDS.add("TTL");
        fTOTAL_WORDS.add("MASTER");
        fTOTAL_WORDS.add("VISA");
        fTOTAL_WORDS.add("AMEX");
        fTOTAL_WORDS.add("CASH");
        fTOTAL_WORDS.add("PAY");
        fTOTAL_WORDS.add("AMOUNT");
        fTOTAL_WORDS.add("BASE");
        fSUBTOTAL_WORDS.add("SUBTOT");
        //fSUBTOTAL_WORDS.add("SUBTOTAL");
        fSUBTOTAL_WORDS.add("SUB-TOT");
        fSUBTOTAL_WORDS.add("SUB\u2014TOT"); // \u2014 is unicode representation of a long dash
        //fSUBTOTAL_WORDS.add("SUB-TOTAL");
        fSUBTOTAL_WORDS.add("SUBTTL");
        fSUBTOTAL_WORDS.add("STTL");
        fSUBTOTAL_WORDS.add("TOTAL-EXCL-GST");
        fGST_WORDS.add("GST");
        fGST_WORDS.add("TAX");
        fGST_WORDS.add("G.S.T.");
        fSVC_WORDS.add("SVC");
        //fSVC_WORDS.add("SVC CHG");
        //fSVC_WORDS.add("SVC CHARGE");
        fSVC_WORDS.add("S.C");
        fSVC_WORDS.add("SVR CHRG");
        fSVC_WORDS.add("SERV CHARG");
        fSVC_WORDS.add("SERVICE");
        //fSVC_WORDS.add("SERVICE CHARGE");
        fCASH_WORDS.add("CASH");
        fWHITELIST_WORDS.add("CHANGE");
        fWHITELIST_WORDS.add("BALANCE");
        fWHITELIST_WORDS.add("QTY");
        fWHITELIST_WORDS.add("SAVINGS");
        //fWHITELIST_WORDS.add("TOTAL SAVINGS");
        fWHITELIST_WORDS.add("DISCOUNT");
        fADJUST_WORDS.add("ROUNDING");
        fADJUST_WORDS.add("ROUNDOFF");
        fADJUST_WORDS.add("ADJUST");
        //fWHITELIST_WORDS.add("ADJUSTMENT");
        //fWHITELIST_WORDS.add("TOTAL ADJUSTMENT");
    }

    public BigDecimal getTotal() {
        return this.mTotal;
    }

    public void setTotal(BigDecimal total) {
        this.mTotal = total;
    }

    public BigDecimal getSubTotal() {
        return this.mSubTotal;
    }

    public void setSubTotal(BigDecimal subTotal) {
        this.mSubTotal = subTotal;
    }

    public BigDecimal getGst() {
        return this.mGST;
    }

    public void setGst(BigDecimal gst){
        this.mGST = gst;
    }

    public int getGstPercent(){
        return this.mGSTpercent;
    }

    public void setGstPercent(int gstPercent){
        this.mGSTpercent = gstPercent;
    }

    public BigDecimal getSvc() {
        return this.mSVC;
    }

    public void setSvc(BigDecimal svc){
        this.mSVC = svc;
    }

    public int getSvcPercent(){
        return this.mSVCpercent;
    }

    public void setSvcPercent(int svcPercent){
        this.mSVCpercent = svcPercent;
    }

    public BigDecimal getAdjust() {
        return this.mAdjust;
    }

    public void setAdjust(BigDecimal adjust){
        this.mAdjust = adjust;
    }

    public int getNoOfPplSharing() {
        return this.mNoOfPplSharing;
    }

    public void setNoOfPplSharing(int noOfPplSharing) {
        this.mNoOfPplSharing = noOfPplSharing;
    }

    public int getBillSplitNum() {
        return this.mBillSplitNum;
    }

    public void setBillSplitNum(int billSplitNum) {
        this.mBillSplitNum = billSplitNum;
    }

    public List<Item> getListOfAllItems() {
        return this.mListOfAllItems;
    }

    public List<BillSplit> getListOfGuests() { return this.mListOfGuests; }

    // New Bill() initialisation method
    // parse receipt text and populate receipt values
    public Bill (String receiptText) {
        // Prep receipt
        receiptText = cleanupOCRerrors(receiptText);
        receiptText = receiptText.toUpperCase();
        String[] lines = receiptText.split("[\n]+");

        // Parse each line of receipt
        for (String line : lines){
            parseLine(line);
        }

        // Finished parsing, make sure bill is balanced
        balanceBill();
    }

    // Check for common OCR parsing errors
    private String cleanupOCRerrors(String receiptText){
        // we don't need question marks, assume 7
        receiptText = receiptText.replaceAll("\\?", "7");
        // common ocr error, where picture has noise at edges
        receiptText = receiptText.replaceAll("(?m)^[^A-Za-z0-9]+", "");
        receiptText = receiptText.replaceAll("(?m)[^A-Za-z0-9]+$", "");
        // common ocr errors:
        receiptText = receiptText.replaceAll("Tota1", "Total");
        receiptText = receiptText.replaceAll("T0ta1", "Total");
        receiptText = receiptText.replaceAll("tota1", "total");
        receiptText = receiptText.replaceAll("10ta1", "total");
        receiptText = receiptText.replaceAll("Totai", "Total");
        receiptText = receiptText.replaceAll("totai", "total");
        receiptText = receiptText.replaceAll("TDTAL", "TOTAL");
        receiptText = receiptText.replaceAll("T0TAL", "TOTAL");
        receiptText = receiptText.replaceAll("(?m)^EST ", "GST ");
        receiptText = receiptText.replaceAll(" EST ", " GST ");
        receiptText = receiptText.replaceAll("MQSTER", "MASTER");
        receiptText = receiptText.replaceAll("MA$TER", "MASTER");

        return receiptText;
    }

    // ********************* PARSING OF RECEIPT ***************************

    // parse receipt line to match ether item amount, total, subtotal, gst or svc
    private void parseLine(String line){
        BigDecimal bdAmount;

        // Get amount from line
        bdAmount = getAmount(line);
        if (bdAmount.compareTo(BigDecimal.ZERO) == 0) {
            // line does not contain amount, skip
            Log.d("parseLine", line);
            return;
        }
        Log.d("amount", bdAmount.toString());

        // check for different tokens
        if(containsToken(fSUBTOTAL_WORDS, line)){
            setSubTotal(bdAmount);
            Log.d("SUBTOTAL", line);
        }
        else if(containsToken(fWHITELIST_WORDS, line)){
            Log.d("WHITELIST", line);
        }
        else if(containsToken(fTOTAL_WORDS, line)){
            if(getTotal().compareTo(BigDecimal.ZERO) == 0) {
                // Cash line may have an associated change line.
                // Skip if we already have our total.
                if (containsToken(fCASH_WORDS, line))
                    return;
            }
            this.setTotal(bdAmount);
            Log.d("TOTAL", line);
        }
        else if(containsToken(fGST_WORDS, line)){
            this.setGst(bdAmount);
            this.setGstPercent(getPercent(line));
            Log.d("GST", line);
        }
        else if(containsToken(fSVC_WORDS, line)){
            this.setSvc(bdAmount);
            this.setSvcPercent(getPercent(line));
            Log.d("SVC", line);
        }
        else if(containsToken(fADJUST_WORDS, line)){
            setAdjust(bdAmount);
            Log.d("ADJUST", line);
        }
        else if(isZero(sumOfTotals()) && isZero(mTotal))
        { // ITEM
            Log.d("ITEM", line);
            int qty = getQuantity(line);
            String desc = getDescription(line);
            BigDecimal amount = bdAmount;

            // Divide amount by the quantity.
            if(qty > 1) {
                BigDecimal bdQuantity = new BigDecimal(qty);
                amount = bdAmount.divide(bdQuantity, 2, RoundingMode.HALF_UP);
                Log.d("new amt", amount.toString());
            }

            for (int i = 0; i < qty; i++) {
                mListOfAllItems.add(new Item(desc, amount));
            }
        }
    }

    // if there is no amount found at end of line, returns 0 instead of 0.00
    private BigDecimal getAmount(String line){
        BigDecimal bdAmount = new BigDecimal(0);
        String strAmount;

        // prep string, remove common ocr mistakes of various symbols in amount
        strAmount = line.replaceAll("([0-9][ ]?)[^0-9 ]([ ]?[0-9])$", "$1.$2");
        //Log.d("strAmount 0", strAmount);

        // receipts tend to have bigger font size of total amount
        // which may lend OCR to add spaces between the numbers and period.
        // otherwise, assume number belongs to item description.
        if(strAmount.contains(" .") || strAmount.contains(". ")) {
            strAmount = strAmount.replaceAll("O", "0");
            strAmount = strAmount.replaceAll("([0-9 ])[.]([0-9 ])", "$1.$2");
            strAmount = strAmount.replaceAll("([0-9.]) ([0-9.])", "$1$2");
            strAmount = strAmount.replaceAll("([0-9.]) ([0-9.])", "$1$2");
            Log.d("strAmount 1", strAmount);
        }

        // Match end of line amount
        String strPattern = "[-]?[0-9]+[.][0-9][0-9]";
        Pattern pattern = Pattern.compile( "(" + strPattern + ")$" );
        Matcher matcher = pattern.matcher(strAmount);

        if(!matcher.find())
            return bdAmount;
        strAmount = matcher.group(1);
        Log.d("strAmount 2", strAmount);

        // Check if we found amount match.
        if(!strAmount.matches(strPattern))
            return bdAmount;

        bdAmount = new BigDecimal(strAmount);
        return bdAmount;
    }

    // returns whether or not the line contains any of the keywords in the list
    private Boolean containsToken(Set<String> linkedHashSet, String line){
        Iterator it = linkedHashSet.iterator();
        Boolean hasToken = false;

        while(it.hasNext() && !hasToken){
            if(line.contains(it.next().toString()))
                hasToken = true;
        }

        return hasToken;
    }

    private int getQuantity(String line){
        int qty = 1;
        String strQuantity;
        Pattern pattern = Pattern.compile("^([0-9]+)[ ]");
        Matcher matcher = pattern.matcher(line);

        if(!matcher.find())
            return qty;

        strQuantity = matcher.group(1);
        // more likely not quantity
        if(strQuantity.length() > 2)
            return qty;
        qty  = Integer.parseInt(strQuantity);

        Log.d("qty", String.valueOf(qty));
        return qty;
    }

    private String getDescription(String line){
        String desc = "Item";

        line = line.replaceFirst("^[0-9][0-9]?[ ]", "");
        line = line.replaceFirst("[0-9., -$]+$", "");

        if(!line.isEmpty())
            desc = line;

        Log.d("desc", desc);
        return desc;
    }

    private int getPercent(String line){
        int percentage = 0;

        if(!line.contains("%"))
            return percentage;

        String tokens[] = line.split(" ");

        for (String token : tokens) {
            if(token.contains("%")){
                token = token.replaceAll("[.]00", "");
                token = token.replaceAll("[%().]", "");
                token = token.replaceAll("O","0");
                token = token.replaceAll("l","1");
                if(token.matches("[0-9]+"))
                    percentage = Integer.parseInt(token);
                break;
            }
        }

        Log.d("percent", String.valueOf(percentage));
        return percentage;
    }

    // ********************* END PARSING OF RECEIPT ************************

    // ********************** BALANCING THE BILL ***************************
    // Rules :
    // IF GST and SVC is included in item price. mUseSubtotals will be true.
    // *********************************************************************

    // To be done at initiazation after parsing of receipt
    private void balanceBill(){
        int comparison;
        BigDecimal sumOfItems = sumOfItems();
        mUseSubtotals = false;

        // initial Total check
        if (isZero(mTotal))
            mTotal = sumOfItems;
        else if (sumOfItems.compareTo(mTotal) < 0) {
            // set flag whether to calculate items using subtotals or total
            mUseSubtotals = true;
            // initial SubTotal check
            if (isZero(mSubTotal)) {
                mSubTotal = sumOfItems;
            }

            // GST
            int percent = 0;
            if (!isZero(mGST))
                percent = calculatePercent(mGST, mSubTotal);
            else if (mGSTpercent != 0)
                percent = mGSTpercent;

            if ((percent >= fMinGSTpercent) && (percent <= fMaxGSTpercent)) {
                if ((mGSTpercent < (percent-1)) || (mGSTpercent > (percent+1))) // allow for margin of error from rounding
                    mGSTpercent = percent;
                else if (isZero(mGST)) {
                    mGST = mTotal.multiply(BigDecimal.valueOf(percent));
                    mGST = mGST.divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_EVEN);
                }
            }
            else if (percent != 0) {
                // Could not resolve error in one of the values, clear
                mGST = BigDecimal.ZERO;
                mGSTpercent = 0;
            }

            // SVC
            percent = 0;
            if (!isZero(mSVC))
                percent = calculatePercent(mSVC, mSubTotal);
            else if (mSVCpercent != 0)
                percent = mSVCpercent;

            if ((percent >= fMinSVCpercent) && (percent <= fMaxSVCpercent)) {
                if ((mSVCpercent < (percent-1)) || (mSVCpercent > (percent+1))) // allow for margin of error
                    mSVCpercent = percent;
                else if (isZero(mSVC)) {
                    mSVC = mTotal.multiply(BigDecimal.valueOf(percent));
                    mSVC = mSVC.divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_EVEN);
                }
            }
            else if (percent != 0) {
                // Could not resolve error in one of the values, clear
                mSVC = BigDecimal.ZERO;
                mSVCpercent = 0;
            }

            // Sub Total
            BigDecimal sumOfTotals = sumOfTotals();
            BigDecimal amount;
            comparison = sumOfTotals.compareTo(mTotal);
            // SubTotal + SVC + GST < Total assume missing a field
            if (comparison < 0) {
                comparison = mSubTotal.compareTo(mTotal);
                switch (comparison) {
                    case 1: // SubTotal is bigger, assume error in sub total.
                    case 0: // GST or SVC is already in item price
                        mUseSubtotals = false;
                        break;
                    case -1: // SubTotal < Total
                        amount = mTotal.subtract(sumOfTotals);
                        percent = calculatePercent(amount, mTotal);

                        // Maximum of both GST and SVC percentage, assume error
                        if(percent > (fMaxGSTpercent + fMaxSVCpercent))
                            break;

                        if (isZero(mSVC)) {
                            Log.d("infer SVC", amount.toString());
                            mSVC = amount;
                            mSVCpercent = percent;
                        } else if (isZero(mGST)) {
                            Log.d("infer GST", amount.toString());
                            mGST = amount;
                            mGSTpercent = percent;
                        }// otherwise, assume we are missing an item.
                        break;
                    default: // other unknown error
                        break;
                }
            } else if (comparison > 0) // assume GST and SVC is included in item price or other error
                mUseSubtotals = false;
        }

        // Total or Sub Total equals to Sum of all items
        BigDecimal total;
        if(mUseSubtotals)
            total = mSubTotal;
        else
            total = mTotal;

        comparison = total.compareTo(sumOfItems);
        if (comparison != 0) {
            // check common error where gst or svc become itemized.
            // because of font size or other formating
            int lastItemIndex = mListOfAllItems.size()-1;
            BigDecimal itemPrice = total.subtract(sumOfItems);
            BigDecimal lastItemPrice;

            // check that listOfAllItems is not empty
            if(lastItemIndex < 0)
                lastItemPrice = BigDecimal.ZERO;
            else
                lastItemPrice = mListOfAllItems.get(lastItemIndex).getPrice();

            // if last item is the same value as what we subtract, remove item
            if( isZero(itemPrice.add(lastItemPrice)) )
                mListOfAllItems.remove(lastItemIndex);
            else
                getListOfAllItems().add(new Item("UNKNOWN ITEM", itemPrice));
        }
        Log.d("UseSubtotals", mUseSubtotals.toString());
    }

    // calculates the percentage of fracAmount/total
    private int calculatePercent(BigDecimal fracAmount, BigDecimal total){
        BigDecimal percent;
        percent = fracAmount.multiply(BigDecimal.valueOf(100));
        percent = percent.divide(total, 0, BigDecimal.ROUND_HALF_EVEN);
        return percent.intValue();
    }

    // Checks that the bill is balanced with 2 rules:
    // Rule 1: total sum of price from listOfAllItems equals to the SubTotal(if present, Total otherwise)
    // Rule 2: Sum of GST, SVC and SubTotal (if present) equals to the Total
    public Boolean isBillBalanced(){
        if (isZero(mTotal))
            return false;
        BigDecimal sumOfItems = sumOfItems();

        if (mUseSubtotals){
            if ( (mSubTotal.compareTo(sumOfItems) != 0))
                return false;

            if(mTotal.compareTo(sumOfTotals()) != 0)
                return false;
        }
        else {
            if (mTotal.compareTo(sumOfItems) != 0)
                return false;
        }

        return true;
    }

    // returns the sum of GST, SVC, SubTotal and adjust.
    private BigDecimal sumOfTotals(){
        BigDecimal total = mSubTotal;
        total = total.add(mGST);
        total = total.add(mSVC);
        total = total.add(mAdjust);
        return total;
    }

    // Returns the sum of all items
    private BigDecimal sumOfItems(){
        BigDecimal total = new BigDecimal(0.00);
        Iterator<Item> itemIterator = mListOfAllItems.iterator();

        while (itemIterator.hasNext()){
            total = total.add(itemIterator.next().getPrice());
        }
        Log.d("calculateTotal", total.toString());
        return total;
    }

    // ******************* END BALANCING THE BILL ***************************

    private Boolean isZero(BigDecimal bdAmount){
        if(bdAmount.compareTo(BigDecimal.ZERO) == 0)
            return  true;
        return false;
    }

    // ******************* BILL SPLITTER ACTIVITY OPERATIONS ****************

    public void selectItem(int itemIndex, Boolean checked){
        Item item = mListOfAllItems.get(itemIndex);

        if(checked)
            item.setGuestIndex(mBillSplitNum);
        else
            item.setGuestIndex(item.fNOT_SELECTED);
    }

    // return total of selected item's price by guest, no Split type calculations
    public BigDecimal getGuestTotal(int guestIndex) {
        BigDecimal total = new BigDecimal(0.00);

        for (int i = 0; i < mListOfAllItems.size(); i++) {
            if(mListOfAllItems.get(i).getGuestIndex() == guestIndex) {
                BigDecimal itemPrice = mListOfAllItems.get(i).getPrice();
                total = total.add(itemPrice);
            }
        }
        if (mUseSubtotals) {
            // Add GST
            if (mGSTpercent > 0) {
                BigDecimal addPrice = total.multiply(BigDecimal.valueOf(mGSTpercent));
                addPrice = addPrice.divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_EVEN);
                total = total.add(addPrice);
                Log.d("GST of total", addPrice.toString());
            }
            // Add SVC
            if (mSVCpercent > 0) {
                BigDecimal addPrice = total.multiply(BigDecimal.valueOf(mSVCpercent));
                addPrice = addPrice.divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_EVEN);
                total = total.add(addPrice);
                Log.d("SVC of item", addPrice.toString());
            }
        }
        return total;
    }

    // User clicked next or done, create new BillSplit ListItem
    public void splitTheBill (BillSplit.BillSplitType splitType) {
        BillSplit billSplit = new BillSplit(splitType);
    }

    // User clicked prev, need to retrieve previous items and
    // delete last BillSplit item.
    public void backTrack () {}
}
