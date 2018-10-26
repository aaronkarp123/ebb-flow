// The Flock (a list of Boid objects)

class Flock {
  ArrayList<Boid> boids; // An ArrayList for all the boids

  Flock() {
    boids = new ArrayList<Boid>(); // Initialize the ArrayList
  }

  void run(Predator p) {
    for (Boid b : boids) {
      if (p != null)
        b.repelForce(p.position);
      b.run(boids);  // Passing the entire list of boids to each boid individually
    }
  }

  void addBoid(Boid b) {
    boids.add(b);
  }
  
  boolean removeBoid()  {
    if (boids.size() > 0){
      boids.remove(int(random(boids.size())));
      return true;
    }
    return false;
  }

}
