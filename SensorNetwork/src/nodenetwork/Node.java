//Lorenzo De Simone N880090

package nodenetwork;

import NetworkMessage.EnterNetworkMessage;
import NetworkMessage.NetworkPacket;
import NetworkMessage.TokenMessage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.JerseyWebTarget;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import nodenetwork.NodeInfo.SensorType;
import sensors.AccelerometerSimulator;
import sensors.Buffer;
import sensors.LightSimulator;
import sensors.Measurement;
import sensors.SimpleBuffer;
import sensors.Simulator;
import sensors.SlidingWindowBuffer;
import sensors.TemperatureSimulator;
import java.net.InetAddress;
import java.net.UnknownHostException;
import static java.lang.Thread.sleep;

public class Node 
{ 
  private final NodeInfo Info;
  private volatile NodeInfo NextInfo;
  private BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
  private Buffer<Measurement> MeasurementsBuffer;
  private final HashSet<NodeInfo> PendingRequests= new HashSet<NodeInfo>();
  private final HashSet<NodeInfo> PendingACKs= new HashSet<NodeInfo>();;
  private ServerSocket InputSocket;
  private final boolean[] IsConnected={false};
  private volatile boolean disconnectionRequested=false; 
  private ListenerThread Listener;  
  private Simulator Sensor;  
  
  public static void main(String[] args)
  {     
    final String StringType=args[0],ID=args[1],Port=args[2],GatewayAddress=args[3];
    String NodeAddress;
    
    try
    {NodeAddress =InetAddress.getLocalHost().getHostAddress();}
    catch (UnknownHostException ex)
    {
      System.err.println("Error trying to get local address");
      return;
    }
    
    HashSet<NodeInfo> Network;
    SensorType EnumType;
    try
    {EnumType=SensorType.valueOf(StringType);}
    catch(IllegalArgumentException e)
    {
      System.err.println("Invalid Sensor Type");
      return;
    }
    Node NewNode = new Node(EnumType,ID,Port,NodeAddress,GatewayAddress);

    try
    {
      //Thread.sleep((long) (Math.random()*1000));
      NewNode.register();
    }
    catch(Exception e)
    {
      System.err.println(e.getLocalizedMessage());
      return;
    }
    
    String UserInput;
    
    while(true)
    {
      try
      {
        UserInput=NewNode.inFromUser.readLine();
        if(UserInput.equals("EXIT") || UserInput.equals("exit"))
        {
          System.out.println("TRYING TO EXIT");
          NewNode.disconnect();
          return;
        }
      }
      catch(Exception ex)
      {
        System.err.println("Exception:\n"+ex);
        return;
      }
    }
  } 
  
  public Node(SensorType Type, String ID, String Port, String Address, String GatewayAddress)
  {
    Info= new NodeInfo(Type,ID,Port,Address,GatewayAddress);
    NextInfo=null;
    
    switch (Type)
    {
      case accelerometer:
        MeasurementsBuffer= new SlidingWindowBuffer();
        Sensor= new AccelerometerSimulator(ID,MeasurementsBuffer);
        break;
      case light:
        MeasurementsBuffer= new SimpleBuffer();
        Sensor= new LightSimulator(ID,MeasurementsBuffer);
        break;
      case temperature:
        MeasurementsBuffer= new SimpleBuffer();
        Sensor= new TemperatureSimulator(ID,MeasurementsBuffer);
        break;
    }
  }
  
  public String getAddress()
  {return Info.getAddress();}
  
  public String getID()
  {return Info.getID();}
  
  public String getPort()
  {return Info.getPort();}
  
  public NodeInfo getNodeInfo()
  {return Info;}
    
  public NodeInfo getNext()
  {return NextInfo;}
  
  public String getNextID()
  {
    if(NextInfo!=null)
      return NextInfo.getID();
    else
      return null;
  }
  
  public String getNextAddress()
  {
    if(NextInfo!=null)
      return NextInfo.getAddress();
    else
      return null;
  }
  
  public String getNextPort()
  {
    if(NextInfo!=null)
      return NextInfo.getPort();
    else
      return null;
  }

