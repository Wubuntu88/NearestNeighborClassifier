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

public class NearestNeighborClassifier {

	public static final String BINARY = "binary";
	public static final String CATEGORICAL = "categorical";
	public static final String ORDINAL = "ordinal";
	public static final String CONTINUOUS = "continuous";
	public static final String LABEL = "label";
	private static final TreeSet<String> attributeTypes = new TreeSet<String>(
			Arrays.asList(BINARY, CATEGORICAL, ORDINAL, CONTINUOUS, LABEL));

	private final double CATEGORICAL_MATCHING_WEIGHT = 0.4;
	private final double BINARY_MATCHING_WEIGHT = 0.4;

	private int numberOfRecords;
	private int numberOfAttributes;

	private int numberOfClasses;
	private int numberOfNearestNeighbors;
	private int distanceMeasure;

	private boolean majorityRule = true;// weighted majority: false

	// list of the type of the variables in records (ordinal, continuous, etc)
	private ArrayList<String> attributeList = new ArrayList<>();

	private ArrayList<Record> records = new ArrayList<>();

	// for continuous variables (key is column, value is range (array of len 2)
	private HashMap<Integer, double[]> rangeAtColumn = new HashMap<>();

	// for ordinal variables
	private HashMap<Integer, HashMap<String, Double>> valsForOrdinalVarAtColumn = new HashMap<>();

	// for binary variables
	private HashMap<String, Integer> binaryNameToIntSymbol = new HashMap<>();

	// for categorical variables
	private HashMap<String, Integer> categoricalNameToIntSymbol = new HashMap<>();

	public double calculateTrainingError() {
		int numberOfMissclassifiedRecords = 0;
		ArrayList<String> predictedLabels = this.classify(this.records);
		assert predictedLabels.size() == this.records.size();
		int rowIndex = 0;
		for (Record record : this.records) {
			if (record.label.equals(predictedLabels.get(rowIndex)) == false) {
				numberOfMissclassifiedRecords++;
			}
			rowIndex++;
		}
		return (double) numberOfMissclassifiedRecords / this.records.size();
	}

	public double calculateTrainingErrorWithLeaveOneOut() {
		int numberOfMissclassifiedRecords = 0;
		// ArrayList<String> predictedLabels = new ArrayList<String>();
		for (int index = 0; index < this.records.size(); index++) {
			Record recordToClassify = this.records.remove(index);
			ArrayList<Record> recordInList = new ArrayList<>();
			recordInList.add(recordToClassify);
			String label = this.classify(recordInList).get(0);
			if (recordToClassify.label.equals(label) == false) {
				numberOfMissclassifiedRecords++;
			}
			this.records.add(0, recordToClassify);
		}
		return (double) numberOfMissclassifiedRecords / this.records.size();
	}

	public ArrayList<String> classify(ArrayList<Record> testRecords) {
		// labels of the classified records
		ArrayList<String> listOfLabels = new ArrayList<>();
		for (Record testRecord : testRecords) {
			ArrayList<Record> nearestNeighbors = this
					.nearestNeighbors(testRecord);
			String majorityLabel;
			if (this.majorityRule) {
				majorityLabel = this.majorityLabel(nearestNeighbors);
			} else {
				ArrayList<Double> distances = new ArrayList<>();
				for (Record theRecord : nearestNeighbors) {
					distances.add(this.distance(testRecord, theRecord));
				}
				majorityLabel = this.majorityLabelWeighted(testRecords,
						distances);
			}
			listOfLabels.add(majorityLabel);
			System.out.println("Record to classify: " + testRecord);
			System.out.println("Nearest Neighbors");
			for (Record record2 : nearestNeighbors) {
				System.out.println(record2);
			}
			System.out.println(
					"classified as (majority class): " + majorityLabel + "\n");
		}
		return listOfLabels;
	}

