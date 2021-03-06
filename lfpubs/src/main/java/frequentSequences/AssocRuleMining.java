package frequentSequences;
/* -------------------------------------------------------------------------- */
/*                                                                            */
/*                      ASSOCIATION RULE DATA MINING                          */
/*                                                                            */
/*                            Frans Coenen                                    */
/*                                                                            */
/*                        Wednesday 9 January 2003                            */
/*   (revised 21/1/2003, 14/2/2003, 2/5/2003, 2/7/2003, 3/2/2004, 8/5/2004,   */
/*     1/2/2005, 3/2/2005, 14/2/06, 14/3/06, 18/6/06, 1/7/2006, 1/6/2009)     */
/*                                                                            */
/*                    Department of Computer Science                          */
/*                     The University of Liverpool                            */
/*                                                                            */
/* -------------------------------------------------------------------------- */

//package lucsKDD_ARM;

/* To compile: javaARMpackc.exe AssocRuleMining.java */

// Java packages
import java.io.*;
import java.util.*;

// Java GUI packages
//import javax.swing.*;

/** Set of utillities to support various Association Rule Mining (ARM)
algorithms included in the LUCS-KDD suite of ARM programs.
@author Frans Coenen
@version 1 June 2008 */

public class AssocRuleMining {

    /* ------ FIELDS ------ */

    // Data structures

    /** The reference to start of the rule list. */
    protected RuleNode startRulelist = null;
    /** 2-D aray to hold input data from data file. Note that within the data
    array records are numbered from zero, thus record one has index 0 etc.
    <P>First index is row (record or TID) number starting from 0, and second
    is attribute (column) number starting from zero. */
    protected short[][] dataArray = null;
    /** 2-D array used to renumber columns for input data in terms of
    frequency of single attributes (reordering will enhance performance
    for some ARM algorithms). */
    protected int[][] conversionArray   = null;
    /** 1-D array used to reconvert input data column numbers to their
    original numbering where the input data has been ordered to enhance
    computational efficiency. */
    protected short[] reconversionArray = null;
    /** 1-D array to hold output schema. */
    private String[] outputSchema = null;

    // Constants

    /** Minimum support value */
    protected static final double MIN_SUPPORT = 0.0;
    /** Maximum support value */
    protected static final double MAX_SUPPORT = 100.0;
    /** Maximum confidence value */
    protected static final double MIN_CONFIDENCE = 0.0;
    /** Maximum confidence value */
    protected static final double MAX_CONFIDENCE = 100.0;

    // Command line arguments with default values and associated fields.

    /** Command line argument for data file name. */
    protected String  fileName   = null;
    /** File name for testset. */
    protected String testSetFileName = null;
    /** Command line argument for number of columns (attributes) in input
    data. */
    protected int     numCols    = 0;
    /** Command line argument for number of rows in input data. */
    protected int     numRows    = 0;
    /** Command line argument for % support (default = 20%). */
    protected double  support    = 20.0;
    /** Minimum support value in terms of number of rows. <P>Set when input
    data is read and the number of records is known, reset if input data is
    resized so that only N percent is used. */
    protected double  minSupport = 0;
    /** Command line argument for % confidence (default = 80%). */
    protected double  confidence = 80.0;
    /** The number of one itemsets (singletons). */
    protected int numOneItemSets = 0;
    /** Number of classes in input data set (input by the user). */
    protected int numClasses = 0;
    /** Number of rows in output schema. */
    private int numRowsInOutputSchema = 0;

    // Flags

    /** Error flag used when checking command line arguments (default =
    <TT>true</TT>). */
    protected boolean errorFlag  = true;
    /** Input format OK flag( default = <TT>true</TT>). */
    protected boolean inputFormatOkFlag = true;
    /** Flag to indicate whether system has data or not. */
    private boolean haveDataFlag = false;
    /** Flag to indicate whether input data has been sorted or not. */
    protected boolean isOrderedFlag = false;
    /** Flag to indicate whether input data has been sorted and pruned or
    not. */
    protected boolean isPrunedFlag = false;
    /** Flag to indicate whether output schema is available or not. */
    protected boolean hasOutputSchemaFlag = false;
    /** Support confidence framework flag. */
    protected boolean supConfFworkFlag = false;
    /** Support lift framework flag. */
    protected boolean supLiftFworkFlag = false;

    // Other fields

    /** The input stream. */
    protected BufferedReader fileInput;
    /** The file path */
    protected File filePath = null;

    /* ------ CONSTRUCTORS ------ */

    /** Constructor with command line arguments to be process.
    @param args the command line arguments (array of String instances).  */

    public AssocRuleMining(String[] args) {

	// Process command line arguments

	for(int index=0;index<args.length;index++) idArgument(args[index]);

	// If command line arguments read successfully (errorFlag set to
        // "true") check validity of arguments

	if (errorFlag) CheckInputArguments();
	else outputMenu();
        }

    /** One argument constructior with argument from existing instance of 
    class AssocRuleMining.
    @param aemInstance the given instance of the <TT>AssocRuleMining</TT>
    class. */
    
    public AssocRuleMining(AssocRuleMining armInstance) {
        outputSchema      = armInstance.outputSchema;
        }

    /** Default constructor used by: (1) BruteForce, (2) Total Support
    Tree, (3) ClassAppMapGUI and (4) AprioriTgui classes and others.   */

    public AssocRuleMining() {
        }

    /* ------ METHODS ------ */

    /* ---------------------------------------------------------------- */
    /*                                                                  */
    /*                        COMMAND LINE ARGUMENTS                    */
    /*                                                                  */
    /* ---------------------------------------------------------------- */

    /* IDENTIFY ARGUMENT */
    /** Identifies nature of individual command line agruments:
    -C = confidence, -F = file name, -S = support. */

    protected void idArgument(String argument) {
	if (argument.length()<3) {
	    System.out.println("ERROR: Command line argument '" + argument +
	    				"' too short.");
            errorFlag = false;
            }
        else if (argument.charAt(0) == '-') {
	    char flag = argument.charAt(1);
	    argument = argument.substring(2,argument.length());
	    switch (flag) {
		case 'C':
	            confidence = Double.parseDouble(argument);
		    break;
	        case 'F':
	    	    fileName = argument;
		    break;
		case 'N':
		    numClasses =  Integer.parseInt(argument);
		    break;
	        case 'S':
	            support = Double.parseDouble(argument);
		    break;
	        default:
	            System.out.println("INPUT ERROR: Unrecognise command " +
		    		"line  argument: '" + flag + argument + "'.");
		    errorFlag = false;
	        }
            }
        else {
	    System.out.println("INPUT ERROR: All command line arguments " +
    				"must commence with a '-' character ('" +
							argument + "')");
            errorFlag = false;
            }
	}

    /* CHECK INPUT ARGUMENTS */
    /** Invokes methods to check values associate with command line
    arguments */

    protected void CheckInputArguments() {

	// Check support and confidence input
	checkSupportAndConfidence();

	// Check file name
	checkFileName();

	// Return
	if (errorFlag) outputSettings();
	else outputMenu();
	}

    /* CHECK SUPPORT AND CONFIDANCE */
    /** Checks support and confidence input % values, if either is out of
    bounds then <TT>errorFlag</TT> set to <TT>false</TT>. */

    protected void checkSupportAndConfidence() {

	// Check Support
	if ((support < MIN_SUPPORT) || (support > MAX_SUPPORT)) {
	    System.out.println("INPUT ERROR: Support must be specified " +
		 			"as a percentage (" + MIN_SUPPORT +
		 				" - " + MAX_SUPPORT + ")");
	    errorFlag = false;
	    }

	// Check confidence
	if ((confidence < MIN_CONFIDENCE) || (confidence > MAX_CONFIDENCE)) {
	    System.out.println("INPUT ERROR: Confidence must be " +
		               "specified as a percentage (" + MIN_CONFIDENCE +
					" - " + MAX_CONFIDENCE + ")");
	    errorFlag = false;
	    }
	}

    /* CHECK FILE NAME */
    /** Checks if data file name provided, if not <TT>errorFlag</TT> set
    to <TT>false</TT>. */

    protected void checkFileName() {
	if (fileName == null) {
	    System.out.println("INPUT ERROR: Must specify file name (-F)");
            errorFlag = false;
	    }
	}

    /* ---------------------------------------------------------------- */
    /*                                                                  */
    /*                     READ INPUT DATA FROM FILE                    */
    /*                                                                  */
    /* ---------------------------------------------------------------- */

    /* INPUT DATA SET */

    /** Commences process of getting input data (GUI version also exists). */

    public void inputDataSet() {
        // Read the file
	readFile();

	// Check ordering (only if input format is OK)
	if (inputFormatOkFlag) {
	    if (checkOrdering()) {
                System.out.println("Number of records = " + numRows);
		countNumCols();
		System.out.println("Number of columns = " + numCols);
		minSupport = (numRows * support)/100.0;
        	System.out.println("Min support       = " +
				twoDecPlaces(minSupport) + " (records)");
		}
	    else {
	        System.out.println("Error reading file: " + fileName + "\n");
	        closeFile();
	        System.exit(1);
		}
	    }
	}

    /* READ FILE */

    /** Reads input data from file specified in command line argument
    <TT>fileName</TT> (GUI version also exists). <P>Note that it is assumed
    that no empty records are included. Proceeds as follows:
    <OL>
    <LI>Gets number of rows (lines) in file, checking format of each line
    (space separated integers), if incorrectly formatted line found
    <TT>inputFormatOkFlag</TT> set to <TT>false</TT>.
    <LI>Dimensions input array.
    <LI>Reads data
    </OL> */

    protected void readFile() {
        try {
	    // Dimension data structure
	    inputFormatOkFlag=true;
	    numRows = getNumberOfLines(fileName);
	    if (inputFormatOkFlag) {
	        dataArray = new short[numRows][];
	        // Read file
		System.out.println("Reading input file: " + fileName);
	        readInputDataSet();
		}
	    else System.out.println("Error reading file: " + fileName + "\n");
	    }
	catch(IOException ioException) {
	    System.out.println("Error reading File");
	    closeFile();
	    System.exit(1);
	    }
	}

    /* GET NUMBER OF LINES */

    /** Gets number of lines/records in input file and checks format of each
    line.
    @param nameOfFile the filename of the file to be opened.
    @return the number of rows in the given file. */

    protected int getNumberOfLines(String nameOfFile) throws IOException {
        int counter = 0;

	// Open the file
	if (filePath==null) openFileName(nameOfFile);
	else openFilePath();

	// Loop through file incrementing counter
	// get first row.
	String line = fileInput.readLine();
	while (line != null) {
	    checkLine(counter+1,line);
	    StringTokenizer dataLine = new StringTokenizer(line);
            int numberOfTokens = dataLine.countTokens();
	    if (numberOfTokens == 0) break;
	    counter++;
            line = fileInput.readLine();
	    }

	// Close file and return
        closeFile();
	return(counter);
	}

    /* CHECK LINE */

    /** Check whether given line from input file is of appropriate format
    (space separated integers), if incorrectly formatted line found
    <TT>inputFormatOkFlag</TT> set to <TT>false</TT>.
    @param counter the line number in the input file.
    @param str the current line from the input file. */

    protected void checkLine(int counter, String str) {

        for (int index=0;index <str.length();index++) {
            if (!Character.isDigit(str.charAt(index)) &&
	    			!Character.isWhitespace(str.charAt(index))) {
		//JOptionPane.showMessageDialog(null,"FILE INPUT ERROR:\n" +
					//"charcater on line " + counter +
					//" is not a digit or white space");
		inputFormatOkFlag = false;
		haveDataFlag = false;
		break;
		}
	    }
	}

    /* READ INPUT DATA SET */
    /** Reads input data from file specified in command line argument. */

    public void readInputDataSet() throws IOException {
        readInputDataSet(fileName);
	}

    /* READ INPUT DATA SET */
    /** Reads input data from given file.
    @param fName the given file name.  */

    protected void readInputDataSet(String fName) throws IOException {
	int rowIndex=0;

	// Open the file
	if (filePath==null) openFileName(fName);
	else openFilePath();

	// Get first row.
	String line = fileInput.readLine();

	// Process rest of file
	while (line != null) {
	    // Process line
	    if (!processInputLine(line,rowIndex)) break;
	    // Increment first (row) index in 2-D data array
	    rowIndex++;
	    // get next line
            line = fileInput.readLine();
	    }

	// Close file
	closeFile();
	}

    /* READ INPUT DATA SEGMENT */
    /** Reads input data segment from a given file and places content into to
    the data array structure commencing at the given row index, continues until
    the end index is rerached.
    @param fName the given file name.
    @param startRowIndex the given row strat index.
    @param endRowIndex the given row end index. */

    protected void readInputDataSetSeg(String fName, int startRowIndex,
    					int endRowIndex) throws IOException {
	int rowIndex=startRowIndex;

	// Open the file
	if (filePath==null) openFileName(fName);
	else openFilePath();

	// get first row.
	String line = fileInput.readLine();
	for (int index=startRowIndex;index<endRowIndex;index++) {
	    // Process line
	    processInputLine(line,index);
	    // get next line
            line = fileInput.readLine();
	    }

	// Close file
	closeFile();
	}

