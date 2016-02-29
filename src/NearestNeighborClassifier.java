import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import com.sun.org.apache.bcel.internal.generic.RETURN;
import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

public class NearestNeighborClassifier {

	private class Record {
		double[] attrList;
		String label;

		public Record(double[] attrList, String label) {
			this.attrList = attrList;
			this.label = label;
		}
		
		public int numberOfAttributes(){
			return attrList.length;
		}

		@Override
		public String toString() {
			StringBuffer sBuffer = new StringBuffer("");
			for (double dub : this.attrList) {
				sBuffer.append(String.format("%.2f", dub) + ", ");
			}
			sBuffer.replace(sBuffer.length() - 2, sBuffer.length(), " || ");
			sBuffer.append(this.label);
			return sBuffer.toString();
		}
	}

	public static final String BINARY = "binary";
	public static final String CATEGORICAL = "categorical";
	public static final String ORDINAL = "ordinal";
	public static final String CONTINUOUS = "continuous";
	public static final String LABEL = "label";
	private static final TreeSet<String> attributeTypes = new TreeSet<String>(
			Arrays.asList(BINARY, CATEGORICAL, ORDINAL, CONTINUOUS, LABEL));

	/*
	 * *********************************************************
	 * MAIN METHOD
	 * *********************************************************
	 */
	public static void main(String[] args) {
		/*
		 * **Part 1
		 */
		NearestNeighborClassifier nnc = new NearestNeighborClassifier();
		
		String[] theFileNames = new String[]{
				"0_1", "0_2", "0_3", "0_4", "0_5",
				"1_1", "1_2", "1_3", "1_4", "1_5",
				"2_1", "2_2", "2_3", "2_4", "2_5",
				"3_1", "3_2", "3_3", "3_4", "3_5"
		};
		ArrayList<String> fileNames = new ArrayList<>(Arrays.asList(theFileNames));
		ArrayList<String> labels = new ArrayList<>();
		for(String nameOfDigit: fileNames){
			labels.add(nameOfDigit.substring(0, 1));
		}
		nnc.loadDigitTrainingData(fileNames, labels);
		for(int index = 0; index < nnc.records.get(0).numberOfAttributes();index++){
			nnc.attributeList.add(BINARY);
		}
		
		String[] theTestFileNames = new String[] {
				"0_test", "1_test", "2_test", "3_test"
		};
		ArrayList<String> testFileNames = new ArrayList<>(Arrays.asList(theTestFileNames));
		ArrayList<String> testLabels = new ArrayList<>();
		for(String theTestFileName: testFileNames){
			testLabels.add(theTestFileName.substring(0, 1));
		}
		ArrayList<Record> testRecords = new ArrayList<>();
		int fileNumber = 0;
		for(String fileName:testFileNames){
			List<String> linesOfFile = null;
			try {
				linesOfFile = Files.readAllLines(Paths.get(fileName),
						Charset.defaultCharset());
			} catch (IOException e) {	e.printStackTrace();	}
			
			Record record = nnc.digitRecordFromLines(linesOfFile);
			record.label = labels.get(fileNumber);
			testRecords.add(record);
			fileNumber++;
		}
		
		//System.out.println(nnc);
		nnc.classify(testRecords);
		/*
		try {
			nnc.loadTrainingData("train4_bank");
		} catch (Exception e) {
			System.out.println("error");
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		System.out.println(nnc.toString());
		//double trainingError = nnc.calculateTrainingError();
		//System.out.println("training error: " + trainingError);
		double trainErrorOneOut = nnc.calculateTrainingErrorWithLeaveOneOut();
		System.out.println("train error one out: " + trainErrorOneOut);
		//nnc.writeClassifiedLabelsToFile("output.txt", nnc.classify(nnc.records));
		 */
		 
		/*
		ArrayList<Record> testRecords = null;
		try {
			testRecords = nnc.getTestRecordsFromFile("test3");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ArrayList<String> listOfLabels = nnc.classify(testRecords);
		for(int i = 0; i < testRecords.size();i++){
			testRecords.get(i).label = listOfLabels.get(i);
			
		}
		System.out.println("Test Records classified");
		for(Record record: testRecords){
			System.out.println(record);
		}
		*/
	}
	private final double CATEGORICAL_MATCHING_WEIGHT = 0.4;
	private final double BINARY_MATCHING_WEIGHT = 0.4;

	private int numberOfRecords;
	private int numberOfAttributes;

	private int numberOfClasses;
	private int numberOfNearestNeighbors;
	private int distanceMeasure;// ?? is this the right type ??

	private boolean majorityRule;

	// list of the type of the variables in records (ordinal, continuous, etc)
	private ArrayList<String> attributeList = new ArrayList<>();

	private ArrayList<Record> records = new ArrayList<>();

