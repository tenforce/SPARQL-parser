package com.tenforce.semtech.SPARQLParser.SPARQL;

import com.tenforce.semtech.SPARQLParser.SPARQLStatements.*;

import java.util.*;

/**
 * Created by langens-jonathan on 18.07.16.
 *
 * version 0.0.1
 *
 * Class that represents a SPARQL query.
 *
 * It can be initialized as a blank query that can be constructed and then parsed to a
 * query or it can be initialized with a query string which will then be parsed. In the latter
 * case this object will contain all information present in the query and it can be used
 * to change or switch based on query characteristics.
 *
 * Eventually this class should do both forms, parsing from String and building up queries,
 * correctly and completely.
 *
 * At this point of the document the creation/building of queries is not yet fully supported.
 * There is for instance no checking whether or not a certain block or statement can legally be
 * added to a specific query (for instance you cauld add an insert block to a SELECT query).
 *
 * The Parsing variant that starts with a query string is better supported in this version.
 * The constructor will call the parsing mechanism upon object instantiation and will provide
 * you with meaningfull exception messages as to where and what goes wrong in the query. It has
 * linenumber support and gets basic extra information about valid queries.
 *
 * The different SPARQL query types that are supported are:
 *  - SELECT
 *  - CONSTRUCT
 *  - ASK
 *  - DESCRIBE
 *  - UPDATE
 *  Update is the only type that is allowed to mutate the graph that is being queried.
 *
 *  The original query string is always stored so that it can be returned unmodified if necessary. In the
 *  case of the query being build the getOriginalQuery method will throw a noSuchElementException.
 */
public class SPARQLQuery
{
    /**
     * Denoting the different types of queries
     */
    public enum Type {
        SELECT, CONSTRUCT, ASK, DESCRIBE, UPDATE
    }

    // a map containing all prefixes
    private Map<String, String> prefixes;

    // the type of query it is
    private Type type;

    // a map with all blocks based on type
    private List<IStatement> statements;

    // a list of the unknowns
    private Set<String> unknowns;

    // the graph upon which we want to run this query
    private String graph = "";

    // the original query
    private String originalQuery = null;

    /**
     * Default constructor
     *
     * The only reason this constructor exists is to facilitates the cloning of a
     * SPARQLQuery
     */
    public SPARQLQuery()
    {
        this.originalQuery = "";
        initializeVariables();
    }

    /**
     * Constructor that takes a query object and produces a parsed query.
     *
     * @param query the query that this object should represent
     * @throws InvalidSPARQLException
     * @result the object is fully initialized with the query as a parsed SPARQLQuery object
     */
    public SPARQLQuery(String query) throws InvalidSPARQLException
    {
        this.originalQuery = query;

        SplitQuery splitQuery = new SplitQuery(query);

        initializeVariables();

        parseSplitQuery(splitQuery);
    }

    /**
     * Initializes all lists, hashmaps, etc..
     *
     * @result all structured variables are initialized correctly
     */
    private void initializeVariables()
    {
        this.prefixes = new HashMap<String, String>();
        this.statements = new ArrayList<IStatement>();
        this.unknowns = new HashSet<String>();
    }