    /* PROCESS INPUT LINE */

    /**	Processes a line from the input file and places it in the
    <TT>dataArray</TT> structure.
    @param line the line to be processed from the input file
    @param rowIndex the index to the current location in the
    <TT>dataArray</TT> structure.
    @return true if successfull, false if empty record. */

    private boolean processInputLine(String line, int rowIndex) {
        // If no line return false
	if (line==null) return(false);

	// Tokenise line
	StringTokenizer dataLine = new StringTokenizer(line);
        int numberOfTokens = dataLine.countTokens();

	// Empty line or end of file found, return false
	if (numberOfTokens == 0) return(false);

	// Convert input string to a sequence of short integers
	short[] code = binConversion(dataLine,numberOfTokens);

	// Dimension row in 2-D dataArray
	int codeLength = code.length;
	dataArray[rowIndex] = new short[codeLength];
	// Assign to elements in row
	for (int colIndex=0;colIndex<codeLength;colIndex++)
				dataArray[rowIndex][colIndex] = code[colIndex];

	// Return
	return(true);
	}

    /* CHECK DATASET ORDERING */
    /** Checks that data set is ordered correctly.
    @return true if appropriate ordering, false otherwise. */

    protected boolean checkOrdering() {
        boolean result = true;

	// Loop through input data
	for(int index=0;index<dataArray.length;index++) {
	    if (!checkLineOrdering(index+1,dataArray[index])) {
		haveDataFlag = false;
		result=false;
		}
	    }

	// Return
	return(result);
	}

    /* CHECK LINE ORDERING */
    /** Checks whether a given line in the input data is in numeric sequence.
    @param lineNum the line number.
    @param itemSet the item set represented by the line
    @return true if OK and false otherwise. */

    protected boolean checkLineOrdering(int lineNum, short[] itemSet) {
        for (int index=0;index<itemSet.length-1;index++) {
	    if (itemSet[index] >= itemSet[index+1]) {
		/*JOptionPane.showMessageDialog(null,"FILE FORMAT ERROR:\n" +
	       		"Attribute data in line " + lineNum +
			" not in numeric order");*/
		return(false);
		}
	    }

	// Default return
	return(true);
	}

    /* COUNT NUMBER OF COLUMNS */
    /** Counts number of columns represented by input data. */

    protected void countNumCols() {
        int maxAttribute=0;

	// Loop through data array
        for(int index=0;index<dataArray.length;index++) {
	    int lastIndex = dataArray[index].length-1;
	    if (dataArray[index][lastIndex] > maxAttribute)
	    		maxAttribute = dataArray[index][lastIndex];
	    }

	numCols        = maxAttribute;
	numOneItemSets = numCols; 	// default value only
	}

    /* OPEN FILE NAME */
    /** Opens input file using fileName (instance field).
    @param nameOfFile the filename of the file to be opened. */

    protected void openFileName(String nameOfFile) {
	try {
	    // Open file
	    FileReader file = new FileReader(nameOfFile);
	    fileInput = new BufferedReader(file);
	    }
	catch(IOException ioException) {
	    /*JOptionPane.showMessageDialog(this,"Error Opening File \"" +
	    		nameOfFile + "\"","Error: ",JOptionPane.ERROR_MESSAGE);*/
	    System.exit(1);
	    }
	}

    /* OPEN FILE PATH */
    /** Opens file using filePath (instance field). */

    protected void openFilePath() {
	try {
	    // Open file
	    FileReader file = new FileReader(filePath);
	    fileInput = new BufferedReader(file);
	    }
	catch(IOException ioException) {
	    /*JOptionPane.showMessageDialog(this,"Error Opening File",
			 "Error: ",JOptionPane.ERROR_MESSAGE);*/
	    System.exit(1);
	    }
	}

    /* CLOSE FILE */
    /** Close file fileName (instance field). */

    protected void closeFile() {
        if (fileInput != null) {
	    try {
	    	fileInput.close();
		}
	    catch (IOException ioException) {
		/*JOptionPane.showMessageDialog(this,"Error Closeing File",
			 "Error: ",JOptionPane.ERROR_MESSAGE);*/
	        System.exit(1);
		}
	    }
	}

    /* BINARY CONVERSION. */

    /** Produce an item set (array of elements) from input line.
    @param dataLine row from the input data file
    @param numberOfTokens number of items in row
    @return 1-D array of short integers representing attributes in input
    row */

    protected short[] binConversion(StringTokenizer dataLine,
    				int numberOfTokens) {
        short number;
	short[] newItemSet = null;

	// Load array

	for (int tokenCounter=0;tokenCounter < numberOfTokens;tokenCounter++) {
            number = new Short(dataLine.nextToken()).shortValue();
	    newItemSet = realloc1(newItemSet,number);
	    }

	// Return itemSet

	return(newItemSet);
	}

    /* ------------------------------------------------------ */
    /*                                                        */
    /*        OUTPUT SCHEMA METHODS (GUI VERSIONS)            */
    /*                                                        */
    /* ------------------------------------------------------ */

    /* Colection of methods concerned wioth the loading and checking of output
    schema file. The output schema is used in relation to outputting attributes
    as lables rather than attribute numbers. Currently (May 2006) this
    fuctionality is only available with respect to the various ARM and CARM GUI
    interfaces that have been developed by the LUCS team. */

    /* INPUT OUTPUT SCHEMA. */

    /** Commences process of reading indicated output schema file and
    stores to 1-D array.
    @param textArea the text area in the GUI used for output.
    @param fName the name of the input file to be read.  */

    public void inputOutputSchema(File fName) {
    	// Set filePath instance field
	filePath = fName;

        // Read the file
	try {
	    // Dimension data structure
	    numRowsInOutputSchema = getNumLinesInOutputSchema(null);
	    /*textArea.append("Number of lines (attributes) in output schema " +
                                    "file = " + numRowsInOutputSchema + "\n");*/
	    outputSchema = new String[numRowsInOutputSchema];
	    // Read file
	    readOutputSchema();
	    // Set have data flag to true
	    hasOutputSchemaFlag = true;
	    }
	catch(IOException ioException) {
	    /*JOptionPane.showMessageDialog(this,"Error reading File",
					"Error: ",JOptionPane.ERROR_MESSAGE);
	    textArea.append("Error reading File\n");*/
	    closeFile();
	    // Set have data flag to true
	    hasOutputSchemaFlag = false;
	    }
	}

    /* GET NUMBER OF LINES IN OUTPUT SCHEMA. */

    /** Gets the number of lines (attributes) in the output schema file. <P>
    Similar to getNmberOfLines method above but without line checking.
    @param fName the name of the output schema file.
    @return the number of lines (attributes) in the output schema. */

    protected int getNumLinesInOutputSchema(String fName) throws IOException {
	int rowIndex=0;

	// Open the file
	if (filePath==null) openFileName(fName);
	else openFilePath();

	// Get first row.
	String line = fileInput.readLine();

	// Process rest of file
	while (line != null) {
	    // Increment row index in output schema array
	    rowIndex++;
	    // get next line
            line = fileInput.readLine();
	    }

	// Close file and returm
	closeFile();
	return(rowIndex);
	}

    /* READ OUTPUT SCHEMA */
    /** Reads output schema from file. */

    public void readOutputSchema() throws IOException {
        readOutputSchema(fileName);
	}

    /* READ OUTPUT SCEMA */
    /** Reads outpur schema from given file.
    @param fName the given file name.  */

    protected void readOutputSchema(String fName) throws IOException {
	int rowIndex=0;

	// Open the file
	if (filePath==null) openFileName(fName);
	else openFilePath();

	// Get first row.
	String line = fileInput.readLine();

	// Process rest of file
	while (line != null) {
	    outputSchema[rowIndex] = line;
	    // Increment row index in output schema array
	    rowIndex++;
	    // get next line
            line = fileInput.readLine();
	    }

	// Close file
	closeFile();
	}

    /** Check if number of attributes in output schema are same as number of
    attributes in input file. <P>If either the output schema or the input
    file has nor been loaded then method will return false.
    @return true if number of attributes are the same and false otherwise. */

    public boolean checkSchemaVdata() {
        boolean schemaAndDataAttsSame = true;

        // Check schema
        if (outputSchema==null) {
            /*JOptionPane.showMessageDialog(null,"CHECK SCHEMA v DATA " +
                        "ATTRIBUTES ERROR:\nNo output schema file.\n");*/
            return(!schemaAndDataAttsSame);
            }

        // Check data array
        if (dataArray==null) {
            /*JOptionPane.showMessageDialog(null,"CHECK SCHEMA v DATA " +
                        "ATTRIBUTES ERROR:\nNo input data file.\n");*/
            return(!schemaAndDataAttsSame);
            }

        // Check lengths.
        if (outputSchema.length==numCols) return(schemaAndDataAttsSame);
        else  {
            /*JOptionPane.showMessageDialog(null,"CHECK SCHEMA v DATA " +
                        "ATTRIBUTES ERROR:\nNumber of attributes in schema " +
                        "file (" + outputSchema.length + ") not same as\n" +
                        "number of attributes in data file (" +
                        numCols + ")\n");*/
            return(!schemaAndDataAttsSame);
            }
        }

    
    /* ---------------------------------------------------------------- */
    /*                                                                  */
    /*        REORDER DATA SET ACCORDING TO ATTRIBUTE FREQUENCY         */
    /*                                                                  */
    /* ---------------------------------------------------------------- */

    /* REORDER INPUT DATA: */

    /** Reorders input data according to frequency of single attributes. <P>
    Example, given the data set:
    <PRE>
    1 2 5
    1 2 3
    2 4 5
    1 2 5
    2 3 5
    </PRE>
    This would produce a countArray (ignore index 0 because there is no
    attributr number 0):
    <PRE>
    +---+---+---+---+---+---+
    |   | 1 | 2 | 3 | 4 | 5 |
    +---+---+---+---+---+---+
    |   | 3 | 5 | 2 | 1 | 4 |
    +---+---+---+---+---+---+
    </PRE>
    Which sorts to:
    <PRE>
    +---+---+---+---+---+---+
    |   | 2 | 5 | 1 | 3 | 4 |
    +---+---+---+---+---+---+
    |   | 5 | 4 | 3 | 2 | 1 |
    +---+---+---+---+---+---+
    </PRE>
    Giving rise to the conversion Array of the form (no index 0):
    <PRE>
    +---+---+---+---+---+---+
    |   | 3 | 1 | 4 | 5 | 2 |
    +---+---+---+---+---+---+
    |   | 3 | 5 | 2 | 1 | 4 |
    +---+---+---+---+---+---+
    </PRE>
    Note that the first row gives the new attribute number (old attribute
    number is the index). The second row here are the counts used to identify
    the ordering but which now no longer play a role in the conversion
    exercise. Thus the new column (attriburte) number for column/attribute 1 is
    column 3 (i.e. the first vale at index 1). The reconversion array will be
    of the form (values are the indexes from the conversion array while indexes
    represent the first vlaue from the conversion array):
    <PRE>
    +---+---+---+---+---+---+
    |   | 2 | 5 | 1 | 3 | 4 |
    +---+---+---+---+---+---+
    </PRE>
    For example to convert the attribute number 3 back to its original number
    we look up the value at index 3.
    */

    public void idInputDataOrdering() {

	// Count singles and store in countArray;
        int[][] countArray = countSingles();

	// Bubble sort count array on support value (second index)
	orderCountArray(countArray);

        // Define conversion and reconversion arrays
	defConvertArrays(countArray);

	// Set sorted flag
	isOrderedFlag = true;
	}

    /* COUNT SINGLES */

    /** Counts number of occurrences of each single attribute in the
    input data.
    @return 2-D array where first row represents column numbers
    and second row represents support counts. */

    protected int[][] countSingles() {

	// Dimension and initialize count array

	int[][] countArray = new int[numCols+1][2];
	for (int index=0;index<countArray.length;index++) {
	    countArray[index][0] = index;
	    countArray[index][1] = 0;
	    }

	// Step through input data array counting singles and incrementing
	// appropriate element in the count array

	for(int rowIndex=0;rowIndex<dataArray.length;rowIndex++) {
	     if (dataArray[rowIndex] != null) {
		for (int colIndex=0;colIndex<dataArray[rowIndex].length;
					colIndex++)
		    	countArray[dataArray[rowIndex][colIndex]][1]++;
		}
	    }

	// Return

	return(countArray);
	}

    /* ORDER COUNT ARRAY */

    /** Bubble sorts count array produced by <TT>countSingles</TT> method
    so that array is ordered according to frequency of single items.
    @param countArray The 2-D array returned by the <TT>countSingles</TT>
    method. */

