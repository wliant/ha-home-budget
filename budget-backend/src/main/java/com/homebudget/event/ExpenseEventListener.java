package com.homebudget.event;

import com.homebudget.dto.ExpenseDTO;
import com.homebudget.event.rule.ExpenseEventRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ExpenseEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseEventListener.class);
    private static final String STREAM_KEY = "expense:events";

    private final List<ExpenseEventRule> rules;
    private final StringRedisTemplate redisTemplate;

    public ExpenseEventListener(List<ExpenseEventRule> rules, StringRedisTemplate redisTemplate) {
        this.rules = rules;
        this.redisTemplate = redisTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExpenseCreated(ExpenseCreatedEvent event) {
        try {
            ExpenseDTO expense = event.getExpense();

            for (ExpenseEventRule rule : rules) {
                if (rule.shouldEmit(expense)) {
                    logger.info("Expense event rule '{}' matched for expense ID={}", rule.ruleName(), expense.getId());
                    publishToRedis(expense, rule.ruleName());
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to process expense event: {}", e.getMessage(), e);
        }
    }

    private void publishToRedis(ExpenseDTO expense, String triggeredBy) {
        Map<String, String> fields = new HashMap<>();
        fields.put("expenseId", String.valueOf(expense.getId()));
        fields.put("amount", expense.getAmount().toPlainString());
        fields.put("description", expense.getDescription());
        fields.put("expenseDate", expense.getExpenseDate().toString());
        fields.put("categoryId", String.valueOf(expense.getCategoryId()));
        fields.put("categoryName", expense.getCategoryName() != null ? expense.getCategoryName() : "");
        fields.put("createdBy", expense.getCreatedBy() != null ? expense.getCreatedBy() : "");
        fields.put("createdAt", expense.getCreatedAt() != null ? expense.getCreatedAt().toString() : "");
        fields.put("triggeredBy", triggeredBy);

        MapRecord<String, String, String> record = StreamRecords.string(fields).withStreamKey(STREAM_KEY);
        redisTemplate.opsForStream().add(record);
        logger.info("Published expense event to Redis stream '{}' for expense ID={}", STREAM_KEY, expense.getId());
    }
}
