import java.io.File;
import java.nio.file.Paths;

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
				System.out.println("loading database..");
				db.loadTitles(Paths.get(args[0]+"/"+MOVIESTITLE).toString());
				db.loadDB(Paths.get(args[0]+"/"+RATINGS).toString());
				File f = new File(args[2]);
				if(!f.exists() || f.isDirectory()) { 
					System.out.println("movies subset file is missing.. ");
					System.out.println("creating a random file.");
					new SubsetGenerator().generateRandomSubset(Paths.get(args[2]).toString());
				}
				//System.out.println("reading subset file..");
				

				if("1".equals(args[1])){
					System.out.println("running pivot algorithm. ");
					System.out.println();
					Pivot p = new Pivot();
					p.getClusters(Paths.get(args[2]).toString());
					System.out.println(p.toString());
					double oldc = p.getCost();
					System.out.println("cost : "+oldc);

				}
				else {
					System.out.println("running improved algorithm.");
					System.out.println();
					ImprovedAlgorithm improved = new ImprovedAlgorithm(Paths.get(args[0]+"/"+USERS).toString());
					improved.getClusters(args[2]);
					System.out.println(improved.toString());
					double newc = improved.getCost();
					System.out.println("cost : "+newc);
				}
			}
		}
	}
}
