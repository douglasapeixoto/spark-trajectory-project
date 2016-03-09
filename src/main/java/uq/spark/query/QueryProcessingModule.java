package uq.spark.query; 

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.spark.broadcast.Broadcast;

import uq.spark.index.TrajectoryCollector;
import uq.spark.index.TrajectoryTrackTable;
import uq.spark.index.VoronoiDiagram;
import uq.spark.index.VoronoiPagesRDD;
import uq.spatial.Box;
import uq.spatial.Trajectory;
import uq.spatial.clustering.Cluster;

/**
 * Service to process trajectory queries
 * on a previously built Voronoi index.
 * </br>
 * Can be called by many concurrent threads.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class QueryProcessingModule implements Serializable {
	// trajectory collector service
	private TrajectoryCollector collector = null;
	
	// Query services
	private SelectionQueryCalculator selectionQuery = null;
	private CrossQueryCalculator crossQuery = null;
	private NearestNeighborQueryCalculator nnQuery = null;
	private DensityQueryCalculator densityQuery = null;
	
	/**
	 * Service Constructor
	 */
	public QueryProcessingModule(final VoronoiPagesRDD voronoiPagesRDD, 
								  final TrajectoryTrackTable trajectoryTrackTable,
								  final Broadcast<VoronoiDiagram> voronoiDiagram){
		// initialize trajectory collector service
		collector = new TrajectoryCollector(voronoiPagesRDD, trajectoryTrackTable);
		
		// initialize query services
		selectionQuery = new SelectionQueryCalculator(voronoiPagesRDD, voronoiDiagram);
		crossQuery = new CrossQueryCalculator(voronoiPagesRDD, voronoiDiagram);
		nnQuery = new NearestNeighborQueryCalculator(voronoiPagesRDD, voronoiDiagram, trajectoryTrackTable);
		densityQuery = new DensityQueryCalculator(voronoiPagesRDD, voronoiDiagram);
	}

	/**
	 * Given a rectangular geographic region, and a time window
	 * from t0 to t1, return all trajectories that overlap with
	 * the given region and time window [t0, t1]. 
	 * 
	 * @param whole True if wants to return the whole trajectories.
	 */
	public synchronized List<Trajectory> getSpatialTemporalSelection (
			final Box region, 
			final long t0, final long t1, 
			final boolean whole){
		System.out.println("[QUERY MODULE] Running Spatial-Temporal Selection Query..\n");
		
		// query result
		List<Trajectory> trajectoryList = new ArrayList<Trajectory>();
		if(whole){
			// collect whole trajectories
			List<String> resultIdList =
					selectionQuery.getSpatialTemporalSelectionId(region, t0, t1);
			trajectoryList = 
					collector.collectTrajectoriesById(resultIdList).collect();
		} else{
			// sub-trajectories only
			trajectoryList = 
				selectionQuery.getSpatialTemporalSelection(region, t0, t1);
		}
		return trajectoryList;	
	}
	
	/**
	 * Given a rectangular geographic region, return all trajectories 
	 * that overlap with the given region. 
	 * 
	 * @param whole True if wants to return the whole trajectories.
	 */
	public synchronized List<Trajectory> getSpatialSelection(
			final Box region,
			final boolean whole){
		System.out.println("\n[QUERY MODULE] Running Spatial Selection Query..\n");
		
		// query result
		List<Trajectory> trajectoryList = new ArrayList<Trajectory>();
		if(whole){
			// collect whole trajectories
			List<String> resultIdList =
					selectionQuery.getSpatialSelectionId(region);
			trajectoryList = 
					collector.collectTrajectoriesById(resultIdList).collect();
		} else {
			// sub-trajectories only
			trajectoryList = 
					selectionQuery.getSpatialSelection(region);
		}
		return trajectoryList;
	}
	
	/**
	 * Given a a time window from t0 to t1, return all trajectories 
	 * that overlap with the time window, that is, return all trajectories 
	 * that have been active during [t0, t1].
	 * 
	 * @param whole True if wants to return the whole trajectories.
	 */
	public synchronized List<Trajectory> getTemporalSelection(
			final long t0, final long t1, 
			final boolean whole){
		System.out.println("\n[QUERY MODULE] Running Time Selection Query..\n");
		
		// query result
		List<Trajectory> trajectoryList = new ArrayList<Trajectory>();
		if(whole){		
			// collect whole trajectories
			List<String> resultIdList = 
					selectionQuery.getTemporalSelectionId(t0, t1);
			trajectoryList = 
				collector.collectTrajectoriesById(resultIdList).collect();
		} else{
			// sub-trajectories only
			trajectoryList =
				selectionQuery.getTimeIntervalSelection(t0, t1);
		}
		return trajectoryList;	
	}
	
	/**
	 * Given a query trajectory Q, not necessarily in the data set, 
	 * return all trajectories in the data set that crosses with Q.
	 */
	public synchronized List<Trajectory> getCrossSelection(
			final Trajectory q){
		
		System.out.println("\n[QUERY MODULE] Running Crossing Selection Query..\n");
		
		// query result
		List<Trajectory> trajectoryList = new ArrayList<Trajectory>();
		// collect whole trajectories
		List<String> resultIdList = 
				crossQuery.runCrossQueryId(q);
		trajectoryList = 
				collector.collectTrajectoriesById(resultIdList).collect();
		return trajectoryList;	
	}	
	
	/**
	 * Given a query trajectory Q, a time interval t0 to t1,
	 * and a integer K, return the K Nearest Neighbors (Most  
	 * Similar Trajectories) from Q, within the interval [t0,t1]. 
	 * 
	 * @param whole True if wants to return the whole trajectories.
	 */
	public synchronized Trajectory getNearestNeighbor(
			final Trajectory q, 
			final long t0, final long t1){
		System.out.println("\n[QUERY MODULE] Running NN Query..\n");
		
		// query result
		NearNeighbor nnResult = 
				nnQuery.getNearestNeighbor(q, t0, t1);
		return nnResult;
	}
	
	/**
	 * Given a query trajectory Q, a time interval t0 to t1,
	 * and a integer K, return the K Nearest Neighbors (Most  
	 * Similar Trajectories) from Q, within the interval [t0,t1]. 
	 */
	public synchronized List<NearNeighbor> getKNearestNeighbors(
			final Trajectory q, 
			final long t0, final long t1, 
			final int k){
		System.out.println("\n[QUERY MODULE] Running " + k + "-NN Query..\n");
		
		// query result
		List<NearNeighbor> resultList = 
				nnQuery.getKNearestNeighbors(q, t0, t1, k);
		return resultList;
	}

	/**
	 * Given a query trajectory Q and a time interval t0 to t1, 
	 * return all trajectories that have Q as their Nearest Neighbor
	 * (Most Similar Trajectory), within the interval [t0,t1].
	 */
	public synchronized List<Trajectory> getReverseNearestNeighbors(
			final Trajectory q, 
			final long t0, final long t1){
		System.out.println("\n[QUERY MODULE] Running Reverse NN Query..\n");
		// query result
		Iterator<Trajectory> resultItr = 
				nnQuery.getReverseNearestNeighbors(q, t0, t1);
		// collect result
		List<Trajectory> rnnList = new ArrayList<Trajectory>();
		while(resultItr.hasNext()){
			rnnList.add(resultItr.next());
		}
		return rnnList;		
	}

	/**
	 * Given a geographic region and a density threshold, 
	 * return the regions with trajectory points density 
	 * greater than the given density threshold, within the
	 * given spatial region.
	 * </br></br>
	 * Density threshold is given by distance threshold and number
	 * of points.
	 *  
	 * @param region The query region to search.
	 * @param distanceThresold The maximum distance between points in the clusters.
	 * @param minPoints The minimum number of points in each cluster.
	 * @return 
	 * 
	 * @return A list of clusters of trajectory points.
	 */
	public synchronized List<Cluster> getSpatialDensityClusters(
			final Box region, 
			final double distanceThresold, 
			final int minPoints){
		System.out.println("\n[QUERY MODULE] Running Spatial Density Query..\n");
		
		// query result
		List<Cluster> resultList = 
				densityQuery.runDensityQuery(region, distanceThresold, minPoints);
		return resultList;	
	}
	
	/**
	 * Density Query:
	 * Given a geographic region (area), a time interval from t0 to t1,
	 * and a density threshold, return the regions with trajectory 
	 * points density greater than the given density threshold, within 
	 * the given region and time interval [t0, t1]. 
	 * </br></br>
	 * Density threshold is given by distance threshold and number
	 * of points.
	 *  
	 * @param region The query region to search.
	 * @param distanceThresold The maximum distance between points in the clusters.
	 * @param minPoints The minimum number of points in each cluster.
	 * @return 
	 * 
	 * @return A list of clusters of trajectory points.
	 */
	public synchronized List<Cluster> getSpatialTemporalDensityClusters(
			final Box region, 
			final long t0, final long t1,
			final double distanceThresold, 
			final int minPoints){
		System.out.println("\n[QUERY MODULE] Running Spatial-Temporal Density Query..\n");
		
		// query result
		List<Cluster> resultList = 
				densityQuery.runDensityQuery(region, t0, t1, distanceThresold, minPoints);
		return resultList;	
	}

}
