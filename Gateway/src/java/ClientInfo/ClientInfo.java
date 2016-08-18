//Lorenzo De Simone N880090

package ClientInfo;

public class ClientInfo
{ 
  private final String ID,Port,Address;
  
  //No argument constructor for gson
  private ClientInfo()
  {
    this.ID=null;
    this.Port=null;
    this.Address=null;
  }
  
  public ClientInfo(String ID, String Port, String Address)
  {
    this.ID=ID;
    this.Port=Port;
    this.Address=Address;
  }
  
  public String getID()
  {return ID;}
  
  public String getPort()
  {return Port;}
  
  public String getAddress()
  {return Address;}
  
  @Override
  public boolean equals(Object O)
  {
    if(O instanceof ClientInfo)
    {
      String ID2=((ClientInfo)O).getID();
      return ID.equals(ID2);
    }
    else
      return false;
  }
  
  @Override
  public int hashCode()
  {return ID.hashCode();}
  
}