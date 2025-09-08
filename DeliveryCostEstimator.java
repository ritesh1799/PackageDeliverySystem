import java.util.*;

class Package {
    String id;
    double weight;
    double distance;
    String offerCode;
    double discount;
    double cost;
    double deliveryTime;

    public Package(String id, double weight, double distance, String offerCode) {
        this.id = id;
        this.weight = weight;
        this.distance = distance;
        this.offerCode = offerCode;
    }
}

class Offer {
    String code;
    double minWeight, maxWeight, minDistance, maxDistance;
    double discountPercent;

    public Offer(String code, double minWeight, double maxWeight,
                 double minDistance, double maxDistance, double discountPercent) {
        this.code = code;
        this.minWeight = minWeight;
        this.maxWeight = maxWeight;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.discountPercent = discountPercent;
    }

    public boolean isApplicable(Package pkg) {
        return (pkg.weight >= minWeight && pkg.weight <= maxWeight &&
                pkg.distance >= minDistance && pkg.distance <= maxDistance);
    }
}

class Vehicle {
    double availableAt;
    int id;

    public Vehicle(int id) {
        this.id = id;
        this.availableAt = 0.0;
    }
}

public class DeliveryCostEstimator {

    private static final double COST_PER_KG = 10;
    private static final double COST_PER_KM = 5;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // Input base cost
        System.out.print("Enter base delivery cost: ");
        double baseCost = sc.nextDouble();

        // Input number of packages
        System.out.print("Enter number of packages: ");
        int n = sc.nextInt();

        // Input package details
        List<Package> inputPackages = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            System.out.print("Enter package details (id weight distance offerCode): ");
            String id = sc.next();
            double weight = sc.nextDouble();
            double distance = sc.nextDouble();
            String offerCode = sc.next();
            Package pkg = new Package(id, weight, distance, offerCode);
            inputPackages.add(new Package(id, weight, distance, offerCode));
        }

        List<Package> packages = new ArrayList<>(inputPackages);

        // Input vehicle info
        System.out.print("Enter number of vehicles: ");
        int numVehicles = sc.nextInt();
        System.out.print("Enter max speed of vehicles: ");
        double maxSpeed = sc.nextDouble();
        System.out.print("Enter max weight each vehicle can carry: ");
        double maxWeight = sc.nextDouble();

        // Initialize offers
        List<Offer> offers = Arrays.asList(
                new Offer("OFR001", 70, 200, 0, 199, 10),
                new Offer("OFR002", 100, 250, 50, 150, 7),
                new Offer("OFR003", 10, 150, 50, 250, 5)
        );

        // Step 1: calculate cost and discount for each package
        for (Package pkg : packages) {
            double cost = baseCost + (pkg.weight * COST_PER_KG) + (pkg.distance * COST_PER_KM);
            double discount = 0;
            for (Offer offer : offers) {
                if (offer.code.equals(pkg.offerCode) && offer.isApplicable(pkg)) {
                    discount = (offer.discountPercent / 100) * cost;
                    break;
                }
            }
            pkg.discount = discount;
            pkg.cost = cost - discount;
        }

        // Step 2: Initialize vehicles
        PriorityQueue<Vehicle> vehicleQueue =
                new PriorityQueue<>(Comparator.comparingDouble(v -> v.availableAt));
        for (int i = 1; i <= numVehicles; i++) {
            vehicleQueue.offer(new Vehicle(i));
        }



        // Step 3: Assign shipments
        List<Package> pending = new ArrayList<>(packages);

        // Step 4: sort packages
        packages.sort((a, b) -> Double.compare(b.weight, a.weight));

        while (!pending.isEmpty()) {
            Vehicle vehicle = vehicleQueue.poll();
            List<Package> shipment = selectShipment(pending, maxWeight);
            double maxDistance = shipment.stream().mapToDouble(p -> p.distance).max().orElse(0.0);

            for (Package pkg : shipment) {
                double travelTime = pkg.distance / maxSpeed;
                pkg.deliveryTime = truncate2(vehicle.availableAt + travelTime);
            }

            vehicle.availableAt =  truncate2(vehicle.availableAt + 2 * (maxDistance / maxSpeed));
            vehicleQueue.offer(vehicle);
            pending.removeAll(shipment);
        }

        // Step 5: Print results
        System.out.println("\n--- Delivery Results ---");
        for (Package pkg : inputPackages) {
            System.out.printf("%s %.0f %.0f %.2f%n",
                    pkg.id, pkg.discount, pkg.cost, truncate2(pkg.deliveryTime));

        }

    }

    private static double truncate2(double value) {
        return Math.floor(value * 100) / 100.0;
    }


    private static List<Package> selectShipment(List<Package> pending, double maxWeight) {
        List<Package> bestShipment = new ArrayList<>();
        int n = pending.size();
        int subsets = 1 << n;

        for (int mask = 1; mask < subsets; mask++) {
            List<Package> candidate = new ArrayList<>();
            double totalWeight = 0;
            for (int j = 0; j < n; j++) {
                if ((mask & (1 << j)) > 0) {
                    totalWeight += pending.get(j).weight;
                    candidate.add(pending.get(j));
                }
            }
            if (totalWeight <= maxWeight && isBetter(candidate, bestShipment)) {
                bestShipment = candidate;
            }
        }
        return bestShipment;
    }

    private static boolean isBetter(List<Package> cand, List<Package> best) {
        if (best.isEmpty()) return true;
        if (cand.size() > best.size()) return true;
        if (cand.size() < best.size()) return false;

        double candWeight = cand.stream().mapToDouble(p -> p.weight).sum();
        double bestWeight = best.stream().mapToDouble(p -> p.weight).sum();

        if (candWeight > bestWeight) return true;
        if (candWeight < bestWeight) return false;

        double candDist = cand.stream().mapToDouble(p -> p.distance).max().orElse(0.0);
        double bestDist = best.stream().mapToDouble(p -> p.distance).max().orElse(0.0);

        return candDist < bestDist;
    }
}
