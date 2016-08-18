//Lorenzo De Simone N880090

package gateway;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import nodenetwork.NodeInfo;
import sensors.Measurement;
import sortedlist.SortedList;

public class NetworkStorage 
{
  private static NetworkStorage Instance=null;  
  private final HashMap<NodeInfo,SortedList<Measurement>> AccelerationNodes,LightNodes,TemperatureNodes;
  private final NetworkStatus NetworkNodes;
  
  private NetworkStorage()
  {
    AccelerationNodes = new HashMap<NodeInfo,SortedList<Measurement>>();
    LightNodes = new HashMap<NodeInfo,SortedList<Measurement>>();
    TemperatureNodes = new HashMap<NodeInfo,SortedList<Measurement>>();
    NetworkNodes= new NetworkStatus();
  }
  
  protected synchronized static NetworkStorage getInstance()
  {
    if(Instance==null)
      Instance=new NetworkStorage();
    return Instance; 
  }
  
  private HashMap<NodeInfo,SortedList<Measurement>> getCorrespondingMap(NodeInfo Node)
  {return getCorrespondingMap(Node.getSensorType());}
  
  private HashMap<NodeInfo,SortedList<Measurement>> getCorrespondingMap(NodeInfo.SensorType Type)
  {
    HashMap<NodeInfo,SortedList<Measurement>> Map=null;

    if(Type.name().equals("accelerometer"))
      Map=AccelerationNodes;
    else if(Type.name().equals("temperature"))
      Map=TemperatureNodes;
    else if(Type.name().equals("light"))
      Map=LightNodes;
    
    return Map;
  }
  
  private HashMap<NodeInfo,SortedList<Measurement>> getCorrespondingMap(String Type)
  {
    HashMap<NodeInfo,SortedList<Measurement>> Map=null;

    if(Type.equals("accelerometer"))
      Map=AccelerationNodes;
    else if(Type.equals("temperature"))
      Map=TemperatureNodes;
    else if(Type.equals("light"))
      Map=LightNodes;
    
    return Map;
  }
  
  protected boolean register(NodeInfo Node)
  {
    HashMap<NodeInfo,SortedList<Measurement>> Map=getCorrespondingMap(Node);
    if(Map!=null)
    { 
      synchronized(Map)
      {
        if(Map.containsKey(Node))
          return false;
        else
        {
          Map.put(Node, new SortedList<Measurement>());
          return true;
        }
      }
    }       
    return false;
  }
  
  protected boolean disconnect(NodeInfo Node)
  {return NetworkNodes.disconnect(Node);}
  
  protected HashSet<NodeInfo> addPendingNode(NodeInfo Node)
  {return NetworkNodes.addPendingNode(Node);}
  
  protected boolean connect(NodeInfo Node)
  {return NetworkNodes.connect(Node);}
  
  protected HashSet<NodeInfo> getConnectedNodes()
  {return NetworkNodes.getConnectedNodes();}
  
  protected HashSet<NodeInfo> getAllNodes()
  {
    HashSet<NodeInfo> AllNodes= new HashSet<NodeInfo>();
    
    synchronized(AccelerationNodes)
      {AllNodes.addAll(AccelerationNodes.keySet());}
    synchronized(LightNodes)
      {AllNodes.addAll(LightNodes.keySet());}
    synchronized(TemperatureNodes)
      {AllNodes.addAll(TemperatureNodes.keySet());}
    
    return AllNodes;
  }
  
  protected void addMeasurements(List<Measurement> MeasurementList)
  {
    for(Measurement CurrMeasurement: MeasurementList)
    {
      HashMap<NodeInfo,SortedList<Measurement>> Map=getCorrespondingMap(CurrMeasurement.getType());
      
      if(Map!=null)
      {
        SortedList<Measurement> Measurements;
        //DUMMY NodeInfo to search O(1) time in the HashMap
        //equals is based only on ID field, which is set
        NodeInfo Node= new NodeInfo(CurrMeasurement.getId());
        synchronized(Map)
        {Measurements=Map.get(Node);}
        if(Measurements!=null)
        {
          synchronized(Measurements)
          {Measurements.add(CurrMeasurement);}
        }
      }
    }
  }
  
  protected Measurement getLastMeasurement(String ID, String Type)
  {
    HashMap<NodeInfo,SortedList<Measurement>> Map=getCorrespondingMap(Type);
    if(Map!=null)
    {
      SortedList<Measurement> Measurements;
      //Dummy node for 0(1) search in the hashmap
      NodeInfo Node= new NodeInfo(ID);
      synchronized(Map)
      {Measurements=Map.get(Node);}
      
      if(Measurements!=null)
      {
        synchronized(Measurements)
        {
          if(!Measurements.isEmpty())
            return Measurements.getLast();//0(1) since Measurement List is sorted 
        }
      }  
    }
    return null;
  }
  
  protected String getSingleMaximum(String ID, String Type, double t1, double t2)
  {
    HashMap<NodeInfo,SortedList<Measurement>> Map=getCorrespondingMap(Type);
    String MaxValue=null;

    if(Map!=null)
    {
      SortedList<Measurement> Measurements;
      //Dummy node for 0(1) search in the hashmap
      NodeInfo Node= new NodeInfo(ID); 
      synchronized(Map)
      {Measurements=Map.get(Node);}
      
      if(Measurements!=null)
      {
        synchronized(Measurements)
        {
          for(Measurement CurrMeasurement: Measurements)
          {
            if(CurrMeasurement.getTimestamp()>=t1 && CurrMeasurement.getTimestamp()<=t2)
            {
              if(MaxValue==null || Double.parseDouble(MaxValue)<Double.parseDouble(CurrMeasurement.getValue()))
                MaxValue=CurrMeasurement.getValue();
            }
            else if(CurrMeasurement.getTimestamp()>t2)//O(logn) thanks to sorted list
              break;
          }          
        }
      }
    }
    return MaxValue;
  }
  
