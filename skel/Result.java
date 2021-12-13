public class Result implements Comparable {
    public String doc_name;
    public float rank;
    public int max_length;
    public int max_length_count;

    @Override
    public int compareTo(Object o) {
        Result r = (Result) o;

        if (r.rank > this.rank)
            return 1;
        else if (r.rank < this.rank)
            return -1;
        return 0;
    }
}
