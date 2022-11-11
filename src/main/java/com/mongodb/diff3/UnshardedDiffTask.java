package com.mongodb.diff3;

import java.util.Queue;
import java.util.concurrent.Callable;

import org.bson.BsonDocument;

import com.mongodb.model.Namespace;
import com.mongodb.shardsync.ShardClient;

public class UnshardedDiffTask extends AbstractDiffTask implements Callable<DiffResult> {

    private Queue<RetryTask> retryQueue;

    public UnshardedDiffTask(ShardClient sourceShardClient, ShardClient destShardClient, String nsStr,
                             String srcShardName, String destShardName, Queue<RetryTask> retryQueue) {
        this.sourceShardClient = sourceShardClient;
        this.destShardClient = destShardClient;
        this.namespace = new Namespace(nsStr);
        this.srcShardName = srcShardName;
        this.destShardName = destShardName;
        this.retryQueue = retryQueue;
    }

    @Override
    public DiffResult call() throws Exception {
        DiffResult output;
        logger.debug("[{}] got an unsharded task ({}-{})",
                Thread.currentThread().getName(), namespace.getNamespace(), chunkString);
        this.start = System.currentTimeMillis();
        result = new DiffResult();
        result.setNs(namespace.getNamespace());
        query = new BsonDocument();
        result.setChunkString(chunkString);

        try {
            computeDiff();
        } catch (Exception me) {
            logger.error("[{}] fatal error diffing chunk, ns: {}", Thread.currentThread().getName(), namespace, me);
            result = null;
        } finally {
            closeCursor(sourceCursor);
            closeCursor(destCursor);
        }

        if (result.getFailureCount() > 0) {
            RetryStatus retryStatus = new RetryStatus(0, System.currentTimeMillis());
            RetryTask retryTask = new RetryTask(retryStatus, this, result, result.getFailedIds(), retryQueue);
            retryQueue.add(retryTask);
            logger.debug("[{}] detected {} failures and added a retry task ({}-{})",
                    Thread.currentThread().getName(), result.getFailureCount(),
                    namespace.getNamespace(), chunkString);
//            output = null;
            output = result;
        } else {
            output = result;
        }

        if (output != null) {
            logger.debug("[{}] completed an unsharded task in {} ms ({}-{})",
                    Thread.currentThread().getName(), timeSpent(System.currentTimeMillis()),
                    namespace.getNamespace(), chunkString);
        }
        return output;
    }

}
