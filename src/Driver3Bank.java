import java.util.ArrayList;

public class Driver3Bank {
	public static void main(String[] args) {
		/*
		 * #2 nearest Neighbor on scores
		 */
		NearestNeighborClassifier nnc = new NearestNeighborClassifier();

		/*
		 * #3 bank loan risk classification
		 */
		String bankTranindDataFileName = "train4";
		String bankTestDataFileName = "test4";
		String bankClassifiedRecordsOutputFileName = "output4";
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
			nnc.writeClassifiedLabelsToFile(
					bankClassifiedRecordsOutputFileName, listOfLabels);
		} catch (Exception e) {
			e.printStackTrace();
		}

		/* B: training error */
		double trainingError = nnc.calculateTrainingError();
		System.out.println("training error: " + trainingError);
		/* C: one out validation error */
		double trainErrorOneOut = nnc.calculateTrainingErrorWithLeaveOneOut();
		System.out.println("One out validation error: " + trainErrorOneOut);
	}
}
