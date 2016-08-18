//Lorenzo De Simone N880090

package nodenetwork;

import NetworkMessage.ACKMessage;
import NetworkMessage.ChangeNextMessage;
import NetworkMessage.EnterNetworkMessage;
import NetworkMessage.NetworkPacket;
import NetworkMessage.TokenMessage;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.HashSet;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.JerseyWebTarget;
import static java.lang.Thread.sleep;

public class MessageHandlerThread extends Thread
{
  private final String JsonPacket;
  private final Node MyNode;
  
  public MessageHandlerThread(Node MyNode, String JsonPacket)
  {
    this.MyNode=MyNode;
    this.JsonPacket=JsonPacket;
  }
  
  @Override
  public void run()
  {
    Gson gson= new Gson();

    JsonReader Reader = new JsonReader(new StringReader(JsonPacket));
    Reader.setLenient(true);
    NetworkPacket ReceivedPacket= gson.fromJson(Reader,NetworkPacket.class);
    
    String MessageClass=ReceivedPacket.getMessageClass();//Gets name of the instance to unmarshal JsonMessage correctly
    String JsonMessage= ReceivedPacket.getJsonMessage();//Gets instance of the message currently encoded in JSON

    //Received request to enter the network right ahead of this node
    if(MessageClass.equals(EnterNetworkMessage.class.getSimpleName()))
      handleEnterNetworkMessage(JsonMessage);
    //Received request to chanfe my next with the one written inside this packet
    else if(MessageClass.equals(ChangeNextMessage.class.getSimpleName()))
      handleChangeNextMessage(JsonMessage);
    //Received an ACK for ChangeNextMessage
    else if(MessageClass.equals(ACKMessage.class.getSimpleName()))
      handleACKMessage(JsonMessage);
    //Received the token
    else if(MessageClass.equals(TokenMessage.class.getSimpleName()))
      handleTokenMessage(JsonMessage);
  }
  
  private void handleEnterNetworkMessage(String JsonMessage)
  {
    Gson gson=new Gson();
    EnterNetworkMessage Message= gson.fromJson(JsonMessage,EnterNetworkMessage.class);
    NodeInfo SenderInfo= Message.getSenderInfo();
    System.out.println(MyNode.getID()+" Received EnterNetworkMessage from "+ SenderInfo.getID());
    HashSet<NodeInfo> PendingRequests=MyNode.getPendingRequests();
    synchronized(PendingRequests)
    {PendingRequests.add(SenderInfo);}
  }
   
  private void handleChangeNextMessage(String JsonMessage)
  {
    Gson gson=new Gson();
    ChangeNextMessage Message= gson.fromJson(JsonMessage,ChangeNextMessage.class);
    NodeInfo SenderInfo= Message.getSenderInfo();
    NodeInfo NextInfo= Message.getNextInfo();
    System.out.println(MyNode.getID()+" Received ChangeNextMessage from "+ SenderInfo.getID());
    MyNode.setNext(NextInfo);
    
    boolean[] IsConnected=MyNode.getConnectionStatus();
    
    synchronized(IsConnected)
    {
      if(!IsConnected[0])//Checks if it's the first time this node receives this message
      {//If so, it means it just received a confirm it is now part of the network
        IsConnected[0]=true; 
        JerseyClient Client = JerseyClientBuilder.createClient();
        JerseyWebTarget Service = Client.target(UriBuilder.fromUri(MyNode.getNodeInfo().getGatewayAddress()).build());
        String JsonNode =gson.toJson(MyNode.getNodeInfo());

        Response Answer= Service.path("GatewayServices").path("Network").path("notifyConnection")
        .request(MediaType.APPLICATION_JSON)
        .post(Entity.entity(JsonNode,MediaType.APPLICATION_JSON));
    
        IsConnected.notify();
      }
    }
    
    ACKMessage ACK= new ACKMessage(MyNode.getNodeInfo());
    NetworkPacket ACKPacket= new NetworkPacket(ACK);
    MyNode.sendMessage(SenderInfo, ACKPacket);
  }
  
  private void handleACKMessage(String JsonMessage)
  {
    Gson gson=new Gson();
    ACKMessage Message= gson.fromJson(JsonMessage,ACKMessage.class);
    NodeInfo SenderInfo= Message.getSenderInfo();
    System.out.println(MyNode.getID()+" Received ACKMessage from "+ SenderInfo.getID());
    HashSet<NodeInfo> PendingACKs= MyNode.getPendingACKs();
    
    synchronized(PendingACKs)
    {
      if(PendingACKs.remove(SenderInfo))//Check needed if a node acts in malicuous way and sends ACK where it isn't needed
      {
        //Notifies the request handler that is waiting for all the ACKs
        if(PendingACKs.isEmpty())
          PendingACKs.notify();
      }
      else
        System.out.println("I wasn't expecting any ACK from "+SenderInfo.getID());
    }
  }
  
