package mg.matsd;

import java.lang.instrument.Instrumentation;

public class Agent {

    public static void premain(String args, Instrumentation instrumentation) {
        DynamicClassLoader dynamicClassLoader = new DynamicClassLoader("/target/classes", instrumentation);
        FileWatcher fileWatcher = new FileWatcher(dynamicClassLoader);
        fileWatcher.addPath("/src/")
            .addPath(dynamicClassLoader.getClassOutputPath());

        Thread thread = new Thread(fileWatcher, "FileWatcher-Thread");
        thread.setDaemon(true);
        thread.start();
    }
}
