package hkube.algo.wrapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.lookup.MainMapLookup;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;

class Main {
    private static Logger logger;

    public static void main(String[] args) {
        String logLevel;
        String debugEnabled = System.getenv("DEBUG_ENABLED");
        if (debugEnabled != null && (debugEnabled.toLowerCase().equals("true"))) {
            logLevel = "DEBUG";
        } else {
            logLevel = "INFO";
        }
        MainMapLookup.setMainArguments(new String[]{logLevel});
        logger = LogManager.getLogger();

        logger.debug("debug logging enabled");
        try {
            if (args.length < 1) {
                throw new RuntimeException("Must provide a program argument pointing to an Algorithm jar location.\n If you are trying to debug locally and you added the Algorithm to the class path manually, please put the word 'debug' as a program argument");
            }
            URLClassLoader ucl;
            if (!args[0].equals("debug")) {
                File jarFile = new File(args[0]);
                if (!jarFile.exists()) {
                    throw new RuntimeException("Algorithm jar file not found under " + jarFile.getAbsolutePath());
                }
                URL jarURL = new URL("file", "", jarFile.getAbsolutePath());

                ucl = URLClassLoader.newInstance(new URL[]{jarURL}, Main.class.getClassLoader());
            } else {
                logger.warn("Running in debug mode");
                ucl = URLClassLoader.newInstance(new URL[]{}, Main.class.getClassLoader());
                Wrapper.setDebugMode();
            }
            WrapperConfig conf = new WrapperConfig();
            Class<?> clazz = ucl.loadClass(conf.getAlgorithmClassName());
            Constructor<?> ctor = clazz.getConstructor();
            IAlgorithm algorithm = (IAlgorithm) ctor.newInstance(new Object[]{});
            new Wrapper(algorithm, conf);
            while (true) {
                System.in.read();
            }
        } catch (IOException | ClassNotFoundException | NoSuchMethodException ex) {
            logger.error(ex);
        } catch (IllegalAccessException ex) {
            logger.error(ex);
        } catch (InstantiationException ex) {
            logger.error(ex);
        } catch (InvocationTargetException ex) {
            logger.error(ex);
        }
    }
}