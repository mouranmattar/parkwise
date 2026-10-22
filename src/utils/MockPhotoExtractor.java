package utils;

public class MockPhotoExtractor {
    public static class ExtractedVehicleData {
        public final String type;
        public final String color;
        public final int size;   // 1=small,2=medium,3=large
        public final double weight;

        public ExtractedVehicleData(String type, String color, int size, double weight) {
            this.type = type;
            this.color = color;
            this.size = size;
            this.weight = weight;
        }
    }

    public static ExtractedVehicleData extract(String photoFront, String photoRear, String photoSide) {
        String combinedNames = (photoFront + " " + photoRear + " " + photoSide).toLowerCase();
        
        // 1. Detect Color
        String mappedColor = mapColor(detectColorFromName(combinedNames));
        
        // 2. Detect Weight (e.g., 1000kg)
        double weight = 1500.0; // Default
        java.util.regex.Pattern weightPattern = java.util.regex.Pattern.compile("(\\d+)(?:kg|k)");
        java.util.regex.Matcher weightMatcher = weightPattern.matcher(combinedNames);
        if (weightMatcher.find()) {
            try {
                weight = Double.parseDouble(weightMatcher.group(1));
            } catch (NumberFormatException ignored) {}
        }

        // 3. Detect Size (e.g., large, medium, small)
        int size = 2; // Default Medium
        if (combinedNames.contains("large") || combinedNames.contains("big") || combinedNames.contains("size3")) {
            size = 3;
        } else if (combinedNames.contains("small") || combinedNames.contains("tiny") || combinedNames.contains("size1")) {
            size = 1;
        } else if (combinedNames.contains("medium") || combinedNames.contains("mid") || combinedNames.contains("size2")) {
            size = 2;
        } else {
            // Fallback to weight-based size if no keyword found
            size = weight < 1400 ? 1 : (weight < 2000 ? 2 : 3);
        }

        // 4. Detect Type
        String type = "private";
        if (combinedNames.contains("truck") || combinedNames.contains("lorry")) type = "truck";
        else if (combinedNames.contains("bus")) type = "bus";
        else if (combinedNames.contains("bike") || combinedNames.contains("motor")) type = "motorcycle";

        return new ExtractedVehicleData(type, mappedColor, size, weight);
    }

    private static String detectColorFromName(String... names) {
        String[] colors = {"white", "black", "silver", "blue", "red", "green", "yellow", "other"};
        String lower = String.join(" ", names).toLowerCase();
        for (String c : colors) {
            if (lower.contains(c)) return c;
        }
        return "other";
    }

    private static String mapColor(String color) {
        if (color == null) return "other";
        String c = color.trim().toLowerCase();
        if (c.equals("white") || c.equals("black") || c.equals("silver") || c.equals("blue")) return c;
        return "other";
    }
}
