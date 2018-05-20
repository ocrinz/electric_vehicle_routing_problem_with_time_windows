package at.ac.tuwien.otl.ss18.pwlk.constructionHeuristic.impl;

import at.ac.tuwien.otl.ss18.pwlk.distance.DistanceHolder;
import at.ac.tuwien.otl.ss18.pwlk.exceptions.BatteryViolationException;
import at.ac.tuwien.otl.ss18.pwlk.exceptions.EvrptwRunException;
import at.ac.tuwien.otl.ss18.pwlk.exceptions.TimewindowViolationException;
import at.ac.tuwien.otl.ss18.pwlk.util.Pair;
import at.ac.tuwien.otl.ss18.pwlk.valueobjects.AbstractNode;
import at.ac.tuwien.otl.ss18.pwlk.valueobjects.ProblemInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ModifyRoute {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private DistanceHolder distanceHolder;
  private ProblemInstance problemInstance;

  public ModifyRoute(DistanceHolder distanceHolder, ProblemInstance problemInstance) {
    this.distanceHolder = distanceHolder;
    this.problemInstance = problemInstance;
  }

  public Pair<Car, LinkedList<AbstractNode>> addChargingStation(Car car, LinkedList<AbstractNode> routeList) throws EvrptwRunException {
    Car newCar;
    LinkedList<AbstractNode> newRoute;

    try {
      newCar = car.cloneCar();
      newCar.driveRoute(routeList.subList(routeList.size()-2, routeList.size()));
      return new Pair(newCar, routeList);
    } catch (BatteryViolationException b) {
    } catch (TimewindowViolationException t) {
      throw new EvrptwRunException("Cannot continue creating route due to infeasibility: " + t.getLocalizedMessage());
    }

    List<Pair<AbstractNode, Double>> list = distanceHolder
            .getNearestRechargingStationsForCustomerInDistance(routeList.get(routeList.size()-1), routeList.get(routeList.size()-2));


    if (list.isEmpty()) {
      throw new EvrptwRunException("Customer is completely out of range");
    }

    logger.info("Must add charging station for pendel route after customer");

    for (Pair<AbstractNode, Double> chargingStations : list) {
      try {
        newCar = car.cloneCar();
        newRoute = (LinkedList<AbstractNode>) routeList.clone();
        newRoute.add(newRoute.size()-1, chargingStations.getKey());
        newCar.driveRoute(newRoute.subList(newRoute.size()-3, newRoute.size()));
        return new Pair(newCar, newRoute);
      } catch (BatteryViolationException b) {
      } catch (TimewindowViolationException t) {
      }
    }

    logger.info("Must add charging station for pendel route before customer");

    Collections.reverse(list);
    for (Pair<AbstractNode, Double> chargingStations : list) {
      try {
        newCar = new Car(problemInstance);
        newRoute = (LinkedList<AbstractNode>) routeList.clone();
        newRoute.add(newRoute.size()-2, chargingStations.getKey());
        newCar.driveRoute(newRoute);
        return new Pair(newCar, newRoute);
      } catch (BatteryViolationException b) {
      } catch (TimewindowViolationException t) {
      }

    }

    logger.info("Must add charging station before and after customer");

    List<Pair<AbstractNode, Double>> list2 = distanceHolder
            .getNearestRechargingStationsForCustomerInDistance(routeList.get(routeList.size()-1), routeList.get(routeList.size()-2));

    // was machen wir hier noch? 2 chargingstations?
    for (Pair<AbstractNode, Double> chargingStation1 : list2) {
      for (Pair<AbstractNode, Double> chargingStation2 : list) {
        try {
          newCar = new Car(problemInstance);
          newRoute = (LinkedList<AbstractNode>) routeList.clone();
          newRoute.add(newRoute.size()-2, chargingStation1.getKey());
          newRoute.add(newRoute.size()-1, chargingStation2.getKey());
          newCar.driveRoute(newRoute);
          return new Pair(newCar, newRoute);
        } catch (BatteryViolationException b) {
        } catch (TimewindowViolationException t) {
        }
      }
    }

    throw new EvrptwRunException("Creating pendel route was not possible");
  }

}
