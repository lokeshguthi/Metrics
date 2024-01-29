package de.tukl.softech.exclaim.dao;

import de.tukl.softech.exclaim.data.SimilarityScore;
import de.tukl.softech.exclaim.data.Upload;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;
import org.springframework.web.util.UriUtils;


@Component
public class SimilarityCheckerDao {

    private final UploadsDao uploadsDao;

    public SimilarityCheckerDao(UploadsDao uploadsDao) {
        this.uploadsDao = uploadsDao;
    }

    private static class ScoreComparer implements Comparator<String> {
        //comparator to sort list of scores hidden in Strings

        @Override
        public int compare(String o1, String o2) {
            String firstScore = o1.split(" ")[2];
            String secondScore = o2.split(" ")[2];
            Double scoreOne = Double.parseDouble(firstScore);
            Double scoreTwo = Double.parseDouble(secondScore);
            //return negative so this sorts descending
            return -scoreOne.compareTo(scoreTwo);
        }
    }


    //gets all the file links for one exercise
    public void makePathsToUploads(String exercise, String sheet) {

        //clear previous paths file if it existed
        String sheetDirectory = "similarityChecker/files/" + exercise + "/" + sheet + "/paths/";
        File directory = new File(sheetDirectory);
        if (directory.exists()) {
            File[] files = directory.listFiles();
            for (File file : files) {
                file.delete();
            }
        }

        List<Upload> uploads = uploadsDao.getUploadsForSheet(exercise, sheet);

        //only add uploads that weren't deleted
        for (Upload upload : uploads) {
            if (upload.getDeleteDate() == null) {
                writeToFile(exercise, sheet, upload.getAssignment(), upload.getPath());
            }
        }

    }

