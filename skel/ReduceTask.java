import java.util.ArrayList;
import java.util.HashMap;

public class ReduceTask {
    public HashMap<Integer, Integer> maps;
    public ArrayList<String> maximal_words;

    public ReduceTask() {
        maps = new HashMap<>();
        maximal_words = new ArrayList<>();
    }

    public ReduceTask(HashMap<Integer, Integer> maps, ArrayList<String> maximal_words) {
        this.maps = maps;
        this.maximal_words = maximal_words;
    }

    @Override
    public String toString() {
        return "ReduceTask{" +
                "maps=" + maps +
                ", maximal_words=" + maximal_words +
                '}';
    }
}
