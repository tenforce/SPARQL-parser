package main.java.com.tenforce.semtech.SPARQLParser.SPARQLStatements;

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
}
