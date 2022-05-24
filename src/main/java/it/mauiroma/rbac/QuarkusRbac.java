package it.mauiroma.rbac;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;


import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;

import io.quarkus.infinispan.client.Remote;
import io.quarkus.runtime.StartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/quarkus-rbac")
public class QuarkusRbac {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuarkusRbac.class);

    @Inject
    RemoteCacheManager cacheManager;    

    @Inject
    @Remote("default")
    RemoteCache<String, String> cache;

    void onStart(@Observes StartupEvent ev) {
        loadCache();
    }


    private void loadCache(){        
        LOGGER.info("Configured Cache"+cacheManager.getCacheNames());        
        try {
			List<String> allLines = Files.readAllLines(Paths.get("/etc/rbac/quarkus-rbac.properties"));
			for (String line : allLines) {
                LOGGER.info("Read line");
                String key = line.substring(0,line.indexOf("="));
                String value = line.substring(line.indexOf("=")+1);
                LOGGER.info("Client ["+key+"] has access to applications ["+value+"]");
                cache.put(key,value);
			}
        }catch (Exception e){
            LOGGER.info("Load cache goes on error");
            e.printStackTrace();
        }
    }



    @GET
    @Path("/size")
    @Produces(MediaType.TEXT_PLAIN)
    public Response size() {
        return Response.ok(cache.size()).status(200).build();
    }

    @GET
    @Path("/auth/{clientId}/{application}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response auth(@PathParam("clientId")String client, @PathParam("application")String application) {
        LOGGER.info("authorising client ["+client+"] on top application ["+application+"]");
        if(cache.get(client)!=null){
            if(cache.get(client).contains(application)){
                return Response.ok("authorized").status(Status.OK).build();
            }
        }
        return Response.ok("not authorized").status(Status.FORBIDDEN).build();
    }

    @GET
    @Path("/reload")
    @Produces(MediaType.TEXT_PLAIN)
    public Response reload() throws Exception {
        cache.clear();
        loadCache();
        return Response.ok(cache.size()).status(Status.OK).build();        
    }

    @GET
    @Path("/clear")
    @Produces(MediaType.TEXT_PLAIN)
    public Response clear() throws Exception {
        cache.clear();
        return Response.ok("clear").status(Status.OK).build();        
    }

}
