//Lorenzo De Simone N880090

package nodenetwork;

import java.util.Calendar;

public class NodeInfo 
{
  public enum SensorType{light,temperature,accelerometer}
  private final String ID, Address, Port, GatewayAddress;
  private final SensorType Type;
  private final long startTime;
  private final long midnight=computeMidnightMilliseconds();
  
  private NodeInfo()
  {
    ID=null;
    Address=null;
    Port=null;
    Type=SensorType.light;
    startTime=System.currentTimeMillis()-midnight;
    GatewayAddress=null;
  }
  
  public NodeInfo(String ID)
  {
    this.ID=ID;
    Address=null;
    Port=null;
    Type=SensorType.light;
    GatewayAddress=null;
    startTime=System.currentTimeMillis()-midnight;
  }
  
  public NodeInfo(SensorType Type, String ID, String Port, String Address, String GatewayAddress)
  {
    this.ID=ID;
    this.Address=Address;
    this.Port=Port;
    this.Type=Type;
    this.GatewayAddress=GatewayAddress;
    startTime=System.currentTimeMillis()-midnight;
  }
 
  public String getAddress()
  {return Address;}
  
  public String getID()
  {return ID;}
  
  public String getPort()
  {return Port;}
  
  public SensorType getSensorType()
  {return Type;}
  
  public String getGatewayAddress()
  {return GatewayAddress;}
  
  public String toString()
  {return "["+ID+"] ["+Address+"] ["+Port+"]"+" ("+ Type+")";}
 
  @Override
  public int hashCode()
  {return ID.hashCode();}
  
  @Override
  public boolean equals(Object O)
  {
    if(O instanceof NodeInfo)
    {
      String ID2=((NodeInfo)O).getID();
      return ID.equals(ID2);
    }
    else
      return false;
  }
  
  public long getUpTime()
  {return System.currentTimeMillis()-midnight-startTime;}

  public long getStartTime()
  {return startTime;}
  
  private static long computeMidnightMilliseconds()
  {
    Calendar c = Calendar.getInstance();
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    return c.getTimeInMillis();
  }
}