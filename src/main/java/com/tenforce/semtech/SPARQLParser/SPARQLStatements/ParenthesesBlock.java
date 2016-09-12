package com.tenforce.semtech.SPARQLParser.SPARQLStatements;

import com.tenforce.semtech.SPARQLParser.SPARQL.InvalidSPARQLException;
import com.tenforce.semtech.SPARQLParser.SPARQL.SplitQuery;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A ParenthesesBlock is a succession of simple statements between a starting '{'
 * and ending '}' parentheses. It may also include a graph statement
  */
public class ParenthesesBlock implements IStatement
{
    // the innerstatements of a parentheses block
    protected List<IStatement> statements = new ArrayList<IStatement>();

    // the graph (if declaration included)
    protected String graph;

    protected boolean allowSelect = false;

    // if the block is tagged optional
    protected boolean optional = false;

    /**
     * Default constructor with a single statementblock and possibly a named
     * graph on which it operates
     *
     * @param block the block between the parentheses
     * @param graph the named graph
     */
    public ParenthesesBlock(String block, String graph)
    {
        this.statements.add(new SimpleStatement(block));
        graph = graph;
    }

    /**
     * Constructor that takes an iterator to construct the parentheses block from it.
     *
     * @param iterator the iterator over the query
     * @param allowSelect
     * @throws InvalidSPARQLException if the iterator reaches a state which is not compatible with what
     *          would be expected if the iterator operates on a valid SPARQL query
     */
    public ParenthesesBlock(SplitQuery.SplitQueryIterator iterator, boolean allowSelect) throws InvalidSPARQLException
    {
        this.allowSelect = allowSelect;
        calculateBlock(iterator);
    }

    /**
     * Constrcutor that only takes an iterator to operate over
     *
     * @param iterator the iterator
     * @throws InvalidSPARQLException if the iterator reaches a state which is not compatible with what
     *          would be expected if the iterator operates on a valid SPARQL query
     */
    public ParenthesesBlock(SplitQuery.SplitQueryIterator iterator) throws InvalidSPARQLException
    {
        calculateBlock(iterator);
    }

    /**
     * constructor that takes a list of statements and a named graph
     *
     * @param statements the list of statements
     * @param graph the named graph over which they operate
     */
    public ParenthesesBlock(List<IStatement> statements, String graph)
    {
        this.graph = graph;
        this.statements = statements;
    }

    /**
     * calculates the unknowns (on the fly) and returns them as a set
     *
     * @return a set containing all unknowns
     */
    public Set<String> getUnknowns()
    {
        Set<String> u = new HashSet<String>();
        for(IStatement s : this.statements)
            u.addAll(s.getUnknowns());
        return u;
    }

    /**
     * calculates where the block with statements is and what properties it may have
     *
     * @param iterator the iterator that operates over the query
     * @throws InvalidSPARQLException if the iterator reaches a state which is not compatible with what
     *          would be expected if the iterator operates on a valid SPARQL query
     */
    private void calculateBlock(SplitQuery.SplitQueryIterator iterator) throws InvalidSPARQLException
    {
        if(!iterator.hasNext())
        {
            throw new InvalidSPARQLException("Invalid SPARQL: on line " + iterator.getCurrentLine() + " near: " + iterator.getPrevious());
        }

        if(iterator.peekNext().toLowerCase().startsWith("graph")) {
            String graphToken = iterator.next();
            iterator.breakOff(graphToken.substring(0, 5));
            String graph = iterator.next();
            if(!(graph.startsWith("<") && graph.endsWith(">"))) {
                throw new InvalidSPARQLException("Invalid SPARQL on line: " + iterator.getCurrentLine() + " no valid graphname: " + graph);
            }
            this.graph = graph.substring(1, graph.length() - 1);

            String innerP = iterator.next();

            if(!innerP.startsWith("{"))
            {
                throw new InvalidSPARQLException("Invalid SPARQL on line: " + iterator.getCurrentLine() + " expected '{' at" + iterator.getPrevious());
            }

            iterator.breakOff("{");
        }
        else if(iterator.peekNext().toLowerCase().startsWith("optional"))
        {
            String optional = iterator.next();
            if(!optional.toLowerCase().equals("optional"))
            {
                iterator.breakOff(optional.substring(0, 8)); // 8 is the length of optional
            }

            this.optional = true;

            String innerP = iterator.next();
            if(!innerP.startsWith("{"))
            {
                throw new InvalidSPARQLException("Invalid SPARQL on line: " + iterator.getCurrentLine() + " expected '{' at" + iterator.getPrevious());
            }

            iterator.breakOff("{");
        }
        else {
            String next = iterator.next();
            if (!next.startsWith("{")) {
                throw new InvalidSPARQLException("Invalid SPARQL on line: " + iterator.getCurrentLine() + " expected '{' at" + iterator.getPrevious());
            }
            iterator.breakOff("{");
        }

        // read inner block now
        this.parseBlock(iterator);
    }

    /**
     * Parses a block between a '{' and a '}' token and returns the resulting block a single string
     *
     * @param iterator a valid SplitQueryIterator
     * @throws InvalidSPARQLException
     * @return everything between the next '{' and '}'
     */
    protected void parseBlock(SplitQuery.SplitQueryIterator iterator) throws InvalidSPARQLException
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
            if(iterator.peekNext().startsWith("{") || iterator.peekNext().toLowerCase().startsWith("graph") ||
                    iterator.peekNext().toLowerCase().startsWith("optional")) {
                if(!block.trim().isEmpty())
                {
                    statements.add(new SimpleStatement(block));
                }
                block = "";
                statements.add(new ParenthesesBlock(iterator));
                continue;
            }

            // the statment is at its end
            if(iterator.peekNext().startsWith("."))
            {
                iterator.next();
                iterator.breakOff(".");
                if(!block.trim().isEmpty())
                {
                    statements.add(new SimpleStatement(block + " ."));
                }
                block = "";
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
     * @param allowSelect this.allowSelect = allowSelect
     */
    public void setAllowSelect(boolean allowSelect)
    {
        this.allowSelect = allowSelect;
    }

    /**
     * @param optional this.optional = optional
     */
    public  void setOptional(boolean optional)
    {
        this.optional = optional;
    }

    /**
     * @return a string representation of this block
     */
    public String toString()
    {
        String toreturn = "";

        if(graph != null && !graph.trim().isEmpty())
        {
            toreturn += "GRAPH <" + this.graph + ">\n{\n";
        }

        if(optional)
            toreturn += "OPTIONAL {\n";

//        if(optional || (graph != null && !graph.trim().isEmpty()))
//            toreturn += "{\n";

        for(IStatement statement : statements)
            toreturn += statement.toString();

        if(optional)
            toreturn += "\n}";

        if((graph != null && !graph.trim().isEmpty()))
            toreturn += "\n}";

        return toreturn;
    }

    /**
     * @return StatementType.PARENTHESESBLOCK
     */
    public StatementType getType()
    {
        return StatementType.PARENTHESESBLOCK;
    }

    /**
     * @return a clone of this object
     */
    public IStatement clone()
    {
        // first clone this block's statements
        List<IStatement> clonedStatements = new ArrayList<IStatement>();
        for(IStatement s : this.statements)
        {
            clonedStatements.add(s.clone());
        }

        // then initialize a new block
        ParenthesesBlock clone = new ParenthesesBlock(clonedStatements, this.graph);

        // setting the 2 booleans
        clone.setAllowSelect(this.allowSelect);
        clone.setOptional(this.optional);

        // and returning the clone
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
}
