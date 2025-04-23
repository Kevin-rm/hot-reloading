package mg.matsd;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicClassLoader {
    private final Path classOutputPath;
    private final Instrumentation instrumentation;
    private final Map<String, Class<?>> loadedClasses;

    DynamicClassLoader(final String classOutputPath, final Instrumentation instrumentation) {
        this.classOutputPath = Utils.stringToPath(classOutputPath);
        this.instrumentation = instrumentation;

        loadedClasses = new ConcurrentHashMap<>();
        Arrays.stream(instrumentation.getAllLoadedClasses())
            .filter(c -> c.getClassLoader() != null)
            .forEachOrdered(c -> loadedClasses.put(c.getName(), c));
    }

    public Path getClassOutputPath() {
        return classOutputPath;
    }

    public boolean reload(final Path classFilePath) {
        final Class<?> loadedClass = loadedClasses.get(classOutputPath.relativize(classFilePath)
            .toString()
            .replace(File.separator, ".")
            .replaceAll("\\.class$", ""));
        if (loadedClass == null) return false;

        try {
            instrumentation.redefineClasses(
                new ClassDefinition(loadedClass, Files.readAllBytes(classFilePath)
            ));

            return true;
        } catch (ClassNotFoundException | UnmodifiableClassException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
