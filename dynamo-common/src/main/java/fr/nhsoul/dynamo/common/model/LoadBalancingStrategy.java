package fr.nhsoul.dynamo.common.model;

public enum LoadBalancingStrategy {
    ROUND_ROBIN,
    LEAST_PLAYERS,
    RANDOM,
    FIRST_AVAILABLE
}