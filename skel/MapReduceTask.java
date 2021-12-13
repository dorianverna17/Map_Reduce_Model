import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;

public class MapReduceTask implements Runnable {
    // these members are part of the Map operation
    private final ArrayList<MapTask> map_task_list;
    private final ArrayList<String> task_strings;

    // those members of the Reduce operation
    private ArrayList<ArrayList<ReduceTask>> worker_reduce_tasks;

    // worker id
    private final int id;

    public MapReduceTask(ArrayList<MapTask> map_task_list, int id) {
        this.map_task_list = map_task_list;
        this.id = id;

        this.task_strings = new ArrayList<>(map_task_list.size());
    }

    public void modifyTaskMapOperation() {
        for (int i = 0; i < map_task_list.size(); i++) {
            MapTask t = map_task_list.get(i);
            FileInputStream file = null;
            try {
                file = new FileInputStream(t.doc_name);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            byte[] byte_array = new byte[Tema2.bytes_read];

            int chars_read = 0;
            if (file != null) {
                try {
                    file.getChannel().position(t.offset);
                    int bytes_to_read = 0;
                    if (t.doc_size >= Tema2.bytes_read + t.offset)
                        bytes_to_read = Tema2.bytes_read;
                    else
                        bytes_to_read = (int) (t.doc_size - t.offset);
                    chars_read = file.read(byte_array, 0, bytes_to_read);

                    // check if the first byte read was
                    // actually part of a previously word
                    // (happens only when the offset is not 0)
                    StringBuilder actual_string_task = new StringBuilder();
                    if (t.offset != 0) {
                        byte[] previous_byte = new byte[1];

                        file.getChannel().position(t.offset - 1);
                        int res_bytes = file.read(previous_byte, 0, 1);
                        if (res_bytes == 0) {
                            System.out.println("Couldn't read from file!");
                        }
                        // if the previous byte is a separator
                        // then create a new string that start
                        // from where is it supposed to start
                        if (!Tema2.separators.contains(new String(previous_byte))) {
                            int j;
                            for (j = 0; j < bytes_to_read; j++) {
                                if (Tema2.separators.contains(new String(String.valueOf((char) byte_array[j])))) {
                                    actual_string_task = new StringBuilder(new String(Arrays.copyOfRange(byte_array, j, bytes_to_read)));
                                    break;
                                }
                            }
                        } else {
                            actual_string_task = new StringBuilder(new String(Arrays.copyOfRange(byte_array, 0, bytes_to_read)));
                        }
                    } else {
                        actual_string_task = new StringBuilder(new String(Arrays.copyOfRange(byte_array, 0, bytes_to_read)));
                    }
                    // now I have to check if it ends in the middle of a word
                    if (t.offset + bytes_to_read != t.doc_size &&
                            !Tema2.separators.contains(String.valueOf(actual_string_task.charAt(actual_string_task.length() - 1)))) {
                        byte[] next_byte = new byte[1];

                        file.getChannel().position(t.offset + bytes_to_read);
                        int res_bytes = file.read(next_byte, 0, 1);
                        if (res_bytes == 0) {
                            System.out.println("Couldn't read from file!");
                        }
                        // if the next byte is not a separator
                        // then read byte after byte until we
                        // meet the end of the word
                        String next_byte_aux = new String(next_byte);

                        int cursor_aux = 0;
                        while (!Tema2.separators.contains(next_byte_aux)) {
                            actual_string_task.append(next_byte_aux);

                            cursor_aux++;
                            if (t.offset + bytes_to_read + cursor_aux >= t.doc_size)
                                break;
                            file.getChannel().position(t.offset + bytes_to_read + cursor_aux);
                            res_bytes = file.read(next_byte, 0, 1);
                            if (res_bytes == 0) {
                                System.out.println("Couldn't read from file!");
                            }
                            next_byte_aux = new String(next_byte);
                        }
                    }
                    task_strings.add(String.valueOf(actual_string_task));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (chars_read == 0)
                System.out.println("Couldn't read from file!");
        }
    }

    public void solveMapOperation() {
        String[] words;
        HashMap<Integer, ArrayList<String>> maximal_words;
        int max_word_len;

        for (int i = 0; i < task_strings.size(); i++) {
            words = task_strings.get(i).split(Tema2.separators);

            MapTask t = map_task_list.get(i);
            int word_length;

            max_word_len = 0;
            maximal_words = new HashMap<>();

            for (int j = 0; j < words.length; j++) {
                word_length = words[j].length();
                if (word_length == 0)
                    continue;
                if (t.words.containsKey(word_length)) {
                    t.words.put(word_length, t.words.get(word_length) + 1);
                    maximal_words.get(word_length).add(words[j]);
                } else {
                    t.words.put(word_length, 1);
                    ArrayList<String> arr_aux = new ArrayList<>();
                    arr_aux.add(words[j]);
                    maximal_words.put(word_length, arr_aux);
                }
                if (max_word_len < word_length)
                    max_word_len = word_length;
            }
            if (maximal_words.get(max_word_len) != null)
                t.max_word = maximal_words.get(max_word_len);
            else
                t.max_word = new ArrayList<>();
            // put the entry in the hash map from the class Tema2 that
            // contains all the tasks for MapReduce
            synchronized (Tema2.map_lock) {
                ArrayList<ReduceTask> red_t = Tema2.reduce_tasks.get(t.doc_name);
                red_t.add(new ReduceTask(t.words, t.max_word));
            }
        }
    }

    public void assignReduceTasks() {
        worker_reduce_tasks = new ArrayList<>();

        for (int i = 0; i < Tema2.worker_assignment.get(id).size(); i++) {
            String file_name = Tema2.worker_assignment.get(id).get(i);
            worker_reduce_tasks.add(Tema2.reduce_tasks.get(file_name));
        }
    }

    public int[] compute_fibonacci(int length) {
        int[] res = new int[length];

        if (length > 0)
            res[0] = 0;
        if (length > 1)
            res[1] = 1;
        for (int i = 2; i < length; i++) {
            res[i] = res[i - 1] + res[i - 2];
        }
        return res;
    }

    public void solveReduceTasks() {
        // these variable is for the calculation of the rank
        int total_words = 0, sum = 0;

        for (int i = 0; i < worker_reduce_tasks.size(); i++) {
            Result res = new Result();
            ArrayList<ReduceTask> arr_tasks = worker_reduce_tasks.get(i);

            // create a greater Reduce task out of this list
            ReduceTask t = new ReduceTask();
            res.doc_name = Tema2.worker_assignment.get(id).get(i);
            res.max_length = 0;
            for (int j = 0; j < arr_tasks.size(); j++) {
                for (Map.Entry<Integer, Integer> entry : arr_tasks.get(j).maps.entrySet()) {
                    if (t.maps.containsKey(entry.getKey())) {
                        t.maps.put(entry.getKey(), t.maps.get(entry.getKey()) + entry.getValue());
                    } else {
                        t.maps.put(entry.getKey(), entry.getValue());
                    }
                    total_words += entry.getValue();
                }
                t.maximal_words.addAll(arr_tasks.get(j).maximal_words);
                // find the element with the largest length
                if (arr_tasks.get(j).maximal_words.size() != 0)
                    if (res.max_length < arr_tasks.get(j).maximal_words.get(0).length())
                        res.max_length = arr_tasks.get(j).maximal_words.get(0).length();
            }
            // find the number of occurences of the words with the largest length
            res.max_length_count = t.maps.get(res.max_length);

            int[] fibonacci = compute_fibonacci(res.max_length + 2);

            for (Map.Entry<Integer, Integer> entry : t.maps.entrySet()) {
                sum += fibonacci[entry.getKey() + 1] * entry.getValue();
            }
            res.rank = (float) sum / total_words;

            synchronized (Tema2.result_lock) {
                Tema2.results.add(res);
            }
        }
    }

    public void performOperation() {
        // solve the issues regarding the bundary words
        modifyTaskMapOperation();
        // solve the Map operation
        solveMapOperation();

        try {
            Tema2.barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }

        // assign Reduce tasks to workers
        assignReduceTasks();
        // solve the reduce tasks
        solveReduceTasks();
    }

    @Override
    public void run() {
        // perform operation
        performOperation();
    }
}
