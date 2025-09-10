class vector2D{
    public double x, y;
    
    public vector2D(double x, double y){
        this.x = x;
        this.y = y;
    }
    
    public vector2D sum(vector2D v){
        return new vector2D(this.x+v.x, this.y+v.y);
    }
    
    public vector2D diff(vector2D v){
        return new vector2D(this.x-v.x , this.y-v.y);
    }
    
    public vector2D constProduct(double scalar){
        return new vector2D(this.x * scalar, this.y * scalar);
    }

    public double dotProduct(vector2D v){
        return (this.x*v.x) + (this.y*v.y);
    }
    
    public double magnitude(){
        return Math.sqrt(x*x + y*y);
    }
    
    public vector2D unitVector(){
        double mag = magnitude();
        if(mag == 0) return new vector2D(0, 0);
        return new vector2D(x/mag , y/mag);
    }
    
    public double distanceTo(vector2D v){
        return diff(v).magnitude();
    }
}