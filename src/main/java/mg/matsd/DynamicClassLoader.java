package mg.matsd;

import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DynamicClassLoader {
    private final String classOutputPath;
    private final Instrumentation instrumentation;
    private final Map<String, Class<?>> loadedClasses;

    DynamicClassLoader(final String classOutputPath, final Instrumentation instrumentation) {
        this.classOutputPath = classOutputPath;
        this.instrumentation = instrumentation;

        loadedClasses = new ConcurrentHashMap<>();
        Arrays.stream(instrumentation.getAllLoadedClasses())
            .filter(c -> c.getClassLoader() != null)
            .forEachOrdered(c -> loadedClasses.put(c.getName(), c));
    }

    public String getClassOutputPath() {
        return classOutputPath;
    }

    public boolean reload(final Path classFilePath) {

    }
}
