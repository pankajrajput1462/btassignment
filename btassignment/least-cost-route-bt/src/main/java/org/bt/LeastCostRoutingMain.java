package org.bt;

import lombok.extern.slf4j.Slf4j;
import org.bt.exception.LeastCostRouteException;
import org.bt.model.LeastCostRoute;
import org.bt.model.Person;

import java.io.IOException;

@Slf4j
public class LeastCostRoutingMain {

    public static void main(String[] args) throws LeastCostRouteException, IOException {
        Least least = new Least();
        Person sourcePerson = new Person();
        sourcePerson.setPerson(args[0]);
        Person destination = new Person();
        destination.setPerson(args[1]);
        LeastCostRoute leastCostRoute = least.findLeastCostRoute(sourcePerson, destination);
        log.info("Route: {}  Price: {}", leastCostRoute.getRoute(), leastCostRoute.getPrice());
    }
}