    private void orderCountArray(int[][] countArray) {
        int attribute, quantity;
        boolean isOrdered;
        int index;

        do {
	    isOrdered = true;
            index     = 1;
            while (index < (countArray.length-1)) {
                if (countArray[index][1] >= countArray[index+1][1]) index++;
	        else {
	            isOrdered=false;
                    // Swap
		    attribute              = countArray[index][0];
		    quantity               = countArray[index][1];
	            countArray[index][0]   = countArray[index+1][0];
	            countArray[index][1]   = countArray[index+1][1];
                    countArray[index+1][0] = attribute;
	            countArray[index+1][1] = quantity;
	            // Increment index
		    index++;
	            }
	  	}
	    } while (isOrdered==false);
    	}

    /* SORT FIRST N ELEMENTS IN COUNT ARRAY */

    /** Bubble sorts first N elements in count array produced by
    <TT>countSingles</TT> method so that array is ordered according to
    frequency of single items. <P> Used when ordering classification input
    data.
    @param countArray The 2-D array returned by the <TT>countSingles</TT>
    method.
    @param endIndex the index of the Nth element. */

    protected void orderFirstNofCountArray(int[][] countArray, int endIndex) {
        int attribute, quantity;
        boolean isOrdered;
        int index;

        do {
	    isOrdered = true;
            index     = 1;
            while (index < endIndex) {
                if (countArray[index][1] >= countArray[index+1][1]) index++;
	        else {
	            isOrdered=false;
                    // Swap
		    attribute              = countArray[index][0];
		    quantity               = countArray[index][1];
	            countArray[index][0]   = countArray[index+1][0];
	            countArray[index][1]   = countArray[index+1][1];
                    countArray[index+1][0] = attribute;
	            countArray[index+1][1] = quantity;
	            // Increment index
		    index++;
	            }
	  	}
	    } while (isOrdered==false);
    	}

    /* DEFINE CONVERSION ARRAYS: */

    /** Defines conversion and reconversion arrays.
    @param countArray The 2-D array sorted by the <TT>orderCcountArray</TT>
    method.*/

    protected void defConvertArrays(int[][] countArray) {

	// Dimension arrays

	conversionArray   = new int[numCols+1][2];
        reconversionArray = new short[numCols+1];

	// Assign values by processing the count array which has now been
	// ordered.
	for(int index=1;index<countArray.length;index++) {
            conversionArray[countArray[index][0]][0] = index;
            conversionArray[countArray[index][0]][1] = countArray[index][1];
	    reconversionArray[index] = (short) countArray[index][0];
	    }

	// Diagnostic ouput if desired
	//outputConversionArrays();
	}

    /* RECAST INPUT DATA. */

    /** Recasts the contents of the (input) data array so that each record is
    ordered according to conversion array.
    <P>Proceed as follows:

    1) For each record in the data array. Create an empty new itemSet array.
    2) Place into this array attribute/column numbers that correspond to the
       appropriate equivalents contained in the conversion array.
    3) Reorder this itemSet and return into the data array. */

    public void recastInputData() {
        short[] itemSet;
	int attribute;

	// Step through data array using loop construct

        for(int rowIndex=0;rowIndex<dataArray.length;rowIndex++) {
	    itemSet = new short[dataArray[rowIndex].length];
	    // For each element in the itemSet replace with attribute number
	    // from conversion array
	    for(int colIndex=0;colIndex<dataArray[rowIndex].length;colIndex++) {
	        attribute = dataArray[rowIndex][colIndex];
		itemSet[colIndex] = (short) conversionArray[attribute][0];
		}
	    // Sort itemSet and return to data array
	    sortItemSet(itemSet);
	    dataArray[rowIndex] = itemSet;
	    }
	}

    /* RECAST INPUT DATA AND REMOVE UNSUPPORTED SINGLE ATTRIBUTES. */

    /** Recasts the contents of the data array so that each record is
    ordered according to ColumnCounts array and excludes non-supported
    elements. <P> Proceed as follows:

    1) For each record in the data array. Create an empty new itemSet array.
    2) Place into this array any column numbers in record that are
       supported at the index contained in the conversion array.
    3) Assign new itemSet back into to data array */

    public void recastInputDataAndPruneUnsupportedAtts() {
        short[] itemSet;
	int attribute;

	// Step through data array using loop construct

        for(int rowIndex=0;rowIndex<dataArray.length;rowIndex++) {
	    // Check for empty row
	    if (dataArray[rowIndex]!= null) {
	        itemSet = null;
	        // For each element in the current record find if supported with
	        // reference to the conversion array. If so add to "itemSet".
	    	for(int colIndex=0;colIndex<dataArray[rowIndex].length;colIndex++) {
	            attribute = dataArray[rowIndex][colIndex];
		    // Check support
		    if (conversionArray[attribute][1] >= minSupport) {
		        itemSet = reallocInsert(itemSet,
		    			(short) conversionArray[attribute][0]);
		        }
		    }
	        // Return new item set to data array
	        dataArray[rowIndex] = itemSet;
	 	}
	    }

	// Set isPrunedFlag (used with GUI interface)
	isPrunedFlag=true;
	// Reset number of one item sets field
	numOneItemSets = getNumSupOneItemSets();
	}

    /* GET NUM OF SUPPORTE ONE ITEM SETS */
    /** Gets number of supported single item sets (note this is not necessarily
    the same as the number of columns/attributes in the input set).
    @return Number of supported 1-item sets */

    protected int getNumSupOneItemSets() {
        int counter = 0;

	// Step through conversion array incrementing counter for each
	// supported element found

	for (int index=1;index < conversionArray.length;index++) {
	    if (conversionArray[index][1] >= minSupport) counter++;
	    }

	// Return
	return(counter);
	}

    /* RESIZE INPUT DATA */

    /** Recasts the input data sets so that only N percent is used.
    @param percentage the percentage of the current input data that is to form
    the new input data set (number between 0 and 100). */

    public void resizeInputData(double percentage) {
	// Redefine number of rows
	numRows = (int) ((double) numRows*(percentage/100.0));
        System.out.println("Recast input data, new num rows = " + numRows);

	// Dimension and populate training set.
	short[][] trainingSet = new short[numRows][];
	for (int index=0;index<numRows;index++)
				trainingSet[index] = dataArray[index];

	// Assign training set label to input data set label.
	dataArray = trainingSet;

	// Determine new minimum support threshold value

	minSupport = (numRows * support)/100.0;
	}

    /** Reconverts given item set according to contents of reconversion array.
    @param itemSet the fgiven itemset.
    @return the reconverted itemset. */

    protected short[] reconvertItemSet(short[] itemSet) {
        // If no conversion return orginal item set
	if (reconversionArray==null) return(itemSet);

	// If item set null return null
	if (itemSet==null) return(null);

	// Define new item set
	short[] newItemSet = new short[itemSet.length];

	// Copy
	for(int index=0;index<newItemSet.length;index++) {
	    newItemSet[index] = reconversionArray[itemSet[index]];
	    }

	// Return
	return(newItemSet);
        }

    /** Reconvert single item if appropriate.
    @param item the given item (attribute).
    @return the reconvered item. */

    protected short reconvertItem(short item) {
        // If no conversion return orginal item
	if (reconversionArray==null) return(item);

	// Otherwise rerturn reconvert item
	return(reconversionArray[item]);
	}

    /* ---------------------------------------------------------- */
    /*                                                            */
    /*        CONVERT FROM HORIZONTAL TO VERTICAL FORMAT          */
    /*                                                            */
    /* ---------------------------------------------------------- */

    /* HORIZONTAL TO VERTICAL */

    /** Converts input data from horizontal format to vertical format. <P>
    Data set is stored in a 2-D array of short integers. First index (the tid
    index) represents the row/record/transaction number and second (the data
    index) the column/attribute number. WARNINGS: (1) Assumes that no input
    data reordering or pruning has been implemented, (2) Original dataset is
    deleted. */

    public void horizontal2vertical() {
        // Initialize current indexes array, list of attribute numbers in the
	// original horizontal dataset. This array will contain current index
	// markers for each new record. Initially these markers will be set at 0.
	// Note that there is no attribute 0
    	int[] currentIndexes = new int[numCols+1];
	for (int index=0;index<currentIndexes.length;index++)
				currentIndexes[index]=0;

	// Dimension new array
	short[][] newArray = h2vDimNewDataArry();

	// Loop through old dataset and cast old horizontal data to vertical
	// format.
	for (int tidIndex=0;tidIndex<dataArray.length;tidIndex++) {
	    // Check if attributes for this record?
	    if (dataArray[tidIndex] != null) {
	        for (int dataIndex=0;dataIndex<dataArray[tidIndex].length;
	    				dataIndex++) {
	            int columnIndex = dataArray[tidIndex][dataIndex]-1;
		    int currentIndex = currentIndexes[columnIndex];
		    newArray[columnIndex][currentIndex] = (short) tidIndex;
		    currentIndexes[columnIndex]++;
		    }
		}
	    }

	// Replace old array reference

	dataArray = newArray;

	// Reassign diemensions
	numCols = numRows;
	numRows = dataArray.length;
	}

    /* HORIZONTAL TO VERTICAL DIMENSION NEW DATA ARRAY */

    /** Dimensions new data array when converting from horizontal to vertical
    format.
    @return the newly dimensioned 2-D array. */

    private short[][] h2vDimNewDataArry() {
        // Count singles on old array and store in countArray,
        int[][] countArray = countSingles();

	// Initialise the new vertical data array. Overall length is equivalent
	// To the number of supported one itemSets. Length of each element
	// depends on the support for that element, i.e. the number of records
	// in the original horizontal data set where the attribute appears.
	// This is available from the countArray local 2-D array.

	short[][] newArray = new short[countArray.length-1][];
	for (int index=1;index<countArray.length;index++) {
	    newArray[countArray[index][0]-1] = new short[countArray[index][1]];
	    }

	// Return
	return(newArray);
	}

    /* -------------------------------------------------------------- */
    /*                                                                */
    /*                         SEGMENT DATA                           */
    /*                                                                */
    /* -------------------------------------------------------------- */

    /* Set of methods used for data segmnetation experiments. */

    /* SEGMENT DATA SET */

    /** Horizontally segements the input data set into N segements and stores
    to disk using file names made up of the input file nam,e plus the segment
    number.
    @param numSegments the number of segments into which the data is to be
    decompossed. */

    public void segmentDataSet(int numSegments) throws IOException {

	// Calculate number of rows per segement
	int rowsPerSegment = calcRowsPerSegment(numSegments);

	// Determin file name
	int    fileNameIndex = fileName.lastIndexOf('/');
	String shortFileName = fileName.substring(fileNameIndex+1,
				fileName.length());

	// Open input data file
	openFileName(shortFileName);

	// Loop through input data for N-1 segments
	int startRecord=0;
	int endRecord=rowsPerSegment;
	for (int segIndex=1;segIndex<numSegments;segIndex++) {
	    String outputFileName = shortFileName + segIndex;
	    // Step through input data file
	    ouputSegmentToFile(outputFileName,startRecord,endRecord);
	    // Increment counters
	    startRecord=endRecord;
	    endRecord=endRecord+rowsPerSegment;
	    }

	// Process last segment (may have slightly more records than previous
	// segments)
	String outputFileName = shortFileName + numSegments;
	ouputSegmentToFile(outputFileName,startRecord,numRows);

	// Close input file
	fileInput.close();
	}

    /* READ, SEGMENT AND PARTITION DATA SEGMENTS */

    /** Reads input data segment by segement and stores segment in memory which
    is then partitioned and written to file.
    @param numSegments the number of segments into which the data is to be
    decompossed. 	*/

    public void readSegAndPartData(int numSegments) throws IOException {

	// Calculate number of rows per segement
	int rowsPerSegment = calcRowsPerSegment(numSegments);

	// Determin "root" file name (which will have segment and partition
	// numbers apppended to it.
	int    fileNameIndex = fileName.lastIndexOf('/');
	String shortFileName = fileName.substring(fileNameIndex+1,
				fileName.length());

	// Open input data file
	openFileName(shortFileName);

	// Process all but last segment
	for (int segIndex=1;segIndex<numSegments;segIndex++) {
	    // Define data array
	    dataArray = new short[rowsPerSegment][];
	    // Read segment from input file and store in data array
	    for (int rowIndex=0;rowIndex<rowsPerSegment;rowIndex++) {
	        // Read line
		String line = fileInput.readLine();
		// Process line
	        processInputLine(line,rowIndex);
		}
	    // Partition segment
	    partitionDataArray(shortFileName + segIndex);
	    }

	// Pocess last segment
	int rowsInLastSeg = numRows-(rowsPerSegment*(numSegments-1));
	// Define data array
	dataArray = new short[rowsInLastSeg][];
	// Read segment from input file and store in data array
	for (int rowIndex=0;rowIndex<rowsInLastSeg;rowIndex++) {
	    // Read line
	    String line = fileInput.readLine();
	    // Process line
	    processInputLine(line,rowIndex);
	    }
	// Partition segment
	partitionDataArray(shortFileName + numSegments);

	// Close file
	fileInput.close();
	}

