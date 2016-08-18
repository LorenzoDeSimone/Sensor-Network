package sensors;

import java.util.List;

/**
 * Created by civi on 22/04/16.
 */
public interface Buffer<T> {
    void add(T t);
    List<T> readAllAndClean();
}
