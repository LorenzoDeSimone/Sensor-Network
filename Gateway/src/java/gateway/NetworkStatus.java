//Lorenzo De Simone N880090

package gateway;

import ClientInfo.ClientMessage;
import java.util.HashSet;
import nodenetwork.NodeInfo;

public class NetworkStatus 
{
  private final HashSet<NodeInfo> ConnectedNodes;//Node actually connected to the token ring
  private final HashSet<NodeInfo> PendingNodes;//Activea nodes that requested to enter the token ring
  
  protected NetworkStatus()
  {
    ConnectedNodes= new HashSet<NodeInfo>(); 
    PendingNodes= new HashSet<NodeInfo>();  
  }
    
  protected synchronized HashSet<NodeInfo> addPendingNode(NodeInfo Node)
  {
    if(ConnectedNodes.isEmpty())
    {
      //If there is no connected node, Node must create the network,
      //thereforse it's immediately connected.
      ConnectedNodes.add(Node);
      PendingNodes.remove(Node);//Needed only if a node starts the network after a reconnection attempt
      
      //Starts a thread for client notifications for the first node
      ClientMessage Message= new ClientMessage(Node,ClientMessage.ClientMessageType.IN);
      ClientUpdater Updater= new ClientUpdater(Message);
      Updater.start();
    }
    else
      PendingNodes.add(Node);//No problems if a node tries multiple times to add itself:
                             //Useful for reconnection attempts
    
    return ConnectedNodes;
  }
  
  protected synchronized boolean connect(NodeInfo Node)
  {
    if(PendingNodes.remove(Node))
      return ConnectedNodes.add(Node);
    
    return false;
  }
  
  protected synchronized boolean disconnect(NodeInfo Node)
  {return ConnectedNodes.remove(Node);}
  
  protected synchronized HashSet<NodeInfo> getConnectedNodes()
  {return (HashSet<NodeInfo>) ConnectedNodes.clone();}
  
}