  public ServerSocket getInputSocket()
  {return InputSocket;}
  
  public HashSet<NodeInfo> getPendingACKs()
  {return PendingACKs;}
  
  public HashSet<NodeInfo> getPendingRequests()
  {return PendingRequests;}
     
  public Buffer<Measurement> getMeasurementsBuffer()
  {return MeasurementsBuffer;}
  
  protected void setNext(NodeInfo NewNext)
  {NextInfo=NewNext;}
    
  public String toString()
  {
    if(NextInfo!=null)
      return "Node: "+Info.toString()+" Next:"+ NextInfo.toString();
    else
      return "Node: "+Info.toString()+" Not attached to Network";
  }
    
  protected void disconnect() throws Exception
  {
    JerseyClient Client;
    JerseyWebTarget Service;      
    Client = JerseyClientBuilder.createClient();
    Service = Client.target(UriBuilder.fromUri(Info.getGatewayAddress()).build());
    
    Gson gson = new Gson();
    String JsonNode= gson.toJson(Info);
    Response answer= Service.path("GatewayServices").path("Network").path("notifyDisconnection")
      .request(MediaType.APPLICATION_JSON)
      .post(Entity.entity(JsonNode,MediaType.APPLICATION_JSON));
   
    inFromUser.close();
    disconnectionRequested=true;
    Sensor.stopMeGently(); 
    if(answer.getStatus()==Response.Status.ACCEPTED.getStatusCode())
    { 
      System.out.println("Node ["+ getID()+"] disconnected from network");
    }
    else if(answer.getStatus()==Response.Status.FORBIDDEN.getStatusCode())
    {
      System.out.println("The node [" + getID()+"] was not registered to the network.\n"
                          + "Probably the gateway disconnected after node registration");   
      throw new Exception();
    }
    else
    {
      System.out.println("Unknown Error trying to unregister Node ID ["+ getID()+"]");       
      throw new Exception();
    }
  }
   
  protected boolean wantsToDisconnect()
  {return disconnectionRequested;} 
  
  protected boolean[] getConnectionStatus()
  {return IsConnected;}    
  
  protected boolean sendMessage(NodeInfo ReceiverNode, NetworkPacket Packet)
  {
    //System.out.println("["+Info.getID()+"]"+"is sending "+Packet+" to "+ReceiverNode.getID());
    try
    {
      Socket OutputSocket = new Socket(ReceiverNode.getAddress(),Integer.parseInt(ReceiverNode.getPort()));
      DataOutputStream outToServer = new DataOutputStream(OutputSocket.getOutputStream());
      Gson gson= new Gson();      
      String JSonMessage = gson.toJson(Packet);//Marshalling of network packet
      outToServer.writeBytes(JSonMessage+'\n');
      OutputSocket.close();
    }
    catch (IOException ex)
    {
      System.out.println("Receiver disconnected\n"+ex);
      return false;
    }
    return true;
  }      

  private void register() throws Exception
  {
    JerseyClient Client;
    JerseyWebTarget Service;     
    
    try
    {
      //Creates the listening socket for this node
      InputSocket=new ServerSocket(Integer.parseInt(getPort()));
    }
    catch (SocketException ex)
    {
      System.err.println(ex.getLocalizedMessage());
      Thread.currentThread().interrupt();
      throw ex;
    }

    Client = JerseyClientBuilder.createClient();
    Service = Client.target(UriBuilder.fromUri(Info.getGatewayAddress()).build());
    Gson gson = new Gson();
    String JsonNode= gson.toJson(Info);    
    
    Response Answer= Service.path("GatewayServices").path("Network").path("register")
      .request(MediaType.APPLICATION_JSON)
      .post(Entity.entity(JsonNode,MediaType.APPLICATION_JSON));

    if(Answer.getStatus()==Response.Status.ACCEPTED.getStatusCode())
      connect();
    else if(Answer.getStatus()==Response.Status.FORBIDDEN.getStatusCode())
    {
      System.out.println("Node ID ["+ getID()+"] already in use");
      throw new Exception();
    }
    else
    {
      System.out.println("Unknown Error trying to register ID ["+ getID()+"]");  
      System.out.println(Answer);
      throw new Exception();
    }
  }   
  
