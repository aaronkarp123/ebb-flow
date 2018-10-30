import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import oscP5.*; 
import netP5.*; 
import java.util.Collections; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class within extends PApplet {

// Sources...
// Flocking by Daniel Shiffman. https://processing.org/examples/flocking.html
// Flocking with predator by Abel Jnsm. https://www.openprocessing.org/sketch/126516
// Inspiration: Eli Stine and his Murmurator

// Written by Aaron Karp, 2018
// www.aaronmkarp.com
// aaronkarp123 @ gmail.com





OscP5 oscP5;

Flock flock;
Predator pred;
int grav_x;
int grav_y;
PVector grav_vel;
PVector grav_acc;
boolean grav_stop;
boolean show_grav = false;
static boolean NORTH, SOUTH, WEST, EAST;

int num_frames;

float back_color = 5;
float back_r = 5, back_g = 5, back_b = 5;
//float back_rd = random(-0.01,0.01), back_gd = random(-0.01,0.01), back_bd = random(-0.01,0.01);
float back_rd=0, back_gd=0, back_bd=0;
float back_trans = 256;
float grav_size = 0;
boolean pulse_train = false;
float pulse_train_c = 0;

//terrain
int cols, rows;
int scl = 8;
int w;
int h;
float[][] terrain;
float zoom = 1500.0f;

boolean pred_pres = false;

int num_birds = 0;
int num_birds_target = num_birds;

ArrayList<ArrayList<Float>> oscstack;
ArrayList<Integer> whereto;
int num_factors = 6;
float[] forces;  // sep - ali - coh - grav
float[] forces_target;

boolean started;

boolean terrain_back = false;

PFont font;

FuzzySmokeSystem[] smokes = new FuzzySmokeSystem[10];

float hour, minute, second;

public void setup() {
  //size(1000, 800, P3D);
  
  h=height;
  w=width;
  println(width + "x"+height);
  font = createFont("Arial Bold",48);
  num_frames = 0;
  started = false;
  oscP5 = new OscP5(this,12000);
  oscstack = new ArrayList<ArrayList<Float>>();
  whereto = new ArrayList<Integer>();
  for (int i = 0; i < num_factors; i++){
    oscstack.add(new ArrayList<Float>());
    whereto.add(i);
  }
  
  forces = new float[num_factors - 1];
  forces_target = new float[num_factors - 1];
  for (int i = 0; i < num_factors - 1; i++){
    forces[i] = 1.0f;
    forces_target[i] = 1.0f;
  }
  
  grav_vel = new PVector(0,0);
  grav_acc = new PVector(0,0);
  grav_stop = true;
  grav_size = width*4;
  
  //Collections.shuffle(whereto);  // Randomize Order
  flock = new Flock();
  pred = new Predator(new PVector(random(0, width), random(0, height)), 50);
  grav_x = width/2;
  grav_y = height/2;
  // Add an initial set of boids into the system
  for (int i = 0; i <num_birds; i++) {
    flock.addBoid(new Boid(grav_x, grav_y));
  }
  
  //terrain
  if (terrain_back){
    w = PApplet.parseInt(width*1.8f);
    h = PApplet.parseInt(height*1.8f);
    cols = w / scl;
    rows = h/ scl;
    terrain = new float[cols][rows];
  }
  
  for(int i = 0; i < smokes.length; i ++) {
    smokes[i] = new FuzzySmokeSystem(200, grav_x, grav_y);
  }
  
  hour = hour();
  minute = minute();
  second = second();
}

public void draw() {
  lights();
  
  hour = hour();
  minute = minute();
  second = second();
  
  if (hour >= 6.0f && hour < 18.0f ){
    textFont(font,60);
    // white float frameRate
    fill(0);
    rect(-width,-height,width * 3,height * 3);
    fill(255);
    text("Come Back At Sundown",width/2-400,height/2, 1);
    // gray int frameRate display:
    fill(255);
    text((int)(18 - hour - 1) + ":" + (int)(60-minute - 1) + ":" + (int)(60-second - 1),width/2 + 150,height/2 + 120, 1);
    return;
  }
  
  /*textFont(font,60);
  fill(0);
  rect(20,-25,250,100);
  // white float frameRate
  fill(255);
  text(frameRate,20,20, 1);
  // gray int frameRate display:
  fill(200);
  text(int(frameRate),20,60, 1);*/
  
  back_r += back_rd;
  back_g += back_gd;
  back_b += back_bd;
  if (back_r < 0 || back_r > 10){
    back_rd *= -1;
  }
  if (back_g < 0 || back_g > 10){
    back_gd *= -1;
  }
  if (back_b < 0 || back_b > 10){
    back_bd *= -1;
  }
  
  if (grav_stop){
    /*
    if (pulse_train && back_trans < 255){
      fill(back_r, back_g, back_b, min(back_trans, 255));
      back_trans ++;
    }
    else{
      fill(back_r, back_g, back_b, 256);
    }*/
    //fill(back_r, back_g, back_b, 256);
    if (grav_size < width*4){
      fill(back_r, back_g, back_b, back_trans);
      rect(-width,-height,width*3,height*3);
      noStroke();
      fill(back_r, back_g, back_b, 256);
      ellipse(grav_x, grav_y, grav_size,grav_size);
      grav_size += min(max(grav_size/23.0f, 3), 16);
    }
    else{
      fill(back_r, back_g, back_b, 256);
      rect(-width,-height,width*3,height*3);
    }
  }
  else{
    grav_size = 5;
    fill(back_r, back_g, back_b, back_trans);
    rect(-width,-height,width*3,height*3);
  }
  
  if (!started){
    return;
  }
  
  
  for(int i = 0; i < smokes.length; i ++) {
    smokes[i].draw(grav_x, grav_y);
    smokes[i].update();
  }
  
  
  num_frames++;
  
  if (forces[4] == 1.0f ){
    if (grav_stop){
        grav_stop = false;
        grav_acc.x = random(0,1) - 0.5f;
        grav_acc.y = random(0,1) - 0.5f;
        grav_acc.mult(0.1f);
        back_trans = random(1,128);
        pulse_train = true;
        pulse_train_c = 0;
    }
  }
  else
    grav_stop = true;
  if (grav_stop){
    grav_acc.x = grav_vel.x / abs(grav_vel.x) * -0.03f;
    grav_acc.y = grav_vel.y / abs(grav_vel.y) * -0.03f;
  }
  else if(num_frames % 400 == 0){
    grav_acc.x = random(0,1) - 0.5f;
    grav_acc.y = random(0,1) - 0.5f;
    grav_acc.mult(0.01f);
  }
  
  if (grav_stop){
    if (grav_acc.x / grav_vel.x > 0.0f)
      grav_acc.x *= -1;
    if (grav_acc.y / grav_vel.y > 0.0f)
      grav_acc.y *= -1;
    if (abs(grav_vel.x) <= 0.03f){
      grav_acc.x = 0.0f;
      grav_vel.x = 0.0f;
    }
    if (abs(grav_vel.y) <= 0.03f){
      grav_acc.y = 0.0f;
      grav_vel.y = 0.0f;
    }
  }
  
  grav_vel.add(grav_acc);
  grav_vel.limit(4);
  
  grav_x += grav_vel.x;
  grav_y += grav_vel.y;
  
  float xdif = width - grav_x;
  if ((abs(xdif) < 50 && grav_acc.x > 0.0f )|| (abs(xdif) > width-50 && grav_acc.x < 0.0f )){
      grav_acc.x*=-1.0f;
  }
  if ((abs(xdif) < 50 && grav_vel.x > 0.0f )|| (abs(xdif) > width-50 && grav_vel.x < 0.0f )){
      grav_vel.x*=-1.0f;
  }
  float ydif = height - grav_y;
  if ((abs(ydif) < 50 && grav_acc.y > 0.0f )|| (abs(ydif) > height-50 && grav_acc.y < 0.0f )){
      grav_acc.y*=-1.0f;
  }
  if ((abs(ydif) < 50 && grav_vel.y > 0.0f )|| (abs(ydif) > height-50 && grav_vel.y < 0.0f )){
      grav_vel.y*=-1.0f;
  }
  
  while(num_birds_target > num_birds){
    float x = 0, y = 0;
    if (random(1) > 0.5f)
      y = random(height) + height;
    else
      y = random(height) - height;
    if (random(1) > 0.5f)
      x = random(width) + width;
    else
      x = random(width) - width;
    
    flock.addBoid(new Boid(x,y));
    num_birds++;
  }
  while(num_birds_target < num_birds){
    if (flock.removeBoid())
      num_birds--;
  }
  
  for (int i = 0; i < 4; i++){
    if (forces[i] != forces_target[i]){
      if (forces[i] < forces_target[i])
        forces[i] += 0.5f;
      else
        forces[i] -= 0.5f;
    }
  }
  
  //terrain
  if (terrain_back){
    float yoff = grav_y/400.0f;
    for (int y = 0; y < rows; y++) {
      float xoff = grav_x/400.0f;
      for (int x = 0; x < cols; x++) {
        terrain[x][y] = map(noise(xoff, yoff), 0, 1, -100, 100);
        xoff += 0.05f;
      }
      yoff += 0.05f;
    }
  }
  
  stroke(255);
  translate(-w/2, -h/2, -101);
  translate(grav_x, grav_y);
  for (int y = 0; y < rows-1; y++) {
    beginShape(TRIANGLE_STRIP);
    for (int x = 0; x < cols; x++) {
      int c = color((terrain[x][y] + 10) * 3, (terrain[x][y] + 20) * 4, (terrain[x][y] +15) * 3);
      //stroke(c, 200);
      noStroke();
      fill(c, 200);
      vertex(x*scl, y*scl, terrain[x][y]);
      vertex(x*scl, (y+1)*scl, terrain[x][y+1]);
      //rect(x*scl, y*scl, scl, scl);
    }
    endShape();
  }
  
  if (keyPressed){
    if (key==CODED){
      if (keyCode == UP && !SOUTH) NORTH = true;
      if (keyCode == DOWN && !NORTH) SOUTH = true;
      if (keyCode == LEFT && !EAST) WEST = true;
      if (keyCode == RIGHT && !WEST) EAST = true;
    }
  }
  
  float distance = 3;
  if (NORTH)  grav_y -= distance;
  if (SOUTH)  grav_y += distance;
  if (WEST)   grav_x -= distance;
  if (EAST)   grav_x += distance;
  
  camera(width/2, height/2, zoom, // eyeX, eyeY, eyeZ
         width/2, height/2, 0.0f, // centerX, centerY, centerZ
         0.0f, 1.0f, 0.0f); // upX, upY, upZ
  
  if (show_grav){
    renderGrav(grav_x, grav_y, back_color);
    colorMode(RGB, 100);
  }
  
  if (pred_pres){
    flock.run(pred);
    pred.run(flock.boids);
  }
  else
    flock.run(null);
}

public void mouseWheel(MouseEvent event) {
  float e = event.getCount();
  //println(e);
  zoom=constrain(zoom-e/10, 110, 2000);
 
}

public void keyReleased() {
  redraw();   //  queue draw()

  final int k = keyCode;

  if      (k == UP    | k == 'W')   NORTH = false;
  else if (k == DOWN  | k == 'S')   SOUTH = false;
  else if (k == LEFT  | k == 'A')   WEST  = false;
  else if (k == RIGHT | k == 'D')   EAST  = false;
  
  if (k == ' ')
    pred_pres = !pred_pres;
}


// Add a new boid into the System
public void mousePressed() {
  /*flock.addBoid(new Boid(mouseX,mouseY));
  num_birds++;*/
}


/* incoming osc message are forwarded to the oscEvent method. */
public void oscEvent(OscMessage theOscMessage) {
  
  started = true;
  
  for (int i = 0; i < num_factors; i++){
    int to_get = whereto.get(i);
    oscstack.get(to_get).add(theOscMessage.get(i).floatValue());
    
    if (oscstack.get(to_get).size() > 10)
      oscstack.get(to_get).remove(0);
  }
  
  if (num_factors >= 5){
    for (int i = 0; i < num_factors-1; i++){
      float temp = oscstack.get(i).get(oscstack.size()-1);
      if (temp != forces[i] && temp!= forces_target[i]){
        forces[i] = temp;
        forces_target[i] = temp;
      }
    }
  }
  
  if (num_factors >= 6){
    num_birds_target = PApplet.parseInt(oscstack.get(5).get(oscstack.size()-1));
  }
}


public static boolean differentValues(ArrayList<Float> arr){
    for (int i = 0; i < arr.size()-1; i++) {
        for (int j = i+1; j < arr.size(); j++) {
             if (arr.get(i) != arr.get(j)) {
                 return false;
             }
        }
    }              
    return true;          
}

class FuzzySmokeParticle {
  float x, y;    // current position
  float vx, vy;  // velocity
  float timer;   // time left before estinguishing
  float dt = 0.5f;  // estinguishing speed
  float r = 5;   // radius
  boolean on=false;
  boolean to_return=true;
  int col, next_col;
  
  FuzzySmokeParticle(int c) { col = c; next_col = c;}

  // Put at original position, fully opaque, with random velocity
  public void initialize(float ox, float oy, int c)
  {
    col = next_col;
    on = true;
    x = ox; y = oy;
    // More vertical than horizontal
    //vx = random(-0.3,0.3); vy = random(-0.3,0.3)-1;
    vx = (float)randomGaussian()*0.2f;
    vy = (float)randomGaussian()*0.2f;
    timer = 255;
    dt = random(0.2f, 2.0f);
  }
  
  public void draw(PImage img)
  {   
    if(!on) return;
    imageMode(CORNER);
    tint(col,timer);
    image(img,x-img.width/2,y-img.height/2);
  }
  
  public void turn_off()
  {
    to_return = false;
  }
  
  public void turn_on()
  {
    to_return = true;
  }
  
  public void switch_on()
  {
    to_return = !to_return;
    if (to_return && timer <= 0){
      timer = random(0,255);
    }
  }
  
  public void update(float ox, float oy)
  {
    // initialize if necessary
    if(!on && to_return) { if(random(0,1) < 0.01f) initialize(ox-5+random(10),oy-5+random(10), col); return; }
    
    timer -= dt;    // decay the transparency
    x += vx;
    y += vy;
    // When exit screen or totally transparent, re-initialize
    if (timer < 0) {
      on = false;
    }
  }
  
  public void change_col(int c)
  {
    next_col = c;
  }
}



class FuzzySmokeSystem {
  FuzzySmokeParticle[] particles;   // the individual bits exploding
  int c;                     // common color
  float x;                     // common origin
  float y;                     // common origin
  float r = 10;                // source radius
  float transparency, dt=3;    // transparency decays over time
  boolean on=true;       
  PImage img;                  // smoke sprite
  
  FuzzySmokeSystem(int numParticles, float x0, float y0)
  {
    c = color(random(30),random(30), random(30), random(85));
    x = x0;
    y = y0;
    // Create the particles for the firework
    particles = new FuzzySmokeParticle[numParticles];
    for (int i=0; i<particles.length; i++) particles[i] = new FuzzySmokeParticle(c);
    
    // Create a mask, from the original sketch
    PImage msk = loadImage("texture.gif");
    img = new PImage(msk.width,msk.height);
    for (int i = 0; i < img.pixels.length; i++) img.pixels[i] = color(255);
    img.mask(msk);
  }
  
  public void draw(int gravx, int gravy)
  {
    stroke(c); noFill();
    strokeWeight(2);
    //ellipse(x,y,2*r,2*r);
    // Draw the individual particles with the shared color
    x = gravx;
    y = gravy;
    if (random(1) > 0.999f){
      on = !on;
      if(on){
        //x = random(width);
        //y = random(height);
        c = color(random(30),random(30), random(30), random(85));
        for (int i=0; i<particles.length; i++) particles[i].change_col(c);
      }
      for (int i=0; i<particles.length; i++) particles[i].switch_on();
    }
    for (int i=0; i<particles.length; i++) particles[i].draw(img);
  }

  public void update()
  {
    // Update the individual particles
    for (int i=0; i<particles.length; i++) particles[i].update(x,y);
  }  
}

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
  float rdif = random(-0.15f,0.15f);
  float bdif = random(-0.15f,0.15f);
  float gdif = random(-0.15f,0.15f);
  float sep_s;
  float ali_s;
  float coh_s;
  float grav_s = 1.0f;
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
    r = random(0.5f) + 0.75f;
    sep_s = random(0.2f) * r + 1.7f;
    ali_s = random(0.2f) * r + 1.1f;
    coh_s = random(0.2f) * r + 1.3f;
    repel_s = random(0.2f) * 1/r + 0.9f;
    repel_r = random(50) + 25;
    maxspeed = 2; //2;
    maxforce = 0.03f;
    neighbordist = random(10) + 45;
    grav_dist_fac = 1.0f;
  }

  public void run(ArrayList<Boid> boids) {
    flock(boids);
    update();
    //borders();
    render();
  }

  public void applyForce(PVector force) {
    // We could add mass here if we want A = F / M
    acceleration.add(force);
  }
  
  public PVector gravitate(){
    float area =r/10.0f;
    float G = 0.001f;
    PVector grav = new PVector(1,0);   
    grav.setMag( G*(area) * constrain(dist(position.x, position.y, grav_x, grav_y), 1, 100000) );                           
    grav.rotate(atan2(grav_y- position.y, grav_x - position.x ));
    //grav_dist_fac = max(1.0, min(4.0, 1.0 / (grav.mag() * grav.mag() * 10000.0)));
    return grav;
  }

  // We accumulate a new acceleration each time based on three rules
  public void flock(ArrayList<Boid> boids) {
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
  public void update() {
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
  public PVector seek(PVector target) {
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

  public void render() {
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
  public void borders() {
    if (position.x < -r) position.x = width+r;
    if (position.y < -r) position.y = height+r;
    if (position.x > width+r) position.x = -r;
    if (position.y > height+r) position.y = -r;
  }

  // Separation
  // Method checks for nearby boids and steers away
  public PVector separate (ArrayList<Boid> boids) {
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
  public PVector align (ArrayList<Boid> boids) {
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
  public PVector cohesion (ArrayList<Boid> boids) {
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
  
  public void repelForce(PVector obstacle) {
    //Force that drives boid away from obstacle.

    PVector futPos = PVector.add(position, velocity); //Calculate future position for more effective behavior.
    PVector dist = PVector.sub(obstacle, futPos);
    float d = dist.mag();


    if (d<=repel_r) {
      PVector repelVec = PVector.sub(position, obstacle);
      repelVec.normalize();
      if (d != 0) { //Don't divide by zero.
        float scale = 1.0f/d; //The closer to the obstacle, the stronger the force.
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
// The Flock (a list of Boid objects)

class Flock {
  ArrayList<Boid> boids; // An ArrayList for all the boids

  Flock() {
    boids = new ArrayList<Boid>(); // Initialize the ArrayList
  }

  public void run(Predator p) {
    for (Boid b : boids) {
      if (p != null)
        b.repelForce(p.position);
      b.run(boids);  // Passing the entire list of boids to each boid individually
    }
  }

  public void addBoid(Boid b) {
    boids.add(b);
  }
  
  public boolean removeBoid()  {
    if (boids.size() > 0){
      boids.remove(PApplet.parseInt(random(boids.size())));
      return true;
    }
    return false;
  }

}

int dim = 200;
float starting_h=0.0f;
float h_rate=0.05f;
float starting_h_rate=0.1f;

public void renderGrav(float x, float y, float basec) {
  int radius = dim/2;
  float h = starting_h;
  starting_h += starting_h_rate;
  if (starting_h > 20.0f || starting_h < 0.0f){
    starting_h_rate *= -1;
    starting_h += starting_h_rate;
  }
  colorMode(HSB, 360, 100, 100);
  ellipseMode(RADIUS);
  noStroke();
  for (int r = radius; r > 0; --r) {
    float scaledr = min(5.0f, 10.0f/(r*r*0.2f));
    fill(h *scaledr / 2.0f, h *scaledr/ 2.0f,h *scaledr/ 2.0f);
    ellipse(x, y, r, r);
    h += h_rate;
  }
}


// the Predator class

class Predator extends Boid { //Predators are just boids with some extra characteristics.
  float maxForce = 10; //Predators are better at steering.
  int times_run = 0;
  boolean to_run = false;
  int to_run_time = 10;
  
  Predator(PVector location, int scope) {
    super(location.x, location.y);
    r = PApplet.parseInt(random(3, 6)); //Predators are bigger and have more mass.
    maxspeed = 2.5f;
    red = 255;
    green = 0;
    blue = 0;
    grav_s = 0.15f;
    sep_s = 0.5f;
    coh_s = 1.8f;
    ali_s = 0.0f;
    neighbordist = 100;
  }
  
  public void run(ArrayList<Boid> boids) {
    times_run++;
    if(times_run % to_run_time == 0){
      times_run = 0;
      to_run_time = PApplet.parseInt(random(20) + 5);
      to_run = !to_run;
    }
    if(to_run){
        sep_s = 0.5f;
        coh_s = 1.8f;
        ali_s = 0.0f;
    }
    else{
        sep_s = 0.0f;
        coh_s = 0.0f;
        ali_s = 0.0f;
    }
    flock(boids);
    update();
    //borders();
    render();
  }

  public void display() {
    update();
    fill(255, 140, 130);
    noStroke();
    ellipse(position.x, position.y, r, r);
  }
}
  public void settings() {  fullScreen(P3D); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "--present", "--window-color=#666666", "--hide-stop", "within" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
