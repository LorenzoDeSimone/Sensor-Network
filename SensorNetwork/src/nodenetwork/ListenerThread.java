//Lorenzo De Simone N880090

package nodenetwork;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class ListenerThread extends Thread
{
  private final Node MyNode;
  
  public ListenerThread(Node MyNode)
  {
    this.MyNode=MyNode;
  }
  
  @Override
  public void run()
  {
    try       
    {
      ServerSocket welcomeSocket = MyNode.getInputSocket();
      
      while(true)
      {
        Socket ConnectionSocket = welcomeSocket.accept();
        BufferedReader InputStream = new BufferedReader(new InputStreamReader(ConnectionSocket.getInputStream()));
        String JsonNodeMessage =InputStream.readLine();
         
        //For each incoming message, one handler thread is created
        MessageHandlerThread MessageHandler= new MessageHandlerThread(MyNode,JsonNodeMessage);
        MessageHandler.start();
      }
    }
    catch (IOException ex)
    {System.out.println("Socket Closed");}
  }  
}