  protected String getSingleMinimum(String ID, String Type, double t1, double t2)
  {
    HashMap<NodeInfo,SortedList<Measurement>> Map=getCorrespondingMap(Type);
    String MinValue=null;

    if(Map!=null)
    {
      SortedList<Measurement> Measurements;
      //Dummy node for 0(1) search in the hashmap
      NodeInfo Node= new NodeInfo(ID);
      
      synchronized(Map)
      {Measurements=Map.get(Node);}
      
      if(Measurements!=null)
      {
        synchronized(Measurements)
        {
          for(Measurement CurrMeasurement: Measurements)
          {
            if(CurrMeasurement.getTimestamp()>=t1 && CurrMeasurement.getTimestamp()<=t2)
            {
              if(MinValue==null || Double.parseDouble(MinValue)>Double.parseDouble(CurrMeasurement.getValue()))
                MinValue=CurrMeasurement.getValue();
            }
            else if(CurrMeasurement.getTimestamp()>t2)//O(logn) thanks to sorted list
              break;
          }          
        }
      }
    }
    return MinValue;
  }
  
  protected String getSingleAverage(String ID, String Type, double t1, double t2)
  {
    HashMap<NodeInfo,SortedList<Measurement>> Map=getCorrespondingMap(Type);
    String AverageValue=null;
    int nValidMeasurements=0;
    double sum=0;

    if(Map!=null)
    {  
      //Dummy node for 0(1) search in the hashmap
      NodeInfo Node= new NodeInfo(ID);  
      SortedList<Measurement> Measurements;
      synchronized(Map)
      {Measurements=Map.get(Node);}

      if(Measurements!=null)
      {
        synchronized(Measurements)
        {
          for(Measurement CurrMeasurement: Measurements)
          {
            if(CurrMeasurement.getTimestamp()>=t1 && CurrMeasurement.getTimestamp()<=t2)
            {
              nValidMeasurements++;
              sum=sum+Double.parseDouble(CurrMeasurement.getValue());
            }
            else if(CurrMeasurement.getTimestamp()>t2)//O(logn) thanks to sorted list
              break;
          }          
        }
        if(nValidMeasurements>0)//Checks if there is at least one valid measurements in the range
          AverageValue=""+(sum/nValidMeasurements);
      }
    }
    return AverageValue;
  }
  
  protected String getTotalMaximum(String Type, double t1, double t2)
  {
    
    HashMap<NodeInfo,SortedList<Measurement>> Map=getCorrespondingMap(Type);
    String MaxValue=null;
    
    if(Map!=null)
    {
      synchronized(Map)
      {
        String CurrMax=null;
        for(NodeInfo N: Map.keySet())
        {
          CurrMax=getSingleMaximum(N.getID(),N.getSensorType().name(),t1,t2);
          
          if(MaxValue==null && CurrMax!=null)
            MaxValue=CurrMax;
          else if(MaxValue !=null && CurrMax!=null && Double.parseDouble(CurrMax)>Double.parseDouble(MaxValue))
            MaxValue=CurrMax;
        }
      }
    }
    return MaxValue;
  } 
  
  protected String getTotalMinimum(String Type, double t1, double t2)
  {
    
    HashMap<NodeInfo,SortedList<Measurement>> Map=getCorrespondingMap(Type);
    String MinValue=null;
    
    if(Map!=null)
    {
      synchronized(Map)
      {
        String CurrMin=null;
        for(NodeInfo N: Map.keySet())
        {
          CurrMin=getSingleMinimum(N.getID(),N.getSensorType().name(),t1,t2);
          
          if(MinValue==null && CurrMin!=null)
            MinValue=CurrMin;
          else if(MinValue !=null && CurrMin!=null && Double.parseDouble(CurrMin)<Double.parseDouble(MinValue))
            MinValue=CurrMin;
        }
      }
    }
    return MinValue;
  } 
  
  protected String getTotalAverage(String Type, double t1, double t2)
  {
    HashMap<NodeInfo,SortedList<Measurement>> Map=getCorrespondingMap(Type);
    
    String AverageValue=null;
    int nValidMeasurements=0;
    double sum=0;

    if(Map!=null)
    {
      synchronized(Map)
      {
        for(NodeInfo N:Map.keySet())
        {
          SortedList<Measurement> Measurements=Map.get(N);
          String CurrAverage=getSingleAverage(N.getID(),N.getSensorType().name(),t1,t2);
          if(Measurements!=null && CurrAverage!=null)
          {
            //Gets single node average and multiplies it for the elements of the measurement list
            sum=sum+(Double.parseDouble(CurrAverage)*Measurements.size());
            nValidMeasurements=nValidMeasurements+Measurements.size();
          }
        }
        if(nValidMeasurements>0)//Checks if there is at least one valid measurements in the range
          AverageValue=""+(sum/nValidMeasurements);
      }
    }
    return AverageValue;
  }
}

