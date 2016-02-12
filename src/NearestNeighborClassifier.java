import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class NearestNeighborClassifier {
	
	public class Record {
		double[] attrList;
		int [] labels;
		public Record(double[] attrList, int[] labels){
			this.attrList = attrList;
			this.labels = labels;
		}
	}
	public static final String BINARY = "binary";
	public static final String NOMINAL = "nominal";
	public static final String ORDINAL = "ordinal";
	public static final String CONTINUOUS = "continuous";
	
	private int numberOfRecords;
	private int numberOfAttributes;
	private int numberOfClasses;
	
	private int numberOfNearestNeighbors;
	private int distanceMeasure;//?? is this the right type ??
	private boolean majorityRule;
	
	private ArrayList<String> typesOfAttributes = new ArrayList<>();
	private ArrayList<Record> records = new ArrayList<>();
	

	public NearestNeighborClassifier(){
		
	}
	
	public void loadTrainingData(){
		String fileName = "data.txt";
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
	
	private Record[] nearestNeighbors(Record inputRecord){
		int nElems = this.numberOfNearestNeighbors;
		ArrayList<Record> nearestNeighbors = new ArrayList<Record>(nElems + 1);
		ArrayList<Double> distances = new ArrayList<>(nElems + 1);
		
		for(Record comparisonRecord: this.records){
			double distance = this.distance(inputRecord, comparisonRecord);
			for(int i = nearestNeighbors.size(); i <= 0; i--){
				if(distance > distances.get(i)){
					break;
				}
				//need to continue here
			}
			//need to continue here
			nearestNeighbors.add(comparisonRecord);
			distances.add(distance);
			
			
		}
		
		return null;
	}
	private double distance(Record record1, Record record2){
		return 10.0;
	}
	/*
	public static void main(String[] args) {
	}
	*/
}















