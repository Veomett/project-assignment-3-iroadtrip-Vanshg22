import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
public class IRoadTrip {
    private Map<String, Map<String, Integer>> bordersData;
    private Map<String, Integer> capDistData;
    private Map<String, String> stateNameData;
    private CountryNameConverter nameConverter;


    public IRoadTrip (String [] args) {
        // Replace with your code
        bordersData = new HashMap<>();
        capDistData = new HashMap<>();
        stateNameData = new HashMap<>();

        // Assuming file paths are passed in args; adjust as needed
        readBordersFile(args[0]);
        readCapDistFile(args[1]);
        readStateNameFile(args[2]);
        nameConverter = new CountryNameConverter(stateNameData);
        nameConverter.convertBordersToIso(bordersData);
    }


    public int getDistance(String country1, String country2) {
        if (!stateNameData.containsKey(country1) || !stateNameData.containsKey(country2)) {
            return -1;
        }

        // Initialize distances map with infinite distances
        Map<String, Integer> distances = new HashMap<>();
        for (String country : stateNameData.keySet()) {
            distances.put(country, Integer.MAX_VALUE);
        }
        distances.put(country1, 0);

        // Priority queue to select the next country to visit
        PriorityQueue<Map.Entry<String, Integer>> queue = new PriorityQueue<>(Comparator.comparingInt(Map.Entry::getValue));
        queue.offer(new AbstractMap.SimpleEntry<>(country1, 0));

        while (!queue.isEmpty()) {
            Map.Entry<String, Integer> current = queue.poll();
            String currentCountry = current.getKey();

            // Iterate over neighbors
            for (String neighbor : stateNameData.keySet()) {
                String pairKey = currentCountry + "-" + neighbor;
                if (capDistData.containsKey(pairKey)) {
                    int distance = capDistData.get(pairKey);
                    int newDist = distances.get(currentCountry) + distance;
                    if (newDist < distances.get(neighbor)) {
                        distances.put(neighbor, newDist);
                        queue.offer(new AbstractMap.SimpleEntry<>(neighbor, newDist));
                    }
                }
            }
        }

        return distances.getOrDefault(country2, -1);
    }

    public List<String> findPath(String country1, String country2) {
        List<String> path = new ArrayList<>();

        if (!stateNameData.containsKey(country1) || !stateNameData.containsKey(country2)) {
            return path; // Return an empty list if either country is not in the dataset
        }

        // Initialize distances and predecessors
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> predecessors = new HashMap<>();
        for (String country : stateNameData.keySet()) {
            distances.put(country, Integer.MAX_VALUE);
            predecessors.put(country, null);
        }
        distances.put(country1, 0);

        // Priority queue for Dijkstra's algorithm
        PriorityQueue<Map.Entry<String, Integer>> queue = new PriorityQueue<>(Comparator.comparingInt(Map.Entry::getValue));
        queue.offer(new AbstractMap.SimpleEntry<>(country1, 0));

        while (!queue.isEmpty()) {
            Map.Entry<String, Integer> current = queue.poll();
            String currentCountry = current.getKey();

            // Iterate over neighbors
            for (String neighbor : stateNameData.keySet()) {
                String pairKey = currentCountry + "-" + neighbor;
                if (capDistData.containsKey(pairKey)) {
                    int distance = capDistData.get(pairKey);
                    int newDist = distances.get(currentCountry) + distance;
                    if (newDist < distances.get(neighbor)) {
                        distances.put(neighbor, newDist);
                        predecessors.put(neighbor, currentCountry);
                        queue.offer(new AbstractMap.SimpleEntry<>(neighbor, newDist));
                    }
                }
            }
        }

        // Build the path from predecessors if a path exists
        if (predecessors.get(country2) != null) {
            Stack<String> stack = new Stack<>();
            String currentCountry = country2;
            while (currentCountry != null) {
                stack.push(currentCountry);
                currentCountry = predecessors.get(currentCountry);
            }

            // Pop from stack to build path in correct order
            String prevCountry = stack.pop();
            while (!stack.isEmpty()) {
                String nextCountry = stack.pop();
                path.add(prevCountry + " --> " + nextCountry + " (" + capDistData.get(prevCountry + "-" + nextCountry) + " km.)");
                prevCountry = nextCountry;
            }
        }

        return path;
    }




