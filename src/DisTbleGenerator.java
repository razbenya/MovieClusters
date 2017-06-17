
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.swing.plaf.synth.SynthSpinnerUI;

public class DisTbleGenerator {
	private DB db;
	private HashMap<Integer,double[]> moviesVectors;
	private String userFilePath;
	private List<List<Integer>> clusters;
	private final double AVG = 3.797466938519524;
	private final double AVG2 = 1.0481874605661525;
	private final int numOfgen = 18;
	
	
	public DisTbleGenerator(String path) {
		userFilePath = path;
		moviesVectors = new HashMap<>();
		db = DB.getInstance();
		clusters = new ArrayList<List<Integer>>();
		loadVectors();
		avgCalac();
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
			vector[18] = (totalAge/totalUsers)/6.0;
			vector[19] = (double) (totalWoman/totalUsers);
			//get year 
			String title = db.getMovieTitle(movieId);
			String year = title.substring(title.length()-5,title.length()-1);		
			//vector[20] = (Integer.parseInt(year)/10 - 191)/9.0;
			vector[20] = (Integer.parseInt(year) - 1919.0)/82.0;
			

		} 
		catch (IOException e){
			e.printStackTrace();
		}
		String[] geners = db.getGeners(movieId).split("\\|");
		for (String gener : geners){
			vector[genresToIndex(gener)] = 1;
		}
		this.moviesVectors.put(movieId, vector);
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
		return v1v2;
	}
	
	public void getClusters(String moviesFileName){
		List<Integer> v = db.loadMoviesFile(moviesFileName);
		this.clusters = getClusters(v);
		
	}

	public List<List<Integer>> getClusters(List<Integer> v){
		List<List<Integer>> tmpclusters = new ArrayList<List<Integer>>();
		while(!v.isEmpty()){
			List<Integer> c = new ArrayList<Integer>();
			int index = (int) (v.size()*Math.random());
			int i = v.get(0);
			c.add(i);
			List<Integer> vTag = new ArrayList<Integer>();
			for(int j : v ){
				if(j != i){
					//if(db.isPositive(i,j) && sim(moviesVectors.get(j),moviesVectors.get(i)) > AVG)
					if(db.isPositive(i,j) && distance(j,i) < AVG2/2.0)
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
			return (Math.log(1.0/db.getProb(cluster.get(0),cluster.get(0))));
		double cost=0;
		for(int i=0;i<cluster.size();i++){
			for(int j=i+1;j<cluster.size();j++){
				cost+= (1.0/(cluster.size()-1.0))*(Math.log(1.0/db.getProb(cluster.get(i),cluster.get(j))));
			}
		}
		return cost;
	}
	
	public double corr(int m1,int m2){
		double mone = db.getProb(m1,m2) - db.getProb(m1,m1)*db.getProb(m2,m2);
		double s1 = db.getProb(m1,m1) - Math.pow(db.getProb(m1,m1),2.0);
		double s2 = db.getProb(m2,m2) - Math.pow(db.getProb(m2,m2),2.0);
		return mone/(Math.sqrt(s1)*Math.sqrt(s2));
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
		}
		return s;
	}



	private double sim(double[] v1, double[] v2){
		double norm1 = norm(v1);
		double norm2 = norm(v2);
		return multiply(v1,v2) / (norm1*norm2);
	}

	private void avgCalac(){
		Set<Integer> movies = db.getListOfMovies();
		double sum = 0;
		double min = 10000000;
		double max = 0;
		double count = 0;
		for(int m1 : movies){
			for(int m2: movies){
				if(m1!=m2){
					//System.out.println(corr(m1,m2));
					//corr(m1,m2);
					//sum+=distance(moviesVectors.get(m1),moviesVectors.get(m2),m1,m2);
					if(distance(m1,m2) > max)
						max = distance(m1,m2);
					else if(distance(m1,m2) < min){
						min = distance(m1,m2);
					
					}
					sum += distance(m1,m2);
					
					count++;
				}
			}
		}
		System.out.println("avg: "+ sum/count);
		System.out.println("max: "+ max);
		System.out.println("min: "+min);
	}
	
	private double distance(int m1,int m2){
		double distance = 0;
		double mutualGen = 0;
		double geners = 0;
		double genGrade = 0;
		double[] v1 = moviesVectors.get(m1);
		double[] v2 = moviesVectors.get(m2);
		
		//geners
				for(int i=0;i < numOfgen; i++){
					if(Double.compare((v1[i]+v2[i]), 2.0) == 0){
						mutualGen++;
						geners++;
					}
					else if(Double.compare(v1[i], 1.0) == 0 || Double.compare(v2[i], 1.0) == 0 ){
						geners++;
					}
				}
				genGrade = (mutualGen/geners);
		if(v1[18] == v2[18] && v1[19] == v2[19] && v1[20] == v2[20])
			System.out.println(m1 + "  " + m2);
		for(int i = numOfgen ; i < v1.length; i++){
			double test = v1[i] - v2[i];
			distance += Math.pow(test,2.0);
		}
		return Math.sqrt(distance) + (1.0 - genGrade);
	}
	
	
	/// grade system 
	public double grade(int m1,int m2){
		double grade = 0;
		double mutualGen = 0;
		double geners= 0;
		double a = 2.5, b = 1.5, c = 1 ,d = 1.5, e = 1000;
		double[] v1 = moviesVectors.get(m1);
		double[] v2 = moviesVectors.get(m2);
		
		//geners
		for(int i=0;i < numOfgen; i++){
			if(v1[i]+v2[i] == 2.0){
				mutualGen++;
				geners++;
			}
			else if(v1[i] == 1 || v2[i] == 1 ){
				geners++;
			}
		}
		grade += a*(mutualGen/geners);
		
		//age
		grade += b*(1 - Math.abs(v1[numOfgen] - v2[numOfgen]));
		
		
		//% woman
		grade += c*(1 -  Math.abs(v1[numOfgen+1] - v2[numOfgen+1]));
		
		//year 
		grade += d*(1 - ((Math.abs(v1[numOfgen+2] - v2[numOfgen+2]))/82));
		
		//prob
		grade += e*db.getProb(m1, m2);
		
		return grade;
		
	}

	private void loadVectors(){
		Set<Integer> movies = db.getListOfMovies();
		for(int i: movies){
			loadVector(i);
		}
	}
}
