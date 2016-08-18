//Lorenzo De Simone N880090

package nodenetwork;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.JerseyWebTarget;
import sensors.Measurement;

public class NetworkToken
{
  private final ArrayList<Measurement> Measurements;
  private final String GatewayAddress;
  
  //Empty constructor for gson
  private NetworkToken()
  {
    Measurements= null;
    GatewayAddress=null;
  }
  
  public NetworkToken(String GatewayAddress)
  {
    Measurements= new ArrayList<Measurement>(15);
    this.GatewayAddress=GatewayAddress;
  }
  
  
  public boolean fill(List<Measurement> SingleNodeMeasurements)
  {
    for(Measurement M:SingleNodeMeasurements)
    {
      Measurements.add(M);
      if(Measurements.size()==15)
        return true;
    }
    return false;
  }
  
  public void sendMeasurementsToGateway()
  {
    System.out.println("TOKEN FULL -> Rest Call to the server");
    JerseyClient Client;
    Client = JerseyClientBuilder.createClient();
    JerseyWebTarget Service = Client.target(UriBuilder.fromUri(GatewayAddress).build());
    Gson gson= new Gson();
    
    String JsonMeasurements =gson.toJson(Measurements);
  
    Response Answer= Service.path("GatewayServices").path("Network").path("postMeasurements")
      .request(MediaType.APPLICATION_JSON)
      .post(Entity.entity(JsonMeasurements,MediaType.APPLICATION_JSON));
    
    Measurements.clear();     
  }
  
  @Override
  public String toString()
  {
    String Output="***";
    for(Measurement M:Measurements)
      Output=Output+"\n["+M.getId()+"] ("+M.getType()+") ="+M.getValue();
    Output=Output+"\n***";
    return Output;
  }
}
