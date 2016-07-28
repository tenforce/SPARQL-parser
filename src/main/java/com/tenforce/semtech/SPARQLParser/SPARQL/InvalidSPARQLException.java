package main.java.com.tenforce.semtech.SPARQLParser.SPARQL;

/**
 * Created by langens-jonathan on 18.07.16.
 *
 * version 0.0.1
 *
 * This exception gets thrown to denote an invalid SPARQL query was encountered.
 *
 * Other than the naming this exception is the same as a normal java exception
 */
public class InvalidSPARQLException extends Exception
{
    /**
     * Default constructor
     *
     * The exception cannot be thrown without a message stating why the query is invalid
     *
     * @param msg the message stating why the query is invalid
     */
    public InvalidSPARQLException(String msg)
    {
        super(msg);
    }
}
