import java.util.HashMap;
import java.util.Map;

public class CountryNameConverter {

    // Mapping from full country names to ISO names
    private Map<String, String> fullNameToIsoMapping;

    public CountryNameConverter(Map<String, String> stateNameData) {
        fullNameToIsoMapping = new HashMap<>();
        for (Map.Entry<String, String> entry : stateNameData.entrySet()) {
            // Assuming stateNameData maps ISO codes to full names
            fullNameToIsoMapping.put(entry.getValue(), entry.getKey());
        }
    }

    public void convertBordersToIso(Map<String, Map<String, Integer>> bordersData) {
        Map<String, Map<String, Integer>> convertedBordersData = new HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> entry : bordersData.entrySet()) {
            String countryIso = fullNameToIsoMapping.getOrDefault(entry.getKey(), entry.getKey());
            Map<String, Integer> convertedNeighbors = new HashMap<>();
            for (Map.Entry<String, Integer> neighborEntry : entry.getValue().entrySet()) {
                String neighborIso = fullNameToIsoMapping.getOrDefault(neighborEntry.getKey(), neighborEntry.getKey());
                convertedNeighbors.put(neighborIso, neighborEntry.getValue());
            }
            convertedBordersData.put(countryIso, convertedNeighbors);
        }

        // Replace the original borders data with the converted data
        bordersData.clear();
        bordersData.putAll(convertedBordersData);
    }

    // Other methods...
}