	// for continuous variables (key is column, value is range (array of len 2)
	private HashMap<Integer, double[]> rangeAtColumn = new HashMap<>();
	// for ordinal variables
	private HashMap<Integer, HashMap<String, Double>> valsForOrdinalVarAtColumn = new HashMap<>();
	//for binary variables
	private HashMap<String, Integer> binaryNameToIntSymbol = new HashMap<>();
	//for categorical variables
	private HashMap<String, Integer> categoricalNameToIntSymbol = new HashMap<>();

	private ArrayList<String> classify(ArrayList<Record> testRecords) {
		// labels of the classified records
		ArrayList<String> listOfLabels = new ArrayList<>();
		for (Record record : testRecords) {
			ArrayList<Record> nearestNeighbors = this.nearestNeighbors(record);
			String majorityLabel = this.majorityLabel(nearestNeighbors);
			listOfLabels.add(majorityLabel);
			System.out.println("Record to classify: " + record);
			System.out.println("Nearest Neighbors");
			for(Record record2: nearestNeighbors){
				System.out.println(record2);
			}
			System.out.println("classified as (majority class): " + majorityLabel + "\n");		
		}
		return listOfLabels;
	}
	
	private void writeClassifiedLabelsToFile(String fileName, ArrayList<String> labels){
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(fileName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		StringBuffer sBuffer = new StringBuffer("");
		for(String label: labels){
			sBuffer.append(label + "\n");
		}
		sBuffer.delete(sBuffer.length() - 1, sBuffer.length());
		pw.write(sBuffer.toString());
		pw.close();
	}
	
	private ArrayList<Record> getTestRecordsFromFile(String fileName) throws Exception{
		ArrayList<Record> testRecords = new ArrayList<>();
		List<String> lines = Files.readAllLines(Paths.get(fileName),
				Charset.defaultCharset());
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			String[] comps = line.split(" ");
			double[] attrs = new double[comps.length];
			for(int colIndex = 0; colIndex < comps.length; colIndex++){
				String stringValAtColIndex = comps[colIndex];
				String typeOfAttr = attributeList.get(colIndex);
				switch (typeOfAttr) {
				case ORDINAL:
					HashMap<String, Double> levelOfOrdinalToDoubleAmount = valsForOrdinalVarAtColumn.get(colIndex);
					double dub = levelOfOrdinalToDoubleAmount.get(stringValAtColIndex);
					attrs[colIndex] = dub;
					break;
				case CONTINUOUS://will have to normalize after
					double[] rangeAtCol = rangeAtColumn.get(colIndex);
					double max = rangeAtCol[1];
					double min = rangeAtCol[0];
					double amountAtColIndex = Double.parseDouble(stringValAtColIndex);
					attrs[colIndex] = (amountAtColIndex - min) / (max - min);
					break;
				case BINARY:
					attrs[colIndex] = (double)binaryNameToIntSymbol.get(stringValAtColIndex);
					break;
				case CATEGORICAL:
					attrs[colIndex] = (double)categoricalNameToIntSymbol.get(stringValAtColIndex);
					break;
				}
			}
			Record recordToAdd = new Record(attrs, null);
			testRecords.add(recordToAdd);
		}
		return testRecords;
	}

	private double distance(Record record1, Record record2) {
		assert record1.attrList.length == record2.attrList.length;
		double[] distances = new double[record1.attrList.length];
		for (int i = 0; i < record1.attrList.length; i++) {
			String attrDataType = this.attributeList.get(i);
			switch (attrDataType) {
			case BINARY:// simple matching coefficient
				distances[i] = (int) record1.attrList[i] != (int) record2.attrList[i]
						? BINARY_MATCHING_WEIGHT : 0;
				break;
			case CATEGORICAL:
				distances[i] = (int) record1.attrList[i] != (int) record2.attrList[i]
						? CATEGORICAL_MATCHING_WEIGHT : 0;
				break;
			case ORDINAL:
				distances[i] = Math
						.abs(record1.attrList[i] - record2.attrList[i]);
				break;
			case CONTINUOUS:
				distances[i] = Math
						.abs(record1.attrList[i] - record2.attrList[i]);
				break;
			}
		}

		double sumOfSquares = 0.0;
		for (double distance : distances) {
			sumOfSquares += Math.pow(distance, 2);
		}
		return Math.sqrt(sumOfSquares);
	}
	
