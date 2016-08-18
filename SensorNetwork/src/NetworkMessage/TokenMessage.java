//Lorenzo De Simone N880090

package NetworkMessage;

import nodenetwork.NetworkToken;
import nodenetwork.NodeInfo;

public class TokenMessage extends Message
{
  private NetworkToken Token;
  
  //Empty constructor for gson conversion
  private TokenMessage()
  {super(null);}
  
  public TokenMessage(NodeInfo SenderInfo, NetworkToken Token)
  {
    super(SenderInfo);
    this.Token=Token;
  }
  
  public NetworkToken getToken()
  {return Token;}
}
