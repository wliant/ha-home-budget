'use client';

import React from 'react';
import {
  Box,
  Typography,
} from '@mui/material';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from 'recharts';
import { useTheme } from '@mui/material/styles';
import { formatCurrency, formatCurrencyCompact } from '@/utils/formatters';
import type { DayOfWeekAggregate } from '@/services/expenseService';

interface DayOfWeekChartProps {
  aggregates: DayOfWeekAggregate[];
}

const SHORT_DAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

export default function DayOfWeekChart({ aggregates }: DayOfWeekChartProps) {
  const theme = useTheme();

  if (aggregates.length === 0) {
    return (
      <Box sx={{ py: 4, textAlign: 'center' }}>
        <Typography color="text.secondary">No spending data for this period.</Typography>
      </Box>
    );
  }

  // Ensure all 7 days are present, sorted Mon-Sun
  const dayOrder = [2, 3, 4, 5, 6, 7, 1]; // Monday first
  const aggMap = new Map(aggregates.map((a) => [a.dayOfWeek, a]));
  const chartData = dayOrder.map((dow) => {
    const agg = aggMap.get(dow);
    return {
      day: SHORT_DAYS[dow - 1],
      dayOfWeek: dow,
      totalAmount: agg?.totalAmount ?? 0,
      expenseCount: agg?.expenseCount ?? 0,
      averageAmount: agg?.averageAmount ?? 0,
      isWeekend: dow === 1 || dow === 7,
    };
  });

  const maxAmount = Math.max(...chartData.map((d) => d.totalAmount));

  return (
    <Box>
      <Box sx={{ width: '100%', height: { xs: 200, sm: 250 } }}>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={chartData} margin={{ top: 5, right: 20, left: 10, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" stroke={theme.palette.divider} />
            <XAxis
              dataKey="day"
              tick={{ fontSize: 12, fill: theme.palette.text.secondary }}
            />
            <YAxis
              tickFormatter={(v) => formatCurrencyCompact(v)}
              tick={{ fontSize: 11, fill: theme.palette.text.secondary }}
              width={70}
            />
            <Tooltip
              formatter={(value) => formatCurrency(Number(value))}
              labelFormatter={(label) => `${label}`}
              contentStyle={{
                backgroundColor: theme.palette.background.paper,
                border: `1px solid ${theme.palette.divider}`,
                borderRadius: 8,
              }}
            />
            <Bar dataKey="totalAmount" name="Total Spent" radius={[4, 4, 0, 0]}>
              {chartData.map((entry, index) => (
                <Cell
                  key={index}
                  fill={
                    entry.isWeekend
                      ? theme.palette.error.light
                      : entry.totalAmount === maxAmount
                        ? theme.palette.warning.main
                        : theme.palette.primary.main
                  }
                />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </Box>

      {/* Summary stats below chart */}
      <Box sx={{ display: 'flex', gap: 3, mt: 1, justifyContent: 'center', flexWrap: 'wrap' }}>
        {chartData.map((d) => (
          <Box key={d.day} sx={{ textAlign: 'center' }}>
            <Typography variant="caption" color={d.isWeekend ? 'error.main' : 'text.secondary'} sx={{ fontWeight: d.isWeekend ? 600 : 400 }}>
              {d.day}
            </Typography>
            <Typography variant="body2" sx={{ fontWeight: 500 }}>
              {d.expenseCount} txn{d.expenseCount !== 1 ? 's' : ''}
            </Typography>
          </Box>
        ))}
      </Box>
    </Box>
  );
}
