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

        ClassRealm apiRealm = world.newRealm("api");
        apiRealm.addURL(new URL(basePath + "/ca/ibodrov/classworlds-test-plugin-api/1.0-SNAPSHOT/classworlds-test-plugin-api-1.0-SNAPSHOT.jar"));

        ClassRealm blueRealm = world.newRealm("blue");
        blueRealm.importFrom("api", "ca.ibodrov.classworldstest.api");
        blueRealm.addURL(new URL(basePath + "/ca/ibodrov/classworlds-test-plugin-blue/1.0-SNAPSHOT/classworlds-test-plugin-blue-1.0-SNAPSHOT.jar"));

        Class<Task> klass = (Class<Task>) blueRealm.loadClass("ca.ibodrov.classworldstest.plugin.blue.BlueTask");
        Task task = klass.newInstance();
        Thread.currentThread().setContextClassLoader(blueRealm);

        task.run("hi!");
    }
}
