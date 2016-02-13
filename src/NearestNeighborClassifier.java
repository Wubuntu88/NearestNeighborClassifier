import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

public class NearestNeighborClassifier {
	
	public class Record {
		double[] attrList;
		int label;
		public Record(double[] attrList, int label){
			this.attrList = attrList;
			this.label = label;
		}
	}
	public static final String BINARY = "binary";
	public static final String NOMINAL = "nominal";
	public static final String ORDINAL = "ordinal";
	public static final String CONTINUOUS = "continuous";
	private static final TreeSet<String> attributeTypes = 
			new TreeSet<String>((List<String>)Arrays.asList(BINARY, NOMINAL, ORDINAL, CONTINUOUS));
	
	private int numberOfRecords;
	private int numberOfAttributes;
	private int numberOfClasses;
	
	private int numberOfNearestNeighbors;
	private int distanceMeasure;//?? is this the right type ??
	private boolean majorityRule;
	
	private ArrayList<String> attributeList = new ArrayList<>();
	private ArrayList<Record> records = new ArrayList<>();
	

	public NearestNeighborClassifier(){
		
	}
	
	private void loadTrainingData(String fileName){
		try{
			List<String> lines = Files.readAllLines(Paths.get(fileName), Charset.defaultCharset());
			String[] componentsOfFirstLine = lines.get(0).split(" ");
			this.numberOfRecords = Integer.parseInt(componentsOfFirstLine[0]);
			this.numberOfAttributes = Integer.parseInt(componentsOfFirstLine[1]);
			this.numberOfClasses = Integer.parseInt(componentsOfFirstLine[2]);
			
			String[] componentsOfSecondLine = lines.get(1).split(" ");
			this.numberOfNearestNeighbors = Integer.parseInt(componentsOfSecondLine[0]);
			this.distanceMeasure = Integer.parseInt(componentsOfSecondLine[1]);
			this.majorityRule = Boolean.parseBoolean(componentsOfSecondLine[2]);
			
			
		}catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	private ArrayList<Integer> classify(ArrayList<Record> testRecords){
		ArrayList<Integer> listOfLabels = new ArrayList<>();//labels of the classified records
		for(Record record: testRecords){
			ArrayList<Record> nearestNeighbors = nearestNeighbors(record);
			int majorityLabel = majorityLabel(nearestNeighbors);
			listOfLabels.add(majorityLabel);
		}
		return listOfLabels;
	}
	
	private ArrayList<Record> nearestNeighbors(Record inputRecord){
		
		ArrayList<Record> nearestNeighbors = new ArrayList<Record>(this.numberOfNearestNeighbors + 1);
		ArrayList<Double> distances = new ArrayList<>(this.numberOfNearestNeighbors + 1);
		
		for(Record comparisonRecord: this.records){
			double distance = this.distance(inputRecord, comparisonRecord);
			int index = nearestNeighbors.size() - 1;
			for(; index >= 0; index--){
				if(index > -1 && distance > distances.get(index)){
					break;
				}
			}
			//index will be one less than the index the record must be inserted at
			//if the index is greater than the # of nearestNeighbors-1, then it is not small
			//enough to be inserted (i.e. its not near enough of a neighbor)
			if(index < this.numberOfNearestNeighbors - 1){
				nearestNeighbors.add(index + 1, comparisonRecord);
				distances.add(index + 1, distance);
				if (nearestNeighbors.size() > this.numberOfNearestNeighbors){
					nearestNeighbors.remove(nearestNeighbors.size() - 1);
					distances.remove(distances.size() - 1);
				}
			}
			assert nearestNeighbors.size() == distances.size();
		}
		return nearestNeighbors;
	}
	private double distance(Record record1, Record record2){
		assert record1.attrList.length == record2.attrList.length;
		double[] distances = new double[record1.attrList.length];
		for(int i = 0; i < record1.attrList.length; i++){
			String attrDataType = attributeList.get(i);
			switch (attrDataType) {
				case BINARY://simple matching coefficient
					distances[i] = (int)record1.attrList[i] != (int)record2.attrList[i] ? 1 : 0;
				case NOMINAL:
					distances[i] = (int)record1.attrList[i] != (int)record2.attrList[i] ? 1 : 0;
				case ORDINAL:
					distances[i] = Math.abs(record1.attrList[i] - record2.attrList[i]);
				case CONTINUOUS:
					distances[i] = Math.abs(record1.attrList[i] - record2.attrList[i]);
			}
		}
		
		double sumOfSquares = 0.0;
		for(double distance : distances){
			sumOfSquares += Math.pow(distance, 2);
		}
		return Math.sqrt(sumOfSquares);
	}
	
	private int majorityLabel(ArrayList<Record> records){
		HashMap<Integer, Integer> labelFrequencies = new HashMap<>();
		for(Record record: records){
			if(labelFrequencies.containsKey(record.label)){
				labelFrequencies.put(record.label, labelFrequencies.get(record.label) + 1);//increment by 1
			}else{
				labelFrequencies.put(record.label, 1);
			}
		}
		
		int maxFreq = -1;
		int maxLabel = -1;
		for(Integer labelKey: labelFrequencies.keySet()){
			if(labelFrequencies.get(labelKey) > maxFreq){
				maxLabel = labelKey;
			}
		}
		assert maxLabel != -1;
		return maxLabel;
	}
	
	
	/*
	public static void main(String[] args) {
	}
	*/
}