	public Record digitRecordFromLines(List<String> lines) {
		ArrayList<Double> vector = new ArrayList<>();
		for (String line : lines) {
			for (int index = 0; index < line.length(); index++) {
				if (line.charAt(index) == '1') {
					vector.add(1.0);
				} else if (line.charAt(index) == '0') {
					vector.add(0.0);
				}
			}
		}
		double[] arr = new double[vector.size()];
		int i = 0;
		for (Double dub : vector) {
			arr[i++] = dub;
		}
		return new Record(arr, null);
	}

	private double distance(Record record1, Record record2) {
		assert record1.attrList.length == record2.attrList.length;
		double[] distances = new double[record1.attrList.length];
		for (int i = 0; i < record1.attrList.length; i++) {
			String attrDataType = this.attributeList.get(i);
			switch (attrDataType) {
			case BINARY:// simple matching coefficient
				distances[i] = (int) record1.attrList[i] != (int) record2.attrList[i]
						? this.BINARY_MATCHING_WEIGHT : 0;
				break;
			case CATEGORICAL:
				distances[i] = (int) record1.attrList[i] != (int) record2.attrList[i]
						? this.CATEGORICAL_MATCHING_WEIGHT : 0;
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

	public ArrayList<String> getAttributeList() {
		return this.attributeList;
	}

	public int getNumberOfNearestNeighbors() {
		return this.numberOfNearestNeighbors;
	}

	public ArrayList<Record> getRecords() {
		return this.records;
	}

	public ArrayList<Record> getTestRecordsFromFile(String fileName)
			throws Exception {
		ArrayList<Record> testRecords = new ArrayList<>();
		List<String> lines = Files.readAllLines(Paths.get(fileName),
				Charset.defaultCharset());
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			String[] comps = line.split(" ");
			double[] attrs = new double[comps.length];
			for (int colIndex = 0; colIndex < comps.length; colIndex++) {
				String stringValAtColIndex = comps[colIndex];
				String typeOfAttr = this.attributeList.get(colIndex);
				switch (typeOfAttr) {
				case ORDINAL:
					HashMap<String, Double> levelOfOrdinalToDoubleAmount = this.valsForOrdinalVarAtColumn
							.get(colIndex);
					double dub = levelOfOrdinalToDoubleAmount
							.get(stringValAtColIndex);
					attrs[colIndex] = dub;
					break;
				case CONTINUOUS:// will have to normalize after
					double[] rangeAtCol = this.rangeAtColumn.get(colIndex);
					double max = rangeAtCol[1];
					double min = rangeAtCol[0];
					double amountAtColIndex = Double
							.parseDouble(stringValAtColIndex);
					attrs[colIndex] = (amountAtColIndex - min) / (max - min);
					break;
				case BINARY:
					attrs[colIndex] = this.binaryNameToIntSymbol
							.get(stringValAtColIndex);
					break;
				case CATEGORICAL:
					attrs[colIndex] = this.categoricalNameToIntSymbol
							.get(stringValAtColIndex);
					break;
				}
			}
			Record recordToAdd = new Record(attrs, null);
			testRecords.add(recordToAdd);
		}
		return testRecords;
	}

	public boolean isMajorityRule() {
		return this.majorityRule;
	}

	public void loadDigitTrainingData(ArrayList<String> fileNames,
			ArrayList<String> labels) {
		this.numberOfNearestNeighbors = 3;
		int fileNumber = 0;
		for (String fileName : fileNames) {
			List<String> linesOfFile = null;
			try {
				linesOfFile = Files.readAllLines(Paths.get(fileName),
						Charset.defaultCharset());
			} catch (IOException e) {
				e.printStackTrace();
			}

			Record record = this.digitRecordFromLines(linesOfFile);
			record.label = labels.get(fileNumber);
			this.records.add(record);
			fileNumber++;
		}

		this.numberOfAttributes = this.records.get(0).numberOfAttributes();
		this.numberOfClasses = 4;
		this.numberOfRecords = this.records.size();
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
		// for binary variables
		int binaryVariableCounter = 0;
		// for categorical variables
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
					range[index] = (double) index / (strRange.length - 1);
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
				for (String binaryName : strRange) {
					this.binaryNameToIntSymbol.put(binaryName,
							binaryVariableCounter);
					binaryVariableCounter++;
				}
				break;// do later because first file doesn't have binary
			case CATEGORICAL:
				for (String categoricalName : strRange) {
					this.categoricalNameToIntSymbol.put(categoricalName,
							categoricalVariableCounter);
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
			for (int colIndex = 0; colIndex < comps.length - 1; colIndex++) {
				String stringValAtColIndex = comps[colIndex];
				String typeOfAttr = this.attributeList.get(colIndex);
				switch (typeOfAttr) {
				case ORDINAL:
					HashMap<String, Double> levelOfOrdinalToDoubleAmount = this.valsForOrdinalVarAtColumn
							.get(colIndex);
					double dub = levelOfOrdinalToDoubleAmount
							.get(stringValAtColIndex);
					attrs[colIndex] = dub;
					break;
				case CONTINUOUS:// will have to normalize after
					double amountAtColIndex = Double
							.parseDouble(stringValAtColIndex);
					attrs[colIndex] = amountAtColIndex;
					break;
				case BINARY:
					attrs[colIndex] = this.binaryNameToIntSymbol
							.get(stringValAtColIndex);
					break;
				case CATEGORICAL:
					attrs[colIndex] = this.categoricalNameToIntSymbol
							.get(stringValAtColIndex);
					break;
				}
			}
			Record recordToAdd = new Record(attrs, label);
			this.records.add(recordToAdd);
		}
		// normalizing continuous variables in records so that they range from 0
		// to 1
		for (Record record : this.records) {
			for (int index = 0; index < record.attrList.length; index++) {
				if (this.attributeList.get(index).equals(CONTINUOUS)) {
					double[] rangeAtCol = this.rangeAtColumn.get(index);
					double max = rangeAtCol[1];
					double min = rangeAtCol[0];
					record.attrList[index] = (record.attrList[index] - min)
							/ (max - min);
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

		double maxFreq = -1;
		String maxLabel = null;
		for (String labelKey : labelFrequencies.keySet()) {
			if (labelFrequencies.get(labelKey) > maxFreq) {
				maxFreq = labelFrequencies.get(labelKey);
				maxLabel = labelKey;
			}
		}
		assert maxLabel != null;
		return maxLabel;
	}

	private String majorityLabelWeighted(ArrayList<Record> records,
			ArrayList<Double> distances) {
		HashMap<String, Double> labelWeights = new HashMap<>();
		int index = 0;
		for (Record record : records) {
			Double theDistance = distances.get(index);
			if (labelWeights.containsKey(record.label)) {
				labelWeights.put(record.label,
						labelWeights.get(record.label) + 1.0 / theDistance);
			} else {
				labelWeights.put(record.label, 1.0 / theDistance);
			}
		}

		double maxWeight = Double.MIN_VALUE;
		String maxLabel = null;
		for (String labelKey : labelWeights.keySet()) {
			if (labelWeights.get(labelKey) > maxWeight) {
				maxWeight = labelWeights.get(labelKey);
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

	public void setMajorityRule(boolean majorityRule) {
		this.majorityRule = majorityRule;
	}

	public void setNumberOfNearestNeighbors(int numberOfNearestNeighbors) {
		this.numberOfNearestNeighbors = numberOfNearestNeighbors;
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

	public void writeClassifiedLabelsToFile(String fileName,
			ArrayList<String> labels) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(fileName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		StringBuffer sBuffer = new StringBuffer("");
		for (String label : labels) {
			sBuffer.append(label + "\n");
		}
		sBuffer.delete(sBuffer.length() - 1, sBuffer.length());
		pw.write(sBuffer.toString());
		pw.close();
	}

}
