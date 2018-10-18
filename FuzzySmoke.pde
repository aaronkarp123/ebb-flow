
class FuzzySmokeParticle {
  float x, y;    // current position
  float vx, vy;  // velocity
  float timer;   // time left before estinguishing
  float dt = 0.5;  // estinguishing speed
  float r = 5;   // radius
  boolean on=false;
  boolean to_return=true;
  color col, next_col;
  
  FuzzySmokeParticle(color c) { col = c; next_col = c;}

  // Put at original position, fully opaque, with random velocity
  void initialize(float ox, float oy, color c)
  {
    col = next_col;
    on = true;
    x = ox; y = oy;
    // More vertical than horizontal
    //vx = random(-0.3,0.3); vy = random(-0.3,0.3)-1;
    vx = (float)randomGaussian()*0.2;
    vy = (float)randomGaussian()*0.2;
    timer = 255;
    dt = random(0.2, 2.0);
  }
  
  void draw(PImage img)
  {   
    if(!on) return;
    imageMode(CORNER);
    tint(col,timer);
    image(img,x-img.width/2,y-img.height/2);
  }
  
  void turn_off()
  {
    to_return = false;
  }
  
  void turn_on()
  {
    to_return = true;
  }
  
  void switch_on()
  {
    to_return = !to_return;
    if (to_return && timer <= 0){
      timer = random(0,255);
    }
  }
  
  void update(float ox, float oy)
  {
    // initialize if necessary
    if(!on && to_return) { if(random(0,1) < 0.01) initialize(ox-5+random(10),oy-5+random(10), col); return; }
    
    timer -= dt;    // decay the transparency
    x += vx;
    y += vy;
    // When exit screen or totally transparent, re-initialize
    if (timer < 0) {
      on = false;
    }
  }
  
  void change_col(color c)
  {
    next_col = c;
  }
}



class FuzzySmokeSystem {
  FuzzySmokeParticle[] particles;   // the individual bits exploding
  color c;                     // common color
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
    PImage msk = loadImage("/Users/aaronkarp/Downloads/texture.gif");
    img = new PImage(msk.width,msk.height);
    for (int i = 0; i < img.pixels.length; i++) img.pixels[i] = color(255);
    img.mask(msk);
  }
  
  void draw(int gravx, int gravy)
  {
    stroke(c); noFill();
    strokeWeight(2);
    //ellipse(x,y,2*r,2*r);
    // Draw the individual particles with the shared color
    x = gravx;
    y = gravy;
    if (random(1) > 0.999){
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

  void update()
  {
    // Update the individual particles
    for (int i=0; i<particles.length; i++) particles[i].update(x,y);
  }  
}
