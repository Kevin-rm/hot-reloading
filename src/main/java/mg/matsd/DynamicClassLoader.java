package mg.matsd;

import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicClassLoader {
    private final String classOutputPath;
    private final Instrumentation instrumentation;
    private final Map<String, Class<?>> loadedClasses;

    DynamicClassLoader(final String classOutputPath, final Instrumentation instrumentation) {
        this.classOutputPath = classOutputPath;
        this.instrumentation = instrumentation;

        loadedClasses = new ConcurrentHashMap<>();
        Arrays.stream(instrumentation.getAllLoadedClasses()).forEachOrdered(c -> loadedClasses.put(c.getName(), c));
    }

    public String getClassOutputPath() {
        return classOutputPath;
    }

    public void reload(final Path classFilePath) {

    }
}
