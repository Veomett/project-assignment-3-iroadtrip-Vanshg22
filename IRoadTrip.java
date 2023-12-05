import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Map.Entry;
import java.util.AbstractMap;
import java.util.ArrayList;

class IRoadTrip {

    private Map<String, Map<String, Integer>> bordersData;
    private Map<String, Integer> capDistData;
    private Map<String, String> stateNameData;

    public IRoadTrip(String[] args) {
        // Initialize data structures
        bordersData = new HashMap<>();
        capDistData = new HashMap<>();
        stateNameData = new HashMap<>();

        // Read and clean data from files
        if (args.length >= 3) {
            readBordersFile(args[0]);
            cleanBordersData();  // Clean data after reading

            readCapDistFile(args[1]);
            cleanCapDistData();  // Clean data after reading

            readStateNameFile(args[2]);
            cleanStateNameData();  // Clean data after reading
        } else {
            System.out.println("Insufficient arguments provided...");
        }
    }


    public int getDistance(String country1, String country2) {
        // Assuming country1 and country2 are the IDs or names used in capDistData
        String key = country1 + "-" + country2;

        // Check if the distance for this pair exists in the opposite direction
        String reverseKey = country2 + "-" + country1;

        if (capDistData.containsKey(key)) {
            return capDistData.get(key);
        } else if (capDistData.containsKey(reverseKey)) {
            return capDistData.get(reverseKey);
        } else {
            // Return some error code or throw an exception if no distance is found
            return -1; // or throw new IllegalArgumentException("Distance not found.");
        }
    }

    public List<String> findPath(String country1, String country2) {
        // Data structures for Dijkstra's algorithm
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> predecessors = new HashMap<>();
        PriorityQueue<Entry<String, Integer>> priorityQueue = new PriorityQueue<>(Map.Entry.comparingByValue());

        // Initialize distances as infinity and set the distance of the start node as 0
        for (String country : bordersData.keySet()) {
            distances.put(country, Integer.MAX_VALUE);
            predecessors.put(country, null);
        }
        distances.put(country1, 0);

        // Add the start node to the priority queue
        priorityQueue.add(new AbstractMap.SimpleEntry<>(country1, 0));

        // Dijkstra's algorithm
        while (!priorityQueue.isEmpty()) {
            String currentCountry = priorityQueue.poll().getKey();
            int currentDistance = distances.get(currentCountry);

            if (currentCountry.equals(country2)) {
                break; // Found the shortest path to the destination
            }

            for (Map.Entry<String, Integer> neighborEntry : bordersData.get(currentCountry).entrySet()) {
                String neighbor = neighborEntry.getKey();
                int borderLength = neighborEntry.getValue();
                int newDistance = currentDistance + borderLength;

                if (newDistance < distances.get(neighbor)) {
                    distances.put(neighbor, newDistance);
                    predecessors.put(neighbor, currentCountry);
                    priorityQueue.add(new AbstractMap.SimpleEntry<>(neighbor, newDistance));
                }
            }
        }

        // Reconstruct the shortest path from country1 to country2
        List<String> path = new ArrayList<>();
        String step = country2;

        // Check if a path exists
        if (predecessors.get(step) == null) {
            return null; // No path found
        }

        path.add(step);
        while (predecessors.get(step) != null) {
            step = predecessors.get(step);
            path.add(0, step); // Insert at the beginning
        }

        return path;
    }

    public void acceptUserInput() {
        // Implement this method based on your project's requirements
        System.out.println("IRoadTrip - skeleton");
    }

    private void readBordersFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" = ");
                String country = parts[0];
                Map<String, Integer> neighbors = new HashMap<>();

                if (parts.length > 1) {
                    String[] borderCountries = parts[1].split("; ");
                    for (String border : borderCountries) {
                        String[] borderInfo = border.trim().split(" ");
                        if (borderInfo.length < 2) continue;
                        String neighborCountry = borderInfo[0];
                        String borderLengthStr = borderInfo[1];

                        if (borderLengthStr.matches("\\d+")) {
                            int borderLength = Integer.parseInt(borderLengthStr);
                            neighbors.put(neighborCountry, borderLength);
                        }
                    }
                }
                bordersData.put(country, neighbors);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cleanBordersData() {
        for (String country : new HashSet<>(bordersData.keySet())) {  // Use a new HashSet to avoid concurrent modification
            // Example: Standardize country names to uppercase
            Map<String, Integer> neighbors = bordersData.remove(country);
            bordersData.put(country.toUpperCase(), neighbors);

            for (Map.Entry<String, Integer> entry : new HashMap<>(neighbors).entrySet()) {
                String neighbor = entry.getKey();
                Integer borderLength = entry.getValue();

                // Handle missing or erroneous border lengths
                if (borderLength == null || borderLength < 0) {
                    neighbors.remove(neighbor);  // Remove this entry
                } else {
                    // Optionally standardize neighbor names as well
                    neighbors.remove(neighbor);
                    neighbors.put(neighbor.toUpperCase(), borderLength);
                }
            }
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
                    String countryAId = values[1].trim(); // ID of the first country
                    String countryBId = values[3].trim(); // ID of the second country
                    int distanceKm = Integer.parseInt(values[4].trim()); // Distance in kilometers

                    // Create a key for the country pair
                    String countryPairKey = countryAId + "-" + countryBId;
                    capDistData.put(countryPairKey, distanceKm); // Store the data
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void cleanCapDistData() {
        for (String countryPair : new HashSet<>(capDistData.keySet())) {  // Avoid concurrent modification
            Integer distance = capDistData.get(countryPair);

            // Handle non-numeric or negative distances
            if (distance == null || distance < 0) {
                capDistData.remove(countryPair);  // Remove erroneous entries
            }

            // Add more conditions as necessary based on your data
        }
    }

    private void readStateNameFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split("\t");
                if (values.length > 1) {
                    String countryId = values[1];
                    String countryName = values[2];
                    stateNameData.put(countryId, countryName);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void cleanStateNameData() {
        for (String code : new HashSet<>(stateNameData.keySet())) {  // Avoid concurrent modification
            String countryName = stateNameData.get(code);

            // Example: Remove entries with empty or null country names
            if (countryName == null || countryName.trim().isEmpty()) {
                stateNameData.remove(code);
            } else {
                // Standardize country names to uppercase
                stateNameData.put(code, countryName.toUpperCase());
            }

            // Add more conditions as necessary
        }
    }

    private void printBordersData() {
        for (String country : bordersData.keySet()) {
            System.out.println("Country: " + country);
            Map<String, Integer> neighbors = bordersData.get(country);
            for (String neighbor : neighbors.keySet()) {
                System.out.println(" - Neighbor: " + neighbor + ", Border Length: " + neighbors.get(neighbor) + " km");
            }
        }
    }

    private void printCapDistData() {
        for (String countryPair : capDistData.keySet()) {
            System.out.println("Country Pair: " + countryPair + ", Capital Distance: " + capDistData.get(countryPair) + " km");
        }
    }

    private void printStateNameData() {
        for (String code : stateNameData.keySet()) {
            System.out.println("Code: " + code + ", Country: " + stateNameData.get(code));
        }
    }

    public static void main(String[] args) {
        IRoadTrip a3 = new IRoadTrip(args);
        //a3.printBordersData();
        //a3.printCapDistData();
       // a3.printStateNameData();
    }
}
