package com.garbigo.collection.model;

/** Which kind of request a rating or complaint refers to; ids are unique across both, so the id alone identifies it. */
public enum RequestType {
    COLLECTION,
    SEWAGE
}