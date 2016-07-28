package main.java.com.tenforce.semtech.SPARQLParser.SPARQL;

import java.util.*;

/**
 * Created by langens-jonathan on 18.07.16.
 *
 * version 0.0.1
 *
 * SplitQuery class splits a query so it can be parsed to produce a parsed query. It has an inner class
 * SplitQueryIterator that provides iteration support for this parsing effort. The query will be split based
 * on different whitespace characters (' '', '\t', '\r', '\n', '\"', '{', '}').
 * The query is also split on newlines ('\n','\r','\"', '{', '}') but this just triggers the previous block
 * * to be added AND the newline to be added as a seperate block.
 *
 * There is support for recognizing strings (everything between 2 '\"' characters) and URI's (evertything
 * between 2 '<', '>' characters). These things are always not parsed and provided in their totalilty as
 * tokens.
 *
 * Comments are also supported and everything that is behind a comment character up until the newline or
 * carriage return is thrown away.
 *
 * It also has support for replacing the currentblock with an array of String objects. This can be used to
 * facilitate 'special' parsing cases.
 *
 * And it also provides support for newlines and keeping count of on which line number you currently are in
 * the original query while taking next() String objects from the SplitQueryIterator.
 */
public class SplitQuery implements Iterable<String>
{
    // the variable that will hold the array of query parts
    private String [] splitQuery = null;

    /**
     * default constructor, expects a valid SPARQL query object
     *
     * @param query the SPARQL query
     */
    public SplitQuery(String query)
    {
        performSplitQuery(query);
    }

    /**
     * returns a new SplitQueryIterator object that is initialised to the front
     * of the splitQuery array. SplitQueryIterator is a public inner class
     *
     * @return new SplitQueryIterator
     */
    public Iterator<String> iterator()
    {
        return new SplitQueryIterator();
    }

    /**
     * Splits a query in to an array of elements with instead of 1 split character all
     * whitespace characters (' '', '\t', '\r', '\n', '\"', '{', '}') are used to split.
     * The query is also split on newlines ('\n','\r', '\"', '{', '}') but this just triggers the previous block
     * to be added AND the newline to be added as a seperate block. This is useful for constructing
     * something like inner blocks, or keeping count of linenumbers.
     *
     * @param query
     * @return an array of strings that are split based on whitespace characters
     */
    private void performSplitQuery(String query)
    {
        Vector<String> splitQuery = new Vector<String>();

        String [] myArr;

        String currentBuffer = "";

        boolean inString = false;

        boolean inComments = false;

        boolean inURI = false;

        for(byte b : query.getBytes())
        {
            if(!inString && !inComments &&!inURI) {
                if (((char) b) == ' ' || ((char) b) == '\t') {
                    currentBuffer = currentBuffer.trim();
                    if (currentBuffer.length() > 0) splitQuery.add(currentBuffer);
                    currentBuffer = "";
                    continue;
                }
                if (((char) b) == '\n' || ((char) b) == '\r' || ((char) b) == '{' || ((char) b) == '}') {
                    currentBuffer = currentBuffer.trim();
                    if (currentBuffer.length() > 0) splitQuery.add(currentBuffer);
                    currentBuffer = "";
                    splitQuery.add("" + ((char) b));
                    continue;
                }
                if (((char) b) == '\"') {
                    currentBuffer = currentBuffer.trim();
                    if (currentBuffer.length() > 0) splitQuery.add(currentBuffer);
                    currentBuffer = "\"";
                    inString = true;
                    continue;
                }
                if (((char) b) == '<') {
                    currentBuffer = currentBuffer.trim();
                    if (currentBuffer.length() > 0) splitQuery.add(currentBuffer);
                    currentBuffer = "<";
                    inURI = true;
                    continue;
                }
                if(((char) b) == '#')
                {
                    currentBuffer = currentBuffer.trim();
                    if (currentBuffer.length() > 0) splitQuery.add(currentBuffer);
                    inComments = true;
                    continue;
                }
            }
            else
            {
                if(inString) {
                    if (((char) b) == '\"') {
                        currentBuffer += "\"";
                        currentBuffer = currentBuffer.trim();
                        if (currentBuffer.length() > 0) splitQuery.add(currentBuffer);
                        currentBuffer = "";
                        inString = false;
                        continue;
                    }
                }
                if(inURI) {
                    if (((char) b) == '>') {
                        currentBuffer += ">";
                        currentBuffer = currentBuffer.trim();
                        if (currentBuffer.length() > 0) splitQuery.add(currentBuffer);
                        currentBuffer = "";
                        inURI = false;
                        continue;
                    }
                }
                if(inComments) {
                    if (((char) b) == '\n' || ((char) b) == '\r') {
                        inComments = false;
                    }
                    continue;
                }
            }

            currentBuffer += ((char) b);
        }
        currentBuffer = currentBuffer.trim();
        if(currentBuffer.length()> 0)
            splitQuery.add(currentBuffer);

        String [] arr = new String[splitQuery.size()];

        for(int i = 0; i < splitQuery.size(); ++i)
        {
            arr[i] = splitQuery.elementAt(i);
        }

        this.splitQuery = arr;
    }

    /**
     * Split Query iterator is a class that supports looping over a split query.
     * Aside from implementing the Iterator&lt;T&gt; interface it also provides methods that
     * are useful when parsing queries.
     *
     * Most notably:
     *
     * ReplaceCurrent replaces the current String object by several String objects that are put into
     * the position of the original one in the array. This might be useful when you for example want
     * to parse only '{' but the splitting algorithm gave you '{?s' in this case you can replace this
     * by the more correct [..... '{' '?s' .....]
     *
     * getCurrentLine returns the current line number in the original the string. This is done by,
     * instead of returning newlines, incrementing the line number and returning the next String object
     * in the splitQuery array.
     *
     * Peeknext returns the next String object if it exists but does not increment the currentPart index
     */
    public class SplitQueryIterator implements Iterator<String>
    {
        // the part we are consuming now
        private int currentPart = 0;

