JRivets
=======
"Rivets and fasteners for java artisans"

What is it?
-----------
This is java library with a number of different java classes that were developed for some project and had been found suitable to be reused in another one.

What are the classes inside?
----------------------------
Ok. Inside are classes that make the life a little bit easy. For example, if you use Log4j you write sometimes a code like this:
<pre>
    logger.debug("I got " + apples.size() + " and put them to the store " + appleStore);
</pre>

you can find that this is quite not good idea to concatenate strings if the debug log level is turned off, so you re-write code as follows:

<pre>
    ...
    if (logger.isDebugEnabled()) {
        logger.debug("I got " + apples.size() + " and put them to the store " + appleStore);
    }
    ...
</pre>
and then your code becomes to look like this:
<pre>
    ...
    appleStore.put(apples);
    if (logger.isDebugEnabled()) {
        logger.debug("I got " + apples.size() + " and put them to the store " + appleStore);
    }
    if (apples.contains(redApple)) {
        if (logger.isTraceEnabled()) {
            logger.trace("Some apples are red: " + apples.getRedApples());
        }
        ...
    }
    ...
</pre>
which looks ugly because it is littered by massive log constructions. I know, it is not a big deal, but there is a solution, what if we could write log messages like this:
<pre>
    appleStore.put(apples);
    logger.debug("I got ", apples.size(), " and put them to the store ", appleStore);
    
    if (apples.contains(redApple)) {
        logger.trace("Some apples are red: ", apples.getRedApples());
        ...
    }
</pre>
The logger in code above can fix both problems - it concatenates strings only when the result is going to be used (be printed to a log), and remove 'if{ }' garbage as much as possible. 

So JRivets contains the logger and even more...