    /* PARTITION DATA AEEAY */

    /** Vertically partitions, one partition per attribute, and stores to disk
    using file names made up of the input file name plus column
    numbers.
    @param fName the name of the file data is to be stored in. */

    protected void partitionDataArray(String fName) throws IOException {

	// Loop through columns
        for (int colIndex=1;colIndex<=numOneItemSets;colIndex++) {
	    outputPartitionToFile(fName,(short) colIndex);
	    }
	}

    /* CALCULATE NUMBER OF ROWS PER SEGMENT */

    /** Calculatwes the number of records per segement for a given number of
    segements
    @param numSegments the number of segments into which the data is to be
    decompossed.
    @return the number of rows per segment. */

    protected int calcRowsPerSegment(int numSegments) {
        int rowsPerSegment = numRows/numSegments;

	// If 0 error message and exits
	if (rowsPerSegment==0) {
	    System.out.println("DATA SEGMENTATION ERROR: number of desired " +
	    	"segments (" + numSegments + ") exceeds number of records ("+
		numRows + ") in input dataset");
	    System.exit(1);
	    }

	// Return
	return(rowsPerSegment);
	}

    /* -------------------------------------------------------------- */
    /*                                                                */
    /*        RULE LINKED LIST ORDERED ACCORDING TO CONFIDENCE        */
    /*                                                                */
    /* -------------------------------------------------------------- */

    /* Methods for inserting rules into a linked list of rules ordered
    according to confidence (most confident first). Each rule described in
    terms of 3 fields: 1) Antecedent (an item set), 2) a consequent (an item
    set), 3) a confidence value (double). <P> The support field is not used. */

    /* INSERT (ASSOCIATION/CLASSIFICATION) RULE INTO RULE LINKED LIST (ORDERED
    ACCORDING CONFIDENCE). */

    /** Inserts an (association/classification) rule into the linkedlist of
    rules pointed at by <TT>startRulelist</TT>. <P> The list is ordered so that
    rules with highest "ordering value" (this is assumed to be aconfidence
    value but could equaly well be some other value such as lift) are listed
    first. If two rules have the same ordering value the new rule will be
    placed after the existing rule. Thus, if using an Apriori approach to
    generating rules, more general rules will appear first in the list with
    more specific rules (i.e. rules with a larger antecedent) appearing later
    as the more general rules will be generated first.
    @param antecedent the antecedent (LHS) of the rule.
    @param consequent the consequent (RHS) of the rule.
    @param orderingValue the associated support value.  */

    protected void insertRuleintoRulelist(short[] antecedent,
    				short[] consequent, double orderingValue) {

	// Create new node
	RuleNode newNode = new RuleNode(antecedent,consequent,orderingValue);

	// Empty list situation
	if (startRulelist == null) {
	    startRulelist = newNode;
	    return;
	    }

	// Add new node to start
	if (orderingValue > startRulelist.confidenceForRule) {
	    newNode.next  = startRulelist;
	    startRulelist = newNode;
	    return;
	    }

	// Add new node to middle
	RuleNode markerNode   = startRulelist;
	RuleNode linkRuleNode = startRulelist.next;
	while (linkRuleNode != null) {
	    if (orderingValue > linkRuleNode.confidenceForRule) {
	        markerNode.next = newNode;
		newNode.next    = linkRuleNode;
		return;
		}
	    markerNode = linkRuleNode;
	    linkRuleNode = linkRuleNode.next;
	    }

	// Add new node to end
	markerNode.next = newNode;
	}

    /* -------------------------------------------------------------- */
    /*                                                                */
    /*    RULE LINKED LIST ORDERED ACCORDING TO SIZE OF ANTECEDENT    */
    /*                                                                */
    /* -------------------------------------------------------------- */

    /* INSERT (ASSOCIATION/CLASSIFICATION) RULE INTO RULE LINKED LIST 2
    (ORDERED ACCORDING SIZE OF ANTECEDENT --- MORE SPECIFIC RULES FIRST). */

    /** Inserts an (association/classification) rule into the linked list of
    rules pointed at by <TT>startRulelist</TT>. <P> List is ordered so that
    more specific rules (i.e. rules with most attributes in their antecedent)
    are listed first.      **** NOT CURRENTLY USED ****
    @param antecedent the antecedent (LHS) of the rule.
    @param consequent the consequent (RHS) of the rule.
    @param confidenceForRule the associated confidence value. */

    protected void insertRuleintoRulelist2(short[] antecedent,
    				short[] consequent, double confidenceForRule) {

	// Create new node
	RuleNode newNode = new RuleNode(antecedent,consequent,
					confidenceForRule);

	// Empty list situation
	if (startRulelist == null) {
	    startRulelist = newNode;
	    return;
	    }

	// Add new node to start
	if (antecedent.length > startRulelist.antecedent.length) {
	    newNode.next = startRulelist;
	    startRulelist  = newNode;
	    return;
	    }

	// Add new node to middle
	RuleNode markerNode = startRulelist;
	RuleNode linkRuleNode = startRulelist.next;
	while (linkRuleNode != null) {
	    if (antecedent.length > linkRuleNode.antecedent.length) {
	        markerNode.next = newNode;
		newNode.next    = linkRuleNode;
		return;
		}
	    markerNode = linkRuleNode;
	    linkRuleNode = linkRuleNode.next;
	    }

	// Add new node to end
	markerNode.next = newNode;
	}

    /* ----------------------------------------------- */
    /*                                                 */
    /*        ITEM SET INSERT AND ADD METHODS          */
    /*                                                 */
    /* ----------------------------------------------- */

    /* APPEND */

    /** Concatenates two itemSets --- resizes given array so that its
    length is increased by size of second array and second array added.
    @param itemSet1 The first item set.
    @param itemSet2 The item set to be appended.
    @return the combined item set */

    protected short[] append(short[] itemSet1, short[] itemSet2) {

	// Test for empty sets, if found return other
	if (itemSet1 == null) return(copyItemSet(itemSet2));
	else if (itemSet2 == null) return(copyItemSet(itemSet1));

	// Create new array
	short[] newItemSet = new short[itemSet1.length+itemSet2.length];

	// Loop through itemSet 1
	int index1;
	for(index1=0;index1<itemSet1.length;index1++) {
	    newItemSet[index1]=itemSet1[index1];
	    }

	// Loop through itemSet 2
	for(int index2=0;index2<itemSet2.length;index2++) {
	    newItemSet[index1+index2]=itemSet2[index2];
	    }

	// Return
	return(newItemSet);
        }

    /* REALLOC INSERT */

    /** Resizes given item set so that its length is increased by one
    and new element inserted.
    @param oldItemSet the original item set
    @param newElement the new element/attribute to be inserted
    @return the combined item set */

    protected short[] reallocInsert(short[] oldItemSet, short newElement) {

	// No old item set
	if (oldItemSet == null) {
	    short[] newItemSet = {newElement};
	    return(newItemSet);
	    }

	// Otherwise create new item set with length one greater than old
	// item set
	int oldItemSetLength = oldItemSet.length;
	short[] newItemSet = new short[oldItemSetLength+1];
	
	// Loop	
	int index1;	
	for (index1=0;index1 < oldItemSetLength;index1++) {
	    if (newElement < oldItemSet[index1]) {
		newItemSet[index1] = newElement;	
		// Add rest	
		for(int index2 = index1+1;index2<newItemSet.length;index2++)
				newItemSet[index2] = oldItemSet[index2-1];
		return(newItemSet);
		}
	    else newItemSet[index1] = oldItemSet[index1];
	    }
	
	// Add to end	
	newItemSet[newItemSet.length-1] = newElement; 
	
	// Return new item set	
	return(newItemSet);
	}
	
    /* REALLOC 1 */
    
    /** Resizes given item set so that its length is increased by one
    and appends new element (identical to append method)
    @param oldItemSet the original item set
    @param newElement the new element/attribute to be appended
    @return the combined item set */
    
    protected short[] realloc1(short[] oldItemSet, short newElement) {
        
	// No old item set	
	if (oldItemSet == null) {
	    short[] newItemSet = {newElement};
	    return(newItemSet);
	    }

	// Otherwise create new item set with length one greater than old
	// item set
	int oldItemSetLength = oldItemSet.length;
	short[] newItemSet = new short[oldItemSetLength+1];

	// Loop
	int index;
	for (index=0;index < oldItemSetLength;index++)
		newItemSet[index] = oldItemSet[index];
	newItemSet[index] = newElement;

	// Return new item set
	return(newItemSet);
	}

    /* REALLOC 2 */

    /** Resizes given array so that its length is increased by one element
    and new element added to front
    @param oldItemSet the original item set
    @param newElement the new element/attribute to be appended
    @return the combined item set */

    protected short[] realloc2(short[] oldItemSet, short newElement) {

	// No old array
	if (oldItemSet == null) {
	    short[] newItemSet = {newElement};
	    return(newItemSet);
	    }
	
	// Otherwise create new array with length one greater than old array	
	int oldItemSetLength = oldItemSet.length;
	short[] newItemSet = new short[oldItemSetLength+1];
	
	// Loop	
	newItemSet[0] = newElement;
	for (int index=0;index < oldItemSetLength;index++)
		newItemSet[index+1] = oldItemSet[index];
	
	// Return new array	
	return(newItemSet);
	}

    /* REALLOC 3 */
    
    /** Resizes given array so that its length is decreased by one element
    and first element removed
    @param oldItemSet the original item set
    @return the shortened item set */
    
    protected short[] realloc3(short[] oldItemSet) {
        	
	// If old item set comprises one element return null	
	if (oldItemSet.length == 1) return null;
	
	// Create new array with length one greater than old array	
	int newItemSetLength = oldItemSet.length-1;
	short[] newItemSet = new short[newItemSetLength];
	
	// Loop	
	for (int index=0;index < newItemSetLength;index++)
		newItemSet[index] = oldItemSet[index+1];
	
	// Return new array	
	return(newItemSet);
	}	

    /* REALLOC 4 */ 
    
    /** Resize given array so that its length is decreased by size of
    second array (which is expected to be a leading subset of the first)
    and remove second array.
    @param oldItemSet The first item set.
    @param array2 The leading subset of the <TT>oldItemSet</TT>. 
    @return Revised item set with leading subset removed. */
   
    protected short[] realloc4(short[] oldItemSet, short[] array2) {
        int array2length   = array2.length;
	int newItemSetLength = oldItemSet.length-array2length;
	
	// Create new array 	
	short[] newItemSet = new short[newItemSetLength];
	
	// Loop	
	for (int index=0;index < newItemSetLength;index++)
		newItemSet[index] = oldItemSet[index+array2length];
	
	// Return new array	
	return(newItemSet);
	}

    /* UNION */

    /** Merge the two given itemSets to create a new itemSet. <P> Note that
    given itemSets may not be disjoint.
    @param itemSet1 The first given item set.
    @param itemSet2 the se4cond given item set.
    @return the union of the two itemSets. */

    protected short[] union(short[] itemSet1, short[] itemSet2) {
        // check for null sets
        if (itemSet1 == null) {
            if (itemSet2 == null) return(null);
            else return(itemSet2);
            }
        if (itemSet2 == null) return(itemSet1);

        // determine size of union and dimension return itemSet
        short[] newItemSet = new short[sizeOfUnion(itemSet1,itemSet2)];

        // Loop through itemSets
        int index1=0, index2=0, index3=0;
        while (index1<itemSet1.length) {
            // Check for end of itemSet2
            if (index2>=itemSet2.length) {
            	for (int index=index1;index<itemSet1.length;index++,index3++)
            	    			newItemSet[index3] = itemSet1[index];
            	break;
            	}
            // Before	
            if (itemSet1[index1] < itemSet2[index2]) {
            	newItemSet[index3] = itemSet1[index1];
            	index1++;
            	}
            else {
                // Equals
                if (itemSet1[index1] == itemSet2[index2]) {
                    newItemSet[index3] = itemSet1[index1];
            	    index1++;
            	    index2++;
            	    }
            	// After
                else {
            	    newItemSet[index3] = itemSet2[index2];
            	    index2++;
                    }	
                }
            index3++;
            }

        // add remainder of itemSet2
        for (int index=index2;index<itemSet2.length;index++,index3++)
    				newItemSet[index3] = itemSet2[index];

        // Return
        return(newItemSet);
        }
	
    /* --------------------------------------------- */
    /*                                               */
    /*            ITEM SET DELETE METHODS            */
    /*                                               */
    /* --------------------------------------------- */
    
    /* REDUNDANT?????? realloc4 does this? */
    
    /* REMOVE LEADING SUBSTRING */
    
    /** Removes leading substring from itemSet1 with respect to its
    itemSet2 which is assumed have less elements than itemSet1 and returns 
    the result.<P> Example:
    itemSet1 = [1,2,3], itemSet2 = [1,2], return [3]. 
    @param itemSet1 The first item set.
    @param itemSet2 The leading subset of the <TT>itemSet1</TT>. 
    @return Revised item set with leading subset removed. */
    
