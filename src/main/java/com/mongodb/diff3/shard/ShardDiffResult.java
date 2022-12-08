package com.mongodb.diff3.shard;

import com.mongodb.diff3.DiffResult;
import org.bson.BsonValue;

import java.util.HashSet;

public class ShardDiffResult extends DiffResult {

    protected String chunkString;

    public void addMismatchedKey(BsonValue id) {
        if (failedKeys == null) {
            failedKeys = new HashSet<>();
        }
        failedKeys.add(id);
    }

    public int getFailureCount() {
        return (failedKeys == null) ? 0 : failedKeys.size();
    }

    @Override
    public ShardDiffResult mergeRetryResult(DiffResult rr) {
        assert rr instanceof ShardDiffResult;
        ShardDiffResult srr = (ShardDiffResult) rr;
        ShardDiffResult merged = new ShardDiffResult();
        merged.matches = srr.matches + this.matches;
        merged.mismatchedKeys.addAll(rr.getMismatchedKeys());
        merged.mismatchedKeys.addAll(this.getMismatchedKeys());
        merged.failedKeys.addAll(rr.getFailedKeys());
        merged.failedKeys.addAll(this.getFailedKeys());
        merged.keysOnlyOnDest.addAll(this.keysOnlyOnDest);
        merged.keysOnlyOnDest.addAll(rr.getKeysOnlyOnDest());
        merged.keysOnlyOnSource.addAll(this.keysOnlyOnSource);
        merged.keysOnlyOnSource.addAll(rr.getKeysOnlyOnSource());
        merged.bytesProcessed = this.bytesProcessed;
        merged.namespace = srr.namespace;
        merged.chunkString = srr.chunkString;
        merged.retryable = srr.retryable;
        return merged;
    }

    @Override
    public ShardDiffResult copy() {
        ShardDiffResult copy = new ShardDiffResult();
        copy.matches = matches;
        copy.bytesProcessed = bytesProcessed;
        copy.namespace = namespace;
        copy.chunkString = chunkString;
        copy.retryable = retryable;
        copy.mismatchedKeys = new HashSet<>(mismatchedKeys);
        copy.failedKeys = new HashSet<>(failedKeys);
        copy.keysOnlyOnSource = new HashSet<>(keysOnlyOnSource);
        copy.keysOnlyOnDest = new HashSet<>(keysOnlyOnDest);
        return copy;
    }

    @Override
    public String unitLogString() {
        return namespace.getNamespace() + "-" + chunkString;
    }

    public String toString() {
        return "DiffResult [ns=" +
                namespace.getNamespace() +
                ", matches=" +
                matches +
                ", failedIds=" +
                (failedKeys == null ? 0 : failedKeys.size()) +
                ", chunk=" +
                chunkString +
                "]";
    }

    public String shortString() {
        return "status=" +
                ((failedKeys != null && failedKeys.size() > 0) ? "FAILED" : "PASSED") +
                ", ns=" +
                namespace.getNamespace() +
                ", chunk=" +
                chunkString;
    }

    public String getChunkString() {
        return chunkString;
    }

    public void setChunkString(String chunkString) {
        this.chunkString = chunkString;
    }
}