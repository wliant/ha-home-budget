package com.homebudget.util;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom Logback appender for capturing log events in tests.
 *
 * Usage in tests:
 * <pre>
 * TestLogAppender appender = new TestLogAppender();
 * appender.start();
 * Logger logger = (Logger) LoggerFactory.getLogger(MyClass.class);
 * logger.addAppender(appender);
 *
 * // Execute code that logs
 *
 * List&lt;ILoggingEvent&gt; logs = appender.getEvents();
 * assertEquals("Expected message", logs.get(0).getFormattedMessage());
 *
 * logger.detachAppender(appender);
 * </pre>
 */
public class TestLogAppender extends AppenderBase<ILoggingEvent> {

    private final List<ILoggingEvent> events = new ArrayList<>();

    @Override
    protected void append(ILoggingEvent event) {
        // Make a defensive copy to prevent modifications
        events.add(event);
    }

    /**
     * Get all captured log events.
     */
    public List<ILoggingEvent> getEvents() {
        return new ArrayList<>(events);
    }

    /**
     * Get all log messages as strings.
     */
    public List<String> getMessages() {
        return events.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }

    /**
     * Get all log events at a specific level.
     */
    public List<ILoggingEvent> getEventsByLevel(ch.qos.logback.classic.Level level) {
        return events.stream()
                .filter(event -> event.getLevel().equals(level))
                .collect(Collectors.toList());
    }

    /**
     * Get all log events containing a specific message substring.
     */
    public List<ILoggingEvent> getEventsByMessage(String messageSubstring) {
        return events.stream()
                .filter(event -> event.getFormattedMessage().contains(messageSubstring))
                .collect(Collectors.toList());
    }

    /**
     * Clear all captured events.
     */
    public void clear() {
        events.clear();
    }

    /**
     * Check if any log event contains the given message substring.
     */
    public boolean contains(String messageSubstring) {
        return events.stream()
                .anyMatch(event -> event.getFormattedMessage().contains(messageSubstring));
    }

    /**
     * Get count of captured events.
     */
    public int size() {
        return events.size();
    }
}
