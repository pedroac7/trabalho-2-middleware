package br.ufrn.middleware.lifecycle;

public enum LifecycleType {
    STATIC_INSTANCE,
    PER_REQUEST_INSTANCE,
    CLIENT_DEPENDENT_INSTANCE,
    LAZY_ACQUISITION,
    POOLING,
    LEASING,
    PASSIVATION
}
