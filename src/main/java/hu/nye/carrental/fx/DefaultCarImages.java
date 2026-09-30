package hu.nye.carrental.fx;

import java.util.Locale;
import java.util.Map;

/**
 * Photos for the sample cars (free images from Wikimedia Commons).
 * Cars without an image URL get one of these automatically, if brand + model match.
 */
public final class DefaultCarImages {

    private static final String BASE = "https://commons.wikimedia.org/wiki/Special:FilePath/";
    private static final String SIZE = "?width=800";

    private static final Map<String, String> IMAGES = Map.of(
            "suzuki|swift", "2021_Suzuki_Swift_GL_PLUS.jpg",
            "skoda|fabia", "Skoda_Fabia_IV_IMG_5307.jpg",
            "volkswagen|golf", "Volkswagen_Golf_VIII_IMG_3472.jpg",
            "renault|clio", "Renault_Clio_V_1X7A0392.jpg",
            "toyota|corolla", "Toyota_Corolla_sedan_E210_hydrid.jpg",
            "bmw|3 series", "BMW_G20_(2022)_IMG_7316.jpg",
            "toyota|rav4", "Toyota_RAV4_(XA50)_Hybrid_(5).jpg",
            "volkswagen|tiguan", "Volkswagen_Tiguan_III_eHybrid_Auto_Zuerich_2023_1X7A0999.jpg",
            "ford|transit", "2020_Ford_Transit_350_Leader_EcoBlue_2.0_facelift_Front.jpg",
            "renault|trafic", "Renault_Trafic_3_fl.jpg"
    );

    private DefaultCarImages() {
    }

    /** Returns a photo URL for the brand + model, or null if we have none. */
    public static String find(String brand, String model) {
        if (brand == null || model == null) {
            return null;
        }
        String key = normalizeBrand(brand) + "|" + model.trim().toLowerCase(Locale.ROOT);
        String file = IMAGES.get(key);
        return file == null ? null : BASE + file + SIZE;
    }

    private static String normalizeBrand(String brand) {
        String value = brand.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "vw" -> "volkswagen";
            case "škoda" -> "skoda";
            default -> value;
        };
    }
}
