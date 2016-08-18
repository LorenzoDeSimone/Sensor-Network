//Lorenzo De Simone N880090

package NetworkMessage;

import com.google.gson.Gson;

public class NetworkPacket 
{
  private String JsonMessage;
  private String MessageClass;
  
  //Empty constructor for Json conversion
  private NetworkPacket()
  {}
  
  public NetworkPacket(Message M)
  {
    Gson gson= new Gson();
    MessageClass=M.getClass().getSimpleName();
    JsonMessage=gson.toJson(M);
  }
  
  public String getJsonMessage()
  {return JsonMessage;}
  
  public String getMessageClass()
  {return MessageClass;}
  
  public String toString()
  {return "["+MessageClass+"] "+JsonMessage;}
}
