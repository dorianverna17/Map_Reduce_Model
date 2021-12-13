import java.util.ArrayList;
import java.util.HashMap;

public class MapTask {
    public String doc_name;
    public long offset;
    public long doc_size;

    // results
    HashMap<Integer, Integer> words;
    ArrayList<String> max_word;

    public MapTask(String doc_name, long offset, long doc_size) {
        this.doc_name = doc_name;
        this.offset = offset;
        this.doc_size = doc_size;

        words = new HashMap<>();
        max_word = new ArrayList<>();
    }

    @Override
    public String toString() {
        return "T(" + doc_name + ' ' + offset + ' ' + doc_size + ')';
    }
}