    /*protected short[] removeLeadingSubstring(short[] itemSet1, 
    							short[] itemSet2) {
	
	// If parent itemSet is null then there is no leading substring.*/

/// ***** LOOKS LIKE THIS IS UNECESSARY???????**********
	/* if (itemSet2 == null) {
	    short[] returnItemSet = new short[itemSet1.length];
	    for (int index = 0;index < itemSet1.length;index++) 
	    			returnItemSet[index] = itemSet1[index];
	    return(itemSet1);
	    }
	    
	// Loop through parent itemSet (parent itemSet will always have less 
	// elements than child itemSet)
	
	int index;
	for(index=0;index<itemSet2.length;index++) {
            if (itemSet1[index] != itemSet2[index]) break;		
	    }
	
	// Row itemSet is a subset of the given itemSet. 

        short[] returnItemSet = new short[itemSet1.length-index];
	for (int newIndex = 0;newIndex < returnItemSet.length;newIndex++) {
	    returnItemSet[newIndex] = itemSet1[index];
	    index++;
	    }
	
	// Return
	    
	return(returnItemSet);
	} */

    /* REMOVE ELEMENT N */
    
    /** Removes the nth element/attribute from the given item set.
    @param oldItemSet the given item set.
    @param n the index of the element to be removed (first index is 0). 
    @return Revised item set with nth element removed. */
    
    protected short[] removeElementN(short [] oldItemSet, int n) {
        if (oldItemSet.length <= n) return(oldItemSet);
	else {
	    short[] newItemSet = new short[oldItemSet.length-1];
	    for (int index=0;index<n;index++) newItemSet[index] = 
	    				oldItemSet[index];
	    for (int index=n+1;index<oldItemSet.length;index++) 
	        			newItemSet[index-1] = oldItemSet[index];
	    return(newItemSet);
	    }
	}
    
    /* REMOVE ELEMENT X */
    
    /** Removes the element/attribute X from the given item set if it exists.
    @param oldItemSet the given item set.
    @param x the element to be removed. 
    @return Revised item set with element x removed. */
    
    protected short[] removeElementX(short [] oldItemSet, short x) {
        // Check if item exists in given item set, if not return the
	// original set unaltered.
	if (notMemberOf(x,oldItemSet)) return(oldItemSet); 
	
	// Define new set
	short [] newItemSet = new short[oldItemSet.length-1];	
	
	// Process original set
	for (int indexOld=0,indexNew=0;indexOld<oldItemSet.length;indexOld++) {
	    if (oldItemSet[indexOld] != x) {
	    	newItemSet[indexNew] = oldItemSet[indexOld];
		indexNew++;
		}
	    }
	
	// Return
	return(newItemSet);
	}
	
    /* REMOVE FIRST ELEMENT */
    
    /** Removes first element in item set. 
    @param oldItemSet the given item set. 
    @return Revised item set with first element removed. */
    
    protected short[] removeFirstElement(short[] oldItemSet) {
        if (oldItemSet.length == 1) return(null);
    	else {
	    short[] newItemSet = new short[oldItemSet.length-1];
	    for (int index=0;index<newItemSet.length;index++) {
	        newItemSet[index] = oldItemSet[index+1];
	        }
	    return(newItemSet);
	    }
	}

    /* REMOVE FIRST N ELEMENTS */
    
    /** Removes the first n elements/attributes from the given item set.
    @param oldItemSet the given item set.
    @param n the number of leading elements to be removed. 
    @return Revised item set with first n elements removed. */
    
    protected short[] removeFirstNelements(short[] oldItemSet, int n) {
        if (oldItemSet.length == n) return(null);
    	else {
	    short[] newItemSet = new short[oldItemSet.length-n];
	    for (int index=0;index<newItemSet.length;index++) {
	        newItemSet[index] = oldItemSet[index+n];
	        }
	    return(newItemSet);
	    }
	}

    /* REMOVE LAST ELEMENT */
    
    /** Removes the last elements/attributes from the given item set.
    @param oldItemSet the given item set.
    @return Revised item set with last elements removed. */
    
    protected short[] removeLastElement(short[] oldItemSet) {
        if (oldItemSet.length == 1) return(null);
    	else {
	    short[] newItemSet = new short[oldItemSet.length-1];
	    for (int index=0;index<newItemSet.length;index++) {
	        newItemSet[index] = oldItemSet[index];
	        }
	    return(newItemSet);
	    }
	}
    
    /* REMOVE LAST N ELEMENTS */
    
    /** Removes the last n elements/attributes from the given item set.
    @param oldItemSet the given item set.
    @param n the number of elements to be removed. 
    @return Revised item set with last n elements removed. */
    
    protected short[] removeLastNelements(short[] oldItemSet, int n) {
        if (oldItemSet.length <= n) return(null);
    	else {
	    short[] newItemSet = new short[oldItemSet.length-n];
	    for (int index=0;index<newItemSet.length;index++) {
	        newItemSet[index] = oldItemSet[index];
	        }
	    return(newItemSet);
	    }
	}

    /* REMOVE ALL AFTER. */
    
    /** Removes all elements/attributes after the given element. <P> will return
    null if empty set
    @param itemSet the given item set.
    @param n the elements after which all element should be removed. 
    @return Revised item set. */
    
    protected short[] removeAllAfter(short[] itemSet, short n) {
  	
	// Check for null set
	if (itemSet == null) return(null);
	
	// Determine length for new itemSet
	int index;
	for (index=0;index<itemSet.length;index++) {
	    if (itemSet[index] > n) break;
	    }  

	// If length is zero return null
	if (index==0) return(null);
	
	// Dimension new array
	short[] newItemSet = new short[index];
	
	// Assign values to new item set
	for (int newIndex=0;newIndex<newItemSet.length;newIndex++) {
	    newItemSet[newIndex] = itemSet[newIndex];
	    }
	    
	// Return
	return(newItemSet);
	}
	
    /* RETURN ELEMENTS LESS THAN N. */
    
    /** Removes all elements/attributes whose column number is greater
    than n (assumes that item set is ordered high to low).
    @param oldItemSet the given item set.
    @param n the attribute column number 
    @return Revised item set with all attributes greater than n removed. */
    
    protected short[] returnElementsLessThanN(short [] oldItemSet, int n) {
    	// Count elements less than N
	
	int total=0;
	for (int index=0;index<oldItemSet.length;index++) {
            if (oldItemSet[index] <= n) total++;
	    }
	
	// Create returnSet
	
	if (total==0) return(null);
	else {
	    short[] returnSet = new short[total];
	    for (int index=0;index<total;index++) {
	        if (oldItemSet[index] <= n) returnSet[index] = oldItemSet[index];
		}
	    return(returnSet);
	    }
	}
	
    /* ---------------------------------------------------------------- */
    /*                                                                  */
    /*              METHODS TO RETURN SUBSETS OF ITEMSETS               */
    /*                                                                  */
    /* ---------------------------------------------------------------- */
    
    /* GET LAST ELEMENT */
    
    /** Gets the last element in the given item set, or '0' if the itemset is
    empty.
    @param itemSet the given item set. 
    @return the last element. */ 
    
    protected short getLastElement(short[] itemSet) {
        // Check for empty item set
	if (itemSet == null) return(0);
	// Otherwise return last element
        return(itemSet[itemSet.length-1]);
	}
	
    /* COMPLEMENT */
    
    /** Returns complement of first itemset with respect to second itemset.
    @param itemSet1 the first given item set.
    @param itemSet2 the second given item set.
    @return complement if <TT>itemSet1</TT> in <TT>itemSet2</TT>. */
    
    protected short[] complement(short[] itemSet1, short[] itemSet2) {
        int lengthOfComp = itemSet2.length-itemSet1.length;
	
	// Return null if no complement
	if (lengthOfComp<1) return(null);
	
	// Otherwsise define combination array and determine complement
	short[] complement  = new short[lengthOfComp];
	int complementIndex = 0;
	for(int index=0;index<itemSet2.length;index++) {
	    // Add to combination if not in first itemset
	    if (notMemberOf(itemSet2[index],itemSet1)) {
	    	complement[complementIndex] = itemSet2[index];
		complementIndex++;
		}	
	    }
	
	// Return
	return(complement);
	}
			    	
    /* --------------------------------------- */
    /*                                         */
    /*             SORT ITEM SET               */
    /*                                         */
    /* --------------------------------------- */
    		
    /* SORT ITEM SET: Given an unordered itemSet, sort the set */
    
    /** Sorts an unordered item set.
    @param itemSet the given item set. */   
    
    protected void sortItemSet(short[] itemSet) {
        short temp;	
        boolean isOrdered;
        int index; 
               
        do {
	    isOrdered = true;
            index     = 0; 
            while (index < (itemSet.length-1)) {
                if (itemSet[index] <= itemSet[index+1]) index++;
	        else {
	            isOrdered=false;
                    // Swap
		    temp = itemSet[index];
	            itemSet[index] = itemSet[index+1];
                    itemSet[index+1] = temp;
	            // Increment index
		    index++;  
	            }
	  	}     
	    } while (isOrdered==false);
    	}	

    /* ----------------------------------------------------- */
    /*                                                       */
    /*             BOOLEAN ITEM SET METHODS ETC.             */
    /*                                                       */
    /* ----------------------------------------------------- */
    		  	
    /* CHECK ITEM SET: */ 
    
    /** Determines relationship between two item sets (same, parent, 
    before, child or after).
    @param itemSet1 the first item set.
    @param itemSet2 the second item set to be compared with first.
    @return 1 = same, 2 = itemSet2 is parent of itemSet1, 3 = itemSet2
    lexicographically before itemSet1, 4 = itemSet2 is child of itemSet1, 
    and 5 = itemSet2 lexicographically after itemSet1. */

    protected int checkItemSets(short[] itemSet1, short[] itemSet2) {
    
        // Check if the same

	if (isEqual(itemSet1,itemSet2)) return(1);

	// Check whether before or after and subset/superset. 

	if (isBefore(itemSet1,itemSet2)) {
	    if (isSubset(itemSet1,itemSet2)) return(2);
	    else return(3);
	    }
        if (isSubset(itemSet2,itemSet1)) return(4);
        return(5); 
        }	

    /* EQUALITY CHECK */
    
    /** Checks whether two item sets are the same.
    @param itemSet1 the first item set.
    @param itemSet2 the second item set to be compared with first.
    @return true if itemSet1 is equal to itemSet2, and false otherwise. */

    protected boolean isEqual(short[] itemSet1, short[] itemSet2) {
	
	// If no itemSet2 (i.e. itemSet2 is null return false)
	
	if (itemSet2 == null) return(false);
	
	// Compare sizes, if not same length they cannot be equal.
	
	int length1 = itemSet1.length;
	int length2 = itemSet2.length;
	if (length1 != length2) return(false);
                             
        // Same size compare elements
                             
        for (int index=0;index < length1;index++) {
	    if (itemSet1[index] != itemSet2[index]) return(false);
	    }

        // itemSet the same. 

        return(true);
        }	
    
    /* BEFORE CHECK */
    
    /** Checks whether one item set is lexicographically before a second
    item set.
    @param itemSet1 the first item set.
    @param itemSet2 the second item set to be compared with first.
    @return true if itemSet1 is less than or equal (before) itemSet2 and
    false otherwise. Note that before here is not numerical but lexical, 
    i.e. {1,2} is before {2} */

    public static boolean isBefore(short[] itemSet1, short[] itemSet2) {
        int length2 = itemSet2.length;
	
	// Compare elements
	for(int index1=0;index1<itemSet1.length;index1++) {
	    if (index1 == length2) return(false); // itemSet2 is a proper subset of itemSet1	
    	    if (itemSet1[index1] < itemSet2[index1]) return(true);
	    if (itemSet1[index1] > itemSet2[index1]) return(false);
	    }
	
	// Return true
	return(true);
	}
		
    /* SUBSET CHECK */
    
    /** Checks whether one item set is subset of a second item set.
    @param itemSet1 the first item set.
    @param itemSet2 the second item set to be compared with first.
    @return true if itemSet1 is a subset of itemSet2, and false otherwise.
    */
    
    protected boolean isSubset(short[] itemSet1, short[] itemSet2) {
	// Check for empty itemsets
	if (itemSet1==null) return(true);
	if (itemSet2==null) return(false);
	
	// Loop through itemSet1
	for(int index1=0;index1<itemSet1.length;index1++) {
	    if (notMemberOf(itemSet1[index1],itemSet2)) return(false);
	    }
	
	// itemSet1 is a subset of itemSet2 
	return(true);
	}
    
    /* DOES INTERSECT */
    
    /** Checks whether one item set intersects with a second item set.
    @param itemSet1 the first item set.
    @param itemSet2 the second item set to be compared with first.
    @return true if two given itemSets intersect, and false otherwise. */
    
