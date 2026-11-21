package ai.gradpath.ml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/** Reads and writes {@link PlacementModel} JSON files. */
public final class ModelStore {

    /** Model bundled with the application (trained by {@link TrainModelCommand}). */
    public static final String CLASSPATH_LOCATION = "ml/placement-model.json";

    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private ModelStore() { }

    public static void write(PlacementModel model, Path file) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        JSON.writeValue(file.toFile(), model);
    }

    public static PlacementModel read(Path file) throws IOException {
        return JSON.readValue(file.toFile(), PlacementModel.class);
    }

    public static PlacementModel read(InputStream in) throws IOException {
        return JSON.readValue(in, PlacementModel.class);
    }
}
