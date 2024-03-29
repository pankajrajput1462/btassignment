package org.bt;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.opencsv.bean.MappingStrategy;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.bt.exception.LeastCostRouteException;
import org.bt.model.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class Least {
    private final String SRC_PATH = "src/main/resources/";
    private LeastCostRoute leastCostRoute;

    private String sourceExchange;
    private String destinationExchange;
    private List<String> routes;

    public LeastCostRoute findLeastCostRoute(String sourcePersonName, String destinationPersonName) throws LeastCostRouteException, IOException {

        log.info("Initializing data structures for source person: {} and destination person: {}", sourcePersonName, destinationPersonName);
        initializeDataStructures(sourcePersonName, destinationPersonName);
        calculateLeastCostRoute();
        return leastCostRoute;
    }

    public LeastCostRoute findLeastCostRoute(Person sourcePerson, Person destinationPerson) throws LeastCostRouteException, IOException {

        log.info("Initializing data structures for source person: {} and destination person: {}", sourcePerson.getPerson(), destinationPerson.getPerson());
        initializeDataStructures(sourcePerson.getPerson(), destinationPerson.getPerson());
        calculateLeastCostRoute();
        return leastCostRoute;
    }

    private void initializeDataStructures(String sourcePersonName, String destinationPersonName) throws LeastCostRouteException, IOException {

        List<Person> persons = readCsvFile(SRC_PATH + "data/Person.csv", Person.class);
        log.info("Successfully read and parsed the CSV file for persons.");

        log.info("Validating source person: {}", sourcePersonName);
        sourceExchange = validatePersonAndFindExchange(sourcePersonName, persons);
        log.info("message=Source Person are valid");

        log.info("Validating source person: {}", sourcePersonName);
        destinationExchange = validatePersonAndFindExchange(destinationPersonName, persons);
        log.info("message=Destination Person is valid");

        List<Route> route = readCsvFile(SRC_PATH + "data/Route.csv", Route.class);
        log.info("Successfully read and parsed the CSV file for route.");
        routes = getRoute(route, sourceExchange, destinationExchange);
    }

    private List<String> getRoute(List<Route> route, String source, String destination) {

        HashMap<String, List<String>> resultMap = new HashMap<>();
        for (Route routeBean : route) {
            String exchange = routeBean.getExchange();
            String path = routeBean.getPath();

            resultMap.computeIfAbsent(exchange, k -> new ArrayList<>()).add(path);
        }
        String key = ExchangeMapping.getExchange(source, destination);
        return resultMap.get(key);
    }

    private void calculateLeastCostRoute() throws LeastCostRouteException, IOException {
        if (!CollectionUtils.isEmpty(routes)) {
            log.info("Routes: {}", routes);
            Map<String[], Long> routeAndCost = calculateRouteAndCost(routes, readCsvFile(SRC_PATH + "data/NetworkElement.csv", NetworkElement.class),
                    readCsvFile(SRC_PATH + "data/Link.csv", Link.class), readCsvFile(SRC_PATH + "data/Network.csv", Network.class));


            Map.Entry<String[], Long> minCost = findMinimumCost(routeAndCost);
            if (minCost != null) {
                leastCostRoute = new LeastCostRoute(arrayToString(minCost.getKey()), minCost.getValue());
            }
            log.info("Calculated least cost:\n Route: {} Price: {}", leastCostRoute.getRoute(),
                    leastCostRoute.getPrice());
        } else {
            throw new LeastCostRouteException(LeastCostRouteException.ROUTE_NOT_FOUND);
        }

    }

    private Map.Entry<String[], Long> findMinimumCost(Map<String[], Long> routeAndCost) {
        return routeAndCost.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .orElse(null);
    }

    private Map<String[], Long> calculateRouteAndCost(List<String> routes, List<NetworkElement> networkElements, List<Link> links, List<Network> networks) {

        Map<String[], Long> routeAndCost = new HashMap<>();
        Map<String, Long> processingTimeMap = networkElements.stream()
                .collect(Collectors.toMap(NetworkElement::getName, NetworkElement::getProcessingTime));

        List<String[]> networksInEachRoute = routes.stream().map(values -> values.split(","))
                .toList();

        log.info("Initiating cost calculation for all routes.");
        for (String[] path : networksInEachRoute) {
            log.info("Calculating route and cost for networks: {}", Arrays.toString(path));
            Long costOfRoute = calculateCostOfRoute(path, processingTimeMap, links, networks);
            routeAndCost.put(path, costOfRoute);

        }
        return routeAndCost;

    }

    private Long calculateCostOfRoute(String[] path, Map<String, Long> processingTimeMap, List<Link> links, List<Network> networks) {
        //Cost of a Route = (5X Total Processing Time) + (2 X Total Price)


        List<Long> calPrice = new ArrayList<>();

        for (int i = 0; i < path.length - 1; i++) {
            String sourceNetwork = path[i];
            String destinationNetwork = path[i + 1];
            String link = findLinkForNetworks(sourceNetwork, destinationNetwork, networks);

            log.info("Calculating cost for link: {}", link);
            Long price = findPriceForLink(links, link);
            calPrice.add(price);
        }

        Long totalProcessingTime = calculateTotalProcessingTime(path, processingTimeMap);
        long totalPrice = calPrice.stream().mapToLong(Long::longValue).sum();

        return (5 * totalProcessingTime) + (2 * totalPrice);
    }

    private String findLinkForNetworks(String sourceNetwork, String destinationNetwork, List<Network> networks) {
        return networks.stream()
                .filter(x -> (x.getSourceNetworkElement().equals(sourceNetwork) && x.getDestinationNetworkElement().equals(destinationNetwork)) ||
                        (x.getSourceNetworkElement().equals(destinationNetwork) && x.getDestinationNetworkElement().equals(sourceNetwork)))
                .map(Network::getLink)
                .findFirst()
                .orElse(null);
    }

    private Long findPriceForLink(List<Link> links, String link) {
        return links.stream()
                .filter(l -> l.getName().equals(link))
                .mapToLong(Link::getPrice)
                .findFirst()
                .orElse(0L);
    }

    private Long calculateTotalProcessingTime(String[] path, Map<String, Long> processingTimeMap) {
        return Arrays.stream(path)
                .filter(processingTimeMap::containsKey)
                .mapToLong(processingTimeMap::get)
                .sum();
    }


    private String validatePersonAndFindExchange(String personName, List<Person> persons) throws LeastCostRouteException {

        return persons.stream().filter(i -> i.getPerson().equals(personName))
                .map(Person::getExchange).findFirst()
                .orElseThrow(() -> new LeastCostRouteException(LeastCostRouteException.PERSON_NAME_NOT_VALID));

    }

    @SneakyThrows
    public <T> List<T> readCsvFile(String filePath, Class<T> classBean) throws LeastCostRouteException {
        Path path = Paths.get(filePath);
        InputStream inputStream = Files.newInputStream(path);
        try (Reader csvReader = new InputStreamReader(inputStream);
             CSVReader reader = new CSVReaderBuilder(csvReader).withCSVParser(new CSVParserBuilder().build()).build()) {

            CsvToBean<T> bean = new CsvToBean<>();
            MappingStrategy<T> mappingStrategy = new HeaderColumnNameMappingStrategy<>();
            mappingStrategy.setType(classBean);
            bean.setMappingStrategy(mappingStrategy);
            bean.setCsvReader(reader);
            return bean.parse();
        } catch (IOException e) {
            log.error("Error reading CSV file: {}", filePath, e);
            throw new LeastCostRouteException(LeastCostRouteException.FILE_READ_ERROR, e);
        }
    }

    private static String arrayToString(String[] array) {
        return String.join("->", array);
    }
}
                