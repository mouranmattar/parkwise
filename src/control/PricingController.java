package control;

import entity.PriceList;
import utils.DatabaseConnection;
import org.json.JSONObject;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

public class PricingController {

    public void importPriceList(String jsonFile, int lotID) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(jsonFile)));
            PriceList priceList = parseJson(content);
            
            if (priceList != null) {
                priceList.setParkingLotID(lotID);
                DatabaseConnection.insertPriceList(priceList);
                System.out.println("Price list imported successfully for lot #" + lotID);
            }
        } catch (Exception e) {
            System.err.println("Error importing price list: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<PriceList> getPriceHistory(int lotID) {
        return DatabaseConnection.getPriceHistory(lotID);
    }

    private PriceList parseJson(String jsonContent) {
        try {
            JSONObject json = new JSONObject(jsonContent);
            
            int year = json.getInt("year");
            double firstHourPrice = json.getDouble("firstHourPrice");
            double additionalHourPrice = json.getDouble("additionalHourPrice");
            double fullDayPrice = json.getDouble("fullDayPrice");
            
            String validFromStr = json.optString("validFrom", null);
            Date validFrom = validFromStr != null ? new Date() : new Date();
            
            return new PriceList(0, 0, year, firstHourPrice, additionalHourPrice, fullDayPrice, validFrom);
            
        } catch (Exception e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
            return null;
        }
    }
}
