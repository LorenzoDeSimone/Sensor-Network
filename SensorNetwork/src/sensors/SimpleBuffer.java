//Lorenzo De Simone N880090

package sensors;

import java.util.List;
import sortedlist.SortedList;

public class SimpleBuffer<T extends Measurement> implements Buffer<T>
{
  private final SortedList<T> MyMeasurements;
  
  public SimpleBuffer()
  {MyMeasurements=new SortedList<T>();}
  
  @Override
  public synchronized void add(T Measurement)
  {MyMeasurements.add(Measurement);}

  @Override
  public synchronized List<T> readAllAndClean()
  {
    List<T> Measurements=(List<T>)MyMeasurements.clone();
    MyMeasurements.clear();
    return Measurements;
  }
}
