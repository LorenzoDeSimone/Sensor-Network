//Lorenzo De Simone N880090

package gateway;

import ClientInfo.ClientMessage;

public class ClientUpdater extends Thread
{
  private final ClientMessage Message;
      
  protected ClientUpdater(ClientMessage Message)
  {this.Message=Message;}
     
  @Override
  public void run()
  {
    ClientStorage Storage= ClientStorage.getInstance();
    Storage.notifyAll(Message);
  }
}
    