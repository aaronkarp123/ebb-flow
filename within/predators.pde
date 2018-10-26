

// the Predator class

class Predator extends Boid { //Predators are just boids with some extra characteristics.
  float maxForce = 10; //Predators are better at steering.
  int times_run = 0;
  boolean to_run = false;
  int to_run_time = 10;
  
  Predator(PVector location, int scope) {
    super(location.x, location.y);
    r = int(random(3, 6)); //Predators are bigger and have more mass.
    maxspeed = 2.5;
    red = 255;
    green = 0;
    blue = 0;
    grav_s = 0.15;
    sep_s = 0.5;
    coh_s = 1.8;
    ali_s = 0.0;
    neighbordist = 100;
  }
  
  void run(ArrayList<Boid> boids) {
    times_run++;
    if(times_run % to_run_time == 0){
      times_run = 0;
      to_run_time = int(random(20) + 5);
      to_run = !to_run;
    }
    if(to_run){
        sep_s = 0.5;
        coh_s = 1.8;
        ali_s = 0.0;
    }
    else{
        sep_s = 0.0;
        coh_s = 0.0;
        ali_s = 0.0;
    }
    flock(boids);
    update();
    //borders();
    render();
  }

  void display() {
    update();
    fill(255, 140, 130);
    noStroke();
    ellipse(position.x, position.y, r, r);
  }
}
