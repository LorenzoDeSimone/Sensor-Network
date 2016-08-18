//Lorenzo De Simone N880090

package ClientInfo;

import nodenetwork.NodeInfo;

public class ClientMessage 
{
  public enum ClientMessageType {IN,OUT}
  private final NodeInfo Node;
  private final ClientMessageType Type;
  
  //No argument constructor for gson
  private ClientMessage()
  {
    this.Node=null;
    this.Type=ClientMessageType.OUT;
  }
  
  public ClientMessage(NodeInfo Node, ClientMessageType Type)
  {
    this.Node=Node;
    this.Type=Type;
  }
  
  public NodeInfo getNode()
  {return Node;}
  
   public ClientMessageType getType()
  {return Type;}
}