        // the line we are currently at
        private int currentLine = 1;

        /**
         * forced override from the Iterator&lt;T&gt; interface
         *
         * @return true if this contains more non-empty, non-newline elements that were not yet returned.
         */
        public boolean hasNext()
        {
            for(int i = this.currentPart; i < splitQuery.length; ++i)
            {
                if(!splitQuery[i].trim().isEmpty())
                {
                    return true;
                }
            }
            return false;
        }

        /**
         * Same as hasNext() but also return true if the next Strings are newlines or empty. This method is
         * intended to be used together with the nextIncludingNewLines
         *
         * @return currentPart < splitQuery.length
         */
        public boolean hasNextIncludingNewLines()
        {
            return currentPart < splitQuery.length;
        }

        /**
         * replaces the current string with the passed block of strings
         *
         * @param a the array containing strings which we will put in the position of the current
         *          string object
         * @result splitQuery = splitQuery[start .... currentPart - 1] + a + splitQuery[currentPart + 1 .... end]
         */
        public void replaceCurrent(String [] a)
        {
            // create a new array list that we will parse to be the new array
            List<String> splitQueryList = new ArrayList<String>();

            // the current part has to be increased because we want
            // ++currentPart;

            // inserting splitQuery[start ... currentPart] -> splitQueryList
            for(int i = 0; i < currentPart - 1; ++i)
            {
                splitQueryList.add(splitQuery[i]);
            }

            // inserting a[start ... end] -> splitQueryList
            for(int i = 0; i < a.length; ++i)
            {
                splitQueryList.add(a[i]);
            }

            // inserting splitQuery[currentPart + 1 ... end] -> splitQueryList
            for(int i = currentPart; i < splitQuery.length; ++i)
            {
                splitQueryList.add(splitQuery[i]);
            }

            // splitQuery <- splitQueryList.toArray()
            splitQuery = new String [splitQueryList.size()];
            int i = 0;
            for(String b : splitQueryList)
                splitQuery[i++] = b;
        }

        public void breakOff(String breakString)
        {
            if(currentPart > 0 && currentPart < splitQuery.length)
            {
                String toReplace = splitQuery[currentPart - 1];
                if(toReplace.toLowerCase().startsWith(breakString) && toReplace.length() > breakString.length())
                {
                    String [] toInsert = { toReplace.substring(0, breakString.length()), toReplace.substring(breakString.length(), toReplace.length()) };
                    replaceCurrent(toInsert);
                }
            }
        }

        /**
         * Forced overridden method for the Iterator&lt;T&gt; interface.
         *
         * if the next block is a newline then the currentLine and currentPart are incremented and this
         * method recursively calls itself, otherwise the currentPArt is incremented and the splitQuery[currentPart]
         * is returned
         *
         * @throws NoSuchElementException
         * @return splitQuery[++currentPart]
         */
        public String next()
        {
            if(this.hasNext())
            {
                if(splitQuery[currentPart].equals("\n"))
                {
                    ++this.currentLine;
                    ++currentPart;
                    return this.next();
                }
                return splitQuery[currentPart++];
            }
            throw new NoSuchElementException();
        }

        /**
         * returns the next String oject without incrementing the current part index, this is
         * essantially a peek
         *
         * @return the String that will be returned by the next next() call
         * @throws NoSuchElementException
         */
        public String peekNext() throws NoSuchElementException
        {
            int currentCurrentPart = currentPart;
            int currentCurrentLine = currentLine;
            String next = next();
            currentPart = currentCurrentPart;
            currentLine = currentCurrentLine;
            return next;
        }

        /**
         * returns the current part including newlines
         *
         * @return splitQuery[++currentPart]
         */
        public String nextIncludingNewLines()
        {
            if(currentPart < splitQuery.length)
            {
                if(splitQuery[currentPart].equals("\n")) {
                    ++this.currentLine;
                }
                return splitQuery[currentPart++];
            }
            throw new NoSuchElementException();
        }

        /**
         * returns the string that will be returned  by the next nextIncludingNewLines() call, also essentially
         * a peek.
         *
         * @return the string that will be returned  by the next nextIncludingNewLines() call
         */
        public String peekNextIncludingNewLines()
        {
            int currentCurrentPart = currentPart;
            int currentCurrentLine = currentLine;
            String next = nextIncludingNewLines();
            currentPart = currentCurrentPart;
            currentLine = currentCurrentLine;
            return next;
        }

        /**
         * forced override from the ITerator&lt;T&gt; interface
         *
         * @throws UnsupportedOperationException
         * @result automatically throws an UnsupportedOperationException
         */
        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        /**
         * returns the currentline number in the original string
         *
         * @return currentLine
         */
        public int getCurrentLine()
        {
            return this.currentLine;
        }

        /**
         * returns up to the last 5 tokens that were extracted
         * @return splitQuery[currentPart - 5 ... currentPart]
         */
        public String getPrevious()
        {
            return getPrevious(5);
        }

        /**
         * returns up to the last X tokens that were extracted where x equals the maxBack parameter
         *
         * @param maxBack the amount of tokens we want to go back
         * @return a string containing the X last tokens concatenated with a ' '
         */
        public String getPrevious(int maxBack)
        {
            int goBackPos = currentPart - maxBack;

            if(goBackPos < 0)
                goBackPos = 0;

            String toreturn = "";

            for(int i = goBackPos; i < currentPart; ++i)
            {
                toreturn += " " + splitQuery[i];
            }
            return toreturn.trim();
        }
    }
}
