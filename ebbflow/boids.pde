
// The Boid class

class Boid {

  PVector position;
  PVector velocity;
  PVector acceleration;
  float r;
  float maxforce;    // Maximum steering force
  float maxspeed;    // Maximum speed
  float red = random(221,254);
  float green = random(221,254);
  float blue = random(221,254);
  float rdif = random(-0.15,0.15);
  float bdif = random(-0.15,0.15);
  float gdif = random(-0.15,0.15);
  float sep_s;
  float ali_s;
  float coh_s;
  float grav_s = 1.0;
  float repel_r;
  float repel_s;
  float neighbordist;
  float grav_dist_fac;
  boolean colored = true;

    Boid(float x, float y) {
    acceleration = new PVector(0, 0);

    // This is a new PVector method not yet implemented in JS
    velocity = PVector.random2D();

    // Leaving the code temporarily this way so that this example runs in JS
    float angle = random(TWO_PI);
    velocity = new PVector(cos(angle), sin(angle));

    position = new PVector(x, y);
    r = random(0.5) + 0.75;
    sep_s = random(0.2) * r + 1.7;
    ali_s = random(0.2) * r + 1.1;
    coh_s = random(0.2) * r + 1.3;
    repel_s = random(0.2) * 1/r + 0.9;
    repel_r = random(50) + 25;
    maxspeed = 2; //2;
    maxforce = 0.03;
    neighbordist = random(10) + 45;
    grav_dist_fac = 1.0;
  }

  void run(ArrayList<Boid> boids) {
    flock(boids);
    update();
    //borders();
    render();
  }

  void applyForce(PVector force) {
    // We could add mass here if we want A = F / M
    acceleration.add(force);
  }
  
  PVector gravitate(){
    float area =r/10.0;
    float G = 0.001;
    PVector grav = new PVector(1,0);   
    grav.setMag( G*(area) * constrain(dist(position.x, position.y, grav_x, grav_y), 1, 100000) );                           
    grav.rotate(atan2(grav_y- position.y, grav_x - position.x ));
    //grav_dist_fac = max(1.0, min(4.0, 1.0 / (grav.mag() * grav.mag() * 10000.0)));
    return grav;
  }

  // We accumulate a new acceleration each time based on three rules
  void flock(ArrayList<Boid> boids) {
    PVector sep = separate(boids);   // Separation
    PVector ali = align(boids);      // Alignment
    PVector coh = cohesion(boids);   // Cohesion'
    PVector grav = gravitate();
    // Arbitrarily weight these forces
    sep.mult(sep_s * forces[0]);  // center at 1.8
    ali.mult(ali_s * forces[1]);  // center at 1.2
    coh.mult(coh_s * forces[2]);  // center at 1.4
    grav.mult(grav_s * forces[3]);  // center at 1.0
    // Add the force vectors to acceleration
    applyForce(sep);
    applyForce(ali);
    applyForce(coh);
    applyForce(grav);
  }

  // Method to update position
  void update() {
    // Update velocity
    velocity.add(acceleration);
    // Limit speed
    velocity.limit(maxspeed);
    position.add(velocity);
    if (position.x < -1*width*2)
      position.x = width*3;
    else if (position.x > width*3)
      position.x = -1*width*2;
    if (position.y < -1*height*2)
      position.y = height*3;
    else if (position.y > height*3)
      position.y = -1*height*2;
    // Reset accelertion to 0 each cycle
    acceleration.mult(0);
  }

  // A method that calculates and applies a steering force towards a target
  // STEER = DESIRED MINUS VELOCITY
  PVector seek(PVector target) {
    PVector desired = PVector.sub(target, position);  // A vector pointing from the position to the target
    // Scale to maximum speed
    desired.normalize();
    desired.mult(maxspeed);

    // Above two lines of code below could be condensed with new PVector setMag() method
    // Not using this method until Processing.js catches up
    // desired.setMag(maxspeed);

    // Steering = Desired minus Velocity
    PVector steer = PVector.sub(desired, velocity);
    steer.limit(maxforce);  // Limit to maximum steering force
    return steer;
  }

