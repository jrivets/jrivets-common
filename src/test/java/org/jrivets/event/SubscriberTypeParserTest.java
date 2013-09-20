package org.jrivets.event;

import java.lang.reflect.Method;

import org.jrivets.event.OnEvent;
import org.jrivets.event.SubscriberTypeDetails;
import org.jrivets.event.SubscriberTypeParser;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class SubscriberTypeParserTest {
    
    private final SubscriberTypeParser typeParser = new SubscriberTypeParser();
    
    class SimpleEvent {
    }
    
    class SimpleSimpleEvent extends SimpleEvent {
    }

    class SimpleEventSubscriber {
        @OnEvent
        public void simpleEvent(SimpleEvent se) {
            System.out.println("SimpleEventSubscriber gets SimpleEvent");
        }
    }
    
    class SimpleSimpleEventSubscriber {
        @SuppressWarnings("unused")
        @OnEvent
        private void simpleEvent(SimpleSimpleEvent se) {
            System.out.println("SimpleSimpleEventSubscriber gets SimpleSimpleEvent");
        }
    }
    
    class NotSimpleEventSubscriber extends SimpleEventSubscriber {
        @OnEvent
        public void simpleEvent(SimpleSimpleEvent se) {
            System.out.println("NotSimpleEventSubscriber gets SimpleSimpleEvent");
        }
    }
    
    class AmbiguousSubscriber {
        @OnEvent
        public void simpleEvent(SimpleSimpleEvent se) {
            
        }
        
        @OnEvent
        public void simpleEvent2(SimpleSimpleEvent se) {
            
        }
    }
    
    class BadMethodSubscriber {
        @OnEvent
        public void simpleEvent2(SimpleSimpleEvent se, int i) {
            
        }
    }
    
    @Test
    public void parseBasic() {
        SubscriberTypeDetails details = typeParser.getSubscriberTypeDetails(SimpleEventSubscriber.class);
        assertEquals(1, details.getEventsMap().size());
        assertNull(details.getMethod(new SimpleSimpleEvent()));
        assertNull(details.getMethod(new Object() {}));
        
        Method m = details.getMethod(new SimpleEvent());
        assertNotNull(m);
        assertEquals("simpleEvent", m.getName());
    }
    
    @Test
    public void parseBasicExtendedClass() {
        SubscriberTypeDetails details = typeParser.getSubscriberTypeDetails(SimpleSimpleEventSubscriber.class);
        assertEquals(1, details.getEventsMap().size());
        assertNull(details.getMethod(new SimpleEvent()));
        assertNull(details.getMethod(new Object() {}));
        
        Method m = details.getMethod(new SimpleSimpleEvent());
        assertNotNull(m);
        assertEquals("simpleEvent", m.getName());
    }

    @Test
    public void parseBasicNotSimpleClass() {
        SubscriberTypeDetails details = typeParser.getSubscriberTypeDetails(NotSimpleEventSubscriber.class);
        assertEquals(2, details.getEventsMap().size());
        assertNotNull(details.getMethod(new SimpleEvent()));
        assertNull(details.getMethod(new Object() {}));
        
        Method m = details.getMethod(new SimpleSimpleEvent());
        assertNotNull(m);
        assertEquals("simpleEvent", m.getName());
    }
    
    @Test
    public void noAnnotations() {
        try {
            typeParser.getSubscriberTypeDetails(SimpleEvent.class);
            fail("This should be wrong subsctiber, because of no annotations");
        } catch (IllegalArgumentException iae) {
            //Ok
        }
    }
    
    @Test
    public void ambiguousSubscriber() {
        try {
            typeParser.getSubscriberTypeDetails(AmbiguousSubscriber.class);
            fail("This should be wrong subsctiber, because it has ambiguous methods");
        } catch (IllegalArgumentException iae) {
            //Ok
        }
    }
    
    @Test
    public void badMethodsSubscriber() {
        try {
            typeParser.getSubscriberTypeDetails(BadMethodSubscriber.class);
            fail("This should be wrong subsctiber, because of bad methods annotated like onEvent");
        } catch (IllegalArgumentException iae) {
            //Ok
        }
    }
}