  private void connect() throws InterruptedException
  {
    JerseyClient Client = JerseyClientBuilder.createClient();
    JerseyWebTarget Service = Client.target(UriBuilder.fromUri(Info.getGatewayAddress()).build());
    
    Gson gson = new Gson();
    //Creates the thread that will listen on this socket and handle message receiving
    Listener= new ListenerThread(this);
    Listener.start();
    Thread SensorThread= new Thread(Sensor);
    SensorThread.start();
    
    NodeInfo TargetNode=null;
    while(true)
    {
      String JsonNode= gson.toJson(Info);
      Response Answer= Service.path("GatewayServices").path("Network").path("addPendingNode")
        .request(MediaType.APPLICATION_JSON)
        .post(Entity.entity(JsonNode,MediaType.APPLICATION_JSON));
          
      if(Answer.getStatus()==Response.Status.ACCEPTED.getStatusCode())
      {
        String JsonNetwork= Answer.readEntity(String.class);
        Type type = new TypeToken<HashSet<NodeInfo>>(){}.getType();
        HashSet<NodeInfo> Network = gson.fromJson(JsonNetwork, type);
       
        //getTargetNode is always called in the first iteration of the while
        //Check needed: if connection fails due to timeout and target node
        //is still in the network, this node should not try a new target
        if(!Network.contains(TargetNode))
        {
          TargetNode=getTargetNode(Network);//Gets one node as chosen predecessor

          System.out.println("Network :"+Network);
          System.out.println("Target Node :"+TargetNode);
                  
          boolean receiverSocketValid=connectToTarget(TargetNode);
        
          if(!receiverSocketValid)
            continue;
        }
        
        synchronized(IsConnected)
        { 
          //Check needed for both first node and cases in which connection attempt
          //has a very fast success.
          //It helps to avoid useless 5sec delay
          if(IsConnected[0]==true)
          {
            System.out.println("Node correctly inserted");
            break;
          }
          
          IsConnected.wait(5000);
          
          if(IsConnected[0]==true)
          {
            System.out.println("Node correctly inserted");
            break;
          }
          else
            System.out.println("Connection to "+TargetNode.getID()+" failed...");
        } 
      }
      else
      {
        System.out.println("Unknown Error trying to connect to the network");  
        System.out.println(Answer);
        try
        {disconnect();}
        catch (Exception ex)
        {Logger.getLogger(Node.class.getName()).log(Level.SEVERE, null, ex);}
      }
    }
  }
  
  private void startNetwork()
  {
    setNext(Info);
    TokenMessage NetworkTokenMex= new TokenMessage(Info,new NetworkToken(Info.getGatewayAddress()));
    NetworkPacket NetworkTokenPacket= new NetworkPacket(NetworkTokenMex);
    sendMessage(Info, NetworkTokenPacket);
    synchronized(IsConnected)//This node is connected without sending any message
    {IsConnected[0]=true;}//and there is no need for REST call since the server knows it's the first
    System.out.println("Correctly started network");
  }
  
  private NodeInfo getTargetNode(HashSet<NodeInfo> Network)
  { 
    NodeInfo TargetNode=null;
        
    //Gets just one node from the network that will be NewNode's predecessor
    for(NodeInfo N : Network)
    {
      TargetNode=N;
      break;
    }    
    return TargetNode;
  }
    
   private boolean connectToTarget(NodeInfo TargetNode)
   {
     if(TargetNode.equals(Info))
     {
       startNetwork();
       return true;
     }
     else
     {
       /*try
       {sleep(10000);}
       catch (InterruptedException ex)
       {Logger.getLogger(Node.class.getName()).log(Level.SEVERE, null, ex);}
       */
       
       EnterNetworkMessage EnterNetworkMex= new EnterNetworkMessage(Info);
       NetworkPacket EnterNetworkPacket= new NetworkPacket(EnterNetworkMex);
       return sendMessage(TargetNode, EnterNetworkPacket);
     }
   }    
}