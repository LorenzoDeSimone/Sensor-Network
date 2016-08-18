//Lorenzo De Simone N880090

package client;

import java.awt.GraphicsDevice;
import java.awt.MouseInfo;
import java.awt.Rectangle;
import java.awt.Window;

public class Client 
{
  public static void main(String[] args)
  {
    int i,nClients=1;
    
    for(i=1;i<=nClients;i++)
    {LoginMenu Interface= new LoginMenu();}
    
  }
  
  public static final void centerWindow(final Window window) 
  {
    GraphicsDevice screen = MouseInfo.getPointerInfo().getDevice();
    Rectangle r = screen.getDefaultConfiguration().getBounds();
    int x = (r.width - window.getWidth()) / 2 + r.x;
    int y = (r.height - window.getHeight()) / 2 + r.y;
    window.setLocation(x, y);
  }
}
