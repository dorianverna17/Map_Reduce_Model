import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ForkJoinPool;

public class Tema2 {
    public static final String separators = "[\r\n\t|\"*=‘+_\\-&^%$#@!\\(\\){}\\]\\['><\\~?/:.;, ]";
    public static int bytes_read;

    public static HashMap<String, ArrayList<ReduceTask>> reduce_tasks;
    public static ArrayList<ArrayList<String>> worker_assignment;

    // final results -> are sorted at the final
    public static ArrayList<Result> results;

    // synchronization elements
    public static final Object map_lock = new Object();
    public static final Object result_lock = new Object();
    public static CyclicBarrier barrier;

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: Tema2 <workers> <in_file> <out_file>");
            return;
        }

        // get the argument values
        int workers_count = Integer.parseInt(args[0]);

        String in_file = args[1];
        String out_file = args[2];

        // read from file
        File file_read = new File(in_file);
        Scanner myScanner = null;
        try {
            myScanner = new Scanner(file_read);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int bytes_count = 0;
        int files_count = 0;
        if (myScanner != null) {
            bytes_count = Integer.parseInt(myScanner.nextLine());
            files_count = Integer.parseInt(myScanner.nextLine());
        }
        bytes_read = bytes_count;

        ArrayList<String> doc_files = new ArrayList<>(files_count);
        for (int i = 0; i < files_count; i++) {
            doc_files.add(myScanner.nextLine());
        }

        // Array of tasks for each worker
        ArrayList<ArrayList<MapTask>> task_list = new ArrayList<>(workers_count);
        for (int i = 0; i < workers_count; i++) {
            task_list.add(new ArrayList<>());
        }

        // initialize the Reduce Tasks
        reduce_tasks = new HashMap<>();

        // initialize the list with assignments
        // here I retain the documents that the
        // worker with a certain id should
        // operate on during the reduce task
        worker_assignment = new ArrayList<>(workers_count);
        int current_worker = 0;

        // here I create the tasks
        int worker = 0;
        for (int i = 0; i < doc_files.size(); i++) {
            String file_name = doc_files.get(i);
            File f = new File(file_name);

            reduce_tasks.putIfAbsent(file_name, new ArrayList<>());

            long size_doc = f.length();
            long cursor = 0;

            if (i < workers_count)
                worker_assignment.add(new ArrayList<>());
            worker_assignment.get(current_worker).add(file_name);

            current_worker++;
            if (current_worker == workers_count) {
                current_worker = 0;
            }

            while (cursor < size_doc) {
                task_list.get(worker).add(new MapTask(file_name, cursor, f.length()));
                cursor += bytes_count;
                worker++;

                if (worker == workers_count) {
                    worker = 0;
                }
            }
        }
        if (doc_files.size() < workers_count) {
            for (int i = 0; i < workers_count - doc_files.size(); i++) {
                worker_assignment.add(new ArrayList<>());
            }
        }

        // initialize the results
        results = new ArrayList<>();

        // initialize the barrier
        barrier = new CyclicBarrier(workers_count);

        // create the Map-Reduce Tasks
        // ie the workers
        Thread[] threads = new Thread[workers_count];
        for (int i = 0; i < workers_count; i++) {
            threads[i] = new Thread(new MapReduceTask(task_list.get(i), i));
            threads[i].start();
        }

        // finish the Map-Reduce operation
        for (int i = 0; i < workers_count; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Collections.sort(results);

        // write to file
        File file_write = new File(out_file);
        Writer writer_file = null;
        try {
            writer_file = new FileWriter(file_write);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < results.size() - 1; i++) {
            String name_file = results.get(i).doc_name;
            String name_file_less;
            name_file_less = name_file.substring(name_file.lastIndexOf("/") + 1);
            if (writer_file != null) {
                try {
                    writer_file.write(name_file_less + "," +
                            String.format("%.2f", results.get(i).rank) + "," + results.get(i).max_length + "," +
                            results.get(i).max_length_count + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (writer_file != null) {
            if (results.size() != 0) {
                Result res = results.get(results.size() - 1);
                String name_file = res.doc_name;
                String name_file_less;
                name_file_less = name_file.substring(name_file.lastIndexOf("/") + 1);
                try {
                    writer_file.write(name_file_less + "," + String.format("%.2f", res.rank) + ","
                            + res.max_length + "," + res.max_length_count);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                writer_file.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
