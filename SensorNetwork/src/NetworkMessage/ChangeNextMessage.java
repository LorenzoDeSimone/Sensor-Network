//Lorenzo De Simone N880090

package NetworkMessage;

import nodenetwork.NodeInfo;

public class ChangeNextMessage extends Message
{
  NodeInfo NextInfo;
  
  //Empty constructor for gson conversion
  private ChangeNextMessage()
  {super(null);}
  
  public ChangeNextMessage(NodeInfo SenderInfo, NodeInfo NextInfo)
  {
    super(SenderInfo);
    this.NextInfo=NextInfo;  
  }
  
  public NodeInfo getNextInfo()
  {return NextInfo;}
}
