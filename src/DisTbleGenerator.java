
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class DisTbleGenerator {
	private DB db;
	private HashMap<Integer,double[]> moviesVectors;
	private String userFilePath;
	private List<List<Integer>> clusters;
	private final double AVG = 0.48242981863555623;
	
	
	public DisTbleGenerator(String path) {
		userFilePath = path;
		moviesVectors = new HashMap<>();
		db = DB.getInstance();
		clusters = new ArrayList<List<Integer>>();
	}

	private int genresToIndex(String gener){
		switch(gener){
		case "Action":
			return 0;
		case "Adventure":
			return 1;
		case "Animation":
			return 2;
		case "Children's":
			return 3;
		case "Comedy":
			return 4;
		case "Crime":
			return 5;
		case "Documentary":
			return 6;
		case "Drama":
			return 7;
		case "Fantasy":
			return 8;
		case "Film-Noir":
			return 9;
		case "Horror":
			return 10;
		case "Musical":
			return 11;
		case "Mystery":
			return 12;
		case "Romance":
			return 13;
		case "Sci-Fi":
			return 14;
		case "Thriller":
			return 15;
		case "War":
			return 16;
		case "Western":
			return 17;
		default:
			return -1;
		}
	}

	public int relAge(int age){
		switch(age){
		case 1:
			return 0;
		case 18:
			return 1;
		case 25:
			return 2;
		case 35:
			return 3;
		case 45:
			return 4;
		case 50:
			return 5;
		case 56:
			return 6;
		default:
			return -1;
		}
	}

	private void loadVector(int movieId){
		ArrayList<Integer> userList = new ArrayList<Integer>(db.getMovieUsersList(movieId));
		double totalAge = 0;
		double totalWoman = 0;
		int totalUsers = userList.size();
		double[] vector = new double[21];
		try (BufferedReader br = new BufferedReader(new FileReader(this.userFilePath))) {
			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null) {
				//System.out.println("movie id: "+movieId + " users :" + totalUsers );
				int currentUserId = userList.get(0); 
				String[] parseLine = sCurrentLine.split("::");
				if(parseLine[0].equals(currentUserId+"")){
					totalAge += relAge(Integer.parseInt(parseLine[2]));
					if(parseLine[1].equals("F"))
						totalWoman++;
					userList.remove(0);
					if(userList.isEmpty())
						break;
				}

			}
			vector[18] = (totalAge/totalUsers)/6;
			vector[19] = totalWoman/totalUsers;
			//get year 
			String title = db.getMovieTitle(movieId);
			String year = title.substring(title.length()-5,title.length()-1);		
			vector[20] = (Integer.parseInt(year)/10 - 191)/9;

		} 
		catch (IOException e){
			e.printStackTrace();
		}
		String[] geners = db.getGeners(movieId).split("\\|");
		for (String gener : geners){
			vector[genresToIndex(gener)] = 0.5;
		}
		this.moviesVectors.put(movieId, vector);
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


	private double norm(double[] v){
		double norm = 0;
		for(int i=0; i < v.length; i++){
			norm += Math.pow(v[i], 2.0);
		}
		return Math.sqrt(norm);
	}

	private double multiply(double[] v1, double[] v2){
		double v1v2 = 0;
		for(int i=0; i < v1.length; i++){
			v1v2 += v1[i]*v2[i];
		}
		/*if(v1[v1.length-1] == v2[ v2.length-1])
			v1v2+=1;*/
		return v1v2;
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
					if(sim(moviesVectors.get(j),moviesVectors.get(i)) > AVG)
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

	
	public double getCost(){
		if(clusters.isEmpty())
			return -1;
		double totalCost = 0;
		for(List<Integer> cluster : clusters){
			totalCost+= cost(cluster);
		}
		return totalCost;
	}


	private double sim(double[] v1, double[] v2){
		double norm1 = norm(v1);
		double norm2 = norm(v2);
		/*if(v1[v1.length-1] == v2[ v2.length-1]){
			norm1 = Math.sqrt(norm1 + 1);
			norm2 = Math.sqrt(norm2 + 1);
		}*/
		return multiply(v1,v2) / (norm1*norm2);
	}

	/*public void clusters(){
		//try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("distances.dat"), "utf-8"))) {
		Set<Integer> movies = db.getListOfMovies();
		double sum = 0;
		int count = 0;
		for(int m1 : movies){
			for(int m2: movies){
				if(m1!=m2){
					sum+=sim(moviesVectors.get(m1),moviesVectors.get(m2));
					count++;
				}
			}
		}
		System.out.println(sum/count);
	}*/

	public void loadVectors(){
		Set<Integer> movies = db.getListOfMovies();
		for(int i: movies){
			loadVector(i);
		}
		//writeDisTable();
	}
}