    protected boolean doesIntersect(short[] itemSet1, short[] itemSet2) {
        
	// Check for null sets
	if (itemSet1 == null) return(false);
	if (itemSet2 == null) return(false);
	
	// Loopthrough sets
	int index1=0;
	int index2=0;
	while (index1<itemSet1.length) {
	    if (index2 >= itemSet2.length) break;
	    if (itemSet1[index1] == itemSet2[index2]) return(true);
	    if (itemSet1[index1] < itemSet2[index2]) index1++;
	    else index2++;
	    }
	
	// Return
	return(false);
	}
	
    /* MEMBER OF */
    
    /** Checks whether a particular element/attribute identified by a 
    column number is a member of the given item set.
    @param number the attribute identifier (column number).
    @param itemSet the given item set.
    @return true if first argument is not a member of itemSet, and false 
    otherwise */
    
    protected boolean memberOf(short number, short[] itemSet) {
        
	// If item set is empty return false
	if (itemSet == null) return(false);
	
	// Loop through itemSet
	for(int index=0;index<itemSet.length;index++) {
	    if (number < itemSet[index]) return(false);
	    if (number == itemSet[index]) return(true);
	    }
	
	// Got to the end of itemSet and found nothing, return false
	return(false);
	}
	
    /* NOT MEMBER OF */
    
    /** Checks whether a particular element/attribute identified by a 
    column number is not a member of the given item set.
    @param number the attribute identifier (column number).
    @param itemSet the given item set.
    @return true if first argument is not a member of itemSet, and false 
    otherwise */
    
    protected boolean notMemberOf(short number, short[] itemSet) {
        
	// Loop through itemSet
	
	for(int index=0;index<itemSet.length;index++) {
	    if (number < itemSet[index]) return(true);
	    if (number == itemSet[index]) return(false);
	    }
	
	// Got to the end of itemSet and found nothing, return true
	
	return(true);
	}
	
    /* CHECK FOR LEADING SUB STRING */ 
    
    /** Checks whether two itemSets share a leading substring. 
    @param itemSet1 the first item set.
    @param itemSet2 the second item set to be compared with first.
    @return the substring if a shared leading substring exists, and null 
    otherwise. */	
	   
    protected short[] checkForLeadingSubString(short[] itemSet1, 
    							short[] itemSet2) {
        //int index3=0;
	short[] itemSet3 = null;
	
	// Loop through itemSets
	
	for(int index=0;index<itemSet1.length;index++) {
	    if (index == itemSet2.length) break;
	    if (itemSet1[index] == itemSet2[index]) 
	    			itemSet3 = realloc1(itemSet3,itemSet1[index]);
	    else break;
	    }	 	
	
	// Return
	
	return(itemSet3);
	}
		
    /* -------------------------------------------------- */
    /*                                                    */
    /*                ITEM SET COMBINATIONS               */
    /*                                                    */
    /* -------------------------------------------------- */ 
    
    /* COMBINATIONS */
    
    /** Invokes <TT>combinations</TT> method to calculate all possible 
    combinations of a given item set. <P>
    For example given the item set [1,2,3] this will result in the
    combinations[[1],[2],[3],[1,2],[1,3],[2,3],[1,2,3]].
    @param inputSet the given item set.
    @return array of arrays representing all possible combinations (may be null
    if no combinations). */

    protected short[][] combinations(short[] inputSet) {
	if (inputSet == null) return(null);
	else {
	    short[][] outputSet = new short[getCombinations(inputSet)][];
	    combinations(inputSet,0,null,outputSet,0);
	    return(outputSet);
	    }
	}
	
    /** Recursively calculates all possible combinations of a given item 
    set. 
    @param inputSet the given item set.
    @param inputIndex the index within the input set marking current 
    element under consideration (0 at start).
    @param sofar the part of a combination determined sofar during the
    recursion (null at start). 
    @param outputSet the combinations collected so far, will hold all
    combinations when recursion ends.
    @param outputIndex the current location in the output set. 
    @return revised output index. */
    
    private int combinations(short[] inputSet, int inputIndex, 
    		short[] sofar, short[][] outputSet, int outputIndex) {
    	short[] tempSet;
	int index=inputIndex;
	
    	// Loop through input array
	
	while(index < inputSet.length) {
            tempSet = realloc1(sofar,inputSet[index]);
            outputSet[outputIndex] = tempSet;
	    outputIndex = combinations(inputSet,index+1,
	    		copyItemSet(tempSet),outputSet,outputIndex+1);	
    	    index++;
	    }    

    	// Return

    	return(outputIndex);
    	}

    /* GET COMBINATTIONS */
    
    /** Gets the number of possible combinations of a given item set.
    @param set the given item set.
    @return number of possible combinations. */
    
    private int getCombinations(short[] set) {
    	int counter=0, numComb;	
	
	numComb = (int) Math.pow(2.0,set.length)-1;
	    
    	// Return

        return(numComb);
        }   	
		
    /* ---------------------------------------------------------------- */
    /*                                                                  */
    /*                            MISCELANEOUS                          */
    /*                                                                  */
    /* ---------------------------------------------------------------- */
    
    /* COPY DATA ARRAY */
    
    /** Makes a copy of the input data set.
    @param itemSet the given item set.
    @return copy of given item set. */

    protected short[][] copyDataArray() {
        return(copyItemSet(dataArray));
        }
                                        	
    /* COPY ITEM SET */

    /** Makes a copy of a given itemSet.
    @param itemSet the given item set.
    @return copy of given item set. */

    protected short[] copyItemSet(short[] itemSet) {

	// Check whether there is a itemSet to copy
	if (itemSet == null) return(null);

	// Do copy and return
	short[] newItemSet = new short[itemSet.length];
	for(int index=0;index<itemSet.length;index++) {
	    newItemSet[index] = itemSet[index];
	    }

	// Return
	return(newItemSet);
	}

    /* COPY SET OF ITEM SETS */

    /** Makes a copy of a given set of itemSets.
    @param itemSets the given set of item sets.
    @return copy of given set of item sets. */

    protected short[][] copyItemSet(short[][] itemSets) {

	// Check whether there is a itemSet to copy
	if (itemSets == null) return(null);

	// Do copy and return
	short[][] newItemSets = new short[itemSets.length][];
	for(int index1=0;index1<itemSets.length;index1++) {
	    if (itemSets[index1]==null) newItemSets[index1]=null;
	    else {
	        newItemSets[index1] = new short[itemSets[index1].length];
		for(int index2=0;index2<itemSets[index1].length;index2++) {
	            newItemSets[index1][index2] = itemSets[index1][index2];
	            }
		}
	    }

        // Return
	return(newItemSets);
	}

    /* SIZE OF UNION */

    /** Determines the size (cardinality) of the union of two given item sets.
    @param itemSet1 the first given item set.
    @param itemSet2 the second given item set.
    @return the size of the union of the two given itemSets. */

    protected int sizeOfUnion(short[] itemSet1, short[] itemSet2) {

        // check for null sets
        if (itemSet1 == null) {
            if (itemSet2 == null) return(0);
            else return(itemSet2.length);
            }
        if (itemSet2 == null) return(itemSet1.length);

        // Otherwise loop through itemSets incrementing counter
        int counter=0;
        int index1=0, index2=0;
        while (index1<itemSet1.length) {
            // Check for end of itemSet2
            if (index2>=itemSet2.length) {
            	counter=counter+itemSet1.length-index1;
            	break;
            	}
            // Before
            if (itemSet1[index1] < itemSet2[index2]) {
            	counter++;
            	index1++;
            	}
            else {
                // Equals
                if (itemSet1[index1] == itemSet2[index2]) {
            	    counter++;
            	    index1++;
            	    index2++;
            	    }
            	// After
                else {
                    counter++;
                    index2++;
                    }
                }
            }

        // add remainder of itemSet2 (if any)
        counter=counter+itemSet2.length-index2;

        // Return
        return(counter);
        }

    /* ------------------------------------------------- */
    /*                                                   */
    /*                    GET METHODS                    */
    /*                                                   */
    /* ------------------------------------------------- */

    /* GET SUPPORT */
    /** Gets the current support setting.
    @return the support value. */

    public double getSupport() {
        return(support);
	}

    /* GET CONFIDENCE */
    /** Gets the current confidence setting.
    @return the confidence value. */

    public double getConfidence() {
        return(confidence);
	}

    /* GET NUMBER OF CLASSES */
    /** Gets the number of classes setting.
    @return the number of classes field value. */

    public int getNumClasses() {
        return(numClasses);
	}

    /* GET IS PRUNED FLAG */
    /** Gets the isPrunedFlag.
    @return the isPrunedFlag. */

    public boolean getIsPrunedFlag() {
        return(isPrunedFlag);
	}

    /* GET NUMBER OF ROWS */
    /** Gets number of records/rows in input dataset.
    @return the number of rows. */

    public int getNumberOfRows() {
	return(numRows);
	}

    /* GET NUMBER OF COLUMNS */
    /** Gets number of attributes/columns in input dataset (note this is not
    the same as the number of supported 1 itemsets).
    @return the number of columns. */

    public int getNumberOfColumns() {
	return(numCols);
	}

    /* GET NUMBER OF ONE ITEM SETS */
    /** Gets the value of the <TT>numOneItemSets</TT> field, this may either be
    set to be equivalent to the number of columns in the input data or
    represent the number of "supported" one items sets after the input data has
    been ordered and pruned.
    @return the value of the <TT>numOneItemSets</TT> field. */

    public int getNumOneItemSets() {
        return(numOneItemSets);
	}

    /* GET NUMBER OF RULES */

    /** Returns the number of generated rules .
    @return the number of rules. */

    public int getNumCRs() {
        int number = 0;
        RuleNode linkRuleNode = startRulelist;

	// Loop through linked list
	while (linkRuleNode != null) {
	    number++;
	    linkRuleNode = linkRuleNode.next;
	    }

	// Return
	return(number);
	}

    /* GET INPUT FILE NAME */
    /** Gets the name of the input file.
    @return the filename. */

    public String getFileName() {
        return(fileName);
	}

    /* GET START OF RULE LIST */
    /** Gets the reference to the start of the (assocoation/classifier) rule
    list.
    @return the reference to the first <TT>RuleNnode</TT> in the rule list. */

    public RuleNode getStartRulelist() {
    	return(startRulelist);
	}

    /* GET DATA ARRAY */
    /** Gets reference to the start of the 2-D data array.
    @return the reference to the 2-D data array. */

    public short[][] getDataArray() {
        return(dataArray);
	}

    /* GET RECONVERSION ARRAY. */
    /** Gets reference to the start of the reconversion array.
    @return the reference to the reconversion array. */

    public short[] getReconArray() {
	return(reconversionArray);
	}

    /* GET HAVE DATA FLAG */

    /** Returns the haveDataFlag.
    @return value for haveDataFlag */

    public boolean getHaveDataFlag() {
        return(haveDataFlag);
	}

    /* GET IS ORDERED FLAG */

    /** Returns the isOrderedFlag.
    @return value for isOrderedFlag */

    public boolean getIsOrderedFlag() {
        return(isOrderedFlag);
        }

    /* ------------------------------------------------- */
    /*                                                   */
    /*                    SET METHODS                    */
    /*                                                   */
    /* ------------------------------------------------- */

    /* SET SUPPORT */
    /** Sets the current support setting.
    @param the supp value. */

    public void setSupport(double supp) {
        support=supp;
	}

    /* SET MIN SUPPORT */
    /** Sets the minimum support (in terms or number of rows in data set)
    setting. */

    public void setMinSupport() {
	minSupport = (numRows * support)/100.0;
	}

    /* SET CONFIDENCE */
    /** Sets the current confidence setting.
    @param the conf value. */

    public void setConfidence(double conf) {
        confidence=conf;
	}

    /* SET NUMBER OF CLASSES */
    /** Sets a value for the number of classes field.
    @param nClasses the given number of classes. */

    public void setNumClasses(int nClasses) {
        numClasses=nClasses;
	}

    /* SET NUMBER ONE ITEM SETS */
    /** Sets a value for the number of one item sets field.
    @param nOneItemSets the given number of classes. */

    public void setNumOneItemSets(int nOneItemSets) {
	numOneItemSets = nOneItemSets;
	}

    /* SET NUMBER OF ROWS AND COLIMNS (UNUSED!) */
    /**	Set the number or rows and number of columns fields.
    @param rows the number of rows
    @param columns the number of columns */

    public void setNumRowsAndCols(int rows, int columns) {
        numRows=rows;
	numCols=columns;
	}

    /* SET NUMBER OF ROWS  */
    /**	Set the number or rows field.
    @param rows the number of rows */

    public void setNumberOfRows(int rows) {
        numRows=rows;
	}

    /* SET NUMBER OF COLUMNS  */
    /**	Set the number or columns field.
    @param columns the number of columns */

    public void setNumberOfCols(int columns) {
        numCols=columns;
	}

    /* SET ORDERED AND PRUNED FLAGS */
    /** Resets ordered and pruned flags to "false" when fresh input data set
    is loaded. */

    public void setOrderedAndPrunedFlags() {
        isOrderedFlag = false;
	isPrunedFlag  = false;
	}

