package com.mongodb.shardsync;

import static com.mongodb.client.model.Filters.eq;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCredential;
import com.mongodb.MongoTimeoutException;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.model.Mongos;
import com.mongodb.model.Shard;
import com.mongodb.model.ShardCollection;

/**
 * This class encapsulates the client related objects needed for each source and
 * destination
 *
 */
public class ShardClient {
    
    private static Logger logger = LoggerFactory.getLogger(ShardClient.class);
    
    private static final String MONGODB_SRV_PREFIX = "mongodb+srv://";
    
    private final static List<Document> countPipeline = new ArrayList<Document>();
    static {
        countPipeline.add(Document.parse("{ $group: { _id: null, count: { $sum: 1 } } }"));
        countPipeline.add(Document.parse("{ $project: { _id: 0, count: 1 } }"));
    }

    private String name;
    private String version;
    private List<Integer> versionArray;
    private MongoClientURI mongoClientURI;
    private MongoClient mongoClient;
    private MongoDatabase configDb;
    private Map<String, Shard> shardsMap = new LinkedHashMap<String, Shard>();
    private String username;
    private String password;
    private MongoClientOptions mongoClientOptions;
    
    private List<Mongos> mongosList = new ArrayList<Mongos>();
    private List<MongoClient> mongosMongoClients = new ArrayList<MongoClient>();
    
    private Map<String, ShardCollection> collectionsMap = new TreeMap<String, ShardCollection>();
    
    private List<MongoClient> shardMongoClients = new ArrayList<MongoClient>();
    
    public ShardClient(String name, String clusterUri) {
        this.name = name;
        this.mongoClientURI = new MongoClientURI(clusterUri);;
        
        this.username = mongoClientURI.getUsername();
        if (username != null) {
            this.password = String.valueOf(mongoClientURI.getPassword());
        }
        
        this.mongoClientOptions = mongoClientURI.getOptions();
        
        boolean isSRVProtocol = clusterUri.startsWith(MONGODB_SRV_PREFIX);
        if (isSRVProtocol) {
            throw new RuntimeException(
                    "mongodb+srv protocol not supported use standard mongodb protocol in connection string");
        }

        // We need to ensure a consistent connection to only a single mongos
        // assume that we will only use the first one in the list
        if (mongoClientURI.getHosts().size() > 1) {
            throw new RuntimeException("Specify only a single mongos in the connection string");
        }
        
        mongoClient = new MongoClient(mongoClientURI);
        
        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClient.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        
        List<ServerAddress> addrs = mongoClient.getServerAddressList();
        mongoClient.getDatabase("admin").runCommand(new Document("ping", 1));
        //logger.debug("Connected to source");
        configDb = mongoClient.getDatabase("config").withCodecRegistry(pojoCodecRegistry);
        populateShardList();
        
        Document destBuildInfo = mongoClient.getDatabase("admin").runCommand(new Document("buildinfo", 1));
        version = destBuildInfo.getString("version");
        versionArray = (List<Integer>)destBuildInfo.get("versionArray");
        logger.debug(name + ": MongoDB version: " + version);
        
        populateMongosList();
    }

    private void populateShardList() {
        
        MongoCollection<Shard> shardsColl = configDb.getCollection("shards", Shard.class);
        FindIterable<Shard> shards = shardsColl.find().sort(Sorts.ascending("_id"));
        for (Shard sh : shards) {
            shardsMap.put(sh.getId(), sh);
        }
        logger.debug(name + ": populateShardList complete, " + shardsMap.size() + " shards added");
    }
    
    private void populateMongosList() {
        MongoCollection<Mongos> mongosColl = configDb.getCollection("mongos", Mongos.class);
        // LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        // TODO this needs to take into account "dead" mongos instances
        mongosColl.find().sort(Sorts.ascending("ping")).into(mongosList);
        for (Mongos mongos : mongosList) {
            String uri = null;
            if (username != null && password != null) {
                uri = "mongodb://" + username + ":" + password + "@" + mongos.getId();
            } else {
                uri = "mongodb://" + mongos.getId();
            }
            
            MongoClientOptions.Builder builder = new MongoClientOptions.Builder(mongoClientOptions);
            MongoClientURI clientUri = new MongoClientURI(uri, builder);
            logger.debug(name + " mongos: " + clientUri);
            MongoClient client = new MongoClient(clientUri);
            mongosMongoClients.add(client);
        }
        logger.debug("populateMongosList complete, " + mongosMongoClients.size() + " mongosMongoClients added");
    }
    
    public void populateCollectionList() {
        MongoCollection<ShardCollection> shardsColl = configDb.getCollection("collections", ShardCollection.class);
        FindIterable<ShardCollection> colls = shardsColl.find(eq("dropped", false)).sort(Sorts.ascending("_id"));
        for (ShardCollection c : colls) {
            collectionsMap.put(c.getId(), c);
        }
    }
    
