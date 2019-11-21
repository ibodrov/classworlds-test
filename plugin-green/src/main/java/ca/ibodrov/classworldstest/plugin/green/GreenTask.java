package ca.ibodrov.classworldstest.plugin.green;

import ca.ibodrov.classworldstest.api.Task;
import com.google.common.graph.GraphBuilder;

public class GreenTask implements Task {

    @Override
    public void run(Object o) {
        // call something from Guava 27.x
        System.out.println("!! " + o + GraphBuilder.directed().build());
    }
}