	private void loadDigitTrainingData(ArrayList<String> fileNames, ArrayList<String> labels){
		this.numberOfNearestNeighbors = 3;
		int fileNumber = 0;
		for(String fileName:fileNames){
			List<String> linesOfFile = null;
			try {
				linesOfFile = Files.readAllLines(Paths.get(fileName),
						Charset.defaultCharset());
			} catch (IOException e) {	e.printStackTrace();	}
			
			Record record = digitRecordFromLines(linesOfFile);
			record.label = labels.get(fileNumber);
			records.add(record);
			fileNumber++;
		}
		
		this.numberOfAttributes = records.get(0).numberOfAttributes();
		this.numberOfClasses = 4;
		this.numberOfRecords = records.size();
	}
	private Record digitRecordFromLines(List<String> lines){
		ArrayList<Double> vector = new ArrayList<>();
		for(String line: lines){
			for(int index = 0; index < line.length();index++){
				if(line.charAt(index) == '#'){
					vector.add(1.0);
				}else if(line.charAt(index) == '-'){
					vector.add(0.0);
				}
			}
		}
		double[] arr = new double[vector.size()];
		int i = 0;
		for(Double dub:vector){
			arr[i++] = (double)dub;
		}
		return new Record(arr, null);
	}

	public void loadTrainingData(String fileName) throws Exception {
		String whitespace = "[ ]+";
		List<String> lines = Files.readAllLines(Paths.get(fileName),
				Charset.defaultCharset());
		// first line
		String[] componentsOfFirstLine = lines.get(0).split(whitespace);
		
		this.numberOfRecords = Integer.parseInt(componentsOfFirstLine[0]);
		this.numberOfAttributes = Integer.parseInt(componentsOfFirstLine[1]);
		this.numberOfClasses = Integer.parseInt(componentsOfFirstLine[2]);

		// second line
		String[] componentsOfSecondLine = lines.get(1).split(whitespace);
		this.numberOfNearestNeighbors = Integer
				.parseInt(componentsOfSecondLine[0]);
		this.distanceMeasure = Integer.parseInt(componentsOfSecondLine[1]);
		this.majorityRule = Boolean.parseBoolean(componentsOfSecondLine[2]);

		// third line reading the attribute types
		String[] componentsOfThirdLine = lines.get(2).split(whitespace);
		for (String attrType : componentsOfThirdLine) {
			if (NearestNeighborClassifier.attributeTypes
					.contains(attrType) == true) {
				this.attributeList.add(attrType);
			} else {
				throw new Exception(
						"attribute in file not one of the correct attributes");
			}
		}
		//for binary variables
		int binaryVariableCounter = 0;
		//for categorical variables
		int categoricalVariableCounter = 0;
		
		// fourth line reading the ranges of the attribute types
		// uses rangeAtColumn hash map
		String[] listOfRanges = lines.get(3).split(whitespace);
		for (int colIndex = 0; colIndex < listOfRanges.length - 1; colIndex++) {
			// range symbols are low to high
			String[] strRange = listOfRanges[colIndex].split(",");
			double[] range = new double[strRange.length];
			String typeOfAttrAtIndex = this.attributeList.get(colIndex);
			switch (typeOfAttrAtIndex) {
			case ORDINAL:
				// range symbols are low to high
				int index = 0;
				for (String symbol : strRange) {
					range[index] = (double)index / (strRange.length - 1);
					if (this.valsForOrdinalVarAtColumn.containsKey(colIndex)) {
						HashMap<String, Double> map = this.valsForOrdinalVarAtColumn
								.get(colIndex);
						map.put(symbol, range[index]);
					} else {// create the hash map
						HashMap<String, Double> map = new HashMap<>();
						map.put(symbol, range[index]);
						this.valsForOrdinalVarAtColumn.put(colIndex, map);
					}
					index++;
				}
				this.rangeAtColumn.put(colIndex, range);
				break;
			case CONTINUOUS:
				range[0] = Double.parseDouble(strRange[0]);
				range[1] = Double.parseDouble(strRange[1]);
				this.rangeAtColumn.put(colIndex, range);
				break;
			case BINARY:
				for(String binaryName: strRange){
					binaryNameToIntSymbol.put(binaryName, binaryVariableCounter);
					binaryVariableCounter++;
				}
				break;// do later because first file doesn't have binary
			case CATEGORICAL:
				for(String categoricalName: strRange){
					categoricalNameToIntSymbol.put(categoricalName, categoricalVariableCounter);
					categoricalVariableCounter++;
				}
				break;
			}
		}
		// now I have to get all of the records
		for (int i = 4; i < lines.size(); i++) {
			String line = lines.get(i);
			String[] comps = line.split(whitespace);
			double[] attrs = new double[comps.length - 1];
			String label = comps[comps.length - 1];
			for(int colIndex = 0; colIndex < comps.length - 1; colIndex++){
				String stringValAtColIndex = comps[colIndex];
				String typeOfAttr = attributeList.get(colIndex);
				switch (typeOfAttr) {
				case ORDINAL:
					HashMap<String, Double> levelOfOrdinalToDoubleAmount = valsForOrdinalVarAtColumn.get(colIndex);
					double dub = levelOfOrdinalToDoubleAmount.get(stringValAtColIndex);
					attrs[colIndex] = dub;
					break;
				case CONTINUOUS://will have to normalize after
					double amountAtColIndex = Double.parseDouble(stringValAtColIndex);
					attrs[colIndex] = amountAtColIndex;
					break;
				case BINARY:
					attrs[colIndex] = (double)binaryNameToIntSymbol.get(stringValAtColIndex);
					break;
				case CATEGORICAL:
					attrs[colIndex] = (double)categoricalNameToIntSymbol.get(stringValAtColIndex);
					break;
				}
			}
			Record recordToAdd = new Record(attrs, label);
			records.add(recordToAdd);
		}
		//normalizing continuous variables in records so that they range from 0 to 1
		for(Record record: records){
			for(int index = 0; index < record.attrList.length;index++){
				if(attributeList.get(index).equals(CONTINUOUS)){
					double[] rangeAtCol = rangeAtColumn.get(index);
					double max = rangeAtCol[1];
					double min = rangeAtCol[0];
					record.attrList[index] = (record.attrList[index] - min) / (max - min);
				}
			}
		}
	}// end of loadTrainingData()

