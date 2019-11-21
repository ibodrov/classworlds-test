package ca.ibodrov.classworldstest.runner;

import ca.ibodrov.classworldstest.api.Task;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.junit.Test;

import java.net.URL;

public class RunnerTest {

    @Test
    @SuppressWarnings("unchecked")
    public void test() throws Exception {
        String basePath = "file://" + System.getProperty("user.home") + "/.m2/repository";

        ClassWorld world = new ClassWorld();

        ClassRealm blueRealm = world.newRealm("blue");
        blueRealm.addURL(new URL(basePath + "/com/google/guava/guava/28.1-jre/guava-28.1-jre.jar"));
        blueRealm.addURL(new URL(basePath + "/ca/ibodrov/classworlds-test-plugin-blue/1.0-SNAPSHOT/classworlds-test-plugin-blue-1.0-SNAPSHOT.jar"));

        Class<Task> klass = (Class<Task>) blueRealm.loadClass("ca.ibodrov.classworldstest.plugin.blue.BlueTask");
        Task task = klass.newInstance();
        Thread.currentThread().setContextClassLoader(blueRealm);

        task.run("hi!");
    }
}
