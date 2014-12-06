package us.yamb.impl;

import us.yamb.spi.YPeer.Coordinates;

public class YCoords implements Coordinates
{
    
    private double x;
    private double y;
    private double z;
    
    public double distance(Coordinates other)
    {
        return Math.sqrt(Math.pow(this.x - other.x(), 2) + Math.pow(this.y - other.y(), 2) + Math.pow(this.z - other.z(), 2));
    }
    
    public double x()
    {
        return this.x;
    }
    
    public double y()
    {
        return this.y;
    }
    
    public double z()
    {
        return this.z;
    }
    
    public void setX(double x)
    {
        this.x = x;
    }
    
    public void setY(double y)
    {
        this.y = y;
    }
    
    public void setZ(double z)
    {
        this.z = z;
    }
    
}
