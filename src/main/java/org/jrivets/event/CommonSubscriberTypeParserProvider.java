package org.jrivets.event;

final class CommonSubscriberTypeParserProvider {

    private static final SubscriberTypeParser subscryberTypeParser = new SubscriberTypeParser();
    
    static SubscriberTypeParser getSubscriberTypeParser() {
        return subscryberTypeParser;
    }
}
