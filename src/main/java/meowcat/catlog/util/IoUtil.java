package meowcat.catlog.util;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.util.FileObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class IoUtil {

    private static final Logger logger = LogManager.getLogger(IoUtil.class);


    public static Map<String, Object> readJson(FileObject file) {
        try {
            //noinspection unchecked
            return JsonMapper.builder().enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
                    .build().readValue(new InputStreamReader(
                            file.getContent().getInputStream()), Map.class);
        } catch (IOException | NullPointerException e) {
            String logging_text = "Cannot read json from FileObject";
            logger.warn("{}: {}", logging_text, e.getClass().getName());
            logger.debug(logging_text, e);
            return null;
        }
    }


    public static List<Object> readJsonArray(FileObject file) {
        try {
            //noinspection unchecked
            return new ObjectMapper().readValue(new InputStreamReader(
                    file.getContent().getInputStream()), List.class);
        } catch (IOException | NullPointerException e) {
            String logging_text = "Cannot read json from FileObject";
            logger.warn("{}: {}", logging_text, e.getClass().getName());
            logger.debug(logging_text, e);
            return null;
        }
    }

    public static String readText(FileObject file) {
        try {
            return FileObjectUtils.getContentAsString(file, StandardCharsets.UTF_8);
        } catch (IOException | NullPointerException e) {
            String logging_text = "Cannot read text from FileObject";
            logger.warn("{}: {}", logging_text, e.getClass().getName());
            logger.debug(logging_text, e);
            return null;
        }
    }

    public static void close(FileObject fileObject) {
        try {
            fileObject.close();
        } catch (FileSystemException e) {
            logger.info("Error closing FileObject", e);
        }
    }
}
