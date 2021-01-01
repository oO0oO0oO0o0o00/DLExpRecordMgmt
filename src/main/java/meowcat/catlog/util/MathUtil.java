package meowcat.catlog.util;

import java.util.List;

public class MathUtil {

    public static int ensureRange(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    public static float mean(List<Float> floats) {
        float sum = 0.0f;
        for (float i : floats) sum += i;
        return sum / floats.size();
    }

    public static float std(List<Float> floats, float mean) {
        float sum = 0.0f;
        for (float i : floats) {
            i = i - mean;
            sum += i * i;
        }
        return (float) Math.sqrt(sum / floats.size());
    }
}
