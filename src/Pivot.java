import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Pivot {
	private DB db = DB.getInstance();
	private List<List<Integer>> clusters;
	

	public Pivot(){
		clusters = new ArrayList<List<Integer>>();
	}

	//check if the input is legal
	private boolean isLegal(String str)
	{
		try{
			int i = Integer.parseInt(str);
			return db.isLegal(i);
		} catch(NumberFormatException e){
			return false;
		}
	}

	private List<Integer> loadMoviesFile(String fileName){
		List<Integer> movies = new ArrayList<Integer>();
		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null) {
				if(isLegal(sCurrentLine))
					movies.add(Integer.parseInt(sCurrentLine));
			}
			Collections.sort(movies);
		} catch(IOException e){
			e.printStackTrace();
		}
		return movies;
	}
	
	//for testing
	private List<Integer> legal(Set<Integer> set){
		List<Integer> l = new ArrayList<Integer>();
		for(int i:set){
			if(db.isLegal(i)){
				l.add(i);
			}
		}
		return l;
	}
	public void getClusters(String moviesFileName){
		List<Integer> v = loadMoviesFile(moviesFileName);
		this.clusters = getClusters(v);
		
	}

	public List<List<Integer>> getClusters(List<Integer> v){
		List<List<Integer>> tmpclusters = new ArrayList<List<Integer>>();
		while(!v.isEmpty()){
			List<Integer> c = new ArrayList<Integer>();
			int i = v.get(0);
			c.add(i);
			List<Integer> vTag = new ArrayList<Integer>();
			for(int j : v ){
				if(j != i){
					if(db.isPositive(i, j))
						c.add(j);
					else
						vTag.add(j);
				}
			}
			tmpclusters.add(c);
			v = vTag;
		}
		return tmpclusters;
	}
	
	public double getCost(){
		if(clusters.isEmpty())
			return -1;
		double totalCost = 0;
		for(List<Integer> cluster : clusters){
			totalCost+= cost(cluster);
		}
		return totalCost;
	}

	private double cost(List<Integer> cluster) {
		if(cluster.size()==1)
			return (Math.log(1/db.getProb(cluster.get(0),cluster.get(0))));
		double cost=0;
		for(int i=0;i<cluster.size();i++){
			for(int j=i+1;j<cluster.size();j++){
				cost+= (1/(cluster.size()-1))*(Math.log(1/db.getProb(cluster.get(i),cluster.get(j))));
			}
		}
		return cost;
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