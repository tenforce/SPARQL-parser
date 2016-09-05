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
 * Parses a correct describe SPARQL block inside a SPARQL query.
 * It looks like
 * DESCRIBE ?x, ?y
 * WHERE
 * {
 *     /WHERE CLAUSE/
 * }
 */
public class DescribeBlock implements IStatement
{
    // the describe clause can be complex just as the select clause
    private String describeClause = "";

    // the unknowns found and parsed in the describe clause
    private Set<String> unknowns = new HashSet<String>();

    // the statements that can be found inside the WHERE block
    private List<IStatement> statements = new ArrayList<IStatement>();

    // the select modifier
    private String selectModifier = ""; // normally this is DISTINCT or REDUCE

    // if we are inside a block then the String representation should include the outer parentheses
    private boolean inBlock = false; // denotes whether or not this select is a subselect (and thus has to be

    // placed between a '{' and a '}'
    private List<String> solutionModifier = new ArrayList<String>();

    // the graph upon which this object operates
    private String graph = null;

    /**
     * Default constructor takes an iterator just after the DESCRIBE token.
     *
     * @param iterator the iterator just after the DESCRIBE token
     * @param inBlock true if we are between { }
     * @throws InvalidSPARQLException if the SPARQL query is invalid
     *
     * @result this object is a valid DESCRIBE block
     */
    public DescribeBlock(SplitQuery.SplitQueryIterator iterator, boolean inBlock) throws InvalidSPARQLException
    {
        calculateBlock(iterator);
        this.inBlock = inBlock;
    }

    /**
     * empty constructor, only intended to be used for cloning this object
     */
    private DescribeBlock()
    {

    }

    /**
     * returns all unknowns within the construct block
     *
     * @return this.unknowns
     */
    public Set<String> getUnknowns()
    {
        return this.unknowns;
    }

    /**
     * calculates the inner DESCRIBE block statement
     *
     * @param iterator the iterator just after the DESCRIBE token
     * @throws InvalidSPARQLException if the query is not a valid SPARQL query
     *
     * @result this object is correctly initialized to represent a parsed version of the DESCRIBE block
     */
    public void calculateBlock(SplitQuery.SplitQueryIterator iterator) throws InvalidSPARQLException
    {
        if(!iterator.hasNext())
        {
            throw new InvalidSPARQLException("Invalid SPARQL: on line " + iterator.getCurrentLine() + " near: " + iterator.getPrevious());
        }

        if(iterator.peekNext().toLowerCase().equals("distinct"))
        {
            this.selectModifier += iterator.next();
        }

        if(iterator.peekNext().toLowerCase().equals("reduce"))
        {
            this.selectModifier += iterator.next();
        }

        while(iterator.hasNext() && (!iterator.peekNext().toLowerCase().equals("where") && (!iterator.peekNext().toLowerCase().equals("from"))))
        {
            String next = iterator.next();
            this.describeClause += next + " ";
            if(next.startsWith("?"))
            {
                unknowns.add(next.substring(1, next.length()));
            }
        }

        this.describeClause = this.describeClause.substring(0, this.describeClause.length() - 1);

        if(!(iterator.peekNext().toLowerCase().equals("where") || iterator.peekNext().toLowerCase().equals("from")))
        {
            throw new InvalidSPARQLException("Invalid SPARQL at line " + iterator.getCurrentLine() + " expected 'WHERE' or 'FROM' after " + iterator.getPrevious());
        }

        if(iterator.peekNext().toLowerCase().equals("from"))
        {
            iterator.next(); // the from
            String graph = iterator.next(); // this should be <...>
            if(!graph.startsWith("<") || !graph.endsWith(">"))
            {
                throw new InvalidSPARQLException("Invalid SPARQL at line " + iterator.getCurrentLine() + " not a valid graph URI: " + graph);
            }
            this.graph = graph.substring(1, graph.length() - 1);
        }

        // the from has passed so now we MUST have a where

        if(!iterator.next().toLowerCase().equals("where"))
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
     * A valid String representation for this SPARQL query
     *
     * @return the representation that this DESCRIBE block would have inside a valid SPARQL query
     */
    public String toString()
    {
        String toreturn = "";

        if(this.inBlock) toreturn += "{";

        toreturn += "DESCRIBE " + this.describeClause + "\n";
        toreturn += "WHERE\n{\n";

        for(IStatement statement : statements)
            toreturn += statement.toString();

        toreturn += "\n}";

        if(this.inBlock) toreturn += "}";

        for(String smod : this.solutionModifier)
        {
            toreturn += smod + "\n";
        }

        return toreturn;
    }

    /**
     * returns the statement type
     * @return DESCRIBE
     */
    public StatementType getType()
    {
        return StatementType.DESCRIBE;
    }

    /**
     * @return this.describeClause
     */
    public String getDescribeClause() {
        return describeClause;
    }

    /**
     * @param describeClause this.describeClause = describeClause
     */
    public void setDescribeClause(String describeClause) {
        this.describeClause = describeClause;
    }

    /**
     * @param unknowns this.unknowns = unknowns
     */
    public void setUnknowns(Set<String> unknowns) {
        this.unknowns = unknowns;
    }

    /**
     * @return this.statements
     */
    public List<IStatement> getStatements() {
        return statements;
    }

    /**
     * @param statements this.statements = statements
     */
    public void setStatements(List<IStatement> statements) {
        this.statements = statements;
    }

    /**
     * @return this.selectModifier
     */
    public String getSelectModifier() {
        return selectModifier;
    }

    /**
     * @param selectModifier this.selectModifier = selectModifier
     */
    public void setSelectModifier(String selectModifier) {
        this.selectModifier = selectModifier;
    }

    /**
     * @return this.inBlock
     */
    public boolean isInBlock() {
        return inBlock;
    }

    /**
     * @param inBlock this.inBlock = inBlock
     */
    public void setInBlock(boolean inBlock) {
        this.inBlock = inBlock;
    }

    /**
     * @return this.solutionModifier
     */
    public List<String> getSolutionModifier() {
        return solutionModifier;
    }

    /**
     * @param solutionModifier this.solutionModifer = solutionModifier
     */
    public void setSolutionModifier(List<String> solutionModifier) {
        this.solutionModifier = solutionModifier;
    }

    /**
     * @return this.graph
     */
    public String getGraph() {
        return graph;
    }

    /**
     * @param graph this.graph = graph
     */
    public void setGraph(String graph) {
        this.graph = graph;
    }

    /**
     * @return a copy of this object
     */
    public DescribeBlock clone()
    {
        DescribeBlock clone = new DescribeBlock();

        // copying member vars
        clone.setDescribeClause(this.describeClause);
        clone.setSelectModifier(this.selectModifier);
        clone.setInBlock(this.inBlock);
        clone.setGraph(this.graph);
        for(String u : this.unknowns)
                clone.getUnknowns().add(u);
        for(String s : this.solutionModifier)
                clone.getSolutionModifier().add(s);
        for(IStatement s : this.statements)
                clone.getStatements().add(s.clone());

        // returning the clone
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
