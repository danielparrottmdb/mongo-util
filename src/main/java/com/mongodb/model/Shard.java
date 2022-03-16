package com.mongodb.model;

public class Shard {

    private String id;
    private String host;
    private int state;
    private ShardTimestamp syncStartTimestamp;
    private String rsName;
    
    // internal property only used by sync process
    private boolean mongomirrorDropped;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public boolean isMongomirrorDropped() {
        return mongomirrorDropped;
    }

    public void setMongomirrorDropped(boolean mongomirrorDropped) {
        this.mongomirrorDropped = mongomirrorDropped;
    }

    public ShardTimestamp getSyncStartTimestamp() {
        return syncStartTimestamp;
    }

    public void setSyncStartTimestamp(ShardTimestamp syncStartTimestamp) {
        this.syncStartTimestamp = syncStartTimestamp;
    }

	public String getRsName() {
		return rsName;
	}

	public void setRsName(String rsName) {
		this.rsName = rsName;
	}

}
