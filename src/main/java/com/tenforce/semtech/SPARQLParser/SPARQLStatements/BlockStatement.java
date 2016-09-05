package com.tenforce.semtech.SPARQLParser.SPARQLStatements;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by langens-jonathan on 20.07.16.
 *
 * version 0.0.1
 *
 * Abstract class.
 *
 * The idea is that the update and where block types inherit from this block type because they allow
 * for nested versions of each other.
 *
 * A block has a certain type, a graph upon which it operates and a list of statements between it's
 * parentheses.
 *
 * The types of blocks are:
 *  - insert
 *  - delete
 *  - where
 *  - select
 */
public abstract class BlockStatement implements IStatement
{
    /**
     * the supported types of block statments
     */
    public enum BLOCKTYPE {
        INSERT, DELETE, WHERE, SELECT
    }

    // the type
    protected BLOCKTYPE type;

    // the graph upon which it operates
    protected String graph = "";

    // the statments between it's brackets
    protected List<IStatement> statements;

    /**
     * Default constructor initializes the statements list.
     */
    public BlockStatement()
    {
        statements = new ArrayList<IStatement>();
    }

    /**
     * returns the statement type
     * @return BLOCK
     */
    public StatementType getType()
    {
        return StatementType.BLOCK;
    }

    /**
     * @return the graph on which this block operates
     */
    public String getGraph()
    {
        return this.graph;
    }

    /**
     * @param graph this.graph = graph
     */
    public void setGraph(String graph){this.graph = graph;}

    /**
     * @return the statements in this block
     */
    public List<IStatement> getStatements()
    {
        return this.statements;
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
        this.graph = newGraph;
        for(IStatement s :  this.statements)
            s.replaceGraphStatements(newGraph);
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
        if(this.graph.equals(oldGraph))
            this.graph = newGraph;

        for(IStatement s : this.statements)
            s.replaceGraphStatements(oldGraph, newGraph);
    }

    /**
     * forcing subsequent classes to override the clone method
     * @return a clone of the inheriting object
     */
    public abstract BlockStatement clone();
}
