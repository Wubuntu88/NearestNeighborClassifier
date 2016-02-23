import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

public class NearestNeighborClassifier {

	private class Record {
		double[] attrList;
		String label;

		public Record(double[] attrList, String label) {
			this.attrList = attrList;
			this.label = label;
		}

		@Override
		public String toString() {
			StringBuffer sBuffer = new StringBuffer("");
			for (double dub : this.attrList) {
				sBuffer.append(dub + ", ");
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

	public static void main(String[] args) {
		NearestNeighborClassifier nnc = new NearestNeighborClassifier();
		try {
			nnc.loadTrainingData("train3_use");
		} catch (Exception e) {
			System.out.println("error");
			System.out.println(e.getMessage());
		}
		System.out.println(nnc.toString());
	}

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
		}
		return listOfLabels;
	}

	private Record convertLineToRecord(String[] components) {
		double[] currentRecordsAttributes = new double[components.length - 1];
		int label = 0; // have to do something with the label
		for (int columnIndex = 0; columnIndex < components.length
				- 1; columnIndex++) {
			String attributeDataType = this.attributeList.get(columnIndex);
			switch (attributeDataType) {
			case ORDINAL:
				HashMap<String, Double> map = this.valsForOrdinalVarAtColumn
						.get(columnIndex);
				double dub = map.get(components[columnIndex]);
				currentRecordsAttributes[columnIndex] = dub;
				break;
			case CONTINUOUS:
				//may need to normalize
				currentRecordsAttributes[columnIndex] = Double.parseDouble(components[columnIndex]);
				break;
			case BINARY:// simple matching coefficient
				System.out.println("no binary conversion");
				break;
			case CATEGORICAL:
				System.out.println("no no categorical conversion");
				break;
			}
			columnIndex++;
		}
		return null;
	}

	private double distance(Record record1, Record record2) {
		assert record1.attrList.length == record2.attrList.length;
		double[] distances = new double[record1.attrList.length];
		for (int i = 0; i < record1.attrList.length; i++) {
			String attrDataType = this.attributeList.get(i);
			switch (attrDataType) {
			case BINARY:// simple matching coefficient
				distances[i] = (int) record1.attrList[i] != (int) record2.attrList[i]
						? 1 : 0;
				break;
			case CATEGORICAL:
				distances[i] = (int) record1.attrList[i] != (int) record2.attrList[i]
						? 1 : 0;
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

	public void loadTrainingData(String fileName) throws Exception {

		List<String> lines = Files.readAllLines(Paths.get(fileName),
				Charset.defaultCharset());
		for (String line : lines) {
			// System.out.println(line);
		}
		// first line
		String[] componentsOfFirstLine = lines.get(0).split(" ");
		this.numberOfRecords = Integer.parseInt(componentsOfFirstLine[0]);
		this.numberOfAttributes = Integer.parseInt(componentsOfFirstLine[1]);
		this.numberOfClasses = Integer.parseInt(componentsOfFirstLine[2]);

		// second line
		String[] componentsOfSecondLine = lines.get(1).split(" ");
		this.numberOfNearestNeighbors = Integer
				.parseInt(componentsOfSecondLine[0]);
		this.distanceMeasure = Integer.parseInt(componentsOfSecondLine[1]);
		this.majorityRule = Boolean.parseBoolean(componentsOfSecondLine[2]);

		// third line reading the attribute types
		String[] componentsOfThirdLine = lines.get(2).split(" ");
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
		String[] listOfRanges = lines.get(3).split(" ");
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
					binaryNameToIntSymbol.put(categoricalName, categoricalVariableCounter);
					categoricalVariableCounter++;
				}
				break;
			}
		}
		// now I have to get all of the records
		for (int i = 4; i < lines.size(); i++) {
			String line = lines.get(i);
			String[] comps = line.split(" ");
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
					break;
				case CATEGORICAL:
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
