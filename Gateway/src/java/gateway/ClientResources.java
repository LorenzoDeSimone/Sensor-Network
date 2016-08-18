//Lorenzo De Simone N880090

package gateway;

import ClientInfo.ClientInfo;
import com.google.gson.Gson;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.POST;
import javax.ws.rs.core.Response;

@Path("Client")
public class ClientResources
{
    public ClientResources()
    {}

    //Adds a client to the ClientStorage instance
    @POST
    @Path("login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(String JsonClient)
    {
      Gson gson= new Gson();
      ClientInfo Client=gson.fromJson(JsonClient, ClientInfo.class);
      ClientStorage Storage=ClientStorage.getInstance();
      
      //Returns false if a client with the same ID is already registered    
      if(Storage.addClient(Client))
        return Response.status(Response.Status.ACCEPTED).build();
      else
        return Response.status(Response.Status.FORBIDDEN).build();
    }
    
    //Removes a client to the ClientStorage instance
    @POST
    @Path("logout")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout(String JsonClient)
    {
      Gson gson= new Gson();
      ClientInfo Client=gson.fromJson(JsonClient, ClientInfo.class);
      ClientStorage Storage=ClientStorage.getInstance();
      
      //Returns false if the client is not registered to the network    
      if(Storage.removeClient(Client))    
        return Response.status(Response.Status.ACCEPTED).build();
      else
        return Response.status(Response.Status.NOT_FOUND).build();
    }
}
