
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class DisTbleGenerator {
	private DB db;
	private HashMap<Integer,double[]> moviesVectors;
	private String userFilePath;
	private List<List<Integer>> clusters;
	private final double simAVG = 1.1096807811694138;
	private final double disAVG = 1.1304697934449213;
	private final int numOfgen = 18;
	private static final double delta = 0.005;

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

	/*
	public void getTitles(String moviesFile){
		List<Integer> v = db.loadMoviesFile(moviesFile);
		for(int m: v){
			System.out.println(m+"\t"+db.getMovieTitle(m));
		}
	}
*/


	public void getClusters(String moviesFileName){
		List<Integer> v = db.loadMoviesFile(moviesFileName);
		this.clusters = getClusters(v);

	}


	public List<List<Integer>> getClusters(List<Integer> v){
		List<List<Integer>> tmpclusters = new ArrayList<List<Integer>>();
		while(!v.isEmpty()){
			List<Integer> c = new ArrayList<Integer>();
			int index = (int) (v.size()*Math.random());
			//int i = v.get(index);
			int i = v.get(0);
			c.add(i);
			for(int j : v ){
				if(j != i){
					if(distance(i, j) < disAVG/3)
						c.add(j);
				}
			}
			removeBadNodes(c, v);
			System.out.println("c size: "+c.size());
			if(c.size() == 0)
				c.add(i);
			addGoodNodes(c, v);
			removeBadNodes(c, v);
			tmpclusters.add(c);
			v.removeAll(c);
		}
		return tmpclusters;
	}


	private void removeBadNodes(List<Integer> c, List<Integer> v) {
	//	List<Integer> badList = new ArrayList<>();
		boolean found = false;

		//List<Integer> vTag = new ArrayList<>(v);
		int m;
		//List<Integer> toAdd = new ArrayList<>();
		while(!allPositive(c)){
			m = findMin(c);
		//	vTag.removeAll(c);
			c.remove((Integer) m);
		//	if(numPositive(m, vTag)==0)
		//		toAdd.add(m);
		}
		//c.addAll(toAdd);

		/*


		while(!found) {
			found = true;
			for (int m : c) {
				if (!(isDeltaGood(m, c, v, 3.0) )) {
					c.remove((Integer) m);
					found = false;
					break;
				}
			}
		}
		*/
	}

	private int findMin(List<Integer> c){
		int minPositive = 10000;
		int movie = -1;
		int n;
		for(int m: c){
			n = numPositive(m, c);
			if(n < minPositive){
				minPositive = n;
				movie = m;
			}
		}
		return movie;
	}

	private boolean allPositive(List<Integer> c){
		for(int m: c){
			if(numPositive(m, c) < c.size())
				return false;
		}
		return true;
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
		//return (numPositive(movie, cluster) >= 0.6*cluster.size());

		return ((numPositive(movie, cluster) >= (1.0 - s*delta)*cluster.size())
				|| (numPositive(movie, vTag) <= s*delta*cluster.size()));
	}
/*
	private boolean isGood(int movie, List<Integer> cluster, List<Integer> v, double s){
		List<Integer> vTag = new ArrayList<>(v);
		vTag.removeAll(cluster);
		return ((numShortDistance(movie, cluster) >= (1.0 - s*delta)*cluster.size()) ||
				(numShortDistance(movie, vTag) <= s*delta*cluster.size()));
	}
*/
	private int numPositive(int movie, List<Integer> cluster){
		int count = 0;
		for(int m: cluster){
			if(db.isPositive(movie, m))
				count++;

		}
		return count;
	}
/*
	private int numShortDistance(int movie, List<Integer> cluster){
		int count = 0;
		for(int m: cluster){
			if(distance(movie, m) < disAVG)
				count++;
		}
		return count;
	}
*/
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
		}
		return s;
	}
/*
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

	private double sim(int m1, int m2){
		double[] v1 = moviesVectors.get(m1);
		double[] v2 = moviesVectors.get(m2);
		double [] sv1 = new double [v1.length-numOfgen];
		double [] sv2 = new double [v1.length-numOfgen];
		for (int i=0; i<sv1.length; i++){
			sv1[i] = v1[i+numOfgen];
			sv2[i] = v2[i+numOfgen];
		}
		double norm1 = norm(sv1);
		double norm2 = norm(sv2);
		return (multiply(sv1,sv2) / (norm1*norm2)) + getMutualGeners(m1, m2) ;
	}

	private void avgcalc(){
		Set<Integer> movies = db.getListOfMovies();
		double sum = 0;
		double min = 10000000;
		double max = 0;
		double count = 0;
		for(int m1 : movies){
			for(int m2: movies){
				if(m1!=m2 && db.isLegal(m1) && db.isLegal(m2)){
					if(sim(m1,m2) > max)
						max = sim(m1,m2);
					else if(sim(m1,m2) < min)
						min = sim(m1,m2);

					sum += sim(m1,m2);
					count++;
				}
			}
		}
		System.out.println("avg: "+ sum/count);
		System.out.println("max: "+ max);
		System.out.println("min: "+min);
	}
*/
	private void avgCalac(){
		Set<Integer> movies = db.getListOfMovies();
		double sum = 0;
		double min = 10000000;
		double max = 0;
		double count = 0;
		for(int m1 : movies){
			for(int m2: movies){
				if(m1!=m2 && db.isLegal(m1) && db.isLegal(m2)){
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
		double genGrade = getMutualGeners(m1, m2);
		double[] v1 = moviesVectors.get(m1);
		double[] v2 = moviesVectors.get(m2);

		for(int i = numOfgen ; i < v1.length; i++){
			double test = v1[i] - v2[i];
			distance += Math.pow(test,2.0);
		}
		return Math.sqrt(distance) + (1.0 - genGrade);
	}


	private double getMutualGeners(int m1, int m2){
		double mutualGen = 0;
		double geners = 0;
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
		return mutualGen/geners;
	}

/*
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

	}*/

	private void loadVectors(){
		Set<Integer> movies = db.getListOfMovies();
		for(int i: movies){
			loadVector(i);
		}
	}
}
