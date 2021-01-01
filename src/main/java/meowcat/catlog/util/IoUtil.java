package meowcat.catlog.util;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class IoUtil {
    public static JsonValue readJson(File file) {
        try {
            FileReader reader = new FileReader(file);
            JsonValue value = Json.parse(reader);
            reader.close();
            return value;
        } catch (IOException e) {
            return null;
        }
    }
}
