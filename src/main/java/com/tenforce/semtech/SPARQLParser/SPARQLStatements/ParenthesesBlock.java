package main.java.com.tenforce.semtech.SPARQLParser.SPARQLStatements;

import main.java.com.tenforce.semtech.SPARQLParser.SPARQL.InvalidSPARQLException;
import main.java.com.tenforce.semtech.SPARQLParser.SPARQL.SplitQuery;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * a string between a '{' and '}'
 */
public class ParenthesesBlock implements IStatement
{
    protected List<IStatement> statements = new ArrayList<IStatement>();

    protected String graph;

    protected boolean allowSelect = false;

    protected boolean optional = false;

    public ParenthesesBlock(String block, String graph)
    {
        this.statements.add(new SimpleStatement(block));
        graph = graph;
    }

    public ParenthesesBlock(SplitQuery.SplitQueryIterator iterator, boolean allowSelect) throws InvalidSPARQLException
    {
        this.allowSelect = allowSelect;
        calculateBlock(iterator);
    }

    public ParenthesesBlock(SplitQuery.SplitQueryIterator iterator) throws InvalidSPARQLException
    {
        calculateBlock(iterator);
    }

    public Set<String> getUnknowns()
    {
        Set<String> u = new HashSet<String>();
        for(IStatement s : this.statements)
            u.addAll(s.getUnknowns());
        return u;
    }

    public void calculateBlock(SplitQuery.SplitQueryIterator iterator) throws InvalidSPARQLException
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

        if(graph != null)
        {
            toreturn += "GRAPH <" + this.graph + ">";
        }

        if(optional)
            toreturn += "OPTIONAL ";

        toreturn += "{\n";

        for(IStatement statement : statements)
            toreturn += statement.toString();

        toreturn += "\n}";

        return toreturn;
    }
}
