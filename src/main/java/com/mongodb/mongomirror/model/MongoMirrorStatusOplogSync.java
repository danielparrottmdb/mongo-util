package com.mongodb.mongomirror.model;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class MongoMirrorStatusOplogSync extends MongoMirrorStatus {
    
    public MongoMirrorStatusOplogSync(String stage, String phase, String errorMessage) {
		super(stage, phase, errorMessage);
	}

	private OplogSyncDetails details;

    public OplogSyncDetails getDetails() {
        return details;
    }

    public void setDetails(OplogSyncDetails details) {
        this.details = details;
    }
    
    public String getLagPretty() {
        if (details != null) {
            Duration duration = details.getLag();
            return String.format("%2dh %2dm %2ds", 
                    duration.toHours(),
                    duration.toMinutes() - TimeUnit.HOURS.toMinutes(duration.toHours()),
                    duration.getSeconds() - TimeUnit.MINUTES.toSeconds(duration.toMinutes()));
        }
        return "<unknown>";
    }

    

}