    /**
     * Parse prefix expects the next 3 blocks that the iterator returns are a perfect SPARQL Query Prefix definition
     * it expects the next things to come out to be [prefix] [:] [uri]
     *
     * @param iterator the iterator that produced the "prefix" statement (thus the previous iterator.next() already
     *                 yielded a "prefix")
     * @param next the form in which this prefix was described, ie. it could be "prefix" but also "pREfiX" this will
     *             help the user of this library to determine where his query was bad
     * @throws InvalidSPARQLException if the expected next's are not correct then the query is not valid
     * @result a prefix is added to this objects' prefixes map
     */
    private void parsePrefix(SplitQuery.SplitQueryIterator iterator, String next) throws InvalidSPARQLException
    {
        // Prefix line found, the next 3 things will be [prefix] [:] [full-uri]
        if(!iterator.hasNext())
        {
            throw new InvalidSPARQLException("Invalid SPARQL: on line " + ((SplitQuery.SplitQueryIterator)iterator).getCurrentLine() + " near: " + next);
        }

        String prefix = iterator.next();

        if(!iterator.hasNext())
        {
            throw new InvalidSPARQLException("Invalid SPARQL: on line " + ((SplitQuery.SplitQueryIterator)iterator).getCurrentLine() + " near: " + next);
        }

        if(prefix.endsWith(":"))
        {
            // ok the prefix ends with : so we dont need it from the next
            prefix = prefix.substring(0, prefix.length()-1).trim();
        }
        else {
            // the prefix doesn't end with :, this means that the next thing we take should be the ':'
            if (!iterator.next().equals(":")) {
                throw new InvalidSPARQLException("Invalid SPARQL: on line " + ((SplitQuery.SplitQueryIterator) iterator).getCurrentLine() + " near: " + next + " " + prefix + " Expected a ':'");
            }
        }

        if(!iterator.hasNext())
        {
            throw new InvalidSPARQLException("Invalid SPARQL: on line " + ((SplitQuery.SplitQueryIterator)iterator).getCurrentLine() + " near: " + next + " " + prefix + ":");
        }

        String uri = iterator.next();

        if(!uri.startsWith("<") || !uri.endsWith(">"))
        {
            throw new InvalidSPARQLException("Invalid SPARQL: on line " + ((SplitQuery.SplitQueryIterator)iterator).getCurrentLine() + " near: " + next + " " + prefix + ":");
        }

        this.prefixes.put(prefix, uri.substring(1, uri.length() - 1));
    }

