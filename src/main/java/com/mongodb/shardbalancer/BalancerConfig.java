package com.mongodb.shardbalancer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoCollection;
import com.mongodb.model.Namespace;
import com.mongodb.shardsync.BaseConfiguration;

public class BalancerConfig extends BaseConfiguration {
	
	private int checkpointIntervalMinutes;
	
	private int analyzerSleepIntervalMinutes;
	
	private int balancerPollIntervalMillis = 30000;
	
	private int balancerChunkBatchSize;
	
	private Namespace balancerStateNamespace = new Namespace("mongoCustomBalancerStats", "balancerState");
	
	private Namespace statsNamespace = new Namespace("mongoCustomBalancerStats", "chunkStats");
	
	private Namespace balancerRoundNamespace = new Namespace("mongoCustomBalancerStats", "balancerRound");
	
	private MongoCollection<Document> statsCollection;
	
	private MongoCollection<Document> balancerRoundCollection;
	
	private MongoCollection<Document> balancerStateCollection;
	
	protected Set<String> sourceShards;
	
	Map<String, NavigableMap<String, CountingMegachunk>> chunkMap;
	
	AtomicBoolean runAnalyzer = new AtomicBoolean(false);
	
	private ObjectId analysisId;

	public int getCheckpointIntervalMinutes() {
		return checkpointIntervalMinutes;
	}
	
	public int getCheckpointIntervalMillis() {
		return checkpointIntervalMinutes * 60 * 1000;
	}
	

	public void setCheckpointIntervalMinutes(int checkpointIntervalMinutes) {
		this.checkpointIntervalMinutes = checkpointIntervalMinutes;
	}

	public Namespace getStatsNamespace() {
		return statsNamespace;
	}

	public void setStatsNamespace(Namespace statsNamespace) {
		this.statsNamespace = statsNamespace;
	}

	public MongoCollection<Document> getStatsCollection() {
		return statsCollection;
	}

	public void setStatsCollection(MongoCollection<Document> statsCollection) {
		this.statsCollection = statsCollection;
	}
	
	public void setSourceShards(String[] shards) {
		this.sourceShards = new HashSet<>();
		sourceShards.addAll(Arrays.asList(shards));
	}

	public Set<String> getSourceShards() {
		return sourceShards;
	}

	public int getAnalyzerSleepIntervalMinutes() {
		return analyzerSleepIntervalMinutes;
	}
	
	public int getAnalyzerSleepIntervalMillis() {
		return analyzerSleepIntervalMinutes  * 60 * 1000;
	}
	

	public void setAnalyzerSleepIntervalMinutes(int analyzerSleepIntervalMinutes) {
		this.analyzerSleepIntervalMinutes = analyzerSleepIntervalMinutes;
	}

	public int getBalancerChunkBatchSize() {
		return balancerChunkBatchSize;
	}

	public void setBalancerChunkBatchSize(int balancerChunkBatchSize) {
		this.balancerChunkBatchSize = balancerChunkBatchSize;
	}

	public Map<String, NavigableMap<String, CountingMegachunk>> getChunkMap() {
		return chunkMap;
	}

	public void setChunkMap(Map<String, NavigableMap<String, CountingMegachunk>> chunkMap) {
		this.chunkMap = chunkMap;
	}
	
	public void setRunAnalyzer(boolean running) {
		runAnalyzer.set(running);
	}
	
	public boolean runAnalyzer() {
		return runAnalyzer.get();
	}

	public ObjectId getAnalysisId() {
		return analysisId;
	}

	public void setAnalysisId(ObjectId analysisId) {
		this.analysisId = analysisId;
	}

	public MongoCollection<Document> getBalancerRoundCollection() {
		return balancerRoundCollection;
	}

	public void setBalancerRoundCollection(MongoCollection<Document> balancerRoundCollection) {
		this.balancerRoundCollection = balancerRoundCollection;
	}

	public Namespace getBalancerRoundNamespace() {
		return balancerRoundNamespace;
	}

	public void setBalancerRoundNamespace(Namespace balancerRoundNamespace) {
		this.balancerRoundNamespace = balancerRoundNamespace;
	}

	public MongoCollection<Document> getBalancerStateCollection() {
		return balancerStateCollection;
	}

	public void setBalancerStateCollection(MongoCollection<Document> balancerStateCollection) {
		this.balancerStateCollection = balancerStateCollection;
	}

	public Namespace getBalancerStateNamespace() {
		return balancerStateNamespace;
	}

	public void setBalancerStateNamespace(Namespace balancerStateNamespace) {
		this.balancerStateNamespace = balancerStateNamespace;
	}

	public int getBalancerPollIntervalMillis() {
		return balancerPollIntervalMillis;
	}

	public void setBalancerPollIntervalMillis(int balancerPollIntervalMillis) {
		this.balancerPollIntervalMillis = balancerPollIntervalMillis;
	}
	
	

}
