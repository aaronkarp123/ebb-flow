
int dim = 200;
float starting_h=0.0;
float h_rate=0.05;
float starting_h_rate=0.1;

void renderGrav(float x, float y, float basec) {
  int radius = dim/2;
  float h = starting_h;
  starting_h += starting_h_rate;
  if (starting_h > 20.0 || starting_h < 0.0){
    starting_h_rate *= -1;
    starting_h += starting_h_rate;
  }
  colorMode(HSB, 360, 100, 100);
  ellipseMode(RADIUS);
  noStroke();
  for (int r = radius; r > 0; --r) {
    float scaledr = min(5.0, 10.0/(r*r*0.2));
    fill(h *scaledr / 2.0, h *scaledr/ 2.0,h *scaledr/ 2.0);
    ellipse(x, y, r, r);
    h += h_rate;
  }
}
