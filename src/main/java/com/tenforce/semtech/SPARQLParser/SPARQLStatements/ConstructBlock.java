package com.tenforce.semtech.SPARQLParser.SPARQLStatements;

import com.tenforce.semtech.SPARQLParser.SPARQL.InvalidSPARQLException;
import com.tenforce.semtech.SPARQLParser.SPARQL.SplitQuery;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by langens-jonathan on 25.07.16.
 *
 * version 0.0.1
 *
 * This object represents a construct block. Usually this block will be the only block in the query.
 * It is of the form
 * CONSTRUCT
 * {
 *     /STUFF TO CONSTRUCT/
 * }
 * WHERE
 * {
 *     /WHERE CONDITIONS/
 * }
 */
public class ConstructBlock implements IStatement
{
    // the construct block as it is simple it can be represented with a string
    private String constuctBlock = "";

    // the statements in the where block
    private List<IStatement> statements = new ArrayList<IStatement>();

    // the selection modifier
    private String selectModifier = ""; // normally this is DISTINCT or REDUCE

    // the solution modifier
    private List<String> solutionModifier = new ArrayList<String>();

    // the graph upon which it operates
    private String graph = null;

    /**
     * Default constructor takes an iterator just after the CONSTRUCT token has been found and constructs
     * this object from there.
     *
     * @param iterator the iterator after the CONSTRUCT token
     * @throws InvalidSPARQLException if the query does not seem to be valid SPARQL
     *
     * @result this objecct is a fully initialized construct block
     */
    public ConstructBlock(SplitQuery.SplitQueryIterator iterator) throws InvalidSPARQLException
    {
        calculateBlock(iterator);
    }

    /**
     * constructor that is only intended to be used for cloning this object
     */
    private ConstructBlock()
    {
    }

    /**
     * Forced to override method by IStatement. For this object it will contain all unknowns
     * present in the where clause.
     *
     * @return all unknowns present in sub statements
     */
    public Set<String> getUnknowns()
    {
        Set<String> u = new HashSet<String>();
        for(IStatement s : this.statements)
            u.addAll(s.getUnknowns());
        return u;
    }

    /**
     * Calculates both the construct and the where block for this CONSTRUCT block.
     *
     * @param iterator the iterator just after the CONSTRUCT token
     * @throws InvalidSPARQLException if the query is not valid SPARQL
     *
     * @result both construct and where blocks are correctly parsed
     */
    public void calculateBlock(SplitQuery.SplitQueryIterator iterator) throws InvalidSPARQLException
    {
        if(!iterator.hasNext())
        {
            throw new InvalidSPARQLException("Invalid SPARQL: on line " + iterator.getCurrentLine() + " near: " + iterator.getPrevious());
        }

        if (!iterator.next().startsWith("{"))
        {
            throw new InvalidSPARQLException("Invalid SPARQL at line " + iterator.getCurrentLine() + " expected '{' after " + iterator.getPrevious());
        }

        while(!iterator.peekNextIncludingNewLines().equals("}"))
        {
            this.constuctBlock += iterator.nextIncludingNewLines() + " ";
        }

        if(!this.constuctBlock.isEmpty())
            this.constuctBlock = this.constuctBlock.substring(0, this.constuctBlock.length() - 1);

        iterator.next(); // should be the '}'

        if(!(iterator.next().toLowerCase().equals("where")))
        {
            throw new InvalidSPARQLException("Invalid SPARQL at line " + iterator.getCurrentLine() + " expected 'WHERE' after " + iterator.getPrevious());
        }


        if(!iterator.next().toLowerCase().equals("{"))
        {
            throw new InvalidSPARQLException("Invalid SPARQL at line " + iterator.getCurrentLine() + " expected '{' after " + iterator.getPrevious());
        }

        // read inner block now
        this.parseBlock(iterator);

        // now we might still have solution modifiers
        while(iterator.hasNext() && (iterator.peekNext().toLowerCase().equals("limit") ||
                iterator.peekNext().toLowerCase().equals("offset") ||
                iterator.peekNext().toLowerCase().startsWith("order") ||
                iterator.peekNext().toLowerCase().startsWith("group")))
        {
            if(iterator.peekNext().toLowerCase().startsWith("group"))
            {
                // group by clause
                // for now I expect this to be of the form:
                // GROUP BY ?uuid
                String group = iterator.next();
                if(!iterator.hasNext())
                {
                    throw new InvalidSPARQLException("Invalid SPARQL on line: " + iterator.getCurrentLine() + " near: " + iterator.getPrevious());
                }
                String by = iterator.next();
                if(!by.toLowerCase().equals("by"))
                {
                    throw new InvalidSPARQLException("Invalid SPARQL on line: " + iterator.getCurrentLine() + " expected 'BY' near: " + iterator.getPrevious());
                }
                if(!iterator.hasNext())
                {
                    throw new InvalidSPARQLException("Invalid SPARQL on line: " + iterator.getCurrentLine() + " near: " + iterator.getPrevious());
                }
                String variable = iterator.next();
                if(!variable.startsWith("?"))
                {
                    throw new InvalidSPARQLException("Invalid SPARQL on line: " + iterator.getCurrentLine() + " near: " + iterator.getPrevious());
                }
                this.solutionModifier.add(group + " " + by + " " + variable);

            }
            if(iterator.peekNext().toLowerCase().equals("limit") ||
                    iterator.peekNext().toLowerCase().equals("offset"))
            {
                this.solutionModifier.add(iterator.next() + " "  + iterator.next());
            }
            else
            {
                String orderClause = iterator.next() + " " + iterator.next(); // this should be ORDER BY
                if(!orderClause.toLowerCase().equals("order by"))
                {
                    throw new InvalidSPARQLException("Invalid SPARQL at line : " + iterator.getCurrentLine() + " expected 'order by' instead of '" + orderClause + "' after " + iterator.getPrevious());
                }

            }
        }

    }

