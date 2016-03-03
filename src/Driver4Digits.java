import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Driver4Digits {
	public static void main(String[] args) {
		/*
		 * **Part 4: classification of digits
		 */

		NearestNeighborClassifier nnc = new NearestNeighborClassifier(); // start
																			// over
																			// with
																			// new
																			// nearest
		// neighbor classifier

		String[] theFileNames = new String[] { "0_1", "0_2", "0_3", "0_4",
				"0_5", "1_1", "1_2", "1_3", "1_4", "1_5", "2_1", "2_2", "2_3",
				"2_4", "2_5", "3_1", "3_2", "3_3", "3_4", "3_5", "4_1", "4_2",
				"4_3", "4_4", "4_5", "5_1", "5_2", "5_3", "5_4", "5_5", "6_1",
				"6_2", "6_3", "6_4", "6_5", "7_1", "7_2", "7_3", "7_4", "7_5",
				"8_1", "8_2", "8_3", "8_4", "8_5", "9_1", "9_2", "9_3", "9_4",
				"9_5" };
		ArrayList<String> fileNames = new ArrayList<>(
				Arrays.asList(theFileNames));
		ArrayList<String> labels = new ArrayList<>();
		for (String nameOfDigit : fileNames) {
			labels.add(nameOfDigit.substring(0, 1));
		}
		nnc.loadDigitTrainingData(fileNames, labels);
		for (int index = 0; index < nnc.getRecords().get(0)
				.numberOfAttributes(); index++) {
			nnc.getAttributeList().add(NearestNeighborClassifier.BINARY);
		}

		// to add a test file of your own, add a string of the following form
		// where the initial character is the label of the digit
		String[] theTestFileNames = new String[] { "0_test", "1_test", "2_test",
				"3_test", "4_test", "5_test", "6_test", "7_test", "8_test",
				"9_test" };
		ArrayList<String> testFileNames = new ArrayList<>(
				Arrays.asList(theTestFileNames));
		ArrayList<String> testLabels = new ArrayList<>();
		for (String theTestFileName : testFileNames) {
			testLabels.add(theTestFileName.substring(0, 1));
		}
		ArrayList<Record> testRecords = new ArrayList<>();
		int fileNumber = 0;
		for (String fileName : testFileNames) {
			List<String> linesOfFile = null;
			try {
				linesOfFile = Files.readAllLines(Paths.get(fileName),
						Charset.defaultCharset());
			} catch (IOException e) {
				e.printStackTrace();
			}

			Record record = nnc.digitRecordFromLines(linesOfFile);
			record.label = testLabels.get(fileNumber);
			testRecords.add(record);
			fileNumber++;
		}

		nnc.classify(testRecords);
	}
}
