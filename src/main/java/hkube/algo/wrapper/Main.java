package hkube.algo.wrapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

class Main {
    private static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) {
        try {
            Config conf = new Config();
            Class<?> clazz = Class.forName(conf.getAlgorithmClassName());
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