  private void handleTokenMessage(String JsonMessage)
  {
    Gson gson=new Gson();
    TokenMessage Message= gson.fromJson(JsonMessage,TokenMessage.class);
    NodeInfo SenderInfo= Message.getSenderInfo();
    NetworkToken Token= Message.getToken();
    System.out.println(MyNode.getID()+" Received TokenMessage from "+ SenderInfo.getID()); 
    
    //System.out.println(Token);
    if(Token.fill(MyNode.getMeasurementsBuffer().readAllAndClean()))
      Token.sendMeasurementsToGateway();
    
    
    try
    {
      //SIMULATING PROCESSING TIME
      sleep(2000);
    } 
    catch (InterruptedException ex)
    {Logger.getLogger(MessageHandlerThread.class.getName()).log(Level.SEVERE, null, ex);}
    
    
    RequestsHandlerThread RequestsHandler= new RequestsHandlerThread();
    RequestsHandler.start();
    try
    {RequestsHandler.join();}
    catch (InterruptedException ex)
    {Logger.getLogger(MessageHandlerThread.class.getName()).log(Level.SEVERE, null, ex);}
    
    if(MyNode.wantsToDisconnect())
    { 
      //If the sender is the node itself, it means it was the only node in the network
      //So it should just shut down the network: if anyone asked it to enter
      //they will just wait for timeout and create a new network
      if(SenderInfo.equals(MyNode.getNodeInfo()))
      {
        try 
        {MyNode.getInputSocket().close();}
        catch (IOException ex) 
        {Logger.getLogger(MessageHandlerThread.class.getName()).log(Level.SEVERE, null, ex);}
      }
      else
      {
        //Tells the previous node of the network(the sender) to change its next        
        ChangeNextMessage ChangeNextMex= new ChangeNextMessage(MyNode.getNodeInfo(),MyNode.getNext());
        NetworkPacket ChangeNextPacket= new NetworkPacket(ChangeNextMex);
        MyNode.sendMessage(SenderInfo, ChangeNextPacket);

        //The Node must wait for an ACK to be sure that the sender node
        //updated its next in order to leave the network in a consistent state
        //before sending the token
        HashSet<NodeInfo> PendingACKs= MyNode.getPendingACKs();
        synchronized(PendingACKs)
        {
          PendingACKs.add(SenderInfo);
          try
          {PendingACKs.wait();}//I am sure that PendingACKs is not Empty because I have the lock and I just added a Node
          catch (InterruptedException ex)
          {Logger.getLogger(MessageHandlerThread.class.getName()).log(Level.SEVERE, null, ex);}
        }
      
        try
        {
          MyNode.getInputSocket().close();//Closes ASAP the Input token to reduce the number of possible requests
          //that will inevitabily be discarded since this node is now going to exit for real
        }
        catch (IOException ex)
        {
          Logger.getLogger(MessageHandlerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
      
        //Passes the Token setting the previous node of the network(the sender) as token sender
        TokenMessage NetworkTokenMex= new TokenMessage(SenderInfo,Token);
        NetworkPacket NetworkTokenPacket= new NetworkPacket(NetworkTokenMex);
        MyNode.sendMessage(MyNode.getNext(), NetworkTokenPacket);
      }
    }
    else
    {
      //Sends the token to the next node
      TokenMessage NetworkTokenMex= new TokenMessage(MyNode.getNodeInfo(),Token);
      NetworkPacket NetworkTokenPacket= new NetworkPacket(NetworkTokenMex);
      MyNode.sendMessage(MyNode.getNext(), NetworkTokenPacket);
    }
  }
  
  private class RequestsHandlerThread extends Thread
  {
    @Override
    public void run()
    {
      HashSet<NodeInfo> PendingACKs= MyNode.getPendingACKs();
      HashSet<NodeInfo> PendingRequests,PendingRequestsCopy;
      //Gets currently nodes that have requested to enter the network setting
      //this node as their predecessor
      PendingRequests=MyNode.getPendingRequests();
      
      synchronized(PendingRequests)
      {
        PendingRequestsCopy=(HashSet<NodeInfo>)PendingRequests.clone();
        PendingRequests.clear();
      }
      
      for(NodeInfo CurrNode: PendingRequestsCopy)
      {
        System.out.println(MyNode.getID()+" handling enter request from: ["+CurrNode+"]");
            
        //Before setting the new node as next, I answer sending my previous next
        ChangeNextMessage ChangeNextMex= new ChangeNextMessage(MyNode.getNodeInfo(),MyNode.getNext());
        NetworkPacket ChangeNextPacket= new NetworkPacket(ChangeNextMex);
        
        if(MyNode.sendMessage(CurrNode, ChangeNextPacket))
        {
          MyNode.setNext(CurrNode);//Sets the sender node as new next              
          synchronized(PendingACKs)
          {PendingACKs.add(CurrNode);}
        }
      } 
      
      synchronized(PendingACKs)
      {
        if(!PendingACKs.isEmpty())//Check needed if all requests are handled before wait code-line is reached
        {
          try
          {PendingACKs.wait();}
          catch (InterruptedException ex)
          {Logger.getLogger(MessageHandlerThread.class.getName()).log(Level.SEVERE, null, ex);}
        }
      }
    }
  } 
}
