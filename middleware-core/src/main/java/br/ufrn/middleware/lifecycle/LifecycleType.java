package br.ufrn.middleware.lifecycle;

public enum LifecycleType {
    STATIC_INSTANCE,
    PER_REQUEST_INSTANCE,
    LAZY_ACQUISITION,
    POOLING,
}
