package mg.matsd;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.instrument.Instrumentation;

public class Agent {
    private static final Logger LOGGER = LogManager.getLogger(Agent.class);

    public static void premain(String args, Instrumentation instrumentation) {
        LOGGER.info("Démarrage de l'agent hot-reloading...");

        DynamicClassLoader dynamicClassLoader = new DynamicClassLoader("target/classes", instrumentation);
        FileWatcher fileWatcher = new FileWatcher(dynamicClassLoader)
            .addPath("src/");

        Thread thread = new Thread(fileWatcher, "FileWatcher-Thread");
        thread.setDaemon(true);
        thread.start();

        LOGGER.info("Agent hot-reloading démarré avec succès");
    }
}