    /**
     * parseSplitQuery
     *
     * This function is the meat of this class. It provides a high level way of parsing a SPARQL query. The only
     * things that it depends on are the SplitQuery class that is able to 'tokenize' a query into string blocks.
     *
     * The strategy it will use is, it has an iterator that always 'points' at the current block in the list
     * of blocks. And then it will loop until all blocks have been processed.
     *
     * In each loop the current block will be switch upon by the following table:
     * switch(currentBlock)
     *   case "FROM": -> handle from [in this function]
     *   case "SELECT": -> parse SelectBlock
     *   case "CONSTRUCT": -> parse ConstructBlock
     *   case "DESCRIBE": -> parse DesribeBlock
     *   case "ASK": -> parse AskBlock
     *   case "DELETE": -> parse UpdateBlock
     *   case "INSERT": -> parse UpdateBlock
     *   case "WHERE": -> parse WhereBlock
     *   default: -> trhow error message
     *
     *   The result will be that this object contains all information contained within the SPARQL query. If this
     *   object would be used to re-construct the query a query that is equivalent to the original one should be
     *   constructed. Over time we might build in optimizers so that the constructed version is always the most
     *   optimal form for this particular query.
     *
     * @param splitQuery a 'tokenized' version of the queryString
     * @throws InvalidSPARQLException if the query is not a valid SPARQL query
     *
     * @result this object contains the full parsed representation of the tokenized query string
     */
    private void parseSplitQuery(SplitQuery splitQuery) throws InvalidSPARQLException
    {
        boolean prologueDone = false; // prologue is the part of the query containing prefixes and base declarations
        boolean typeDone = false; // the type of the query has been parsed

        Iterator<String> it = splitQuery.iterator();
        SplitQuery.SplitQueryIterator iterator = (SplitQuery.SplitQueryIterator) it;

        while (iterator.hasNext())
        {
            String next = iterator.next();

            // maybe this is a prefix thingie
            if(next.toLowerCase().equals("prefix"))
            {
                if(prologueDone == true)
                {
                    throw new InvalidSPARQLException("Invalid SPARQL: on line " + ((SplitQuery.SplitQueryIterator)iterator).getCurrentLine() + " near: " + next +
                    " prefixes have to be declared at the start of the query!");
                }

                this.parsePrefix(((SplitQuery.SplitQueryIterator) iterator), next);
                continue;
            }

            // if we get this far then we are no longer parsing prologue stuff
            prologueDone = true;

            // are we describing a graph?
            if(next.toLowerCase().equals("from"))
            {
                String graph = iterator.next();
                if(graph.toLowerCase().equals("named"))
                {
                    // ok you can have from and from named :)
                    // thus the graph name follows
                    graph = iterator.next();
                }
                if(!graph.startsWith("<") || !graph.endsWith(">"))
                {
                    throw new InvalidSPARQLException("Invalid SPARQL on line: " + iterator.getCurrentLine() + graph + "is not a valid graph name near " + iterator.getPrevious());
                }
                this.graph = graph.substring(1, graph.length() - 1);
                continue;
            }

            // we can also describe a graph with "WITH"
            if(next.toLowerCase().equals("with"))
            {
                String graph = iterator.next();
                if(!graph.startsWith("<") || !graph.endsWith(">"))
                {
                    throw new InvalidSPARQLException("Invalid SPARQL on line: " + iterator.getCurrentLine() + graph + "is not a valid graph name near " + iterator.getPrevious());
                }
                this.graph = graph.substring(1, graph.length() - 1);
                continue;
            }


            // is it a select?
            if(next.toLowerCase().equals("select"))
            {
                if(typeDone)
                {
                    throw new InvalidSPARQLException("Invalid SPARQL query, unexpected " + next + " on line " + iterator.getCurrentLine());
                }
                typeDone = true;
                this.type = Type.SELECT;

                SelectBlock sb = new SelectBlock(iterator, false);
                this.statements.add(sb);
                this.unknowns.addAll(sb.getUnknowns());
                continue;
            }

            // or a construct
            if(next.toLowerCase().equals("construct"))
            {
                if(typeDone)
                {
                    throw new InvalidSPARQLException("Invalid SPARQL query, unexpected " + next + " on line " + iterator.getCurrentLine());
                }
                typeDone = true;
                this.type = Type.CONSTRUCT;
                this.statements.add(new ConstructBlock(iterator));
                break;
            }

            // or a describe
            if(next.toLowerCase().equals("describe"))
            {
                if(typeDone)
                {
                    throw new InvalidSPARQLException("Invalid SPARQL query, unexpected " + next + " on line " + iterator.getCurrentLine());
                }
                typeDone = true;
                this.type = Type.DESCRIBE;
                this.statements.add(new DescribeBlock(iterator, false));
                break;
            }

            // or an ask
            if(next.toLowerCase().equals("ask"))
            {
                if(typeDone)
                {
                    throw new InvalidSPARQLException("Invalid SPARQL query, unexpected " + next + " on line " + iterator.getCurrentLine());
                }
                typeDone = true;
                this.type = Type.ASK;
                if(iterator.peekNext().toLowerCase().equals("from"))
                {
                    // we are asking stuff about a specific graph
                    iterator.next(); // the 'FROM'
                    String graph = iterator.next();
                    if(!graph.startsWith("<") || !(graph.endsWith(">")))
                    {
                        throw new InvalidSPARQLException("Invalid SPARQL on line :" + iterator.getCurrentLine() + " " + graph + " is not a valid graph name");
                    }
                    this.graph = graph.substring(1, graph.length() - 1);
                }
                // add the next thing as an ask block
                this.statements.add(new AskBlock(iterator));
                break;
            }

            // or a delete
            if(next.toLowerCase().equals("delete"))
            {
                if(this.type != Type.UPDATE && typeDone)
                {
                    throw new InvalidSPARQLException("Invalid SPARQL query, unexpected " + next + " on line " + iterator.getCurrentLine());
                }
                typeDone = true;
                this.type = Type.UPDATE;
                //extractBlocks(iterator, UpdateBlockStatement.BLOCKTYPE.DELETE);
                statements.add(new UpdateBlockStatement(BlockStatement.BLOCKTYPE.DELETE, iterator));
                continue;
            }

            // or a insert
            if(next.toLowerCase().equals("insert"))
            {
                if(this.type != Type.UPDATE && typeDone)
                {
                    throw new InvalidSPARQLException("Invalid SPARQL query, unexpected " + next + " on line " + iterator.getCurrentLine());
                }
                typeDone = true;
                this.type = Type.UPDATE;
                //extractBlocks(iterator, UpdateBlockStatement.BLOCKTYPE.INSERT);
                statements.add(new UpdateBlockStatement(BlockStatement.BLOCKTYPE.INSERT, iterator));
                continue;
            }

            // maybe we encounter a where block
            if(next.toLowerCase().equals("where"))
            {
                //extractBlocks(iterator, UpdateBlockStatement.BLOCKTYPE.WHERE);
                statements.add(new WhereBlockStatement(iterator));
                continue;
            }

            // on the 'root' level of the query i will ignore ;'s
            if(next.equals(";"))
            {
                continue;
            }
            // oh oh not match found this thing is no SPARQL sir
            throw new InvalidSPARQLException("Invalid SPARQL: on line " + ((SplitQuery.SplitQueryIterator)iterator).getCurrentLine() + " unexpected token: " + next +
                    " this is not correct SPARQL. When this library is updated I will tell what kind of token I expect");

        }

        System.out.println("PARSED!!");
    }