	private String majorityLabel(ArrayList<Record> records) {
		HashMap<String, Integer> labelFrequencies = new HashMap<>();
		for (Record record : records) {
			if (labelFrequencies.containsKey(record.label)) {
				labelFrequencies.put(record.label,
						labelFrequencies.get(record.label) + 1);
			} else {
				labelFrequencies.put(record.label, 1);
			}
		}

		int maxFreq = -1;
		String maxLabel = null;
		for (String labelKey : labelFrequencies.keySet()) {
			if (labelFrequencies.get(labelKey) > maxFreq) {
				maxLabel = labelKey;
			}
		}
		assert maxLabel != null;
		return maxLabel;
	}

	private ArrayList<Record> nearestNeighbors(Record inputRecord) {

		ArrayList<Record> nearestNeighbors = new ArrayList<Record>(
				this.numberOfNearestNeighbors + 1);
		ArrayList<Double> distances = new ArrayList<>(
				this.numberOfNearestNeighbors + 1);

		for (Record comparisonRecord : this.records) {
			double distance = this.distance(inputRecord, comparisonRecord);
			int index = nearestNeighbors.size() - 1;
			for (; index >= 0; index--) {
				if (index > -1 && distance > distances.get(index)) {
					break;
				}
			}
			// index will be one less than the index the record must be inserted
			// at
			// if the index is greater than the # of nearestNeighbors-1, then it
			// is not small
			// enough to be inserted (i.e. its not near enough of a neighbor)
			if (index < this.numberOfNearestNeighbors - 1) {
				nearestNeighbors.add(index + 1, comparisonRecord);
				distances.add(index + 1, distance);
				if (nearestNeighbors.size() > this.numberOfNearestNeighbors) {
					nearestNeighbors.remove(nearestNeighbors.size() - 1);
					distances.remove(distances.size() - 1);
				}
			}
			assert nearestNeighbors.size() == distances.size();
		}
		return nearestNeighbors;
	}
	
	private double calculateTrainingError(){
		int numberOfMissclassifiedRecords = 0;
		ArrayList<String> predictedLabels = classify(records);
		assert predictedLabels.size() == records.size();
		int rowIndex = 0;
		for(Record record: records){
			if(record.label.equals(predictedLabels.get(rowIndex)) == false){
				numberOfMissclassifiedRecords++;
			}
			rowIndex++;
		}
		return (double)numberOfMissclassifiedRecords / records.size();
	}
	
	private double calculateTrainingErrorWithLeaveOneOut(){
		int numberOfMissclassifiedRecords = 0;
		//ArrayList<String> predictedLabels = new ArrayList<String>();
		for(int index = 0; index < records.size();index++){
			Record recordToClassify = records.remove(index);
			ArrayList<Record> recordInList = new ArrayList<>();
			recordInList.add(recordToClassify);
			String label = classify(recordInList).get(0);
			if(recordToClassify.label.equals(label) == false){
				numberOfMissclassifiedRecords++;
			}
			records.add(0, recordToClassify);
		}
		return (double)numberOfMissclassifiedRecords / records.size();
	}

	@Override
	public String toString() {
		StringBuffer sBuffer = new StringBuffer("");
		sBuffer.append("# Of Attributes: " + this.numberOfAttributes + "\n");
		sBuffer.append("# Of Classes: " + this.numberOfClasses + "\n");
		sBuffer.append("# Of Records: " + this.numberOfRecords + "\n");
		// can add # of nearest neighbors, etc
		for (Record record : this.records) {
			sBuffer.append(record.toString() + "\n");
		}
		sBuffer.deleteCharAt(sBuffer.length() - 1);
		return sBuffer.toString();
	}

}
