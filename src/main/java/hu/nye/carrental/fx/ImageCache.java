package hu.nye.carrental.fx;

import javafx.application.Platform;
import javafx.scene.image.Image;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Downloads car photos from the internet ONCE and keeps a copy in
 * ~/CarRentalData/images, so the app works fast (and offline) afterwards.
 * Images are loaded in the background; the callback runs on the JavaFX thread.
 */
public final class ImageCache {

    private static final Path DIRECTORY = Path.of(System.getProperty("user.home"), "CarRentalData", "images");
    private static final String USER_AGENT =
            "CarRentalSystem/3.0 (NYE university project; JavaFX desktop app)";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /** One download at a time: friendly to image servers (Wikimedia limits parallel requests). */
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "image-loader");
        thread.setDaemon(true);
        return thread;
    });

    private static final Map<String, Image> MEMORY = new ConcurrentHashMap<>();

    private ImageCache() {
    }

    /**
     * Loads the image behind the URL. Calls {@code onReady} with the image,
     * or with {@code null} if it could not be loaded. Blank URLs are ignored.
     */
    public static void load(String url, Consumer<Image> onReady) {
        if (url == null || url.isBlank()) {
            return;
        }
        Image cached = MEMORY.get(url);
        if (cached != null) {
            onReady.accept(cached);
            return;
        }
        WORKER.submit(() -> {
            Image image = loadNow(url);
            if (image != null) {
                MEMORY.put(url, image);
            }
            Platform.runLater(() -> onReady.accept(image));
        });
    }

    /**
     * Makes pasted links work better:
     * a Wikimedia Commons file page ("commons.wikimedia.org/wiki/File:...") becomes a direct image link.
     */
    public static String normalize(String url) {
        if (url == null) {
            return null;
        }
        String trimmed = url.trim();
        String marker = "commons.wikimedia.org/wiki/File:";
        int index = trimmed.indexOf(marker);
        if (index >= 0) {
            String fileName = trimmed.substring(index + marker.length());
            int hash = fileName.indexOf('#');
            if (hash >= 0) {
                fileName = fileName.substring(0, hash);
            }
            return "https://commons.wikimedia.org/wiki/Special:FilePath/" + fileName + "?width=800";
        }
        return trimmed;
    }

    private static Image loadNow(String url) {
        try {
            Files.createDirectories(DIRECTORY);
            Path file = DIRECTORY.resolve(hash(url) + ".img");
            if (!Files.exists(file)) {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(30))
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "image/jpeg,image/png,image/gif,image/*;q=0.8")
                        .GET()
                        .build();
                HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() != 200) {
                    response.body().close();
                    return null;
                }
                Path temp = Files.createTempFile(DIRECTORY, "download", ".tmp");
                try (InputStream body = response.body()) {
                    Files.copy(body, temp, StandardCopyOption.REPLACE_EXISTING);
                }
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            try (InputStream in = Files.newInputStream(file)) {
                Image image = new Image(in, 800, 0, true, true);
                if (image.isError() || image.getWidth() <= 0) {
                    Files.deleteIfExists(file);
                    return null;
                }
                return image;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static String hash(String text) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
    }
}