    public void acceptUserInput() {
        Scanner scanner = new Scanner(System.in);
        String country1, country2;

        while (true) {
            country1 = getUserCountry(scanner, "first");
            if (country1 == null) break; // User chose to exit

            country2 = getUserCountry(scanner, "second");
            if (country2 == null) break; // User chose to exit

            List<String> path = findPath(country1, country2);
            if (path.isEmpty()) {
                System.out.println("No path found or one of the countries does not exist.");
                continue;
            }

            System.out.println("Route from " + country1 + " to " + country2 + ":");
            for (String step : path) {
                System.out.println("* " + step);
            }
            break;
        }
        scanner.close();
    }

    private String getUserCountry(Scanner scanner, String order) {
        String country;
        while (true) {
            System.out.println("Enter the ISO code of the " + order + " country (type EXIT to quit):");
            country = scanner.nextLine().trim().toUpperCase(); // ISO codes are typically in uppercase
            if (country.equalsIgnoreCase("EXIT")) {
                return null;
            }

            if (stateNameData.containsKey(country)) {
                return country;
            } else {
                System.out.println("Invalid country ISO code. Please enter a valid ISO code.");
            }
        }
    }


    private void readBordersFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" = ");
                String country = parts[0].trim();

                // Handle country aliases
                String actualCountry = country.contains("(") ? country.split(" \\(")[0].trim() : country;

                Map<String, Integer> neighbors = new HashMap<>();

                if (parts.length > 1) {
                    String[] borderCountries = parts[1].split("; ");
                    for (String border : borderCountries) {
                        String[] borderInfo = border.trim().split(" ");
                        if (borderInfo.length < 2) continue;

                        String neighborCountry = borderInfo[0];
                        String borderLengthStr = borderInfo[1].replaceAll("[^0-9]", "");  // Remove non-numeric characters

                        if (borderLengthStr.matches("\\d+")) {
                            int borderLength = Integer.parseInt(borderLengthStr);
                            neighbors.put(neighborCountry, borderLength);
                        }
                    }
                }

                // Add the country and its neighbors to the map
                bordersData.put(actualCountry, neighbors);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void readCapDistFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isFirstLine = true; // To skip the header

            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip the header line
                }

                String[] values = line.split(",");
                if (values.length >= 6 && values[4].matches("\\d+")) { // Check if the distance value is numeric
                    String countryAId = values[1].trim(); // Country code for country A
                    String countryBId = values[3].trim(); // Country code for country B
                    int distanceKm = Integer.parseInt(values[4].trim()); // Distance in kilometers

                    // Create a unique key for each country pair
                    String key = countryAId + "-" + countryBId;

                    // Store the data using the key
                    capDistData.put(key, distanceKm);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    private void readStateNameFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Map<String, Date> latestDates = new HashMap<>(); // To store the latest dates for each country

            while ((line = br.readLine()) != null) {
                String[] values = line.split("\t");
                if (values.length >= 5) {
                    String countryId = values[1].trim();
                    String countryName = values[2].trim();
                    Date endDate;

                    try {
                        endDate = dateFormat.parse(values[4].trim());
                    } catch (ParseException e) {
                        // Skip this line if the date format is incorrect
                        continue;
                    }

                    // Compare and store the most recent date's data
                    if (!latestDates.containsKey(countryId) || endDate.after(latestDates.get(countryId))) {
                        latestDates.put(countryId, endDate);
                        stateNameData.put(countryId, countryName);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void printStandardizedBordersData() {
        System.out.println("Standardized Borders Data:");
        for (Map.Entry<String, Map<String, Integer>> countryEntry : bordersData.entrySet()) {
            String country = countryEntry.getKey();
            Map<String, Integer> neighbors = countryEntry.getValue();

            System.out.println("Country: " + country + " has borders with:");
            for (Map.Entry<String, Integer> neighborEntry : neighbors.entrySet()) {
                String neighbor = neighborEntry.getKey();
                Integer borderLength = neighborEntry.getValue();
                System.out.println("\t- " + neighbor + " (Border Length: " + borderLength + " km)");
            }
            System.out.println(); // Adding a line break for better readability
        }
    }


    public static void main(String[] args) {
        IRoadTrip a3 = new IRoadTrip(args);
        //a3.printStandardizedBordersData();
        a3.acceptUserInput();
    }

}
