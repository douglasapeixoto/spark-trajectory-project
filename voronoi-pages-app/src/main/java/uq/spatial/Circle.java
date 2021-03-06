package uq.spatial;

import java.io.Serializable;

import uq.spatial.distance.EuclideanDistanceCalculator;
import uq.spatial.distance.PointDistanceCalculator;

/**
 * A simple circle, defined by it center
 * coordinates and radius.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class Circle implements Serializable, GeoInterface {
	/**
	 * Circle center X coordinate
	 */
	public double xCenter;
	/**
	 * Circle center Y coordinate
	 */
	public double yCenter;
	/**
	 * The radius of this circle
	 */
	public double radius;
		
	// distance measure for points
	private PointDistanceCalculator dist = 
			new EuclideanDistanceCalculator();
	/**
	 * The perimeter of this circle
	 */
	public double perimeter(){
		return (2*PI*radius);
	}
	
	/**
	 * The area of this circle
	 */
	public double area(){
		return (PI*radius*radius);
	}
	
	/**
	 * Returns the center of this circle as a coordinate point.
	 */
	public Point center(){
		return new Point(xCenter, yCenter);
	}

	/**
	 * True is this circle contains the given point inside its perimeter.
	 * Check if the point lies inside the circle area.
	 */
	public boolean contains(Point p){
		double dist = p.dist(xCenter, yCenter);
		return (dist <= radius);
	}
	
	/**
	 * True is this circle contains the given point inside its perimeter.
	 * Check if the point lies inside the circle area.
	 * </br>
	 * Point given by x,y,z coordinates
	 */
	public boolean contains(double x, double y){
		return contains(new Point(x, y));
	}

	/**
	 * True if the given point touches this circle (circle perimeter).
	 */
	public boolean touch(Point p){
		double dist = p.dist(xCenter, yCenter);
		return (dist == radius);
	}
	
	/**
	 * True if the given point touches this circle (circle perimeter).
	 *</br>
	 * Point given by X and Y coordinates
	 */
	public boolean touch(double x, double y){
		return touch(new Point(x, y));
	}

	/**
	 * True if the given line segment intersects this circle.
	 * Line segment is given by end point coordinates.
	 */
	public boolean intersect(
			double x1, double y1, 
			double x2, double y2){
		// triangle sides
		double distP1 = dist.getDistance(xCenter, yCenter, x1, y1);
		double distP2 =	dist.getDistance(xCenter, yCenter, x2, y2);
		double base   = dist.getDistance(x1, y1, x2, y2); 
		// triangle area
		double p = (distP1 + distP2 + base) / 2;
		double area = Math.sqrt(p*(p-distP1)*(p-distP2)*(p-base));
		// use triangulation to calculate distance from 
		// the circle center to the segment
		double hight = 2 * area * base;
		if(hight >= radius){
			return false;
		} 
		// get distance between the center of the circle 
		// and the first endpoint
		if(distP1 > radius){
			return true;
		}
		// get distance between the center of the circle 
		//and the second endpoint
		if(distP2 > radius){
			return true;
		}
		return false;
	}
	
	/**
	 * True is this circle overlaps with the given line segment.
	 * Line segment given by end points X and Y coordinates.
	 */
	public boolean overlap(double x1, double y1, double x2, double y2){
		if(contains(x1, y1) || touch(x1, y1)){
			if(contains(x2, y2) || touch(x2, y2)){
				return true;
			}
		}
		return false;
	}
}
