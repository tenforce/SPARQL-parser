package com.tenforce.semtech.SPARQLParser.SPARQLStatements;

import java.util.Set;

/**
 * Created by langens-jonathan on 20.07.16.
 *
 * Interface to implement to be a statement.
 *
 * We expect the minimum for each statement is that a statement knows:
 * - how to present itself as a string
 * - which unknowns are inside it
 * - what kind of statement it is
 */
public interface IStatement {
    /**
     * The types of statements we foresee. At this point we have:
     * - ask
     * - block
     * - construct
     * - describe
     * - parentheses-block (in SPARQL it is valid to insert something
     *   between { and } without it preceded by 'where' or 'insert' or ...
     * - select-block
     * - simple statement (this is basicly just a string)
     * - update-block (insert and delete)
     * - where-block
     */
    public enum StatementType
    {
        ASK, BLOCK, CONSTRUCT, DESCRIBE, PARENTHESESBLOCK, SELECTBLOCK,
        SIMPLE, UPDATEBLOCK, WHEREBLOCK
    }
    /**
     * @return a string representing this block
     */
    public String toString();

    /**
     * @return a set with the names of all unknowns in this block
     */
    public Set<String> getUnknowns();

    /**
     * the types are describe in the StatementType definition
     * @return the type of block this is
     */
    public StatementType getType();

    /**
     * @return a clone of this object
     */
    public IStatement clone();

    /**
     * this will propagate the replacement of ALL subsequent graph statements with the new
     * graph name.
     *
     * note that to remove all graph statements you can just pass an empty string as parameter
     *
     * @param newGraph the name of the new graph
     */
    public void replaceGraphStatements(String newGraph);

    /**
     * this will propagate the replacement of ALL subsequent graph statements which are
     * equal to the oldGraph's name. All graph statements targetting other graphs will remain
     * untouched.
     *
     * @param oldGraph the name of tha graph that needs be replaced
     * @param newGraph the new graph name
     */
    public void replaceGraphStatements(String oldGraph, String newGraph);
}
