// Sources...
// Flocking by Daniel Shiffman. https://processing.org/examples/flocking.html
// Flocking with predator by Abel Jnsm. https://www.openprocessing.org/sketch/126516
// Inspiration: Eli Stine and his Murmurator

// Written by Aaron Karp, 2018
// www.aaronmkarp.com
// aaronkarp123 @ gmail.com

import oscP5.*;
import netP5.*;
import java.util.Collections;

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
float zoom = 1500.0;

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

void setup() {
  //size(1000, 800, P3D);
  noCursor();
  fullScreen(P3D);
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
    forces[i] = 1.0;
    forces_target[i] = 1.0;
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
    w = int(width*1.8);
    h = int(height*1.8);
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

void draw() {
  lights();
  
  hour = hour();
  minute = minute();
  second = second();
  
  if (hour >= 6.0 && hour < 18.0 ){
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
      grav_size += min(max(grav_size/23.0, 3), 16);
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
  
  if (forces[4] == 1.0 ){
    if (grav_stop){
        grav_stop = false;
        grav_acc.x = random(0,1) - 0.5;
        grav_acc.y = random(0,1) - 0.5;
        grav_acc.mult(0.1);
        back_trans = random(1,128);
        pulse_train = true;
        pulse_train_c = 0;
    }
  }
  else
    grav_stop = true;
  if (grav_stop){
    grav_acc.x = grav_vel.x / abs(grav_vel.x) * -0.03;
    grav_acc.y = grav_vel.y / abs(grav_vel.y) * -0.03;
  }
  else if(num_frames % 400 == 0){
    grav_acc.x = random(0,1) - 0.5;
    grav_acc.y = random(0,1) - 0.5;
    grav_acc.mult(0.01);
  }
  
  if (grav_stop){
    if (grav_acc.x / grav_vel.x > 0.0)
      grav_acc.x *= -1;
    if (grav_acc.y / grav_vel.y > 0.0)
      grav_acc.y *= -1;
    if (abs(grav_vel.x) <= 0.03){
      grav_acc.x = 0.0;
      grav_vel.x = 0.0;
    }
    if (abs(grav_vel.y) <= 0.03){
      grav_acc.y = 0.0;
      grav_vel.y = 0.0;
    }
  }
  
  grav_vel.add(grav_acc);
  grav_vel.limit(4);
  
  grav_x += grav_vel.x;
  grav_y += grav_vel.y;
  
  float xdif = width - grav_x;
  if ((abs(xdif) < 50 && grav_acc.x > 0.0 )|| (abs(xdif) > width-50 && grav_acc.x < 0.0 )){
      grav_acc.x*=-1.0;
  }
  if ((abs(xdif) < 50 && grav_vel.x > 0.0 )|| (abs(xdif) > width-50 && grav_vel.x < 0.0 )){
      grav_vel.x*=-1.0;
  }
  float ydif = height - grav_y;
  if ((abs(ydif) < 50 && grav_acc.y > 0.0 )|| (abs(ydif) > height-50 && grav_acc.y < 0.0 )){
      grav_acc.y*=-1.0;
  }
  if ((abs(ydif) < 50 && grav_vel.y > 0.0 )|| (abs(ydif) > height-50 && grav_vel.y < 0.0 )){
      grav_vel.y*=-1.0;
  }
  
  while(num_birds_target > num_birds){
    float x = 0, y = 0;
    if (random(1) > 0.5)
      y = random(height) + height;
    else
      y = random(height) - height;
    if (random(1) > 0.5)
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
        forces[i] += 0.5;
      else
        forces[i] -= 0.5;
    }
  }
  
  //terrain
  if (terrain_back){
    float yoff = grav_y/400.0;
    for (int y = 0; y < rows; y++) {
      float xoff = grav_x/400.0;
      for (int x = 0; x < cols; x++) {
        terrain[x][y] = map(noise(xoff, yoff), 0, 1, -100, 100);
        xoff += 0.05;
      }
      yoff += 0.05;
    }
  }
  
  stroke(255);
  translate(-w/2, -h/2, -101);
  translate(grav_x, grav_y);
  for (int y = 0; y < rows-1; y++) {
    beginShape(TRIANGLE_STRIP);
    for (int x = 0; x < cols; x++) {
      color c = color((terrain[x][y] + 10) * 3, (terrain[x][y] + 20) * 4, (terrain[x][y] +15) * 3);
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
         width/2, height/2, 0.0, // centerX, centerY, centerZ
         0.0, 1.0, 0.0); // upX, upY, upZ
  
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

void mouseWheel(MouseEvent event) {
  float e = event.getCount();
  //println(e);
  zoom=constrain(zoom-e/10, 110, 2000);
 
}

void keyReleased() {
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
void mousePressed() {
  /*flock.addBoid(new Boid(mouseX,mouseY));
  num_birds++;*/
}


/* incoming osc message are forwarded to the oscEvent method. */
void oscEvent(OscMessage theOscMessage) {
  
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
    num_birds_target = int(oscstack.get(5).get(oscstack.size()-1));
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
