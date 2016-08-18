//Lorenzo De Simone N880090

package gateway;

import ClientInfo.ClientMessage;
import ClientInfo.ClientMessage.ClientMessageType;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import nodenetwork.NodeInfo;
import java.lang.reflect.Type;
import java.util.List;
import sensors.Measurement;

@Path("Network")
public class NetworkResources
{
    public NetworkResources()
    {}
    
    //Node Registration: returns ok if ID is valid and registers it
    @POST
    @Path("register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerNode(String JsonNode)
    {
      NetworkStorage Storage=NetworkStorage.getInstance();
      Gson gson = new Gson();
      NodeInfo NewNode= gson.fromJson(JsonNode, NodeInfo.class);
      if(Storage.register(NewNode))    
        return Response.status(Response.Status.ACCEPTED).build();
      else
        return Response.status(Response.Status.FORBIDDEN).build();
    }
    
    //Node connection attempt: it adds the node to pending nodes if not present.
    //Always returns the network(nodes that are actually connected to the token ring)
    @POST
    @Path("addPendingNode")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response reconnect(String JsonNode)
    {
      NetworkStorage Storage=NetworkStorage.getInstance();
      Gson gson = new Gson();
      NodeInfo NewNode= gson.fromJson(JsonNode, NodeInfo.class);
      String JsonNetwork=gson.toJson(Storage.addPendingNode(NewNode));          
      return Response.status(Response.Status.ACCEPTED).entity(JsonNetwork).build();
    }
    
    //Node tells the gateway he modified the network correctly and it is out.
    //The gateway acknowledges that and removes it from the connected nodes list.
    //Returns forbidden if the node wasn't in the network
    @POST
    @Path("notifyDisconnection")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response notifyDisconnection(String JsonNode)
    {
      NetworkStorage Storage=NetworkStorage.getInstance();
      Gson gson = new Gson();
      NodeInfo Node= gson.fromJson(JsonNode, NodeInfo.class);
      
      if(Storage.disconnect(Node))   
      {
        //Starts a thread for client notifications
        ClientMessage Message= new ClientMessage(Node,ClientMessageType.OUT);
        ClientUpdater Updater= new ClientUpdater(Message);
        Updater.start();
        
        return Response.status(Response.Status.ACCEPTED).build();
      }
      else
        return Response.status(Response.Status.FORBIDDEN).build();     
    }

    //Node notifies it managed to connect to the network
    //The gateway acknowledges that and adds it to the connected nodes list.
    //Returns forbidden if the node wasn't in the network
    @POST
    @Path("notifyConnection")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response notifyConnection(String JsonNode)
    {
      NetworkStorage Storage=NetworkStorage.getInstance();
      Gson gson = new Gson();
      NodeInfo Node= gson.fromJson(JsonNode, NodeInfo.class);
      
      if(Storage.connect(Node)==true)   
      {
        //Starts a thread for client notifications
        ClientMessage Message= new ClientMessage(Node,ClientMessageType.IN);
        ClientUpdater Updater= new ClientUpdater(Message);
        Updater.start();
        
        return Response.status(Response.Status.ACCEPTED).build();
      }
      else
        return Response.status(Response.Status.FORBIDDEN).build();     
    }
    
    //Gets current actual network
    @GET
    @Path("getNetwork")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNetwork()
    {
      NetworkStorage Storage=NetworkStorage.getInstance();
      Gson gson = new Gson();
      String JsonNetwork=gson.toJson(Storage.getConnectedNodes());     
      return Response.status(Response.Status.ACCEPTED).entity(JsonNetwork).build();     
    }

    //Gets all nodes (even disconnected ones)
    @GET
    @Path("getAllNodes")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllNodes()
    {
      NetworkStorage Storage=NetworkStorage.getInstance();
      Gson gson = new Gson();
      String JsonNetwork=gson.toJson(Storage.getAllNodes());     
      return Response.status(Response.Status.ACCEPTED).entity(JsonNetwork).build();
    }
    
    //Adds the measurements in the list to the NetworkStorage
    @POST
    @Path("postMeasurements")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postMeasurements(String JsonMeasurements)
    {
      NetworkStorage Storage=NetworkStorage.getInstance();
      Gson gson = new Gson();
     
      Type type = new TypeToken<List<Measurement>>(){}.getType();
      List<Measurement> Measurements = gson.fromJson(JsonMeasurements, type);
            
      Storage.addMeasurements(Measurements);
      return Response.status(Response.Status.ACCEPTED).build();
    } 
    
    //Gets the last measurement for the node with the ID and the Type indicated
    //Returns conflict if there is no node with that infos 
    //or there are no measurements yet
    @GET
    @Path("getLastMeasurement/{ID}/{Type}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getLastMeasurement(@PathParam("ID") String ID, @PathParam("Type") String Type) 
    {
      NetworkStorage Storage=NetworkStorage.getInstance();
      Measurement LastMeasurement=Storage.getLastMeasurement(ID, Type);
      Response Answer;
      
      if(LastMeasurement!=null)
      {
        Gson gson = new Gson();
        String JsonLastMeasurement= gson.toJson(LastMeasurement);
        Answer=Response.status(Response.Status.ACCEPTED).entity(JsonLastMeasurement).build();
      }
      else
        Answer=Response.status(Response.Status.CONFLICT).build();
      
      return Answer;
    }
    
    //Gets the maximum measurement for the node with the ID and the Type indicated
    //between T1 and T2.
    //Returns conflict if there is no node with that infos 
    //and if there is no measurement in that time window
    @GET
    @Path("getSingleMaximum/{ID}/{Type}/{T1}/{T2}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getSingleMaximum(@PathParam("ID") String ID, @PathParam("Type") String Type,
                                  @PathParam("T1") String T1, @PathParam("T2")  String T2) 
    {
      NetworkStorage Storage=NetworkStorage.getInstance();
      Gson gson = new Gson();
      
      String MaximumValue=Storage.getSingleMaximum(ID, Type, Double.parseDouble(T1) , Double.parseDouble(T2));
      Response Answer;
      if(MaximumValue!=null)
        Answer=Response.status(Response.Status.ACCEPTED).entity(MaximumValue).build();
      else
        Answer=Response.status(Response.Status.CONFLICT).build();
      
      return Answer;
    }
    
    //Gets the minimum measurement for the node with the ID and the Type indicated
    //between T1 and T2.
    //Returns conflict if there is no node with that infos 
    //and if there is no measurement in that time window
    @GET
    @Path("getSingleMinimum/{ID}/{Type}/{T1}/{T2}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getSingleMinimum(@PathParam("ID") String ID, @PathParam("Type") String Type,
                                  @PathParam("T1") String T1, @PathParam("T2")  String T2) 
    {
      NetworkStorage Storage=NetworkStorage.getInstance();
      Gson gson = new Gson();
      
      String MinimumValue=Storage.getSingleMinimum(ID, Type, Double.parseDouble(T1) , Double.parseDouble(T2));
      Response Answer;
      if(MinimumValue!=null)
        Answer=Response.status(Response.Status.ACCEPTED).entity(MinimumValue).build();
      else
        Answer=Response.status(Response.Status.CONFLICT).build();
      
      return Answer;
    }
    
    //Gets the average measurement for the node with the ID and the Type indicated
    //between T1 and T2.
    //Returns conflict if there is no node with that infos 
    //and if there is no measurement in that time window
    @GET
    @Path("getSingleAverage/{ID}/{Type}/{T1}/{T2}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getSingleAverage(@PathParam("ID") String ID, @PathParam("Type") String Type,
                                  @PathParam("T1") String T1, @PathParam("T2")  String T2) 
    {
      NetworkStorage Storage=NetworkStorage.getInstance();
      Gson gson = new Gson();
      
      String AverageValue=Storage.getSingleAverage(ID, Type, Double.parseDouble(T1) , Double.parseDouble(T2));
      Response Answer;
      if(AverageValue!=null)
        Answer=Response.status(Response.Status.ACCEPTED).entity(AverageValue).build();
      else
        Answer=Response.status(Response.Status.CONFLICT).build();
      
      return Answer;
    }
     
    //Gets the maximum measurement for every node with the Type indicated
    //between T1 and T2.
    //Returns conflict if there is no measurement in that time window
    @GET
    @Path("getTotalMaximum/{Type}/{T1}/{T2}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getTotalMaximum(@PathParam("Type") String Type,
                                  @PathParam("T1") String T1, @PathParam("T2")String T2) 
    {
      NetworkStorage Storage=NetworkStorage.getInstance();
      Gson gson = new Gson();
      
      String Maximum=Storage.getTotalMaximum(Type, Double.parseDouble(T1) , Double.parseDouble(T2));
      Response Answer;
      if(Maximum!=null)
        Answer=Response.status(Response.Status.ACCEPTED).entity(Maximum).build();
      else
        Answer=Response.status(Response.Status.CONFLICT).build();
      
      return Answer;
    }
     
    //Gets the minimum measurement for every node with the Type indicated
    //between T1 and T2.
    //Returns conflict if there is no measurement in that time window
    @GET
    @Path("getTotalMinimum/{Type}/{T1}/{T2}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getTotalMinimum(@PathParam("Type") String Type,
                                  @PathParam("T1") String T1, @PathParam("T2")String T2) 
    {
      NetworkStorage Storage=NetworkStorage.getInstance();
      Gson gson = new Gson();
      
      String Minimum=Storage.getTotalMinimum(Type, Double.parseDouble(T1) , Double.parseDouble(T2));
      Response Answer;
      if(Minimum!=null)
        Answer=Response.status(Response.Status.ACCEPTED).entity(Minimum).build();
      else
        Answer=Response.status(Response.Status.CONFLICT).build();
      
      return Answer;
    }
    
    //Gets the average measurement for every node with the Type indicated
    //between T1 and T2.
    //Returns conflict if there is no measurement in that time window
    @GET
    @Path("getTotalAverage/{Type}/{T1}/{T2}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getTotalAverage(@PathParam("Type") String Type,
                                  @PathParam("T1") String T1, @PathParam("T2")String T2) 
    {
      NetworkStorage Storage=NetworkStorage.getInstance();
      Gson gson = new Gson();
      
      String AverageValue=Storage.getTotalAverage(Type, Double.parseDouble(T1) , Double.parseDouble(T2));
      Response Answer;
      if(AverageValue!=null)
        Answer=Response.status(Response.Status.ACCEPTED).entity(AverageValue).build();
      else
        Answer=Response.status(Response.Status.CONFLICT).build();
      
      return Answer;
    }
 }
