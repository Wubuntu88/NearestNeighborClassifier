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

		NearestNeighborClassifier nnc = new NearestNeighborClassifier(); // start over with new nearest
												// neighbor classifier

		String[] theFileNames = new String[] { "0_1", "0_2", "0_3", "0_4",
				"0_5", "1_1", "1_2", "1_3", "1_4", "1_5", "2_1", "2_2", "2_3",
				"2_4", "2_5", "3_1", "3_2", "3_3", "3_4", "3_5" };
		ArrayList<String> fileNames = new ArrayList<>(
				Arrays.asList(theFileNames));
		ArrayList<String> labels = new ArrayList<>();
		for (String nameOfDigit : fileNames) {
			labels.add(nameOfDigit.substring(0, 1));
		}
		nnc.loadDigitTrainingData(fileNames, labels);
		for (int index = 0; index < nnc.getRecords().get(0).numberOfAttributes(); index++) {
			nnc.getAttributeList().add(NearestNeighborClassifier.BINARY);
		}

		String[] theTestFileNames = new String[] { "0_test", "1_test",
				"2_test", "3_test" };
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
			record.label = labels.get(fileNumber);
			testRecords.add(record);
			fileNumber++;
		}

		nnc.classify(testRecords);
	}
}
