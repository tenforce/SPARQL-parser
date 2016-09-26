package com.tenforce.semtech.SPARQLParser.SPARQLStatements;

import java.util.HashSet;
import java.util.Set;

/**
 * A simple statement is basicly a String
 */
public class SimpleStatement implements IStatement {
    // the string
    private String statement;

    // a hashset with the unknowns
    private Set<String> unknowns = new HashSet<String>();

    /**
     * default constructor
     *
     * since a simple statement is just a string you can pass it here already
     * @param statement
     */
    public SimpleStatement(String statement)
    {
        this.statement = statement;
        calculateUnknowns();
    }

    /**
     * extracts the unknowns in the statement
     */
    private void calculateUnknowns()
    {
        // to detect the unknowns we split the statement into blocks
        for(String token : this.statement.split(" "))
        {
            // removing ), (
            token.replace("(","").replace(")","");

            if(token.startsWith("?"))
            {
                // if the token starts with ? it is a unknown
                this.unknowns.add(token.substring(1, token.length()));
            }
        }
    }

    /**
     * @return the hashset with unknowns
     */
    public Set<String> getUnknowns()
    {
        return this.unknowns;
    }

    /**
     * @return this.statement
     */
    public String toString()
    {
        return statement.toString();
    }

    /**
     * @return a clone of this object
     */
    public IStatement clone()
    {
        return new SimpleStatement(this.statement);
    }

    /**
     * the type of this object is always a simple statement
     * @return StatementType.SIMPLE
     */
    public StatementType getType() {
        return StatementType.SIMPLE;
    }


    /**
     * this will propagate the replacement of ALL subsequent graph statements with the new
     * graph name.
     *
     * note that to remove all graph statements you can just pass an empty string as parameter
     *
     * @param newGraph the name of the new graph
     */
    public void replaceGraphStatements(String newGraph)
    {
    }

    /**
     * this will propagate the replacement of ALL subsequent graph statements which are
     * equal to the oldGraph's name. All graph statements targetting other graphs will remain
     * untouched.
     *
     * @param oldGraph the name of tha graph that needs be replaced
     * @param newGraph the new graph name
     */
    public void replaceGraphStatements(String oldGraph, String newGraph)
    {
    }

    /**
     * a simple statement cannot 'have' a graph, therefor it returns null
     * when asked
     *
     * @return null
     */
    public String getGraph()
    {
        return null;
    }
}
