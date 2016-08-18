//Lorenzo De Simone N880090

package NetworkMessage;

import nodenetwork.NodeInfo;

public class ACKMessage extends Message
{  
  //Empty constructor for gson conversion
  private ACKMessage()
  {super(null);}
  
  public ACKMessage(NodeInfo SenderInfo)
  {super(SenderInfo);}
}
