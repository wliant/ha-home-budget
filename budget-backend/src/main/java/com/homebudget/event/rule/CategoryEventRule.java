package com.homebudget.event.rule;

import com.homebudget.dto.ExpenseDTO;
import com.homebudget.model.EventSetting;
import com.homebudget.repository.EventSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CategoryEventRule implements ExpenseEventRule {

    private static final Logger logger = LoggerFactory.getLogger(CategoryEventRule.class);
    private static final String SETTING_KEY = "expense.emit.category";

    private final EventSettingRepository eventSettingRepository;

    public CategoryEventRule(EventSettingRepository eventSettingRepository) {
        this.eventSettingRepository = eventSettingRepository;
    }

    @Override
    public boolean shouldEmit(ExpenseDTO expense) {
        if (expense.getCategoryId() == null) {
            return false;
        }

        List<EventSetting> settings = eventSettingRepository.findBySettingKey(SETTING_KEY);
        Set<Long> categoryIds = settings.stream()
                .map(s -> {
                    try {
                        return Long.parseLong(s.getSettingValue());
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid category ID in event_settings: '{}'", s.getSettingValue());
                        return null;
                    }
                })
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        boolean match = categoryIds.contains(expense.getCategoryId());
        if (match) {
            logger.debug("Category rule matched: expense categoryId={} is in emit set {}", expense.getCategoryId(), categoryIds);
        }
        return match;
    }

    @Override
    public String ruleName() {
        return "category-match";
    }
}
