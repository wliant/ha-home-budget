'use client';

import React, { useMemo } from 'react';
import {
  Box,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
} from '@mui/material';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts';
import { useTheme } from '@mui/material/styles';
import { formatCurrency, formatCurrencyCompact } from '@/utils/formatters';
import type { UserSpendingAggregate } from '@/services/expenseService';

const USER_COLORS = ['#2196F3', '#4CAF50', '#FF9800', '#E91E63', '#9C27B0', '#00BCD4', '#FF5722', '#795548'];

interface UserSpendingViewProps {
  aggregates: UserSpendingAggregate[];
}

export default function UserSpendingView({ aggregates }: UserSpendingViewProps) {
  const theme = useTheme();

  const { users, categoryChart, userTotals } = useMemo(() => {
    if (aggregates.length === 0) return { users: [], categoryChart: [], userTotals: [] };

    // Get unique users and categories
    const userSet = new Set<string>();
    const categoryMap = new Map<string, Map<string, number>>();

    for (const agg of aggregates) {
      userSet.add(agg.username);
      if (!categoryMap.has(agg.categoryName)) {
        categoryMap.set(agg.categoryName, new Map());
      }
      categoryMap.get(agg.categoryName)!.set(agg.username, agg.amount);
    }

    const users = Array.from(userSet).sort();

    // Build chart data: one bar group per category, one bar per user
    const categoryChart = Array.from(categoryMap.entries())
      .map(([category, userAmounts]) => {
        const entry: Record<string, unknown> = { category };
        let total = 0;
        for (const user of users) {
          const amount = userAmounts.get(user) ?? 0;
          entry[user] = amount;
          total += amount;
        }
        entry._total = total;
        return entry;
      })
      .sort((a, b) => (b._total as number) - (a._total as number))
      .slice(0, 10); // Top 10 categories

    // User totals
    const userTotalMap = new Map<string, number>();
    for (const agg of aggregates) {
      userTotalMap.set(agg.username, (userTotalMap.get(agg.username) ?? 0) + agg.amount);
    }
    const userTotals = Array.from(userTotalMap.entries())
      .map(([username, total]) => ({ username, total }))
      .sort((a, b) => b.total - a.total);

    return { users, categoryChart, userTotals };
  }, [aggregates]);

  if (aggregates.length === 0) {
    return (
      <Box sx={{ py: 4, textAlign: 'center' }}>
        <Typography color="text.secondary">No spending data for this period.</Typography>
      </Box>
    );
  }

  return (
    <Box>
      {/* User totals */}
      <Box sx={{ display: 'flex', gap: 2, mb: 2, flexWrap: 'wrap' }}>
        {userTotals.map((ut, i) => (
          <Chip
            key={ut.username}
            label={`${ut.username}: ${formatCurrency(ut.total)}`}
            sx={{
              fontWeight: 500,
              borderLeft: `4px solid ${USER_COLORS[i % USER_COLORS.length]}`,
              borderRadius: 1,
            }}
            variant="outlined"
          />
        ))}
      </Box>

      {/* Stacked bar chart: spending by category, grouped by user */}
      {categoryChart.length > 0 && (
        <Box sx={{ width: '100%', height: { xs: 250, sm: 300 } }}>
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={categoryChart} layout="vertical" margin={{ top: 5, right: 20, left: 10, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" stroke={theme.palette.divider} />
              <XAxis
                type="number"
                tickFormatter={(v) => formatCurrencyCompact(v)}
                tick={{ fontSize: 11, fill: theme.palette.text.secondary }}
              />
              <YAxis
                type="category"
                dataKey="category"
                width={100}
                tick={{ fontSize: 11, fill: theme.palette.text.secondary }}
              />
              <Tooltip
                formatter={(value) => formatCurrency(Number(value))}
                contentStyle={{
                  backgroundColor: theme.palette.background.paper,
                  border: `1px solid ${theme.palette.divider}`,
                  borderRadius: 8,
                }}
              />
              <Legend />
              {users.map((user, i) => (
                <Bar
                  key={user}
                  dataKey={user}
                  stackId="a"
                  fill={USER_COLORS[i % USER_COLORS.length]}
                  radius={i === users.length - 1 ? [0, 4, 4, 0] : undefined}
                />
              ))}
            </BarChart>
          </ResponsiveContainer>
        </Box>
      )}

      {/* Detailed table */}
      <TableContainer sx={{ mt: 2 }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>User</TableCell>
              <TableCell align="right">Total Spending</TableCell>
              <TableCell align="right">% of Total</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {userTotals.map((ut) => {
              const grandTotal = userTotals.reduce((s, u) => s + u.total, 0);
              const pct = grandTotal > 0 ? (ut.total / grandTotal) * 100 : 0;
              return (
                <TableRow key={ut.username}>
                  <TableCell>
                    <Typography variant="body2" sx={{ fontWeight: 500 }}>{ut.username}</Typography>
                  </TableCell>
                  <TableCell align="right">
                    <Typography variant="body2">{formatCurrency(ut.total)}</Typography>
                  </TableCell>
                  <TableCell align="right">
                    <Typography variant="body2" color="text.secondary">{pct.toFixed(1)}%</Typography>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
}
