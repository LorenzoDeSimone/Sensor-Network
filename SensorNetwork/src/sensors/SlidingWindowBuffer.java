//Lorenzo De Simone N880090

package sensors;

import java.util.List;
import sortedlist.SortedList;

public class SlidingWindowBuffer<T extends Measurement> implements Buffer<T>
{
  private final SortedList<T> MyMeasurements;
  private double lastAddTime,WindowSum;
  private int nWindowMeasurements;
  
  public SlidingWindowBuffer()
  {
    lastAddTime=System.currentTimeMillis();
    MyMeasurements=new SortedList<T>();
  }
  
  @Override
  public synchronized void add(T Measurement)
  { 
    long currentTime=System.currentTimeMillis();
    nWindowMeasurements++;
    WindowSum=(WindowSum+Double.parseDouble(Measurement.getValue()));
    
    //Checks if more than one second passed from last measurement    
    if(currentTime-lastAddTime>1000)
    {
      //nWindowMeasurements is always >=1 here
      Double Average=WindowSum/nWindowMeasurements;
      Measurement.setValue(""+Average);
      MyMeasurements.add(Measurement);
      WindowSum=0;
      nWindowMeasurements=0;
      lastAddTime=currentTime;
    }
  }

  @Override
  public synchronized List<T> readAllAndClean()
  {
    List<T> Measurements=(List<T>)MyMeasurements.clone();
    MyMeasurements.clear();
    return Measurements;
  }
}
