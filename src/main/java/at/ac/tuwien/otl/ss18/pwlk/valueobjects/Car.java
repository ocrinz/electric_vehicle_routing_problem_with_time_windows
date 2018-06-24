package at.ac.tuwien.otl.ss18.pwlk.valueobjects;

import at.ac.tuwien.otl.ss18.pwlk.distance.DistanceHolder;
import at.ac.tuwien.otl.ss18.pwlk.exceptions.BatteryViolationException;
import at.ac.tuwien.otl.ss18.pwlk.exceptions.TimewindowViolationException;

import java.util.List;

public class Car {
  private final ProblemInstance problemInstance;
  private final DistanceHolder distanceHolder;
  private double currentBatteryCapacity;
  private double currentTime;
  private double currentDistance;

  public Car(ProblemInstance problemInstance, DistanceHolder distanceHolder) {
    this.problemInstance = problemInstance;
    this.distanceHolder = distanceHolder;
    this.currentBatteryCapacity = problemInstance.getBatteryCapacity();
    this.currentTime = problemInstance.getDepot().getTimeWindow().getReadyTime();
    this.currentDistance = 0;
  }

  public Car cloneCar() {
    Car newCar = new Car(problemInstance, distanceHolder);
    newCar.currentBatteryCapacity = this.currentBatteryCapacity;
    newCar.currentTime = this.currentTime;
    newCar.currentDistance = this.currentDistance;

    return newCar;
  }

  public double getCurrentDistance() {
    return currentDistance;
  }

  public double getCurrentBatteryCapacity() {
    return currentBatteryCapacity;
  }

  private void chargeCar() {
    currentTime += problemInstance.getInverseRechargingRate()
            * (problemInstance.getBatteryCapacity() - currentBatteryCapacity); // wait till vehicle capacity is full
    currentBatteryCapacity = problemInstance.getBatteryCapacity(); // vehicle has now full capacity
  }

  private void moveCar(AbstractNode departure, AbstractNode arrival) throws BatteryViolationException, TimewindowViolationException {
    double distance = distanceHolder.getInterNodeDistance(departure, arrival);
    currentTime += distance / problemInstance.getAverageVelocity(); // travel time = distance/velocity
    currentBatteryCapacity -= problemInstance.getChargeConsumptionRate() * distance; // subtract used battery capacity
    currentDistance += distance; // add travelled distance to total travelled distance

    if (currentBatteryCapacity < 0) { // if battery was not enough to travel the distance throw exception
      throw new BatteryViolationException("Battery constraint was violated");
    }

    if (currentTime > arrival.getTimeWindow().getDueTime()) { // if arrival after latest arrival time throw exception
      throw new TimewindowViolationException("Time window was violated (arrival was to late on node "
              + arrival.getId()
              + ", window end: "
              + arrival.getTimeWindow().getDueTime()
              + ", arrival time: "
              + currentTime
              + ")");
    }

    if (currentTime < arrival.getTimeWindow().getReadyTime()) { // if arrival before ready time wait
      currentTime += arrival.getTimeWindow().getReadyTime() - currentTime; // add waiting time
    }

    currentTime += arrival.getServiceTime(); // add service time for service in arrival node

    if (arrival instanceof ChargingStations) { // if arrival node is a charging station then charge vehicle
      chargeCar();
    }

  }

  public void driveRoute(List<AbstractNode> route) throws BatteryViolationException, TimewindowViolationException {
    AbstractNode lastNode = null;
    for (AbstractNode abstractNode : route) {
      if (lastNode == null) {
        lastNode = abstractNode;
      } else {
        moveCar(lastNode, abstractNode);
        lastNode = abstractNode;
      }
    }
  }
}
