import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import hkube.algo.wrapper.IAlgorithm;
import hkube.algo.wrapper.Wrapper;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

class Main {
    private static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) {
        try {

            Class<?> clazz = Class.forName(args[0]);
        Constructor<?> ctor = clazz.getConstructor();

        IAlgorithm algorithm = (IAlgorithm) ctor.newInstance(new Object[] {  });
        new Wrapper(algorithm);

            while(true){
                System.in.read();
            }
        }catch(IOException | ClassNotFoundException | NoSuchMethodException ex){
            logger.error(ex);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        System.out.println("Hello, World.");
    }
}