  void render() {
    // Draw a triangle rotated in the direction of velocity
    float theta = velocity.heading2D() + radians(90);
    // heading2D() above is now heading() but leaving old syntax until Processing.js catches up
    
    if (colored){
      red += rdif;
      blue += bdif;
      green += gdif;
      if (red < 212 || red > 255){
        rdif *= -1;
      }
      if (green < 212 || green > 255){
        gdif *= -1;
      }
      if (blue < 212 || blue > 255){
        bdif *= -1;
      }
    }
    else{
      red = 255;
      green = 255;
      blue = 255;
    }
    fill(red, green, blue);
    stroke(red, green, blue);
    pushMatrix();
    translate(position.x, position.y);
    rotate(theta);
    
    beginShape(TRIANGLES);
    vertex(0, -r*2*grav_dist_fac);
    vertex(-r*grav_dist_fac, r*2*grav_dist_fac);
    vertex(r*grav_dist_fac, r*2*grav_dist_fac);
    endShape();
    //ellipse(0, 0, r, r);
    popMatrix();
  }

  // Wraparound
  void borders() {
    if (position.x < -r) position.x = width+r;
    if (position.y < -r) position.y = height+r;
    if (position.x > width+r) position.x = -r;
    if (position.y > height+r) position.y = -r;
  }

  // Separation
  // Method checks for nearby boids and steers away
  PVector separate (ArrayList<Boid> boids) {
    float desiredseparation = 25.0f;
    PVector steer = new PVector(0, 0, 0);
    int count = 0;
    // For every boid in the system, check if it's too close
    for (Boid other : boids) {
      float d = PVector.dist(position, other.position);
      // If the distance is greater than 0 and less than an arbitrary amount (0 when you are yourself)
      if ((d > 0) && (d < desiredseparation)) {
        // Calculate vector pointing away from neighbor
        PVector diff = PVector.sub(position, other.position);
        diff.normalize();
        diff.div(d);        // Weight by distance
        steer.add(diff);
        count++;            // Keep track of how many
      }
    }
    // Average -- divide by how many
    if (count > 0) {
      steer.div((float)count);
    }

    // As long as the vector is greater than 0
    if (steer.mag() > 0) {
      // First two lines of code below could be condensed with new PVector setMag() method
      // Not using this method until Processing.js catches up
      // steer.setMag(maxspeed);

      // Implement Reynolds: Steering = Desired - Velocity
      steer.normalize();
      steer.mult(maxspeed);
      steer.sub(velocity);
      steer.limit(maxforce);
    }
    return steer;
  }

  // Alignment
  // For every nearby boid in the system, calculate the average velocity
  PVector align (ArrayList<Boid> boids) {
    PVector sum = new PVector(0, 0);
    int count = 0;
    for (Boid other : boids) {
      float d = PVector.dist(position, other.position);
      if ((d > 0) && (d < neighbordist)) {
        sum.add(other.velocity);
        count++;
      }
    }
    if (count > 0) {
      sum.div((float)count);
      // First two lines of code below could be condensed with new PVector setMag() method
      // Not using this method until Processing.js catches up
      // sum.setMag(maxspeed);

      // Implement Reynolds: Steering = Desired - Velocity
      sum.normalize();
      sum.mult(maxspeed);
      PVector steer = PVector.sub(sum, velocity);
      steer.limit(maxforce);
      return steer;
    } 
    else {
      return new PVector(0, 0);
    }
  }

  // Cohesion
  // For the average position (i.e. center) of all nearby boids, calculate steering vector towards that position
  PVector cohesion (ArrayList<Boid> boids) {
    PVector sum = new PVector(0, 0);   // Start with empty vector to accumulate all positions
    int count = 0;
    for (Boid other : boids) {
      float d = PVector.dist(position, other.position);
      if ((d > 0) && (d < neighbordist)) {
        sum.add(other.position); // Add position
        count++;
      }
    }
    if (count > 0) {
      sum.div(count);
      return seek(sum);  // Steer towards the position
    } 
    else {
      return new PVector(0, 0);
    }
  }
  
  void repelForce(PVector obstacle) {
    //Force that drives boid away from obstacle.

    PVector futPos = PVector.add(position, velocity); //Calculate future position for more effective behavior.
    PVector dist = PVector.sub(obstacle, futPos);
    float d = dist.mag();


    if (d<=repel_r) {
      PVector repelVec = PVector.sub(position, obstacle);
      repelVec.normalize();
      if (d != 0) { //Don't divide by zero.
        float scale = 1.0/d; //The closer to the obstacle, the stronger the force.
        repelVec.normalize();
        repelVec.mult(maxforce*7);
        if (repelVec.mag()<0) { //Don't let the boids turn around to avoid the obstacle.
          repelVec.y = 0;
        }
      }
      repelVec.mult(repel_s);
      applyForce(repelVec);
    }
  }
}