    /**
     * Parses a block between a '{' and a '}' token and returns the resulting block a single string
     *
     * @param iterator a valid SplitQueryIterator
     * @throws InvalidSPARQLException
     * @return everything between the next '{' and '}'
     */
    private void parseBlock(SplitQuery.SplitQueryIterator iterator) throws InvalidSPARQLException
    {
        String block = "";

        while (iterator.hasNextIncludingNewLines()) {
            // do we get a new inner block
            if (iterator.peekNext().startsWith("}")) {
                int i = 0; // fu intellij
                iterator.next();
                iterator.breakOff("}");
                statements.add(new SimpleStatement(block));
                return;
            }

            // hooray we have a new inner block!
            if(iterator.peekNext().startsWith("{") || iterator.peekNext().toLowerCase().startsWith("graph")) {
                if(!block.trim().isEmpty())
                {
                    statements.add(new SimpleStatement(block));
                }
                block = "";
                statements.add(new ParenthesesBlock(iterator, true));
                continue;
            }

            String nextPart = iterator.nextIncludingNewLines();

            if (nextPart.trim().equals("\n")) {
                block += nextPart;
            } else {
                block += " " + nextPart;
            }
        }
    }

    /**
     * Provides a string representation for this object. This is a construct representation as it would be
     * found in a valid SPARQL query.
     *
     * @return the string representation for this object
     */
    public String toString()
    {
        String toreturn = "";


        toreturn += "CONSTRUCT\n{\n" + this.constuctBlock + "\n}\n";
        toreturn += "WHERE\n{\n";

        for(IStatement statement : statements)
            toreturn += statement.toString();

        toreturn += "\n}";

        for(String smod : this.solutionModifier)
        {
            toreturn += smod + "\n";
        }

        return toreturn;
    }

    /**
     * @param constructBlock this.constructBlock = constructBlock
     */
    public void setConstuctBlock(String constructBlock)
    {
        this.constuctBlock = constructBlock;
    }

    /**
     * @param selectModifier this.selectModifier = selectModifier
     */
    public void setSelectModifier(String selectModifier)
    {
        this.selectModifier = selectModifier;
    }

    /**
     * @param solutionModifier this.solutionModifier = solutionModifier
     */
    public void setSolutionModifier(List<String>solutionModifier)
    {
        this.solutionModifier = solutionModifier;
    }

    /**
     * @return this.solutionModifier
     */
    public List<String> getSolutionModifier()
    {
        return this.solutionModifier;
    }

    /**
     * @param statements this.statements = statements
     */
    public void setStatements(List<IStatement> statements)
    {
        this.statements = statements;
    }

    /**
     * @return this.statements
     */
    public List<IStatement> getStatements()
    {
        return this.statements;
    }

    /**
     * @param graph this.graph = graph
     */
    public void setGraph(String graph)
    {
        this.graph = graph;
    }

    /**
     * returns the statement type
     * @return CONSTRUCT
     */
    public StatementType getType()
    {
        return StatementType.CONSTRUCT;
    }

    /**
     * @return a carbon copy of this object
     */
    public ConstructBlock clone()
    {
        // initialize the clone
        ConstructBlock clone = new ConstructBlock();

        // copying the member vars
        clone.setConstuctBlock(this.constuctBlock);
        clone.setSelectModifier(this.selectModifier);
        clone.setGraph(this.graph);
        for(String sol:this.solutionModifier)
                clone.getSolutionModifier().add(sol);
        for(IStatement statement:this.statements)
                clone.getStatements().add(statement.clone());

        // return the clone
        return clone;
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

        for(IStatement s : this.statements)
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
     * @return the graph on which this statement operates
     */
    public String getGraph()
    {
        return this.graph;
    }
}