    public void populateShardMongoClients() {
       // MongoCredential sourceCredentials = mongoClientURI.getCredentials();
        
        for (Shard shard : shardsMap.values()) {
            String host = shard.getHost();
            String seeds = StringUtils.substringAfter(host, "/");
            String uri = null;
            if (username != null && password != null) {
                uri = "mongodb://" + username + ":" + password + "@" + seeds;
            } else {
                uri = "mongodb://" + seeds;
            }
            
            MongoClientOptions.Builder builder = new MongoClientOptions.Builder(mongoClientOptions);
            MongoClientURI clientUri = new MongoClientURI(uri, builder);
            MongoClient client = new MongoClient(clientUri);
            
            client.getDatabase("admin").runCommand(new Document("ping", 1));
            logger.debug("Connected to shard host: " + host);
            shardMongoClients.add(client);
        }
    }
    
    public void dropDatabases(List<String> databasesList) {
        for (MongoClient c : shardMongoClients) {
            for (String dbName : databasesList) {
                logger.debug("Dropping " + dbName + " on " + c.getConnectPoint());
                c.dropDatabase(dbName);
            }
        }
    }
    
    public static Number getCollectionCount(MongoDatabase db, String collectionName) {
        Document result = db.getCollection(collectionName).aggregate(countPipeline).first();
        Number count = null;
        if  (result != null) {
            count = (Number)result.get("count");
        }
        return count;
    }
    
    public MongoCollection<Document> getChunksCollection() {
        return configDb.getCollection("chunks");
    }
    
    public MongoCollection<Document> getDatabasesCollection() {
        return configDb.getCollection("databases");
    }
    
    public void createDatabase(String databaseName) {
        String tmpName = "tmp_ShardConfigSync_" + System.currentTimeMillis();
        mongoClient.getDatabase(databaseName).createCollection(tmpName);
        mongoClient.getDatabase(databaseName).getCollection(tmpName).drop();
    }
    
    public void flushRouterConfig() {
        logger.debug(String.format("flushRouterConfig() for %s mongos routers", mongosMongoClients.size()));
        for (MongoClient client : mongosMongoClients) {
            Document flushRouterConfig = new Document("flushRouterConfig", true);
            
            try {
                logger.debug(String.format("flushRouterConfig for mongos %s", client.getAddress()));
                client.getDatabase("admin").runCommand(flushRouterConfig);
            } catch (MongoTimeoutException timeout) {
                logger.debug("Timeout connecting", timeout);
            }
        }
    }
    
    public void stopBalancer() {
        if (versionArray.get(0) == 2 || (versionArray.get(0) == 3 && versionArray.get(1) <= 2)) {
            Document balancerId = new Document("_id", "balancer");
            Document setStopped = new Document("$set", new Document("stopped", true));
            UpdateOptions updateOptions = new UpdateOptions().upsert(true);
            configDb.getCollection("settings").updateOne(balancerId, setStopped, updateOptions);
        } else {
            mongoClient.getDatabase("admin").runCommand(new Document("balancerStop", 1));
        }
    }
    
    public Document adminCommand(Document command) {
        return mongoClient.getDatabase("admin").runCommand(command);
    }


    public String getVersion() {
        return version;
    }

    public List<Integer> getVersionArray() {
        return versionArray;
    }

    public Map<String, Shard> getShardsMap() {
        return shardsMap;
    }

    public Map<String, ShardCollection> getCollectionsMap() {
        return collectionsMap;
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    public MongoDatabase getConfigDb() {
        return configDb;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public MongoClientOptions getOptions() {
        return mongoClientOptions;
    }

    public List<MongoClient> getMongosMongoClients() {
        return mongosMongoClients;
    }

    public MongoCredential getCredentials() {
        return mongoClientURI.getCredentials();
    }

    public void checkAutosplit() {
        logger.debug(String.format("checkAutosplit() for %s mongos routers", mongosMongoClients.size()));
        for (MongoClient client : mongosMongoClients) {
            Document getCmdLine = new Document("getCmdLineOpts", true);
            Boolean autoSplit = null;
            try {
                //logger.debug(String.format("flushRouterConfig for mongos %s", client.getAddress()));
                Document result = adminCommand(getCmdLine);
                Document parsed = (Document)result.get("parsed");
                Document sharding = (Document)parsed.get("sharding");
                if (sharding != null) {
                    sharding.getBoolean("autoSplit");
                }
                if (autoSplit != null && !autoSplit) {
                    logger.debug("autoSplit disabled for " + client.getAddress());
                } else {
                    logger.warn("autoSplit NOT disabled for " + client.getAddress());
                }
            } catch (MongoTimeoutException timeout) {
                logger.debug("Timeout connecting", timeout);
            }
        }
        
    }
    
    

}
