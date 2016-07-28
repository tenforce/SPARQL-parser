package main.java.com.tenforce.semtech.SPARQLParser.SPARQLStatements;

import java.util.Set;

/**
 * Created by langens-jonathan on 20.07.16.
 */
public interface IStatement {
    public String toString();
    public Set<String> getUnknowns();
}
