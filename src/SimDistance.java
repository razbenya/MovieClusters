import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Neta on 2017-07-06.
 */
public class SimDistance {
    private DB db;
    private HashMap<Integer,double[]> moviesVectors;
    private String userFilePath;
    private List<List<Integer>> clusters;
    private final double AVG = 3.797466938519524;
    private final double AVG2 = 1.13046979344492;
    private final int numOfgen = 18;

    public SimDistance(String path) {
        userFilePath = path;
        moviesVectors = new HashMap<>();
        db = DB.getInstance();
        clusters = new ArrayList<List<Integer>>();
       // loadVectors();
       // avgCalac();
    }
}
