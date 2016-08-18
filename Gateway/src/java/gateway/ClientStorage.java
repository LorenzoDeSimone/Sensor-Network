//Lorenzo De Simone N880090

package gateway;

import ClientInfo.ClientInfo;
import ClientInfo.ClientMessage;
import com.google.gson.Gson;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import nodenetwork.NodeInfo;

public class ClientStorage
{
  private static ClientStorage Instance=null;  
  private final HashSet<ClientInfo> Clients;
  
  private ClientStorage()
  {Clients = new HashSet<ClientInfo>();}
  
  public synchronized static ClientStorage getInstance()
  {
    if(Instance==null)
      Instance=new ClientStorage();
    return Instance; 
  }
  
  public synchronized boolean addClient(ClientInfo Client)
  {return Clients.add(Client);}
  
  public synchronized boolean removeClient(ClientInfo Client)
  {return Clients.remove(Client);}
  
  public synchronized void notifyAll(ClientMessage Message) 
  {
    Gson gson= new Gson();      
    for(ClientInfo Client: Clients)
    {
      try
      {
        NodeInfo Node= Message.getNode();
        Socket OutputSocket = new Socket(Client.getAddress(),Integer.parseInt(Client.getPort()));
        DataOutputStream outToClient = new DataOutputStream(OutputSocket.getOutputStream());
        
        String JSonMessage = gson.toJson(Message);
        outToClient.writeBytes(JSonMessage+'\n');
        OutputSocket.close();
      }
      catch (IOException ex)
      {Logger.getLogger(ClientStorage.class.getName()).log(Level.SEVERE, null, ex);}
    }
  }
}


