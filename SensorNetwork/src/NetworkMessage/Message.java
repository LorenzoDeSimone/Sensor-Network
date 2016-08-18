//Lorenzo De Simone N880090

package NetworkMessage;

import nodenetwork.NodeInfo;

public abstract class Message 
{
  private final NodeInfo SenderInfo;
    
  //Empty constructor for gson conversion
  private Message()
  {SenderInfo=null;}
  
  protected Message(NodeInfo SenderInfo)
  {this.SenderInfo=SenderInfo;}
  
  public NodeInfo getSenderInfo()
  {return SenderInfo;}
}
