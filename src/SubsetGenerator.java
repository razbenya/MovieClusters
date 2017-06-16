import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

public class SubsetGenerator {
	private DB db;
	public SubsetGenerator(){
		db = DB.getInstance();
	}

	private int[] shuffle(int[] array){
		int tmp;
		for(int i=array.length-1;i>=0;i--){
			int index =(int) ((Math.random()*i));
			tmp = array[i];
			array[i] = array[index];
			array[index] = tmp;
		}
		return array;
	}

	private int[] toArray(Set<Integer> set){
		int[] arr = new int[set.size()];
		int i=0;
		for(int item: set){
			arr[i] = item;
			i++;
		}
		return arr;
	}

	public void generateRandomSubset(String fileName){
		try( PrintWriter writer = new PrintWriter(fileName, "UTF-8")){
			int[] shuffleMovies = shuffle(toArray(db.getListOfMovies()));
			int j = 0;
			for(int i=0; i<shuffleMovies.length && i<500  ;i++){
				if(db.isLegal(shuffleMovies[i])){
					writer.println(shuffleMovies[i]);
					j++;
				}
			}
		}catch (IOException e) {
			e.printStackTrace();
		} 
	}

}
