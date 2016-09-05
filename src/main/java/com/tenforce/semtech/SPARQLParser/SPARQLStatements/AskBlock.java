package com.tenforce.semtech.SPARQLParser.SPARQLStatements;

import com.tenforce.semtech.SPARQLParser.SPARQL.InvalidSPARQLException;
import com.tenforce.semtech.SPARQLParser.SPARQL.SplitQuery;

/**
 * Created by langens-jonathan on 25.07.16.
 *
 * version 0.0.1
 *
 * An ASK block is a block that is formatted as follows
 * ASK
 * {
 *     /REGULAR BLOCK INTERNALS/
 * }
 *
 * This function will take an iterator that is at the start of an ASK block in the token array
 * just after the ASK token has been read in. It will assume that it still has to parse everything
 * between the parentheses.
 */
public class AskBlock extends ParenthesesBlock {
    /**
     * Default constructor
     * Takes an iterator and parses until the block is ended
     *
     * @param iterator the iterator that is currently after the ASK position
     * @throws InvalidSPARQLException if the query is not a valid SPARQL query
     *
     * @result this object is instantiated as being a correct ask block
     */
    public AskBlock(SplitQuery.SplitQueryIterator iterator) throws InvalidSPARQLException
    {
        super(iterator, false);
    }

    /**
     * The standard string representation for this object. It is the representation that this block
     * would have inside a valid SPARQL query
     *
     * @return the string representation
     */
    public String toString()
    {
        String toreturn = "ASK\n{\n";

        for(IStatement statement:this.statements)
        {
            toreturn += statement.toString();
        }

        toreturn += "\n}";

        return toreturn;
    }

    /**
     * inspector that returns the type
     * @return ASK
     */
    public StatementType getType()
    {
        return StatementType.ASK;
    }
}