    /* SET START OF RULE LIST */
    /** Sets the start of the (association/classification) rule list.
    @param startRef the reference to the start of the rule list. */

    public void setStartRulelist(RuleNode startRef) {
    	startRulelist=startRef;
	}

    /* SET DATA ARRAY */
    /** Sets reference to start of the given 2-D data array.
    @param dataArrayRef the reference to the given 2-D data array. */

    public void setDataArray(short[][] dataArrayRef) {
        dataArray = dataArrayRef;
	}

    /* SET RECONVERSION ARRAY. */
    /** Sets reference to start of the given reconversion array.
    @param dataArrayRef the reference to the given reconversion array. */

    public void setReconArray(short[] dataArrayRef) {
	reconversionArray=dataArrayRef;
	}

    /* ------------------------------------------------- */
    /*                                                   */
    /*                   OUTPUT METHODS                  */
    /*                                                   */
    /* ------------------------------------------------- */

    /* ----------------- */	
    /* OUTPUT DATA TABLE */  
    /* ----------------- */  
    /** Outputs stored input data set; initially read from input data file, but
    may be reordered or pruned if desired by a particular application. */
     
    public void outputDataArray() {
        if (isPrunedFlag) System.out.println("DATA SET (Ordered and Pruned)\n" +
					"-----------------------------");
	else {
	    if (isOrderedFlag) System.out.println("DATA SET (Ordered)\n" +
					"------------------");
	    else System.out.println("DATA SET\n" + "--------");
	    }
	
	// Loop through data array
        for(int index=0;index<dataArray.length;index++) {
	    System.out.print("Rec #" + (index+1) + ": ");
	    outputItemSet(dataArray[index]);
	    System.out.println();
	    }
	
	// End
	System.out.println();
	}
    
    /** Outputs the given array of array of short integers. <P> Used for
    diagnostic purposes. 
    @param dataSet the given array of arrays. */
    
    protected void outputDataArray(short[][] dataSet) {
        if (dataSet==null) {
	    System.out.println("null");
	    return;
	    } 
	
	// Loop through data array
        for(int index=0;index<dataSet.length;index++) {
	    System.out.print("Rec #" + (index+1) + ": ");
	    outputItemSet(dataSet[index]);
	    System.out.println();
	    }
	
	// End
	System.out.println();
	}
	
    /* -------------- */
    /* OUTPUT ITEMSET */  
    /* -------------- */  
    /** Outputs a given item set. 
    @param itemSet the given item set. */
    
    protected ArrayList<Integer> outputItemSet(short[] itemSet) {
    	ArrayList<Integer> tempSequence = new ArrayList<Integer>();
	// Check for empty set
	if (itemSet == null) System.out.print(" null ");
	// Process
	else {
	    // Reconvert where input dataset has been reordered and possible 
	    // pruned.
	    short[] tempItemSet = reconvertItemSet(itemSet);
	    // Loop through item set elements
            int counter = 0;
	    for (int index=0;index<tempItemSet.length;index++) {
	        /*if (counter == 0) {
	    	    counter++;
		    System.out.print(" {");
		    }
	        else System.out.print(" ");*/
	        tempSequence.add(Integer.parseInt(Short.toString(tempItemSet[index])));
	        //System.out.print(tempItemSet[index]);
		}
	    //System.out.print("} ");
	    }
	return tempSequence;
	}

    /* ---------------------------------*/
    /* OUTPUT ITEMSET WITH RECONVERSION */
    /* ---------------------------------*/
    /** Outputs a given item set reconverting it to its original column number
    labels (used where input dataset has been reordered and possible pruned).
    @param itemSet the given item set. */

    protected void outputItemSetWithReconversion(short[] itemSet) {
        System.out.println("ERROR: outputItemSetWithReconversion depricated " +
		"use outputItemSet instead");
	}

    /* ---------------------- */
    /* OUTPUT DATA ARRAY SIZE */
    /* ---------------------- */
    /** Outputs size (number of records and number of elements) of stored
    input data set read from input data file. */

    public void outputDataArraySize() {
    	int numRecords = 0;
	int numElements = 0;

	// Loop through data array
	for (int index=0;index<dataArray.length;index++) {
	    if (dataArray[index] != null) {
	        numRecords++;
		numElements = numElements+dataArray[index].length;
	        }
	    }

	// Output
	System.out.println("Number of records        = " + numRecords);
	System.out.println("Number of elements       = " + numElements);
	double density = (double) numElements/ (numCols*numRecords);
	System.out.println("Data set density   = " + twoDecPlaces(density) +
								"%");
	}

    /* ------------------------ */
    /* OUTPUT CONVERSION ARRAYS */
    /* ------------------------ */

    /** Outputs conversion array (used to renumber columns for input data
    in terms of frequency of single attributes --- reordering will enhance
    performance for some ARM algorithms). */

    public void outputConversionArrays() {
        // Conversion array
        System.out.println("CONVERSION ARRAY\nindex = old attribute number\n" +
	                          "value [index][0] = new attribute number, " +
	                                  "value [index][1] = support count.");
	for(int index=1;index<conversionArray.length;index++) {
	    System.out.println("(" + index + ") " + conversionArray[index][0] +
	    			            ", " + conversionArray[index][1]);
	    }

        // Reconversion array
        System.out.println("\nRECONVERSION ARRAY\nindex = new attribute " +
		                      "number, value = old attribute number.");
	for(int index=1;index<reconversionArray.length;index++) {
	    System.out.println("(" + index + ") " + reconversionArray[index]);
	    }
	}

    /** Outputs conversion array (used to renumber columns for input data
    in terms of frequency of single attributes --- reordering will enhance
    performance for some ARM algorithms) (GUI Version.
    @param textArea the given instance of the <TT>JTextArea</TT> class.  */

    /*public void outputConversionArrays(JTextArea textArea) {
        if (conversionArray==null) {
            textArea.append("No conversion arrays found.\n");
            return;
            }

        // Conversion array
        textArea.append("CONVERSION ARRAY\nindex = old attribute number\n" +
	                          "value [index][0] = new attribute number, " +
	                                "value [index][1] = support count.\n");
	for(int index=1;index<conversionArray.length;index++) {
	    textArea.append("(" + index + ") " + conversionArray[index][0] +
	    			      ", " + conversionArray[index][1] + "\n");
	    }

        // Reconversion array
        textArea.append("\nRECONVERSION ARRAY\nindex = new attribute " +
		                    "number, value = old attribute number.\n");
	for(int index=1;index<reconversionArray.length;index++) {
	    textArea.append("(" + index + ") " + reconversionArray[index] +
                                                                         "\n");
	    }
	}*/


    /* ----------- */
    /* OUTPUT MENU */
    /* ----------- */
    /** Outputs menu for command line arguments. */

    protected void outputMenu() {
        System.out.println();
	System.out.println("-C  = Confidence (default 80%)");
	System.out.println("-F  = File name");
	System.out.println("-N  = Number of classes (Optional)");
	System.out.println("-S  = Support (default 20%)");
	System.out.println();

	// Exit

	System.exit(1);
	}

    /* --------------- */
    /* OUTPUT SETTINGS */
    /* --------------- */
    /** Outputs command line values provided by user. */

    protected void outputSettings() {
        System.out.println("SETTINGS\n--------");
	System.out.println("File name                = " + fileName);
	System.out.println("Support (default 20%)    = " + support);
	System.out.println("Confidence (default 80%) = " + confidence);
	System.out.println("Num. classes (Optional)  = " + numClasses);
	System.out.println();
        }

    /* OUTPUT SETTINGS */
    /** Outputs instance field values. */

    protected void outputSettings2() {
        System.out.println("SETTINGS\n--------");
        System.out.println("Number of records        = " + numRows);
	System.out.println("Number of columns        = " + numCols);
	System.out.println("Support (default 20%)    = " + support);
	System.out.println("Confidence (default 80%) = " + confidence);
        System.out.println("Min support              = " + minSupport +
					" (records)");
	System.out.println("Num one itemsets         = " + numOneItemSets);
	System.out.println("Num. classes (Optional)  = " + numClasses);
	}

    /* -------------------------------------- */
    /* OUTPUT SUPPORT AND CONFIDENCE SETTINGS */
    /* -------------------------------------- */
    /** Outputs current support and confidence settings. */

    public void outputSuppAndConf() {
	System.out.println("Support = " + twoDecPlaces(support) +
			", Confidence = " + twoDecPlaces(confidence));
        }

    /* ------------------------ */
    /* OUTPUT RULE LINKED LISTS */
    /* ------------------------ */

    /** Outputs contents of rule linked list (if any) asuming that the list
    represents a set of ARs.	*/

    public void outputRules() {
        // Check for empty rule list
	if (startRulelist==null) {
	    System.out.println("NO RULES GENERATED");
	    return;
	    }

	// Otherwise process list
        outputRules(startRulelist);
	}

    /** Outputs given rule list.
    @param ruleList the given rule list. */

    public void outputRules(RuleNode ruleList) {
	// Check for empty rule list
	if (ruleList==null) System.out.println("No rules generated!");

	// Loop through rule list
	int number = 1;
        RuleNode linkRuleNode = ruleList;
	while (linkRuleNode != null) {
	    System.out.print("(" + number + ") ");
	    outputRule(linkRuleNode);
            System.out.println(" " +
	    		twoDecPlaces(linkRuleNode.confidenceForRule));
	    number++;
	    linkRuleNode = linkRuleNode.next;
	    }
	}

    /** Outputs a rule asuming that the rule represents an ARs.
    @param rule the rule to be output. */

    private void outputRule(RuleNode rule) {
        outputItemSet(rule.antecedent);
	System.out.print(" -> ");
        outputItemSet(rule.consequent);
	}

    /* OUTPUT RULE LINKED LIST WITH RECONVERSION */
    /** Outputs contents of rule linked list (if any) asuming that the list
    represents a set of ARs with reconversion. <P> Used where input has been
    reordered so that most frequent attributes are listed first in which case,
    without reconversion, the orinal attribute N will probably not be the new
    attribute N. */

    public void outputRulesWithReconversion() {
	System.out.println("ERROR: outputRulesWithReconversion  depricated" +
		"use outputRules instead");
	}

    /* OUTPUT RULE LINKED LIST WITH DEFAULT */
    /** Outputs contents of rule linked list (if any), asuming that the list
    represents a set of CRs, such that last rule is the default rule. */

    public void outputRulesWithDefault() {
        // Check for emty rule list
	if (startRulelist==null) {
	    System.out.println("NO RULES GENERATED");
	    return;
	    }

	// Process list
        int number = 1;
        RuleNode linkRuleNode = startRulelist;
	while (linkRuleNode != null) {
	    // Output rule number
	    System.out.print("(" + number + ") ");
	    // Output antecedent
	    if (linkRuleNode.next==null) System.out.print("Default -> ");
	    else {
	        outputItemSet(linkRuleNode.antecedent);
	        System.out.print(" -> ");
		}
	    // Output concequent
            outputItemSet(linkRuleNode.consequent);
            System.out.println(" " +
	    		twoDecPlaces(linkRuleNode.confidenceForRule));
	    // Increment parameters
	    number++;
	    linkRuleNode = linkRuleNode.next;
	    }
	}

    /* OUTPUT RULE LINKED LIST WITH DEFAULT (FIRST N) */
    /** Outputs first N tule in rule linked list (if any), asuming that the
    list represents a set of CRs, such that last rule is the default rule.
    @param numberOfRulesToBeOutput the maximum number of rules to output. */

    public void outputRulesWithDefault(int numberOfRulesToBeOutput) {
        // Check for emty rule list
	if (startRulelist==null) {
	    System.out.println("NO RULES GENERATED");
	    return;
	    }

	// Process list
        int number = 1;
        RuleNode linkRuleNode = startRulelist;
	while (numberOfRulesToBeOutput>=0) {
	    if (linkRuleNode== null) break;
	    // Output rule number
	    System.out.print("(" + number + ") ");
	    // Output antecedent
	    if (linkRuleNode.next==null) System.out.print("Default -> ");
	    else {
	        outputItemSet(linkRuleNode.antecedent);
	        System.out.print(" -> ");
		}
	    // Output concequent
            outputItemSet(linkRuleNode.consequent);
            System.out.println(" " +
	    		twoDecPlaces(linkRuleNode.confidenceForRule));
	    // Increment/decrement parameters
	    number++;
	    linkRuleNode = linkRuleNode.next;
	    numberOfRulesToBeOutput--;
	    }
	}

    /* OUTPUT RULE LINKED LIST WITH DEFAULT AND RECONVERSION */
    /** Outputs contents of rule linked list (if any), asuming that the list
    represents a set of CRs, such that last rule is the default rule with
    reconversion.  <P> Used where input has been reordered so that most
    frequent attributes are listed first in which case, without reconversion,
    the orinal attribute N will probably not be the new attribute N. */

