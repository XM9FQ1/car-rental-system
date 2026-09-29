package hu.nye.carrental.fx;

import javafx.scene.layout.Region;

/**
 * Simple vector icons (Material Design shapes, 24x24 paths).
 * No extra library is needed: the shape is drawn by CSS (-fx-shape) on a Region,
 * and the colour/size come from app.css (class "icon").
 */
public final class Icons {

    public static final String DASHBOARD =
            "M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z";
    public static final String RENTAL =
            "M17 12h-5v5h5v-5zM16 1v2H8V1H6v2H5c-1.11 0-1.99.9-1.99 2L3 19c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5"
            + "c0-1.1-.9-2-2-2h-1V1h-2zm3 18H5V8h14v11z";
    public static final String CAR =
            "M18.92 6.01C18.72 5.42 18.16 5 17.5 5h-11c-.66 0-1.21.42-1.42 1.01L3 12v8c0 .55.45 1 1 1h1"
            + "c.55 0 1-.45 1-1v-1h12v1c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-8l-2.08-5.99zM6.5 16"
            + "c-.83 0-1.5-.67-1.5-1.5S5.67 13 6.5 13s1.5.67 1.5 1.5S7.33 16 6.5 16zm11 0"
            + "c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zM5 11l1.5-4.5h11L19 11H5z";
    public static final String PEOPLE =
            "M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0"
            + "c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5"
            + "c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.970 1.97 3.45V19h6v-2.5"
            + "c0-2.33-4.67-3.5-7-3.5z";
    public static final String TAG =
            "M21.41 11.58l-9-9C12.05 2.22 11.55 2 11 2H4c-1.1 0-2 .9-2 2v7c0 .55.22 1.05.59 1.42l9 9"
            + "c.36.36.86.58 1.41.58.55 0 1.05-.22 1.41-.59l7-7c.37-.36.59-.86.59-1.41 0-.55-.23-1.06-.59-1.42z"
            + "M5.5 7C4.67 7 4 6.33 4 5.5S4.67 4 5.5 4 7 4.67 7 5.5 6.33 7 5.5 7z";
    public static final String CATEGORY =
            "M12 2l-5.5 9h11zM17.5 13c-2.49 0-4.5 2.01-4.5 4.5s2.01 4.5 4.5 4.5 4.5-2.01 4.5-4.5"
            + "-2.01-4.5-4.5-4.5zM3 21.5h8v-8H3z";
    public static final String SHIELD =
            "M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z";
    public static final String WARNING =
            "M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z";
    public static final String MONEY =
            "M11.8 10.9c-2.27-.59-3-1.2-3-2.15 0-1.09 1.01-1.85 2.7-1.85 1.78 0 2.44.85 2.5 2.1h2.21"
            + "c-.07-1.72-1.12-3.3-3.21-3.81V3h-3v2.16c-1.94.42-3.5 1.68-3.5 3.61 0 2.31 1.91 3.46 4.7 4.13"
            + " 2.5.6 3 1.48 3 2.41 0 .69-.49 1.79-2.7 1.79-2.06 0-2.87-.92-2.98-2.1h-2.2"
            + "c.12 2.19 1.76 3.42 3.68 3.83V21h3v-2.15c1.95-.37 3.5-1.5 3.5-3.55 0-2.84-2.43-3.81-4.7-4.4z";
    public static final String KEY =
            "M12.65 10C11.83 7.67 9.61 6 7 6c-3.31 0-6 2.690-6 6s2.69 6 6 6c2.61 0 4.83-1.67 5.65-4H17v4h4v-4h2v-4"
            + "H12.65zM7 14c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z";

    private Icons() {
    }

    /** Creates an icon with the given shape. Extra style classes (e.g. colours) can be added. */
    public static Region of(String svgPath, String... styleClasses) {
        Region icon = new Region();
        icon.setStyle("-fx-shape: \"" + svgPath + "\";");
        icon.getStyleClass().add("icon");
        icon.getStyleClass().addAll(styleClasses);
        return icon;
    }

    /** Icon for a menu item of the sidebar. */
    public static Region nav(String menuName) {
        String path = switch (menuName) {
            case "Dashboard" -> DASHBOARD;
            case "Rentals" -> RENTAL;
            case "Cars" -> CAR;
            case "Customers" -> PEOPLE;
            case "Brands" -> TAG;
            case "Categories" -> CATEGORY;
            case "Insurance" -> SHIELD;
            default -> DASHBOARD;
        };
        return of(path);
    }
}
