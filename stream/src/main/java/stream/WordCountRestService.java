package stream;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("word-count")
public class WordCountRestService {
    private final KafkaStreams kafkaStreams;
    private final HostInfo hostInfo;
    private Server jettyServer;

    WordCountRestService(KafkaStreams streams, HostInfo hostInfo) {
        this.kafkaStreams = streams;
        this.hostInfo = hostInfo;
    }

    @GET
    @Path("/kafka-table/{word}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getKTable(@PathParam("word") final String word) {
        System.out.println("http request handling ...");
        return String.valueOf(kafkaStreams.store(WordCount.stateStoreName,
                QueryableStoreTypes.<String, Long>keyValueStore()).get(word));
    }


    void start() throws Exception {
        ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        servletContextHandler.setContextPath("/");

        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(this);
        resourceConfig.register(JacksonFeature.class);

        ServletContainer servletContainer = new ServletContainer(resourceConfig);
        ServletHolder servletHolder = new ServletHolder(servletContainer);

        servletContextHandler.addServlet(servletHolder, "/*");

        jettyServer = new Server(hostInfo.port());
        jettyServer.setHandler(servletContextHandler);
        jettyServer.start();
    }


    void stop() {
        if (jettyServer != null) {
            try {
                jettyServer.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
