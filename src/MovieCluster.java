import java.io.File;

public class MovieCluster {
	private static final String RATINGS = "ratings.dat";
	private static final String MOVIESTITLE = "movies.dat";
	private static final String USERS = "users.dat";
	
	public static void main(String[] args) {
		if(args.length != 3){
			System.out.println("the number of arguments is invalid");
		}
		else {
			DB db = DB.getInstance();
			File dir = new File(args[0]);
			if(!dir.exists() || !dir.isDirectory()) {
				System.out.println("dataset folder is missing");
			}
			else {
				System.out.println("loading database please wait..");
				db.loadTitles(args[0]+"\\"+MOVIESTITLE);
				db.loadDB(args[0]+"\\"+RATINGS);
				File f = new File(args[2]);
				if(!f.exists() || f.isDirectory()) { 
					System.out.println("movie subset file is missing.. ");
					System.out.println("creating a random file.");
					new SubsetGenerator().generateRandomSubset(args[2]);
				}
				if("1".equals(args[1])){
					System.out.println("running pivot algorithm ");
					System.out.println();
					Pivot p = new Pivot();
					p.getClusters(args[2]);
					System.out.println(p.toString());
					System.out.println("finish with total cost : "+p.getCost());
					System.out.println();
					DisTbleGenerator dtg = new DisTbleGenerator(args[0]+"\\"+USERS);
					dtg.loadVectors();
					dtg.getClusters(args[2]);
					System.out.println(dtg.toString());
					System.out.println("finish with total cost : "+dtg.getCost());
				}
				else {
					
				}
			}
		}
	}
}
