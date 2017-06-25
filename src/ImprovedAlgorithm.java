import java.util.ArrayList;
import java.util.List;

public class ImprovedAlgorithm {
	private DB db = DB.getInstance();
	private List<List<Integer>> clusters;
	private static final double delta = 0.0005;
	
	public ImprovedAlgorithm(){
		clusters = new ArrayList<List<Integer>>();
	}
	
	public void getClusters(String moviesFileName){
		List<Integer> v = db.loadMoviesFile(moviesFileName);
		this.clusters = getClusters(v);
		
	}

	public List<List<Integer>> getClusters(List<Integer> v){
		List<List<Integer>> tmpclusters = new ArrayList<List<Integer>>();
		while(!v.isEmpty()){
			List<Integer> c = new ArrayList<Integer>();
			int i = v.get(0);
			c.add(i);
			for(int j : v ){
				if(j != i){
					if(db.isPositive(i, j))
						c.add(j);
				}
			}
			removeBadNodes(c, v);
			addGoodNodes(c, v);
			removeBadNodes(c, v);
            if(c.size() == 0)
                c.add(i);

			tmpclusters.add(c);
			v.removeAll(c);
		}
		return tmpclusters;
	}
	


    private void removeBadNodes(List<Integer> c, List<Integer> v) {
        List<Integer> badList = new ArrayList<>();
        boolean found = false;
        while(!found) {
            found = true;
            for (int m : c) {
                if (!isDeltaGood(m, c, v, 3.0)) {
                    c.remove((Integer) m);
                    found = false;
                    break;
                }
            }
        }
    }

	
	private void addGoodNodes(List<Integer> c, List<Integer> v) {
		List<Integer> goodNodes = new ArrayList<Integer>();
		for(int m: v){
			if(isDeltaGood(m, c, v, 7.0))
				goodNodes.add(m);
		}
		c.removeAll(goodNodes);
		c.addAll(goodNodes);
	}

	private boolean isDeltaGood(int movie, List<Integer> cluster, List<Integer> v, double s){
		List<Integer> vTag = new ArrayList<>(v);
		vTag.removeAll(cluster);
		return ((numPositive(movie, cluster) >= (1.0 - s*delta)*cluster.size()) ||
				(numPositive(movie, vTag) <= s*delta*cluster.size()));
	}
	
	private int numPositive(int movie, List<Integer> cluster){
        int count = 0;
        for(int m: cluster){
            if(db.isPositive(movie, m))
                count++;

        }
        return count;
    }
	
	public double getCost(){
		if(clusters.isEmpty())
			return -1;
		double totalCost = 0;
		for(List<Integer> cluster : clusters){
			totalCost+= db.cost(cluster);
		}
		return totalCost;
	}

	public String toString(){
		String s = "";
		for(List<Integer> cluster : clusters){
			s+="[";
			for(int movie : cluster){
				s+=+movie+" "+db.getMovieTitle(movie)+", ";
			}
			s = s.substring(0, s.length()-2);
			s+="]\n";
			//s+=cluster.toString()+"\n";
		}
		return s;
	}
}
