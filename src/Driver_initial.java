import java.util.ArrayList;

public class Driver_initial {
	/*
	 * Driver that performs the task of part 2 of the nearest neighbor classifier
	 */
	public static void main(String[] args) {
		NearestNeighborClassifier nnc = new NearestNeighborClassifier();
		String bankTranindDataFileName = "train3";
		String bankTestDataFileName = "test3";
		String bankClassifiedRecordsOutputFileName = "output3";
		nnc = new NearestNeighborClassifier();
		try {
			nnc.loadTrainingData(bankTranindDataFileName); // bank training data
		} catch (Exception e) {
			e.printStackTrace();
		}

		/* A: classification of test records */
		ArrayList<Record> BankTestRecords = null;
		try {
			BankTestRecords = nnc.getTestRecordsFromFile(bankTestDataFileName);
			ArrayList<String> listOfLabels = nnc.classify(BankTestRecords);
			nnc.writeClassifiedLabelsToFile(bankClassifiedRecordsOutputFileName,
					listOfLabels);
		} catch (Exception e) {
			e.printStackTrace();
		}

		nnc.setNumberOfNearestNeighbors(3);
		// nnc.setNumberOfNearestNeighbors(10);
		// nnc.setNumberOfNearestNeighbors(20);
		nnc.setMajorityRule(true);// false is weighted majority
		/* C: training error */
		double trainingError = nnc.calculateTrainingError();
		System.out.println("training error: " + trainingError);
		/* D: one out validation error */
		double trainErrorOneOut = nnc.calculateTrainingErrorWithLeaveOneOut();
		System.out.println("One out validation error: " + trainErrorOneOut);
	}

}