    public void outputRulesWithDefaultRecon() {
	System.out.println("ERROR: outputRulesWithDefaultRecon  depricated " +
		"use outputRulesWithDefault instead");
	}

    /* OUTPUT NUMBER OF RULES */

    /** Outputs number of generated rules (ARs or CARS). */

    public void outputNumRules() {
        System.out.println("Number of rules         = " + getNumCRs());
	}

    /* ----------------------------------------------------- */
    /*                                                       */
    /*                   GUI OUTPUT METHODS                  */
    /*                                                       */
    /* ----------------------------------------------------- */

    /* OUTPUT DATA TABLE */
    /** Outputs stored input data set; initially read from input data file, but
    may be reordered or pruned if desired by a particular application.
    @param textArea the text area. */

    /*public void outputDataArray(JTextArea textArea) {
        if (isPrunedFlag) textArea.append("DATA SET (Ordered and Pruned)\n" +
					"-----------------------------\n");
	else {
	    if (isOrderedFlag) textArea.append("DATA SET (Ordered)\n" +
					"------------------\n");
	    else textArea.append("DATA SET\n" + "--------\n");
	    }

	for(int index=0;index<dataArray.length;index++) {
	    outputItemSet(textArea,dataArray[index]);
	    textArea.append("\n");
	    }
	}*/

    /* OUTPUT DATA TABLE SCHEMA */
    /** Outputs stored input data set as a set of outputb schema labels.
    @param textArea the text area. */

    /*public void outputDataArraySchema(JTextArea textArea) {
        if (isPrunedFlag) textArea.append("DATA SET (pruned)\n" +
					"-----------------------------\n");
	else {
	    if (isOrderedFlag) textArea.append("DATA SET (Ordered)\n" +
					"------------------\n");
	    else textArea.append("DATA SET\n" + "--------\n");
	    }

	for(int index=0;index<dataArray.length;index++) {
	    outputItemSetSchema(textArea,dataArray[index]);
	    textArea.append("\n");
	    }
	}*/

    /** Outputs a given item set using output schema labels.
    @param textArea the text area.
    @param itemSet the given item set. */

    /*protected void outputItemSetSchema(JTextArea textArea, short[] itemSet) {
	// Check for empjava ty set
	if (itemSet == null) textArea.append(" {} ");
	// Process
	else {
	    // Reconvert where input dataset has been reordered and possible
	    // pruned.
	    short[] tempItemSet = reconvertItemSet(itemSet);
	    // Loop through item set elements
            int counter = 0;
	    for (int index=0;index<tempItemSet.length;index++) {
	        if (counter == 0) {
	    	    counter++;
		    textArea.append(" {");
		    }
	        else textArea.append(" ");
	        textArea.append(outputSchema[tempItemSet[index]-1]);
		}
	    textArea.append("} ");
	    }
	}*/

    /* OUTPUT OUTPUT SCHEMA */
    /** Outputs stored input data set; initially read from input data file, but
    may be reordered or pruned if desired by a particular application.
    @param textArea the text area. */

   /* public void outputOutputSchema(JTextArea textArea) {
	for(int index=0;index<outputSchema.length;index++) {
            textArea.append("(" + (index+1) + ") " + outputSchema[index] +
                                  "\n");
	    }
	}*/

    /* OUTPUT ITEMSET */
    /** Outputs a given item set.
    @param textArea the text area.
    @param itemSet the given item set. */

    /*protected void outputItemSet(JTextArea textArea, short[] itemSet) {
	// Check for emty set
	if (itemSet == null) textArea.append(" null ");
	// Process
	else {
	    // Reconvert where input dataset has been reordered and possible
	    // pruned.
	    short[] tempItemSet = reconvertItemSet(itemSet);
	    // Loop through item set elements
            int counter = 0;
	    for (int index=0;index<tempItemSet.length;index++) {
	        if (counter == 0) {
	    	    counter++;
		    textArea.append(" {");
		    }
	        else textArea.append(" ");
	        textArea.append(Short.toString(tempItemSet[index]));
		}
	    textArea.append("} ");
	    }
	}*/

    /** Outputs contents of rule linked list (if any) in terms of a set of
    attrivute numbers and asuming that the list represents a set of ARs (
    (GUI version).
    @param textArea the text area. */

   /* public void outputRules(JTextArea textArea) {
        // Check for emty rule list
	if (startRulelist==null) {
	    textArea.append("NO RULES GENERATED\n");
	    return;
	    }

	// Process list
        int number = 1;
        RuleNode linkRuleNode = startRulelist;
	while (linkRuleNode != null) {
	    textArea.append("(" + number + ") ");
	    outputItemSet(textArea,linkRuleNode.antecedent);
	    textArea.append(" -> ");
            outputItemSet(textArea,linkRuleNode.consequent);
            textArea.append(" " +
	    		twoDecPlaces(linkRuleNode.confidenceForRule) + "\n");
	    number++;
	    linkRuleNode = linkRuleNode.next;
	    }
	}*/


    /** Outputs contents of rule linked list (if any) in terns of schema
    labels asuming that the list represents a set of ARs (GUI version).
    @param textArea the text area. */

    /*public void outputRulesSchema(JTextArea textArea) {
        // Check for emty rule list
	if (startRulelist==null) {
	    textArea.append("NO RULES GENERATED\n");
	    return;
	    }

	// Process list
        int number = 1;
        RuleNode linkRuleNode = startRulelist;
	while (linkRuleNode != null) {
	    textArea.append("(" + number + ") ");
	    outputItemSetSchema(textArea,linkRuleNode.antecedent);
	    textArea.append(" -> ");
            outputItemSetSchema(textArea,linkRuleNode.consequent);
            textArea.append(" " +
	    		twoDecPlaces(linkRuleNode.confidenceForRule) + "\n");
	    number++;
	    linkRuleNode = linkRuleNode.next;
	    }
	}*/

    /* OUTPUT MINIMUM SUPPORT */
    /** First calculate and then outputs the minimum support value in terms of
    number of records in the dataset.
    @param textArea the text area. */

    /*public void outputMinSupport(JTextArea textArea) {
	minSupport = (numRows * support)/100.0;
        textArea.append("Min support = " + minSupport + " (records)\n");
	}*/

    /* OUTPUT SETTINGS */
    /** Outputs instance field values --- GUI version.
    @param textArea the text area. */

    /*public void outputSettings(JTextArea textArea) {
        textArea.append("SETTINGS\n--------\n");
	textArea.append("Number of records = " + numRows + "\n");
	textArea.append("Number of columns = " + numCols + "\n");
	textArea.append("Support (default 20%)    = " + support + "\n");
	textArea.append("Confidence (default 80%) = " + confidence + "\n");
        textArea.append("Min support = " + twoDecPlaces(minSupport) +
			" (records)\n");
	textArea.append("Num one itemsets = " + numOneItemSets
			+ "\n");
	}*/

    /* --------------------------------- */
    /*                                   */
    /*        DIAGNOSTIC OUTPUT          */
    /*                                   */
    /* --------------------------------- */

    /* OUTPUT DURATION */
    /** Outputs difference between two given times.
    @param time1 the first time.
    @param time2 the second time.
    @return duration. */

    public double outputDuration(double time1, double time2) {
        double duration = (time2-time1)/1000;
	System.out.println("Generation time = " + twoDecPlaces(duration) +
			" seconds (" + twoDecPlaces(duration/60) + " mins)");

	// Return
	return(duration);
	}

    /* OUTPUT DURATION (GUI VERSION). */
    /** Outputs difference between two given times.
    @param textArea the text area.
    @param time1 the first time.
    @param time2 the second time.   */

    /*public void outputDuration(JTextArea textArea, double time1, double time2) {
        double duration = (time2-time1)/1000;
	textArea.append("Generation time = " + twoDecPlaces(duration) +
			" seconds (" + twoDecPlaces(duration/60) + " mins)\n");
	}*/

    /* GET DURATION */
    /** Returns the difference between two given times as a string.
    @param time1 the first time.
    @param time2 the second time.
    @return the difference between the given times as a string. */

    protected String getDuration(double time1, double time2) {
        double duration = (time2-time1)/1000;
    	return("Generation time = " + twoDecPlaces(duration) +
			" seconds (" + twoDecPlaces(duration/60) + " mins)");
	}

    /* -------------------------------- */
    /*                                  */
    /*        SIMILARITY UTILITIES      */
    /*                                  */
    /* -------------------------------- */

    /* SIMILAR 2 DFECIMAL PLACES */

    /** Compares two real numbers and returns true if the two numbers are
    the same within two decimal places.
    @param number1 the first given real number.
    @param number2 the second given number.
    @return true if similar within two decimal places ad false otherwise. */

    protected boolean similar2dec(double number1, double number2) {
        // Convert to integers
        int numInt1 = (int) ((number1+0.005)*100.0);
        int numInt2 = (int) ((number2+0.005)*100.0);

        // Compare and return
        if (numInt1 == numInt2) return(true);
        else return(false);
        }

    /* ------------------------------------------------------ */
    /*                                                        */
    /*                   FILE OUTPUT METHODS                  */
    /*                                                        */
    /* ------------------------------------------------------ */

    /* OUTPUT DATA ARRAY TO FILE */

    /** Outputs contents of data array to file. <P> WARNING will overwrite
    existing data if stored in the same directory as the application
    exacutable, data files are better stored in a separate "DataFiles"
    directory. */

    public void outputDataArrayToFile() throws IOException{
        //Determin file name
	int    fileNameIndex = fileName.lastIndexOf('/');
	String shortFileName = fileName.substring(fileNameIndex+1,
							fileName.length());

	// Open file for writing
	PrintWriter outputFile = new PrintWriter(new FileWriter(shortFileName));

	// Write contents of Data array to file
	for (int rowIndex = 0;rowIndex<numRows;rowIndex++) {
	    if (dataArray[rowIndex] != null) {
	        boolean firstAtt = true;
	        // Loop through row
	        for (int colIndex=0;colIndex<dataArray[rowIndex].length;
	    							colIndex++) {
	            if (firstAtt) firstAtt=false;
	            else outputFile.print(" ");
	            outputFile.print(dataArray[rowIndex][colIndex]);
	            }
	        outputFile.println();
	        }
	    }

	// Close file
	outputFile.close();
	}

    /* OUTPUT SEGMENT TO FILE */

    /** Outputs a segment of the input dataset to file.
    @param  outputFileName the name of the output file
    @param startRecord the record marking the start of the segment
    @param endRecord the record marking the end of the segement. */

    private void ouputSegmentToFile(String outputFileName, int startRecord,
    					int endRecord) throws IOException {
        // Open file for writing
	PrintWriter outputFile = new
				PrintWriter(new FileWriter(outputFileName));

	// Step through data array
	for (int rowIndex = startRecord;rowIndex<endRecord;rowIndex++) {
	    // Read line
	    String line = fileInput.readLine();
	    // Output line
	    outputFile.println(line);
	    }
	outputFile.close();
	}

    /* OUTPUT PARTION TO FILE */

    /** Outputs a one column partition of the data array to file.
    @param fName the name of the output file
    @param colNumber the column number indicating the partition. */

    private void outputPartitionToFile(String fName, short colNumber)
    				throws IOException {

        // Open file for writing
	PrintWriter outputFile = new PrintWriter(new FileWriter(fName +
						"." + colNumber));

	// Step through data array
	for (int rowIndex = 0;rowIndex<dataArray.length;rowIndex++) {
	    if (memberOf(colNumber,dataArray[rowIndex])) {
	        boolean firstAtt = true;
	        for (int colIndex=0;colIndex<dataArray[rowIndex].length;
								colIndex++) {
		    if (firstAtt) firstAtt=false;
	            else outputFile.print(" ");
		    outputFile.print(dataArray[rowIndex][colIndex]);
		    if (dataArray[rowIndex][colIndex]==colNumber) break;
		    }
		outputFile.println();
		}
	    else outputFile.println();
	    }

	// End
	outputFile.close();
	}

    /* OUTPUT ITEMSET TO FILE */
    /** Outputs a given item set: GUI version.
    @param itemSet the given item set.
    @param outputFile the name ofb the desirted output file. */

    protected void outputItemSetToFile(short[] itemSet,
    				PrintWriter outputFile) throws IOException {

	// Loop through item set elements

	if (itemSet == null) outputFile.print(" null ");
	else {
            int counter = 0;
	    for (int index=0;index<itemSet.length;index++) {
	        if (counter == 0) counter++;
		else outputFile.print(" ");
	        outputFile.print(Short.toString(itemSet[index]));
		}
	     outputFile.println();
	    }
	}

    /* -------------------------------- */
    /*                                  */
    /*        OUTPUT UTILITIES          */
    /*                                  */
    /* -------------------------------- */

    /* TWO DECIMAL PLACES */

    /** Converts given real number to real number rounded up to two decimal
    places.
    @param number the given number.
    @return the number to two decimal places. */

    protected double twoDecPlaces(double number) {
    	int numInt = (int) ((number+0.005)*100.0);
	number = ((double) numInt)/100.0;
	return(number);
	}
    }


