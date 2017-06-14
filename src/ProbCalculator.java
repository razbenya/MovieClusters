
public class ProbCalculator implements Runnable {
	private DB db = DB.getInstance();
	private int startIndex;


	public ProbCalculator(int startIndex){
		this.startIndex = startIndex;
	}

	@Override
	public void run() {
		for(int i=startIndex; i<db.getNumMovies() && i<startIndex+100;i++){
			db.writeProb(i,i,db.calcPmovie(i));
			for(int j=i+1; j<db.getNumMovies(); j++){
				double p = db.calcPpair(i,j);
				db.writeProb(i, j, p);
				db.writeProb(j, i, p);
			}
		}

	}

}
