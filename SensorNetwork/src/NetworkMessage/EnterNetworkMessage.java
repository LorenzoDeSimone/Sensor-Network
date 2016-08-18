//Lorenzo De Simone N880090

package NetworkMessage;

import nodenetwork.NodeInfo;

public class EnterNetworkMessage extends Message
{
  //Empty constructor for gson conversion
  private EnterNetworkMessage()
  {super(null);}
  
  public EnterNetworkMessage(NodeInfo SenderInfo)
  {super(SenderInfo);}
}
