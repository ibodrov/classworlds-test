package ca.ibodrov.classworldstest.plugin.blue;

import ca.ibodrov.classworldstest.api.Task;
import com.google.common.graph.GraphBuilder;

public class BlueTask implements Task {

    @Override
    public void run(Object o) {
        System.out.println("This class' classloader: " + this.getClass().getClassLoader());

        // call something from Guava 28.x
        System.out.println("!! " + o + GraphBuilder.directed().immutable().build());
    }
}
