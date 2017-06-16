import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class DB {

	private int numMovies;
	private int numUsers;
	private HashMap<Integer,Integer> usersMoviesNum; // usersID -> number of movies that the user watched
	private HashMap<Integer,List<Integer>> moviesUsers; //movieID -> list of users that watch the movie
	private HashMap<Integer,Integer> indMoviesId; //array Index -> movieId
	private HashMap<Integer,Integer> moviesIdInd; //movieId -> array Index
	private HashMap<Integer,String> idToTitle;//movieId -> movie title
	private HashMap<Integer,String> movieGeners;
	private double[][] pMovies;
	//private int[][] correlations;

	

	/*
	 * Singleton instance
	 */
	private static class DBHolder {
		private static DB instance = new DB();
	}

	public static DB getInstance(){
		return DBHolder.instance;
	}

	private DB(){
		numMovies = 0;
		numUsers = 0;
		usersMoviesNum = new HashMap<Integer,Integer>();
		moviesUsers = new HashMap<Integer,List<Integer>>();
		indMoviesId = new HashMap<Integer,Integer>();
		moviesIdInd = new HashMap<Integer,Integer>();
		idToTitle = new HashMap<Integer,String>();
		movieGeners = new HashMap<>();
	}


	public boolean isPositive(int movieId1,int movieId2){
		int i = moviesIdInd.get(movieId1);
		int j = moviesIdInd.get(movieId2);
		//return correlations[i][j] == 1;
		return pMovies[i][j] >= pMovies[i][i]*pMovies[j][j];
	}
	
	public String getGeners(int movieId){
		return movieGeners.get(movieId);
	}

	public void loadTitles(String path){
		new Thread(()->{
			try (BufferedReader br = new BufferedReader(new FileReader(path))) {
				String sCurrentLine;
				while ((sCurrentLine = br.readLine()) != null) {
					String[] line = sCurrentLine.split("::");
					idToTitle.put(Integer.parseInt(line[0]),line[1]);
					movieGeners.put(Integer.parseInt(line[0]), line[2]);
				}
			} catch(IOException e){
				e.printStackTrace();
			}
		}).start();
	}
	
	
	//check if the input is legal
	private boolean isLegal(String str)
	{
		try{
			int i = Integer.parseInt(str);
			return isLegal(i);
		} catch(NumberFormatException e){
			return false;
		}
	}

	public List<Integer> loadMoviesFile(String fileName){
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

	public String getMovieTitle(int movieId){
		return idToTitle.get(movieId);
	}

	public void loadDB(String path){
		int currentUser = 0;
		int usersCounter = 0;
		int moviesCounter = 0;

		try (BufferedReader br = new BufferedReader(new FileReader(path))) {
			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null) {
				String[] dataLine = sCurrentLine.split("::");
				int user = Integer.parseInt(dataLine[0]);
				int movie = Integer.parseInt(dataLine[1]);

				if(user != currentUser){
					currentUser = user;
					usersCounter++;
					usersMoviesNum.put(user, 0);
				}
				usersMoviesNum.put(user,usersMoviesNum.get(user)+1);

				if(moviesUsers.containsKey(movie)){
					List<Integer> usersList = moviesUsers.get(movie);
					usersList.add(user);
				}
				else {
					indMoviesId.put(moviesCounter,movie);
					moviesIdInd.put(movie, moviesCounter);
					moviesCounter++;
					List<Integer> usersList= new ArrayList<Integer>();
					usersList.add(user);
					moviesUsers.put(movie, usersList);
				}
			}
			this.numUsers = usersCounter;
			this.numMovies = moviesCounter;

			System.out.println("calculating probabilities..");
			calcProbs();

			//checkCorrelations();

			System.out.println("finish loading");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 *
	 */
	public List<Integer> mutualList(List<Integer> l1,List<Integer> l2){
		List<Integer> shortList;
		List<Integer> longList;
		List<Integer> finalList = new ArrayList<Integer>();
		if(l1.size()>l2.size()){
			shortList = l2; longList = l1;
		} else {
			shortList = l1; longList = l2;
		}
		for(int user: shortList){
			if(Collections.binarySearch(longList,user) >= 0)
				finalList.add(user);
		}
		return finalList;
	}
	
	


	public void writeProb(int i,int j,double prob){
		this.pMovies[i][j] = prob;
	}

	/*public void checkCorrelations(){
		correlations = new int[numMovies][numMovies];
		for(int i=0;i<numMovies;i++){
			correlations[i][i] = 1;
			for(int j=i+1;j<numMovies;j++){
				if(pMovies[i][j] >= pMovies[i][i]*pMovies[j][j])
					correlations[i][j] = 1;
				else
					correlations[i][j] = 0;
			}
		}
	}*/

	private void calcProbs() {
		pMovies = new double[numMovies][numMovies];

		int numOfThreads = (numMovies/100)+1;
		ExecutorService es = Executors.newFixedThreadPool(numOfThreads);
		for(int i=0;i<numOfThreads;i++)
			es.submit(new ProbCalculator(i*100));
		es.shutdown();
		try {
			es.awaitTermination(5, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		
		/*System.out.println(pMovies[moviesIdInd.get(1)][moviesIdInd.get(1)]);
		System.out.println(pMovies[moviesIdInd.get(573)][moviesIdInd.get(573)]);
		System.out.println(pMovies[moviesIdInd.get(1)][moviesIdInd.get(573)]);
		System.out.println(pMovies[moviesIdInd.get(573)][moviesIdInd.get(573)]*pMovies[moviesIdInd.get(1)][moviesIdInd.get(1)]);
		for(int i=0; i<getNumMovies() ;i++){
			writeProb(i,i,calcPmovie(i));
			for(int j=i+1; j<getNumMovies(); j++){
				double p = calcPpair(i,j);
				writeProb(i, j, p);
			}
		}*/
	}


	public double calcPpair(int index1, int index2) {
		int movieId1 = indMoviesId.get(index1);
		int movieId2 = indMoviesId.get(index2);
		if(moviesUsers.get(movieId1) == null || moviesUsers.get(movieId2) == null)
			return 0;
		List<Integer> users = mutualList(moviesUsers.get(movieId2), moviesUsers.get(movieId1));
		double sum = 0;
		for(int user : users)
			sum += 2.0/(getUserNumOfMovies(user)*(getUserNumOfMovies(user)-1));
		sum += 2.0/(numMovies*(numMovies-1));
		sum *= 1.0/(numUsers+1);
		return sum;
	}

	public double calcPmovie(int index){
		int movieId = indMoviesId.get(index);
		List<Integer> users = moviesUsers.get(movieId);
		if(users == null)
			return 0;
		double sum = 0;
		for(int user : users)
			sum +=  2.0/getUserNumOfMovies(user);
		sum += 2.0/numMovies;
		sum *= 1.0/(numUsers+1);
		return sum;
	}



	public int getNumMovies() {
		return numMovies;
	}

	public int getNumUsers() {
		return numUsers;
	}

	public int getUserNumOfMovies(int userId){
		return usersMoviesNum.get(userId);
	}

	public boolean isLegal(int movieId){
		return moviesUsers.get(movieId)!=null && moviesUsers.get(movieId).size() >= 10;
	}

	public Set<Integer> getListOfMovies(){
		return moviesUsers.keySet();
	}

	public List<Integer> getMovieUsersList(int movieId){
		return moviesUsers.get(movieId);
	}

	public double getProb(int movieID1,int movieID2) {
		return pMovies[moviesIdInd.get(movieID1)][moviesIdInd.get(movieID2)];
	}
}
