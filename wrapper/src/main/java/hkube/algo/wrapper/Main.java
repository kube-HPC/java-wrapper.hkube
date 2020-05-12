package hkube.algo.wrapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;

class Main {
    private static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) {
        try {
            if(args.length<1){
                throw new RuntimeException("Must provide an argument pointing to Algorithm jar location");
            }
            File jarFile = new File(args[0]);
            if (!jarFile.exists()){
                throw new RuntimeException("Algorithm jar file not found under " + jarFile.getAbsolutePath());
            }
            URL jarURL = new URL("file","",jarFile.getAbsolutePath());
            URLClassLoader ucl = URLClassLoader.newInstance(new URL[]{jarURL},Main.class.getClassLoader());
            Config conf = new Config();
            Class<?> clazz = ucl.loadClass(conf.getAlgorithmClassName());
            Constructor<?> ctor = clazz.getConstructor();
            IAlgorithm algorithm = (IAlgorithm) ctor.newInstance(new Object[]{});
            new Wrapper(algorithm);

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
        System.out.println("Hello, World.");
    }
}