    /**
     * Returns a constructed version of the query that this object represents. This is always constructed so if you
     * pass an original query in the constructor and call this function afterwards you will still get a contructed
     * version of this query.
     *
     * @return the query that this object represetns
     */
    public String constructQuery()
    {
        return this.toString();
    }

    /**
     * Default toString function
     *
     * @return string representation of this object
     */
    public String toString() {
        String asString = "";
        for (String key : this.prefixes.keySet())
        {
            asString += "PREFIX " + key + ": <" + this.prefixes.get(key) + ">\n";
        }

        if(this.graph != null && !this.graph.isEmpty())
        {
            asString += "WITH <" + this.graph + ">\n";
        }

        for(IStatement statement:this.statements)
        {
            asString += statement.toString();
        }

        return asString;
    }

    /**
     * default accessor method
     * @return returns the unknowns
     */
    public Set<String> getUnknowns()
    {
        return this.unknowns;
    }

    /**
     * default accessor method
     * @return a list of all statements in this query
     */

    public List<IStatement> getStatements()
    {
        return this.statements;
    }

    /**
     * default accessor method
     * @return the type of the query
     */
    public Type getType()
    {
        return this.type;
    }

    /**
     * default setter method
     * @param type sets this.type to type
     */
    public void setType(Type type) {this.type = type;}

    /**
     * default accessor method
     * @return the prefixes as a string -> string map
     */
    public Map<String, String> getPrefixes()
    {
        return this.prefixes;
    }

    /**
     * returns the graph IF it is set on the outer level of this query
     * @return this.graph
     */
    public String getGraph()
    {
        return this.graph;
    }

    /**
     * default setter method for graph
     * @param graph sets this.graph to graph
     */
    public void setGraph(String graph){this.graph = graph;}

    /**
     * @return the original query before it was parsed
     */
    public String getOriginalQuery()
    {
        return this.originalQuery;
    }

    /**
     * replaces ALL graph statements in this query with the new graph.
     * The equivalent of removing all graph statemetns would be calling this
     * function with "" as parameter
     *
     * @param newGraph the new graph name
     */
    public void replaceGraphStatements(String newGraph)
    {
        this.setGraph(newGraph);
        for(IStatement s : this.statements)
            s.replaceGraphStatements(newGraph);
    }

    /**
     * replaces ALL graph statements that are equal to the passed oldGraph
     * argument with the newGraph argument. If you just want to remove the
     * oldGraph statements this function can be called with "" as the parameter
     * for the new graph name
     *
     * @param oldGraph the named of the old graph
     * @param newGraph the name of the new graph
     */
    public void replaceGraphStatement(String oldGraph, String newGraph)
    {
        if(this.graph != null && this.graph.equals(oldGraph))
        {
            this.setGraph(newGraph);
        }
        for(IStatement s : this.statements)
            s.replaceGraphStatements(oldGraph, newGraph);
    }

    /**
     * A carbon copy of this query
     * @return a clone of this object
     */
    public SPARQLQuery clone()
    {
        SPARQLQuery clone = new SPARQLQuery();

        // copying the keys
        for(String key: this.prefixes.keySet())
        {
            clone.getPrefixes().put(key, this.prefixes.get(key));
        }

        // copy the type
        clone.setType(this.type);

        // setting the unknowns
        for(String unknown: this.unknowns)
        {
            clone.getUnknowns().add(unknown);
        }

        // setting the graph
        clone.setGraph(this.graph);

        // copying the statements
        for(IStatement statement : this.statements)
            clone.getStatements().add(statement.clone());

        return clone;
    }
}
