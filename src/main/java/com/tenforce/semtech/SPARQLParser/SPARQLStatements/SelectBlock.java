package com.tenforce.semtech.SPARQLParser.SPARQLStatements;


import com.tenforce.semtech.SPARQLParser.SPARQL.InvalidSPARQLException;
import com.tenforce.semtech.SPARQLParser.SPARQL.SplitQuery;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by langens-jonathan on 25.07.16.
 */
public class SelectBlock implements IStatement
{
    private String selectClause = "";
    private Set<String> unknowns = new HashSet<String>();
    private List<IStatement> statements = new ArrayList<IStatement>();
    private String selectModifier = ""; // normally this is DISTINCT or REDUCE
    private boolean inBlock = false; // denotes whether or not this select is a subselect (and thus has to be
                                     // placed between a '{' and a '}'
    private List<String> solutionModifier = new ArrayList<String>();
    private String graph = null;

    public SelectBlock(SplitQuery.SplitQueryIterator iterator, boolean inBlock) throws InvalidSPARQLException
    {
        calculateBlock(iterator);
        this.inBlock = inBlock;
    }

    private SelectBlock()
    {

    }


    public Set<String> getUnknowns()
    {
        Set<String> u = new HashSet<String>();
        for(IStatement s : this.statements)
            u.addAll(s.getUnknowns());
        u.addAll(unknowns);
        return u;
    }

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
            this.selectClause += next + " ";
            if(next.startsWith("?"))
            {
                unknowns.add(next.substring(1, next.length()));
            }
        }

        this.selectClause = this.selectClause.substring(0, this.selectClause.length() - 1);

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

    public String toString()
    {
        String toreturn = "";

        if(this.inBlock) toreturn += "{";

        toreturn += "SELECT " + this.selectClause + "\n";
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

    public String getSelectClause() {
        return selectClause;
    }

    public void setSelectClause(String selectClause) {
        this.selectClause = selectClause;
    }

    public void setUnknowns(Set<String> unknowns) {
        this.unknowns = unknowns;
    }

    public String getSelectModifier() {
        return selectModifier;
    }

    public void setSelectModifier(String selectModifier) {
        this.selectModifier = selectModifier;
    }

    public List<IStatement> getStatements() {
        return statements;
    }

    public void setStatements(List<IStatement> statements) {
        this.statements = statements;
    }

    public boolean isInBlock() {
        return inBlock;
    }

    public void setInBlock(boolean inBlock) {
        this.inBlock = inBlock;
    }

    public List<String> getSolutionModifier() {
        return solutionModifier;
    }

    public void setSolutionModifier(List<String> solutionModifier) {
        this.solutionModifier = solutionModifier;
    }

    public String getGraph() {
        return graph;
    }

    public void setGraph(String graph) {
        this.graph = graph;
    }

    public StatementType getType() {
        return StatementType.SELECTBLOCK;
    }

    public SelectBlock clone()
    {
        SelectBlock clone = new SelectBlock();

        clone.setSelectClause(this.selectClause);
        clone.setSelectModifier(this.selectModifier);
        clone.setGraph(this.graph);
        clone.setInBlock(this.inBlock);
        for(String u : this.unknowns)
            clone.getUnknowns().add(u);
        for(String s : this.solutionModifier)
            clone.getSolutionModifier().add(s);
        for(IStatement s : this.statements)
            clone.getStatements().add(s.clone());

        return this.clone();
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