    //writes path to correct file in similarityChecker filestructure
    private static void writeToFile(String exercise, String sheet, String assignment, String path){
        String sheetDirectory = "similarityChecker/files/" + exercise + "/" + sheet + "/paths/";
        String fileName = assignment + "_Paths.txt";

        File directory = new File(sheetDirectory);
        if (! directory.exists()){
            directory.mkdirs();
        }

        File file = new File(sheetDirectory + "/" + fileName);

        try (FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
             BufferedWriter bw = new BufferedWriter(fw);) {

            bw.write(path + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void runCheck(ArrayList<String> argumentsForChecker, String exercise, String sheet, String pathsFile) {

        Runtime rt = Runtime.getRuntime();
        String command = "./similarityChecker/SimilarityChecker";

        //run the exe if running on windows
        if (SystemUtils.IS_OS_WINDOWS) {
            command = "similarityChecker/SimilarityChecker.exe";
        }

        argumentsForChecker.add(0, command);

        final Process proc;

        try {
            proc = rt.exec(argumentsForChecker.toArray(new String[0]));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(proc.getInputStream()));


        //write logfiles for errors
        String sheetDirectory = "similarityChecker/files/" + exercise + "/" + sheet + "/logs/";
        String fileName = pathsFile.split("_")[0] + "_log.txt";

        File directory = new File(sheetDirectory);
        if (!directory.exists()){
            directory.mkdirs();
        }

        File file = new File(sheetDirectory + "/" + fileName);


        File finalFile = file;
        new Thread(() -> {
            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(proc.getErrorStream()));

            try (FileWriter fw = new FileWriter(finalFile.getAbsoluteFile(), false);
                 BufferedWriter bw = new BufferedWriter(fw)) {
                // Read the output from the command line and write to file
                bw.write("Error log: \n");
                String s;
                while ((s = stdError.readLine()) != null) {
                    bw.write(s + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();


        //write results to file
        sheetDirectory = "similarityChecker/files/" + exercise + "/" + sheet + "/checks/";
        fileName = pathsFile.split("_")[0] + "_Results.txt";

        directory = new File(sheetDirectory);
        if (! directory.exists()){
            directory.mkdirs();
        }

        file = new File(sheetDirectory + "/" + fileName);

        try (FileWriter fw = new FileWriter(file.getAbsoluteFile(), false);
             BufferedWriter bw = new BufferedWriter(fw)) {
            ArrayList<String> scores = new ArrayList<>();
            // Read the output from the command line and write to file
            String s;
            while ((s = stdInput.readLine()) != null) {
                String[] splitOutput = s.split(" ");
                String pathOne = argumentsForChecker.get(Integer.parseInt(splitOutput[0]) + 1); //+1 because argument 0 is the checker
                String pathTwo = argumentsForChecker.get(Integer.parseInt(splitOutput[1]) + 1);
                pathOne = pathOne.replace("\\", "/");
                pathTwo = pathTwo.replace("\\", "/");

                String[] splitPathOne = pathOne.split("/");
                String fileNameOne = splitPathOne[splitPathOne.length - 1];
                String[] splitPathTwo = pathTwo.split("/");
                String fileNameTwo = splitPathTwo[splitPathOne.length - 1];

                String groupTeamOne = splitPathOne[splitPathOne.length - 3];
                groupTeamOne = groupTeamOne.replace("|", "-");

                String groupTeamTwo = splitPathTwo[splitPathTwo.length - 3];
                groupTeamTwo = groupTeamTwo.replace("|", "-");

                String fileOneUrl = UriUtils.encodePathSegment(fileNameOne, StandardCharsets.UTF_8.toString());
                String fileTwoUrl = UriUtils.encodePathSegment(fileNameTwo, StandardCharsets.UTF_8.toString());


                double scoreAsDouble;
                scoreAsDouble = Double.parseDouble(splitOutput[2]) * 100;

                scores.add(groupTeamOne + " " + groupTeamTwo + " " + scoreAsDouble + " " + fileOneUrl + " " + fileTwoUrl + "\n");
            }

            proc.waitFor();

            ScoreComparer sc = new ScoreComparer();
            scores.sort(sc);


            for (String score : scores) {
                bw.write(score);
            }
        } catch (IOException | InterruptedException e){
            e.printStackTrace();
        }
    }

    //returns a list with an array for every assignment in the exercise and sheet, only works if the test has run already
    public static ArrayList<SimilarityScore[]> getScores(String exercise, String sheet) {
        ArrayList<SimilarityScore[]> list = new ArrayList<>();

        //loop through all of these and check them
        File directoryPath = new File("similarityChecker/files/" + exercise + "/" + sheet + "/checks/");
        String[] contents = directoryPath.list();

        //if there aren't any scores yet return empty list
        if (contents == null) {
            return new ArrayList<>();
        }

        for(String file : contents) {

            ArrayList<SimilarityScore> oneLine = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(
                    "similarityChecker/files/" + exercise + "/" + sheet + "/checks/" + file))) {

                String line = reader.readLine();
                while (line != null) {
                    String[] content = line.split(" ");
                    String scoreString = content[2];
                    if (scoreString.split("\\.")[1].length() > 2) {
                        scoreString = content[2].split("\\.")[0] + "." + content[2].split("\\.")[1].substring(0, 2);
                    }
                    double score = Double.parseDouble(scoreString);
                    String group1 = content[0].split("-")[0];
                    String team1 = content[0].split("-")[1];
                    String group2 = content[1].split("-")[0];
                    String team2 = content[1].split("-")[1];
                    String filename1 = UriUtils.decode(content[3], StandardCharsets.UTF_8.toString());
                    String filename2 = UriUtils.decode(content[4], StandardCharsets.UTF_8.toString());
                    String assignmentID = file.split("_")[0];
                    oneLine.add(new SimilarityScore(score, team1, team2, group1, group2, filename1, filename2, sheet, assignmentID));
                    // read next line
                    line = reader.readLine();
                }

                list.add(oneLine.toArray(new SimilarityScore[0]));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    //returns an array containing all the scores for one assignemnt in a sheet
    public static SimilarityScore[] getScoresForAssignment(String exercise, String sheet, String assignment) {
        ArrayList<SimilarityScore> scores = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(
                "similarityChecker/files/" + exercise + "/" + sheet + "/checks/" + assignment + "_Results.txt"))) {
            String line = reader.readLine();
            while (line != null) {
                String[] content = line.split(" ");
                String scoreString = content[2];
                if (scoreString.split("\\.")[1].length() > 2) {
                    scoreString = content[2].split("\\.")[0] + "." + content[2].split("\\.")[1].substring(0, 2);
                }
                double score = Double.parseDouble(scoreString);
                String group1 = content[0].split("-")[0];
                String team1 = content[0].split("-")[1];
                String group2 = content[1].split("-")[0];
                String team2 = content[1].split("-")[1];
                String filename1 = UriUtils.decode(content[3], StandardCharsets.UTF_8.toString());
                String filename2 = UriUtils.decode(content[4], StandardCharsets.UTF_8.toString());
                scores.add(new SimilarityScore(score, team1, team2, group1, group2, filename1, filename2, sheet, assignment));
                // read next line
                line = reader.readLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return scores.toArray(new SimilarityScore[0]);
    }
}
