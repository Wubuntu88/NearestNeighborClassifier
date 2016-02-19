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
		int label;

		public Record(double[] attrList, int label) {
			this.attrList = attrList;
			this.label = label;
		}
	}

	public static final String BINARY = "binary";
	public static final String NOMINAL = "nominal";
	public static final String ORDINAL = "ordinal";
	public static final String CONTINUOUS = "continuous";
	private static final TreeSet<String> attributeTypes = new TreeSet<String>(
			Arrays.asList(BINARY, NOMINAL, ORDINAL, CONTINUOUS));

	public static void main(String[] args) {
		NearestNeighborClassifier nnc = new NearestNeighborClassifier();
		try {
			nnc.loadTrainingData("train3_use");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	private int numberOfRecords;
	private int numberOfAttributes;

	private int numberOfClasses;
	private int numberOfNearestNeighbors;
	private int distanceMeasure;// ?? is this the right type ??

	private boolean majorityRule;

	// list of the type of the variables in the records
	private ArrayList<String> attributeList = new ArrayList<>();

	private ArrayList<Record> records = new ArrayList<>();

	// for continuous variables (key is column, value is range (array of len 2)
	private HashMap<Integer, double[]> rangeAtColumn = new HashMap<>();
	private HashMap<Integer, HashMap<String, Double>> valsForOrdinalVarAtColumn = new HashMap<>();

	private ArrayList<Integer> classify(ArrayList<Record> testRecords) {
		// labels of the classified records
		ArrayList<Integer> listOfLabels = new ArrayList<>();
		for (Record record : testRecords) {
			ArrayList<Record> nearestNeighbors = this.nearestNeighbors(record);
			int majorityLabel = this.majorityLabel(nearestNeighbors);
			listOfLabels.add(majorityLabel);
		}
		return listOfLabels;
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
			case NOMINAL:
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

		// fourth line reading the ranges of the attribute types
		// uses rangeAtColumn hash map
		String[] listOfRanges = lines.get(3).split(" ");
		for (int colIndex = 0; colIndex < listOfRanges.length; colIndex++) {
			String typeOfAttrAtIndex = this.attributeList.get(colIndex);
			switch (typeOfAttrAtIndex) {
			case ORDINAL:
				// range symbols are low to high
				String[] strRange = listOfRanges[colIndex].split(",");
				double[] range = new double[strRange.length];
				int index = 0;
				for (String symbol : strRange) {
					range[index] = index / (strRange.length - 1);
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
			case CONTINUOUS:
				// TODO
				break;
			case BINARY:
				break;// no range for binary
			case NOMINAL:
				break;// no range for nominals

			}
		}
	}

	private int majorityLabel(ArrayList<Record> records) {
		HashMap<Integer, Integer> labelFrequencies = new HashMap<>();
		for (Record record : records) {
			if (labelFrequencies.containsKey(record.label)) {
				labelFrequencies.put(record.label,
						labelFrequencies.get(record.label) + 1);// increment by
																// 1
			} else {
				labelFrequencies.put(record.label, 1);
			}
		}

		int maxFreq = -1;
		int maxLabel = -1;
		for (Integer labelKey : labelFrequencies.keySet()) {
			if (labelFrequencies.get(labelKey) > maxFreq) {
				maxLabel = labelKey;
			}
		}
		assert maxLabel != -1;
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

}
