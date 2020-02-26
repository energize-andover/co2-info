package com.energizeandover.co2info;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Main class for co2-info. Reads CO2 data from csv and allows the user to find information.
 */
public class CO2Info {
    private static Scanner scanner;
    private static MeterDatabase meterDatabase;
    private static Path csvPath;

    /**
     * co2-info main method.
     *
     * @param args CSV file to read from
     */
    public static void main(String[] args) {
        try {
            scanner = new Scanner(System.in);
            csvPath = findPath(args);

            addReadings();

            System.out.println("Successfully loaded " + meterDatabase.size() + " meters.");
            showMenu();
        } catch (FileNotFoundException e) {
            System.out.println("Failed to load meters: File not found.");
        } catch (IOException e) {
            System.out.println("Failed to load meters: An IOException occurred.");
        } catch (CsvException e) {
            System.out.println("Failed to load meters: An CsvException occurred.");
        }
    }

    /**
     * Returns a file path from command-line argument or user input.
     *
     * @param args command-line argument
     * @return file path
     */
    private static Path findPath(String[] args) throws FileNotFoundException {
        Path path;

        if (args.length > 0) {
            path = Paths.get(args[0]);
        } else {
            System.out.print("\nEnter input file name: ");
            path = Paths.get(scanner.nextLine().trim());
        }

        if (!Files.exists(path)) {
            throw new FileNotFoundException();
        }
        return path;
    }

    /**
     * Iterates through the CSV file and adds Readings to the MeterData of the MeterDatabase.
     */
    private static void addReadings() throws IOException, CsvValidationException {
        try (CSVReader csvReader = new CSVReader(new FileReader(csvPath.toFile()))) {
            meterDatabase = initializeMeterDatabase(csvReader);

            System.out.println("Reading CSV...");
            String[] line;
            while ((line = csvReader.readNext()) != null) {
                LocalDateTime localDateTime = parseTime(line[0]);

                String[] ppmEntries = new String[line.length - 1];
                System.arraycopy(line, 1, ppmEntries, 0, ppmEntries.length);

                meterDatabase.addAll(localDateTime, ppmEntries);
            }
        } catch (FileNotFoundException e) {
            System.out.println("The file could not be found!");
            System.exit(-1);
        } catch (IOException | CsvException e) {
            System.out.println("An exception occurred when reading the csv file!");
            e.printStackTrace();
            System.exit(-2);
        }
    }

    /**
     * Gets meter names from headers and returns a MeterDatabase from them.
     *
     * @return MeterDatabase with meters from header names (skipping temperature column)
     */
    private static MeterDatabase initializeMeterDatabase(CSVReader csvReader)
            throws IOException, CsvValidationException {
        // TODO add custom exception
        String[] meterNames;
        meterNames = csvReader.readNext();
        System.arraycopy(meterNames, 1, meterNames, 0, meterNames.length - 1);
        return new MeterDatabase(meterNames);
    }

    /**
     * Parses a LocalDateTime from String timeEntry.
     *
     * @param timeEntry String containing date and time separated by a space
     * @return LocalDateTime matching timeEntry
     */
    private static LocalDateTime parseTime(String timeEntry) {
        String[] date_time = timeEntry.split(" ");
        LocalDate localDate = LocalDate.parse(date_time[0]);
        LocalTime localTime = LocalTime.parse(date_time[1]);
        return LocalDateTime.of(localDate, localTime);
    }

    /**
     * Shows the co2-info menu.
     */
    private static void showMenu() {
        while (true) {
            String choice = promptChoice();
            switch (choice) {
                case "1":
                    System.out.println(meterDatabase.toStringAverages());
                    break;
                case "2":
                    System.out.println("Unhealthy ppm readings:\n"
                            + String.join("", findUnhealthyReadings(meterDatabase)));
                    break;
                case "3":
                    System.out.println("Broken ppm readings:\n"
                            + String.join("", findBrokenReadings(meterDatabase)));
                    break;
                case "4":
                    System.out.println("Which meter?");
                    findMeters(scanner.nextLine());
                    break;
                case "5":
                    return;
                default:
                    System.out.println("Please enter 1, 2, 3, 4, or 5.");
            }
        }
    }

    private static String promptChoice() {
        System.out.println("Search Database:");
        System.out.println("1. Find all average readings");
        System.out.println("2. Find unhealthy readings");
        System.out.println("3. Find broken readings");
        System.out.println("4. Find individual meter's readings");
        System.out.println("5. Quit");

        return scanner.nextLine().trim();
    }

    private static List<String> findBrokenReadings(MeterDatabase meterDatabase) {
        List<String> brokenReadings = new ArrayList<>();

        for (MeterData meterData : meterDatabase) {
            brokenReadings.add(meterData.toStringBrokenReadings());
        }
        return brokenReadings;
    }

    private static List<String> findUnhealthyReadings(MeterDatabase meterDatabase) {
        List<String> unhealthyReadings = new ArrayList<>();

        for (MeterData meterData : meterDatabase) {
            unhealthyReadings.add(meterData.toStringUnhealthyReadings());
        }
        return unhealthyReadings;
    }

    /**
     * Lists the MeterData in the MeterDatabase that match the input name and lets the user view one.
     *
     * @param name input meter name
     */
    private static void findMeters(String name) {
        List<MeterData> meterDataList = meterDatabase.matchMeterName(name);

        System.out.println("Found " + meterDataList.size() + " meters.");

        if (meterDataList.size() == 0) {
            return;
        }
        for (int i = 0, size = meterDataList.size(); i < size; i++) {
            System.out.println((i + 1) + ". " + meterDataList.get(i).getMeterName());
        }
        int meterChoice = scanner.nextInt();
        scanner.nextLine();
        System.out.println(meterDataList.get(meterChoice - 1));
    }
}