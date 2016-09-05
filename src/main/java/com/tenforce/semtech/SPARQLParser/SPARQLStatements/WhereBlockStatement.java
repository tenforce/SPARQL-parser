package com.tenforce.semtech.SPARQLParser.SPARQLStatements;

import com.tenforce.semtech.SPARQLParser.SPARQL.SplitQuery;
import com.tenforce.semtech.SPARQLParser.SPARQL.InvalidSPARQLException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by langens-jonathan on 20.07.16.
 */
public class WhereBlockStatement extends BlockStatement
{
    //private List<IStatement> statements = new ArrayList<IStatement>();

    public WhereBlockStatement(String block, String graph)
    {
        this.statements.add(new SimpleStatement(block));
        this.type = BLOCKTYPE.WHERE;
        this.graph = graph;
    }

    private WhereBlockStatement()
    {

    }

    public WhereBlockStatement(SplitQuery.SplitQueryIterator iterator) throws InvalidSPARQLException
    {
        this.type = BLOCKTYPE.WHERE;

        if(!iterator.hasNext())
        {
            throw new InvalidSPARQLException("Invalid SPARQL: on line " + iterator.getCurrentLine() + " near: " + iterator.getPrevious());
        }

        String next = iterator.next();
        if (!next.startsWith("{")) {
            throw new InvalidSPARQLException("Invalid SPARQL on line: " + iterator.getCurrentLine() + " expected '{' at" + iterator.getPrevious());
        }
        iterator.breakOff("{");

        parseBlock(iterator);
    }

    public Set<String> getUnknowns()
    {
        Set<String> u = new HashSet<String>();
        for(IStatement s : this.statements)
            u.addAll(s.getUnknowns());
        return u;
    }

    private void parseBlock(SplitQuery.SplitQueryIterator iterator) throws InvalidSPARQLException
    {
        String block = "";

        while (iterator.hasNextIncludingNewLines()) {

            // do we get a new inner block
            if (iterator.peekNext().startsWith("}")) {
                iterator.next();
                iterator.breakOff("}");
                if(!block.trim().isEmpty()) {
                    statements.add(new SimpleStatement(block));
                }
                return;
            }

            // hooray we have a new inner block!
            if(iterator.peekNext().startsWith("{") || iterator.peekNext().toLowerCase().startsWith("graph")
                    || iterator.peekNext().toLowerCase().startsWith("otpional")) {
                if(!block.trim().isEmpty()) {
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
        String toReturn = "WHERE\n {";

        for(IStatement statement:statements)
        {
            toReturn += statement.toString() + "\n";
        }
        toReturn = toReturn.substring(0, toReturn.length());

        toReturn += "\n}";

        return toReturn;
    }

    @Override
    public StatementType getType() {
        return StatementType.WHEREBLOCK;
    }

    public void setType(BLOCKTYPE type){this.type = type;}

    public WhereBlockStatement clone()
    {
        WhereBlockStatement clone = new WhereBlockStatement();

        clone.setType(this.type);
        clone.setGraph(this.graph);

        for(IStatement s : this.statements)
            clone.getStatements().add(s.clone());

        for(String s : this.getUnknowns())
            clone.getUnknowns().add(s);

        return clone;
    }
}
