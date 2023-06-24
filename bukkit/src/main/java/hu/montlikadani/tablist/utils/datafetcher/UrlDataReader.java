package hu.montlikadani.tablist.utils.datafetcher;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;

@SuppressWarnings("deprecation")
public final class UrlDataReader {

    public static JsonObject readJsonObject(String urlName) {
        try {
            java.net.URLConnection urlConnection = new java.net.URL(urlName).openConnection();

            urlConnection.setConnectTimeout(3000);
            urlConnection.setReadTimeout(5000);

            try (InputStreamReader content = new InputStreamReader(urlConnection.getInputStream())) {
                try {
                    return JsonParser.parseReader(content).getAsJsonObject();
                } catch (NoSuchMethodError e) {
                    return new JsonParser().parse(content).getAsJsonObject();
                }
            }
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static JsonObject decodeSkinValue(String value) {
        String decodedValue = new String(java.util.Base64.getDecoder().decode(value));

        try {
            return JsonParser.parseString(decodedValue).getAsJsonObject();
        } catch (NoSuchMethodError e) {
            return new JsonParser().parse(decodedValue).getAsJsonObject();
        }
    }
}
