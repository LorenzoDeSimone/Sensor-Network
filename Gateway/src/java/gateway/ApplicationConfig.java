package gateway;

import java.util.Set;
import javax.ws.rs.core.Application;


@javax.ws.rs.ApplicationPath("GatewayServices")
public class ApplicationConfig extends Application
{
  @Override
  public Set<Class<?>> getClasses()
  {
    Set<Class<?>> resources = new java.util.HashSet<>();
    addRestResourceClasses(resources);
    return resources;
  }
   
  private void addRestResourceClasses(Set<Class<?>> resources)
  {
    resources.add(gateway.ClientResources.class);
    resources.add(gateway.NetworkResources.class);
  }    
}
