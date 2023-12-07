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
    private Map<String, String> nameToCodeMapping;

    public IRoadTrip(String[] args) {
        bordersData = new HashMap<>();
        capDistData = new HashMap<>();
        stateNameData = new HashMap<>();

        readBordersFile(args[0]);
        readCapDistFile(args[1]);
        readStateNameFile(args[2]);
        nameConverter = new CountryNameConverter(stateNameData);
        nameConverter.convertBordersToIso(bordersData);

        nameToCodeMapping = new HashMap<>();
        for (Map.Entry<String, String> entry : stateNameData.entrySet()) {
            nameToCodeMapping.put(entry.getValue().toLowerCase(), entry.getKey());
        }
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

    public List<String> findPath(String isoCountry1, String isoCountry2) {
        List<String> path = new ArrayList<>();
        if (!bordersData.containsKey(isoCountry1) || !bordersData.containsKey(isoCountry2)) {
            return path; // Return an empty list if either country is not in the dataset
        }

        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> predecessors = new HashMap<>();
        PriorityQueue<String> queue = new PriorityQueue<>(Comparator.comparingInt(distances::get));

        for (String country : bordersData.keySet()) {
            distances.put(country, Integer.MAX_VALUE);
            predecessors.put(country, null);
        }
        distances.put(isoCountry1, 0);
        queue.add(isoCountry1);

        while (!queue.isEmpty()) {
            String current = queue.poll();

            // Check if the destination is reached
            if (current.equals(isoCountry2)) {
                break;
            }

            Map<String, Integer> neighbors = bordersData.getOrDefault(current, Collections.emptyMap());
            for (String neighbor : neighbors.keySet()) {
                int newDist = distances.get(current) + neighbors.get(neighbor);
                if (newDist < distances.get(neighbor)) {
                    distances.put(neighbor, newDist);
                    predecessors.put(neighbor, current);
                    if (!queue.contains(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
        }

        // Reconstruct the path from the destination back to the source
        Stack<String> stack = new Stack<>();
        for (String at = isoCountry2; at != null; at = predecessors.get(at)) {
            stack.push(at);
        }

        if (!stack.isEmpty() && stack.peek().equals(isoCountry1)) {
            while (!stack.isEmpty()) {
                path.add(stack.pop());
            }
        }

        return path;
    }


    public void acceptUserInput() {
        Scanner scanner = new Scanner(System.in);
        String isoCountry1, isoCountry2;

        while (true) {
            System.out.println("Enter the ISO code of the first country (type EXIT to quit):");
            isoCountry1 = scanner.nextLine().trim().toUpperCase();
            if (isoCountry1.equalsIgnoreCase("EXIT")) {
                break;
            }

            if (!stateNameData.containsKey(isoCountry1)) {
                System.out.println("Invalid country ISO code. Please enter a valid ISO code.");
                continue;
            }

            System.out.println("Enter the ISO code of the second country (type EXIT to quit):");
            isoCountry2 = scanner.nextLine().trim().toUpperCase();
            if (isoCountry2.equalsIgnoreCase("EXIT")) {
                break;
            }

            if (!stateNameData.containsKey(isoCountry2)) {
                System.out.println("Invalid country ISO code. Please enter a valid ISO code.");
                continue;
            }

            List<String> path = findPath(isoCountry1, isoCountry2);
            if (path.isEmpty()) {
                System.out.println("No path found between " + isoCountry1 + " and " + isoCountry2 + ".");
                continue;
            }

            System.out.println("Route from " + isoCountry1 + " to " + isoCountry2 + ":");
            for (int i = 0; i < path.size() - 1; i++) {
                String fromCountry = path.get(i);
                String toCountry = path.get(i + 1);
                int distance = getDistance(fromCountry, toCountry);
                System.out.println("* " + fromCountry + " --> " + toCountry + " (" + distance + " km.)");
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
                if (parts.length < 2) continue;

                String country = parts[0].trim();
                Map<String, Integer> neighbors = new HashMap<>();
                String[] borderCountries = parts[1].split("; ");

                for (String border : borderCountries) {
                    int lastSpaceIndex = border.lastIndexOf(' ');
                    if (lastSpaceIndex == -1) continue;

                    String neighborCountry = border.substring(0, lastSpaceIndex).trim();
                    String borderLengthStr = border.substring(lastSpaceIndex).replaceAll("[^0-9]", "");

                    if (borderLengthStr.matches("\\d+")) {
                        int borderLength = Integer.parseInt(borderLengthStr);
                        neighbors.put(neighborCountry, borderLength);
                    }
                }

                // Print statement to debug the parsed data
                System.out.println("Country: " + country + ", Neighbors: " + neighbors);

                bordersData.put(country, neighbors);
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
