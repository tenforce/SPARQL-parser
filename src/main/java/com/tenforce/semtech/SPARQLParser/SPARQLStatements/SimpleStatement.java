package main.java.com.tenforce.semtech.SPARQLParser.SPARQLStatements;

import java.util.HashSet;
import java.util.Set;

/**
 * A simple statement is basicly a String
 */
public class SimpleStatement implements IStatement {
    private String statement;

    public SimpleStatement(String statement)
    {
        this.statement = statement;
    }

    public Set<String> getUnknowns()
    {
        return new HashSet<String>();
    }

    public String toString()
    {
        return statement.toString();
    }
}
